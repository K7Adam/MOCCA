package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.bridge.opencode.BridgeFeatureUnavailableException
import com.mocca.app.bridge.opencode.OpenCodeBridgeRepository
import com.mocca.app.domain.model.Command
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Repository for slash commands.
 */
class CommandRepository(
    private val apiClient: MoccaApiClient,
    private val bridgeConnectionManager: BridgeConnectionManager
) {
    // Cached commands for quick access
    private val _cachedCommands = MutableStateFlow<List<Command>>(emptyList())
    val cachedCommands: StateFlow<List<Command>> = _cachedCommands.asStateFlow()
    
    /**
     * Get all available slash commands from /command endpoint.
     */
    fun getCommands(): Flow<Resource<List<Command>>> = flow {
        emit(Resource.Loading())

        // Try bridge-first
        val bridgeStatus = bridgeConnectionManager.status.value
        if (bridgeStatus is BridgeConnectionStatus.Connected) {
            try {
                val client = bridgeConnectionManager.client.value
                    ?: throw BridgeFeatureUnavailableException("MOCCA CLI connection")
                // Check if commands namespace is available
                if ("commands" in bridgeStatus.capabilities.namespaces) {
                    Napier.d("[CommandRepository] Fetching commands from bridge")
                    val result = OpenCodeBridgeRepository(client).fetchCommands()
                    val commands = result.map { info -> Command(name = info.name, description = info.description ?: "") }
                    _cachedCommands.value = commands
                    emit(Resource.Success(commands))
                    return@flow
                }
            } catch (e: Exception) {
                Napier.w("[CommandRepository] Bridge failed, HTTP fallback", e)
            }
        }

        apiClient.getCommands().fold(
            onSuccess = { commands ->
                _cachedCommands.value = commands
                Napier.d("Successfully fetched ${commands.size} commands")
                emit(Resource.Success(commands))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch commands: ${error::class.simpleName} - ${error.message}", error)
                error.cause?.let { cause ->
                    Napier.e("  Cause: ${cause::class.simpleName} - ${cause.message}")
                }
                emit(Resource.Error(error.message ?: "Failed to fetch commands"))
            }
        )
    }
    
    /**
     * Refresh commands from server.
     * Called by RealtimeSyncService during periodic sync.
     */
    suspend fun refresh() {
        apiClient.getCommands().fold(
            onSuccess = { commands ->
                _cachedCommands.value = commands
                Napier.d("[CommandRepository] Refreshed ${commands.size} commands")
            },
            onFailure = { error ->
                Napier.w("[CommandRepository] Failed to refresh commands: ${error.message}")
                throw error
            }
        )
    }
}
