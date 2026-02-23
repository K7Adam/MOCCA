package com.mocca.app.ui.screens.chat

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.*
import com.mocca.app.domain.model.*
import com.mocca.app.ui.screens.chat.delegates.*
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
    val commands: ImmutableList<Command> = persistentListOf(),
    val recentModels: ImmutableList<RecentModel> = persistentListOf(),
    val todos: ImmutableList<Todo> = persistentListOf(),
    val showTodoPanel: Boolean = false,
    val maxTokens: Int = 0,
    val showTimestamps: Boolean = true,
    val showTokenCounts: Boolean = true
) {
    val totalInputTokens: Int by lazy { messages.filter { it.role == MessageRole.ASSISTANT }.sumOf { it.tokens?.input ?: 0 } }
    val totalOutputTokens: Int by lazy { messages.filter { it.role == MessageRole.ASSISTANT }.sumOf { it.tokens?.output ?: 0 } }
    val lastTurnInputTokens: Int by lazy { messages.lastOrNull { it.role == MessageRole.ASSISTANT && it.tokens != null }?.tokens?.input ?: 0 }
    val contextWindowUsage: Int by lazy {
        messages.lastOrNull { it.role == MessageRole.ASSISTANT && it.tokens != null }?.let { msg ->
            (msg.tokens?.input ?: 0) + (msg.tokens?.output ?: 0)
        } ?: 0
    }
        
    val availableVariants: ImmutableList<String> get() {
        if (providerInfo == null || selectedProviderId.isEmpty() || selectedModelId.isEmpty()) return persistentListOf()
        val provider = providerInfo.all.find { it.id == selectedProviderId } ?: return persistentListOf()
        val modelsObj = provider.models as? kotlinx.serialization.json.JsonObject ?: return persistentListOf()
        val modelObj = modelsObj[selectedModelId] as? kotlinx.serialization.json.JsonObject ?: return persistentListOf()
        val variantsObj = modelObj["variants"] as? kotlinx.serialization.json.JsonObject ?: return persistentListOf()
        return variantsObj.keys.toList().sorted().toImmutableList()
    }
}

class ChatScreenModel(
    initialSessionId: String?,
    private val sessionRepository: SessionRepository,
    private val stateCoordinator: StateCoordinator,
    private val commandRepository: CommandRepository,
    private val agentRepository: AgentRepository,
    private val appStateStore: AppStateStore,
    private val chatStateStore: ChatStateStore
) : ScreenModel {
    
    private val configDelegate: ChatConfigDelegate by lazy { 
        ChatConfigDelegateImpl(appStateStore, sessionRepository, agentRepository, screenModelScope) 
    }
    
    val aggregatedMessages: StateFlow<ImmutableList<Message>> = combine(
        chatStateStore.messages,
        chatStateStore.childSessions,
        chatStateStore.childMessages
    ) { msgs, children, childMsgs ->
        val rootMessages = msgs.toMutableList()
        val syntheticMessages = children.values.map { child ->
            Message(
                id = "child-${child.id}",
                sessionId = child.parentID ?: "",
                role = MessageRole.ASSISTANT,
                parts = listOf(MessagePart.SubTask(
                    sessionId = child.id,
                    title = child.title ?: "Sub-task",
                    status = child.status,
                    messages = childMsgs[child.id] ?: emptyList(),
                    streamingText = ""
                )),
                createdAt = child.createdAt
            )
        }
        (rootMessages + syntheticMessages).sortedBy { it.createdAt }.toImmutableList()
    }.stateIn(screenModelScope, SharingStarted.Eagerly, persistentListOf())

    private val _state = MutableStateFlow(ChatState(sessionId = initialSessionId ?: ""))
    val state: StateFlow<ChatState> = _state.asStateFlow()
    
    private val sessionId get() = _state.value.sessionId

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    val streamingText = chatStateStore.streamingText

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
                chatStateStore.messages,
                chatStateStore.childSessions,
                chatStateStore.childMessages,
                chatStateStore.childStreamingText,
                chatStateStore.isThinking,
                chatStateStore.thinkingContent,
                chatStateStore.thinkingElapsedMs,
                chatStateStore.pendingPermission,
                chatStateStore.pendingQuestion,
                stateCoordinator.connectionStatus,
                configDelegate.providerInfo,
                configDelegate.selectedProviderId,
                configDelegate.selectedModelId,
                configDelegate.selectedVariantId,
                configDelegate.modes,
                configDelegate.selectedModeId,
                configDelegate.modelName,
                configDelegate.agentName,
                configDelegate.maxTokens,
                configDelegate.recentModels,
                configDelegate.commands,
                chatStateStore.todos,
                appStateStore.userPreferences,
                // CRITICAL: Sync loading states from ChatStateStore
                chatStateStore.isLoading,
                chatStateStore.isSending,
                chatStateStore.isSessionIdle,
                chatStateStore.error
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                val prefs = args[22] as UserPreferences
                _state.value.copy(
                    messages = (args[0] as List<Message>).toImmutableList(),
                    childSessions = (args[1] as Map<String, Session>).toImmutableMap(),
                    childMessages = (args[2] as Map<String, List<Message>>).mapValues { it.value.toImmutableList() }.toImmutableMap(),
                    childStreamingText = (args[3] as Map<String, String>).toImmutableMap(),
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
                    recentModels = args[19] as ImmutableList<RecentModel>,
                    commands = args[20] as ImmutableList<Command>,
                    todos = (args[21] as List<Todo>).toImmutableList(),
                    showTimestamps = prefs.showTimestamps,
                    showTokenCounts = prefs.showTokenCounts,
                    // CRITICAL: Sync loading states from ChatStateStore
                    isLoading = args[23] as Boolean,
                    isSending = args[24] as Boolean,
                    isSessionIdle = args[25] as Boolean,
                    error = args[26] as String?
                )
            }.collect { newState ->
                _state.update { newState }
            }
        }
    }

    fun loadSession(newSessionId: String) {
        if (_state.value.sessionId == newSessionId && _state.value.messages.isNotEmpty()) return
        // Note: isLoading is now synced from ChatStateStore, no need to set here
        _state.update { it.copy(sessionId = newSessionId) }
        _inputText.value = ""
        screenModelScope.launch { chatStateStore.loadSession(newSessionId) }
    }

    private fun loadMessages() {
        chatStateStore.loadMoreMessages()
        _state.update { it.copy(isSending = false, isSessionIdle = chatStateStore.isSessionIdle.value) }
    }

    fun loadMoreMessages() = chatStateStore.loadMoreMessages()

    private fun loadTodos() {
        chatStateStore.refreshTodos()
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

    fun executeCommand(cmd: Command) {
        screenModelScope.launch {
            _inputText.value = ""
            _state.update { it.copy(isSending = true, isSessionIdle = false) }
            if (sessionRepository.executeCommand(sessionId, cmd.name, null) is Resource.Error) {
                _state.update { it.copy(isSending = false, isSessionIdle = true, error = "Command failed") }
            }
        }
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return
        
        if (text.startsWith("/")) {
            val parts = text.drop(1).split(" ", limit = 2)
            val command = parts.getOrNull(0) ?: ""
            val args = parts.getOrNull(1)
            
            if (command.isNotBlank()) {
                screenModelScope.launch {
                    val userMessage = sessionRepository.createLocalUserMessage(sessionId, text)
                    _inputText.value = ""
                    _state.update { it.copy(isSending = true, isSessionIdle = false) }
                    if (sessionRepository.executeCommand(sessionId, command, args) is Resource.Error) {
                        _state.update { it.copy(isSending = false, isSessionIdle = true, error = "Command failed") }
                    }
                }
                return
            }
        }
        
        screenModelScope.launch {
            _inputText.value = ""
            val currentAttachments = _state.value.attachedFiles
            clearAttachments()
            
            val result = chatStateStore.sendMessage(
                text = text,
                mode = _state.value.selectedModeId,
                variant = _state.value.selectedVariantId,
                attachments = currentAttachments,
                modelId = _state.value.selectedModelId.ifEmpty { null },
                providerId = _state.value.selectedProviderId.ifEmpty { null }
            )
            
            if (result.isFailure) {
                _inputText.value = text // Restore text on failure
            }
        }
    }

    fun abortSession() {
        screenModelScope.launch { chatStateStore.abortSession() }
    }

    fun approvePermission() {
        screenModelScope.launch { chatStateStore.approvePermission() }
    }

    fun denyPermission() {
        screenModelScope.launch { chatStateStore.denyPermission() }
    }

    fun dismissPermission() = chatStateStore.dismissPermission()

    fun answerQuestion(a: List<List<String>>) {
        screenModelScope.launch { chatStateStore.answerQuestion(a) }
    }

    fun rejectQuestion() {
        screenModelScope.launch { chatStateStore.rejectQuestion() }
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
            // isLoading is now synced from ChatStateStore
            loadMessages()
            configDelegate.loadConfig()
            configDelegate.loadRecentModels()
            loadTodos()
        }
    }

    fun retry() {
        clearError()
        screenModelScope.launch { chatStateStore.loadSession(sessionId, forceReload = true) }
    }

    fun clearError() { _state.update { it.copy(error = null) } }
    
    override fun onDispose() {
        // StateCoordinator manages SSE lifecycle, no need to disconnect here
        // The session will remain monitored for notifications
    }
}
