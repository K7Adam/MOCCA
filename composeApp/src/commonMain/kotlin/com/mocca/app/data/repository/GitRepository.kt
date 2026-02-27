package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Repository for Git operations using OpenCode's built-in VCS endpoints.
 * Replaces the old GitApiClient (port 4097) architecture.
 *
 * Read operations use /vcs and /session/:id/diff endpoints.
 * Write operations use /session/:id/shell to execute git commands on the server.
 */
class GitRepository(
    private val apiClient: MoccaApiClient,
    private val localCache: LocalCache
) {
    companion object {
        private const val TAG = "GitRepository"
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Read Operations (via OpenCode built-in endpoints)
    // ═══════════════════════════════════════════════════════════════════════════════

    fun getVcsInfo(): Flow<Resource<VcsInfo>> = flow {
        emit(Resource.Loading())
        Napier.d("$TAG: Fetching VCS info via /vcs...")
        apiClient.getVcsInfo().fold(
            onSuccess = { vcsInfo ->
                Napier.d("$TAG: VCS info fetched: branch=${vcsInfo.branch}")
                emit(Resource.Success(vcsInfo))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get VCS info", e)
                emit(Resource.Error(e.message ?: "Failed to get VCS info", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    /**
     * Get git status using /vcs for branch name and shell commands for rich status data.
     * The /vcs endpoint only returns { branch: string }, so we use:
     * - `git status --porcelain=v1` for staged/unstaged/untracked files
     * - `git rev-list --left-right --count HEAD...@{u}` for ahead/behind counts
     */
    fun getStatus(sessionId: String? = null): Flow<Resource<GitStatusResponse>> = flow {
        emit(Resource.Loading())
        val cached = localCache.getGitStatus()
        if (cached != null) {
            emit(Resource.Loading(cached))
        }

        Napier.d("$TAG: Fetching git status...")
        
        // Get branch name from /vcs endpoint (no session needed)
        val branch = apiClient.getVcsInfo().getOrNull()?.branch?.ifBlank { null } ?: "unknown"
        
        if (sessionId == null) {
            // Minimal status when no session available — branch only
            val status = GitStatusResponse(branch = branch)
            localCache.saveGitStatus(status)
            emit(Resource.Success(status))
            return@flow
        }
        
        // Get full status via shell
        apiClient.executeShell(sessionId, "git status --porcelain=v1").fold(
            onSuccess = { output ->
                val staged = mutableListOf<GitFileChange>()
                val unstaged = mutableListOf<GitFileChange>()
                val untracked = mutableListOf<String>()
                
                output.lines().filter { it.length >= 3 }.forEach { line ->
                    val index = line[0]   // staged status character
                    val worktree = line[1] // unstaged status character
                    val filePath = line.substring(3).trim()
                    
                    if (index == '?' && worktree == '?') {
                        untracked.add(filePath)
                    } else {
                        if (index != ' ' && index != '?') {
                            staged.add(GitFileChange(path = filePath, status = parseGitStatusChar(index)))
                        }
                        if (worktree != ' ' && worktree != '?') {
                            unstaged.add(GitFileChange(path = filePath, status = parseGitStatusChar(worktree)))
                        }
                    }
                }
                
                // Get ahead/behind counts
                val (ahead, behind) = getAheadBehind(sessionId)
                
                val status = GitStatusResponse(
                    branch = branch,
                    staged = staged,
                    unstaged = unstaged,
                    untracked = untracked,
                    clean = staged.isEmpty() && unstaged.isEmpty() && untracked.isEmpty(),
                    ahead = ahead,
                    behind = behind
                )
                localCache.saveGitStatus(status)
                Napier.d("$TAG: Git status: branch=$branch, staged=${staged.size}, unstaged=${unstaged.size}, untracked=${untracked.size}, ahead=$ahead, behind=$behind")
                emit(Resource.Success(status))
            },
            onFailure = { e ->
                Napier.w("$TAG: Shell git status failed, falling back to branch-only: ${e.message}")
                val status = GitStatusResponse(branch = "")
                localCache.saveGitStatus(status)
                emit(Resource.Success(status))
            }
        )
    }.flowOn(Dispatchers.Default)

    /**
     * Get session diffs via OpenCode built-in endpoint.
     */
    fun getSessionDiffs(sessionId: String): Flow<Resource<List<FileDiff>>> = flow {
        emit(Resource.Loading())
        Napier.d("$TAG: Fetching session diffs for $sessionId...")
        apiClient.getSessionDiffs(sessionId).fold(
            onSuccess = { diffs ->
                Napier.d("$TAG: Fetched ${diffs.size} file diffs")
                emit(Resource.Success(diffs))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get session diffs", e)
                emit(Resource.Error(e.message ?: "Failed to get session diffs", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    // ═══════════════════════════════════════════════════════════════════════════════
    // Write Operations (via shell execution)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Execute a git command on the server via shell.
     * Requires an active session for command execution context.
     */
    private suspend fun executeGitCommand(sessionId: String, command: String): Result<GitOperationResult> {
        return apiClient.executeShell(sessionId, command).fold(
            onSuccess = { output ->
                Result.success(GitOperationResult(success = true, message = output))
            },
            onFailure = { e ->
                Result.failure(e)
            }
        )
    }

    suspend fun stage(sessionId: String, files: List<String>): Result<GitOperationResult> {
        val paths = files.joinToString(" ") { "\"$it\"" }
        return executeGitCommand(sessionId, "git add $paths")
    }

    suspend fun unstage(sessionId: String, files: List<String>): Result<GitOperationResult> {
        val paths = files.joinToString(" ") { "\"$it\"" }
        return executeGitCommand(sessionId, "git reset HEAD $paths")
    }

    suspend fun discard(sessionId: String, files: List<String>): Result<GitOperationResult> {
        val paths = files.joinToString(" ") { "\"$it\"" }
        return executeGitCommand(sessionId, "git checkout -- $paths")
    }

    suspend fun commit(sessionId: String, message: String, amend: Boolean = false): Result<GitOperationResult> {
        val amendFlag = if (amend) " --amend" else ""
        return executeGitCommand(sessionId, "git commit -m \"$message\"$amendFlag")
    }

    suspend fun push(sessionId: String, remote: String = "origin", branch: String? = null, force: Boolean = false, setUpstream: Boolean = false): Result<GitOperationResult> {
        val forceFlag = if (force) " --force" else ""
        val upstreamFlag = if (setUpstream) " --set-upstream" else ""
        val branchArg = branch?.let { " $it" } ?: ""
        return executeGitCommand(sessionId, "git push$forceFlag$upstreamFlag $remote$branchArg")
    }

    suspend fun pull(sessionId: String, remote: String = "origin", branch: String? = null, rebase: Boolean = false): Result<GitOperationResult> {
        val rebaseFlag = if (rebase) " --rebase" else ""
        val branchArg = branch?.let { " $it" } ?: ""
        return executeGitCommand(sessionId, "git pull$rebaseFlag $remote$branchArg")
    }

    suspend fun fetch(sessionId: String, remote: String = "origin", prune: Boolean = false, all: Boolean = false): Result<GitOperationResult> {
        val pruneFlag = if (prune) " --prune" else ""
        val allFlag = if (all) " --all" else ""
        return executeGitCommand(sessionId, "git fetch$pruneFlag$allFlag $remote")
    }

    suspend fun checkout(sessionId: String, ref: String, create: Boolean = false, force: Boolean = false): Result<GitOperationResult> {
        val createFlag = if (create) " -b" else ""
        val forceFlag = if (force) " --force" else ""
        return executeGitCommand(sessionId, "git checkout$createFlag$forceFlag $ref")
    }

    suspend fun refreshStatus(): Result<VcsInfo> {
        return apiClient.getVcsInfo().map { vcsInfo ->
            val status = GitStatusResponse(
                branch = vcsInfo.branch.ifBlank { "unknown" }
            )
            localCache.saveGitStatus(status)
            vcsInfo
        }
    }
    
    /**
     * Refresh git status from server.
     * Called by RealtimeSyncService during periodic sync.
     * Uses /vcs for branch (lightweight, no session needed).
     */
    suspend fun refresh() {
        apiClient.getVcsInfo().fold(
            onSuccess = { vcsInfo ->
                val status = GitStatusResponse(
                    branch = vcsInfo.branch.ifBlank { "unknown" }
                )
                localCache.saveGitStatus(status)
                Napier.d("[GitRepository] Refreshed VCS info: branch=${vcsInfo.branch}")
            },
            onFailure = { error ->
                Napier.w("[GitRepository] Failed to refresh VCS info: ${error.message}")
                throw error
            }
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get ahead/behind counts relative to upstream.
     */
    private suspend fun getAheadBehind(sessionId: String): Pair<Int, Int> {
        return apiClient.executeShell(sessionId, "git rev-list --left-right --count HEAD...@{u} 2>/dev/null || echo '0\t0'")
            .getOrNull()?.let { output ->
                val parts = output.trim().split("\\s+".toRegex())
                Pair(
                    parts.getOrNull(0)?.toIntOrNull() ?: 0,
                    parts.getOrNull(1)?.toIntOrNull() ?: 0
                )
            } ?: Pair(0, 0)
    }
    
    /**
     * Parse a git status porcelain character to GitFileStatus.
     */
    private fun parseGitStatusChar(c: Char): GitFileStatus {
        return when (c) {
            'M' -> GitFileStatus.MODIFIED
            'A' -> GitFileStatus.ADDED
            'D' -> GitFileStatus.DELETED
            'R' -> GitFileStatus.RENAMED
            'C' -> GitFileStatus.COPIED
            'U' -> GitFileStatus.UNMERGED
            else -> GitFileStatus.MODIFIED
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Read Operations via Shell (branches, log, remotes, tags, stashes)
    // ═══════════════════════════════════════════════════════════════════════════════

    fun getBranches(sessionId: String? = null): Flow<Resource<List<GitBranch>>> = flow {
        emit(Resource.Loading())
        if (sessionId == null) {
            emit(Resource.Error("No active session for Git operations"))
            return@flow
        }
        apiClient.executeShell(sessionId, "git branch -a --format='%(refname:short)|%(HEAD)|%(objectname:short)|%(upstream:short)'").fold(
            onSuccess = { output ->
                val branches = output.lines().filter { it.isNotBlank() }.mapNotNull { line ->
                    try {
                        val parts = line.trim().removePrefix("'").removeSuffix("'").split("|")
                        val name = parts.getOrElse(0) { return@mapNotNull null }.trim()
                        if (name.isBlank()) return@mapNotNull null
                        val isCurrent = parts.getOrElse(1) { "" }.trim() == "*"
                        val lastCommit = parts.getOrElse(2) { "" }.trim().takeIf { it.isNotBlank() }
                        val upstream = parts.getOrElse(3) { "" }.trim().takeIf { it.isNotBlank() }
                        val isRemote = name.startsWith("remotes/") || name.startsWith("origin/")
                        GitBranch(
                            name = name.removePrefix("remotes/"),
                            current = isCurrent,
                            remote = isRemote,
                            upstream = upstream,
                            lastCommit = lastCommit
                        )
                    } catch (e: Exception) { null }
                }
                Napier.d("$TAG: Parsed ${branches.size} branches")
                emit(Resource.Success(branches))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get branches", e)
                emit(Resource.Error(e.message ?: "Failed to get branches", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    fun getLog(sessionId: String? = null, branch: String? = null, count: Int = 50, skip: Int = 0): Flow<Resource<GitLog>> = flow {
        emit(Resource.Loading())
        if (sessionId == null) {
            emit(Resource.Error("No active session for Git operations"))
            return@flow
        }
        val branchArg = branch?.let { " $it" } ?: ""
        apiClient.executeShell(sessionId, "git log --format='%H|%h|%s|%an|%ae|%at' -n $count --skip $skip$branchArg").fold(
            onSuccess = { output ->
                val commits = output.lines().filter { it.isNotBlank() }.mapNotNull { line ->
                    try {
                        val parts = line.trim().removePrefix("'").removeSuffix("'").split("|", limit = 6)
                        GitCommit(
                            hash = parts.getOrElse(0) { return@mapNotNull null },
                            shortHash = parts.getOrElse(1) { "" },
                            message = parts.getOrElse(2) { "" },
                            author = parts.getOrElse(3) { "" },
                            email = parts.getOrElse(4) { "" }.takeIf { it.isNotBlank() },
                            date = parts.getOrElse(5) { "0" }.toLongOrNull()?.times(1000) ?: 0L
                        )
                    } catch (e: Exception) { null }
                }
                val log = GitLog(commits = commits, total = commits.size, hasMore = commits.size >= count)
                Napier.d("$TAG: Parsed ${commits.size} commits")
                emit(Resource.Success(log))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get log", e)
                emit(Resource.Error(e.message ?: "Failed to get log", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    fun getRemotes(sessionId: String? = null): Flow<Resource<List<GitRemote>>> = flow {
        emit(Resource.Loading())
        if (sessionId == null) {
            emit(Resource.Error("No active session for Git operations"))
            return@flow
        }
        apiClient.executeShell(sessionId, "git remote -v").fold(
            onSuccess = { output ->
                val remotesMap = mutableMapOf<String, Pair<String?, String?>>()
                output.lines().filter { it.isNotBlank() }.forEach { line ->
                    try {
                        val parts = line.trim().split("\\s+".toRegex())
                        val name = parts.getOrNull(0) ?: return@forEach
                        val url = parts.getOrNull(1) ?: return@forEach
                        val type = parts.getOrNull(2)?.removeSurrounding("(", ")") ?: ""
                        val current = remotesMap.getOrDefault(name, Pair(null, null))
                        remotesMap[name] = if (type == "fetch") Pair(url, current.second) else Pair(current.first, url)
                    } catch (e: Exception) { /* skip malformed line */ }
                }
                val remotes = remotesMap.map { (name, urls) ->
                    GitRemote(
                        name = name,
                        url = urls.first ?: urls.second ?: "",
                        fetchUrl = urls.first,
                        pushUrl = urls.second
                    )
                }
                Napier.d("$TAG: Parsed ${remotes.size} remotes")
                emit(Resource.Success(remotes))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get remotes", e)
                emit(Resource.Error(e.message ?: "Failed to get remotes", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    fun getTags(sessionId: String? = null): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading())
        if (sessionId == null) {
            emit(Resource.Error("No active session for Git operations"))
            return@flow
        }
        apiClient.executeShell(sessionId, "git tag --list").fold(
            onSuccess = { output ->
                val tags = output.lines().map { it.trim() }.filter { it.isNotBlank() }
                Napier.d("$TAG: Parsed ${tags.size} tags")
                emit(Resource.Success(tags))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get tags", e)
                emit(Resource.Error(e.message ?: "Failed to get tags", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    fun getStashes(sessionId: String? = null): Flow<Resource<List<GitStash>>> = flow {
        emit(Resource.Loading())
        if (sessionId == null) {
            emit(Resource.Error("No active session for Git operations"))
            return@flow
        }
        apiClient.executeShell(sessionId, "git stash list --format='%gd|%gs'").fold(
            onSuccess = { output ->
                val stashes = output.lines().filter { it.isNotBlank() }.mapNotNull { line ->
                    try {
                        val parts = line.trim().removePrefix("'").removeSuffix("'").split("|", limit = 2)
                        val ref = parts.getOrElse(0) { return@mapNotNull null }
                        val message = parts.getOrElse(1) { "" }
                        val index = ref.removePrefix("stash@{").removeSuffix("}").toIntOrNull() ?: return@mapNotNull null
                        GitStash(index = index, message = message)
                    } catch (e: Exception) { null }
                }
                Napier.d("$TAG: Parsed ${stashes.size} stashes")
                emit(Resource.Success(stashes))
            },
            onFailure = { e ->
                Napier.e("$TAG: Failed to get stashes", e)
                emit(Resource.Error(e.message ?: "Failed to get stashes", null, e))
            }
        )
    }.flowOn(Dispatchers.Default)

    // ═══════════════════════════════════════════════════════════════════════════════
    // Additional Write Operations (stash, merge, rebase, remote, tag)
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun createStash(sessionId: String, message: String?): Result<GitOperationResult> {
        val cmd = if (message != null) "git stash push -m \"$message\"" else "git stash push"
        return executeGitCommand(sessionId, cmd)
    }

    suspend fun popStash(sessionId: String, index: Int): Result<GitOperationResult> {
        return executeGitCommand(sessionId, "git stash pop stash@{$index}")
    }

    suspend fun applyStash(sessionId: String, index: Int): Result<GitOperationResult> {
        return executeGitCommand(sessionId, "git stash apply stash@{$index}")
    }

    suspend fun dropStash(sessionId: String, index: Int): Result<GitOperationResult> {
        return executeGitCommand(sessionId, "git stash drop stash@{$index}")
    }

    suspend fun merge(sessionId: String, branch: String): Result<GitOperationResult> {
        return executeGitCommand(sessionId, "git merge $branch")
    }

    suspend fun rebase(sessionId: String, branch: String): Result<GitOperationResult> {
        return executeGitCommand(sessionId, "git rebase $branch")
    }

    suspend fun addRemote(sessionId: String, name: String, url: String): Result<GitOperationResult> {
        return executeGitCommand(sessionId, "git remote add $name $url")
    }

    suspend fun removeRemote(sessionId: String, name: String): Result<GitOperationResult> {
        return executeGitCommand(sessionId, "git remote remove $name")
    }

    suspend fun createTag(sessionId: String, name: String, message: String?): Result<GitOperationResult> {
        val cmd = if (message != null) "git tag -a $name -m \"$message\"" else "git tag $name"
        return executeGitCommand(sessionId, cmd)
    }

    suspend fun deleteTag(sessionId: String, name: String): Result<GitOperationResult> {
        return executeGitCommand(sessionId, "git tag -d $name")
    }
}
