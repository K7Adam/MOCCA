package com.mocca.app.data.repository

import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import com.mocca.app.util.AppLifecycleObserver
import com.mocca.app.util.AppLifecycleState
import com.mocca.app.util.NetworkObserver
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Central coordinator for all application state.
 * 
 * This is the SINGLE SOURCE OF TRUTH for:
 * - SSE event dispatching to all state stores
 * - Active session tracking
 * - Connection state propagation
 * - State synchronization on connection restore
 * - Optimistic updates with rollback
 * 
 * Architecture:
 * ```
 * Server (SSE) → EventStreamRepository → StateCoordinator → State Stores → UI
 *                                         ↓
 *                                   LocalCache (persistence)
 * ```
 * 
 * PRINCIPLES:
 * 1. All events flow through StateCoordinator
 * 2. State stores subscribe to StateCoordinator, NOT directly to SSE
 * 3. Single source of truth for active session ID
 * 4. Automatic state reconciliation on connection restore
 * 5. Thread-safe with proper mutex protection
 */
class StateCoordinator(
    private val eventStreamRepository: EventStreamRepository,
    private val connectionManager: ConnectionManager,
    private val localCache: LocalCache,
    private val sessionRepository: SessionRepository,
    private val appLifecycleObserver: AppLifecycleObserver?,
    private val networkObserver: NetworkObserver?
) {
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ACTIVE SESSION TRACKING - Single source of truth
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val sessionMutex = Mutex()
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()
    
    private val _monitoredSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val monitoredSessionIds: StateFlow<Set<String>> = _monitoredSessionIds.asStateFlow()
    
    /**
     * Callback invoked when app resumes from background.
     * Used to trigger state sync.
     */
    var onAppResume: (() -> Unit)? = null
    
    /**
     * Callback invoked when a session becomes idle (message complete).
     * Used to trigger message refresh in state stores.
     */
    var onSessionIdle: ((sessionId: String) -> Unit)? = null
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONNECTION STATE - Unified from ConnectionManager
    // ═══════════════════════════════════════════════════════════════════════════════
    
    val connectionStatus: StateFlow<ConnectionStatus> = connectionManager.status
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STREAMING STATE - From EventStreamRepository
    // ═══════════════════════════════════════════════════════════════════════════════
    
    val streamingText: StateFlow<String> = eventStreamRepository.streamingText
    val isThinking: StateFlow<Boolean> = eventStreamRepository.isThinking
    val thinkingContent: StateFlow<String> = eventStreamRepository.thinkingContent
    val thinkingStartTime: StateFlow<Long?> = eventStreamRepository.thinkingStartTime
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PERMISSION/QUESTION STATE - From EventStreamRepository
    // ═══════════════════════════════════════════════════════════════════════════════
    
    val pendingPermission: StateFlow<PermissionRequest?> = eventStreamRepository.pendingPermission
    val pendingQuestion: StateFlow<QuestionRequest?> = eventStreamRepository.pendingQuestion
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EVENT BROADCAST - For state stores to subscribe
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _broadcastEvents = MutableSharedFlow<BroadcastEvent>(
        replay = 1,
        extraBufferCapacity = 256,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.SUSPEND
    )
    val broadcastEvents: SharedFlow<BroadcastEvent> = _broadcastEvents.asSharedFlow()
    
    /**
     * Get events filtered for monitored sessions.
     * This replaces direct observation of eventStreamRepository.eventsForMonitoredSessions().
     */
    fun eventsForMonitoredSessions(): Flow<ServerEvent> = broadcastEvents
        .filter { it is BroadcastEvent.ServerEvent }
        .map { (it as BroadcastEvent.ServerEvent).event }
        .filter { event ->
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
            sessionId == null || _monitoredSessionIds.value.contains(sessionId)
        }
    
    /**
     * Get events filtered for a specific session.
     */
    fun eventsForSession(sessionId: String): Flow<ServerEvent> = broadcastEvents
        .filter { it is BroadcastEvent.ServerEvent }
        .map { (it as BroadcastEvent.ServerEvent).event }
        .filter { event ->
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
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SESSION RUNNING STATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _runningSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val runningSessionIds: StateFlow<Set<String>> = _runningSessionIds.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SYNC STATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INTERNAL STATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private var isInitialized = false
    private var syncJob: Job? = null
    private var eventObserverJob: Job? = null
    private var connectionObserverJob: Job? = null
    private var lifecycleObserverJob: Job? = null
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    init {
        Napier.i("[StateCoordinator] Initializing...")
        
        // Wire up callbacks
        eventStreamRepository.onAppResume = { 
            Napier.i("[StateCoordinator] App resume callback - syncing state")
            syncFromServer() 
        }
        
        connectionManager.onConnectionEstablished = suspend { 
            Napier.i("[StateCoordinator] Connection established callback - starting and syncing")
            start()
            syncFromServer()
        }
        
        startObservingEvents()
        startObservingConnection()
        startObservingLifecycle()
    }
    
    /**
     * Start the coordinator.
     * Called automatically when connection is established.
     */
    fun start() {
        if (isInitialized) {
            Napier.v("[StateCoordinator] Already initialized")
            return
        }
        isInitialized = true
        Napier.i("[StateCoordinator] Started")
        
        // Sync if connected
        if (connectionStatus.value.isConnected) {
            syncFromServer()
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SESSION MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Set the active session.
     * This updates EventStreamRepository and all state stores.
     */
    suspend fun setActiveSession(sessionId: String?) {
        sessionMutex.withLock {
            val previousId = _activeSessionId.value
            if (previousId == sessionId) {
                Napier.v("[StateCoordinator] Session already active: $sessionId")
                return
            }
            
            Napier.i("[StateCoordinator] Setting active session: $previousId -> $sessionId")
            _activeSessionId.value = sessionId
            
            // Update monitored sessions
            if (sessionId != null) {
                _monitoredSessionIds.update { it + sessionId }
                // Connect SSE for this session
                eventStreamRepository.connect(coordinatorScope, sessionId)
                eventStreamRepository.monitorSession(sessionId)
            }
            
            // Broadcast session change
            _broadcastEvents.emit(BroadcastEvent.ActiveSessionChanged(sessionId))
        }
    }
    
    /**
     * Add a session to monitor (child sessions, etc.)
     */
    fun monitorSession(sessionId: String) {
        _monitoredSessionIds.update { it + sessionId }
        eventStreamRepository.monitorSession(sessionId)
        Napier.v("[StateCoordinator] Now monitoring session: $sessionId")
    }
    
    /**
     * Stop monitoring a session.
     */
    fun stopMonitoringSession(sessionId: String) {
        _monitoredSessionIds.update { it - sessionId }
        eventStreamRepository.stopMonitoringSession(sessionId)
        Napier.v("[StateCoordinator] Stopped monitoring session: $sessionId")
    }
    
    /**
     * Get whether a session is currently active.
     */
    fun isSessionActive(sessionId: String): Boolean = _activeSessionId.value == sessionId
    
    /**
     * Get whether a session is being monitored.
     */
    fun isSessionMonitored(sessionId: String): Boolean = _monitoredSessionIds.value.contains(sessionId)
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EVENT OBSERVATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private fun startObservingEvents() {
        eventObserverJob = coordinatorScope.launch {
            eventStreamRepository.events.collect { event ->
                handleServerEvent(event)
            }
        }
    }
    
    private fun startObservingConnection() {
        connectionObserverJob = coordinatorScope.launch {
            connectionManager.status.collect { status ->
                if (status.isConnected && !_isSyncing.value) {
                    Napier.i("[StateCoordinator] Connection established - syncing")
                    syncFromServer()
                }
                // Broadcast connection state changes
                _broadcastEvents.emit(BroadcastEvent.ConnectionStateChanged(status))
            }
        }
    }
    
    private fun startObservingLifecycle() {
        appLifecycleObserver?.let { observer ->
            lifecycleObserverJob = coordinatorScope.launch {
                observer.lifecycleState.collect { state ->
                    when (state) {
                        AppLifecycleState.FOREGROUND -> {
                            Napier.i("[StateCoordinator] App foregrounded")
                            onForeground()
                        }
                        AppLifecycleState.BACKGROUND -> {
                            Napier.i("[StateCoordinator] App backgrounded")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Handle incoming server events.
     * This is the central event dispatch point.
     */
    private suspend fun handleServerEvent(event: ServerEvent) {
        // Update running session tracking
        when (event) {
            is ServerEvent.SessionUpdated -> {
                val session = event.properties.info
                val isRunning = session.status == SessionStatus.RUNNING
                _runningSessionIds.update { current ->
                    if (isRunning) current + session.id else current - session.id
                }
            }
            is ServerEvent.SessionIdle -> {
                _runningSessionIds.update { it - event.properties.sessionID }
                // Trigger callback for message refresh
                onSessionIdle?.invoke(event.properties.sessionID)
            }
            is ServerEvent.SessionError -> {
                event.properties.sessionID?.let { 
                    _runningSessionIds.update { it - it }
                }
            }
            is ServerEvent.SessionDeleted -> {
                _runningSessionIds.update { it - event.properties.info.id }
            }
            else -> {}
        }
        
        // Broadcast to all subscribers
        _broadcastEvents.emit(BroadcastEvent.ServerEvent(event))
        
        // Additional handling for specific events
        when (event) {
            is ServerEvent.Connected -> {
                Napier.i("[StateCoordinator] Server connected: ${event.properties.version}")
                syncFromServer()
            }
            else -> {}
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SYNC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Called when app comes to foreground.
     */
    private fun onForeground() {
        // Trigger app resume callback
        onAppResume?.invoke()
        
        coordinatorScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val lastSync = _lastSyncTime.value
            val needsSync = lastSync == null || (now - lastSync) > 30_000L
            
            if (needsSync && connectionStatus.value.isConnected) {
                syncFromServer()
            }
            
            // Refresh session status
            refreshSessionStatus()
        }
    }
    
    /**
     * Sync all state from server.
     */
    fun syncFromServer() {
        syncJob?.cancel()
        syncJob = coordinatorScope.launch {
            if (_isSyncing.value) {
                Napier.v("[StateCoordinator] Already syncing, skipping")
                return@launch
            }
            
            _isSyncing.value = true
            Napier.i("[StateCoordinator] Starting full sync...")
            
            try {
                // Sync sessions
                sessionRepository.refreshSessions()
                
                // Refresh session status
                refreshSessionStatus()
                
                _lastSyncTime.value = Clock.System.now().toEpochMilliseconds()
                Napier.i("[StateCoordinator] Sync completed successfully")
                
                // Broadcast sync complete
                _broadcastEvents.emit(BroadcastEvent.SyncCompleted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("[StateCoordinator] Sync failed", e)
                _broadcastEvents.emit(BroadcastEvent.SyncFailed(e.message ?: "Unknown error"))
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * Refresh real-time session status.
     */
    private suspend fun refreshSessionStatus() {
        sessionRepository.getSessionStatus().fold(
            onSuccess = { statusMap ->
                val runningIds = statusMap.filter { (_, status) ->
                    status.isBusy || status.isRetrying
                }.keys
                _runningSessionIds.value = runningIds
                Napier.v("[StateCoordinator] Refreshed session status: ${runningIds.size} running")
            },
            onFailure = { error ->
                Napier.w("[StateCoordinator] Failed to refresh session status: ${error.message}")
            }
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PERMISSION/QUESTION HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    fun dismissPermission() = eventStreamRepository.dismissPermission()
    fun dismissQuestion() = eventStreamRepository.dismissQuestion()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SSE CONNECTION CONTROL
    // ═══════════════════════════════════════════════════════════════════════════════
    
    fun connectSse(sessionId: String? = null) {
        eventStreamRepository.connect(coordinatorScope, sessionId)
    }
    
    fun disconnectSse() {
        eventStreamRepository.disconnect()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════════════
    
    fun dispose() {
        eventObserverJob?.cancel()
        connectionObserverJob?.cancel()
        lifecycleObserverJob?.cancel()
        syncJob?.cancel()
        coordinatorScope.cancel()
        Napier.i("[StateCoordinator] Disposed")
    }
}

/**
 * Broadcast events for state stores to subscribe to.
 */
sealed class BroadcastEvent {
    /** Server event received */
    data class ServerEvent(val event: com.mocca.app.domain.model.ServerEvent) : BroadcastEvent()
    
    /** Active session changed */
    data class ActiveSessionChanged(val sessionId: String?) : BroadcastEvent()
    
    /** Connection state changed */
    data class ConnectionStateChanged(val status: ConnectionStatus) : BroadcastEvent()
    
    /** Sync completed successfully */
    data object SyncCompleted : BroadcastEvent()
    
    /** Sync failed */
    data class SyncFailed(val error: String) : BroadcastEvent()
}
