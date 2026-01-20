package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.Project
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ProjectRepository(
    private val apiClient: MoccaApiClient
) {
    fun getProjects(): Flow<Resource<List<Project>>> = flow {
        emit(Resource.Loading())
        apiClient.listProjects().fold(
            onSuccess = { projects ->
                emit(Resource.Success(projects))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch projects", error)
                emit(Resource.Error(error.message ?: "Failed to fetch projects"))
            }
        )
    }.flowOn(Dispatchers.IO)

    fun getCurrentProject(): Flow<Resource<Project>> = flow {
        emit(Resource.Loading())
        apiClient.getCurrentProject().fold(
            onSuccess = { project ->
                emit(Resource.Success(project))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch current project", error)
                emit(Resource.Error(error.message ?: "Failed to fetch current project"))
            }
        )
    }.flowOn(Dispatchers.IO)
}