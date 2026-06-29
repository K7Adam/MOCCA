package com.mocca.app.data.repository

import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Specialized state store for chat screen state.
 * 
 * Provides reactive chat state with:
 * - Message observation from local cache
 * - StateCoordinator-driven streaming updates
 * - Pending permission/question management
 * - Thinking state for extended reasoning models
 * 
 * Architecture:
 * SSE Events -> EventStreamRepository -> StateCoordinator -> ChatStateStore -> UI
 *                    ↓                        ↓                  ↑
 *              LocalCache <--------------------------------------┘
 */
class ChatStateStore(
    private val localCache: LocalCache,
    private val stateCoordinator: StateCoordinator,
    private val sessionRepository: SessionRepository,
    private val aiChatGateway: AiChatGateway
) {
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private companion object {
        const val INITIAL_MESSAGE_FETCH_LIMIT = 80
        const val CHILD_MESSAGE_FETCH_LIMIT = 50
        const val RELOAD_COOLDOWN_MS = 2_500L
    }

    // SESSION STATE

    
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()
    
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    // MESSAGE STATE - Reactive from DB

    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private val _optimisticUserMessage = MutableStateFlow<Message?>(null)
    
    val messages: StateFlow<List<Message>> = combine(_messages, _optimisticUserMessage) { dbMsgs, optMsg ->
        if (optMsg == null) {
            dbMsgs
        } else {
            val optText = (optMsg.parts.firstOrNull() as? MessagePart.Text)?.text ?: ""
            val alreadyInDb = dbMsgs.takeLast(5).any { 
                it.role == MessageRole.USER && 
                it.parts.filterIsInstance<MessagePart.Text>().any { part -> part.text == optText }
            }
            if (alreadyInDb) {
                storeScope.launch { _optimisticUserMessage.value = null }
                dbMsgs
            } else {
                dbMsgs + optMsg
            }
        }
    }.stateIn(storeScope, SharingStarted.Eagerly, emptyList())
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    // Server-authoritative session idle state based on runningSessionIds from StateCoordinator
    // This is purely driven by the server's /session/status endpoint which is polled regularly
    // and updated via SSE events. This replaces the old logic that relied on local _isSending
    // which had race conditions causing the button to incorrectly switch back to SEND.
    //
    // The session is IDLE only when:
    // - The server reports this session as NOT running (runningSessionIds doesn't contain our sessionId)
    //
    // This is the CORRECT source of truth for button state.
    val isSessionIdle: StateFlow<Boolean> = combine(
        _currentSessionId,
        stateCoordinator.runningSessionIds
    ) { currentSessionId, runningSessionIds ->
        // Session is idle when it's not in the running set
        currentSessionId == null || !runningSessionIds.contains(currentSessionId)
    }.stateIn(
        storeScope,
        SharingStarted.Eagerly,
        true
    )
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _sessionDisposed = MutableStateFlow<String?>(null)
    /** Non-null when a server.instance.disposed or global.disposed event was received; contains the reason string. */
    val sessionDisposed: StateFlow<String?> = _sessionDisposed.asStateFlow()

    // STREAMING STATE - From StateCoordinator

    
    val streamingText: StateFlow<String> = stateCoordinator.streamingText
    val isThinking: StateFlow<Boolean> = stateCoordinator.isThinking
    val thinkingContent: StateFlow<String> = stateCoordinator.thinkingContent
    val chatTurnState: StateFlow<ChatTurnState> = stateCoordinator.chatTurnState
    
    // Agent running state - authoritative source for whether agent is working
    // This is used for button control (ABORT vs SEND)
    val isAgentRunning: StateFlow<Boolean> = stateCoordinator.isAgentRunning
    
    private val _thinkingElapsedMs = MutableStateFlow(0L)
    val thinkingElapsedMs: StateFlow<Long> = _thinkingElapsedMs.asStateFlow()

    // PERMISSION/QUESTION STATE - From StateCoordinator

    
    val pendingPermission: StateFlow<PermissionRequest?> = stateCoordinator.pendingPermission
    val pendingQuestion: StateFlow<QuestionRequest?> = stateCoordinator.pendingQuestion

    // Task state

    
    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()

    // CHILD SESSION STATE (for subagents)

    
    private val _childSessions = MutableStateFlow<Map<String, Session>>(emptyMap())
    val childSessions: StateFlow<Map<String, Session>> = _childSessions.asStateFlow()
    
    private val _childMessages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val childMessages: StateFlow<Map<String, List<Message>>> = _childMessages.asStateFlow()
    
    private val _childStreamingText = MutableStateFlow<Map<String, String>>(emptyMap())
    val childStreamingText: StateFlow<Map<String, String>> = _childStreamingText.asStateFlow()

    // INTERNAL STATE

    
    private var messageObserverJob: Job? = null
    private var broadcastObserverJob: Job? = null
    private var todoObserverJob: Job? = null

    // Conflated channel: rapid reloadMessages() calls collapse to one pending reload.
    // CONFLATED means only the latest value survives — no queue buildup, no concurrent fetches.
    private val reloadChannel = Channel<String>(Channel.CONFLATED)
    private val reloadMutex = Mutex()
    private val lastReloadMs = mutableMapOf<String, Long>()

    // INITIALIZATION

    
    init {
        Napier.i("[ChatStateStore] Initialized with StateCoordinator")
        observeBroadcastEvents()
        startReloadConsumer()
    }
    
    /**
     * Load a session for chat.
     * This sets up message observation and SSE connection via StateCoordinator.
     * @param sessionId The session ID to load
     * @param forceReload If true, reload even if session appears already loaded
     */
    suspend fun loadSession(sessionId: String, forceReload: Boolean = false) {
        // Skip if already loaded (unless force reload)
        if (!forceReload && _currentSessionId.value == sessionId && _messages.value.isNotEmpty()) {
            Napier.v("[ChatStateStore] Session $sessionId already loaded")
            return
        }
        
        Napier.i("[ChatStateStore] Loading session: $sessionId (forceReload=$forceReload)")
        _currentSessionId.value = sessionId
        _isLoading.value = true
        _error.value = null
        
        // Set active session via StateCoordinator (this also connects SSE)
        stateCoordinator.setActiveSession(sessionId)
        
        // Cancel previous observations
        messageObserverJob?.cancel()
        todoObserverJob?.cancel()
        
        // Observe messages from DB (reactive)
        messageObserverJob = storeScope.launch {
            localCache.observeRecentMessages(sessionId, 100).collect { messages ->
                _messages.value = messages
                _isLoading.value = false
            }
        }
        
        // Load child sessions
        loadChildSessions(sessionId)
        
        // Load todos
        loadTodos(sessionId)
        
        // Fetch messages from server to populate cache
        reloadMessages(sessionId)
    }
    
    /**
     * Unload current session.
     */
    fun unloadSession() {
        val sessionId = _currentSessionId.value ?: return
        
        Napier.i("[ChatStateStore] Unloading session: $sessionId")
        stateCoordinator.stopMonitoringSession(sessionId)
        
        messageObserverJob?.cancel()
        messageObserverJob = null
        todoObserverJob?.cancel()
        todoObserverJob = null
        
        _currentSessionId.value = null
        _session.value = null
        _messages.value = emptyList()
        _todos.value = emptyList()
        _childSessions.value = emptyMap()
        _childMessages.value = emptyMap()
    }

    // BROADCAST EVENT HANDLING

    
    private fun observeBroadcastEvents() {
        broadcastObserverJob = storeScope.launch {
            stateCoordinator.broadcastEvents.collect { event ->
                handleBroadcastEvent(event)
            }
        }
        
        // Observe thinking time
        storeScope.launch {
            stateCoordinator.thinkingStartTime.collect { startTime ->
                if (startTime != null) {
                    while (stateCoordinator.isThinking.value) {
                        _thinkingElapsedMs.value = Clock.System.now().toEpochMilliseconds() - startTime
                        delay(100)
                    }
                } else {
                    _thinkingElapsedMs.value = 0
                }
            }
        }
    }
    
    private fun handleBroadcastEvent(event: BroadcastEvent) {
        when (event) {
            is BroadcastEvent.ServerEvent -> handleServerEvent(event.event)
            is BroadcastEvent.ActiveSessionChanged -> {
                Napier.v("[ChatStateStore] Active session changed to: ${event.sessionId}")
            }
            is BroadcastEvent.SyncCompleted -> {
                // Full app sync refreshes session metadata. Chat history is loaded by loadSession
                // and SSE/idle events; reloading here caused duplicate startup fetches.
            }
            else -> {}
        }
    }
    
    private fun handleServerEvent(event: ServerEvent) {
        val currentId = _currentSessionId.value ?: return
        
        when (event) {
            is ServerEvent.SessionUpdated -> {
                val session = event.properties.info
                if (session.id == currentId) {
                    _session.value = session
                }
                // Handle child session updates
                if (session.effectiveParentID == currentId) {
                    _childSessions.value = _childSessions.value + (session.id to session)
                    loadChildMessages(session.id)
                }
            }
            
            is ServerEvent.SessionIdle -> {
                if (event.properties.sessionID == currentId) {
                    _isSending.value = false
                    
                    // Fast-sync messages on idle instead of full reload (Cache Optimization)
                    syncLatestMessages(currentId)
                    refreshTodos()
                }
                // Child session idle
                if (_childSessions.value.containsKey(event.properties.sessionID)) {
                    _childStreamingText.value = _childStreamingText.value - event.properties.sessionID
                }
            }
            
            is ServerEvent.MessageUpdated -> {
                val msgSessionId = event.properties.info.sessionID
                if (msgSessionId == currentId) {
                    // DB observer will pick this up
                    _isSending.value = false
                }
            }
            
            is ServerEvent.MessagePartUpdated -> {
                // Streaming is handled by StateCoordinator -> EventStreamRepository
                // We just need to track child session streaming
                val part = event.properties.part
                if (_childSessions.value.containsKey(part.sessionID)) {
                    if (part.type == "text") {
                        val existing = _childStreamingText.value[part.sessionID] ?: ""
                        _childStreamingText.value = _childStreamingText.value + 
                            (part.sessionID to (existing + (event.properties.delta ?: part.text ?: "")))
                    }
                }
            }
            
            is ServerEvent.ServerInstanceDisposed -> {
                Napier.w("[ChatStateStore] Server instance disposed: ${event.properties.reason}")
                _isSending.value = false
                _sessionDisposed.value = event.properties.reason ?: "Server instance disposed"
            }
            is ServerEvent.GlobalDisposed -> {
                Napier.w("[ChatStateStore] Global disposed: ${event.properties.reason}")
                _isSending.value = false
                _sessionDisposed.value = event.properties.reason ?: "Server session ended"
            }
            else -> { /* Other events handled elsewhere */ }
        }
    }

    // MESSAGE OPERATIONS

    
    /**
     * Reload messages from server.
     * Instead of launching a coroutine directly (causing concurrent fetches), we send the sessionId
     * to the conflated reloadChannel. Channel.CONFLATED means rapid calls collapse — only the last
     * pending value survives — so we never have multiple concurrent getMessages() in-flight.
     */
    private fun reloadMessages(sessionId: String) {
        // trySend on a CONFLATED channel never blocks and never fails (replaces pending value).
        reloadChannel.trySend(sessionId)
    }

    /**
     * Fast-sync messages from server without full DB reload.
     * Used when session goes idle to efficiently update latest message states.
     */
    private fun syncLatestMessages(sessionId: String) {
        storeScope.launch {
            try {
                sessionRepository.syncLatestMessages(sessionId, keepLimit = 5)
                // DB observer will automatically update _messages
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("[ChatStateStore] Error fast-syncing messages: \${e.message}", e)
                }
            }
        }
    }

    /**
     * Single coroutine consumer for the reload channel.
     * Processes one reload at a time, so we can never have concurrent getMessages() calls.
     */
    private fun startReloadConsumer() {
        storeScope.launch {
            for (sessionId in reloadChannel) {
                try {
                    if (_currentSessionId.value != sessionId || !shouldRunReload(sessionId)) {
                        continue
                    }
                    Napier.i("[ChatStateStore] Fetching messages for session: $sessionId")
                    val result = sessionRepository.refreshMessages(sessionId, INITIAL_MESSAGE_FETCH_LIMIT)
                    result.fold(
                        onSuccess = { messages ->
                            Napier.i("[ChatStateStore] Fetched ${messages.size} messages for session: $sessionId")
                            if (_messages.value.isEmpty()) {
                                _isLoading.value = false
                            }
                        },
                        onFailure = { error ->
                            Napier.e("[ChatStateStore] Failed to fetch messages: ${error.message}")
                            if (_messages.value.isEmpty()) {
                                _error.value = error.message ?: "Failed to fetch messages"
                                _isLoading.value = false
                            }
                        }
                    )
                } catch (e: OutOfMemoryError) {
                    Napier.e("[ChatStateStore] OOM while fetching messages for $sessionId — skipping", e)
                    // Don't set error state — cached messages are still shown
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Napier.e("[ChatStateStore] Error fetching messages: \${e.message}", e)
                }
            }
        }
    }
    
    /**
     * Load more messages (pagination).
     */
    fun loadMoreMessages() {
        val sessionId = _currentSessionId.value ?: return
        storeScope.launch {
            val cursor = _messages.value.lastOrNull()?.createdAt
            sessionRepository.loadMoreMessages(sessionId, cursor ?: 0, 50)
        }
    }

    // CHILD SESSION OPERATIONS

    
    private fun loadChildSessions(parentId: String) {
        storeScope.launch {
            sessionRepository.getChildren(parentId).fold(
                onSuccess = { children ->
                    _childSessions.value = children.associateBy { it.id }
                    children.forEach { child ->
                        loadChildMessages(child.id)
                        // Monitor child sessions for updates
                        stateCoordinator.monitorSession(child.id)
                    }
                },
                onFailure = { Napier.w("[ChatStateStore] Failed to load child sessions") }
            )
        }
    }
    
    private fun loadChildMessages(childSessionId: String) {
        storeScope.launch {
            try {
                val cached = withContext(Dispatchers.IO) {
                    localCache.getMessagesPaged(childSessionId, null, CHILD_MESSAGE_FETCH_LIMIT.toLong()).asReversed()
                }
                if (cached.isNotEmpty()) {
                    _childMessages.value = _childMessages.value + (childSessionId to cached)
                }

                sessionRepository.refreshMessages(childSessionId, CHILD_MESSAGE_FETCH_LIMIT)
                    .onSuccess { messages ->
                        _childMessages.value = _childMessages.value + (childSessionId to messages)
                    }
                    .onFailure { error ->
                        Napier.w("[ChatStateStore] Failed to load child messages: ${error.message}")
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("[ChatStateStore] Failed to load child messages", e)
            }
        }
    }

    // Task operations

    
    private fun loadTodos(sessionId: String) {
        todoObserverJob?.cancel()
        todoObserverJob = storeScope.launch {
            sessionRepository.getSessionTodos(sessionId).collect { resource ->
                if (resource is Resource.Success) {
                    _todos.value = resource.data
                }
            }
        }
    }
    
    fun refreshTodos() {
        _currentSessionId.value?.let { loadTodos(it) }
    }

    // MESSAGE SENDING

    
    /**
     * Send a message to the current session.
     */
    suspend fun sendMessage(
        text: String,
        selection: AiEffectiveSelection,
        attachments: List<AttachedFile> = emptyList(),
    ): Result<Unit> {
        val sessionId = _currentSessionId.value ?: return Result.failure(Exception("No session selected"))
        
        // Optimistic UI update
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        _optimisticUserMessage.value = Message(
            id = "local-$now",
            sessionId = sessionId,
            role = MessageRole.USER,
            parts = listOf(MessagePart.Text(text = text)),
            createdAt = now
        )
        
        _isSending.value = true
        _error.value = null

        Napier.i("[ChatStateStore] Sending message: session=$sessionId provider=${selection.providerId} model=${selection.modelId}")
        return aiChatGateway.sendMessage(
            sessionId = sessionId,
            text = text,
            selection = selection,
            attachments = attachments
        ).also { result ->
            if (result.isFailure) {
                Napier.e("[ChatStateStore] Send failed: ${result.exceptionOrNull()?.message}", result.exceptionOrNull())
                _isSending.value = false
                _error.value = result.exceptionOrNull()?.message
                _optimisticUserMessage.value = null
            } else {
                Napier.i("[ChatStateStore] Send accepted: session=$sessionId")
            }
        }
    }
    
    /**
     * Abort the current session.
     */
    suspend fun abortSession(): Result<Boolean> {
        val sessionId = _currentSessionId.value ?: return Result.failure(Exception("No session selected"))
        
        return sessionRepository.abortSession(sessionId).also { result ->
            if (result.isSuccess) {
                _isSending.value = false
            }
        }
    }

    private suspend fun shouldRunReload(sessionId: String): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        return reloadMutex.withLock {
            val lastRun = lastReloadMs[sessionId]
            if (lastRun != null && now - lastRun < RELOAD_COOLDOWN_MS) {
                false
            } else {
                lastReloadMs[sessionId] = now
                true
            }
        }
    }

    // PERMISSION/QUESTION HANDLING

    
    suspend fun approvePermission(): Result<Boolean> {
        val permission = pendingPermission.value ?: return Result.failure(Exception("No pending permission"))
        return sessionRepository.replyToPermission(permission.sessionId, permission.id, PermissionResponseType.ONCE).also {
            if (it.isSuccess) stateCoordinator.dismissPermission()
        }
    }

    suspend fun approvePermissionAlways(): Result<Boolean> {
        val permission = pendingPermission.value ?: return Result.failure(Exception("No pending permission"))
        return sessionRepository.replyToPermission(permission.sessionId, permission.id, PermissionResponseType.ALWAYS, remember = true).also {
            if (it.isSuccess) stateCoordinator.dismissPermission()
        }
    }

    suspend fun denyPermission(): Result<Boolean> {
        val permission = pendingPermission.value ?: return Result.failure(Exception("No pending permission"))
        return sessionRepository.replyToPermission(permission.sessionId, permission.id, PermissionResponseType.REJECT).also {
            if (it.isSuccess) stateCoordinator.dismissPermission()
        }
    }
    
    fun dismissPermission() = stateCoordinator.dismissPermission()
    
    suspend fun answerQuestion(answers: List<List<String>>): Result<Boolean> {
        val question = pendingQuestion.value ?: return Result.failure(Exception("No pending question"))
        return sessionRepository.replyToQuestion(question.id, answers).also {
            if (it.isSuccess) stateCoordinator.dismissQuestion()
        }
    }
    
    suspend fun rejectQuestion(): Result<Boolean> {
        val question = pendingQuestion.value ?: return Result.failure(Exception("No pending question"))
        return sessionRepository.rejectQuestion(question.id).also {
            if (it.isSuccess) stateCoordinator.dismissQuestion()
        }
    }

    // SESSION OPERATIONS

    
    suspend fun forkSession(messageId: String): Result<Session> {
        val sessionId = _currentSessionId.value ?: return Result.failure(Exception("No session selected"))
        return sessionRepository.forkFromMessage(sessionId, messageId)
    }
    
    suspend fun revertSession(messageId: String): Result<Session> {
        val sessionId = _currentSessionId.value ?: return Result.failure(Exception("No session selected"))
        return sessionRepository.revertToMessage(sessionId, messageId).also {
            if (it.isSuccess) reloadMessages(sessionId)
        }
    }
    
    suspend fun unrevertSession(): Result<Session> {
        val sessionId = _currentSessionId.value ?: return Result.failure(Exception("No session selected"))
        return sessionRepository.unrevertSession(sessionId).also {
            if (it.isSuccess) reloadMessages(sessionId)
        }
    }
    
    suspend fun shareSession(): Result<Session> {
        val sessionId = _currentSessionId.value ?: return Result.failure(Exception("No session selected"))
        return sessionRepository.shareSession(sessionId).let { result ->
            if (result is Resource.Success) Result.success(result.data)
            else Result.failure(Exception((result as? Resource.Error)?.message ?: "Failed to share"))
        }
    }

    // UTILITY

    
    fun clearError() { _error.value = null }
    fun clearDisposed() { _sessionDisposed.value = null }
    
    /**
     * Retry loading the current session.
     * Uses forceReload=true to bypass the early return check.
     */
    suspend fun retryLoad() {
        val sessionId = _currentSessionId.value ?: return
        clearError()
        loadSession(sessionId, forceReload = true)
    }
    
    fun dispose() {
        messageObserverJob?.cancel()
        broadcastObserverJob?.cancel()
        todoObserverJob?.cancel()
        reloadChannel.close()
        storeScope.cancel()
        Napier.i("[ChatStateStore] Disposed")
    }
}
