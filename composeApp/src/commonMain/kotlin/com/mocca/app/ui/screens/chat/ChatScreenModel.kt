package com.mocca.app.ui.screens.chat

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.*
import com.mocca.app.domain.model.*
import com.mocca.app.util.ChatExporter
import com.mocca.app.util.VoiceInputProvider
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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
    val agentError: AgentErrorClassifier.AgentError? = null,
    val pendingPermission: PermissionRequest? = null,
    val pendingQuestion: QuestionRequest? = null,
    val pendingPermissions: ImmutableList<PermissionRequest> = persistentListOf(),
    val pendingQuestions: ImmutableList<QuestionRequest> = persistentListOf(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected(),
    val isSessionIdle: Boolean = true,
    val modelName: String = "--",
    val agentName: String = "--",
    val isThinking: Boolean = false,
    val thinkingContent: String = "",
    val thinkingElapsedMs: Long = 0,
    val agentActivity: AgentActivity? = null,
    val aiConfigState: AiConfigState = AiConfigState(),
    val effectiveSelection: AiEffectiveSelection? = null,
    val modelPickerState: ModelPickerUiState = ModelPickerUiState(),
    val variantPickerState: VariantPickerUiState = VariantPickerUiState(),
    val selectedVariantId: String? = null,
    val modes: ImmutableList<Mode> = persistentListOf(),
    val selectedModeId: String? = null,
    val attachedFiles: ImmutableList<AttachedFile> = persistentListOf(),
    val commands: ImmutableList<Command> = persistentListOf(),
    val todos: ImmutableList<Todo> = persistentListOf(),
    val showTodoPanel: Boolean = false,
    val maxTokens: Int = 0,
    val showTimestamps: Boolean = true,
    val showTokenCounts: Boolean = true,
    val isPlanMode: Boolean = false,
    val sessionDisposed: Boolean = false,
    val disposalReason: String? = null
) {
    val totalInputTokens: Int by lazy { messages.filter { it.role == MessageRole.ASSISTANT }.sumOf { it.tokens?.input ?: 0 } }
    val totalOutputTokens: Int by lazy { messages.filter { it.role == MessageRole.ASSISTANT }.sumOf { it.tokens?.output ?: 0 } }
    val lastTurnInputTokens: Int by lazy { messages.lastOrNull { it.role == MessageRole.ASSISTANT && it.tokens != null }?.tokens?.input ?: 0 }
    val contextWindowUsage: Int by lazy {
        messages.lastOrNull { it.role == MessageRole.ASSISTANT && it.tokens != null }?.let { msg ->
            (msg.tokens?.input ?: 0) + (msg.tokens?.output ?: 0)
        } ?: 0
    }
    val availableVariants: ImmutableList<String> by lazy {
        variantPickerState.variants.map { it.id }.toImmutableList()
    }
}

class ChatScreenModel(
    initialSessionId: String?,
    private val sessionRepository: SessionRepository,
    private val stateCoordinator: StateCoordinator,
    private val commandRepository: CommandRepository,
    private val appStateStore: AppStateStore,
    private val chatStateStore: ChatStateStore,
    private val aiRuntimeConfigRepository: AiRuntimeConfigRepository,
    private val voiceInputProvider: VoiceInputProvider
) : ScreenModel {
    
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

    private val _shellMode = MutableStateFlow(false)
    val shellMode: StateFlow<Boolean> = _shellMode.asStateFlow()

    private val _promptHistory = MutableStateFlow<List<String>>(emptyList())
    val promptHistory: StateFlow<List<String>> = _promptHistory.asStateFlow()
    private val _historyIndex = MutableStateFlow(-1)

    /** Holds the (Message, MessagePart) being edited; null when no edit dialog is open. */
    private val _editingPart = MutableStateFlow<Pair<Message, MessagePart>?>(null)
    val editingPart: StateFlow<Pair<Message, MessagePart>?> = _editingPart.asStateFlow()

    /** True when the fork-from-message dialog should be shown. */
    private val _showForkDialog = MutableStateFlow(false)
    val showForkDialog: StateFlow<Boolean> = _showForkDialog.asStateFlow()
    
    private val _showShareDialog = MutableStateFlow(false)
    val showShareDialog: StateFlow<Boolean> = _showShareDialog.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    val voiceInputState: StateFlow<VoiceInputState> = voiceInputProvider.state

    private val _voicePermissionRequestToken = MutableStateFlow(0)
    val voicePermissionRequestToken: StateFlow<Int> = _voicePermissionRequestToken.asStateFlow()

    fun togglePlanMode() {
        val next = if (_state.value.isPlanMode) null else "plan"
        if (next == null || _state.value.modes.any { it.id == next }) {
            selectMode(next)
        }
    }

    fun toggleShellMode() { _shellMode.value = !_shellMode.value }

    fun navigateHistoryUp() {
        val history = _promptHistory.value
        if (history.isEmpty()) return
        val newIndex = (_historyIndex.value + 1).coerceAtMost(history.size - 1)
        _historyIndex.value = newIndex
        _inputText.value = history[newIndex]
    }

    fun navigateHistoryDown() {
        val idx = _historyIndex.value
        if (idx <= 0) {
            _historyIndex.value = -1
            _inputText.value = ""
            return
        }
        val newIndex = idx - 1
        _historyIndex.value = newIndex
        _inputText.value = _promptHistory.value[newIndex]
    }

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    val streamingText = chatStateStore.streamingText

    init {
        syncDelegates()
        // Wait for lazy init
        screenModelScope.launch {
            if (initialSessionId != null) {
                loadSession(initialSessionId)
            }
        }
        // Observe shared content from share-sheet / external sources
        screenModelScope.launch {
            SharedContentBus.sharedContent.collect { content ->
                val current = _inputText.value
                _inputText.value = if (current.isBlank()) content else "$current\n\n$content"
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun syncDelegates() {
        // 1. Session & Message Updates
        screenModelScope.launch {
            combine(
                chatStateStore.messages,
                chatStateStore.childSessions,
                chatStateStore.childMessages,
                chatStateStore.childStreamingText,
                chatStateStore.isLoading,
                chatStateStore.isSending,
                chatStateStore.isSessionIdle,
                chatStateStore.error,
                chatStateStore.sessionDisposed,
                chatStateStore.agentError
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                val msgs = args[0] as List<Message>
                val children = args[1] as Map<String, Session>
                val childMsgs = args[2] as Map<String, List<Message>>
                val childStreaming = args[3] as Map<String, String>
                val loading = args[4] as Boolean
                val sending = args[5] as Boolean
                val idle = args[6] as Boolean
                val err = args[7] as String?
                val disposed = args[8] as String?
                val agentErr = args[9] as AgentErrorClassifier.AgentError?

                _state.update { it.copy(
                    messages = msgs.toImmutableList(),
                    childSessions = children.toImmutableMap(),
                    childMessages = childMsgs.mapValues { m -> m.value.toImmutableList() }.toImmutableMap(),
                    childStreamingText = childStreaming.toImmutableMap(),
                    isLoading = loading,
                    isSending = sending,
                    isSessionIdle = idle,
                    error = err,
                    agentError = agentErr,
                    sessionDisposed = disposed != null,
                    disposalReason = disposed
                ) }
            }.collect()
        }

        // 2. Thinking State Updates
        screenModelScope.launch {
            combine(
                chatStateStore.isThinking,
                chatStateStore.thinkingContent,
                chatStateStore.thinkingElapsedMs
            ) { thinking, content, elapsed ->
                _state.update { it.copy(
                    isThinking = thinking,
                    thinkingContent = content,
                    thinkingElapsedMs = elapsed
                ) }
            }.collect()
        }

        // 3. Permissions & Interaction Updates
        screenModelScope.launch {
            combine(
                chatStateStore.pendingPermission,
                chatStateStore.pendingQuestion,
                commandRepository.cachedCommands,
                chatStateStore.todos,
                chatStateStore.chatTurnState,
                chatStateStore.currentSessionId
            ) { args ->
                val perm = args[0] as PermissionRequest?
                val quest = args[1] as QuestionRequest?
                @Suppress("UNCHECKED_CAST")
                val cmds = args[2] as List<Command>
                @Suppress("UNCHECKED_CAST")
                val todos = args[3] as List<Todo>
                val turnState = args[4] as ChatTurnState
                val currentSessionId = args[5] as String?
                val queuedPermissions = currentSessionId
                    ?.let { turnState.pendingPermissionsBySession[it].orEmpty() }
                    .orEmpty()
                val queuedQuestions = currentSessionId
                    ?.let { turnState.pendingQuestionsBySession[it].orEmpty() }
                    .orEmpty()
                _state.update { it.copy(
                    pendingPermission = perm ?: queuedPermissions.firstOrNull(),
                    pendingQuestion = quest ?: queuedQuestions.firstOrNull(),
                    pendingPermissions = queuedPermissions.toImmutableList(),
                    pendingQuestions = queuedQuestions.toImmutableList(),
                    agentActivity = currentSessionId?.let { sessionId -> turnState.sessionActivities[sessionId] },
                    commands = cmds.toImmutableList(),
                    todos = todos.toImmutableList()
                ) }
            }.collect()
        }

        // 4. Configuration & Model Updates
        screenModelScope.launch {
            combine(
                aiRuntimeConfigRepository.configState,
                aiRuntimeConfigRepository.effectiveSelection,
                aiRuntimeConfigRepository.modelPickerState,
                aiRuntimeConfigRepository.variantPickerState
            ) { args ->
                val config = args[0] as AiConfigState
                val effective = args[1] as AiEffectiveSelection?
                val modelPicker = args[2] as ModelPickerUiState
                val variantPicker = args[3] as VariantPickerUiState
                val modes = config.snapshot?.agentModeOptions().orEmpty().toImmutableList()

                _state.update { it.copy(
                    aiConfigState = config,
                    effectiveSelection = effective,
                    modelPickerState = modelPicker,
                    variantPickerState = variantPicker,
                    selectedVariantId = effective?.variantId,
                    modes = modes,
                    selectedModeId = effective?.agentId ?: effective?.modeId,
                    isPlanMode = effective?.agentId == "plan" || effective?.modeId == "plan",
                    modelName = effective?.displayModel ?: when (config.status) {
                        AiConfigStatus.UPDATE_REQUIRED -> "UPDATE CLI"
                        AiConfigStatus.ERROR -> "NO MODEL"
                        else -> "--"
                    },
                    agentName = effective?.displayAgentOrMode ?: "--",
                    maxTokens = effective?.contextLimit ?: 0
                ) }
            }.collect()
        }

        // 5. App & User Preference Updates
        screenModelScope.launch {
            appStateStore.userPreferences.collect { prefs ->
                _state.update { it.copy(
                    showTimestamps = prefs.showTimestamps,
                    showTokenCounts = prefs.showTokenCounts
                ) }
            }
        }

        // 6. Infrastructure status
        screenModelScope.launch {
            stateCoordinator.connectionStatus.collect { status ->
                _state.update { it.copy(connectionStatus = status) }
            }
        }

        // 7. Voice input state
        screenModelScope.launch {
            voiceInputProvider.state.collect { state ->
                when (state) {
                    is VoiceInputState.PartialResult -> _inputText.value = state.text
                    is VoiceInputState.FinalResult -> _inputText.value = state.text
                    is VoiceInputState.NeedsPermission -> _voicePermissionRequestToken.update { it + 1 }
                    else -> Unit
                }
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
    fun toggleVoiceInput() {
        when (voiceInputProvider.state.value) {
            is VoiceInputState.Listening -> voiceInputProvider.stopListening()
            is VoiceInputState.NeedsPermission -> _voicePermissionRequestToken.update { it + 1 }
            is VoiceInputState.NotAvailable -> Unit
            else -> voiceInputProvider.startListening()
        }
    }

    fun onVoicePermissionResult(granted: Boolean) {
        if (granted) {
            voiceInputProvider.startListening()
        }
    }

    fun selectModel(p: String, m: String) {
        screenModelScope.launch { aiRuntimeConfigRepository.selectModel(p, m) }
    }
    fun selectVariant(v: String?) {
        screenModelScope.launch { aiRuntimeConfigRepository.selectVariant(v) }
    }
    fun selectMode(m: String?) {
        screenModelScope.launch { aiRuntimeConfigRepository.selectAgentOrMode(m) }
    }
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
        val rawText = _inputText.value.trim()
        val currentAttachments = _state.value.attachedFiles
        if (rawText.isEmpty() && currentAttachments.isEmpty()) return

        val messageText = rawText.ifEmpty {
            if (currentAttachments.size == 1) {
                "Please review the attached file."
            } else {
                "Please review the attached files."
            }
        }
        // Prepend '!' prefix when shell mode is active
        val text = if (_shellMode.value && rawText.isNotEmpty() && !messageText.startsWith("!")) "!$messageText" else messageText
        // Push to prompt history (most-recent first, cap at 50)
        if (rawText.isNotEmpty()) {
            _promptHistory.value = (listOf(rawText) + _promptHistory.value).take(50)
        }
        _historyIndex.value = -1
        
        if (currentAttachments.isEmpty() && text.startsWith("/")) {
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
            val selection = try {
                aiRuntimeConfigRepository.requireEffectiveSelection()
            } catch (error: Exception) {
                _state.update { it.copy(error = error.message ?: "No valid AI model is selected") }
                return@launch
            }

            _inputText.value = ""
            clearAttachments()
            
            val result = chatStateStore.sendMessage(
                text = text,
                selection = selection,
                attachments = currentAttachments
            )
            
            if (result.isFailure) {
                _inputText.value = rawText // Restore original text on failure
                _state.update { it.copy(attachedFiles = currentAttachments) }
            }
        }
    }

    fun abortSession() {
        screenModelScope.launch { chatStateStore.abortSession() }
    }

    fun approvePermission() {
        screenModelScope.launch { chatStateStore.approvePermission() }
    }

    fun approvePermissionAlways() {
        screenModelScope.launch { chatStateStore.approvePermissionAlways() }
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

    fun openForkDialog() {
        _showForkDialog.value = true
    }

    fun dismissForkDialog() {
        _showForkDialog.value = false
    }

    fun openShareDialog() {
        _showShareDialog.value = true
    }

    fun dismissShareDialog() {
        _showShareDialog.value = false
    }

    fun forkSession(m: Message) {
        _showForkDialog.value = false
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

    fun deleteMessage(m: Message) {
        screenModelScope.launch {
            sessionRepository.deleteMessage(sessionId, m.id)
        }
    }

    fun deleteMessagePart(m: Message, partId: String) {
        screenModelScope.launch {
            sessionRepository.deleteMessagePart(sessionId, m.id, partId)
        }
    }

    fun showEditPart(message: Message, part: MessagePart) {
        _editingPart.value = Pair(message, part)
    }

    fun dismissEditPart() {
        _editingPart.value = null
    }

    fun commitEditPart(content: String) {
        val (message, part) = _editingPart.value ?: return
        val partId = when (part) {
            is MessagePart.Text -> part.id
            is MessagePart.ToolInvocation -> part.id
            is MessagePart.ToolResult -> part.id
            else -> return
        }
        _editingPart.value = null
        screenModelScope.launch {
            sessionRepository.patchMessagePart(sessionId, message.id, partId, content)
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

    fun exportChatToMarkdown(): String {
        val sessionTitle = _state.value.session?.title ?: "Chat Export"
        val messages = aggregatedMessages.value.toList()
        return ChatExporter.exportSessionToMarkdown(sessionTitle, messages)
    }

    fun openExportDialog() {
        _showExportDialog.value = true
    }

    fun dismissExportDialog() {
        _showExportDialog.value = false
    }

    fun exportToClipboard() {
        val markdown = exportChatToMarkdown()
        // Export to clipboard - actual clipboard operation handled in UI layer
        dismissExportDialog()
    }

    fun refreshData() {
        screenModelScope.launch {
            // isLoading is now synced from ChatStateStore
            loadMessages()
            loadTodos()
        }
    }

    fun retry() {
        clearError()
        screenModelScope.launch { chatStateStore.loadSession(sessionId, forceReload = true) }
    }

    fun clearError() { _state.update { it.copy(error = null) } }
    fun dismissAgentError() {
        chatStateStore.clearAgentError()
        _state.update { it.copy(agentError = null) }
    }
    fun dismissDisposal() = chatStateStore.clearDisposed()
    
    override fun onDispose() {
        // StateCoordinator manages SSE lifecycle, no need to disconnect here
        // The session will remain monitored for notifications
        voiceInputProvider.release()
    }
}

private fun AiRuntimeConfigSnapshot.agentModeOptions(): List<Mode> {
    val agentModes = agents
        .filterNot { it.hidden }
        .map { agent -> Mode(id = agent.id, name = agent.name, description = agent.description) }
    val runtimeModes = modes.map { mode -> Mode(id = mode.id, name = mode.name, description = mode.description) }
    return (agentModes + runtimeModes)
        .distinctBy { it.id }
        .sortedWith(compareBy<Mode> { it.id != "build" }.thenBy { it.name })
}
