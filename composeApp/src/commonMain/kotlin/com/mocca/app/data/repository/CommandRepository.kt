package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.Command
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for slash commands.
 */
class CommandRepository(
    private val apiClient: MoccaApiClient
) {
    /**
     * Get all available slash commands from /command endpoint.
     */
    fun getCommands(): Flow<Resource<List<Command>>> = flow {
        emit(Resource.Loading())
        apiClient.getCommands().fold(
            onSuccess = { commands ->
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
}
