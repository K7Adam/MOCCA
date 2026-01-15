package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.McpServerConfig
import com.mocca.app.domain.model.McpServerInfo
import com.mocca.app.domain.model.McpServerStatus
import com.mocca.app.domain.model.McpConnectionStatus
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repository for MCP server management.
 * Handles fetching status, connecting/disconnecting servers.
 * Uses in-memory caching since MCP status is transient.
 */
class McpRepository(
    private val apiClient: MoccaApiClient
) {
    private val _mcpServers = MutableStateFlow<Map<String, McpServerInfo>>(emptyMap())
    val mcpServers: Flow<Map<String, McpServerInfo>> = _mcpServers.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: Flow<String?> = _error.asStateFlow()
    
    /**
     * Get current cached MCP servers.
     */
    fun getCachedServers(): Map<String, McpServerInfo> = _mcpServers.value
    
    /**
     * Get list of MCP servers as a list for UI convenience.
     */
    fun getCachedServerList(): List<McpServerInfo> = _mcpServers.value.values.toList()
    
    /**
     * Refresh MCP server status from the server.
     */
    suspend fun refresh(directory: String? = null): Resource<Map<String, McpServerInfo>> {
        _isLoading.value = true
        _error.value = null
        
        return apiClient.getMcpStatus(directory).fold(
            onSuccess = { statusMap ->
                val servers = statusMap.mapValues { (name, status) ->
                    McpServerInfo(
                        name = name,
                        status = status,
                        config = null
                    )
                }
                _mcpServers.value = servers
                _isLoading.value = false
                Napier.d("MCP status refreshed: ${servers.size} servers")
                Resource.Success(servers)
            },
            onFailure = { error ->
                _isLoading.value = false
                _error.value = error.message
                Napier.e("Failed to refresh MCP status", error)
                Resource.Error(error.message ?: "Failed to refresh MCP status")
            }
        )
    }
    
    /**
     * Connect to an MCP server.
     */
    suspend fun connect(name: String, directory: String? = null): Resource<Unit> {
        Napier.d("Connecting to MCP server: $name")
        
        updateServerStatus(name, McpConnectionStatus.CONNECTING)
        
        return apiClient.connectMcp(name, directory).fold(
            onSuccess = {
                Napier.d("Successfully connected to MCP server: $name")
                refresh(directory)
                Resource.Success(Unit)
            },
            onFailure = { error ->
                Napier.e("Failed to connect to MCP server: $name", error)
                updateServerStatus(name, McpConnectionStatus.FAILED, error.message)
                Resource.Error(error.message ?: "Failed to connect to MCP server")
            }
        )
    }
    
    /**
     * Disconnect from an MCP server.
     */
    suspend fun disconnect(name: String, directory: String? = null): Resource<Unit> {
        Napier.d("Disconnecting from MCP server: $name")
        
        return apiClient.disconnectMcp(name, directory).fold(
            onSuccess = {
                Napier.d("Successfully disconnected from MCP server: $name")
                refresh(directory)
                Resource.Success(Unit)
            },
            onFailure = { error ->
                Napier.e("Failed to disconnect from MCP server: $name", error)
                Resource.Error(error.message ?: "Failed to disconnect from MCP server")
            }
        )
    }
    
    /**
     * Toggle MCP server connection status.
     */
    suspend fun toggleConnection(name: String, connect: Boolean, directory: String? = null): Resource<Unit> {
        return if (connect) {
            connect(name, directory)
        } else {
            disconnect(name, directory)
        }
    }
    
    /**
     * Configure an MCP server.
     */
    suspend fun configure(name: String, config: McpServerConfig): Resource<Unit> {
        Napier.d("Configuring MCP server: $name")
        
        return apiClient.configureMcp(name, config).fold(
            onSuccess = {
                Napier.d("Successfully configured MCP server: $name")
                refresh()
                Resource.Success(Unit)
            },
            onFailure = { error ->
                Napier.e("Failed to configure MCP server: $name", error)
                Resource.Error(error.message ?: "Failed to configure MCP server")
            }
        )
    }
    
    /**
     * Get a specific server by name.
     */
    fun getServer(name: String): McpServerInfo? = _mcpServers.value[name]
    
    /**
     * Clear cached data.
     */
    fun clearCache() {
        _mcpServers.value = emptyMap()
        _error.value = null
    }
    
    /**
     * Update server status optimistically for better UX.
     */
    private fun updateServerStatus(
        name: String,
        status: McpConnectionStatus,
        error: String? = null
    ) {
        _mcpServers.update { current ->
            val existingServer = current[name]
            if (existingServer != null) {
                current + (name to existingServer.copy(
                    status = McpServerStatus(
                        status = status,
                        error = error,
                        tools = existingServer.status.tools,
                        resources = existingServer.status.resources,
                        prompts = existingServer.status.prompts
                    )
                ))
            } else {
                current + (name to McpServerInfo(
                    name = name,
                    status = McpServerStatus(status = status, error = error)
                ))
            }
        }
    }
}
