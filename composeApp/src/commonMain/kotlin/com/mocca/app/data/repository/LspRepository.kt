package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.LspStatus
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for Language Server Protocol status.
 */
class LspRepository(
    private val apiClient: MoccaApiClient
) {
    /**
     * Get all LSP server statuses from /lsp endpoint.
     */
    fun getLspStatus(): Flow<Resource<List<LspStatus>>> = flow {
        emit(Resource.Loading())
        apiClient.getLspStatus().fold(
            onSuccess = { lspStatus ->
                emit(Resource.Success(lspStatus))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch LSP status", error)
                emit(Resource.Error(error.message ?: "Failed to fetch LSP status"))
            }
        )
    }
}
