package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.Agent
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for agent configuration.
 */
class AgentRepository(
    private val apiClient: MoccaApiClient
) {
    /**
     * Get all available agents from /agent endpoint.
     */
    fun getAgents(): Flow<Resource<List<Agent>>> = flow {
        emit(Resource.Loading())
        apiClient.getAgents().fold(
            onSuccess = { agents ->
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
}
