package com.mocca.app.ui.screens.chat

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.*
import com.mocca.app.domain.model.*
import com.mocca.app.ui.screens.chat.delegates.*
import com.mocca.app.util.TerminalCommand
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

@Immutable
data class ChatState(
    val sessionId: String = "",
    val session: Session? = null,
    val messages: ImmutableList<Message> = persistentListOf(),
    val childSessions: ImmutableMap<String, Session> = persistentMapOf(),
    val childMessages: ImmutableMap<String, ImmutableList<Message>> = persistentMapOf(),
    val childStreamingText: ImmutableMap<String, String> = persistentMapOf(),
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
    val selectedVariantId: String? = null,
    val modes: ImmutableList<Mode> = persistentListOf(),
    val selectedModeId: String? = null,
    val attachedFiles: ImmutableList<AttachedFile> = persistentListOf(),
    val commands: ImmutableList<TerminalCommand> = persistentListOf(),
    val recentModels: ImmutableList<RecentModel> = persistentListOf(),
    val todos: ImmutableList<Todo> = persistentListOf(),
    val showTodoPanel: Boolean = false,
    val maxTokens: Int = 0
) {
    val totalInputTokens: Int by lazy { messages.filter { it.role == MessageRole.ASSISTANT }.sumOf { it.tokens?.input ?: 0 } }
    val totalOutputTokens: Int by lazy { messages.filter { it.role == MessageRole.ASSISTANT }.sumOf { it.tokens?.output ?: 0 } }
    val lastTurnInputTokens: Int by lazy { messages.lastOrNull { it.role == MessageRole.ASSISTANT && it.tokens != null }?.tokens?.input ?: 0 }
        
    val availableVariants: List<String> get() {
        if (providerInfo == null || selectedProviderId.isEmpty() || selectedModelId.isEmpty()) return emptyList()
        val provider = providerInfo.all.find { it.id == selectedProviderId } ?: return emptyList()
        val modelsObj = provider.models as? kotlinx.serialization.json.JsonObject ?: return emptyList()
        val modelObj = modelsObj[selectedModelId] as? kotlinx.serialization.json.JsonObject ?: return emptyList()
        val variantsObj = modelObj["variants"] as? kotlinx.serialization.json.JsonObject ?: return emptyList()
        return variantsObj.keys.toList().sorted()
    }
}

class ChatScreenModel(
    initialSessionId: String?,
    private val sessionRepository: SessionRepository,
    private val stateCoordinator: StateCoordinator,
    private val commandRepository: CommandRepository,
    private val agentRepository: AgentRepository,
    private val appStateStore: AppStateStore
) : ScreenModel {
    
    // Delegate construction using Lazy and screenModelScope
    // IMPORTANT: Now passes appStateStore for single source of truth
    private val messageDelegate: ChatMessageDelegate by lazy { 
        ChatMessageDelegateImpl(sessionRepository, screenModelScope) 
    }
    private val configDelegate: ChatConfigDelegate by lazy { 
        ChatConfigDelegateImpl(appStateStore, sessionRepository, agentRepository, screenModelScope) 
    }
    private val eventDelegate: ChatEventDelegate by lazy { 
        ChatEventDelegateImpl(stateCoordinator, screenModelScope) 
    }

    private val _state = MutableStateFlow(ChatState(sessionId = initialSessionId ?: ""))
    val state: StateFlow<ChatState> = _state.asStateFlow()
    
    private val sessionId get() = _state.value.sessionId

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    val aggregatedMessages: StateFlow<ImmutableList<Message>> by lazy { messageDelegate.aggregatedMessages }
    val streamingText by lazy { eventDelegate.streamingText }

    init {
        syncDelegates()
        // Wait for lazy init
        screenModelScope.launch {
            configDelegate.loadConfig()
            configDelegate.loadRecentModels()
            if (initialSessionId != null) {
                loadSession(initialSessionId)
            }
        }
    }

    private fun syncDelegates() {
        screenModelScope.launch {
            combine(
                messageDelegate.messages,
                messageDelegate.childSessions,
                messageDelegate.childMessages,
                eventDelegate.childStreamingText,
                eventDelegate.isThinking,
                eventDelegate.thinkingContent,
                eventDelegate.thinkingElapsedMs,
                eventDelegate.pendingPermission,
                eventDelegate.pendingQuestion,
                eventDelegate.connectionStatus,
                configDelegate.providerInfo,
                configDelegate.selectedProviderId,
                configDelegate.selectedModelId,
                configDelegate.selectedVariantId,
                configDelegate.modes,
                configDelegate.selectedModeId,
                configDelegate.modelName,
                configDelegate.agentName,
                configDelegate.maxTokens,
                configDelegate.recentModels
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                _state.value.copy(
                    messages = args[0] as ImmutableList<Message>,
                    childSessions = args[1] as ImmutableMap<String, Session>,
                    childMessages = args[2] as ImmutableMap<String, ImmutableList<Message>>,
                    childStreamingText = args[3] as ImmutableMap<String, String>,
                    isThinking = args[4] as Boolean,
                    thinkingContent = args[5] as String,
                    thinkingElapsedMs = args[6] as Long,
                    pendingPermission = args[7] as PermissionRequest?,
                    pendingQuestion = args[8] as QuestionRequest?,
                    connectionStatus = args[9] as ConnectionStatus,
                    providerInfo = args[10] as ProviderResponse?,
                    selectedProviderId = args[11] as String,
                    selectedModelId = args[12] as String,
                    selectedVariantId = args[13] as String?,
                    modes = args[14] as ImmutableList<Mode>,
                    selectedModeId = args[15] as String?,
                    modelName = args[16] as String,
                    agentName = args[17] as String,
                    maxTokens = args[18] as Int,
                    recentModels = args[19] as ImmutableList<RecentModel>
                )
            }.collect { newState ->
                _state.update { newState }
            }
        }
    }

    fun loadSession(newSessionId: String) {
        if (_state.value.sessionId == newSessionId && _state.value.messages.isNotEmpty()) return
        _state.update { it.copy(sessionId = newSessionId, isLoading = true) }
        _inputText.value = ""
        eventDelegate.connectToEventStream(newSessionId)
        eventDelegate.observeEvents(newSessionId, { loadMessages() }, { id -> messageDelegate.loadChildMessages(id) })
        messageDelegate.loadMessages(newSessionId, 50)
        messageDelegate.loadChildren(newSessionId)
        loadTodos()
    }

    private fun loadMessages() {
        messageDelegate.loadMessages(sessionId, 50)
        _state.update { it.copy(isSending = false, isSessionIdle = true) }
    }

    fun loadMoreMessages() = messageDelegate.loadMoreMessages(sessionId)

    private fun loadTodos() {
        screenModelScope.launch {
            sessionRepository.getSessionTodos(sessionId).collect { resource ->
                if (resource is Resource.Success) {
                    _state.update { it.copy(todos = resource.data.toImmutableList()) }
                }
            }
        }
    }

    fun toggleTodoPanel() {
        _state.update { it.copy(showTodoPanel = !it.showTodoPanel) }
        if (_state.value.showTodoPanel) loadTodos()
    }

    fun updateInputText(text: String) { _inputText.value = text }
    fun selectModel(p: String, m: String) = configDelegate.selectModel(p, m)
    fun selectVariant(v: String?) = configDelegate.selectVariant(v)
    fun selectMode(m: String?) = configDelegate.selectMode(m)
    fun addAttachment(f: AttachedFile) = _state.update { it.copy(attachedFiles = (it.attachedFiles + f).toImmutableList()) }
    fun removeAttachment(f: AttachedFile) = _state.update { it.copy(attachedFiles = it.attachedFiles.filter { a -> a.id != f.id }.toImmutableList()) }
    fun clearAttachments() = _state.update { it.copy(attachedFiles = persistentListOf()) }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return
        
        if (text.startsWith("/")) {
            val parts = text.drop(1).split(" ", limit = 2)
            val command = parts.getOrNull(0) ?: ""
            val args = parts.getOrNull(1)
            
            if (command.isNotBlank()) {
                screenModelScope.launch {
                    val userMessage = messageDelegate.createLocalUserMessage(sessionId, text)
                    _inputText.value = ""
                    _state.update { it.copy(isSending = true, isSessionIdle = false) }
                    if (sessionRepository.executeCommand(sessionId, command, args) is Resource.Error) {
                        _state.update { it.copy(isSending = false, isSessionIdle = true, error = "Command failed") }
                    }
                    // Safety timeout for command execution
                    screenModelScope.launch {
                        delay(30_000) // 30 second timeout for commands
                        if (_state.value.isSending && !_state.value.isThinking) {
                            Napier.w("Command isSending stuck for 30s, auto-resetting")
                            _state.update { it.copy(isSending = false, isSessionIdle = true) }
                        }
                    }
                }
                return
            }
        }
        
        screenModelScope.launch {
            messageDelegate.createLocalUserMessage(sessionId, text)
            _inputText.value = ""
            clearAttachments()
            _state.update { it.copy(isSending = true, isSessionIdle = false) }
            
            val result = sessionRepository.sendMessageAsync(
                sessionId = sessionId,
                text = text,
                mode = _state.value.selectedModeId,
                variant = _state.value.selectedVariantId,
                attachments = _state.value.attachedFiles,
                modelId = _state.value.selectedModelId.ifEmpty { null },
                providerId = _state.value.selectedProviderId.ifEmpty { null }
            )
            
            if (!result.isSuccess) {
                _inputText.value = text
                _state.update { it.copy(isSending = false, isSessionIdle = true, error = result.exceptionOrNull()?.message) }
            }
            
            // Safety timeout: reset isSending after 60 seconds if still stuck
            // This prevents permanent loading indicator if SSE events are missed
            screenModelScope.launch {
                delay(60_000) // 60 second timeout
                if (_state.value.isSending && !_state.value.isThinking) {
                    Napier.w("isSending stuck for 60s, auto-resetting")
                    _state.update { it.copy(isSending = false, isSessionIdle = true) }
                }
            }
        }
    }

    fun abortSession() {
        screenModelScope.launch {
            if (sessionRepository.abortSession(sessionId).isSuccess) {
                _state.update { it.copy(isSending = false, isSessionIdle = true) }
            }
        }
    }

    fun approvePermission() {
        val p = _state.value.pendingPermission ?: return
        screenModelScope.launch {
            if (sessionRepository.respondToPermission(p.sessionId, p.id, true).isSuccess) stateCoordinator.dismissPermission()
        }
    }

    fun denyPermission() {
        val p = _state.value.pendingPermission ?: return
        screenModelScope.launch {
            if (sessionRepository.respondToPermission(p.sessionId, p.id, false).isSuccess) stateCoordinator.dismissPermission()
        }
    }

    fun dismissPermission() = stateCoordinator.dismissPermission()

    fun answerQuestion(a: List<List<String>>) {
        val q = _state.value.pendingQuestion ?: return
        screenModelScope.launch {
            if (sessionRepository.replyToQuestion(q.id, a).isSuccess) stateCoordinator.dismissQuestion()
        }
    }

    fun rejectQuestion() {
        val q = _state.value.pendingQuestion ?: return
        screenModelScope.launch {
            if (sessionRepository.rejectQuestion(q.id).isSuccess) stateCoordinator.dismissQuestion()
        }
    }

    fun forkSession(m: Message) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            sessionRepository.forkFromMessage(sessionId, m.id).onSuccess {
                _state.update { s -> s.copy(isLoading = false) }
                _navigationEvent.emit(it.id)
            }
        }
    }

    fun revertSession(m: Message) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            if (sessionRepository.revertToMessage(sessionId, m.id).isSuccess) {
                _state.update { it.copy(isLoading = false) }
                loadMessages()
            }
        }
    }

    fun unrevertSession() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            if (sessionRepository.unrevertSession(sessionId).isSuccess) {
                _state.update { it.copy(isLoading = false) }
                loadMessages()
            }
        }
    }

    fun shareSession() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            sessionRepository.shareSession(sessionId).let { res ->
                if (res is Resource.Success) _state.update { it.copy(isLoading = false, session = res.data) }
            }
        }
    }

    fun unshareSession() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            sessionRepository.unshareSession(sessionId).let { res ->
                if (res is Resource.Success) _state.update { it.copy(isLoading = false, session = res.data) }
            }
        }
    }

    fun refreshData() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            loadMessages()
            configDelegate.loadConfig()
            configDelegate.loadRecentModels()
            loadTodos()
        }
    }

    fun retry() {
        loadMessages()
        eventDelegate.connectToEventStream(sessionId)
    }

    fun clearError() { _state.update { it.copy(error = null) } }
    
    override fun onDispose() {
        // StateCoordinator manages SSE lifecycle, no need to disconnect here
        // The session will remain monitored for notifications
    }
}
