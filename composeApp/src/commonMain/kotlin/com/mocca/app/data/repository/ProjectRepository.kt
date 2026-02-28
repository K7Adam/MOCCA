package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.Project
import com.mocca.app.domain.model.ProjectUpdateRequest
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ProjectRepository(
    private val apiClient: MoccaApiClient
) {
    fun getProjects(): Flow<Resource<List<Project>>> = flow {
        emit(Resource.Loading())
        try {
            val result = apiClient.listProjects()
            result.fold(
                onSuccess = { projects ->
                    Napier.d { "ProjectRepository: Received ${projects.size} projects from API" }
                    projects.forEach { project ->
                        Napier.d { "Project: id=${project.id}, name=${project.name}, path=${project.path}, directory=${project.directory}, displayName=${project.displayName}" }
                    }
                    emit(Resource.Success(projects))
                },
                onFailure = { error ->
                    Napier.e("Failed to fetch projects", error)
                    emit(Resource.Error(error.message ?: "Failed to fetch projects"))
                }
            )
        } catch (e: Exception) {
            Napier.e("Exception in getProjects", e)
            emit(Resource.Error(e.message ?: "Unknown error fetching projects"))
        }
    }.flowOn(Dispatchers.IO)

    fun getCurrentProject(): Flow<Resource<Project>> = flow {
        emit(Resource.Loading())
        try {
            val result = apiClient.getCurrentProject()
            result.fold(
                onSuccess = { project ->
                    Napier.d { "ProjectRepository: Current project received: id=${project.id}, name=${project.name}, path=${project.path}, directory=${project.directory}, displayName=${project.displayName}" }
                    emit(Resource.Success(project))
                },
                onFailure = { error ->
                    Napier.e("Failed to fetch current project", error)
                    emit(Resource.Error(error.message ?: "Failed to fetch current project"))
                }
            )
        } catch (e: Exception) {
            Napier.e("Exception in getCurrentProject", e)
            emit(Resource.Error(e.message ?: "Unknown error fetching current project"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun updateProject(projectId: String, newPath: String): Resource<Project> =
        withContext(Dispatchers.IO) {
            try {
                val result = apiClient.updateProject(projectId, ProjectUpdateRequest(path = newPath))
                result.fold(
                    onSuccess = { project ->
                        Napier.d { "ProjectRepository: Updated project $projectId path to $newPath" }
                        Resource.Success(project)
                    },
                    onFailure = { error ->
                        Napier.e("Failed to update project", error)
                        Resource.Error(error.message ?: "Failed to update project")
                    }
                )
            } catch (e: Exception) {
                Napier.e("Exception in updateProject", e)
                Resource.Error(e.message ?: "Unknown error updating project")
            }
        }
}