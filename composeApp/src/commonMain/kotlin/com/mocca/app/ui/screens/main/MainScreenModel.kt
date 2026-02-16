package com.mocca.app.ui.screens.main

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.ConnectionManager
import com.mocca.app.data.repository.EventStreamRepository
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.data.repository.SessionRepository
import com.mocca.app.data.repository.UpdateNotifier
import com.mocca.app.data.repository.UpdateRepository
import com.mocca.app.domain.model.McpServerInfo
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.Session
import com.mocca.app.domain.model.SessionGroup
import com.mocca.app.domain.model.SessionRunningState
import com.mocca.app.domain.model.SessionStatus
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.ServerEvent
import com.mocca.app.domain.model.UpdateInfo
import com.mocca.app.domain.provider.AppVersionProvider
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the main screen.
 */
data class MainScreenState(
    // Session state
    val currentSessionId: String? = null,
    val sessions: List<Session> = emptyList(),
    val messages: List<Message> = emptyList(),
    
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
    val mcpServers: List<McpServerInfo> = emptyList(),
    val isMcpLoading: Boolean = false,
    
    // App info
    val appVersion: String = "",
    
    // Update info
    val updateInfo: UpdateInfo? = null,
    val isUpdateAvailable: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val downloadProgress: Float = 0f,
    val updateError: String? = null,
    
    // Session grouping and real-time status
    val sessionGroups: List<SessionGroup> = emptyList(),
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
 */
class MainScreenModel(
    private val initialSessionId: String?,
    private val sessionRepository: SessionRepository,
    private val eventStreamRepository: EventStreamRepository,
    private val connectionManager: ConnectionManager,
    private val mcpRepository: McpRepository,
    private val updateRepository: UpdateRepository,
    private val updateNotifier: UpdateNotifier,
    private val appVersionProvider: AppVersionProvider
) : ScreenModel {
    
    private val _state = MutableStateFlow(MainScreenState(currentSessionId = initialSessionId))
    val state: StateFlow<MainScreenState> = _state.asStateFlow()
    
    init {
        _state.update { it.copy(appVersion = "V${appVersionProvider.getVersion()}") }
        observeAppConnectionState()
        observeConnectionState()
        observeMcpServers()
        observeSessionEvents()
        observeUpdateNotifications()
        loadSessions()
        checkForUpdates()
        if (initialSessionId != null) {
            loadMessages(initialSessionId)
        }
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
                    onFailure = { e ->
                        Napier.w("Failed to check for updates", e)
                    }
                )
            } catch (e: Exception) {
                 Napier.e("Error checking for updates", e)
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
                    updateError = null
                )
            }

            try {
                updateRepository.downloadAndInstall(info, "mocca-update.apk")
                    .collect { progress ->
                        _state.update { it.copy(downloadProgress = progress) }
                    }
                // Install triggered automatically by repository at end of flow
                // Dismiss dialog after successful download/install trigger
                _state.update { it.copy(isUpdateAvailable = false, isDownloadingUpdate = false) }
            } catch (e: Exception) {
                Napier.e("Update download failed", e, "MainScreenModel")
                _state.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        updateError = e.message ?: "Download failed"
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
                    onFailure = { e ->
                        Napier.w("Manual update check failed", e)
                    }
                )
            } catch (e: Exception) {
                Napier.e("Error during manual update check", e)
            }
        }
    }

    private fun observeAppConnectionState() {
        screenModelScope.launch {
            connectionManager.status.collect { status ->
                when (status) {
                    is ConnectionStatus.Connected -> {
                        val config = connectionManager.activeConfig.value
                        _state.update { it.copy(
                            latency = "${status.latencyMs}ms",
                            port = ":${config?.port ?: "--"}"
                        )}
                        connectToEventStream()
                        loadSessions()
                    }
                    is ConnectionStatus.Disconnected -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = false,
                            connectionError = status.reason,
                            mcpStatus = "OFFLINE",
                            isMcpOnline = false
                        )}
                    }
                    is ConnectionStatus.Connecting -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = true,
                            mcpStatus = "CONNECTING",
                            isMcpOnline = false
                        )}
                    }
                    is ConnectionStatus.Reconnecting -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = true,
                            connectionAttempt = status.attempt,
                            mcpStatus = "RECONNECTING",
                            isMcpOnline = false
                        )}
                    }
                    is ConnectionStatus.WaitingForNetwork -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = false,
                            isWaitingForNetwork = true,
                            mcpStatus = "WAITING",
                            isMcpOnline = false
                        )}
                    }
                    is ConnectionStatus.NotConfigured -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = false,
                            mcpStatus = "NOT_CONFIGURED",
                            isMcpOnline = false
                        )}
                    }
                    is ConnectionStatus.Error -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = false,
                            connectionError = status.message,
                            mcpStatus = "ERROR",
                            isMcpOnline = false
                        )}
                    }
                }
            }
        }
    }
    
    private fun connectToEventStream() {
        eventStreamRepository.connect(screenModelScope, _state.value.currentSessionId)
    }
    
    private fun observeMcpServers() {
        screenModelScope.launch {
            mcpRepository.mcpServers.collect { serversMap ->
                _state.update { it.copy(
                    mcpServers = serversMap.values.toList().sortedBy { server -> server.name }
                )}
            }
        }
        
        screenModelScope.launch {
            mcpRepository.isLoading.collect { loading ->
                _state.update { it.copy(isMcpLoading = loading) }
            }
        }
    }
    
    /**
     * Observe SSE events for real-time session status updates.
     * Updates runningSessionIds based on session.updated, session.idle, and session.error events.
     */
    private fun observeSessionEvents() {
        screenModelScope.launch {
            eventStreamRepository.events.collect { event ->
                when (event) {
                    is ServerEvent.SessionUpdated -> {
                        val session = event.properties.info
                        val isRunning = session.status == SessionStatus.RUNNING
                        
                        _state.update { current ->
                            val newRunningIds = if (isRunning) {
                                current.runningSessionIds + session.id
                            } else {
                                current.runningSessionIds - session.id
                            }
                            
                            // Update session in the list and groups
                            val updatedSessions = current.sessions.map { 
                                if (it.id == session.id) session else it 
                            }
                            val updatedGroups = buildSessionGroups(updatedSessions)
                            
                            current.copy(
                                sessions = updatedSessions,
                                sessionGroups = updatedGroups,
                                runningSessionIds = newRunningIds
                            )
                        }
                        Napier.d("Session ${session.id} status: ${session.status}, running: $isRunning")
                    }
                    
                    is ServerEvent.SessionIdle -> {
                        val sessionId = event.properties.sessionID
                        _state.update { current ->
                            current.copy(
                                runningSessionIds = current.runningSessionIds - sessionId
                            )
                        }
                        Napier.d("Session $sessionId is now idle")
                    }
                    
                    is ServerEvent.SessionError -> {
                        val sessionId = event.properties.sessionID
                        if (sessionId != null) {
                            _state.update { current ->
                                current.copy(
                                    runningSessionIds = current.runningSessionIds - sessionId
                                )
                            }
                            Napier.w("Session $sessionId encountered error")
                        }
                    }
                    
                    is ServerEvent.SessionDeleted -> {
                        val sessionId = event.properties.info.id
                        _state.update { current ->
                            val updatedSessions = current.sessions.filter { it.id != sessionId }
                            val updatedGroups = buildSessionGroups(updatedSessions)
                            current.copy(
                                sessions = updatedSessions,
                                sessionGroups = updatedGroups,
                                runningSessionIds = current.runningSessionIds - sessionId
                            )
                        }
                        Napier.d("Session $sessionId deleted")
                    }
                    
                    else -> { /* Ignore other events */ }
                }
            }
        }
    }
    
    /**
     * Build session groups from flat session list (used by observeSessionEvents).
     */
    private fun buildSessionGroups(sessions: List<Session>): List<SessionGroup> {
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
        
        // Build groups
        return rootSessions.map { parent ->
            val children = childrenByParent[parent.id] ?: emptyList()
            val isExpanded = _state.value.expandedGroupIds.contains(parent.id)
            SessionGroup(
                parent = parent,
                children = children.sortedByDescending { it.updatedAt },
                isExpanded = isExpanded
            )
        }.sortedByDescending { it.lastActivityTime }
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
    
    private fun observeConnectionState() {
        screenModelScope.launch {
            eventStreamRepository.connectionStatus.collect { status ->
                _state.update { current ->
                    when (status) {
                        is com.mocca.app.domain.model.ConnectionStatus.NotConfigured -> current.copy(
                            isConnected = false,
                            isConnecting = false,
                            isWaitingForNetwork = false,
                            mcpStatus = "NOT_CONFIGURED",
                            isMcpOnline = false
                        )
                        is com.mocca.app.domain.model.ConnectionStatus.Connected -> {
                            val config = connectionManager.activeConfig.value
                            current.copy(
                                isConnected = true,
                                isConnecting = false,
                                isWaitingForNetwork = false,
                                connectionError = null,
                                mcpStatus = "ONLINE",
                                isMcpOnline = true,
                                latency = "${status.latencyMs}ms",
                                port = ":${config?.port ?: "--"}"
                            )
                        }
                        is com.mocca.app.domain.model.ConnectionStatus.Connecting -> current.copy(
                            isConnected = false,
                            isConnecting = true,
                            isWaitingForNetwork = false,
                            connectionAttempt = 1,
                            mcpStatus = "CONNECTING",
                            isMcpOnline = false
                        )
                        is com.mocca.app.domain.model.ConnectionStatus.WaitingForNetwork -> current.copy(
                            isConnected = false,
                            isConnecting = false,
                            isWaitingForNetwork = true,
                            mcpStatus = "WAITING",
                            isMcpOnline = false
                        )
                        is com.mocca.app.domain.model.ConnectionStatus.Reconnecting -> current.copy(
                            isConnected = false,
                            isConnecting = true,
                            isWaitingForNetwork = false,
                            connectionAttempt = status.attempt,
                            mcpStatus = "RECONNECTING",
                            isMcpOnline = false
                        )
                        is com.mocca.app.domain.model.ConnectionStatus.Disconnected -> current.copy(
                            isConnected = false,
                            isConnecting = false,
                            isWaitingForNetwork = false,
                            mcpStatus = "OFFLINE",
                            isMcpOnline = false
                        )
                        is com.mocca.app.domain.model.ConnectionStatus.Error -> current.copy(
                            isConnected = false,
                            isConnecting = false,
                            isWaitingForNetwork = false,
                            connectionError = status.message,
                            mcpStatus = "ERROR",
                            isMcpOnline = false
                        )
                    }
                }
            }
        }
    }
    
    private fun loadSessions() {
        screenModelScope.launch {
            sessionRepository.getSessions().collect { resource ->
                when (resource) {
                    is Resource.Loading<*> -> _state.update { it.copy(isLoading = true) }
                    is Resource.Success<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val sessions = (resource.data as? List<Session>) ?: emptyList()
                        // Sort sessions by update time here to avoid expensive sorting in UI recomposition
                        val sortedSessions = sessions.sortedByDescending { it.updatedAt }
                        
                        // Build session groups with parent-child hierarchy
                        val sessionGroups = buildSessionGroups(sortedSessions)
                        
                        _state.update { current ->
                            val newState = current.copy(
                                isLoading = false,
                                sessions = sortedSessions,
                                sessionGroups = sessionGroups
                            )
                            // Auto-select first session if none selected and sessions exist
                            if (current.currentSessionId == null && sessions.isNotEmpty()) {
                                val firstSession = sessions.maxByOrNull { it.updatedAt }
                                if (firstSession != null) {
                                    loadMessages(firstSession.id)
                                    newState.copy(currentSessionId = firstSession.id)
                                } else {
                                    newState
                                }
                            } else {
                                newState
                            }
                        }
                        
                        // Fetch real-time session status after loading sessions
                        fetchSessionStatus()
                    }
                    is Resource.Error<*> -> _state.update { 
                        it.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Fetch real-time session status from API.
     */
    private fun fetchSessionStatus() {
        screenModelScope.launch {
            try {
                sessionRepository.getSessionStatus().fold(
                    onSuccess = { statusMap ->
                        val runningIds = statusMap.filter { (_, status) ->
                            status.isBusy || status.isRetrying
                        }.keys
                        _state.update { it.copy(runningSessionIds = runningIds) }
                        Napier.d("Session status updated: ${runningIds.size} running sessions")
                    },
                    onFailure = { error ->
                        Napier.w("Failed to fetch session status: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Napier.e("Error fetching session status", e)
            }
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
            val updatedGroups = current.sessionGroups.map { group ->
                if (group.parent.id == parentSessionId) {
                    group.copy(isExpanded = newExpandedIds.contains(parentSessionId))
                } else {
                    group
                }
            }
            current.copy(
                expandedGroupIds = newExpandedIds,
                sessionGroups = updatedGroups
            )
        }
    }
    
    private fun loadMessages(sessionId: String) {
        screenModelScope.launch {
            sessionRepository.getMessages(sessionId).collect { resource ->
                when (resource) {
                    is Resource.Loading<*> -> { /* Already loading sessions */ }
                    is Resource.Success<*> -> _state.update {
                        @Suppress("UNCHECKED_CAST")
                        val messages = (resource.data as? List<Message>) ?: emptyList()
                        it.copy(messages = messages)
                    }
                    is Resource.Error<*> -> _state.update { 
                        it.copy(error = resource.message)
                    }
                }
            }
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
        
        // Load messages in background
        screenModelScope.launch {
            loadMessages(sessionId)
            // Clear loading state after messages load
            _state.update { it.copy(
                loadingSessionId = null,
                isLoadingSession = false
            )}
        }
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
                            sessions = listOf(session) + current.sessions,
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
                            sessions = emptyList(),
                            currentSessionId = null,
                            messages = emptyList()
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
        eventStreamRepository.reconnect()
    }
    
    /**
     * Refresh all data - sessions, messages, config, and reconnection.
     * Called from DashboardPanel refresh button.
     */
    fun refreshAll() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // 1. Refresh sessions from server
                sessionRepository.refreshSessions()
                
                // 2. Force SSE reconnection to get fresh event stream
                eventStreamRepository.disconnect()
                kotlinx.coroutines.delay(500) // Brief delay before reconnecting
                _state.value.currentSessionId?.let { sessionId ->
                    eventStreamRepository.connect(screenModelScope, sessionId)
                }
                
                // 3. Reload sessions to update UI
                loadSessions()
                
                // 4. Reload messages for current session
                _state.value.currentSessionId?.let { sessionId ->
                    loadMessages(sessionId)
                }
                
                Napier.i("Global refresh completed")
            } catch (e: Exception) {
                Napier.e("Failed to refresh all data", e)
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
    
    override fun onDispose() {
        eventStreamRepository.disconnect()
    }
}
