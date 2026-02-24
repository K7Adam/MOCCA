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
import kotlinx.coroutines.sync.withLock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for managing Server-Sent Events streaming.
 * Includes automatic reconnection, network state awareness, and DB persistence.
 * Connection lifecycle (HttpClient management) is handled by ConnectionManager.
 * 
 * IMPROVEMENTS:
 * - Background/foreground lifecycle awareness
 * - Adaptive heartbeat based on connection quality
 * - Thread-safe streaming text with Mutex
 * - Event deduplication with TTL
 * - Pause/resume for background optimization
 */
class EventStreamRepository(
    private val sseClient: MoccaSseClient,
    private val networkObserver: NetworkObserver? = null,
    private val apiClient: MoccaApiClient? = null,
    private val appLifecycleObserver: AppLifecycleObserver? = null,
    private val notificationTracker: NotificationTracker? = null
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var connectionJob: Job? = null
    private var networkObserverJob: Job? = null
    private var lifecycleObserverJob: Job? = null
    private var backgroundPauseJob: Job? = null
    private var permissionActionJob: Job? = null
    
    // Removed 'scope' variable as we use repositoryScope now
    private var autoReconnect: Boolean = true
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = NetworkConfig.SSE_MAX_RECONNECT_ATTEMPTS
    
    // Connection quality tracker for adaptive behavior
    private val qualityTracker = ConnectionQualityTracker()
    
    // Pause state for background optimization
    private var isPaused = false
    private var wasConnectedBeforePause = false
    
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
    
    // IMPROVED: Increased buffer capacity, changed overflow strategy to SUSPEND
    // REPLAY=1 is CRITICAL: Ensures the last event (like SessionIdle) is not missed if collector reconnects
    private val _events = MutableSharedFlow<ServerEvent>(
        replay = 1,
        extraBufferCapacity = 2048,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.SUSPEND
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
        .stateIn(CoroutineScope(SupervisorJob()), SharingStarted.Eagerly, null)
    
    private val _pendingQuestions = MutableStateFlow<List<QuestionRequest>>(emptyList())
    val pendingQuestions: StateFlow<List<QuestionRequest>> = _pendingQuestions.asStateFlow()
    
    // Backwards-compatible accessor for first pending question
    val pendingQuestion: StateFlow<QuestionRequest?> = _pendingQuestions
        .map { it.firstOrNull() }
        .stateIn(CoroutineScope(SupervisorJob()), SharingStarted.Eagerly, null)
    
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
        if (connectionJob?.isActive == true) {
            if (activeSessionId == sessionId) {
                Napier.i("SSE already connected to session: $sessionId")
                return // Already connected to this session
            }
            Napier.i("Switching SSE session from $activeSessionId to $sessionId")
            // Clear streaming state when switching sessions
            setActiveSession(sessionId)
        } else {
            setActiveSession(sessionId)
        }
        
        // Add session to monitored set (don't replace - preserve other monitored sessions)
        if (sessionId != null) {
            monitoredSessionIds.update { it + sessionId }
        }
        autoReconnect = true
        reconnectAttempts = 0
        isPaused = false
        
        // Only start a new connection if one isn't active
        if (connectionJob?.isActive != true) {
            _connectionStatus.value = ConnectionStatus.Connecting
            startConnection()
            startNetworkObserver()
            startLifecycleObserver()
            startPermissionActionObserver()
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
                Napier.i("[EventStream] Received permission action from notification: ${action.permissionId}, approved=${action.isApproved}")
                
                // Call the API to respond to the permission
                apiClient?.let { client ->
                    val result = client.respondToPermission(
                        sessionId = action.sessionId,
                        permissionId = action.permissionId,
                        allow = action.isApproved,
                        remember = false
                    )
                    
                    result.fold(
                        onSuccess = {
                            Napier.i("[EventStream] Permission ${action.permissionId} ${if (action.isApproved) "approved" else "denied"} successfully")
                            // Dismiss the pending permission from our state
                            dismissPermission(action.permissionId)
                        },
                        onFailure = { error ->
                            Napier.e("[EventStream] Failed to respond to permission ${action.permissionId}", error)
                        }
                    )
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

    private fun startConnection() {
        connectionJob?.cancel()
        heartbeatJob?.cancel()
        
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
     */
    private fun extractEventId(event: ServerEvent): String? {
        return when (event) {
            is ServerEvent.MessageUpdated -> "msg-${event.properties.info.id}"
            is ServerEvent.MessagePartUpdated -> "part-${event.properties.part.id}-${event.properties.part.text?.hashCode()}"
            is ServerEvent.SessionUpdated -> "session-${event.properties.info.id}"
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
        if (!autoReconnect || reconnectAttempts >= maxReconnectAttempts) {
            Napier.w("Not reconnecting: autoReconnect=$autoReconnect, attempts=$reconnectAttempts/$maxReconnectAttempts")
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
            
            if (autoReconnect && !isPaused) {
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
        autoReconnect = false
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
        autoReconnect = true
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
     * Get the current (first) pending question for external handling.
     */
    fun getCurrentQuestion(): QuestionRequest? = _pendingQuestions.value.firstOrNull()

    /**
     * Handle incoming SSE events.
     * Persists relevant events to local database for offline-first support.
     */
    private suspend fun handleEvent(event: ServerEvent) = withContext(Dispatchers.IO) {
        Napier.v("Dispatching event: ${event.type}")
        _events.emit(event)
        
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
                if (sessionId == activeSessionId) {
                    // IMPROVED: Thread-safe streaming text clear
                    streamingTextMutex.withLock {
                        _streamingText.value = ""
                    }
                    // Clear thinking state on session idle
                    _isThinking.value = false
                    _thinkingContent.value = ""
                    _thinkingStartTime.value = null
                }
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
                
                // Record latency for quality tracking
                qualityTracker.recordLatency(System.currentTimeMillis() - lastEventTime)
                
                // DIAGNOSTIC: Log all MessagePartUpdated events for debugging
                Napier.i(">>> MessagePartUpdated: type=${part.type}, hasDelta=${delta != null}, deltaLen=${delta?.length ?: 0}, textLen=${part.text?.length ?: 0}, sessionID=${part.sessionID}")
                
                // Handle thinking state for extended reasoning models (Claude/o1)
                if (part.type == "thinking") {
                    if (monitoredSessionIds.value.contains(part.sessionID)) {
                        if (part.sessionID == activeSessionId) {
                            _isThinking.value = true
                            
                            // IMPROVED: Thread-safe thinking content update
                            streamingTextMutex.withLock {
                                // Initialize thinking content if empty, or append delta
                                if (_thinkingContent.value.isEmpty() && !part.text.isNullOrEmpty()) {
                                    _thinkingContent.value = part.text
                                } else if (delta != null) {
                                    _thinkingContent.value += delta
                                } else if (!part.text.isNullOrEmpty()) {
                                    _thinkingContent.value = part.text
                                }
                            }
                            
                            if (_thinkingStartTime.value == null) {
                                _thinkingStartTime.value = System.currentTimeMillis()
                            }
                            Napier.i(">>> Thinking state updated")
                        }
                    }
                }
                
                // IMPROVED: Thread-safe streaming text handling
                if (part.type == "text") {
                    // Text part received means thinking is complete
                    if (_isThinking.value) {
                        _isThinking.value = false
                        _thinkingContent.value = ""
                        _thinkingStartTime.value = null
                    }
                    
                    if (monitoredSessionIds.value.contains(part.sessionID)) {
                        if (part.sessionID == activeSessionId) {
                            // IMPROVED: Thread-safe streaming text update
                            streamingTextMutex.withLock {
                                // Initialize streaming text if empty, or append delta
                                if (_streamingText.value.isEmpty() && !part.text.isNullOrEmpty()) {
                                    _streamingText.value = part.text
                                } else if (delta != null) {
                                    _streamingText.value += delta
                                } else if (!part.text.isNullOrEmpty()) {
                                    _streamingText.value = part.text
                                }
                                
                                // Limit streaming text size to prevent memory issues
                                if (_streamingText.value.length > NetworkConfig.STREAMING_TEXT_MAX_SIZE) {
                                    Napier.w("[EventStream] Streaming text exceeded max size, truncating")
                                    _streamingText.value = _streamingText.value.takeLast(NetworkConfig.STREAMING_TEXT_MAX_SIZE)
                                }
                            }
                            
                            Napier.i(">>> Streaming text updated: ...${_streamingText.value.takeLast(50)}")
                        } else {
                            Napier.w(">>> Session ID mismatch: event.sessionID=${part.sessionID}, activeSessionId=$activeSessionId")
                        }
                    } else {
                        Napier.w(">>> Session ${part.sessionID} NOT in monitored set: ${monitoredSessionIds.value}")
                    }
                }
                // Tool execution state changes
                if (part.type == "tool" && part.state != null) {
                    Napier.d("Tool ${part.tool}: ${part.state.status}")
                }
            }
            
            is ServerEvent.MessageUpdated -> {
                val messageInfo = event.properties.info
                if (messageInfo.sessionID == activeSessionId) {
                    // Message complete - clear streaming text and thinking state
                    streamingTextMutex.withLock {
                        _streamingText.value = ""
                    }
                    _isThinking.value = false
                    _thinkingContent.value = ""
                    _thinkingStartTime.value = null
                }
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
                val status = event.properties.status
                val agentName = event.properties.agentName
                val sessionId = event.properties.sessionID
                Napier.i("Agent $agentName: $status${event.properties.message?.let { " - $it" } ?: ""}")
                
                // Track agent running state for button control and thinking indicator
                // Only track if this event is for the active session or a monitored session
                if (sessionId == activeSessionId || monitoredSessionIds.value.contains(sessionId)) {
                    when (status) {
                        "starting", "running" -> {
                            _isAgentRunning.value = true
                            _runningAgentName.value = agentName
                            // Also set isThinking to true for the thinking indicator
                            // (this is independent of "thinking" type message parts)
                            _isThinking.value = true
                            // Also set thinking start time if not already set
                            if (_thinkingStartTime.value == null) {
                                _thinkingStartTime.value = System.currentTimeMillis()
                            }
                        }
                        "completed", "error" -> {
                            // Only clear if this is the currently tracked agent
                            if (_runningAgentName.value == agentName) {
                                _isAgentRunning.value = false
                                _runningAgentName.value = null
                                // Clear thinking state when agent completes
                                _isThinking.value = false
                                _thinkingContent.value = ""
                                _thinkingStartTime.value = null
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
                // Heartbeat events are used to keep the connection alive
                // No action needed, just acknowledge receipt
                Napier.v("Heartbeat received")
            }
            
            is ServerEvent.Unknown -> {
                Napier.w("Unknown event type: ${event.type}")
            }
        }
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
