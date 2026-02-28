package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.WorktreeCreateRequest
import com.mocca.app.domain.model.WorktreeInfo
import com.mocca.app.domain.model.WorktreeResetRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Repository for Git worktree management.
 *
 * Exposes CRUD operations for OpenCode's experimental worktree API.
 * Uses in-memory caching (worktrees are transient workspace state).
 */
class WorktreeRepository(
    private val apiClient: MoccaApiClient
) {

    /** List all worktrees — emits Loading then Success/Error. */
    fun getWorktrees(): Flow<Resource<List<WorktreeInfo>>> = flow {
        emit(Resource.Loading())

        apiClient.listWorktrees().fold(
            onSuccess = { worktrees ->
                Napier.d("[WorktreeRepository] Listed ${worktrees.size} worktrees")
                emit(Resource.Success(worktrees))
            },
            onFailure = { error ->
                Napier.e("[WorktreeRepository] Failed to list worktrees", error)
                emit(Resource.Error(error.message ?: "Failed to list worktrees"))
            }
        )
    }.flowOn(Dispatchers.IO)

    /** Create a new worktree from a branch name. */
    suspend fun createWorktree(branch: String?, sessionId: String? = null): Resource<WorktreeInfo> {
        Napier.d("[WorktreeRepository] Creating worktree for branch: $branch")
        return apiClient.createWorktree(WorktreeCreateRequest(branch = branch, sessionID = sessionId)).fold(
            onSuccess = { worktree ->
                Napier.d("[WorktreeRepository] Created worktree: ${worktree.id}")
                Resource.Success(worktree)
            },
            onFailure = { error ->
                Napier.e("[WorktreeRepository] Failed to create worktree", error)
                Resource.Error(error.message ?: "Failed to create worktree")
            }
        )
    }

    /** Delete a worktree by ID. */
    suspend fun deleteWorktree(id: String): Resource<Unit> {
        Napier.d("[WorktreeRepository] Deleting worktree: $id")
        return apiClient.deleteWorktree(id).fold(
            onSuccess = {
                Napier.d("[WorktreeRepository] Deleted worktree: $id")
                Resource.Success(Unit)
            },
            onFailure = { error ->
                Napier.e("[WorktreeRepository] Failed to delete worktree: $id", error)
                Resource.Error(error.message ?: "Failed to delete worktree")
            }
        )
    }

    /** Reset a worktree to a clean state. */
    suspend fun resetWorktree(id: String): Resource<WorktreeInfo> {
        Napier.d("[WorktreeRepository] Resetting worktree: $id")
        return apiClient.resetWorktree(id, WorktreeResetRequest(id = id)).fold(
            onSuccess = { worktree ->
                Napier.d("[WorktreeRepository] Reset worktree: $id")
                Resource.Success(worktree)
            },
            onFailure = { error ->
                Napier.e("[WorktreeRepository] Failed to reset worktree: $id", error)
                Resource.Error(error.message ?: "Failed to reset worktree")
            }
        )
    }
}
