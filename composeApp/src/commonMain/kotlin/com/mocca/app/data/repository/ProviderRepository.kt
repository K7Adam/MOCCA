package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.domain.model.ProvidersConfig
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Repository for provider and model configuration.
 */
class ProviderRepository(
    private val apiClient: MoccaApiClient
) {
    // Cached providers for quick access
    private val _cachedProviders = MutableStateFlow<ProviderResponse?>(null)
    val cachedProviders: StateFlow<ProviderResponse?> = _cachedProviders.asStateFlow()
    
    /**
     * Get providers from /provider endpoint.
     */
    fun getProviders(): Flow<Resource<ProviderResponse>> = flow {
        emit(Resource.Loading())
        apiClient.getProviderInfo().fold(
            onSuccess = { response ->
                _cachedProviders.value = response
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
    
    /**
     * Refresh providers from server.
     * Called by RealtimeSyncService during periodic sync.
     */
    suspend fun refresh() {
        apiClient.getProviderInfo().fold(
            onSuccess = { response ->
                _cachedProviders.value = response
                Napier.d("[ProviderRepository] Refreshed providers: ${response.all.size}")
            },
            onFailure = { error ->
                Napier.w("[ProviderRepository] Failed to refresh providers: ${error.message}")
                throw error
            }
        )
    }
}
