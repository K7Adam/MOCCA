package com.mocca.app.data.repository

import com.mocca.app.api.GitApiClient
import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GitRepository(
    private val gitApiClient: GitApiClient,
    private val localCache: LocalCache
) {
    companion object {
        private const val TAG = "GitRepository"
    }

    fun getStatus(): Flow<Resource<GitStatusResponse>> = flow {
        emit(Resource.Loading())
        val cached = localCache.getGitStatus()
        if (cached != null) {
            emit(Resource.Loading(cached))
        }
        
        Napier.d("$TAG: Fetching git status via API...")
        gitApiClient.getStatus().fold(
            onSuccess = { status ->
                localCache.saveGitStatus(status)
                Napier.d("$TAG: Git status fetched: branch=${status.branch}")
                emit(Resource.Success(status))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get git status", e)
                emit(Resource.Error(e.message ?: "Failed to get Git status", cached, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    fun getBranches(): Flow<Resource<List<GitBranch>>> = flow {
        emit(Resource.Loading())
        Napier.d("$TAG: Fetching branches...")
        gitApiClient.getBranches().fold(
            onSuccess = { branches ->
                Napier.d("$TAG: Found ${branches.size} branches")
                emit(Resource.Success(branches))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get branches", e)
                emit(Resource.Error(e.message ?: "Failed to get branches", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    fun getLog(branch: String? = null, limit: Int = 50, skip: Int = 0): Flow<Resource<GitLog>> = flow {
        emit(Resource.Loading())
        val b = branch ?: "HEAD"
        Napier.d("$TAG: Fetching log for $b...")
        gitApiClient.getLog(limit, skip, branch).fold(
            onSuccess = { log ->
                Napier.d("$TAG: Found ${log.commits.size} commits")
                emit(Resource.Success(log))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get log", e)
                emit(Resource.Error(e.message ?: "Failed to get commit log", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    fun getDiff(ref: String? = null, cached: Boolean = false): Flow<Resource<GitDiff>> = flow {
        emit(Resource.Loading())
        // For full repo diff
        Napier.d("$TAG: Fetching diff (cached=$cached, ref=$ref)...")
        gitApiClient.getDiff(null, cached).fold(
            onSuccess = { diff ->
                Napier.d("$TAG: Fetched diff with ${diff.files.size} files")
                emit(Resource.Success(diff))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get diff", e)
                emit(Resource.Error(e.message ?: "Failed to get diff", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)
    
    // Support for single file diff (called by new GitDiffScreenModel)
    fun getFileDiff(path: String, cached: Boolean = false): Flow<Resource<GitDiff>> = flow {
        emit(Resource.Loading())
        Napier.d("$TAG: Fetching diff for file $path...")
        gitApiClient.getDiff(path, cached).fold(
            onSuccess = { diff ->
                emit(Resource.Success(diff))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get diff for $path", e)
                emit(Resource.Error(e.message ?: "Failed to get file diff", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    suspend fun stage(files: List<String>): Result<GitOperationResult> {
        return gitApiClient.stage(files)
    }

    suspend fun unstage(files: List<String>): Result<GitOperationResult> {
        return gitApiClient.unstage(files)
    }

    suspend fun discard(files: List<String>): Result<GitOperationResult> {
        return gitApiClient.discard(files)
    }

    suspend fun commit(message: String, files: List<String>? = null, amend: Boolean = false): Result<GitOperationResult> {
        return gitApiClient.commit(message, files, amend)
    }

    suspend fun push(remote: String = "origin", branch: String? = null, force: Boolean = false, setUpstream: Boolean = false): Result<GitOperationResult> {
        return gitApiClient.push(remote, branch, force, setUpstream)
    }

    suspend fun pull(remote: String = "origin", branch: String? = null, rebase: Boolean = false): Result<GitOperationResult> {
        return gitApiClient.pull(remote, branch, rebase)
    }

    suspend fun fetch(remote: String = "origin", prune: Boolean = false, all: Boolean = false): Result<GitOperationResult> {
        return gitApiClient.fetch(remote, prune, all)
    }

    suspend fun checkout(ref: String, create: Boolean = false, force: Boolean = false): Result<GitOperationResult> {
        return gitApiClient.checkout(ref, create, force)
    }

    fun getRemotes(): Flow<Resource<List<GitRemote>>> = flow {
        emit(Resource.Loading())
        Napier.d("$TAG: Fetching remotes...")
        gitApiClient.getRemotes().fold(
            onSuccess = { remotes ->
                Napier.d("$TAG: Found ${remotes.size} remotes")
                emit(Resource.Success(remotes))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get remotes", e)
                emit(Resource.Error(e.message ?: "Failed to get remotes", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    fun getStashes(): Flow<Resource<List<GitStash>>> = flow {
        emit(Resource.Loading())
        Napier.d("$TAG: Fetching stashes...")
        gitApiClient.getStashes().fold(
            onSuccess = { stashes ->
                Napier.d("$TAG: Found ${stashes.size} stashes")
                emit(Resource.Success(stashes))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get stashes", e)
                emit(Resource.Error(e.message ?: "Failed to get stashes", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    suspend fun refreshStatus(): Result<GitStatusResponse> {
        return gitApiClient.getStatus().map { status ->
            localCache.saveGitStatus(status)
            status
        }
    }

    suspend fun requestStartGitServer(): Result<Unit> {
        return gitApiClient.requestStartGitServer()
    }

    /**
     * Request git server start and wait for it to become available.
     * Uses polling to verify the server is actually running.
     */
    suspend fun requestStartGitServerAndWait(
        maxWaitMs: Long = 10_000L,
        pollIntervalMs: Long = 500L
    ): Result<Boolean> {
        return gitApiClient.requestStartGitServerAndWait(maxWaitMs, pollIntervalMs)
    }

    /**
     * Check if the git server is currently running.
     */
    suspend fun isServerRunning(): Boolean {
        return gitApiClient.isServerRunning()
    }

    // VcsInfo was used in Dashboard, maybe keep it here or move to another repo?
    // It's still useful.
    fun getVcsInfo(): Flow<Resource<VcsInfo>> = flow {
        emit(Resource.Loading())
        gitApiClient.getStatus().fold(
            onSuccess = { status ->
                emit(Resource.Success(VcsInfo(
                    type = "git",
                    branch = status.branch,
                    dirty = status.hasChanges,
                    ahead = status.ahead,
                    behind = status.behind,
                    changeCount = status.totalChanges
                )))
            },
            onFailure = { e ->
                emit(Resource.Error(e.message ?: "Failed to get VCS info", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)
}
