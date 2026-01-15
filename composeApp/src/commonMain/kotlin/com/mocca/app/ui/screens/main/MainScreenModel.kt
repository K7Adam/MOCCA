package com.mocca.app.ui.screens.main

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.AppConnectionManager
import com.mocca.app.data.repository.AppConnectionState
import com.mocca.app.data.repository.EventStreamRepository
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.data.repository.SessionRepository
import com.mocca.app.domain.model.McpServerInfo
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.Session
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
    val modelName: String = "CLAUDE OPUS 4.5",
    val agentName: String = "SISYPHUS",
    val latency: String = "--ms",
    val port: String = ":4096",
    val usedTokens: Int = 0,
    val maxTokens: Int = 32000,
    
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
    val appVersion: String = "V2.0.4"
) {
    val mcpConnectedCount: Int get() = mcpServers.count { it.isConnected }
    val mcpTotalCount: Int get() = mcpServers.size
}

/**
 * ScreenModel for the main screen.
 */
class MainScreenModel(
    private val initialSessionId: String?,
    private val sessionRepository: SessionRepository,
    private val eventStreamRepository: EventStreamRepository,
    private val appConnectionManager: AppConnectionManager,
    private val mcpRepository: McpRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(MainScreenState(currentSessionId = initialSessionId))
    val state: StateFlow<MainScreenState> = _state.asStateFlow()
    
    init {
        observeAppConnectionState()
        observeConnectionState()
        observeMcpServers()
        loadSessions()
        if (initialSessionId != null) {
            loadMessages(initialSessionId)
        }
    }
    
    private fun observeAppConnectionState() {
        screenModelScope.launch {
            appConnectionManager.connectionState.collect { appState ->
                when (appState) {
                    is AppConnectionState.Connected -> {
                        connectToEventStream()
                        loadSessions()
                    }
                    is AppConnectionState.Disconnected -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = false,
                            connectionError = appState.error,
                            mcpStatus = "OFFLINE",
                            isMcpOnline = false
                        )}
                    }
                    is AppConnectionState.Connecting -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = true,
                            connectionAttempt = appState.attempt,
                            mcpStatus = "CONNECTING",
                            isMcpOnline = false
                        )}
                    }
                    is AppConnectionState.Reconnecting -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = true,
                            connectionAttempt = appState.attempt,
                            mcpStatus = "RECONNECTING",
                            isMcpOnline = false
                        )}
                    }
                    is AppConnectionState.WaitingForNetwork -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = false,
                            isWaitingForNetwork = true,
                            mcpStatus = "WAITING",
                            isMcpOnline = false
                        )}
                    }
                    is AppConnectionState.Checking -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = true,
                            mcpStatus = "CHECKING",
                            isMcpOnline = false
                        )}
                    }
                    is AppConnectionState.NotConfigured -> {
                        _state.update { it.copy(
                            isConnected = false,
                            isConnecting = false,
                            mcpStatus = "NOT_CONFIGURED",
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
                        is com.mocca.app.domain.model.ConnectionStatus.Connected -> current.copy(
                            isConnected = true,
                            isConnecting = false,
                            isWaitingForNetwork = false,
                            connectionError = null,
                            mcpStatus = "ONLINE",
                            isMcpOnline = true
                        )
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
                        _state.update { current ->
                            val newState = current.copy(
                                isLoading = false,
                                sessions = sortedSessions
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
        appConnectionManager.checkConnection()
        eventStreamRepository.reconnect()
    }
    
    override fun onDispose() {
        eventStreamRepository.disconnect()
    }
}
