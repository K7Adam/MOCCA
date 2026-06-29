package com.mocca.app.data.repository

import com.mocca.app.api.ConnectionQualityTracker
import com.mocca.app.api.MoccaApiClient
import com.mocca.app.api.MoccaSseClient
import com.mocca.app.api.NetworkConfig
import com.mocca.app.domain.model.*
import com.mocca.app.domain.manager.NotificationTracker
import com.mocca.app.util.AppLifecycleObserver
import com.mocca.app.util.AppLifecycleState
import com.mocca.app.util.NetworkObserver
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.sync.withLock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for managing Server-Sent Events streaming.
 * Includes automatic reconnection, network state awareness, and DB persistence.
 * Connection lifecycle (HttpClient management) is handled by ConnectionManager.
 *
 * SINGLE OWNER: This repository is the sole writer of streaming message part deltas
 * to LocalCache. StateCoordinator and other consumers must NOT double-write the same
 * delta to prevent duplicate content in the UI.
 */
class EventStreamRepository(
    private val sseClient: MoccaSseClient,
    private val networkObserver: NetworkObserver? = null,
    private val apiClient: MoccaApiClient? = null,
    private val appLifecycleObserver: AppLifecycleObserver? = null,
    private val notificationTracker: NotificationTracker? = null,
    private val localCache: com.mocca.app.data.local.LocalCache? = null,
    private val bridgeConnectionManager: com.mocca.app.bridge.connection.BridgeConnectionManager? = null
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var connectionJob: Job? = null
    private var networkObserverJob: Job? = null
    private var lifecycleObserverJob: Job? = null
    private var backgroundPauseJob: Job? = null
    private var permissionActionJob: Job? = null
    private var questionActionJob: Job? = null
    private var bridgeEventJob: Job? = null
    private val bridgeEventObserverMutex = Mutex()
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }
    
    // Removed 'scope' variable as we use repositoryScope now
    // autoReconnect toggle was removed from Settings; always enabled.
    private val autoReconnectEnabled = true
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = NetworkConfig.SSE_MAX_RECONNECT_ATTEMPTS
    
    // Connection quality tracker for adaptive behavior
    private val qualityTracker = ConnectionQualityTracker()
    
    // Pause state for background optimization
    private var isPaused = false
    private var wasConnectedBeforePause = false
    
    // Throttle database writes for streaming
    private var lastDbWriteTime = 0L
    private val dbWriteThrottleMs = 150L 
    private var pendingDbDelta = ""
    private var dbWriteJob: Job? = null
    
    // Track streaming messages that have already been inserted into LocalCache
    private val streamingMessagesInserted = ConcurrentHashMap<String, Boolean>()

    
    /**
     * Callback invoked when app resumes from background.
     * Used by AppStateStore to trigger state sync.
     */
    var onAppResume: (() -> Unit)? = null
    
    /**
     * Callback invoked when installation is updated (plugins installed, config changed).
     * Used by StateCoordinator to trigger full cache invalidation and sync.
     */
    var onInstallationUpdated: (() -> Unit)? = null
    
    // Flow for global events (non-session-specific)
    private val _globalEvents = MutableSharedFlow<ServerEvent>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val globalEvents: SharedFlow<ServerEvent> = _globalEvents.asSharedFlow()
    
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected())
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    // OOM FIX: Reduced buffer from 2048→64, changed overflow to DROP_OLDEST to prevent memory exhaustion
    // REPLAY=1 is CRITICAL: Ensures the last event (like SessionIdle) is not missed if collector reconnects
    private val _events = MutableSharedFlow<ServerEvent>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<ServerEvent> = _events.asSharedFlow()
    
    // IMPROVED: Thread-safe streaming text with Mutex
    private val streamingTextMutex = Mutex()
    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()
    
    // Extended thinking state tracking (for Claude/o1 reasoning models)
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()
    
    private val _thinkingContent = MutableStateFlow("")
    val thinkingContent: StateFlow<String> = _thinkingContent.asStateFlow()
    
    private val _thinkingStartTime = MutableStateFlow<Long?>(null)
    val thinkingStartTime: StateFlow<Long?> = _thinkingStartTime.asStateFlow()
    
    // Agent running state tracking (for all agents, not just extended reasoning)
    // This tracks whether any agent is currently working, used for button state
    private val _isAgentRunning = MutableStateFlow(false)
    val isAgentRunning: StateFlow<Boolean> = _isAgentRunning.asStateFlow()
    
    // Track which agent is currently running with its name for UI display
    private val _runningAgentName = MutableStateFlow<String?>(null)
    val runningAgentName: StateFlow<String?> = _runningAgentName.asStateFlow()
    
    // Use Lists instead of single values to prevent race condition overwrites
    private val _pendingPermissions = MutableStateFlow<List<PermissionRequest>>(emptyList())
    val pendingPermissions: StateFlow<List<PermissionRequest>> = _pendingPermissions.asStateFlow()
    
    // Backwards-compatible accessor for first pending permission
    val pendingPermission: StateFlow<PermissionRequest?> = _pendingPermissions
        .map { it.firstOrNull() }
        .stateIn(repositoryScope, SharingStarted.Eagerly, null)
    
    private val _pendingQuestions = MutableStateFlow<List<QuestionRequest>>(emptyList())
    val pendingQuestions: StateFlow<List<QuestionRequest>> = _pendingQuestions.asStateFlow()

    private val _chatTurnState = MutableStateFlow(ChatTurnState())
    val chatTurnState: StateFlow<ChatTurnState> = _chatTurnState.asStateFlow()
    
    // Backwards-compatible accessor for first pending question
    val pendingQuestion: StateFlow<QuestionRequest?> = _pendingQuestions
        .map { it.firstOrNull() }
        .stateIn(repositoryScope, SharingStarted.Eagerly, null)
    
    // Track active sessions for filtering
    private var activeSessionId: String? = null
    private val monitoredSessionIds = MutableStateFlow<Set<String>>(emptySet())
    
    // IMPROVED: Event deduplication with TTL
    private val processedEventIds = ConcurrentHashMap<String, Long>()
    private val eventIdMutex = Mutex()

    /**
     * Connect to the SSE event stream.
     * Will automatically reconnect on failures and when network becomes available.
     * Note: externalScope is ignored in favor of internal repositoryScope
     */
    fun connect(externalScope: CoroutineScope? = null, sessionId: String? = null) {
        if (isEventConnectionActive()) {
            if (activeSessionId == sessionId) {
                Napier.i("[EventStream] Event stream already connected to session: $sessionId")
                startBridgeEventObserver()
                return
            }
            Napier.i("[EventStream] Switching event stream session from $activeSessionId to $sessionId")
            // Clear streaming state when switching sessions
            setActiveSession(sessionId)
        } else {
            setActiveSession(sessionId)
        }
        
        // Add session to monitored set (don't replace - preserve other monitored sessions)
        if (sessionId != null) {
            monitoredSessionIds.update { it + sessionId }
        }
        reconnectAttempts = 0
        isPaused = false
        
        // Only start a new connection if one isn't active. In bridge mode the
        // WebSocket event observer is the live event connection, so there is no
        // SSE job to check here.
        if (!isEventConnectionActive()) {
            _connectionStatus.value = ConnectionStatus.Connecting
            startConnection()
            startNetworkObserver()
            startLifecycleObserver()
            startPermissionActionObserver()
            startQuestionActionObserver()
            startBridgeEventObserver()
        } else {
            startBridgeEventObserver()
        }
    }
    
    /**
     * Start observing events from the MOCCA Bridge WebSocket.
     * This is critical for chat synchronization when connected via the CLI bridge.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun startBridgeEventObserver() {
        if (bridgeEventJob?.isActive == true || bridgeConnectionManager == null) return

        repositoryScope.launch {
            bridgeEventObserverMutex.withLock {
                if (bridgeEventJob?.isActive == true) return@withLock

                Napier.i("[EventStream] Starting Bridge event observer")
                bridgeEventJob = repositoryScope.launch {
                    bridgeConnectionManager.client
                        .filterNotNull()
                        .flatMapLatest { client ->
                            Napier.d("[EventStream] Observing events from new Bridge client")
                            client.events
                        }
                        .collect { bridgeEvent ->
                            handleBridgeEvent(bridgeEvent)
                        }
                }
            }
        }
    }

    private suspend fun handleBridgeEvent(bridgeEvent: com.mocca.app.bridge.protocol.BridgeEvent) {
        // We only care about AI runtime events which contain ServerEvent payloads
        if (bridgeEvent.ns != "ai") return
        
        // Both "runtime.event" (new) and "event" (old/compat) might be used
        if (bridgeEvent.event != "runtime.event" && bridgeEvent.event != "event") return
        
        val payload = bridgeEvent.payload ?: return
        
        try {
            val serverEvent = parseBridgePayload(payload)
            if (serverEvent != null) {
                if (isDuplicateEvent(serverEvent)) {
                    Napier.v("[EventStream] Skipping duplicate Bridge event: ${serverEvent.type}")
                    return
                }
                Napier.v("[EventStream] Processing Bridge event: ${serverEvent.type}")
                handleEvent(serverEvent)
            }
        } catch (e: Exception) {
            Napier.w("[EventStream] Failed to parse Bridge event payload", e)
        }
    }

    private fun parseBridgePayload(payload: JsonElement): ServerEvent? {
        if (payload !is JsonObject) return null
        
        // OpenCode wraps events in a "payload" object sometimes, 
        // but the bridge router.js sends the raw event as the payload.
        // We handle both cases for robustness.
        val eventObject = (payload["payload"] as? JsonObject) ?: payload
        val type = eventObject["type"]?.jsonPrimitive?.content ?: return null
        
        val eventData = json.encodeToString(JsonObject.serializer(), eventObject)
        
        return try {
            when (type) {
                "server.connected" -> json.decodeFromString<ServerEvent.Connected>(eventData)
                "server.heartbeat" -> json.decodeFromString<ServerEvent.Heartbeat>(eventData)
                "session.created" -> json.decodeFromString<ServerEvent.SessionCreated>(eventData)
                "session.updated" -> json.decodeFromString<ServerEvent.SessionUpdated>(eventData)
                "session.deleted" -> json.decodeFromString<ServerEvent.SessionDeleted>(eventData)
                "session.idle" -> json.decodeFromString<ServerEvent.SessionIdle>(eventData)
                "session.error" -> json.decodeFromString<ServerEvent.SessionError>(eventData)
                "session.status" -> json.decodeFromString<ServerEvent.SessionStatus>(eventData)
                "session.diff" -> json.decodeFromString<ServerEvent.SessionDiff>(eventData)
                "session.compacted" -> json.decodeFromString<ServerEvent.SessionCompacted>(eventData)
                "message.updated" -> json.decodeFromString<ServerEvent.MessageUpdated>(eventData)
                "message.removed" -> json.decodeFromString<ServerEvent.MessageRemoved>(eventData)
                "message.part.updated" -> json.decodeFromString<ServerEvent.MessagePartUpdated>(eventData)
                "message.part.delta" -> json.decodeFromString<ServerEvent.MessagePartDelta>(eventData)
                "message.part.removed" -> json.decodeFromString<ServerEvent.MessagePartRemoved>(eventData)
                "permission.asked" -> json.decodeFromString<ServerEvent.PermissionAsked>(eventData)
                "permission.updated" -> json.decodeFromString<ServerEvent.PermissionUpdated>(eventData)
                "permission.replied" -> json.decodeFromString<ServerEvent.PermissionReplied>(eventData)
                "question.asked" -> json.decodeFromString<ServerEvent.QuestionAsked>(eventData)
                "question.replied" -> json.decodeFromString<ServerEvent.QuestionReplied>(eventData)
                "question.rejected" -> json.decodeFromString<ServerEvent.QuestionRejected>(eventData)
                "file.edited" -> json.decodeFromString<ServerEvent.FileEdited>(eventData)
                "file.watcher.updated" -> json.decodeFromString<ServerEvent.FileWatcherUpdated>(eventData)
                "file.updated" -> json.decodeFromString<ServerEvent.FileUpdated>(eventData)
                "installation.updated" -> json.decodeFromString<ServerEvent.InstallationUpdated>(eventData)
                "installation.update.available" -> json.decodeFromString<ServerEvent.InstallationUpdateAvailable>(eventData)
                "todo.updated" -> json.decodeFromString<ServerEvent.TodoUpdated>(eventData)
                "agent.status" -> json.decodeFromString<ServerEvent.AgentStatus>(eventData)
                "pty.created" -> json.decodeFromString<ServerEvent.PtyCreated>(eventData)
                "pty.updated" -> json.decodeFromString<ServerEvent.PtyUpdated>(eventData)
                "pty.exited" -> json.decodeFromString<ServerEvent.PtyExited>(eventData)
                "pty.deleted" -> json.decodeFromString<ServerEvent.PtyDeleted>(eventData)
                "project.updated" -> json.decodeFromString<ServerEvent.ProjectUpdated>(eventData)
                "vcs.branch.updated" -> json.decodeFromString<ServerEvent.VcsBranchUpdated>(eventData)
                "lsp.updated" -> json.decodeFromString<ServerEvent.LspUpdated>(eventData)
                "lsp.client.diagnostics" -> json.decodeFromString<ServerEvent.LspDiagnostics>(eventData)
                "mcp.tools.changed" -> json.decodeFromString<ServerEvent.McpToolsChanged>(eventData)
                "mcp.browser.open.failed" -> json.decodeFromString<ServerEvent.McpBrowserOpenFailed>(eventData)
                "worktree.ready" -> json.decodeFromString<ServerEvent.WorktreeReady>(eventData)
                "worktree.failed" -> json.decodeFromString<ServerEvent.WorktreeFailed>(eventData)
                "server.instance.disposed" -> json.decodeFromString<ServerEvent.ServerInstanceDisposed>(eventData)
                "global.disposed" -> json.decodeFromString<ServerEvent.GlobalDisposed>(eventData)
                "tui.toast.show" -> null
                else -> {
                    Napier.d("[EventStream] Unknown Bridge event type: $type")
                    null
                }
            }
        } catch (e: Exception) {
            Napier.w("[EventStream] Error decoding Bridge event: $type", e)
            null
        }
    }
    
    /**
     * Set the active session for streaming.
     * Clears streaming state when switching to a different session.
     * This ensures streaming text doesn't carry over between sessions.
     */
    fun setActiveSession(sessionId: String?) {
        val previousId = activeSessionId
        if (previousId != sessionId) {
            Napier.i("[EventStream] Switching active session: $previousId -> $sessionId")
            // Clear streaming state when switching sessions
            _streamingText.value = ""
            _isThinking.value = false
            _thinkingContent.value = ""
            _thinkingStartTime.value = null
            // Clear agent running state
            _isAgentRunning.value = false
            _runningAgentName.value = null
        }
        activeSessionId = sessionId
    }
    
    /**
     * Add a session ID to monitor.
     */
    fun monitorSession(sessionId: String) {
        monitoredSessionIds.value = monitoredSessionIds.value + sessionId
    }

    /**
     * Remove a session ID from monitoring.
     */
    fun stopMonitoringSession(sessionId: String) {
        monitoredSessionIds.value = monitoredSessionIds.value - sessionId
    }
    
    /**
     * Start observing permission actions from notification interactions.
     * Handles APPROVE/DENY actions from the notification system.
     */
    private fun startPermissionActionObserver() {
        if (permissionActionJob?.isActive == true) return

        permissionActionJob = repositoryScope.launch {
            PermissionActionBus.actions.collect { action ->
                Napier.i("[EventStream] Received permission action from notification: ${action.permissionId}, reply=${action.replyType.value}")

                // Call the API to reply to the permission using the V2 session-scoped endpoint
                apiClient?.let { client ->
                    val result = client.replyToPermission(
                        sessionId = action.sessionId,
                        requestId = action.permissionId,
                        response = action.replyType
                    )

                    result.fold(
                        onSuccess = {
                            Napier.i("[EventStream] Permission ${action.permissionId} ${action.replyType.value} successfully")
                            // Dismiss the pending permission from our state
                            dismissPermission(action.permissionId)
                        },
                        onFailure = { error ->
                            Napier.e("[EventStream] Failed to reply to permission ${action.permissionId}", error)
                        }
                    )
                }
            }
        }
    }

    /**
     * Start observing question actions from notification interactions.
     * Handles inline replies and rejections from the notification system.
     */
    private fun startQuestionActionObserver() {
        if (questionActionJob?.isActive == true) return

        questionActionJob = repositoryScope.launch {
            // Handle reply actions
            launch {
                QuestionActionBus.replyActions.collect { action ->
                    Napier.i("[EventStream] Received question reply from notification: ${action.questionId}")

                    apiClient?.let { client ->
                        val result = client.replyToQuestion(
                            requestId = action.questionId,
                            answers = action.answers,
                            sessionId = action.sessionId
                        )

                        result.fold(
                            onSuccess = {
                                Napier.i("[EventStream] Question ${action.questionId} replied successfully")
                                dismissQuestion(action.questionId)
                            },
                            onFailure = { error ->
                                Napier.e("[EventStream] Failed to reply to question ${action.questionId}", error)
                            }
                        )
                    }
                }
            }

            // Handle reject actions
            launch {
                QuestionActionBus.rejectActions.collect { action ->
                    Napier.i("[EventStream] Received question reject from notification: ${action.questionId}")

                    apiClient?.let { client ->
                        val result = client.rejectQuestion(
                            requestId = action.questionId,
                            sessionId = action.sessionId
                        )

                        result.fold(
                            onSuccess = {
                                Napier.i("[EventStream] Question ${action.questionId} rejected successfully")
                                dismissQuestion(action.questionId)
                            },
                            onFailure = { error ->
                                Napier.e("[EventStream] Failed to reject question ${action.questionId}", error)
                            }
                        )
                    }
                }
            }
        }
    }
    
    /**
     * IMPROVED: Pause SSE streaming when app goes to background.
     * Keeps connection alive but stops processing events to save resources.
     */
    fun pause() {
        if (isPaused) return
        isPaused = true
        wasConnectedBeforePause = _connectionStatus.value.isConnected
        Napier.i("[EventStream] Paused - wasConnected: $wasConnectedBeforePause")
        
        // Don't disconnect - just pause heartbeat monitoring
        heartbeatJob?.cancel()
    }
    
    /**
     * IMPROVED: Resume SSE streaming when app returns to foreground.
     * Triggers onAppResume callback for state sync.
     */
    fun resume() {
        if (!isPaused) return
        isPaused = false
        Napier.i("[EventStream] Resumed - wasConnected: $wasConnectedBeforePause")
        
        // Resume heartbeat monitoring
        if (_connectionStatus.value.isConnected) {
            startHeartbeatMonitor()
        } else if (wasConnectedBeforePause) {
            // Was connected before pause, try to reconnect
            reconnect(force = true)
        }
        
        // Trigger sync callback for state recovery
        onAppResume?.invoke()
    }
    
    /**
     * Start observing app lifecycle for background/foreground detection.
     */
    private fun startLifecycleObserver() {
        if (appLifecycleObserver == null) return
        
        lifecycleObserverJob?.cancel()
        lifecycleObserverJob = repositoryScope.launch {
            appLifecycleObserver.lifecycleState.collect { state ->
                when (state) {
                    AppLifecycleState.FOREGROUND -> {
                        Napier.i("[EventStream] App foregrounded")
                        resume()
                    }
                    AppLifecycleState.BACKGROUND -> {
                        Napier.i("[EventStream] App backgrounded")
                        // Delay pause to allow for quick returns
                        delay(NetworkConfig.BACKGROUND_PAUSE_DELAY_MS)
                        if (appLifecycleObserver.lifecycleState.value == AppLifecycleState.BACKGROUND) {
                            pause()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Start the SSE connection.
     */
    private var lastEventTime = 0L
    private var heartbeatJob: Job? = null

    private fun isEventConnectionActive(): Boolean {
        if (connectionJob?.isActive == true) return true
        return bridgeConnectionManager?.client?.value != null &&
            _connectionStatus.value is ConnectionStatus.Connected
    }

    private fun startConnection() {
        connectionJob?.cancel()
        heartbeatJob?.cancel()

        if (bridgeConnectionManager?.client?.value != null) {
            Napier.i("[EventStream] Bridge event stream active; skipping direct SSE to avoid duplicate chat deltas")
            _connectionStatus.value = ConnectionStatus.Connected(
                AppInfo(version = "bridge-events", initialized = true)
            )
            return
        }
        
        connectionJob = repositoryScope.launch {
            try {
                sseClient.subscribeToEvents()
                    .onEach { 
                        lastEventTime = System.currentTimeMillis()
                        // Record successful event for quality tracking
                        qualityTracker.recordResult(true)
                    }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        Napier.e("SSE error", error)
                        _connectionStatus.value = ConnectionStatus.Error(error.message ?: "Connection error")
                        qualityTracker.recordResult(false)
                        scheduleReconnect()
                    }
                    .collect { event ->
                        reconnectAttempts = 0 // Reset on successful event
                        
                        // IMPROVED: Event deduplication
                        if (isDuplicateEvent(event)) {
                            Napier.v("[EventStream] Skipping duplicate event: ${event.type}")
                            return@collect
                        }
                        
                        handleEvent(event)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("SSE connection failed", e)
                _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Connection failed")
                qualityTracker.recordResult(false)
                scheduleReconnect()
            }
        }

        // IMPROVED: Start heartbeat monitor with adaptive interval
        startHeartbeatMonitor()
    }
    
    /**
     * IMPROVED: Heartbeat monitor with adaptive interval based on connection quality.
     */
    private fun startHeartbeatMonitor() {
        heartbeatJob?.cancel()
        heartbeatJob = repositoryScope.launch {
            while (true) {
                delay(NetworkConfig.SSE_HEARTBEAT_CHECK_INTERVAL_MS)
                if (!coroutineContext[Job]!!.isActive) break
                if (isPaused) continue // Skip check when paused
                
                val idleTime = System.currentTimeMillis() - lastEventTime
                val isConnected = _connectionStatus.value is ConnectionStatus.Connected
                
                // IMPROVED: Use adaptive heartbeat timeout based on connection quality
                val heartbeatTimeout = qualityTracker.getRecommendedHeartbeatInterval()
                
                if (idleTime > heartbeatTimeout && isConnected) {
                    Napier.w("[EventStream] Heartbeat timeout (${idleTime}ms > ${heartbeatTimeout}ms), reconnecting...")
                    qualityTracker.recordLatency(idleTime)
                    reconnect(force = true)
                }
            }
        }
    }
    
    /**
     * IMPROVED: Check if event is a duplicate using TTL-based deduplication.
     */
    private suspend fun isDuplicateEvent(event: ServerEvent): Boolean {
        val eventId = extractEventId(event) ?: return false
        val now = System.currentTimeMillis()
        
        return eventIdMutex.withLock {
            // Clean old entries
            val iterator = processedEventIds.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value > NetworkConfig.EVENT_DEDUP_TTL_MS) {
                    iterator.remove()
                }
            }
            
            // Check if already processed
            val existing = processedEventIds[eventId]
            if (existing != null) {
                true
            } else {
                processedEventIds[eventId] = now
                false
            }
        }
    }
    
    /**
     * Extract a unique ID from an event for deduplication.
     * Avoids hashCode() — unreliable across process restarts and can collide.
     * Uses deterministic fields (ids, timestamps, lengths) for stable identity.
     */
    private fun extractEventId(event: ServerEvent): String? {
        return when (event) {
            is ServerEvent.MessageUpdated -> with(event.properties.info) {
                "msg-${id}-${time?.completed ?: time?.created ?: 0}-${cost ?: 0.0}"
            }
            is ServerEvent.MessagePartUpdated -> with(event.properties) {
                "part-${part.id}-${part.time?.start ?: 0}-${delta?.length ?: 0}"
            }
            is ServerEvent.SessionUpdated -> with(event.properties.info) {
                "session-${id}-${time?.updated ?: time?.created ?: 0}"
            }
            is ServerEvent.PermissionAsked -> "perm-${event.properties.id}"
            is ServerEvent.QuestionAsked -> "question-${event.properties.id}"
            else -> null // Don't deduplicate connection events, heartbeats, etc.
        }
    }
    
    /**
     * Schedule a reconnection attempt with backoff.
     * IMPROVED: Uses connection quality for adaptive delay.
     */
    private fun scheduleReconnect() {
        if (!autoReconnectEnabled || reconnectAttempts >= maxReconnectAttempts) {
            Napier.w("Not reconnecting: autoReconnectEnabled=$autoReconnectEnabled, attempts=$reconnectAttempts/$maxReconnectAttempts")
            return
        }
        
        if (isPaused) {
            Napier.i("[EventStream] Not reconnecting while paused")
            return
        }
        
        val isOnline = networkObserver?.isCurrentlyOnline() ?: true
        if (!isOnline) {
            Napier.i("Waiting for network before reconnecting")
            _connectionStatus.value = ConnectionStatus.WaitingForNetwork
            return
        }
        
        repositoryScope.launch {
            reconnectAttempts++
            val baseDelay = calculateBackoff(reconnectAttempts)
            // IMPROVED: Apply quality-based multiplier
            val qualityMultiplier = qualityTracker.getReconnectDelayMultiplier()
            val delayMs = (baseDelay * qualityMultiplier).toLong()
            
            Napier.i("Reconnecting in ${delayMs}ms (attempt $reconnectAttempts/$maxReconnectAttempts, quality multiplier: ${qualityMultiplier}x)")
            _connectionStatus.value = ConnectionStatus.Reconnecting(reconnectAttempts, maxReconnectAttempts)
            delay(delayMs)
            
            if (autoReconnectEnabled && !isPaused) {
                _connectionStatus.value = ConnectionStatus.Connecting
                startConnection()
            }
        }
    }
    
    /**
     * Calculate exponential backoff delay.
     */
    private fun calculateBackoff(attempt: Int): Long {
        val delay = (NetworkConfig.SSE_RECONNECT_BASE_DELAY_MS * (1 shl minOf(attempt - 1, 5)))
            .coerceAtMost(NetworkConfig.SSE_RECONNECT_MAX_DELAY_MS)
        return delay + (0..NetworkConfig.SSE_RECONNECT_JITTER_MS.toInt()).random()
    }
    
    /**
     * Start observing network connectivity changes.
     */
    private fun startNetworkObserver() {
        networkObserverJob?.cancel()
        networkObserverJob = repositoryScope.launch {
            networkObserver?.isOnline?.collect { isOnline ->
                Napier.d("Network status changed: online=$isOnline")
                if (isOnline && _connectionStatus.value is ConnectionStatus.WaitingForNetwork) {
                    Napier.i("Network available, reconnecting...")
                    _connectionStatus.value = ConnectionStatus.Connecting
                    startConnection()
                } else if (!isOnline && _connectionStatus.value is ConnectionStatus.Connected) {
                    Napier.w("Network lost")
                    _connectionStatus.value = ConnectionStatus.WaitingForNetwork
                }
            }
        }
    }

    /**
     * Disconnect from the SSE event stream.
     */
    fun disconnect() {
        isPaused = false
        connectionJob?.cancel()
        heartbeatJob?.cancel()
        networkObserverJob?.cancel()
        lifecycleObserverJob?.cancel()
        backgroundPauseJob?.cancel()
        connectionJob = null
        heartbeatJob = null
        networkObserverJob = null
        lifecycleObserverJob = null
        backgroundPauseJob = null
        // Do NOT cancel repositoryScope, it persists for the app lifecycle
        reconnectAttempts = 0
        activeSessionId = null
        monitoredSessionIds.value = emptySet()
        _connectionStatus.value = ConnectionStatus.Disconnected()
        _streamingText.value = ""
        _isThinking.value = false
        _thinkingContent.value = ""
        _thinkingStartTime.value = null
        _pendingPermissions.value = emptyList()
        _pendingQuestions.value = emptyList()
        _chatTurnState.value = ChatTurnState()
        processedEventIds.clear()
        Napier.i("SSE disconnected")
    }
    
    /**
     * Force a reconnection attempt.
     * @param force If true, restarts connection even if already connected or connecting.
     */
    fun reconnect(force: Boolean = false) {
        val currentStatus = _connectionStatus.value
        
        // Prevent redundant reconnections
        if (!force) {
            if (currentStatus is ConnectionStatus.Connected) {
                Napier.d("Already connected, ignoring reconnect request")
                return
            }
            if (currentStatus is ConnectionStatus.Connecting) {
                Napier.d("Already connecting, ignoring reconnect request")
                return
            }
        }

        reconnectAttempts = 0
        _connectionStatus.value = ConnectionStatus.Connecting
        startConnection()
    }

    /**
     * Clear the current streaming text buffer.
     * IMPROVED: Thread-safe with mutex.
     */
    suspend fun clearStreamingText() {
        streamingTextMutex.withLock {
            _streamingText.value = ""
        }
    }
    
    /**
     * Clear the thinking state (call when message completes).
     */
    fun clearThinkingState() {
        _isThinking.value = false
        _thinkingContent.value = ""
        _thinkingStartTime.value = null
    }

    /**
     * Dismiss the first pending permission request.
     */
    fun dismissPermission() {
        val current = _pendingPermissions.value
        if (current.isNotEmpty()) {
            _pendingPermissions.value = current.drop(1)
        }
    }
    
    /**
     * Dismiss a specific permission request by ID.
     * Used when handling permission actions from notifications.
     */
    fun dismissPermission(permissionId: String) {
        val current = _pendingPermissions.value
        _pendingPermissions.value = current.filter { it.id != permissionId }
    }
    
    /**
     * Get the current (first) pending permission for external handling.
     */
    fun getCurrentPermission(): PermissionRequest? = _pendingPermissions.value.firstOrNull()

    /**
     * Dismiss the first pending question request.
     */
    fun dismissQuestion() {
        val current = _pendingQuestions.value
        if (current.isNotEmpty()) {
            _pendingQuestions.value = current.drop(1)
        }
    }

    /**
     * Dismiss a specific question request by ID.
     * Used when handling question actions from notifications.
     */
    fun dismissQuestion(questionId: String) {
        val current = _pendingQuestions.value
        _pendingQuestions.value = current.filter { it.id != questionId }
    }
    
    /**
     * Get the current (first) pending question for external handling.
     */
    fun getCurrentQuestion(): QuestionRequest? = _pendingQuestions.value.firstOrNull()

    /**
     * Handle incoming SSE events.
     * Persists relevant events to local database for offline-first support.
     * This is internal so it can be called by bridge adapters or state coordinators.
     *
     * NOTE: This method is called from BOTH handleBridgeEvent() and the SSE collect
     * loop. Both callers perform isDuplicateEvent() before invoking handleEvent(),
     * so dedup is handled upstream.
     */
    internal suspend fun handleEvent(event: ServerEvent) = withContext(Dispatchers.IO) {
        // Capture inter-event arrival time for quality tracking.
        // Updated for both SSE and bridge paths so latency calculations
        // don't use a stale/zero lastEventTime value.
        val now = System.currentTimeMillis()
        val interEventLatencyMs = now - lastEventTime
        lastEventTime = now
        Napier.v("Dispatching event: ${event.type}")

        // Early-exit for events from non-monitored sessions
        // OpenCode sends ALL events for ALL sessions—filter client-side
        if (event !is ServerEvent.Connected &&
            event !is ServerEvent.Heartbeat &&
            event !is ServerEvent.Log &&
            event !is ServerEvent.InstallationUpdateAvailable &&
            event !is ServerEvent.ServerInstanceDisposed &&
            event !is ServerEvent.ProjectUpdated) {
            val sessionId = when (event) {
                is ServerEvent.MessageUpdated -> event.properties.info.sessionID
                is ServerEvent.MessagePartUpdated -> event.properties.part.sessionID
                is ServerEvent.MessagePartDelta -> event.properties.sessionID
                is ServerEvent.SessionUpdated -> event.properties.info.id
                is ServerEvent.SessionDeleted -> event.properties.info.id
                is ServerEvent.SessionError -> event.properties.sessionID
                is ServerEvent.AgentStatus -> event.properties.sessionID
                else -> null
            }
            if (sessionId != null && !monitoredSessionIds.value.contains(sessionId)) {
                Napier.v("[EventStream] Skipping event for non-monitored session: $sessionId (${event.type})")
                return@withContext
            }
        }

        reduceChatTurn(event)
        
        when (event) {
            is ServerEvent.Connected -> {
                _connectionStatus.value = ConnectionStatus.Connected(
                    AppInfo(version = event.properties.version, initialized = true)
                )
                Napier.i("SSE connected: ${event.properties.status}")
            }
            
            is ServerEvent.SessionUpdated -> {
                Napier.d("Session updated: ${event.properties.info.id}")
            }
            
            is ServerEvent.SessionDeleted -> {
                Napier.d("Session deleted: ${event.properties.info.id}")
            }
            
            is ServerEvent.SessionIdle -> {
                val sessionId = event.properties.sessionID
                Napier.d("Session idle: $sessionId")
            }
            
            is ServerEvent.SessionError -> {
                event.properties.sessionID?.let { sessionId ->
                    if (monitoredSessionIds.value.contains(sessionId)) {
                        notificationTracker?.showAgentErrorNotification(
                            sessionId = sessionId,
                            errorMessage = event.properties.error?.message ?: "Unknown error"
                        )
                    }
                }
                Napier.e("Session error: ${event.properties.error?.message}")
            }
            
            is ServerEvent.MessagePartUpdated -> {
                val part = event.properties.part
                val delta = event.properties.delta

                // Record inter-event latency for quality tracking
                qualityTracker.recordLatency(interEventLatencyMs)

                // DIAGNOSTIC: Log all MessagePartUpdated events for debugging
                Napier.v(">>> MessagePartUpdated: type=${part.type}, hasDelta=${delta != null}, deltaLen=${delta?.length ?: 0}, textLen=${part.text?.length ?: 0}, sessionID=${part.sessionID}")

                // Insert initial streaming message to LocalCache if it doesn't exist yet
                if (localCache != null && streamingMessagesInserted.putIfAbsent(part.messageID, true) == null) {
                    try {
                        val placeholder = Message(
                            id = part.messageID,
                            sessionId = part.sessionID,
                            role = com.mocca.app.domain.model.MessageRole.ASSISTANT,
                            parts = emptyList(),
                            createdAt = System.currentTimeMillis(),
                            isStreaming = true
                        )
                        localCache.insertMessages(listOf(placeholder))
                        Napier.d("Inserted placeholder streaming message: ${part.messageID}")
                    } catch (e: Exception) {
                        Napier.w("Could not insert placeholder message: ${e.message}")
                    }
                }

                // Persist reasoning/thinking delta to LocalCache (single-owner path)
                if ((part.type == "thinking" || part.type == "reasoning") && delta != null && localCache != null) {
                    repositoryScope.launch {
                        try {
                            localCache.updateMessagePart(
                                messageId = part.messageID,
                                partId = part.id,
                                partType = part.type,
                                delta = delta
                            )
                        } catch (e: Exception) {
                            Napier.w("Failed to persist reasoning delta", e)
                        }
                    }
                }

                // Persist streaming text delta to LocalCache (throttled)
                if (part.type == "text" && delta != null && localCache != null) {
                    pendingDbDelta += delta
                    val now = System.currentTimeMillis()
                    if (now - lastDbWriteTime > dbWriteThrottleMs) {
                        val deltaToWrite = pendingDbDelta
                        pendingDbDelta = ""
                        lastDbWriteTime = now

                        // Write in background to avoid blocking the stream
                        dbWriteJob?.cancel()
                        dbWriteJob = repositoryScope.launch {
                            localCache.updateMessagePart(
                                messageId = part.messageID,
                                partId = part.id,
                                partType = part.type,
                                delta = deltaToWrite
                            )
                        }
                    } else {
                        // Schedule a delayed write for the remainder if no new chunks arrive soon
                        dbWriteJob?.cancel()
                        dbWriteJob = repositoryScope.launch {
                            delay(dbWriteThrottleMs * 2)
                            if (pendingDbDelta.isNotEmpty()) {
                                val deltaToWrite = pendingDbDelta
                                pendingDbDelta = ""
                                localCache.updateMessagePart(
                                    messageId = part.messageID,
                                    partId = part.id,
                                    partType = part.type,
                                    delta = deltaToWrite
                                )
                            }
                        }
                    }
                }

                // Tool execution state changes
                if (part.type == "tool" && part.state != null) {
                    Napier.d("Tool ${part.tool}: ${part.state.status}")
                }
            }
            
            is ServerEvent.MessageUpdated -> {
                val messageInfo = event.properties.info
                Napier.d("[EventStream] Message updated: ${messageInfo.id}")
            }
            
            is ServerEvent.MessageRemoved -> {
                val messageId = event.properties.messageID
                Napier.d("Message removed: $messageId")
            }
            
            is ServerEvent.MessagePartRemoved -> {
                Napier.d("Message part removed: ${event.properties.partID}")
            }
            
            is ServerEvent.PermissionUpdated -> {
                val permission = PermissionRequest.fromLegacyEvent(event)
                if (monitoredSessionIds.value.contains(permission.sessionId)) {
                    addPendingPermission(permission)
                    Napier.i("Permission requested (legacy): ${event.properties.title}")
                    notificationTracker?.showPermissionNotification(
                        sessionId = permission.sessionId,
                        permissionId = permission.id,
                        title = "Permission Requested",
                        description = event.properties.title
                    )
                }
            }
            
            is ServerEvent.PermissionAsked -> {
                val permission = PermissionRequest.fromEvent(event)
                if (monitoredSessionIds.value.contains(permission.sessionId)) {
                    addPendingPermission(permission)
                    Napier.i("Permission requested: ${permission.permission} for ${permission.patterns}")
                    notificationTracker?.showPermissionNotification(
                        sessionId = permission.sessionId,
                        permissionId = permission.id,
                        title = "Agent Permission Required",
                        description = "Tool access requested: ${permission.permission}"
                    )
                }
            }
            
            is ServerEvent.PermissionReplied -> {
                removePendingPermission(event.properties.requestID)
                Napier.i("Permission replied: ${event.properties.reply}")
                notificationTracker?.dismissPermissionNotification(event.properties.requestID)
            }
            
            is ServerEvent.QuestionAsked -> {
                val question = QuestionRequest.fromEvent(event)
                if (monitoredSessionIds.value.contains(question.sessionId)) {
                    addPendingQuestion(question)
                    Napier.i("Question requested: ${question.questions.size} questions")
                    notificationTracker?.showQuestionNotification(
                        sessionId = question.sessionId,
                        questionId = question.id,
                        question = question.questions.firstOrNull()?.question ?: "Agent has a question"
                    )
                }
            }
            
            is ServerEvent.QuestionReplied -> {
                removePendingQuestion(event.properties.requestID)
                Napier.i("Question replied: ${event.properties.answers.size} answers")
                notificationTracker?.dismissQuestionNotification(event.properties.requestID)
            }
            
            is ServerEvent.FileEdited -> {
                Napier.d("File edited: ${event.properties.file}")
            }
            
            is ServerEvent.FileWatcherUpdated -> {
                Napier.d("File watcher: ${event.properties.event} - ${event.properties.file}")
            }
            
            is ServerEvent.InstallationUpdated -> {
                Napier.i("OpenCode updated to: ${event.properties.version}")
                // Trigger callback for cache invalidation - this is critical for staying in sync
                onInstallationUpdated?.invoke()
            }
            
            is ServerEvent.LspDiagnostics -> {
                Napier.d("LSP diagnostics: ${event.properties.path}")
            }
            
            is ServerEvent.Log -> {
                val level = event.properties.level
                val message = event.properties.message
                when (level) {
                    "error" -> Napier.e("Server log: $message")
                    "warn" -> Napier.w("Server log: $message")
                    "debug" -> Napier.d("Server log: $message")
                    else -> Napier.i("Server log: $message")
                }
            }
            
            is ServerEvent.AgentStatus -> {
                val status = event.properties.statusString()
                val agentName = event.properties.agentName
                val sessionId = event.properties.sessionID
                Napier.i("Agent $agentName: $status${event.properties.message?.let { " - $it" } ?: ""}")

                // Track agent running state for button control.
                // Thinking visibility is derived from ChatTurnReducer part state, NOT from
                // agent status events, to prevent non-reasoning models from showing fake
                // thinking indicators.
                if (sessionId == activeSessionId || monitoredSessionIds.value.contains(sessionId)) {
                    when (status) {
                        "starting", "running" -> {
                            _isAgentRunning.value = true
                            _runningAgentName.value = agentName
                        }
                        "completed", "error" -> {
                            // Only clear if this is the currently tracked agent
                            if (_runningAgentName.value == agentName) {
                                _isAgentRunning.value = false
                                _runningAgentName.value = null
                            }
                        }
                    }
                }

                // Show completion/error notifications for monitored sessions
                if (monitoredSessionIds.value.contains(event.properties.sessionID)) {
                    if (status == "completed") {
                        notificationTracker?.showAgentFinishedNotification(
                            sessionId = event.properties.sessionID,
                            sessionTitle = "Agent $agentName task completed"
                        )
                    } else if (status == "error") {
                        notificationTracker?.showAgentErrorNotification(
                            sessionId = event.properties.sessionID,
                            errorMessage = event.properties.message ?: "Agent $agentName encountered an error"
                        )
                    }
                }
            }
            
            is ServerEvent.Heartbeat -> {
                Napier.v("Heartbeat received")
            }
            // New event types - log and ignore
            is ServerEvent.SessionCreated -> Napier.d("Session created: ${event.properties.info.id}")
            is ServerEvent.SessionStatus -> Napier.d("Session status: ${event.properties.sessionID}")
            is ServerEvent.SessionDiff -> Napier.d("Session diff: ${event.properties.sessionID}")
            is ServerEvent.SessionCompacted -> Napier.d("Session compacted: ${event.properties.sessionID}")
            is ServerEvent.TodoUpdated -> {
                Napier.d("Todo updated for session ${event.properties.sessionID}")
                // V2: todos array (full replacement)
                if (event.properties.todos.isNotEmpty()) {
                    val mappedTodos = event.properties.todos.map { info ->
                        Todo(
                            content = info.content,
                            status = runCatching { TodoStatus.valueOf(info.status.uppercase()) }
                                .getOrDefault(TodoStatus.PENDING),
                            priority = runCatching { TodoPriority.valueOf(info.priority.uppercase()) }
                                .getOrDefault(TodoPriority.MEDIUM)
                        )
                    }
                    localCache?.insertSessionTodos(event.properties.sessionID, mappedTodos)
                }
            }
            is ServerEvent.QuestionRejected -> Napier.d("Question rejected: ${event.properties.requestID}")
            is ServerEvent.MessagePartDelta -> {
                handleMessagePartDelta(event)
            }
            is ServerEvent.PtyCreated -> Napier.d("PTY created: ${event.properties.ptyID}")
            is ServerEvent.PtyUpdated -> Napier.d("PTY updated")
            is ServerEvent.PtyExited -> Napier.d("PTY exited")
            is ServerEvent.PtyDeleted -> Napier.d("PTY deleted")
            is ServerEvent.ProjectUpdated -> Napier.d("Project updated")
            is ServerEvent.VcsBranchUpdated -> Napier.d("VCS branch updated")
            is ServerEvent.FileUpdated -> Napier.d("File updated: ${event.properties.path}")
            is ServerEvent.LspUpdated -> Napier.d("LSP updated")
            is ServerEvent.McpToolsChanged -> Napier.d("MCP tools changed")
            is ServerEvent.McpBrowserOpenFailed -> Napier.d("MCP browser open failed")
            is ServerEvent.WorktreeReady -> Napier.d("Worktree ready: ${event.properties.id}")
            is ServerEvent.WorktreeFailed -> Napier.d("Worktree failed: ${event.properties.id}")
            is ServerEvent.InstallationUpdateAvailable -> Napier.i("Installation update available: ${event.properties.version}")
            is ServerEvent.ServerInstanceDisposed -> Napier.i("Server instance disposed")
            is ServerEvent.GlobalDisposed -> Napier.i("Global disposed")
            
            is ServerEvent.Unknown -> {
                Napier.w("Unknown event type: ${event.type}")
            }
        }

        syncCompatibilityMirrors()
        _events.emit(event)
    }

    private fun reduceChatTurn(event: ServerEvent) {
        _chatTurnState.value = ChatTurnReducer.reduce(_chatTurnState.value, event)
    }

    /**
     * Sync compatibility mirrors (streamingText, isThinking, thinkingContent)
     * from the canonical ChatTurnReducer/part state.
     *
     * These are read-only mirrors for legacy UI consumers.
     * The canonical source of truth is ChatTurnState.messagesById and their parts.
     * Do NOT mutate these flows independently; always derive from chatTurnState.
     */
    private fun syncCompatibilityMirrors() {
        val sessionId = activeSessionId ?: return
        val latestMessage = _chatTurnState.value.latestAssistantMessage(sessionId)

        if (latestMessage != null && latestMessage.isStreaming) {
            // Streaming text: latest text part content
            val textParts = latestMessage.parts.filterIsInstance<MessagePart.Text>()
            val latestText = textParts.lastOrNull()
            _streamingText.value = latestText?.text?.takeLast(NetworkConfig.STREAMING_TEXT_MAX_SIZE) ?: ""

            // Thinking state: derived from actual reasoning/thinking parts only
            val reasoningParts = latestMessage.parts.filter {
                it is MessagePart.Reasoning || it is MessagePart.Thinking
            }
            if (reasoningParts.isNotEmpty()) {
                _isThinking.value = true
                val content = reasoningParts.joinToString("") { part ->
                    when (part) {
                        is MessagePart.Reasoning -> part.content
                        is MessagePart.Thinking -> part.content
                        else -> ""
                    }
                }
                _thinkingContent.value = content.takeLast(NetworkConfig.STREAMING_TEXT_MAX_SIZE)
                if (_thinkingStartTime.value == null) {
                    _thinkingStartTime.value = System.currentTimeMillis()
                }
            } else {
                _isThinking.value = false
                _thinkingContent.value = ""
                _thinkingStartTime.value = null
            }
        } else {
            // No active streaming message - clear mirrors
            _streamingText.value = ""
            _isThinking.value = false
            _thinkingContent.value = ""
            _thinkingStartTime.value = null
        }
    }

    private suspend fun handleMessagePartDelta(event: ServerEvent.MessagePartDelta) {
        val props = event.properties
        if (props.field != "text") {
            Napier.v("Ignoring non-text MessagePartDelta field=${props.field}")
            return
        }

        val part = _chatTurnState.value.messagesById[props.messageID]
            ?.parts
            ?.firstOrNull { it.openCodePartId == props.partID }

        // Compatibility mirrors (streamingText, isThinking, thinkingContent) are synced
        // from chatTurnState by syncCompatibilityMirrors() after reduceChatTurn().
        // Do NOT mutate them here independently.

        localCache?.updateMessagePart(
            messageId = props.messageID,
            partId = props.partID,
            partType = part.openCodePartType,
            delta = props.delta
        )
        Napier.v("MessagePartDelta applied: ${props.messageID}/${props.partID}")
    }
    
    /**
     * Add a permission request to the pending list.
     */
    private fun addPendingPermission(permission: PermissionRequest) {
        val current = _pendingPermissions.value.toMutableList()
        // Remove existing request with same ID (in case of update)
        current.removeAll { it.id == permission.id }
        current.add(permission)
        _pendingPermissions.value = current
    }
    
    /**
     * Remove a permission request from the pending list.
     */
    private fun removePendingPermission(requestId: String) {
        val current = _pendingPermissions.value.toMutableList()
        current.removeAll { it.id == requestId }
        _pendingPermissions.value = current
    }
    
    /**
     * Add a question request to the pending list.
     */
    private fun addPendingQuestion(question: QuestionRequest) {
        val current = _pendingQuestions.value.toMutableList()
        // Remove existing request with same ID (in case of update)
        current.removeAll { it.id == question.id }
        current.add(question)
        _pendingQuestions.value = current
    }
    
    /**
     * Remove a question request from the pending list.
     */
    private fun removePendingQuestion(requestId: String) {
        val current = _pendingQuestions.value.toMutableList()
        current.removeAll { it.id == requestId }
        _pendingQuestions.value = current
    }

    /**
     * Filter events for monitored sessions.
     */
    fun eventsForMonitoredSessions(): Flow<ServerEvent> {
        return events.filter { event ->
            val sessionId = when (event) {
                is ServerEvent.SessionUpdated -> event.properties.info.id
                is ServerEvent.SessionDeleted -> event.properties.info.id
                is ServerEvent.SessionIdle -> event.properties.sessionID
                is ServerEvent.SessionError -> event.properties.sessionID
                is ServerEvent.MessageUpdated -> event.properties.info.sessionID
                is ServerEvent.MessageRemoved -> event.properties.sessionID
                is ServerEvent.MessagePartUpdated -> event.properties.part.sessionID
                is ServerEvent.MessagePartDelta -> event.properties.sessionID
                is ServerEvent.PermissionUpdated -> event.properties.sessionID
                is ServerEvent.PermissionAsked -> event.properties.sessionID
                is ServerEvent.PermissionReplied -> event.properties.sessionID
                is ServerEvent.QuestionAsked -> event.properties.sessionID
                is ServerEvent.QuestionReplied -> event.properties.sessionID
                else -> null
            }
            sessionId == null || monitoredSessionIds.value.contains(sessionId)
        }
    }

    /**
     * Filter events for a specific session.
     */
    fun eventsForSession(sessionId: String): Flow<ServerEvent> {
        return events.filter { event ->
            when (event) {
                is ServerEvent.SessionUpdated -> event.properties.info.id == sessionId
                is ServerEvent.SessionDeleted -> event.properties.info.id == sessionId
                is ServerEvent.SessionIdle -> event.properties.sessionID == sessionId
                is ServerEvent.SessionError -> event.properties.sessionID == sessionId
                is ServerEvent.MessageUpdated -> event.properties.info.sessionID == sessionId
                is ServerEvent.MessageRemoved -> event.properties.sessionID == sessionId
                is ServerEvent.MessagePartUpdated -> event.properties.part.sessionID == sessionId
                is ServerEvent.MessagePartDelta -> event.properties.sessionID == sessionId
                is ServerEvent.MessagePartRemoved -> true
                is ServerEvent.PermissionUpdated -> event.properties.sessionID == sessionId
                is ServerEvent.PermissionAsked -> event.properties.sessionID == sessionId
                is ServerEvent.PermissionReplied -> event.properties.sessionID == sessionId
                is ServerEvent.QuestionAsked -> event.properties.sessionID == sessionId
                is ServerEvent.QuestionReplied -> event.properties.sessionID == sessionId
                else -> true
            }
        }
    }
}

private val MessagePart.openCodePartId: String?
    get() = when (this) {
        is MessagePart.Text -> id
        is MessagePart.Reasoning -> id
        is MessagePart.Thinking -> id
        is MessagePart.ToolInvocation -> id
        is MessagePart.ToolResult -> id
        is MessagePart.File -> null
        is MessagePart.SubTask -> sessionId
        is MessagePart.Snapshot -> id
        is MessagePart.Patch -> id
        is MessagePart.AgentDelegate -> id
        is MessagePart.Retry -> id
        is MessagePart.Compaction -> id
    }

private val MessagePart?.openCodePartType: String?
    get() = when (this) {
        is MessagePart.Text -> "text"
        is MessagePart.Reasoning -> "reasoning"
        is MessagePart.Thinking -> "thinking"
        is MessagePart.ToolInvocation -> "tool"
        is MessagePart.ToolResult -> "tool"
        is MessagePart.File -> "file"
        is MessagePart.SubTask -> "subtask"
        is MessagePart.Snapshot -> "snapshot"
        is MessagePart.Patch -> "patch"
        is MessagePart.AgentDelegate -> "agent"
        is MessagePart.Retry -> "retry"
        is MessagePart.Compaction -> "compaction"
        null -> null
    }
