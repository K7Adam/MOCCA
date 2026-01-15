package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for Git operations.
 * Provides offline-first access to Git status and operations.
 */
class GitRepository(
    private val apiClient: MoccaApiClient,
    private val localCache: LocalCache
) {
    /**
     * Get current Git status with staged, unstaged, and untracked files.
     */
    fun getStatus(): Flow<Resource<GitStatusResponse>> = flow {
        emit(Resource.Loading())
        
        // Try to get from cache first
        val cached = localCache.getGitStatus()
        if (cached != null) {
            emit(Resource.Loading(cached))
        }
        
        // Fetch fresh from API
        apiClient.getGitStatus()
            .onSuccess { status ->
                localCache.saveGitStatus(status)
                emit(Resource.Success(status))
            }
            .onFailure { error ->
                emit(Resource.Error(error.message ?: "Failed to get Git status", cached))
            }
    }
    
    /**
     * Get list of branches (local and remote).
     */
    fun getBranches(): Flow<Resource<List<GitBranch>>> = flow {
        emit(Resource.Loading())
        
        apiClient.getGitBranches()
            .onSuccess { branches ->
                emit(Resource.Success(branches))
            }
            .onFailure { error ->
                emit(Resource.Error(error.message ?: "Failed to get branches"))
            }
    }
    
    /**
     * Get commit log for a branch.
     */
    fun getLog(
        branch: String? = null,
        limit: Int = 50,
        skip: Int = 0
    ): Flow<Resource<GitLog>> = flow {
        emit(Resource.Loading())
        
        apiClient.getGitLog(branch, limit, skip)
            .onSuccess { log ->
                emit(Resource.Success(log))
            }
            .onFailure { error ->
                emit(Resource.Error(error.message ?: "Failed to get commit log"))
            }
    }
    
    /**
     * Get diff for working directory or a specific ref.
     */
    fun getDiff(
        ref: String? = null,
        cached: Boolean = false
    ): Flow<Resource<GitDiff>> = flow {
        emit(Resource.Loading())
        
        apiClient.getGitDiff(ref, cached)
            .onSuccess { diff ->
                emit(Resource.Success(diff))
            }
            .onFailure { error ->
                emit(Resource.Error(error.message ?: "Failed to get diff"))
            }
    }
    
    /**
     * Stage files for commit.
     */
    suspend fun stage(files: List<String>): Result<GitOperationResult> {
        return apiClient.gitStage(files).also { result ->
            result.onSuccess {
                // Refresh status after staging
                apiClient.getGitStatus().onSuccess { status ->
                    localCache.saveGitStatus(status)
                }
            }
        }
    }
    
    /**
     * Unstage files.
     */
    suspend fun unstage(files: List<String>): Result<GitOperationResult> {
        return apiClient.gitUnstage(files).also { result ->
            result.onSuccess {
                apiClient.getGitStatus().onSuccess { status ->
                    localCache.saveGitStatus(status)
                }
            }
        }
    }
    
    /**
     * Discard changes to files.
     */
    suspend fun discard(files: List<String>): Result<GitOperationResult> {
        return apiClient.gitDiscard(files).also { result ->
            result.onSuccess {
                apiClient.getGitStatus().onSuccess { status ->
                    localCache.saveGitStatus(status)
                }
            }
        }
    }
    
    /**
     * Create a commit.
     */
    suspend fun commit(
        message: String,
        files: List<String>? = null,
        amend: Boolean = false
    ): Result<GitOperationResult> {
        return apiClient.gitCommit(message, files, amend).also { result ->
            result.onSuccess {
                apiClient.getGitStatus().onSuccess { status ->
                    localCache.saveGitStatus(status)
                }
            }
        }
    }
    
    /**
     * Push commits to remote.
     */
    suspend fun push(
        remote: String = "origin",
        branch: String? = null,
        force: Boolean = false,
        setUpstream: Boolean = false
    ): Result<GitOperationResult> {
        return apiClient.gitPush(remote, branch, force, setUpstream)
    }
    
    /**
     * Pull changes from remote.
     */
    suspend fun pull(
        remote: String = "origin",
        branch: String? = null,
        rebase: Boolean = false
    ): Result<GitOperationResult> {
        return apiClient.gitPull(remote, branch, rebase).also { result ->
            result.onSuccess {
                apiClient.getGitStatus().onSuccess { status ->
                    localCache.saveGitStatus(status)
                }
            }
        }
    }
    
    /**
     * Fetch from remote.
     */
    suspend fun fetch(
        remote: String = "origin",
        prune: Boolean = false,
        all: Boolean = false
    ): Result<GitOperationResult> {
        return apiClient.gitFetch(remote, prune, all)
    }
    
    /**
     * Checkout a branch, tag, or commit.
     */
    suspend fun checkout(
        ref: String,
        create: Boolean = false,
        force: Boolean = false
    ): Result<GitOperationResult> {
        return apiClient.gitCheckout(ref, create, force).also { result ->
            result.onSuccess {
                apiClient.getGitStatus().onSuccess { status ->
                    localCache.saveGitStatus(status)
                }
            }
        }
    }
    
    /**
     * Get list of remotes.
     */
    fun getRemotes(): Flow<Resource<List<GitRemote>>> = flow {
        emit(Resource.Loading())
        
        apiClient.getGitRemotes()
            .onSuccess { remotes ->
                emit(Resource.Success(remotes))
            }
            .onFailure { error ->
                emit(Resource.Error(error.message ?: "Failed to get remotes"))
            }
    }
    
    /**
     * Get list of stashes.
     */
    fun getStashes(): Flow<Resource<List<GitStash>>> = flow {
        emit(Resource.Loading())
        
        apiClient.getGitStashes()
            .onSuccess { stashes ->
                emit(Resource.Success(stashes))
            }
            .onFailure { error ->
                emit(Resource.Error(error.message ?: "Failed to get stashes"))
            }
    }
    
    /**
     * Force refresh the Git status.
     */
    suspend fun refreshStatus(): Result<GitStatusResponse> {
        return apiClient.getGitStatus().also { result ->
            result.onSuccess { status ->
                localCache.saveGitStatus(status)
            }
        }
    }
    
    /**
     * Get VCS info from /vcs endpoint.
     * Provides simplified repository state (branch, dirty, ahead/behind).
     */
    fun getVcsInfo(): Flow<Resource<VcsInfo>> = flow {
        emit(Resource.Loading())
        
        apiClient.getVcsInfo()
            .onSuccess { vcsInfo ->
                emit(Resource.Success(vcsInfo))
            }
            .onFailure { error ->
                emit(Resource.Error(error.message ?: "Failed to get VCS info"))
            }
    }
}
