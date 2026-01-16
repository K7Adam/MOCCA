package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import com.mocca.app.data.repository.GitParsers.parseDiff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for Git operations.
 * Provides offline-first access to Git status and operations.
 */
class GitRepository(
    private val apiClient: MoccaApiClient,
    private val localCache: LocalCache,
    private val gitService: GitService
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
            .onFailure { 
                // Fallback to GitService
                gitService.execute("git status --porcelain=v2 -b").fold(
                    onSuccess = { output ->
                        try {
                            val status = GitParsers.parseStatus(output)
                            localCache.saveGitStatus(status)
                            emit(Resource.Success(status))
                        } catch (e: Exception) {
                            emit(Resource.Error("Failed to parse git status: ${e.message}", cached))
                        }
                    },
                    onFailure = { e ->
                        emit(Resource.Error(e.message ?: "Failed to get Git status", cached))
                    }
                )
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
            .onFailure {
                gitService.execute("git branch -vv -a").fold(
                    onSuccess = { output ->
                        try {
                            val branches = GitParsers.parseBranches(output)
                            emit(Resource.Success(branches))
                        } catch (e: Exception) {
                            emit(Resource.Error("Failed to parse branches: ${e.message}"))
                        }
                    },
                    onFailure = { e ->
                        emit(Resource.Error(e.message ?: "Failed to get branches"))
                    }
                )
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
            .onFailure {
                val b = branch ?: "HEAD"
                gitService.execute("git log $b --pretty=format:\"%H|%h|%s|%an|%ae|%at|%P\" -n $limit --skip $skip").fold(
                    onSuccess = { output ->
                        try {
                            val log = GitParsers.parseLog(output)
                            emit(Resource.Success(log))
                        } catch (e: Exception) {
                            emit(Resource.Error("Failed to parse log: ${e.message}"))
                        }
                    },
                    onFailure = { e ->
                        emit(Resource.Error(e.message ?: "Failed to get commit log"))
                    }
                )
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
            .onFailure {
                val cmd = StringBuilder("git diff")
                if (cached) cmd.append(" --cached")
                if (ref != null) cmd.append(" $ref")
                
                gitService.execute(cmd.toString()).fold(
                    onSuccess = { output ->
                        try {
                            val diff = GitParsers.parseDiff(output)
                            emit(Resource.Success(diff))
                        } catch (e: Exception) {
                            emit(Resource.Error("Failed to parse diff: ${e.message}"))
                        }
                    },
                    onFailure = { e ->
                        emit(Resource.Error(e.message ?: "Failed to get diff"))
                    }
                )
            }
    }
    
    /**
     * Stage files for commit.
     */
    suspend fun stage(files: List<String>): Result<GitOperationResult> {
        return apiClient.gitStage(files).recoverCatching {
            gitService.execute("git add ${files.joinToString(" ")}").map { 
                GitOperationResult(true, "Staged ${files.size} files")
            }.getOrThrow()
        }.also { result ->
            result.onSuccess {
                // Refresh status after staging
                getStatus().collect {} // Force refresh cache
            }
        }
    }
    
    /**
     * Unstage files.
     */
    suspend fun unstage(files: List<String>): Result<GitOperationResult> {
        return apiClient.gitUnstage(files).recoverCatching {
            gitService.execute("git restore --staged ${files.joinToString(" ")}").map {
                GitOperationResult(true, "Unstaged ${files.size} files")
            }.getOrThrow()
        }.also { result ->
            result.onSuccess {
                getStatus().collect {}
            }
        }
    }
    
    /**
     * Discard changes to files.
     */
    suspend fun discard(files: List<String>): Result<GitOperationResult> {
        return apiClient.gitDiscard(files).recoverCatching {
            gitService.execute("git restore ${files.joinToString(" ")}").map {
                GitOperationResult(true, "Discarded changes")
            }.getOrThrow()
        }.also { result ->
            result.onSuccess {
                getStatus().collect {}
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
        return apiClient.gitCommit(message, files, amend).recoverCatching {
            val cmd = StringBuilder("git commit -m \"$message\"")
            if (amend) cmd.append(" --amend")
            gitService.execute(cmd.toString()).map {
                GitOperationResult(true, "Commit successful")
            }.getOrThrow()
        }.also { result ->
            result.onSuccess {
                getStatus().collect {}
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
        return apiClient.gitPush(remote, branch, force, setUpstream).recoverCatching {
            val cmd = StringBuilder("git push $remote")
            if (branch != null) cmd.append(" $branch")
            if (force) cmd.append(" --force")
            if (setUpstream) cmd.append(" -u")
            gitService.execute(cmd.toString()).map {
                GitOperationResult(true, "Push successful")
            }.getOrThrow()
        }
    }
    
    /**
     * Pull changes from remote.
     */
    suspend fun pull(
        remote: String = "origin",
        branch: String? = null,
        rebase: Boolean = false
    ): Result<GitOperationResult> {
        return apiClient.gitPull(remote, branch, rebase).recoverCatching {
            val cmd = StringBuilder("git pull $remote")
            if (branch != null) cmd.append(" $branch")
            if (rebase) cmd.append(" --rebase")
            gitService.execute(cmd.toString()).map {
                GitOperationResult(true, "Pull successful")
            }.getOrThrow()
        }.also { result ->
            result.onSuccess {
                getStatus().collect {}
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
        return apiClient.gitFetch(remote, prune, all).recoverCatching {
            val cmd = StringBuilder("git fetch $remote")
            if (prune) cmd.append(" --prune")
            if (all) cmd.append(" --all")
            gitService.execute(cmd.toString()).map {
                GitOperationResult(true, "Fetch successful")
            }.getOrThrow()
        }
    }
    
    /**
     * Checkout a branch, tag, or commit.
     */
    suspend fun checkout(
        ref: String,
        create: Boolean = false,
        force: Boolean = false
    ): Result<GitOperationResult> {
        return apiClient.gitCheckout(ref, create, force).recoverCatching {
            val cmd = StringBuilder("git checkout")
            if (create) cmd.append(" -b")
            if (force) cmd.append(" -f")
            cmd.append(" $ref")
            gitService.execute(cmd.toString()).map {
                GitOperationResult(true, "Checkout successful")
            }.getOrThrow()
        }.also { result ->
            result.onSuccess {
                getStatus().collect {}
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
            .onFailure {
                gitService.execute("git remote -v").fold(
                    onSuccess = { output ->
                        try {
                            val remotes = GitParsers.parseRemotes(output)
                            emit(Resource.Success(remotes))
                        } catch (e: Exception) {
                            emit(Resource.Error("Failed to parse remotes: ${e.message}"))
                        }
                    },
                    onFailure = { e ->
                        emit(Resource.Error(e.message ?: "Failed to get remotes"))
                    }
                )
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
