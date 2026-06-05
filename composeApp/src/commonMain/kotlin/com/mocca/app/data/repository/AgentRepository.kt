package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.bridge.opencode.BridgeFeatureUnavailableException
import com.mocca.app.bridge.opencode.OpenCodeBridgeRepository
import com.mocca.app.domain.model.Agent
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Repository for agent configuration.
 */
class AgentRepository(
    private val apiClient: MoccaApiClient,
    private val bridgeConnectionManager: BridgeConnectionManager
) {
    // Cached agents for quick access
    private val _cachedAgents = MutableStateFlow<List<Agent>>(emptyList())
    val cachedAgents: StateFlow<List<Agent>> = _cachedAgents.asStateFlow()
    
    /**
     * Get all available agents from /agent endpoint.
     */
    fun getAgents(): Flow<Resource<List<Agent>>> = flow {
        emit(Resource.Loading())

        // Try bridge-first
        val bridgeStatus = bridgeConnectionManager.status.value
        if (bridgeStatus is BridgeConnectionStatus.Connected) {
            try {
                val client = bridgeConnectionManager.client.value
                    ?: throw BridgeFeatureUnavailableException("MOCCA CLI connection")
                if (bridgeStatus.capabilities.ai.agents) {
                    Napier.d("[AgentRepository] Fetching agents from bridge")
                    val result = OpenCodeBridgeRepository(client).fetchAgents()
                    val agents = result.map { info -> Agent(name = info.name) }
                    _cachedAgents.value = agents
                    emit(Resource.Success(agents))
                    return@flow
                }
            } catch (e: Exception) {
                Napier.w("[AgentRepository] Bridge failed, HTTP fallback", e)
            }
        }

        apiClient.getAgents().fold(
            onSuccess = { agents ->
                _cachedAgents.value = agents
                Napier.d("Successfully fetched ${agents.size} agents")
                emit(Resource.Success(agents))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch agents: ${error::class.simpleName} - ${error.message}", error)
                error.cause?.let { cause ->
                    Napier.e("  Cause: ${cause::class.simpleName} - ${cause.message}")
                }
                emit(Resource.Error(error.message ?: "Failed to fetch agents"))
            }
        )
    }
    
    /**
     * Refresh agents from server.
     * Called by RealtimeSyncService during periodic sync.
     */
    suspend fun refresh() {
        apiClient.getAgents().fold(
            onSuccess = { agents ->
                _cachedAgents.value = agents
                Napier.d("[AgentRepository] Refreshed ${agents.size} agents")
            },
            onFailure = { error ->
                Napier.w("[AgentRepository] Failed to refresh agents: ${error.message}")
                throw error
            }
        )
    }
}
