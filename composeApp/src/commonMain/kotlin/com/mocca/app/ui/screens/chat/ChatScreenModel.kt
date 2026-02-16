package com.mocca.app.ui.screens.chat

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.EventStreamRepository
import com.mocca.app.data.repository.SessionRepository
import com.mocca.app.data.repository.CommandRepository
import com.mocca.app.data.repository.AgentRepository
import com.mocca.app.domain.model.*
import com.mocca.app.util.TerminalCommand
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull

@Immutable
data class ChatState(
    val sessionId: String = "",
    val session: Session? = null,
    val messages: List<Message> = emptyList(),
    val childSessions: Map<String, Session> = emptyMap(),
    val childMessages: Map<String, List<Message>> = emptyMap(),
    val childStreamingText: Map<String, String> = emptyMap(),
    val streamingText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val pendingPermission: PermissionRequest? = null,
    val pendingQuestion: QuestionRequest? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected(),
    val isSessionIdle: Boolean = true,
    val modelName: String = "--",
    val agentName: String = "--",
    val isThinking: Boolean = false,
    val thinkingContent: String = "",
    val thinkingElapsedMs: Long = 0,
    val providerInfo: ProviderResponse? = null,
    val selectedProviderId: String = "",
    val selectedModelId: String = "",
    val modes: List<Mode> = emptyList(),
    val selectedModeId: String? = null,
    val attachedFiles: List<AttachedFile> = emptyList(),
    val commands: List<TerminalCommand> = emptyList(),
    val recentModels: List<RecentModel> = emptyList(),
    val todos: List<Todo> = emptyList(),
    val showTodoPanel: Boolean = false,
    val maxTokens: Int = 0
) {
    /** Total input tokens consumed across all assistant messages in this session. */
    val totalInputTokens: Int get() = messages
        .filter { it.role == MessageRole.ASSISTANT }
        .sumOf { it.tokens?.input ?: 0 }
    
    /** Total output tokens produced across all assistant messages in this session. */
    val totalOutputTokens: Int get() = messages
        .filter { it.role == MessageRole.ASSISTANT }
        .sumOf { it.tokens?.output ?: 0 }
    
    /** Last assistant message's input tokens — represents context window usage for the most recent turn. */
    val lastTurnInputTokens: Int get() = messages
        .lastOrNull { it.role == MessageRole.ASSISTANT && it.tokens != null }
        ?.tokens?.input ?: 0
}

class ChatScreenModel(
    initialSessionId: String?,
    private val sessionRepository: SessionRepository,
    private val eventStreamRepository: EventStreamRepository,
    private val commandRepository: CommandRepository,
    private val agentRepository: AgentRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(ChatState(sessionId = initialSessionId ?: ""))
    val state: StateFlow<ChatState> = _state.asStateFlow()
    
    private val sessionId get() = _state.value.sessionId

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()
    
    private val _childStreamingText = MutableStateFlow<Map<String, String>>(emptyMap())
    val childStreamingText: StateFlow<Map<String, String>> = _childStreamingText.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    private var messagesJob: Job? = null
    private var currentLimit = 50L

    val aggregatedMessages: StateFlow<ImmutableList<Message>> = _state
        .map { s -> s.messages to s.childSessions }
        .distinctUntilChanged()
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
                        streamingText = ""
                    )),
                    createdAt = child.createdAt
                )
            }
            (rootMessages + syntheticMessages).sortedBy { it.createdAt }.toImmutableList()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())
    
    init {
        loadConfig()
        loadRecentModels()
        loadCommands()
        if (initialSessionId != null) {
            connectToEventStream()
            loadSession(initialSessionId)
        }
    }
    
    private fun loadRecentModels() {
        screenModelScope.launch {
            val recent = sessionRepository.getRecentModels()
            _state.value = _state.value.copy(recentModels = recent)
        }
    }

    fun loadSession(newSessionId: String) {
        if (_state.value.sessionId == newSessionId && _state.value.messages.isNotEmpty()) return

        Napier.i("ChatScreenModel switching to session: $newSessionId")
        
        // Preserve provider/model state across session switches to keep the model selector functional.
        // These are global configuration values that don't change per-session.
        val currentState = _state.value
        _state.value = ChatState(
            sessionId = newSessionId,
            isLoading = true,
            connectionStatus = eventStreamRepository.connectionStatus.value,
            // Preserve display names
            modelName = currentState.modelName,
            agentName = currentState.agentName,
            // CRITICAL: Preserve provider info so model selector remains clickable
            providerInfo = currentState.providerInfo,
            selectedProviderId = currentState.selectedProviderId,
            selectedModelId = currentState.selectedModelId,
            // Preserve modes and recent models (global config, not session-specific)
            modes = currentState.modes,
            selectedModeId = currentState.selectedModeId,
            recentModels = currentState.recentModels,
            // Preserve commands (global, not session-specific)
            commands = currentState.commands
        )
        _inputText.value = ""
        _streamingText.value = ""
        _childStreamingText.value = emptyMap()

        connectToEventStream()
        loadMessages()
        loadChildren()
        loadTodos()
    }
    
    private fun loadTodos() {
        val currentSessionId = _state.value.sessionId
        if (currentSessionId.isEmpty()) return
        
        screenModelScope.launch {
            sessionRepository.getSessionTodos(currentSessionId).collect { resource ->
                if (resource is Resource.Success) {
                    _state.value = _state.value.copy(todos = resource.data)
                }
            }
        }
    }
    
    fun toggleTodoPanel() {
        _state.value = _state.value.copy(showTodoPanel = !_state.value.showTodoPanel)
        if (_state.value.showTodoPanel) {
            loadTodos()
        }
    }

    fun shareSession() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = sessionRepository.shareSession(sessionId)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(isLoading = false, session = result.data)
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
                else -> {}
            }
        }
    }

    fun unshareSession() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = sessionRepository.unshareSession(sessionId)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(isLoading = false, session = result.data)
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
                else -> {}
            }
        }
    }

    fun summarizeSession() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = sessionRepository.summarizeSession(sessionId)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(isLoading = false, session = result.data)
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
                else -> {}
            }
        }
    }

    fun initSession(providerId: String, modelId: String) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val initMsg = "Initialize Project"
            
            val sendResult = sessionRepository.sendMessage(sessionId, initMsg)
            if (sendResult.isSuccess) {
                val message = sendResult.getOrThrow()
                when (val result = sessionRepository.initializeProject(
                    sessionId = sessionId,
                    messageId = message.id,
                    providerId = providerId,
                    modelId = modelId
                )) {
                    is Resource.Success -> {
                        _state.value = _state.value.copy(isLoading = false)
                        loadMessages()
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(isLoading = false, error = result.message)
                    }
                    else -> {}
                }
            } else {
                 val error = sendResult.exceptionOrNull()
                 _state.value = _state.value.copy(isLoading = false, error = error?.message)
            }
        }
    }
    
    private fun loadChildren() {
        val currentSessionId = _state.value.sessionId
        if (currentSessionId.isEmpty()) return
        
        screenModelScope.launch {
            sessionRepository.getChildren(currentSessionId).onSuccess { children ->
                val childMap = children.associateBy { it.id }
                _state.value = _state.value.copy(childSessions = childMap)
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
        screenModelScope.launch {
            eventStreamRepository.connectionStatus.collect { status ->
                _state.value = _state.value.copy(connectionStatus = status)
            }
        }
        
        screenModelScope.launch {
            @OptIn(FlowPreview::class)
            eventStreamRepository.streamingText
                .sample(50)
                .collect { text -> _streamingText.value = text }
        }
        
        screenModelScope.launch {
            eventStreamRepository.isThinking.collect { isThinking ->
                _state.value = _state.value.copy(isThinking = isThinking)
            }
        }
        
        screenModelScope.launch {
            eventStreamRepository.thinkingContent.collect { content ->
                _state.value = _state.value.copy(thinkingContent = content)
            }
        }
        
        screenModelScope.launch {
            eventStreamRepository.thinkingStartTime.collect { startTime ->
                if (startTime != null) {
                    while (eventStreamRepository.isThinking.value) {
                        val elapsed = System.currentTimeMillis() - startTime
                        _state.value = _state.value.copy(thinkingElapsedMs = elapsed)
                        kotlinx.coroutines.delay(100)
                    }
                } else {
                    _state.value = _state.value.copy(thinkingElapsedMs = 0)
                }
            }
        }
        
        screenModelScope.launch {
            eventStreamRepository.pendingPermission.collect { permission ->
                _state.value = _state.value.copy(pendingPermission = permission)
            }
        }
        
        screenModelScope.launch {
            eventStreamRepository.pendingQuestion.collect { question ->
                _state.value = _state.value.copy(pendingQuestion = question)
            }
        }
        
        screenModelScope.launch {
            eventStreamRepository.eventsForMonitoredSessions().collect { event ->
                Napier.d("ChatScreenModel received: ${event.type}")
                when (event) {
                    is ServerEvent.MessageUpdated -> {
                        if (event.properties.info.sessionID == sessionId) {
                            Napier.i("Message complete, reloading")
                            _state.value = _state.value.copy(isSending = false, isSessionIdle = true)
                            loadMessages()
                        } else if (_state.value.childSessions.containsKey(event.properties.info.sessionID)) {
                            loadChildMessages(event.properties.info.sessionID)
                        }
                    }
                    is ServerEvent.MessagePartUpdated -> {
                        val part = event.properties.part
                        val delta = event.properties.delta
                        
                        if (part.sessionID != sessionId && _state.value.childSessions.containsKey(part.sessionID)) {
                            if (part.type == "text") {
                                 val currentText = _childStreamingText.value.toMutableMap()
                                 val existing = currentText[part.sessionID] ?: ""
                                 
                                 if (existing.isEmpty() && !part.text.isNullOrEmpty()) {
                                     currentText[part.sessionID] = part.text
                                 } else if (delta != null) {
                                     currentText[part.sessionID] = existing + delta
                                 } else if (!part.text.isNullOrEmpty()) {
                                     currentText[part.sessionID] = part.text
                                 }
                                 
                                 _childStreamingText.value = currentText
                            }
                        }
                    }
                    is ServerEvent.SessionIdle -> {
                        if (event.properties.sessionID == sessionId) {
                            _state.value = _state.value.copy(isSessionIdle = true, isSending = false)
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
                            _state.value = _state.value.copy(session = session, isSessionIdle = session.status == SessionStatus.IDLE)
                        } else if (session.parentID == sessionId) {
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
            sessionRepository.getProviderInfo().onSuccess { providerResponse ->
                val (defaultModelId, defaultProviderId) = sessionRepository.getDefaultModelProvider()
                
                // Try to restore session model from history now that we have provider info
                // (This handles the case where DB loaded messages before network loaded providers)
                var restoredProviderId = ""
                var restoredModelId = ""
                
                val lastModelId = _state.value.messages.findLast { 
                    (it.role == MessageRole.ASSISTANT || it.role == MessageRole.USER) && !it.model.isNullOrBlank() 
                }?.model
                
                if (!lastModelId.isNullOrBlank()) {
                     val provider = providerResponse.all.find { it.models.toString().contains(lastModelId) }
                     if (provider != null) {
                         restoredProviderId = provider.id
                         restoredModelId = lastModelId
                         Napier.i("ChatScreenModel: Restored model from history in loadConfig: $restoredProviderId/$restoredModelId")
                     }
                }
                
                // If we already have a selection (from user or previous restore), keep it. 
                // Otherwise use restored. Otherwise use default.
                val currentProviderId = _state.value.selectedProviderId
                val currentModelId = _state.value.selectedModelId
                
                val finalProviderId = when {
                    currentProviderId.isNotEmpty() -> currentProviderId
                    restoredProviderId.isNotEmpty() -> restoredProviderId
                    else -> defaultProviderId
                }
                
                val finalModelId = when {
                    currentModelId.isNotEmpty() -> currentModelId
                    restoredModelId.isNotEmpty() -> restoredModelId
                    else -> defaultModelId
                }
                
                _state.value = _state.value.copy(
                    providerInfo = providerResponse,
                    selectedProviderId = finalProviderId,
                    selectedModelId = finalModelId
                )
                
                 if (finalModelId.isNotEmpty()) {
                    val modelName = finalModelId.uppercase().replace("-", " ").take(30)
                    
                    // Parse maxTokens from ProviderModel.limit
                    val contextTokens = parseContextLimit(providerResponse, finalProviderId, finalModelId)
                    
                    _state.value = _state.value.copy(
                        modelName = modelName,
                        maxTokens = contextTokens
                    )
                }
            }
            sessionRepository.getModes().onSuccess { modes ->
                // Legacy mode fetch - keep as fallback or remove if unused
            }
            
            // Fetch real agents (Sisyphus, etc.) and map to modes
            agentRepository.getAgents().collect { resource ->
                if (resource is Resource.Success) {
                    val agents = resource.data
                    val modes = agents.filter { !it.hidden }.map { agent ->
                        Mode(
                            id = agent.name,
                            name = agent.name,
                            description = agent.description
                        )
                    }
                    
                    val defaultMode = sessionRepository.getDefaultMode()
                    // If current selection is invalid, reset to default or first available
                    val currentSelection = _state.value.selectedModeId
                    val newSelection = if (modes.any { it.id == currentSelection }) {
                        currentSelection
                    } else {
                        defaultMode
                    }
                    
                    _state.value = _state.value.copy(modes = modes, selectedModeId = newSelection)
                    
                    // Update agent name display
                    if (newSelection != null) {
                        val modeName = modes.find { it.id == newSelection }?.name ?: newSelection.uppercase()
                        _state.value = _state.value.copy(agentName = modeName.uppercase())
                    }
                }
            }

            sessionRepository.getCurrentModelInfo()?.let { (modelName, agentName) ->
                _state.value = _state.value.copy(modelName = modelName, agentName = agentName)
            }
            loadCommands()
        }
    }
    
    private fun loadCommands() {
        screenModelScope.launch {
            commandRepository.getCommands().collect { resource ->
                if (resource is Resource.Success) {
                    val remoteCommands = resource.data.map { cmd ->
                        TerminalCommand(
                            trigger = cmd.name,
                            description = cmd.description ?: "",
                            action = { 
                                updateInputText("/${cmd.name}")
                                sendMessage() 
                            }
                        )
                    }
                    _state.value = _state.value.copy(commands = remoteCommands)
                }
            }
        }
    }
    
    private fun loadMessages() {
        messagesJob?.cancel()
        messagesJob = screenModelScope.launch {
            sessionRepository.getMessages(sessionId, currentLimit).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(isLoading = true, messages = resource.data ?: _state.value.messages)
                    }
                    is Resource.Success -> {
                        val messages = resource.data
                        var updatedState = _state.value.copy(isLoading = false, messages = messages, error = null)

                        // Restore session model from history if we haven't manually selected one yet (or just switched session)
                        // logic: Find last assistant message with a modelID
                        val lastModelId = messages.findLast { it.role == MessageRole.ASSISTANT && !it.model.isNullOrBlank() }?.model
                        
                        if (!lastModelId.isNullOrBlank()) {
                            // We have a model ID (e.g. "claude-sonnet-4-5") but need the provider ID
                            // Look it up in our loaded provider info
                            val providers = _state.value.providerInfo?.all ?: emptyList()
                            val provider = providers.find { provider -> 
                                provider.models.toString().contains(lastModelId) 
                            }

                            if (provider != null) {
                                val modelName = lastModelId.uppercase().replace("-", " ").take(30)
                                val contextTokens = parseContextLimit(resource.data.let { _state.value.providerInfo!! }, provider.id, lastModelId)
                                updatedState = updatedState.copy(
                                    selectedModelId = lastModelId,
                                    selectedProviderId = provider.id,
                                    modelName = modelName,
                                    maxTokens = contextTokens
                                )
                                Napier.i("Restored session model: ${provider.id} / $lastModelId")
                            }
                        }
                        
                        _state.value = updatedState
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(isLoading = false, error = resource.message, messages = resource.data ?: _state.value.messages)
                    }
                }
            }
        }
    }

    fun loadMoreMessages() {
        if (_state.value.isLoading) return
        currentLimit += 50
        loadMessages()
    }
    
    private fun connectToEventStream() {
        eventStreamRepository.connect(screenModelScope, sessionId)
    }
    
    fun updateInputText(text: String) {
        _inputText.value = text
    }
    
    fun selectModel(providerId: String, modelId: String) {
        sessionRepository.setDefaultModel(modelId, providerId)
        val modelName = modelId.uppercase().replace("-", " ").take(30)
        
        // Parse maxTokens from ProviderModel.limit
        val contextTokens = _state.value.providerInfo?.let { 
            parseContextLimit(it, providerId, modelId) 
        } ?: 0
        
        _state.value = _state.value.copy(
            selectedProviderId = providerId, 
            selectedModelId = modelId, 
            modelName = modelName,
            maxTokens = contextTokens
        )
        screenModelScope.launch {
            sessionRepository.addRecentModel(providerId, modelId)
            loadRecentModels()
        }
    }
    
    fun selectMode(modeId: String?) {
        val newModeId = modeId ?: "build"
        sessionRepository.setDefaultMode(newModeId)
        val modeName = _state.value.modes.find { it.id == newModeId }?.name ?: newModeId.uppercase()
        _state.value = _state.value.copy(selectedModeId = newModeId, agentName = modeName.uppercase())
    }
    
    fun addAttachment(file: AttachedFile) {
        val current = _state.value.attachedFiles
        if (current.none { it.id == file.id }) {
            _state.value = _state.value.copy(attachedFiles = current + file)
        }
    }
    
    fun removeAttachment(file: AttachedFile) {
        _state.value = _state.value.copy(attachedFiles = _state.value.attachedFiles.filter { it.id != file.id })
    }
    
    fun clearAttachments() {
        _state.value = _state.value.copy(attachedFiles = emptyList())
    }
    
    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return
        
        val attachments = _state.value.attachedFiles
        val selectedMode = _state.value.selectedModeId
        val selectedModel = _state.value.selectedModelId
        val selectedProvider = _state.value.selectedProviderId
        
        if (text.startsWith("/")) {
            val parts = text.drop(1).split(" ", limit = 2)
            val command = parts.getOrNull(0) ?: ""
            val args = parts.getOrNull(1)
            
            if (command.isNotBlank()) {
                screenModelScope.launch {
                    val userMessage = sessionRepository.createLocalUserMessage(sessionId, text)
                    _inputText.value = ""
                    _state.value = _state.value.copy(isSending = true, isSessionIdle = false, messages = _state.value.messages + userMessage)
                    
                    when (val result = sessionRepository.executeCommand(sessionId, command, args)) {
                        is Resource.Success -> { /* OK */ }
                        is Resource.Error -> {
                            _state.value = _state.value.copy(isSending = false, isSessionIdle = true, error = "Command failed: ${result.message}")
                        }
                        else -> {}
                    }
                }
                return
            }
        }
        
        eventStreamRepository.connect(screenModelScope, sessionId)
        eventStreamRepository.monitorSession(sessionId)
        
        screenModelScope.launch {
            val userMessage = sessionRepository.createLocalUserMessage(sessionId, text)
            _inputText.value = ""
            clearAttachments()
            _state.value = _state.value.copy(isSending = true, isSessionIdle = false, messages = _state.value.messages + userMessage)
            
            val result = sessionRepository.sendMessageAsync(
                sessionId = sessionId,
                text = text,
                mode = selectedMode,
                attachments = attachments,
                modelId = selectedModel.ifEmpty { null },
                providerId = selectedProvider.ifEmpty { null }
            )
            
            if (result.isSuccess) {
                screenModelScope.launch {
                    kotlinx.coroutines.delay(120_000)
                    val currentStreaming = _streamingText.value
                    if (_state.value.isSending && currentStreaming.isEmpty()) {
                        sessionRepository.getMessages(sessionId).collect { resource ->
                            if (resource is Resource.Success) {
                                val messages = resource.data
                                val lastMessage = messages.lastOrNull()
                                if (lastMessage != null && lastMessage.role == MessageRole.ASSISTANT) {
                                    _state.value = _state.value.copy(isSending = false, isSessionIdle = true, messages = messages)
                                }
                            }
                        }
                    }
                }
            } else {
                val error = result.exceptionOrNull()
                _inputText.value = text
                _state.value = _state.value.copy(isSending = false, isSessionIdle = true, error = error?.message)
            }
        }
    }
    
    fun abortSession() {
        screenModelScope.launch {
            val result = sessionRepository.abortSession(sessionId)
            if (result.isSuccess) {
                _state.value = _state.value.copy(isSending = false, isSessionIdle = true)
            } else {
                val error = result.exceptionOrNull()
                _state.value = _state.value.copy(error = error?.message)
            }
        }
    }
    
    fun approvePermission() {
        val permission = _state.value.pendingPermission ?: return
        screenModelScope.launch {
            val result = sessionRepository.respondToPermission(
                sessionId = permission.sessionId,
                permissionId = permission.id,
                allow = true,
                remember = false
            )
            if (result.isSuccess) {
                eventStreamRepository.dismissPermission()
            } else {
                val error = result.exceptionOrNull()
                _state.value = _state.value.copy(error = error?.message)
            }
        }
    }
    
    fun denyPermission() {
        val permission = _state.value.pendingPermission ?: return
        screenModelScope.launch {
            val result = sessionRepository.respondToPermission(
                sessionId = permission.sessionId,
                permissionId = permission.id,
                allow = false,
                remember = false
            )
            if (result.isSuccess) {
                eventStreamRepository.dismissPermission()
            } else {
                val error = result.exceptionOrNull()
                _state.value = _state.value.copy(error = error?.message)
            }
        }
    }
    
    fun dismissPermission() {
        eventStreamRepository.dismissPermission()
    }
    
    fun answerQuestion(answers: List<List<String>>) {
        val question = _state.value.pendingQuestion ?: return
        screenModelScope.launch {
            val result = sessionRepository.replyToQuestion(
                requestId = question.id,
                answers = answers
            )
            if (result.isSuccess) {
                eventStreamRepository.dismissQuestion()
            } else {
                val error = result.exceptionOrNull()
                _state.value = _state.value.copy(error = error?.message)
            }
        }
    }
    
    fun rejectQuestion() {
        val question = _state.value.pendingQuestion ?: return
        screenModelScope.launch {
            val result = sessionRepository.rejectQuestion(requestId = question.id)
            if (result.isSuccess) {
                eventStreamRepository.dismissQuestion()
            } else {
                val error = result.exceptionOrNull()
                _state.value = _state.value.copy(error = error?.message)
            }
        }
    }
    
    fun forkSession(message: Message) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = sessionRepository.forkFromMessage(sessionId, message.id)
            if (result.isSuccess) {
                val newSession = result.getOrThrow()
                _state.value = _state.value.copy(isLoading = false)
                _navigationEvent.emit(newSession.id)
            } else {
                 val error = result.exceptionOrNull()
                 _state.value = _state.value.copy(isLoading = false, error = error?.message)
            }
        }
    }
    
    fun executeCommand(command: String) {
        when (command) {
            "clear" -> clearHistory()
            "reset" -> resetSession()
            "settings" -> screenModelScope.launch { /* Navigate to settings */ }
            else -> Napier.w("Unknown command: $command")
        }
    }
    
    private fun clearHistory() {
        screenModelScope.launch {
             _state.value = _state.value.copy(isLoading = true)
            val delResult = sessionRepository.deleteAllSessions()
            if (delResult.isSuccess) {
                val createResult = sessionRepository.createSession()
                if (createResult.isSuccess) {
                     val session = createResult.getOrThrow()
                     _state.value = _state.value.copy(isLoading = false)
                    _navigationEvent.emit(session.id)
                }
            } else {
                val error = delResult.exceptionOrNull()
                _state.value = _state.value.copy(isLoading = false, error = error?.message)
            }
        }
    }
    
    private fun resetSession() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = sessionRepository.createSession()
            if (result.isSuccess) {
                 val session = result.getOrThrow()
                 _state.value = _state.value.copy(isLoading = false)
                _navigationEvent.emit(session.id)
            } else {
                val error = result.exceptionOrNull()
                _state.value = _state.value.copy(isLoading = false, error = error?.message)
            }
        }
    }

    fun revertSession(message: Message) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = sessionRepository.revertToMessage(sessionId, message.id)
            if (result.isSuccess) {
                _state.value = _state.value.copy(isLoading = false)
                loadMessages()
            } else {
                val error = result.exceptionOrNull()
                _state.value = _state.value.copy(isLoading = false, error = error?.message)
            }
        }
    }

    fun unrevertSession() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = sessionRepository.unrevertSession(sessionId)
            if (result.isSuccess) {
                _state.value = _state.value.copy(isLoading = false)
                loadMessages()
            } else {
                val error = result.exceptionOrNull()
                _state.value = _state.value.copy(isLoading = false, error = error?.message)
            }
        }
    }

    fun retry() {
        loadMessages()
        connectToEventStream()
    }
    
    fun refreshData() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                loadMessages()
                loadConfig()
                loadCommands()
                loadRecentModels()
                loadTodos()
                connectToEventStream()
                Napier.i("ChatScreenModel refresh completed")
            } catch (e: Exception) {
                Napier.e("Failed to refresh chat data", e)
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
    
    private fun parseContextLimit(providerResponse: ProviderResponse, providerId: String, modelId: String): Int {
        val provider = providerResponse.all.find { it.id == providerId } ?: return 0
        val modelsObj = provider.models as? JsonObject ?: return 0
        val modelObj = modelsObj[modelId] as? JsonObject ?: return 0
        
        // OpenCode ProviderModel has a 'limit' field which is a JsonObject
        val limit = modelObj["limit"] as? JsonObject ?: return 0
        
        return limit["context"]?.let { (it as? JsonPrimitive)?.intOrNull }
            ?: limit["max_tokens"]?.let { (it as? JsonPrimitive)?.intOrNull }
            ?: limit["context_length"]?.let { (it as? JsonPrimitive)?.intOrNull }
            ?: 0
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    override fun onDispose() {
        eventStreamRepository.disconnect()
    }
}