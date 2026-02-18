package com.mocca.app.data.repository

import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
 * ```
 * SSE Events → EventStreamRepository → StateCoordinator → ChatStateStore → UI
 *                    ↓                        ↓                  ↑
 *              LocalCache ←────────────────────────────────────┘
 * ```
 */
class ChatStateStore(
    private val localCache: LocalCache,
    private val stateCoordinator: StateCoordinator,
    private val sessionRepository: SessionRepository
) {
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SESSION STATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()
    
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MESSAGE STATE - Reactive from DB
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    private val _isSessionIdle = MutableStateFlow(true)
    val isSessionIdle: StateFlow<Boolean> = _isSessionIdle.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STREAMING STATE - From StateCoordinator
    // ═══════════════════════════════════════════════════════════════════════════════
    
    val streamingText: StateFlow<String> = stateCoordinator.streamingText
    val isThinking: StateFlow<Boolean> = stateCoordinator.isThinking
    val thinkingContent: StateFlow<String> = stateCoordinator.thinkingContent
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PERMISSION/QUESTION STATE - From StateCoordinator
    // ═══════════════════════════════════════════════════════════════════════════════
    
    val pendingPermission: StateFlow<PermissionRequest?> = stateCoordinator.pendingPermission
    val pendingQuestion: StateFlow<QuestionRequest?> = stateCoordinator.pendingQuestion
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TODO STATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CHILD SESSION STATE (for subagents)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _childSessions = MutableStateFlow<Map<String, Session>>(emptyMap())
    val childSessions: StateFlow<Map<String, Session>> = _childSessions.asStateFlow()
    
    private val _childMessages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val childMessages: StateFlow<Map<String, List<Message>>> = _childMessages.asStateFlow()
    
    private val _childStreamingText = MutableStateFlow<Map<String, String>>(emptyMap())
    val childStreamingText: StateFlow<Map<String, String>> = _childStreamingText.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INTERNAL STATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private var messageObserverJob: Job? = null
    private var broadcastObserverJob: Job? = null
    private var todoObserverJob: Job? = null
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    init {
        Napier.i("[ChatStateStore] Initialized with StateCoordinator")
        observeBroadcastEvents()
    }
    
    /**
     * Load a session for chat.
     * This sets up message observation and SSE connection via StateCoordinator.
     */
    suspend fun loadSession(sessionId: String) {
        if (_currentSessionId.value == sessionId && _messages.value.isNotEmpty()) {
            Napier.v("[ChatStateStore] Session $sessionId already loaded")
            return
        }
        
        Napier.i("[ChatStateStore] Loading session: $sessionId")
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
                Napier.v("[ChatStateStore] Messages updated: ${messages.size}")
            }
        }
        
        // Load child sessions
        loadChildSessions(sessionId)
        
        // Load todos
        loadTodos(sessionId)
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
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BROADCAST EVENT HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private fun observeBroadcastEvents() {
        broadcastObserverJob = storeScope.launch {
            stateCoordinator.broadcastEvents.collect { event ->
                handleBroadcastEvent(event)
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
                // Reload messages on sync complete if we have a session
                _currentSessionId.value?.let { sessionId ->
                    reloadMessages(sessionId)
                }
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
                    _isSessionIdle.value = session.status != SessionStatus.RUNNING
                }
                // Handle child session updates
                if (session.effectiveParentID == currentId) {
                    _childSessions.value = _childSessions.value + (session.id to session)
                    loadChildMessages(session.id)
                }
            }
            
            is ServerEvent.SessionIdle -> {
                if (event.properties.sessionID == currentId) {
                    _isSessionIdle.value = true
                    _isSending.value = false
                    
                    // Reload messages on idle (message complete)
                    reloadMessages(currentId)
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
            
            else -> { /* Other events handled elsewhere */ }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MESSAGE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Reload messages from server.
     */
    private fun reloadMessages(sessionId: String) {
        storeScope.launch {
            sessionRepository.getMessages(sessionId, 100).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        // DB observer will update _messages
                    }
                    is Resource.Error -> {
                        _error.value = resource.message
                    }
                    else -> {}
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
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CHILD SESSION OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
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
            sessionRepository.getMessages(childSessionId, 50).collect { resource ->
                if (resource is Resource.Success) {
                    _childMessages.value = _childMessages.value + (childSessionId to resource.data)
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TODO OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
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
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MESSAGE SENDING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Send a message to the current session.
     */
    suspend fun sendMessage(
        text: String,
        mode: String? = null,
        variant: String? = null,
        attachments: List<AttachedFile> = emptyList(),
        modelId: String? = null,
        providerId: String? = null
    ): Result<Unit> {
        val sessionId = _currentSessionId.value ?: return Result.failure(Exception("No session selected"))
        
        _isSending.value = true
        _isSessionIdle.value = false
        _error.value = null
        
        return sessionRepository.sendMessageAsync(
            sessionId = sessionId,
            text = text,
            mode = mode,
            variant = variant,
            attachments = attachments,
            modelId = modelId,
            providerId = providerId
        ).also { result ->
            if (result.isFailure) {
                _isSending.value = false
                _isSessionIdle.value = true
                _error.value = result.exceptionOrNull()?.message
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
                _isSessionIdle.value = true
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PERMISSION/QUESTION HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    suspend fun approvePermission(): Result<Boolean> {
        val permission = pendingPermission.value ?: return Result.failure(Exception("No pending permission"))
        return sessionRepository.respondToPermission(permission.sessionId, permission.id, true).also {
            if (it.isSuccess) stateCoordinator.dismissPermission()
        }
    }
    
    suspend fun denyPermission(): Result<Boolean> {
        val permission = pendingPermission.value ?: return Result.failure(Exception("No pending permission"))
        return sessionRepository.respondToPermission(permission.sessionId, permission.id, false).also {
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
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SESSION OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
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
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════════════
    
    fun clearError() { _error.value = null }
    
    fun dispose() {
        messageObserverJob?.cancel()
        broadcastObserverJob?.cancel()
        todoObserverJob?.cancel()
        storeScope.cancel()
        Napier.i("[ChatStateStore] Disposed")
    }
}
