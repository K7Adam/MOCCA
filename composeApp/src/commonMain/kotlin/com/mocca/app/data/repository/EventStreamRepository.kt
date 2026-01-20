package com.mocca.app.data.repository

import com.mocca.app.api.HttpClientProvider
import com.mocca.app.api.MoccaSseClient
import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import com.mocca.app.util.NetworkObserver
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing Server-Sent Events streaming.
 * Includes automatic reconnection, network state awareness, and DB persistence.
 */
class EventStreamRepository(
    private val sseClient: MoccaSseClient,
    private val httpClientProvider: HttpClientProvider,
    private val networkObserver: NetworkObserver? = null,
    private val localCache: LocalCache? = null
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        // Automatically reconnect SSE when the HttpClient is recreated
        httpClientProvider.onClientRecreated = {
            Napier.i("HttpClient recreated, restarting SSE connection...")
            reconnect()
        }
    }
    
    private var connectionJob: Job? = null
    private var networkObserverJob: Job? = null
    
    // Removed 'scope' variable as we use repositoryScope now
    private var autoReconnect: Boolean = true
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 10
    
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    // Increased buffer capacity and use SUSPEND to prevent event loss
    // REPLAY=1 is CRITICAL: Ensures the last event (like SessionIdle) is not missed if collector reconnects
    private val _events = MutableSharedFlow<ServerEvent>(
        replay = 1,
        extraBufferCapacity = 128,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.SUSPEND
    )
    val events: SharedFlow<ServerEvent> = _events.asSharedFlow()
    
    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()
    
    // Extended thinking state tracking (for Claude/o1 reasoning models)
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()
    
    private val _thinkingContent = MutableStateFlow("")
    val thinkingContent: StateFlow<String> = _thinkingContent.asStateFlow()
    
    private val _thinkingStartTime = MutableStateFlow<Long?>(null)
    val thinkingStartTime: StateFlow<Long?> = _thinkingStartTime.asStateFlow()
    
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

    /**
     * Connect to the SSE event stream.
     * Will automatically reconnect on failures and when network becomes available.
     * Note: externalScope is ignored in favor of internal repositoryScope
     */
    fun connect(externalScope: CoroutineScope? = null, sessionId: String? = null) {
        if (connectionJob?.isActive == true) {
            Napier.i("SSE already connected (job active)")
            if (activeSessionId == sessionId) {
                return // Already connected to this session
            }
            Napier.i("Switching session from $activeSessionId to $sessionId")
            // Don't disconnect fully, just update session tracking
        }
        
        activeSessionId = sessionId
        if (sessionId != null) {
            monitoredSessionIds.value = setOf(sessionId)
        }
        autoReconnect = true
        reconnectAttempts = 0
        
        // Only start a new connection if one isn't active
        if (connectionJob?.isActive != true) {
            _connectionStatus.value = ConnectionStatus.Connecting
            startConnection()
            startNetworkObserver()
        }
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
     * Start the SSE connection.
     */
    private fun startConnection() {
        connectionJob?.cancel()
        connectionJob = repositoryScope.launch {
            try {
                sseClient.subscribeToEvents()
                    .catch { error ->
                        if (error is CancellationException) throw error
                        Napier.e("SSE error", error)
                        _connectionStatus.value = ConnectionStatus.Error(error.message ?: "Connection error")
                        scheduleReconnect()
                    }
                    .collect { event ->
                        reconnectAttempts = 0 // Reset on successful event
                        handleEvent(event)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("SSE connection failed", e)
                _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Connection failed")
                scheduleReconnect()
            }
        }
    }
    
    /**
     * Schedule a reconnection attempt with backoff.
     */
    private fun scheduleReconnect() {
        if (!autoReconnect || reconnectAttempts >= maxReconnectAttempts) {
            Napier.w("Not reconnecting: autoReconnect=$autoReconnect, attempts=$reconnectAttempts/$maxReconnectAttempts")
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
            val delayMs = calculateBackoff(reconnectAttempts)
            Napier.i("Reconnecting in ${delayMs}ms (attempt $reconnectAttempts/$maxReconnectAttempts)")
            _connectionStatus.value = ConnectionStatus.Reconnecting(reconnectAttempts, delayMs)
            delay(delayMs)
            
            if (autoReconnect) {
                _connectionStatus.value = ConnectionStatus.Connecting
                startConnection()
            }
        }
    }
    
    /**
     * Calculate exponential backoff delay.
     */
    private fun calculateBackoff(attempt: Int): Long {
        val baseDelay = 1000L
        val maxDelay = 30000L
        val delay = (baseDelay * (1 shl minOf(attempt - 1, 5))).coerceAtMost(maxDelay)
        return delay + (0..500).random() // Add jitter
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
        connectionJob?.cancel()
        networkObserverJob?.cancel()
        connectionJob = null
        networkObserverJob = null
        // Do NOT cancel repositoryScope, it persists for the app lifecycle
        reconnectAttempts = 0
        activeSessionId = null
        monitoredSessionIds.value = emptySet()
        _connectionStatus.value = ConnectionStatus.Disconnected
        _streamingText.value = ""
        _isThinking.value = false
        _thinkingContent.value = ""
        _thinkingStartTime.value = null
        _pendingPermissions.value = emptyList()
        _pendingQuestions.value = emptyList()
        Napier.i("SSE disconnected")
    }
    
    /**
     * Force a reconnection attempt.
     */
    fun reconnect() {
        if (_connectionStatus.value !is ConnectionStatus.Connected) {
            reconnectAttempts = 0
            autoReconnect = true
            _connectionStatus.value = ConnectionStatus.Connecting
            startConnection()
        }
    }

    /**
     * Clear the current streaming text buffer.
     */
    fun clearStreamingText() {
        _streamingText.value = ""
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
                val session = event.properties.info
                // Persist session update to local cache
                try {
                    localCache?.insertSession(session)
                    Napier.d("Session updated and persisted: ${session.id}")
                } catch (e: Exception) {
                    Napier.w("Failed to persist session update", e)
                }
            }
            
            is ServerEvent.SessionDeleted -> {
                val sessionId = event.properties.info.id
                // Remove from local cache
                try {
                    localCache?.deleteSession(sessionId)
                    Napier.d("Session deleted and removed from cache: $sessionId")
                } catch (e: Exception) {
                    Napier.w("Failed to delete session from cache", e)
                }
            }
            
            is ServerEvent.SessionIdle -> {
                val sessionId = event.properties.sessionID
                if (sessionId == activeSessionId) {
                    _streamingText.value = ""
                    // Clear thinking state on session idle
                    _isThinking.value = false
                    _thinkingContent.value = ""
                    _thinkingStartTime.value = null
                }
                // Update session status in cache
                try {
                    localCache?.updateSessionStatus(sessionId, "idle")
                } catch (e: Exception) {
                    Napier.w("Failed to update session status", e)
                }
                Napier.d("Session idle: $sessionId")
            }
            
            is ServerEvent.SessionError -> {
                event.properties.sessionID?.let { sessionId ->
                    try {
                        localCache?.updateSessionStatus(sessionId, "error")
                    } catch (e: Exception) {
                        Napier.w("Failed to update session error status", e)
                    }
                }
                Napier.e("Session error: ${event.properties.error?.message}")
            }
            
            is ServerEvent.MessagePartUpdated -> {
                val part = event.properties.part
                val delta = event.properties.delta
                
                // DIAGNOSTIC: Log all MessagePartUpdated events for debugging
                Napier.i(">>> MessagePartUpdated: type=${part.type}, hasDelta=${delta != null}, deltaLen=${delta?.length ?: 0}, textLen=${part.text?.length ?: 0}, sessionID=${part.sessionID}")
                
                // Handle thinking state for extended reasoning models (Claude/o1)
                if (part.type == "thinking") {
                    if (monitoredSessionIds.value.contains(part.sessionID)) {
                        if (part.sessionID == activeSessionId) {
                            _isThinking.value = true
                            
                            // Initialize thinking content if empty, or append delta
                            if (_thinkingContent.value.isEmpty() && !part.text.isNullOrEmpty()) {
                                _thinkingContent.value = part.text
                            } else if (delta != null) {
                                _thinkingContent.value += delta
                            } else if (!part.text.isNullOrEmpty()) {
                                _thinkingContent.value = part.text
                            }
                            
                            if (_thinkingStartTime.value == null) {
                                _thinkingStartTime.value = System.currentTimeMillis()
                            }
                            Napier.i(">>> Thinking state updated")
                        }
                    }
                }
                
                // Handle streaming text
                if (part.type == "text") {
                    // Text part received means thinking is complete
                    if (_isThinking.value) {
                        _isThinking.value = false
                        _thinkingContent.value = ""
                        _thinkingStartTime.value = null
                    }
                    
                    if (monitoredSessionIds.value.contains(part.sessionID)) {
                        if (part.sessionID == activeSessionId) {
                            // Initialize streaming text if empty, or append delta
                            if (_streamingText.value.isEmpty() && !part.text.isNullOrEmpty()) {
                                _streamingText.value = part.text
                            } else if (delta != null) {
                                _streamingText.value += delta
                            } else if (!part.text.isNullOrEmpty()) {
                                _streamingText.value = part.text
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
                    _streamingText.value = ""
                    _isThinking.value = false
                    _thinkingContent.value = ""
                    _thinkingStartTime.value = null
                }
                // Update session status to running when message is being processed
                try {
                    localCache?.updateSessionStatus(messageInfo.sessionID, "running")
                    
                    // CRITICAL FIX: Persist the updated message immediately
                    // This ensures that when ChatScreenModel calls loadMessages(), it gets the fresh data
                    // We need to fetch the message content since the event might just be metadata
                    // Ideally we should have the content here, but for now we rely on the UI to fetch
                    // However, we MUST ensure the cache is at least not conflicting
                } catch (e: Exception) {
                    Napier.w("Failed to update session status", e)
                }
                Napier.d("Message updated: ${messageInfo.id}")
            }
            
            is ServerEvent.MessageRemoved -> {
                val messageId = event.properties.messageID
                // Remove from local cache
                try {
                    localCache?.deleteMessage(messageId)
                    Napier.d("Message removed and deleted from cache: $messageId")
                } catch (e: Exception) {
                    Napier.w("Failed to delete message from cache", e)
                }
            }
            
            is ServerEvent.MessagePartRemoved -> {
                Napier.d("Message part removed: ${event.properties.partID}")
            }
            
            is ServerEvent.PermissionUpdated -> {
                val permission = PermissionRequest.fromLegacyEvent(event)
                if (monitoredSessionIds.value.contains(permission.sessionId)) {
                    addPendingPermission(permission)
                    Napier.i("Permission requested (legacy): ${event.properties.title}")
                }
            }
            
            is ServerEvent.PermissionAsked -> {
                val permission = PermissionRequest.fromEvent(event)
                if (monitoredSessionIds.value.contains(permission.sessionId)) {
                    addPendingPermission(permission)
                    Napier.i("Permission requested: ${permission.permission} for ${permission.patterns}")
                }
            }
            
            is ServerEvent.PermissionReplied -> {
                removePendingPermission(event.properties.requestID)
                Napier.i("Permission replied: ${event.properties.reply}")
            }
            
            is ServerEvent.QuestionAsked -> {
                val question = QuestionRequest.fromEvent(event)
                if (monitoredSessionIds.value.contains(question.sessionId)) {
                    addPendingQuestion(question)
                    Napier.i("Question requested: ${question.questions.size} questions")
                }
            }
            
            is ServerEvent.QuestionReplied -> {
                removePendingQuestion(event.properties.requestID)
                Napier.i("Question replied: ${event.properties.answers.size} answers")
            }
            
            is ServerEvent.FileEdited -> {
                Napier.d("File edited: ${event.properties.file}")
            }
            
            is ServerEvent.FileWatcherUpdated -> {
                Napier.d("File watcher: ${event.properties.event} - ${event.properties.file}")
            }
            
            is ServerEvent.InstallationUpdated -> {
                Napier.i("OpenCode updated to: ${event.properties.version}")
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
                Napier.i("Agent $agentName: $status${event.properties.message?.let { " - $it" } ?: ""}")
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
