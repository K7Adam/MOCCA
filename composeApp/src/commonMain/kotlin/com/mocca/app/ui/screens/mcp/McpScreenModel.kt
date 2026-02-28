package com.mocca.app.ui.screens.mcp

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.domain.model.McpConnectionStatus
import com.mocca.app.domain.model.McpOAuthState
import com.mocca.app.domain.model.McpServerInfo
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.McpServerConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for MCP management screen.
 */
@Immutable
data class McpScreenState(
    val servers: ImmutableList<McpServerInfo> = persistentListOf(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedServer: McpServerInfo? = null,
    val showServerDetails: Boolean = false,
    val operationInProgress: String? = null,
    // OAuth flow state
    val pendingOAuth: McpOAuthState? = null,
    val isOAuthInProgress: Boolean = false
) {
    val connectedCount: Int get() = servers.count { it.isConnected }
    val totalCount: Int get() = servers.size
    val hasServers: Boolean get() = servers.isNotEmpty()
    val showOAuthDialog: Boolean get() = pendingOAuth != null
}

/**
 * ScreenModel for MCP server management.
 * W3-T3: includes MCP OAuth flow support.
 */
class McpScreenModel(
    private val mcpRepository: McpRepository
) : ScreenModel {

    private val _state = MutableStateFlow(McpScreenState())
    val state: StateFlow<McpScreenState> = _state.asStateFlow()

    init {
        observeMcpServers()
        refresh()
    }

    private fun observeMcpServers() {
        screenModelScope.launch {
            mcpRepository.mcpServers.collect { serversMap ->
                _state.update {
                    it.copy(
                        servers = serversMap.values.toList()
                            .sortedBy { server -> server.name }
                            .toImmutableList()
                    )
                }
            }
        }

        screenModelScope.launch {
            mcpRepository.isLoading.collect { loading ->
                _state.update { it.copy(isLoading = loading) }
            }
        }

        screenModelScope.launch {
            mcpRepository.error.collect { error ->
                _state.update { it.copy(error = error) }
            }
        }
    }

    /** Refresh MCP server status. */
    fun refresh() {
        screenModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            mcpRepository.refresh()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    /** Connect to an MCP server. */
    fun connect(serverName: String) {
        screenModelScope.launch {
            _state.update { it.copy(operationInProgress = serverName) }
            mcpRepository.connect(serverName)
            _state.update { it.copy(operationInProgress = null) }
        }
    }

    /** Disconnect from an MCP server. */
    fun disconnect(serverName: String) {
        screenModelScope.launch {
            _state.update { it.copy(operationInProgress = serverName) }
            mcpRepository.disconnect(serverName)
            _state.update { it.copy(operationInProgress = null) }
        }
    }

    /** Toggle server connection status. */
    fun toggleConnection(serverName: String, connect: Boolean) {
        if (connect) connect(serverName) else disconnect(serverName)
    }

    /** Select a server to view details. */
    fun selectServer(server: McpServerInfo) {
        _state.update {
            it.copy(selectedServer = server, showServerDetails = true)
        }
    }

    /** Close server details view. */
    fun closeServerDetails() {
        _state.update {
            it.copy(selectedServer = null, showServerDetails = false)
        }
    }

    /** Add a new MCP server. */
    fun addServer(name: String, config: McpServerConfig) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            mcpRepository.addServer(name, config)
            _state.update { it.copy(isLoading = false) }
        }
    }

    /** Clear error message. */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // ─── OAuth Flow ──────────────────────────────────────────────────────────

    /**
     * Start the OAuth flow for a given MCP server.
     * On success, puts the server into [McpScreenState.pendingOAuth] to show the dialog.
     */
    fun startOAuthFlow(serverName: String) {
        screenModelScope.launch {
            _state.update { it.copy(isOAuthInProgress = true) }
            mcpRepository.startOAuthFlow(serverName).fold(
                onSuccess = { oauthState ->
                    _state.update { it.copy(pendingOAuth = oauthState, isOAuthInProgress = false) }
                },
                onFailure = { e ->
                    Napier.e("[McpScreenModel] startOAuthFlow failed: ${e.message}")
                    _state.update {
                        it.copy(
                            error = "OAuth init failed: ${e.message}",
                            isOAuthInProgress = false
                        )
                    }
                }
            )
        }
    }

    /**
     * Submit the OAuth authorization code received after the user completes the browser flow.
     */
    fun submitOAuthCode(code: String) {
        val serverName = _state.value.pendingOAuth?.serverName ?: return
        screenModelScope.launch {
            _state.update { it.copy(isOAuthInProgress = true) }
            mcpRepository.handleOAuthCallback(serverName, code).fold(
                onSuccess = {
                    _state.update {
                        it.copy(pendingOAuth = null, isOAuthInProgress = false)
                    }
                    // Re-connect to pick up the newly authorized credentials
                    connect(serverName)
                },
                onFailure = { e ->
                    Napier.e("[McpScreenModel] submitOAuthCode failed: ${e.message}")
                    _state.update {
                        it.copy(
                            pendingOAuth = it.pendingOAuth?.copy(error = e.message),
                            isOAuthInProgress = false
                        )
                    }
                }
            )
        }
    }

    /** Dismiss the OAuth dialog without completing auth. */
    fun dismissOAuthDialog() {
        _state.update { it.copy(pendingOAuth = null, isOAuthInProgress = false) }
    }
}
