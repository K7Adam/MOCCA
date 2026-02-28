package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.CrossProjectSession
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Repository for cross-project sessions via the experimental /session endpoint.
 *
 * Returns sessions spanning multiple projects as tracked by the OpenCode server.
 * No local cache (experimental data is transient).
 */
class CrossProjectSessionsRepository(
    private val apiClient: MoccaApiClient
) {

    /** Load all cross-project sessions — emits Loading then Success/Error. */
    fun getCrossProjectSessions(): Flow<Resource<List<CrossProjectSession>>> = flow {
        emit(Resource.Loading())
        apiClient.listCrossProjectSessions().fold(
            onSuccess = { sessions ->
                Napier.d("[CrossProjectSessionsRepository] Listed ${sessions.size} sessions")
                emit(Resource.Success(sessions))
            },
            onFailure = { error ->
                Napier.e("[CrossProjectSessionsRepository] Failed to list sessions", error)
                emit(Resource.Error(error.message ?: "Failed to load sessions"))
            }
        )
    }.flowOn(Dispatchers.IO)
}
