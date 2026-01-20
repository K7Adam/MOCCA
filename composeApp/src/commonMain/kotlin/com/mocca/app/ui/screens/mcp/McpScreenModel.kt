package com.mocca.app.ui.screens.mcp

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.domain.model.McpConnectionStatus
import com.mocca.app.domain.model.McpServerInfo
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.McpServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for MCP management screen.
 */
data class McpScreenState(
    val servers: List<McpServerInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedServer: McpServerInfo? = null,
    val showServerDetails: Boolean = false,
    val operationInProgress: String? = null
) {
    val connectedCount: Int get() = servers.count { it.isConnected }
    val totalCount: Int get() = servers.size
    val hasServers: Boolean get() = servers.isNotEmpty()
}

/**
 * ScreenModel for MCP server management.
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
                _state.update { it.copy(
                    servers = serversMap.values.toList().sortedBy { server -> server.name }
                )}
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
    
    /**
     * Refresh MCP server status.
     */
    fun refresh() {
        screenModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            mcpRepository.refresh()
            _state.update { it.copy(isRefreshing = false) }
        }
    }
    
    /**
     * Connect to an MCP server.
     */
    fun connect(serverName: String) {
        screenModelScope.launch {
            _state.update { it.copy(operationInProgress = serverName) }
            mcpRepository.connect(serverName)
            _state.update { it.copy(operationInProgress = null) }
        }
    }
    
    /**
     * Disconnect from an MCP server.
     */
    fun disconnect(serverName: String) {
        screenModelScope.launch {
            _state.update { it.copy(operationInProgress = serverName) }
            mcpRepository.disconnect(serverName)
            _state.update { it.copy(operationInProgress = null) }
        }
    }
    
    /**
     * Toggle server connection status.
     */
    fun toggleConnection(serverName: String, connect: Boolean) {
        if (connect) {
            connect(serverName)
        } else {
            disconnect(serverName)
        }
    }
    
    /**
     * Select a server to view details.
     */
    fun selectServer(server: McpServerInfo) {
        _state.update { it.copy(
            selectedServer = server,
            showServerDetails = true
        )}
    }
    
    /**
     * Close server details view.
     */
    fun closeServerDetails() {
        _state.update { it.copy(
            selectedServer = null,
            showServerDetails = false
        )}
    }
    
    /**
     * Add a new MCP server.
     */
    fun addServer(name: String, config: McpServerConfig) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            mcpRepository.addServer(name, config)
            _state.update { it.copy(isLoading = false) }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}