package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository for configuration, authentication, and OAuth flows.
 */
class ConfigRepository(
    private val apiClient: MoccaApiClient
) {
    /**
     * Get full configuration from /config endpoint.
     * This includes default provider, default model, modes, etc.
     */
    fun getConfig(): Flow<Resource<ConfigResponse>> = flow {
        emit(Resource.Loading())
        apiClient.getConfig().fold(
            onSuccess = { config ->
                Napier.i { "Config loaded: defaultModel=${config.model}, modes=${config.modes.size}" }
                emit(Resource.Success(config))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch config", error)
                emit(Resource.Error(error.message ?: "Failed to fetch config"))
            }
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Get providers configuration from /config/providers endpoint.
     */
    fun getProvidersConfig(): Flow<Resource<ProvidersConfig>> = flow {
        emit(Resource.Loading())
        apiClient.getProvidersConfig().fold(
            onSuccess = { config ->
                Napier.i { "Providers config loaded: ${config.providers.size} providers" }
                emit(Resource.Success(config))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch providers config", error)
                emit(Resource.Error(error.message ?: "Failed to fetch providers config"))
            }
        )
    }.flowOn(Dispatchers.IO)
    // ═══════════════════════════════════════════════════════════════════════
    // OAUTH & PROVIDER AUTHENTICATION (Priority 1.1)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get available authentication methods for a provider.
     */
    fun getProviderAuthMethods(providerId: String): Flow<Resource<List<ProviderAuthMethod>>> = flow {
        emit(Resource.Loading())
        apiClient.getProviderAuthMethods(providerId).fold(
            onSuccess = { methods ->
                emit(Resource.Success(methods))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch auth methods for $providerId", error)
                emit(Resource.Error(error.message ?: "Failed to fetch auth methods"))
            }
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Initiate OAuth authorization flow for a provider.
     * Returns the authorization URL to redirect the user to.
     */
    suspend fun startOAuthFlow(providerId: String): Resource<ProviderAuthAuthorization> = withContext(Dispatchers.IO) {
        apiClient.authorizeProvider(providerId).fold(
            onSuccess = { auth ->
                Resource.Success(auth)
            },
            onFailure = { error ->
                Napier.e("Failed to start OAuth flow for $providerId", error)
                Resource.Error(error.message ?: "Failed to start OAuth flow")
            }
        )
    }

    /**
     * Complete OAuth flow after user authorizes.
     */
    suspend fun completeOAuthFlow(providerId: String, code: String, state: String): Resource<Unit> = withContext(Dispatchers.IO) {
        apiClient.handleOAuthCallback(providerId, code, state).fold(
            onSuccess = {
                Resource.Success(Unit)
            },
            onFailure = { error ->
                Napier.e("OAuth callback failed for $providerId", error)
                Resource.Error(error.message ?: "OAuth callback failed")
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MANUAL API KEY AUTHENTICATION (Priority 1.2)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Set provider authentication credentials manually (API key).
     */
    suspend fun setProviderCredentials(providerId: String, credentials: ProviderCredentials): Resource<Unit> = withContext(Dispatchers.IO) {
        apiClient.setProviderAuth(providerId, credentials).fold(
            onSuccess = {
                Napier.i("Provider credentials set for $providerId")
                Resource.Success(Unit)
            },
            onFailure = { error ->
                Napier.e("Failed to set credentials for $providerId", error)
                Resource.Error(error.message ?: "Failed to set credentials")
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONFIG WRITE (Priority 1.3)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update configuration settings.
     */
    suspend fun updateConfig(update: ConfigUpdate): Resource<ConfigResponse> = withContext(Dispatchers.IO) {
        apiClient.updateConfig(update).fold(
            onSuccess = { config ->
                Napier.i("Config updated successfully")
                Resource.Success(config)
            },
            onFailure = { error ->
                Napier.e("Failed to update config", error)
                Resource.Error(error.message ?: "Failed to update config")
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GLOBAL CONFIG (GET/PATCH /global/config)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get global application config from /global/config.
     * Includes autoshare, autoupdate, telemetry, and experimental flags.
     */
    fun getGlobalConfig(): Flow<Resource<GlobalAppConfig>> = flow {
        emit(Resource.Loading())
        apiClient.getGlobalConfig().fold(
            onSuccess = { config ->
                Napier.i { "Global config loaded" }
                emit(Resource.Success(config))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch global config", error)
                emit(Resource.Error(error.message ?: "Failed to fetch global config"))
            }
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Update global application config (autoshare, autoupdate, telemetry, experimental flags).
     */
    suspend fun updateGlobalConfig(update: AppConfigUpdate): Resource<GlobalAppConfig> = withContext(Dispatchers.IO) {
        apiClient.updateGlobalConfig(update).fold(
            onSuccess = { config ->
                Napier.i { "Global config updated" }
                Resource.Success(config)
            },
            onFailure = { error ->
                Napier.e("Failed to update global config", error)
                Resource.Error(error.message ?: "Failed to update global config")
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROVIDER AUTH REMOVAL (Priority 1.4)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Remove stored authentication credentials for a provider.
     */
    suspend fun deleteProviderAuth(providerId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        apiClient.deleteProviderAuth(providerId).fold(
            onSuccess = {
                Napier.i("Provider auth removed for $providerId")
                Resource.Success(Unit)
            },
            onFailure = { error ->
                Napier.e("Failed to remove auth for $providerId", error)
                Resource.Error(error.message ?: "Failed to remove auth")
            }
        )
    }
}

