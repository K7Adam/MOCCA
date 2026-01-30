package com.mocca.app.api

import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.plugins.timeout
import kotlinx.serialization.Serializable
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import com.mocca.app.api.NetworkError
import kotlinx.coroutines.sync.withLock

// --- Response models (Internal DTOs) ---

@Serializable
internal data class GitServerStatus(
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
internal data class GitServerFileChange(
    val path: String,
    val status: String = "modified"
)

@Serializable
internal data class GitServerLog(
    val commits: List<GitServerCommit> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false
)

@Serializable
internal data class GitServerCommit(
    val hash: String,
    val shortHash: String,
    val message: String,
    val author: String,
    val email: String? = null,
    val date: Long
)

@Serializable
internal data class GitServerBranches(
    val current: String,
    val branches: List<GitServerBranch> = emptyList()
)

@Serializable
internal data class GitServerBranch(
    val name: String,
    val current: Boolean = false,
    val remote: Boolean = false,
    val commit: String? = null,
    val label: String? = null
)

@Serializable
internal data class GitServerRemote(
    val name: String,
    val fetchUrl: String? = null,
    val pushUrl: String? = null
)

@Serializable
internal data class GitServerDiff(
    val raw: String = "",
    val files: List<GitServerDiffFile> = emptyList(),
    val additions: Int = 0,
    val deletions: Int = 0
)

@Serializable
internal data class GitServerDiffFile(
    val file: String,
    val additions: Int = 0,
    val deletions: Int = 0,
    val binary: Boolean = false
)

@Serializable
internal data class GitServerResult(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val commit: String? = null
)

@Serializable
internal data class GitServerStash(
    val index: Int,
    val hash: String,
    val message: String,
    val date: String? = null
)

// --- Additional Git Operations Requests ---

@Serializable
internal data class GitStashRequest(
    val message: String? = null,
    val includeUntracked: Boolean = false
)

@Serializable
internal data class GitStashApplyRequest(
    val index: Int,
    val drop: Boolean = false
)

@Serializable
internal data class GitMergeRequest(
    val branch: String,
    val noFf: Boolean = false,
    val squash: Boolean = false,
    val message: String? = null
)

@Serializable
internal data class GitRebaseRequest(
    val onto: String,
    val interactive: Boolean = false
)

@Serializable
internal data class GitTagRequest(
    val name: String,
    val message: String? = null,
    val ref: String? = null,
    val annotated: Boolean = false
)

@Serializable
internal data class GitRemoteRequest(
    val name: String,
    val url: String
)

// --- Git Operations Requests ---

@Serializable
internal data class GitCommitRequest(
    val message: String,
    val amend: Boolean = false,
    val stageAll: Boolean = false
)

@Serializable
internal data class GitPushRequest(
    val remote: String = "origin",
    val branch: String? = null,
    val force: Boolean = false,
    val setUpstream: Boolean = false
)

@Serializable
internal data class GitPullRequest(
    val remote: String = "origin",
    val branch: String? = null,
    val rebase: Boolean = false
)

@Serializable
internal data class GitFetchRequest(
    val remote: String = "origin",
    val prune: Boolean = false,
    val all: Boolean = false
)

@Serializable
internal data class GitCheckoutRequest(
    val ref: String,
    val create: Boolean = false,
    val force: Boolean = false
)

@Serializable
internal data class GitFilesRequest(
    val files: List<String>
)

/**
 * Git API Client using the integrated Git HTTP server.
 */
class GitApiClient(
    private val httpClientProvider: HttpClientProvider,
    private val serverConfigProvider: () -> ServerConfig,
    private val retryPolicy: RetryPolicy = RetryPolicy.Default
) {
    /**
     * Custom exception for when Git server is not running.
     * Nested inside class to maintain backward compatibility.
     */
    class GitServerNotRunningException(
        message: String, 
        val isConnectionRefused: Boolean = false,
        val url: String? = null
    ) : Exception(message) {
        val isServerDown: Boolean = true
    }

    // Mutex to synchronize URL resolution (prevents race conditions)
    private val urlResolutionMutex = Mutex()

    // Cache the working URL to avoid repeated failed connection attempts
    private var cachedWorkingUrl: String? = null
    private var urlChecked = false

    // Track connection attempts for diagnostics
    @Volatile
    private var connectionAttempts = 0
    @Volatile
    private var configuredUrlAttempts = 0
    @Volatile
    private var localhostAttempts = 0
    @Volatile
    private var configuredUrlSuccess = false
    @Volatile
    private var localhostSuccess = false

    init {
        val config = serverConfigProvider()
        Napier.i("$TAG: Initialized - OpenCode URL: ${config.baseUrl}, Target Git Port: $GIT_SERVER_PORT")
    }

    /**
     * Get connection attempt statistics for diagnostics.
     */
    fun getConnectionStats(): Map<String, Any> {
        return mapOf(
            "totalAttempts" to connectionAttempts,
            "configuredUrlAttempts" to configuredUrlAttempts,
            "localhostAttempts" to localhostAttempts,
            "configuredUrlSuccess" to configuredUrlSuccess,
            "localhostSuccess" to localhostSuccess,
            "cachedWorkingUrl" to (cachedWorkingUrl ?: "none")
        )
    }

    fun logConnectionStats() {
        Napier.i("$TAG: Connection Stats - Total: $connectionAttempts, Configured: $configuredUrlAttempts, Localhost: $localhostAttempts, Working URL: ${cachedWorkingUrl ?: "none"}")
    }

    private suspend fun quickCheckServer(): Triple<Boolean?, String, Long?> {
        val url = gitServerUrl()
        Napier.d("$TAG: Quick checking Git server at: $url")
        val result = GitServerChecker.checkServerRunning(getClient(), url)
        Napier.d("$TAG: Quick check result: ${result.second} (time: ${result.third}ms)")
        return result
    }

    private fun getClient(): HttpClient {
        return httpClientProvider.getClientSync()
    }

    private suspend fun resolveConfiguredGitUrl(): String = urlResolutionMutex.withLock {
        cachedWorkingUrl?.let {
            Napier.d("$TAG: Using cached working URL: $it")
            return@withLock it
        }

        connectionAttempts++

        val config = serverConfigProvider()
        val baseUrl = config.baseUrl.trimEnd('/')
        
        // Use the centralized getGitEndpoint() which handles all connection types
        val gitUrl = NetworkConfig.ServiceEndpoints.getGitEndpoint(baseUrl)
        val isTailscalePathRouting = NetworkConfig.ServiceEndpoints.usesTailscalePaths(baseUrl)
        val isLocal = NetworkConfig.ServiceEndpoints.isLocalConnection(baseUrl)
        
        Napier.i("$TAG: Resolving Git URL from base: $baseUrl")
        Napier.d("$TAG: Connection type - Tailscale paths: $isTailscalePathRouting, Local: $isLocal")
        Napier.d("$TAG: Computed Git URL: $gitUrl")
        
        // Try the computed URL first
        configuredUrlAttempts++
        val checkResult = GitServerChecker.checkServerRunning(getClient(), gitUrl)
        if (GitServerChecker.isServerAvailable(checkResult)) {
            Napier.i("$TAG: [SUCCESS] Git endpoint working: $gitUrl (${checkResult.third}ms)")
            configuredUrlSuccess = true
            cachedWorkingUrl = gitUrl
            logConnectionStats()
            return@withLock gitUrl
        }
        
        Napier.w("$TAG: [ATTEMPT $connectionAttempts] Primary Git URL failed: ${checkResult.second}")
        
        // Fallback: Try localhost only for non-Tailscale connections
        // (Tailscale connections shouldn't fallback to localhost - it won't work)
        if (!NetworkConfig.ServiceEndpoints.isTailscaleUrl(baseUrl)) {
            val localhostUrl = "http://127.0.0.1:$GIT_SERVER_PORT"
            Napier.d("$TAG: Trying localhost fallback: $localhostUrl")
            localhostAttempts++
            val localhostCheck = GitServerChecker.checkServerRunning(getClient(), localhostUrl)
            
            if (GitServerChecker.isServerAvailable(localhostCheck)) {
                Napier.i("$TAG: [FALLBACK SUCCESS] Localhost working: $localhostUrl (${localhostCheck.third}ms)")
                localhostSuccess = true
                cachedWorkingUrl = localhostUrl
                logConnectionStats()
                return@withLock localhostUrl
            }
            
            Napier.e("$TAG: [ATTEMPT $connectionAttempts] Localhost fallback also failed: ${localhostCheck.second}")
        }
        
        // Return the computed URL even if check failed (let the actual request fail with better error)
        Napier.e("$TAG: All Git URL attempts failed, using computed URL: $gitUrl")
        logConnectionStats()
        gitUrl
    }

    private suspend fun gitServerUrl(): String {
        val url = resolveConfiguredGitUrl()
        if (!urlChecked) {
            Napier.d("$TAG: Using Git server URL: $url")
            urlChecked = true
        }
        return url
    }

    private suspend fun ensureServerRunning(): Result<Unit> {
        val url = gitServerUrl()
        val (isRunning, message, responseTime) = quickCheckServer()

        return when {
            GitServerChecker.isServerUnavailable(Triple(isRunning, message, responseTime)) -> {
                Napier.e("$TAG: Server check failed at $url: $message")
                val isConnectionRefused = message.contains("Connection refused", ignoreCase = true)
                Result.failure(GitServerNotRunningException(
                    message = "$message (URL: $url)",
                    isConnectionRefused = isConnectionRefused,
                    url = url
                ))
            }
            GitServerChecker.isServerSlow(Triple(isRunning, message, responseTime)) -> {
                Napier.w("$TAG: Server slow at $url (response time: ${responseTime}ms), proceeding anyway")
                Result.success(Unit)
            }
            else -> {
                Napier.d("$TAG: Server check passed at $url (response time: ${responseTime}ms)")
                Result.success(Unit)
            }
        }
    }

    suspend fun getStatus(): Result<GitStatusResponse> = safeCall("getStatus") {
        val checkResult = ensureServerRunning()
        if (checkResult.isFailure) {
            throw checkResult.exceptionOrNull()!!
        }

        val response: GitServerStatus = getClient().get("${gitServerUrl()}/status") { addAuth() }.body()

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
        val url = "${gitServerUrl()}/log?limit=$limit&skip=$skip" +
            (encodedBranch?.let { "&branch=$it" } ?: "")
        
        val response: GitServerLog = getClient().get(url) { addAuth() }.body()
        
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
        val response: GitServerBranches = getClient().get("${gitServerUrl()}/branches") { addAuth() }.body()
        
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
        val url = "${gitServerUrl()}/diff?cached=$cached" +
            (encodedPath?.let { "&path=$it" } ?: "")
        
        val response: GitServerDiff = getClient().get(url) {
            addAuth()
            timeout {
                requestTimeoutMillis = NetworkConfig.GIT_DIFF_TIMEOUT_MS
                socketTimeoutMillis = NetworkConfig.GIT_DIFF_TIMEOUT_MS
                connectTimeoutMillis = NetworkConfig.CONNECT_TIMEOUT_MS
            }
        }.body()
        
        GitDiff(
            files = response.files.map {
                GitDiffFile(
                    path = it.file,
                    additions = it.additions,
                    deletions = it.deletions,
                    binary = it.binary,
                    status = GitFileStatus.MODIFIED, // Default to modified for now
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
        val response = getClient().post("${gitServerUrl()}/commit") {
            addAuth()
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
        val response = getClient().post("${gitServerUrl()}/push") {
            addAuth()
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
        val response = getClient().post("${gitServerUrl()}/pull") {
            addAuth()
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
        val response = getClient().post("${gitServerUrl()}/fetch") {
            addAuth()
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
        val response = getClient().post("${gitServerUrl()}/checkout") {
            addAuth()
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
        val response = getClient().post("${gitServerUrl()}/stage") {
            addAuth()
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
        val response = getClient().post("${gitServerUrl()}/unstage") {
            addAuth()
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
        val response = getClient().post("${gitServerUrl()}/discard") {
            addAuth()
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
        val response: List<GitServerRemote> = getClient().get("${gitServerUrl()}/remotes") { addAuth() }.body()

        response.map {
            GitRemote(
                name = it.name,
                url = it.fetchUrl ?: "",
                pushUrl = it.pushUrl
            )
        }
    }

    suspend fun getStashes(): Result<List<GitStash>> = safeCall("getStashes") {
        val response: List<GitServerStash> = getClient().get("${gitServerUrl()}/stash") { addAuth() }.body()

        response.map {
            GitStash(
                index = it.index,
                message = it.message,
                branch = null,
                date = null
            )
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STASH MANAGEMENT (Priority 3.1)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a new stash with optional message.
     * @param message Optional stash message
     * @param includeUntracked Include untracked files
     */
    suspend fun createStash(
        message: String? = null,
        includeUntracked: Boolean = false
    ): Result<GitOperationResult> = safeCall("createStash") {
        val response = getClient().post("${gitServerUrl()}/stash") {
            addAuth()
            contentType(ContentType.Application.Json)
            setBody(GitStashRequest(message = message, includeUntracked = includeUntracked))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Stash created")
        } else {
            throw Exception(result.error ?: "Stash creation failed")
        }
    }
    
    /**
     * Pop the top stash (apply and remove).
     * @param index Stash index (0 = most recent)
     */
    suspend fun popStash(index: Int = 0): Result<GitOperationResult> = safeCall("popStash") {
        val response = getClient().post("${gitServerUrl()}/stash/pop") {
            addAuth()
            contentType(ContentType.Application.Json)
            setBody(GitStashApplyRequest(index = index, drop = true))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Stash popped")
        } else {
            throw Exception(result.error ?: "Stash pop failed")
        }
    }
    
    /**
     * Apply a stash without removing it.
     * @param index Stash index (0 = most recent)
     */
    suspend fun applyStash(index: Int = 0): Result<GitOperationResult> = safeCall("applyStash") {
        val response = getClient().post("${gitServerUrl()}/stash/apply") {
            addAuth()
            contentType(ContentType.Application.Json)
            setBody(GitStashApplyRequest(index = index, drop = false))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Stash applied")
        } else {
            throw Exception(result.error ?: "Stash apply failed")
        }
    }
    
    /**
     * Drop a stash without applying it.
     * @param index Stash index to drop
     */
    suspend fun dropStash(index: Int): Result<GitOperationResult> = safeCall("dropStash") {
        val response = getClient().delete("${gitServerUrl()}/stash/$index") { addAuth() }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Stash dropped")
        } else {
            throw Exception(result.error ?: "Stash drop failed")
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MERGE OPERATIONS (Priority 3.2)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Merge a branch into current branch.
     * @param branch Branch to merge
     * @param noFf Disable fast-forward merge
     * @param squash Squash commits into single commit
     * @param message Custom merge commit message
     */
    suspend fun merge(
        branch: String,
        noFf: Boolean = false,
        squash: Boolean = false,
        message: String? = null
    ): Result<GitOperationResult> = safeCall("merge") {
        val response = getClient().post("${gitServerUrl()}/merge") {
            addAuth()
            contentType(ContentType.Application.Json)
            setBody(GitMergeRequest(
                branch = branch,
                noFf = noFf,
                squash = squash,
                message = message
            ))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Merge successful")
        } else {
            throw Exception(result.error ?: "Merge failed")
        }
    }
    
    /**
     * Abort an in-progress merge.
     */
    suspend fun abortMerge(): Result<GitOperationResult> = safeCall("abortMerge") {
        val response = getClient().post("${gitServerUrl()}/merge/abort") { addAuth() }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Merge aborted")
        } else {
            throw Exception(result.error ?: "Merge abort failed")
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // REBASE OPERATIONS (Priority 3.3)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Rebase current branch onto another branch.
     * @param onto Target branch to rebase onto
     */
    suspend fun rebase(onto: String): Result<GitOperationResult> = safeCall("rebase") {
        val response = getClient().post("${gitServerUrl()}/rebase") {
            addAuth()
            contentType(ContentType.Application.Json)
            setBody(GitRebaseRequest(onto = onto))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Rebase successful")
        } else {
            throw Exception(result.error ?: "Rebase failed")
        }
    }
    
    /**
     * Continue a rebase after resolving conflicts.
     */
    suspend fun rebaseContinue(): Result<GitOperationResult> = safeCall("rebaseContinue") {
        val response = getClient().post("${gitServerUrl()}/rebase/continue") { addAuth() }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Rebase continued")
        } else {
            throw Exception(result.error ?: "Rebase continue failed")
        }
    }
    
    /**
     * Abort an in-progress rebase.
     */
    suspend fun rebaseAbort(): Result<GitOperationResult> = safeCall("rebaseAbort") {
        val response = getClient().post("${gitServerUrl()}/rebase/abort") { addAuth() }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Rebase aborted")
        } else {
            throw Exception(result.error ?: "Rebase abort failed")
        }
    }
    
    /**
     * Skip the current commit during rebase.
     */
    suspend fun rebaseSkip(): Result<GitOperationResult> = safeCall("rebaseSkip") {
        val response = getClient().post("${gitServerUrl()}/rebase/skip") { addAuth() }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Commit skipped")
        } else {
            throw Exception(result.error ?: "Rebase skip failed")
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TAG MANAGEMENT (Priority 3.4)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * List all tags.
     */
    suspend fun getTags(): Result<List<String>> = safeCall("getTags") {
        getClient().get("${gitServerUrl()}/tags") { addAuth() }.body()
    }
    
    /**
     * Create a new tag.
     * @param name Tag name
     * @param message Optional tag message (for annotated tags)
     * @param ref Commit reference to tag (defaults to HEAD)
     * @param annotated Create an annotated tag
     */
    suspend fun createTag(
        name: String,
        message: String? = null,
        ref: String? = null,
        annotated: Boolean = false
    ): Result<GitOperationResult> = safeCall("createTag") {
        val response = getClient().post("${gitServerUrl()}/tags") {
            addAuth()
            contentType(ContentType.Application.Json)
            setBody(GitTagRequest(
                name = name,
                message = message,
                ref = ref,
                annotated = annotated || message != null
            ))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Tag created")
        } else {
            throw Exception(result.error ?: "Tag creation failed")
        }
    }
    
    /**
     * Delete a tag.
     * @param name Tag name to delete
     */
    suspend fun deleteTag(name: String): Result<GitOperationResult> = safeCall("deleteTag") {
        val response = getClient().delete("${gitServerUrl()}/tags/${name.encodeURLParameter()}") { addAuth() }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Tag deleted")
        } else {
            throw Exception(result.error ?: "Tag deletion failed")
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // REMOTE MANAGEMENT (Priority 3.5)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Add a new remote.
     * @param name Remote name
     * @param url Remote URL
     */
    suspend fun addRemote(name: String, url: String): Result<GitOperationResult> = safeCall("addRemote") {
        val response = getClient().post("${gitServerUrl()}/remotes") {
            addAuth()
            contentType(ContentType.Application.Json)
            setBody(GitRemoteRequest(name = name, url = url))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Remote added")
        } else {
            throw Exception(result.error ?: "Add remote failed")
        }
    }
    
    /**
     * Remove a remote.
     * @param name Remote name to remove
     */
    suspend fun removeRemote(name: String): Result<GitOperationResult> = safeCall("removeRemote") {
        val response = getClient().delete("${gitServerUrl()}/remotes/${name.encodeURLParameter()}") { addAuth() }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Remote removed")
        } else {
            throw Exception(result.error ?: "Remove remote failed")
        }
    }
    
    /**
     * Update a remote URL.
     * @param name Remote name
     * @param url New remote URL
     */
    suspend fun setRemoteUrl(name: String, url: String): Result<GitOperationResult> = safeCall("setRemoteUrl") {
        val response = getClient().put("${gitServerUrl()}/remotes/${name.encodeURLParameter()}") {
            addAuth()
            contentType(ContentType.Application.Json)
            setBody(GitRemoteRequest(name = name, url = url))
        }
        
        val result: GitServerResult = response.body()
        if (result.success) {
            GitOperationResult(true, result.message ?: "Remote URL updated")
        } else {
            throw Exception(result.error ?: "Update remote failed")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SECURITY CONFIGURATION (Priority 7.1)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    companion object {
        private const val TAG = "GitApiClient"
        // Git HTTP server runs on port 4097 (OpenCode plugin auto-starts it)
        private const val GIT_SERVER_PORT = NetworkConfig.GIT_SERVER_PORT
        
        /**
         * Whitelist of allowed commands for security.
         * Only these commands can be executed via the /command endpoint.
         */
        private val ALLOWED_COMMANDS = setOf(
            "start-git-server",
            "stop-git-server",
            "restart-git-server"
        )
        
        /**
         * Maximum number of command requests per minute (rate limiting).
         */
        private const val MAX_COMMANDS_PER_MINUTE = NetworkConfig.MAX_COMMANDS_PER_MINUTE
    }
    
    // Rate limiting tracking
    private val commandExecutionLog = mutableListOf<Long>()
    private val commandLock = Mutex()
    
    /**
     * Validates if a command is allowed to be executed.
     * @param command The command to validate
     * @return true if command is in whitelist
     */
    private fun isCommandAllowed(command: String): Boolean {
        return ALLOWED_COMMANDS.contains(command.lowercase().trim())
    }
    
    /**
     * Checks rate limiting for command execution.
     * @return true if within rate limits
     */
    private suspend fun checkRateLimit(): Boolean {
        return commandLock.withLock {
            val now = System.currentTimeMillis()
            val windowStart = now - NetworkConfig.RATE_LIMIT_WINDOW_MS
            
            // Remove entries older than rate limit window
            commandExecutionLog.removeAll { it < windowStart }
            
            // Check if under limit
            if (commandExecutionLog.size >= MAX_COMMANDS_PER_MINUTE) {
                Napier.w("$TAG: Rate limit exceeded for command execution")
                false
            } else {
                commandExecutionLog.add(now)
                true
            }
        }
    }
    
    /**
     * Logs command execution for security auditing.
     */
    private fun logCommandExecution(command: String, success: Boolean, error: String? = null) {
        val status = if (success) "SUCCESS" else "FAILED"
        val timestamp = java.time.Instant.now().toString()
        val message = "[$timestamp] Command execution: $command - $status"
        
        if (success) {
            Napier.i("$TAG: $message")
        } else {
            Napier.e("$TAG: $message${error?.let { " - Error: $it" } ?: ""}")
        }
    }

    /**
     * Request the Git HTTP server to start.
     * Sends a command to the OpenCode server which triggers start-git-server.ps1.
     * 
     * SECURITY: This operation is protected by:
     * - Command whitelist validation (only 'start-git-server' allowed)
     * - Rate limiting (max 10 commands per minute)
     * - Execution logging for audit trail
     * - Authentication via HttpClient auth headers
     */
    suspend fun requestStartGitServer(): Result<Unit> = safeCall("requestStartGitServer") {
        val command = "start-git-server"
        
        // Validate command is in whitelist
        if (!isCommandAllowed(command)) {
            logCommandExecution(command, false, "Command not in whitelist")
            throw SecurityException("Command '$command' is not allowed. Allowed commands: $ALLOWED_COMMANDS")
        }
        
        // Check rate limiting
        if (!checkRateLimit()) {
            logCommandExecution(command, false, "Rate limit exceeded")
            throw SecurityException("Rate limit exceeded. Maximum $MAX_COMMANDS_PER_MINUTE commands per minute.")
        }
        
        // Execute command
        try {
            getClient().post("${gitServerUrl()}/command") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("command" to command))
            }
            logCommandExecution(command, true)
        } catch (e: Exception) {
            logCommandExecution(command, false, e.message)
            throw e
        }
        
        Unit
    }

    /**
     * Check if the Git HTTP server is currently running.
     */
    suspend fun isServerRunning(): Boolean {
        return try {
            val result = quickCheckServer()
            GitServerChecker.isServerAvailable(result)
        } catch (e: Exception) {
            Napier.w("$TAG: isServerRunning check failed: ${e.message}")
            false
        }
    }

    /**
     * Request Git server start and wait for it to become available.
     * Uses polling to verify the server is actually running.
     */
    suspend fun requestStartGitServerAndWait(
        maxWaitMs: Long = NetworkConfig.GIT_SERVER_MAX_WAIT_MS,
        pollIntervalMs: Long = NetworkConfig.GIT_SERVER_POLL_INTERVAL_MS
    ): Result<Boolean> = safeCall("requestStartGitServerAndWait") {
        requestStartGitServer().getOrThrow()

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            if (isServerRunning()) {
                Napier.i("$TAG: Git server is now running")
                return@safeCall true
            }
            kotlinx.coroutines.delay(pollIntervalMs)
        }

        throw Exception("Git server did not start within ${maxWaitMs}ms")
    }

    // --- Helpers ---

    private fun addAuth() {
        // Auth is handled at the HttpClient level via HttpClientProvider
        // No per-request auth needed here
    }

    private suspend inline fun <reified T> safeCall(
        tag: String = "API",
        crossinline block: suspend () -> T
    ): Result<T> {
        return try {
            Napier.d("$TAG: $tag")
            Result.success(block())
        } catch (e: Exception) {
            Napier.e("$TAG: $tag failed: ${e.message}", e)
            Result.failure(NetworkError.from(e))
        }
    }
}