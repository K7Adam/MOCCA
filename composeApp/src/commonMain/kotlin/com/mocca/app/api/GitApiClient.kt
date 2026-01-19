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
import kotlinx.coroutines.sync.withLock

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

    /**
     * Quick check if Git server is running before attempting operations.
     * Uses 500ms timeout to quickly detect server availability.
     *
     * @return Triple<Boolean?, String, Long?> where:
     *         - Boolean?: true if server running, false if not, null if timeout
     *         - String: Status message
     *         - Long?: Response time
     */
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

    /**
     * Resolve the Git server URL with intelligent fallback logic.
     * Tries configured URL first, falls back to localhost if it fails.
     * NOTE: Mutex ensures only one coroutine performs URL resolution at a time.
     */
    private suspend fun resolveConfiguredGitUrl(): String = urlResolutionMutex.withLock {
        // Return cached working URL if we found one
        cachedWorkingUrl?.let {
            Napier.d("$TAG: Using cached working URL: $it")
            return@withLock it
        }

        connectionAttempts++

        val config = serverConfigProvider()
        val baseUrl = config.baseUrl.trimEnd('/')
        val configuredUrl = try {
            val regex = Regex("""https?://([^:/]+)""")
            val match = regex.find(baseUrl)
            if (match != null) {
                "http://${match.groupValues[1]}:$GIT_SERVER_PORT"
            } else {
                null
            }
        } catch (e: Exception) {
            Napier.w("$TAG: Failed to parse configured URL: ${e.message}")
            null
        }

        val localhostUrl = "http://127.0.0.1:$GIT_SERVER_PORT"

        // If no configured URL, use localhost
        if (configuredUrl == null) {
            Napier.w("$TAG: No configured URL found, using localhost: $localhostUrl")
            cachedWorkingUrl = localhostUrl
            localhostAttempts++
            return@withLock localhostUrl
        }

        // Try configured URL first
        Napier.d("$TAG: [Attempt $connectionAttempts] Checking configured URL: $configuredUrl")
        configuredUrlAttempts++
        val checkResult = GitServerChecker.checkServerRunning(getClient(), configuredUrl)
        if (GitServerChecker.isServerAvailable(checkResult)) {
            Napier.i("$TAG: [SUCCESS] Configured URL working: $configuredUrl (${checkResult.third}ms)")
            configuredUrlSuccess = true
            cachedWorkingUrl = configuredUrl
            logConnectionStats()
            return@withLock configuredUrl
        }

        // Configured URL failed, try localhost fallback
        Napier.w("$TAG: [ATTEMPT $connectionAttempts] Configured URL failed: ${checkResult.second}, trying localhost")
        localhostAttempts++
        val localhostCheck = GitServerChecker.checkServerRunning(getClient(), localhostUrl)

        return if (GitServerChecker.isServerAvailable(localhostCheck)) {
            Napier.i("$TAG: [FALLBACK SUCCESS] Localhost working: $localhostUrl (${localhostCheck.third}ms)")
            localhostSuccess = true
            cachedWorkingUrl = localhostUrl
            logConnectionStats()
            localhostUrl
        } else {
            // Both failed, use configured URL (it will fail with proper error message)
            Napier.e("$TAG: [ATTEMPT $connectionAttempts] Both URLs failed, using configured URL: $configuredUrl")
            Napier.e("$TAG: Configured error: ${checkResult.second}, Localhost error: ${localhostCheck.second}")
            logConnectionStats()
            configuredUrl
        }
    }

    /**
     * Git server base URL - uses same host as OpenCode but on port 4097.
     * The git-plugin.js embedded in OpenCode automatically starts an HTTP server
     * on port 4097 when `opencode serve` runs.
     *
     * IMPORTANT: Git server ALWAYS uses HTTP (not HTTPS) because it's a
     * local development server that doesn't configure SSL/TLS.
     *
     * This property uses intelligent fallback:
     * 1. First try configured URL (e.g., http://10.0.2.2:4097 for emulator)
     * 2. If that fails, try localhost (http://127.0.0.1:4097) which works via adb reverse
     */
    private suspend fun gitServerUrl(): String {
        val url = resolveConfiguredGitUrl()
        if (!urlChecked) {
            Napier.d("$TAG: Using Git server URL: $url")
            urlChecked = true
        }
        return url
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

    /**
     * Quick check if Git server is running before attempting full operation.
     * Returns error immediately if server is not available.
     */
    private suspend fun ensureServerRunning(): Result<Unit> {
        val url = gitServerUrl()
        val (isRunning, message, responseTime) = quickCheckServer()

        return when {
            GitServerChecker.isServerUnavailable(Triple(isRunning, message, responseTime)) -> {
                // Server is not running - fail fast
                Napier.e("$TAG: Server check failed at $url: $message")
                // Check if connection was refused (port closed/no service)
                val isConnectionRefused = message.contains("Connection refused", ignoreCase = true)
                Result.failure(GitServerNotRunningException(
                    message = "$message (URL: $url)",
                    isConnectionRefused = isConnectionRefused,
                    url = url
                ))
            }
            GitServerChecker.isServerSlow(Triple(isRunning, message, responseTime)) -> {
                // Server is slow but running - log warning but continue
                Napier.w("$TAG: Server slow at $url (response time: ${responseTime}ms), proceeding anyway")
                Result.success(Unit)
            }
            else -> {
                // Server is running normally
                Napier.d("$TAG: Server check passed at $url (response time: ${responseTime}ms)")
                Result.success(Unit)
            }
        }
    }

    /**
     * Custom exception for when Git server is not running.
     */
    class GitServerNotRunningException(
        message: String, 
        val isConnectionRefused: Boolean = false,
        val url: String? = null
    ) : Exception(message) {
        val isServerDown: Boolean = true
    }

    suspend fun getStatus(): Result<GitStatusResponse> = safeCall("getStatus") {
        // Quick check before full operation
        val checkResult = ensureServerRunning()
        if (checkResult.isFailure) {
            throw checkResult.exceptionOrNull()!!
        }

        val response: GitServerStatus = getClient().get("${gitServerUrl()}/status").body()

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
        val response: GitServerBranches = getClient().get("${gitServerUrl()}/branches").body()
        
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
        val response = getClient().post("${gitServerUrl()}/commit") {
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
        val response: List<GitServerRemote> = getClient().get("${gitServerUrl()}/remotes").body()
        
        response.map {
            GitRemote(
                name = it.name,
                url = it.fetchUrl ?: "",
                pushUrl = it.pushUrl
            )
        }
    }

    suspend fun getStashes(): Result<List<GitStash>> = safeCall("getStashes") {
        val response: List<GitServerStash> = getClient().get("${gitServerUrl()}/stash").body()
        
        response.map {
            GitStash(
                index = it.index,
                message = it.message,
                branch = null,
                date = null
            )
        }
    }

    suspend fun requestStartGitServer(): Result<Unit> = safeCall("requestStartGitServer") {
        val config = serverConfigProvider()
        val openCodeUrl = config.baseUrl.trimEnd('/')
        
        Napier.i("$TAG: Requesting git server start at $openCodeUrl/command")
        
        val response = getClient().post("$openCodeUrl/command") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("command" to "start-git-server"))
        }
        
        if (response.status.value in 200..299) {
            Napier.i("$TAG: Git server start request sent successfully")
            Unit
        } else {
            throw Exception("Failed to start git server: ${response.status}")
        }
    }

    /**
     * Request git server start and wait for it to become available.
     * Polls every 500ms for up to maxWaitMs (default 10 seconds).
     *
     * @return Result.success(true) if server started, Result.failure if timeout or error
     */
    suspend fun requestStartGitServerAndWait(
        maxWaitMs: Long = 10_000L,
        pollIntervalMs: Long = 500L
    ): Result<Boolean> {
        // First, send the start command
        val startResult = requestStartGitServer()
        if (startResult.isFailure) {
            Napier.e("$TAG: Failed to send start command: ${startResult.exceptionOrNull()?.message}")
            return Result.failure(startResult.exceptionOrNull() ?: Exception("Failed to send start command"))
        }

        Napier.i("$TAG: Start command sent, polling for server availability...")

        // Poll until server is available or timeout
        val startTime = System.currentTimeMillis()
        var attempts = 0
        val url = gitServerUrl()
        Napier.d("$TAG: Polling server at: $url")

        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            attempts++
            kotlinx.coroutines.delay(pollIntervalMs)

            val checkResult = quickCheckServer()
            if (GitServerChecker.isServerAvailable(checkResult)) {
                val elapsed = System.currentTimeMillis() - startTime
                Napier.i("$TAG: Git server is now available at $url after ${elapsed}ms ($attempts attempts)")
                return Result.success(true)
            }

            if (attempts % 4 == 0) { // Log every 2 seconds
                Napier.d("$TAG: Server not yet available at $url (attempt $attempts/${maxWaitMs / pollIntervalMs}), continuing...")
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        Napier.w("$TAG: Git server did not become available at $url within ${elapsed}ms ($attempts attempts)")
        return Result.failure(
            Exception("Git server did not start within ${maxWaitMs / 1000} seconds. URL: $url. Please check host logs.")
        )
    }

    /**
     * Check if git server is currently running (public access for polling).
     */
    suspend fun isServerRunning(): Boolean {
        val url = gitServerUrl()
        Napier.d("$TAG: Checking if server running at: $url")
        val checkResult = quickCheckServer()
        val isAvailable = GitServerChecker.isServerAvailable(checkResult)
        Napier.d("$TAG: Server running check at $url: $isAvailable (${checkResult.second})")
        return isAvailable
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