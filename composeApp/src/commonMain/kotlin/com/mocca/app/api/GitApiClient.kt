package com.mocca.app.api

import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.plugins.timeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Git API Client using the integrated Git HTTP server.
 * 
 * The Git server runs on port 4097 (separate from OpenCode's 4096) and is
 * automatically started by the git-plugin.js when `opencode serve` launches.
 * This provides fast, reliable REST endpoints for all Git operations.
 */
class GitApiClient(
    private val httpClientProvider: HttpClientProvider,
    private val serverConfigProvider: () -> ServerConfig,
    private val retryPolicy: RetryPolicy = RetryPolicy.Default
) {
    companion object {
        private const val TAG = "GitApiClient"
        // Git HTTP server runs on port 4097 (OpenCode plugin auto-starts it)
        private const val GIT_SERVER_PORT = 4097
    }

    private fun getClient(): HttpClient {
        return httpClientProvider.getClientSync()
    }

    /**
     * Git server base URL - uses same host as OpenCode but on port 4097.
     * The git-plugin.js embedded in OpenCode automatically starts an HTTP server
     * on port 4097 when `opencode serve` runs.
     *
     * IMPORTANT: Git server ALWAYS uses HTTP (not HTTPS) because it's a
     * local development server that doesn't configure SSL/TLS.
     */
    private val gitServerUrl: String
        get() {
            val config = serverConfigProvider()
            // Extract host from baseUrl and use Git server port
            val baseUrl = config.baseUrl.trimEnd('/')
            return try {
                // Extract hostname without scheme or port
                val regex = Regex("""https?://([^:/]+)""")
                val match = regex.find(baseUrl)
                if (match != null) {
                    // Force HTTP for git server (never HTTPS)
                    "http://${match.groupValues[1]}:$GIT_SERVER_PORT"
                } else {
                    // Fallback: assume localhost
                    "http://127.0.0.1:$GIT_SERVER_PORT"
                }
            } catch (e: Exception) {
                "http://127.0.0.1:$GIT_SERVER_PORT"
            }
        }

    // --- Response models ---
    
    @Serializable
    private data class GitServerStatus(
        val branch: String = "",
        val upstream: String? = null,
        val ahead: Int = 0,
        val behind: Int = 0,
        val staged: List<GitServerFileChange> = emptyList(),
        val unstaged: List<GitServerFileChange> = emptyList(),
        val untracked: List<String> = emptyList(),
        val conflicted: List<String> = emptyList(),
        val deleted: List<String> = emptyList(),
        val renamed: List<String> = emptyList(),
        val clean: Boolean = true
    )
    
    @Serializable
    private data class GitServerFileChange(
        val path: String,
        val status: String = "modified"
    )
    
    @Serializable
    private data class GitServerLog(
        val commits: List<GitServerCommit> = emptyList(),
        val total: Int = 0,
        val hasMore: Boolean = false
    )
    
    @Serializable
    private data class GitServerCommit(
        val hash: String,
        val shortHash: String,
        val message: String,
        val author: String,
        val email: String? = null,
        val date: Long
    )
    
    @Serializable
    private data class GitServerBranches(
        val current: String,
        val branches: List<GitServerBranch> = emptyList()
    )
    
    @Serializable
    private data class GitServerBranch(
        val name: String,
        val current: Boolean = false,
        val remote: Boolean = false,
        val commit: String? = null,
        val label: String? = null
    )
    
    @Serializable
    private data class GitServerRemote(
        val name: String,
        val fetchUrl: String? = null,
        val pushUrl: String? = null
    )
    
    @Serializable
    private data class GitServerDiff(
        val raw: String = "",
        val files: List<GitServerDiffFile> = emptyList(),
        val additions: Int = 0,
        val deletions: Int = 0
    )
    
    @Serializable
    private data class GitServerDiffFile(
        val file: String,
        val additions: Int = 0,
        val deletions: Int = 0,
        val binary: Boolean = false
    )
    
    @Serializable
    private data class GitServerResult(
        val success: Boolean,
        val message: String? = null,
        val error: String? = null,
        val commit: String? = null
    )
    
    @Serializable
    private data class GitServerStash(
        val index: Int,
        val hash: String,
        val message: String,
        val date: String? = null
    )

    // --- Git Operations ---

    @Serializable
    private data class GitCommitRequest(
        val message: String,
        val amend: Boolean = false,
        val stageAll: Boolean = false
    )

    @Serializable
    private data class GitPushRequest(
        val remote: String = "origin",
        val branch: String? = null,
        val force: Boolean = false,
        val setUpstream: Boolean = false
    )

    @Serializable
    private data class GitPullRequest(
        val remote: String = "origin",
        val branch: String? = null,
        val rebase: Boolean = false
    )

    @Serializable
    private data class GitFetchRequest(
        val remote: String = "origin",
        val prune: Boolean = false,
        val all: Boolean = false
    )

    @Serializable
    private data class GitCheckoutRequest(
        val ref: String,
        val create: Boolean = false,
        val force: Boolean = false
    )

    @Serializable
    private data class GitFilesRequest(
        val files: List<String>
    )

    // --- Git Operations ---

    suspend fun getStatus(): Result<GitStatusResponse> = safeCall("getStatus") {
        val response: GitServerStatus = getClient().get("$gitServerUrl/status").body()
        
        GitStatusResponse(
            branch = response.branch,
            upstream = response.upstream,
            ahead = response.ahead,
            behind = response.behind,
            staged = response.staged.map { 
                GitFileChange(it.path, mapStatus(it.status), null) 
            },
            unstaged = response.unstaged.map { 
                GitFileChange(it.path, mapStatus(it.status), null) 
            },
            untracked = response.untracked,
            conflicted = response.conflicted,
            stashes = 0,
            clean = response.clean
        )
    }
    
    private fun mapStatus(status: String): GitFileStatus {
        return when (status.lowercase()) {
            "added", "staged" -> GitFileStatus.ADDED
            "modified" -> GitFileStatus.MODIFIED
            "deleted" -> GitFileStatus.DELETED
            "renamed" -> GitFileStatus.RENAMED
            "copied" -> GitFileStatus.COPIED
            "unmerged" -> GitFileStatus.UNMERGED
            else -> GitFileStatus.UNKNOWN
        }
    }

    suspend fun getLog(
        limit: Int = 50,
        skip: Int = 0,
        branch: String? = null
    ): Result<GitLog> = safeCall("getLog") {
        val encodedBranch = branch?.encodeURLParameter()
        val url = "$gitServerUrl/log?limit=$limit&skip=$skip" +
            (encodedBranch?.let { "&branch=$it" } ?: "")
        
        val response: GitServerLog = getClient().get(url).body()
        
        GitLog(
            commits = response.commits.map {
                GitCommit(
                    hash = it.hash,
                    shortHash = it.shortHash,
                    message = it.message,
                    author = it.author,
                    email = it.email ?: "",
                    date = it.date,
                    parents = emptyList(),
                    refs = emptyList()
                )
            },
            total = response.total,
            hasMore = response.hasMore
        )
    }

    suspend fun getBranches(): Result<List<GitBranch>> = safeCall("getBranches") {
        val response: GitServerBranches = getClient().get("$gitServerUrl/branches").body()
        
        response.branches.map {
            GitBranch(
                name = it.name,
                current = it.current,
                remote = it.remote
            )
        }
    }

    suspend fun getDiff(
        path: String? = null,
        cached: Boolean = false
    ): Result<GitDiff> = safeCall("getDiff") {
        val encodedPath = path?.encodeURLParameter()
        val url = "$gitServerUrl/diff?cached=$cached" +
            (encodedPath?.let { "&path=$it" } ?: "")
        
        val response: GitServerDiff = getClient().get(url) {
            timeout {
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
                connectTimeoutMillis = 15_000
            }
        }.body()
        
        GitDiff(
            files = response.files.map {
                GitDiffFile(
                    path = it.file,
                    additions = it.additions,
                    deletions = it.deletions,
                    binary = it.binary,
                    status = GitFileStatus.MODIFIED, // Default to modified for now as we don't get exact status from simple-git diffSummary
                    hunks = emptyList()
                )
            },
            additions = response.additions,
            deletions = response.deletions,
            binary = false
        )
    }

    suspend fun commit(
        message: String,
        files: List<String>? = null,
        amend: Boolean = false
    ): Result<GitOperationResult> = safeCall("commit") {
        val response = getClient().post("$gitServerUrl/commit") {
            contentType(ContentType.Application.Json)
            setBody(GitCommitRequest(
                message = message,
                amend = amend,
                stageAll = files.isNullOrEmpty()
            ))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Commit successful")
        } else {
            throw Exception(result.error ?: "Commit failed")
        }
    }

    suspend fun push(
        remote: String = "origin",
        branch: String? = null,
        force: Boolean = false,
        setUpstream: Boolean = false
    ): Result<GitOperationResult> = safeCall("push") {
        val response = getClient().post("$gitServerUrl/push") {
            contentType(ContentType.Application.Json)
            setBody(GitPushRequest(
                remote = remote,
                branch = branch,
                force = force,
                setUpstream = setUpstream
            ))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Push successful")
        } else {
            throw Exception(result.error ?: "Push failed")
        }
    }

    suspend fun pull(
        remote: String = "origin",
        branch: String? = null,
        rebase: Boolean = false
    ): Result<GitOperationResult> = safeCall("pull") {
        val response = getClient().post("$gitServerUrl/pull") {
            contentType(ContentType.Application.Json)
            setBody(GitPullRequest(
                remote = remote,
                branch = branch,
                rebase = rebase
            ))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Pull successful")
        } else {
            throw Exception(result.error ?: "Pull failed")
        }
    }

    suspend fun fetch(
        remote: String = "origin",
        prune: Boolean = false,
        all: Boolean = false
    ): Result<GitOperationResult> = safeCall("fetch") {
        val response = getClient().post("$gitServerUrl/fetch") {
            contentType(ContentType.Application.Json)
            setBody(GitFetchRequest(
                remote = remote,
                prune = prune,
                all = all
            ))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Fetch successful")
        } else {
            throw Exception(result.error ?: "Fetch failed")
        }
    }

    suspend fun checkout(
        ref: String,
        create: Boolean = false,
        force: Boolean = false
    ): Result<GitOperationResult> = safeCall("checkout") {
        val response = getClient().post("$gitServerUrl/checkout") {
            contentType(ContentType.Application.Json)
            setBody(GitCheckoutRequest(
                ref = ref,
                create = create,
                force = force
            ))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Checkout successful")
        } else {
            throw Exception(result.error ?: "Checkout failed")
        }
    }

    suspend fun stage(files: List<String>): Result<GitOperationResult> = safeCall("stage") {
        val response = getClient().post("$gitServerUrl/stage") {
            contentType(ContentType.Application.Json)
            setBody(GitFilesRequest(files))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Files staged")
        } else {
            throw Exception(result.error ?: "Stage failed")
        }
    }

    suspend fun unstage(files: List<String>): Result<GitOperationResult> = safeCall("unstage") {
        val response = getClient().post("$gitServerUrl/unstage") {
            contentType(ContentType.Application.Json)
            setBody(GitFilesRequest(files))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Files unstaged")
        } else {
            throw Exception(result.error ?: "Unstage failed")
        }
    }

    suspend fun discard(files: List<String>): Result<GitOperationResult> = safeCall("discard") {
        val response = getClient().post("$gitServerUrl/discard") {
            contentType(ContentType.Application.Json)
            setBody(GitFilesRequest(files))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Changes discarded")
        } else {
            throw Exception(result.error ?: "Discard failed")
        }
    }

    suspend fun getRemotes(): Result<List<GitRemote>> = safeCall("getRemotes") {
        val response: List<GitServerRemote> = getClient().get("$gitServerUrl/remotes").body()
        
        response.map {
            GitRemote(
                name = it.name,
                url = it.fetchUrl ?: "",
                pushUrl = it.pushUrl
            )
        }
    }

    suspend fun getStashes(): Result<List<GitStash>> = safeCall("getStashes") {
        val response: List<GitServerStash> = getClient().get("$gitServerUrl/stash").body()
        
        response.map {
            GitStash(
                index = it.index,
                message = it.message,
                branch = null,
                date = null
            )
        }
    }

    // --- Helpers ---

    private suspend inline fun <reified T> safeCall(
        operation: String,
        crossinline block: suspend () -> T
    ): Result<T> {
        return try {
            Napier.d("$TAG: $operation")
            Result.success(block())
        } catch (e: Exception) {
            Napier.e("$TAG: $operation failed: ${e.message}", e)
            Result.failure(NetworkError.from(e))
        }
    }
}
