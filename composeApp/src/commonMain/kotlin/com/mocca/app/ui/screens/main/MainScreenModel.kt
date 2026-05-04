package com.mocca.app.ui.screens.main

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.AppStateStore
import com.mocca.app.data.repository.AiRuntimeConfigRepository
import com.mocca.app.data.repository.ChatStateStore
import com.mocca.app.data.repository.ConnectionManager
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.data.repository.SearchRepository
import com.mocca.app.data.repository.SessionRepository
import com.mocca.app.data.repository.UpdateCheckScheduler
import com.mocca.app.data.repository.UpdateNotifier
import com.mocca.app.data.repository.UpdateRepository
import com.mocca.app.domain.model.McpServerInfo
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.FileContentSearchResult
import com.mocca.app.domain.model.FileSearchResult
import com.mocca.app.domain.model.SearchMode
import com.mocca.app.domain.model.SearchQuery
import com.mocca.app.domain.model.Session
import com.mocca.app.domain.model.SessionGroup
import com.mocca.app.domain.model.SessionRunningState
import com.mocca.app.domain.model.SessionStatus
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.DownloadStatus
import com.mocca.app.domain.model.ServerEvent
import com.mocca.app.domain.model.UnifiedSearchResult
import com.mocca.app.domain.model.UpdateInfo
import com.mocca.app.domain.model.deriveAiShellStatus
import com.mocca.app.domain.provider.AppVersionProvider
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State for the main screen.
 */
@Immutable
data class MainScreenState(
    // Session state
    val currentSessionId: String? = null,
    val sessions: ImmutableList<Session> = persistentListOf(),
    val messages: ImmutableList<Message> = persistentListOf(),
    val isFileSearchLoading: Boolean = false,
    val fileSearchMode: SearchMode = SearchMode.FILE_PATTERN,
    val fileSearchResults: ImmutableList<FileSearchResult> = persistentListOf(),
    val fileContentSearchResults: ImmutableList<FileContentSearchResult> = persistentListOf(),
    val fileSearchError: String? = null,
    
    // Input state
    val inputText: String = "",
    val isPlanMode: Boolean = false,
    val isSending: Boolean = false,
    
    // Connection state
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isWaitingForNetwork: Boolean = false,
    val connectionAttempt: Int = 0,
    val maxConnectionAttempts: Int = 10,
    val connectionError: String? = null,
    
    // SSE connection state (real-time event streaming)
    val isSseConnected: Boolean = false,
    
    // Context info
    val mcpStatus: String = "OFFLINE",
    val isMcpOnline: Boolean = false,
    val modelName: String = "--",
    val agentName: String = "--",
    val latency: String = "--ms",
    val port: String = "--",
    val usedTokens: Int = 0,
    val maxTokens: Int = 0,
    
    // Loading states
    val isLoading: Boolean = false,
    val isCreatingSession: Boolean = false,  // Loading state for INIT_NEW_SESSION button
    val isLoadingSession: Boolean = false,   // Loading state when switching sessions
    val loadingSessionId: String? = null,    // ID of session being loaded (for list item feedback)
    val newlyCreatedSessionId: String? = null, // ID of just-created session (for animation)
    val error: String? = null,
    
    // MCP servers
    val mcpServers: ImmutableList<McpServerInfo> = persistentListOf(),
    val isMcpLoading: Boolean = false,
    
    // App info
    val appVersion: String = "",
    
    // Update info
    val updateInfo: UpdateInfo? = null,
    val isUpdateAvailable: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val downloadProgress: Float = 0f,
    val updateError: String? = null,
    val updateLogs: ImmutableList<String> = persistentListOf(),
    
    // Session grouping and real-time status
    val sessionGroups: ImmutableList<SessionGroup> = persistentListOf(),
    val runningSessionIds: Set<String> = emptySet(), // Sessions with active agent/LLM
    val expandedGroupIds: Set<String> = emptySet() // IDs of expanded session groups
) {
    val mcpConnectedCount: Int get() = mcpServers.count { it.isConnected }
    val mcpTotalCount: Int get() = mcpServers.size
    
    /** Check if any session in the entire list is currently running */
    val hasAnyRunningSession: Boolean get() = runningSessionIds.isNotEmpty()
    
    /** Get count of currently running sessions */
    val runningSessionCount: Int get() = runningSessionIds.size
}

/**
 * ScreenModel for the main screen.
 * Observes centralized state from AppStateStore for reactive updates.
 * 
 * IMPORTANT: All state is now automatically synced via:
 * - StateCoordinator (SSE events)
 * - RealtimeSyncService (periodic polling)
 * 
 * NO manual refresh is needed.
 */
class MainScreenModel(
    private val initialSessionId: String?,
    private val appStateStore: AppStateStore,
    private val sessionRepository: SessionRepository,
    private val searchRepository: SearchRepository,
    private val aiRuntimeConfigRepository: AiRuntimeConfigRepository,
    private val connectionManager: ConnectionManager,
    private val mcpRepository: McpRepository,
    private val updateRepository: UpdateRepository,
    private val updateNotifier: UpdateNotifier,
    private val updateCheckScheduler: UpdateCheckScheduler,
    private val appVersionProvider: AppVersionProvider
) : ScreenModel {

    private var fileSearchJob: Job? = null
    
    private val _state = MutableStateFlow(MainScreenState(currentSessionId = initialSessionId))
    val state: StateFlow<MainScreenState> = _state.asStateFlow()
    
    // Session groups from centralized store (reactive)
    val sessionGroups = appStateStore.sessionGroups
        .stateIn(screenModelScope, SharingStarted.Eagerly, emptyList())
    
    init {
        _state.update { it.copy(appVersion = "V${appVersionProvider.getVersion()}") }
        
        // Observe centralized state from AppStateStore
        observeAppStateStore()
        observeConnectionState()
        observeMcpServers()
        observeUpdateNotifications()
        
        // Keep GitHub update checks off the critical startup path.
        screenModelScope.launch {
            delay(5 * 60_000L)
            updateCheckScheduler.start()
        }

        // Resume active update download polling if any
        screenModelScope.launch {
            try {
                updateRepository.pollActiveDownload()
                    .collect { status ->
                        when (status) {
                            is DownloadStatus.Progress -> _state.update { 
                                it.copy(isDownloadingUpdate = true, isUpdateAvailable = true, downloadProgress = status.progress) 
                            }
                            is DownloadStatus.Log -> _state.update { 
                                it.copy(updateLogs = (it.updateLogs + status.message).toImmutableList()) 
                            }
                            is DownloadStatus.Error -> {
                                if (status.message != "No active download found") {
                                    _state.update { 
                                        it.copy(
                                            isDownloadingUpdate = false,
                                            updateError = status.message,
                                            updateLogs = (it.updateLogs + "ERROR: ${status.message}").toImmutableList()
                                        ) 
                                    }
                                }
                            }
                            is DownloadStatus.Complete -> {
                                _state.update { 
                                    it.copy(
                                        isDownloadingUpdate = false,
                                        isUpdateAvailable = false,
                                        updateLogs = (it.updateLogs + "Download complete. Waiting for installation...").toImmutableList()
                                    ) 
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                // Ignore failure if no active download
            }
        }
        
        // Sync initial state
        appStateStore.start()
    }
    
    /**
     * Observe centralized state from AppStateStore.
     * This replaces manual session loading and SSE event observation.
     */
    private fun observeAppStateStore() {
        // Observe current session ID from centralized store (restored from preferences on startup)
        screenModelScope.launch {
            appStateStore.currentSessionId.collect { sessionId ->
                Napier.i("[MainScreenModel] Current session ID from store: $sessionId")
                // Only update if we don't have an explicit initialSessionId or if it changed
                if (sessionId != null && _state.value.currentSessionId == null) {
                    _state.update { it.copy(currentSessionId = sessionId) }
                    Napier.i("[MainScreenModel] Restored session from store: $sessionId")
                }
            }
        }
        
        // Observe sessions
        screenModelScope.launch {
            appStateStore.sessions.collect { sessions ->
                val current = _state.value
                val sessionList = withContext(Dispatchers.Default) {
                    sessions.toImmutableList()
                }
                val sessionGroups = withContext(Dispatchers.Default) {
                    buildSessionGroups(
                        sessions = sessions,
                        runningIds = current.runningSessionIds,
                        expandedIds = current.expandedGroupIds
                    ).toImmutableList()
                }
                _state.update {
                    it.copy(
                        sessions = sessionList,
                        sessionGroups = sessionGroups
                    )
                }
            }
        }
        
        // Observe running sessions
        screenModelScope.launch {
            appStateStore.runningSessionIds.collect { runningIds ->
                val current = _state.value
                val sessionGroups = withContext(Dispatchers.Default) {
                    buildSessionGroups(
                        sessions = current.sessions,
                        runningIds = runningIds,
                        expandedIds = current.expandedGroupIds
                    ).toImmutableList()
                }
                _state.update {
                    it.copy(
                        runningSessionIds = runningIds,
                        sessionGroups = sessionGroups
                    )
                }
            }
        }
        
        // Observe model/mode info from the normalized AI runtime config.
        screenModelScope.launch {
            combine(
                aiRuntimeConfigRepository.configState,
                aiRuntimeConfigRepository.effectiveSelection
            ) { configState, effectiveSelection ->
                deriveAiShellStatus(configState, effectiveSelection)
            }.collect { shellStatus ->
                _state.update {
                    it.copy(
                        modelName = shellStatus.modelName,
                        agentName = shellStatus.agentName
                    )
                }
            }
        }

        // Observe sync state
        screenModelScope.launch {
            appStateStore.isSyncing.collect { isSyncing ->
                _state.update { it.copy(isLoading = isSyncing) }
            }
        }
    }
    
    /**
     * Build session groups from flat session list with UI state.
     */
    private fun buildSessionGroups(
        sessions: List<Session>,
        runningIds: Set<String>,
        expandedIds: Set<String>
    ): List<SessionGroup> {
        // Separate root sessions (no parent) from child sessions
        val rootSessions = sessions.filter { session ->
            val hasParent = !session.effectiveParentID.isNullOrBlank()
            val isInternal = session.title.orEmpty().let { title ->
                title.startsWith("Background:") || 
                title.startsWith("look_at:") ||
                title.contains("subagent", ignoreCase = true)
            }
            !hasParent && !isInternal
        }
        
        // Group children by parent ID
        val childrenByParent = sessions.filter { session ->
            val hasParent = !session.effectiveParentID.isNullOrBlank()
            val isInternal = session.title.orEmpty().let { title ->
                title.startsWith("Background:") || 
                title.startsWith("look_at:") ||
                title.contains("subagent", ignoreCase = true)
            }
            hasParent || isInternal
        }.groupBy { it.effectiveParentID ?: "internal" }
        
        return rootSessions.map { parent ->
            val children = childrenByParent[parent.id] ?: emptyList()
            SessionGroup(
                parent = parent,
                children = children.sortedByDescending { it.updatedAt },
                isExpanded = expandedIds.contains(parent.id)
            )
        }.sortedByDescending { it.lastActivityTime }
    }

    /**
     * Observe update notifications from other screens (e.g., Settings).
     * This allows manual update checks to trigger the update dialog.
     */
    private fun observeUpdateNotifications() {
        screenModelScope.launch {
            updateNotifier.pendingUpdate.collect { updateInfo ->
                if (updateInfo != null) {
                    _state.update {
                        it.copy(
                            updateInfo = updateInfo,
                            isUpdateAvailable = true,
                            updateError = null
                        )
                    }
                    // Clear the notification so it doesn't trigger again
                    updateNotifier.clearNotification()
                }
            }
        }
    }
    
    private fun checkForUpdates() {
        screenModelScope.launch {
            try {
                updateRepository.checkForUpdate().fold(
                    onSuccess = { updateInfo ->
                        if (updateInfo != null) {
                            _state.update { 
                                it.copy(
                                    updateInfo = updateInfo,
                                    isUpdateAvailable = true
                                )
                            }
                        }
                    },
                    onFailure = { e: Throwable ->
                        Napier.w("Failed to check for updates: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                 Napier.w("Error checking for updates: ${e.message}")
            }
        }
    }

    fun startUpdate() {
        val info = _state.value.updateInfo ?: return

        screenModelScope.launch {
            _state.update {
                it.copy(
                    isDownloadingUpdate = true,
                    downloadProgress = 0f,
                    updateError = null,
                    updateLogs = persistentListOf("Initializing download...")
                )
            }

            try {
                updateRepository.downloadAndInstall(info, "mocca-update.apk")
                    .collect { status ->
                        when (status) {
                            is DownloadStatus.Progress -> _state.update { it.copy(downloadProgress = status.progress) }
                            is DownloadStatus.Log -> _state.update { it.copy(updateLogs = (it.updateLogs + status.message).toImmutableList()) }
                            is DownloadStatus.Error -> _state.update { 
                                it.copy(
                                    isDownloadingUpdate = false,
                                    updateError = status.message,
                                    updateLogs = (it.updateLogs + "ERROR: ${status.message}").toImmutableList()
                                ) 
                            }
                            is DownloadStatus.Complete -> {
                                _state.update { 
                                    it.copy(
                                        isDownloadingUpdate = false,
                                        isUpdateAvailable = false,
                                        updateLogs = (it.updateLogs + "Download complete. Installing...").toImmutableList()
                                    ) 
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Napier.e("Update download failed", e, "MainScreenModel")
                _state.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        updateError = e.message ?: "Download failed",
                        updateLogs = (it.updateLogs + "CRITICAL EXCEPTION: ${e.message}\n${e.stackTraceToString()}").toImmutableList()
                    )
                }
            }
        }
    }

    fun retryUpdate() {
        // Clear error and retry download
        _state.update { it.copy(updateError = null) }
        startUpdate()
    }

    fun dismissUpdate() {
        _state.update {
            it.copy(
                isUpdateAvailable = false,
                updateError = null
            )
        }
    }

    /**
     * Trigger update check and show dialog if update available.
     * Used for manual update checks (e.g., from Settings screen).
     */
    fun checkForUpdatesManual() {
        screenModelScope.launch {
            try {
                updateRepository.checkForUpdate().fold(
                    onSuccess = { updateInfo ->
                        if (updateInfo != null) {
                            _state.update {
                                it.copy(
                                    updateInfo = updateInfo,
                                    isUpdateAvailable = true,
                                    updateError = null
                                )
                            }
                        }
                    },
                    onFailure = { e: Throwable ->
                        Napier.w("Manual update check failed: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Napier.w("Error during manual update check: ${e.message}")
            }
        }
    }
    
    private fun observeConnectionState() {
        // Observe from ConnectionManager for latency/port updates
        screenModelScope.launch {
            connectionManager.status.collect { status ->
                when (status) {
                    is ConnectionStatus.Connected -> {
                        val config = connectionManager.activeConfig.value
                        _state.update { it.copy(
                            isConnected = true,
                            isConnecting = false,
                            isWaitingForNetwork = false,
                            connectionError = null,
                            latency = "${status.latencyMs}ms",
                            port = ":${config?.port ?: "--"}",
                            mcpStatus = "ONLINE",
                            isMcpOnline = true
                        )}
                    }
                    is ConnectionStatus.Disconnected -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = false,
                            connectionError = status.reason,
                            mcpStatus = "OFFLINE",
                            isMcpOnline = false,
                            isSseConnected = false
                        )}
                    }
                    is ConnectionStatus.Connecting -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = true,
                            mcpStatus = "CONNECTING",
                            isMcpOnline = false,
                            isSseConnected = false
                        )}
                    }
                    is ConnectionStatus.Reconnecting -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = true,
                            connectionAttempt = status.attempt,
                            mcpStatus = "RECONNECTING",
                            isMcpOnline = false,
                            isSseConnected = false
                        )}
                    }
                    is ConnectionStatus.WaitingForNetwork -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = false,
                            isWaitingForNetwork = true,
                            mcpStatus = "WAITING",
                            isMcpOnline = false,
                            isSseConnected = false
                        )}
                    }
                    is ConnectionStatus.NotConfigured -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = false,
                            mcpStatus = "NOT_CONFIGURED",
                            isMcpOnline = false,
                            isSseConnected = false
                        )}
                    }
                    is ConnectionStatus.Error -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = false,
                            connectionError = status.message,
                            mcpStatus = "ERROR",
                            isMcpOnline = false,
                            isSseConnected = false
                        )}
                    }
                }
            }
        }
        
        // Observe SSE connection status for real-time event streaming indicator
        screenModelScope.launch {
            appStateStore.isSseConnected.collect { isSseConnected ->
                _state.update { it.copy(isSseConnected = isSseConnected) }
            }
        }
    }
    
    private fun observeMcpServers() {
        screenModelScope.launch {
            mcpRepository.mcpServers.collect { serversMap ->
                _state.update { it.copy(
                    mcpServers = serversMap.values.toList().sortedBy { server -> server.name }.toImmutableList()
                )}
            }
        }
        
        screenModelScope.launch {
            mcpRepository.isLoading.collect { loading ->
                _state.update { it.copy(isMcpLoading = loading) }
            }
        }
    }
    
    fun refreshMcpServers() {
        screenModelScope.launch {
            mcpRepository.refresh()
        }
    }
    
    fun toggleMcpServer(serverName: String, connect: Boolean) {
        screenModelScope.launch {
            mcpRepository.toggleConnection(serverName, connect)
        }
    }
    
    /**
     * Toggle expand/collapse state of a session group.
     */
    fun toggleGroupExpanded(parentSessionId: String) {
        _state.update { current ->
            val newExpandedIds = if (current.expandedGroupIds.contains(parentSessionId)) {
                current.expandedGroupIds - parentSessionId
            } else {
                current.expandedGroupIds + parentSessionId
            }
            // Rebuild groups with new expanded state
            val updatedGroups = buildSessionGroups(current.sessions, current.runningSessionIds, newExpandedIds)
            current.copy(
                expandedGroupIds = newExpandedIds,
                sessionGroups = updatedGroups.toImmutableList()
            )
        }
    }
    
    /**
     * Select a session and navigate to chat.
     * @param sessionId The session ID to select
     * @param onNavigate Optional callback to trigger navigation (e.g., close panel)
     */
    fun selectSession(sessionId: String, onNavigate: (() -> Unit)? = null) {
        // Immediately update loading state for instant feedback
        _state.update { it.copy(
            loadingSessionId = sessionId,
            isLoadingSession = true
        )}
        
        // Trigger navigation immediately (don't wait for data)
        onNavigate?.invoke()
        
        // Update session ID immediately for optimistic UI
        _state.update { it.copy(currentSessionId = sessionId) }
        
        _state.update { it.copy(
            loadingSessionId = null,
            isLoadingSession = false
        )}
    }
    
    /**
     * Create a new session and navigate to chat.
     * @param onNavigate Optional callback to trigger navigation (e.g., close panel)
     */
    fun createSession(onNavigate: (() -> Unit)? = null) {
        // Set loading state immediately for button feedback
        _state.update { it.copy(isCreatingSession = true) }
        
        screenModelScope.launch {
            sessionRepository.createSession().fold(
                onSuccess = { session ->
                    // Add to sessions list immediately for animation
                    _state.update { current ->
                        current.copy(
                            sessions = (listOf(session) + current.sessions).toImmutableList(),
                            newlyCreatedSessionId = session.id,
                            isCreatingSession = false
                        )
                    }
                    
                    // Select session and trigger navigation
                    selectSession(session.id, onNavigate)
                    
                    // Clear the newlyCreatedSessionId after animation delay
                    kotlinx.coroutines.delay(500)
                    _state.update { it.copy(newlyCreatedSessionId = null) }
                },
                onFailure = { error ->
                    _state.update { it.copy(
                        isCreatingSession = false,
                        error = error.message
                    )}
                }
            )
        }
    }
    
    fun clearHistory() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            sessionRepository.deleteAllSessions().fold(
                onSuccess = {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            sessions = persistentListOf(),
                            currentSessionId = null,
                            messages = persistentListOf()
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }
    
    fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun searchFiles(query: String) {
        val trimmedQuery = query.trim()
        fileSearchJob?.cancel()

        if (trimmedQuery.isEmpty()) {
            _state.update {
                it.copy(
                    isFileSearchLoading = false,
                    fileSearchMode = it.fileSearchMode,
                    fileSearchResults = persistentListOf(),
                    fileContentSearchResults = persistentListOf(),
                    fileSearchError = null
                )
            }
            return
        }

        val searchQuery = SearchQuery(
            query = if (_state.value.fileSearchMode == SearchMode.FILE_PATTERN) {
                buildFileSearchPattern(trimmedQuery)
            } else {
                trimmedQuery
            },
            mode = _state.value.fileSearchMode,
            contextLines = 2
        )

        fileSearchJob = screenModelScope.launch {
            delay(180)
            when (searchQuery.mode) {
                SearchMode.FILE_PATTERN,
                SearchMode.TEXT_CONTENT -> {
                    searchRepository.search(searchQuery).collect { resource ->
                        when (resource) {
                            is Resource.Loading<*> -> _state.update {
                                it.copy(
                                    isFileSearchLoading = true,
                                    fileSearchResults = persistentListOf(),
                                    fileContentSearchResults = persistentListOf(),
                                    fileSearchError = null
                                )
                            }

                            is Resource.Success<*> -> {
                                val result = resource.data as? UnifiedSearchResult
                                _state.update {
                                    it.copy(
                                        isFileSearchLoading = false,
                                        fileSearchResults = result?.fileResults
                                            ?.take(12)
                                            ?.toImmutableList()
                                            ?: persistentListOf(),
                                        fileContentSearchResults = result?.textResults
                                            ?.take(12)
                                            ?.toImmutableList()
                                            ?: persistentListOf(),
                                        fileSearchError = null
                                    )
                                }
                            }

                            is Resource.Error<*> -> _state.update {
                                it.copy(
                                    isFileSearchLoading = false,
                                    fileSearchResults = persistentListOf(),
                                    fileContentSearchResults = persistentListOf(),
                                    fileSearchError = resource.message
                                )
                            }
                        }
                    }
                }

                SearchMode.SYMBOL -> Unit
            }
        }
    }

    fun updateFileSearchMode(mode: SearchMode, query: String) {
        if (mode == SearchMode.SYMBOL) return
        _state.update {
            it.copy(
                fileSearchMode = mode,
                fileSearchResults = persistentListOf(),
                fileContentSearchResults = persistentListOf(),
                fileSearchError = null,
                isFileSearchLoading = false
            )
        }
        searchFiles(query)
    }
    
    fun togglePlanMode() {
        _state.update { it.copy(isPlanMode = !it.isPlanMode) }
    }
    
    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return
        
        val sessionId = _state.value.currentSessionId
        if (sessionId == null) {
            // Create new session first
            screenModelScope.launch {
                sessionRepository.createSession().fold(
                    onSuccess = { session ->
                        _state.update { 
                            it.copy(
                                currentSessionId = session.id,
                                inputText = ""
                            )
                        }
                        sendMessageToSession(session.id, text)
                    },
                    onFailure = { error ->
                        _state.update { it.copy(error = error.message) }
                    }
                )
            }
        } else {
            _state.update { it.copy(inputText = "") }
            sendMessageToSession(sessionId, text)
        }
    }
    
    private fun sendMessageToSession(sessionId: String, text: String) {
        _state.update { it.copy(isSending = true) }
        
        screenModelScope.launch {
            sessionRepository.sendMessageAsync(sessionId, text).fold(
                onSuccess = {
                    _state.update { it.copy(isSending = false) }
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            isSending = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }
    
    fun retryConnection() {
        connectionManager.checkConnection()
        appStateStore.syncFromServer()
    }
    
    /**
     * Refresh all data - uses centralized AppStateStore sync.
     * Called from DashboardPanel refresh button.
     */
    fun refreshAll() {
        appStateStore.syncFromServer()
        
        // Chat history is owned by ChatStateStore; refreshAll only syncs shared app state.
    }
    
    override fun onDispose() {
        // Stop periodic update checks
        fileSearchJob?.cancel()
        updateCheckScheduler.stop()
        // State is maintained in AppStateStore, no need to disconnect
    }

    private fun buildFileSearchPattern(query: String): String {
        return if (query.any { it in "*?[]" }) query else "*$query*"
    }
}
