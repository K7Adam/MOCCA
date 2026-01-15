package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.domain.model.ProvidersConfig
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for provider and model configuration.
 */
class ProviderRepository(
    private val apiClient: MoccaApiClient
) {
    /**
     * Get providers from /provider endpoint.
     */
    fun getProviders(): Flow<Resource<ProviderResponse>> = flow {
        emit(Resource.Loading())
        apiClient.getProviderInfo().fold(
            onSuccess = { response ->
                emit(Resource.Success(response))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch providers", error)
                emit(Resource.Error(error.message ?: "Failed to fetch providers"))
            }
        )
    }
    
    /**
     * Get providers config from /config/providers endpoint.
     */
    fun getProvidersConfig(): Flow<Resource<ProvidersConfig>> = flow {
        emit(Resource.Loading())
        apiClient.getProvidersConfig().fold(
            onSuccess = { config ->
                emit(Resource.Success(config))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch providers config", error)
                emit(Resource.Error(error.message ?: "Failed to fetch providers config"))
            }
        )
    }
}
