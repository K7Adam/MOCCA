package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.FormatterStatus
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for formatter status.
 */
class FormatterRepository(
    private val apiClient: MoccaApiClient
) {
    /**
     * Get all formatter statuses from /formatter endpoint.
     */
    fun getFormatters(): Flow<Resource<List<FormatterStatus>>> = flow {
        emit(Resource.Loading())
        apiClient.getFormatters().fold(
            onSuccess = { formatters ->
                emit(Resource.Success(formatters))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch formatters", error)
                emit(Resource.Error(error.message ?: "Failed to fetch formatters"))
            }
        )
    }
}
