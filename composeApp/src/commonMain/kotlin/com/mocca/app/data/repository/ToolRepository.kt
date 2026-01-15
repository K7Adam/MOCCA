package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for tool information.
 */
class ToolRepository(
    private val apiClient: MoccaApiClient
) {
    /**
     * Get all available tool IDs from /experimental/tool/ids endpoint.
     */
    fun getToolIds(): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading())
        apiClient.getToolIds().fold(
            onSuccess = { tools ->
                emit(Resource.Success(tools))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch tools", error)
                emit(Resource.Error(error.message ?: "Failed to fetch tools"))
            }
        )
    }
}
