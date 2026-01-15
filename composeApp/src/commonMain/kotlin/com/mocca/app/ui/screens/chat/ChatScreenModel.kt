package com.mocca.app.ui.screens.chat

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.EventStreamRepository
import com.mocca.app.data.repository.SessionRepository
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

data class ChatState(
    val sessionId: String = "",
    val session: Session? = null,
    val messages: List<Message> = emptyList(),
    val childSessions: Map<String, Session> = emptyMap(), // sessionId -> Session
    val childMessages: Map<String, List<Message>> = emptyMap(), // sessionId -> Messages
    val childStreamingText: Map<String, String> = emptyMap(), // sessionId -> text
    val streamingText: String = "",
    // NOTE: inputText moved to separate StateFlow to prevent recomposition cascade
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val pendingPermission: PermissionRequest? = null,
    val pendingQuestion: QuestionRequest? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val isSessionIdle: Boolean = true,
    val modelName: String = "CLAUDE",
    val agentName: String = "SISYPHUS"
)

class ChatScreenModel(
    initialSessionId: String?,
    private val sessionRepository: SessionRepository,
    private val eventStreamRepository: EventStreamRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(ChatState(sessionId = initialSessionId ?: ""))
    val state: StateFlow<ChatState> = _state.asStateFlow()
    
    // Helper to keep existing logic working with dynamic state
    private val sessionId get() = _state.value.sessionId

    // PERFORMANCE FIX: Separate inputText to prevent aggregatedMessages recomputation on every keystroke
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()
    
    // Child streaming text map (sessionId -> text)
    private val _childStreamingText = MutableStateFlow<Map<String, String>>(emptyMap())
    val childStreamingText: StateFlow<Map<String, String>> = _childStreamingText.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    val aggregatedMessages: StateFlow<List<Message>> = _state
        .map { s -> s.messages to s.childSessions }
        .distinctUntilChanged() // IMPORTANT: Only recompute if messages or children structure changes
        .map { (messages, childSessions) ->
            val rootMessages = messages.toMutableList()
            
            val syntheticMessages = childSessions.values.map { child ->
                Message(
                    id = "child-${child.id}",
                    sessionId = _state.value.sessionId,
                    role = MessageRole.ASSISTANT,
                    parts = listOf(MessagePart.SubTask(
                        sessionId = child.id,
                        title = child.title ?: "Sub-task",
                        status = child.status,
                        messages = _state.value.childMessages[child.id] ?: emptyList(),
                        streamingText = "" // Don't use state.childStreamingText here to prevent re-sort on every char
                    )),
                    createdAt = child.createdAt
                )
            }
            
            (rootMessages + syntheticMessages).sortedBy { it.createdAt }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    init {
        loadConfig()
        // Ensure we are monitoring events globally for this screen model
        observeEvents()
        
        if (initialSessionId != null) {
            loadSession(initialSessionId)
        }
    }

    fun loadSession(newSessionId: String) {
        // Don't reload if already loaded, unless it's empty/initial
        if (_state.value.sessionId == newSessionId && _state.value.messages.isNotEmpty()) return

        Napier.i("ChatScreenModel switching to session: $newSessionId")
        
        // Reset state for new session
        // Reset state for new session, but PRESERVE connection status and config
        // CRITICAL FIX: If we reset connectionStatus to Disconnected, the state collector won't 
        // fire again if the repo is already Connected, leaving the UI permanently disabled.
        _state.value = ChatState(
            sessionId = newSessionId,
            isLoading = true, // Show loading spinner immediately
            connectionStatus = eventStreamRepository.connectionStatus.value,
            modelName = _state.value.modelName,
            agentName = _state.value.agentName
        )
        _inputText.value = ""
        _streamingText.value = ""
        _childStreamingText.value = emptyMap()

        // Connect and load
        connectToEventStream()
        loadMessages()
        loadChildren()
    }
    
    private fun loadChildren() {
        val currentSessionId = _state.value.sessionId
        if (currentSessionId.isEmpty()) return
        
        screenModelScope.launch {
            sessionRepository.getChildren(currentSessionId).onSuccess { children ->
                val childMap = children.associateBy { it.id }
                _state.value = _state.value.copy(childSessions = childMap)
                
                // Monitor each child session
                children.forEach { child ->
                    eventStreamRepository.monitorSession(child.id)
                    loadChildMessages(child.id)
                }
            }
        }
    }

    private fun loadChildMessages(childId: String) {
        screenModelScope.launch {
            sessionRepository.getMessages(childId).collect { resource ->
                if (resource is Resource.Success) {
                    val currentMessages = _state.value.childMessages.toMutableMap()
                    currentMessages[childId] = resource.data
                    _state.value = _state.value.copy(childMessages = currentMessages)
                }
            }
        }
    }

    private fun observeEvents() {
        // Connection status
        screenModelScope.launch {
            eventStreamRepository.connectionStatus.collect { status ->
                _state.value = _state.value.copy(connectionStatus = status)
            }
        }
        
        // Streaming text
        screenModelScope.launch {
            eventStreamRepository.streamingText.collect { text ->
                _streamingText.value = text
                // Removed from _state to prevent recomposition loop
            }
        }
        
        // Permission requests
        screenModelScope.launch {
            eventStreamRepository.pendingPermission.collect { permission ->
                _state.value = _state.value.copy(pendingPermission = permission)
            }
        }
        
        // Question requests
        screenModelScope.launch {
            eventStreamRepository.pendingQuestion.collect { question ->
                _state.value = _state.value.copy(pendingQuestion = question)
            }
        }
        
        // Session-specific events
        screenModelScope.launch {
            eventStreamRepository.eventsForMonitoredSessions().collect { event ->
                Napier.d("ChatScreenModel received: ${event.type}")
                when (event) {
                    is ServerEvent.MessageUpdated -> {
                        if (event.properties.info.sessionID == sessionId) {
                            Napier.i("Message complete, reloading and resetting state")
                            // CRITICAL FIX: Reset sending state here too, in case SessionIdle is delayed/missed
                            _state.value = _state.value.copy(
                                isSending = false,
                                isSessionIdle = true // Assume idle if message is done, SessionIdle will confirm
                            )
                            loadMessages()
                        } else if (_state.value.childSessions.containsKey(event.properties.info.sessionID)) {
                            loadChildMessages(event.properties.info.sessionID)
                        }
                    }
                    is ServerEvent.MessagePartUpdated -> {
                        val part = event.properties.part
                        if (part.sessionID != sessionId && _state.value.childSessions.containsKey(part.sessionID)) {
                            if (part.type == "text" && part.text != null) {
                                 val currentText = _childStreamingText.value.toMutableMap()
                                 currentText[part.sessionID] = part.text
                                 _childStreamingText.value = currentText
                            }
                        }
                    }
                    is ServerEvent.SessionIdle -> {
                        if (event.properties.sessionID == sessionId) {
                            _state.value = _state.value.copy(
                                isSessionIdle = true,
                                isSending = false
                            )
                            loadMessages()
                        } else if (_state.value.childSessions.containsKey(event.properties.sessionID)) {
                             val currentText = _childStreamingText.value.toMutableMap()
                             currentText.remove(event.properties.sessionID)
                             _childStreamingText.value = currentText
                             loadChildMessages(event.properties.sessionID)
                        }
                    }
                    is ServerEvent.SessionUpdated -> {
                        val session = event.properties.info
                        if (session.id == sessionId) {
                            _state.value = _state.value.copy(
                                session = session,
                                isSessionIdle = session.status == SessionStatus.IDLE
                            )
                        } else if (session.parentID == sessionId) {
                            // New or updated child
                            val currentChildren = _state.value.childSessions.toMutableMap()
                            val isNew = !currentChildren.containsKey(session.id)
                            currentChildren[session.id] = session
                            _state.value = _state.value.copy(childSessions = currentChildren)
                            
                            if (isNew) {
                                eventStreamRepository.monitorSession(session.id)
                                loadChildMessages(session.id)
                            }
                        }
                    }
                    is ServerEvent.SessionError -> {
                        if (event.properties.sessionID == sessionId) {
                            _state.value = _state.value.copy(
                                error = event.properties.error?.message ?: "Session error",
                                isSending = false,
                                isSessionIdle = true
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun loadConfig() {
        screenModelScope.launch(Dispatchers.IO) {
            sessionRepository.loadDefaultConfig()
            // Fetch config to get model/agent names for display
            sessionRepository.getCurrentModelInfo()?.let { (modelName, agentName) ->
                _state.value = _state.value.copy(
                    modelName = modelName,
                    agentName = agentName
                )
            }
        }
    }
    
    private fun loadMessages() {
        screenModelScope.launch {
            sessionRepository.getMessages(sessionId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(
                            isLoading = true,
                            messages = resource.data ?: _state.value.messages
                        )
                    }
                    is Resource.Success -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            messages = resource.data,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = resource.message,
                            messages = resource.data ?: _state.value.messages
                        )
                    }
                }
            }
        }
    }
    
    private fun connectToEventStream() {
        eventStreamRepository.connect(screenModelScope, sessionId)
    }
    
    fun updateInputText(text: String) {
        _inputText.value = text
    }
    
    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return
        
        Napier.i(">>> sendMessage() called with sessionId=$sessionId")
        Napier.i(">>> SSE connectionStatus=${eventStreamRepository.connectionStatus.value}")
        
        // Ensure SSE is connected and monitoring this session
        eventStreamRepository.connect(screenModelScope, sessionId)
        eventStreamRepository.monitorSession(sessionId)
        Napier.i(">>> After connect: monitorSession($sessionId) called")
        
        screenModelScope.launch {
            // Optimistically add user message to UI
            val userMessage = sessionRepository.createLocalUserMessage(sessionId, text)
            _inputText.value = "" // Clear input immediately
            _state.value = _state.value.copy(
                isSending = true,
                isSessionIdle = false,
                messages = _state.value.messages + userMessage
            )
            
            Napier.i(">>> Optimistic message added, calling sendMessageAsync for session $sessionId...")
            
            // Send message async (response comes via SSE)
            sessionRepository.sendMessageAsync(sessionId, text).fold(
                onSuccess = {
                    // Message sent successfully, waiting for SSE events.
                    // Timeout increased to 120s to handle slow server inference.
                    screenModelScope.launch {
                        kotlinx.coroutines.delay(120_000) // 2 minutes timeout
                        
                        // Check if we are still sending AND haven't received any streaming text
                        val currentStreaming = _streamingText.value
                        if (_state.value.isSending && currentStreaming.isEmpty()) {
                            Napier.w("sendMessage timeout reached (120s) - attempting fallback fetch.")
                            
                            var responseFound = false
                            
                            // FALLBACK: Silent SSE stream issue? Try to fetch messages directly.
                            sessionRepository.getMessages(sessionId).collect { resource ->
                                if (resource is Resource.Success) {
                                    val messages = resource.data
                                    val lastMessage = messages.lastOrNull()
                                    // Check if the last message is from the assistant (the response we were waiting for)
                                    if (lastMessage != null && lastMessage.role == MessageRole.ASSISTANT) {
                                        Napier.i("Fallback fetch successful: Response found despite silent stream.")
                                        _state.value = _state.value.copy(
                                            isSending = false,
                                            isSessionIdle = true,
                                            messages = messages
                                        )
                                        responseFound = true
                                        // We found it, no need to process further emissions (if any)
                                        // However, verify if this breaks the flow collection if we don't return.
                                        // Since 'collect' is terminal, we can't easily 'break', but subsequent emissions
                                        // typically won't happen for a simple fetch.
                                    }
                                } else if (resource is Resource.Error) {
                                     Napier.e("Fallback fetch failed: ${resource.message}")
                                }
                            }
                            
                            // If after checking we still haven't found the response, declare timeout
                            if (!responseFound) {
                                Napier.w("Fallback fetch failed to find assistant response.")
                                _state.value = _state.value.copy(
                                    isSending = false,
                                    error = "Request timed out (no response or stream)"
                                )
                            }
                        } else {
                            Napier.i("Timeout check passed or data received.")
                        }
                    }
                },
                onFailure = { error ->
                    Napier.e("sendMessageAsync failed", error)
                    _inputText.value = text // Restore input on failure
                    _state.value = _state.value.copy(
                        isSending = false,
                        isSessionIdle = true,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun abortSession() {
        screenModelScope.launch {
            sessionRepository.abortSession(sessionId).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        isSending = false,
                        isSessionIdle = true
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(error = error.message)
                }
            )
        }
    }
    
    fun approvePermission() {
        val permission = _state.value.pendingPermission ?: return
        screenModelScope.launch {
            sessionRepository.respondToPermission(
                sessionId = permission.sessionId,
                permissionId = permission.id,
                allow = true,
                remember = false
            ).fold(
                onSuccess = {
                    eventStreamRepository.dismissPermission()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(error = error.message)
                }
            )
        }
    }
    
    fun denyPermission() {
        val permission = _state.value.pendingPermission ?: return
        screenModelScope.launch {
            sessionRepository.respondToPermission(
                sessionId = permission.sessionId,
                permissionId = permission.id,
                allow = false,
                remember = false
            ).fold(
                onSuccess = {
                    eventStreamRepository.dismissPermission()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(error = error.message)
                }
            )
        }
    }
    
    fun dismissPermission() {
        eventStreamRepository.dismissPermission()
    }
    
    fun answerQuestion(answers: List<List<String>>) {
        val question = _state.value.pendingQuestion ?: return
        screenModelScope.launch {
            sessionRepository.replyToQuestion(
                requestId = question.id,
                answers = answers
            ).fold(
                onSuccess = {
                    eventStreamRepository.dismissQuestion()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(error = error.message)
                }
            )
        }
    }
    
    fun rejectQuestion() {
        val question = _state.value.pendingQuestion ?: return
        screenModelScope.launch {
            sessionRepository.rejectQuestion(
                requestId = question.id
            ).fold(
                onSuccess = {
                    eventStreamRepository.dismissQuestion()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(error = error.message)
                }
            )
        }
    }
    
    fun forkSession(message: Message) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            sessionRepository.forkFromMessage(sessionId, message.id).fold(
                onSuccess = { newSession ->
                    _state.value = _state.value.copy(isLoading = false)
                    _navigationEvent.emit(newSession.id)
                },
                onFailure = { error ->
                     _state.value = _state.value.copy(isLoading = false, error = error.message)
                }
            )
        }
    }

    fun revertSession(message: Message) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            sessionRepository.revertToMessage(sessionId, message.id).fold(
                onSuccess = {
                    _state.value = _state.value.copy(isLoading = false)
                    // Reload messages after revert
                    loadMessages()
                    // Reload session info to update reverted status
                    // sessionRepository.getSession(sessionId) ... usually updated via SSE but we can force fetch
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(isLoading = false, error = error.message)
                }
            )
        }
    }

    fun unrevertSession() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            sessionRepository.unrevertSession(sessionId).fold(
                onSuccess = {
                    _state.value = _state.value.copy(isLoading = false)
                    loadMessages()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(isLoading = false, error = error.message)
                }
            )
        }
    }

    fun retry() {
        loadMessages()
        connectToEventStream()
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    override fun onDispose() {
        eventStreamRepository.disconnect()
    }
}
