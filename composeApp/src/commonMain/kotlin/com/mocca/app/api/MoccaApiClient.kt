package com.mocca.app.api

import com.mocca.app.domain.model.*
import com.mocca.app.domain.model.McpServerStatus
import com.mocca.app.domain.model.McpConnectRequest
import com.mocca.app.domain.model.McpConfigureRequest
import com.mocca.app.domain.model.McpServerConfig
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.sse.SSE
import io.ktor.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * OpenCode API Client for REST endpoints.
 * Supports dynamic HttpClient recreation when server config changes.
 */
class MoccaApiClient(
    private var httpClient: HttpClient,
    private val serverConfigProvider: () -> ServerConfig,
    private val retryPolicy: RetryPolicy = RetryPolicy.Default,
    private val httpClientProvider: HttpClientProvider? = null
) {
    init {
        // Subscribe to client recreation events
        httpClientProvider?.onClientRecreated = {
            // Update the client reference when provider recreates it
            httpClientProvider.let { provider ->
                httpClient = provider.getClientSync()
            }
        }
    }
    
    /**
     * Get the current HttpClient, ensuring it's up-to-date with server config.
     */
    private fun getClient(): HttpClient {
        return httpClientProvider?.getClientSync() ?: httpClient
    }
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    private val baseUrl: String
        get() = serverConfigProvider().baseUrl.trimEnd('/')

    // Health
    suspend fun getHealth(): Result<AppInfo> = safeCall("getHealth") {
        getClient().get("$baseUrl/global/health").body()
    }
    
    suspend fun getAppInfo(): Result<AppInfo> = safeCall("getAppInfo") {
        getClient().get("$baseUrl/app").body()
    }

    // Sessions
    suspend fun listSessions(): Result<List<Session>> = safeCall("listSessions") {
        getClient().get("$baseUrl/session").body()
    }

    suspend fun createSession(): Result<Session> = safeCallNoRetry("createSession") {
        getClient().post("$baseUrl/session").body()
    }

    suspend fun getChildren(sessionId: String): Result<List<Session>> = safeCall("getChildren") {
        getClient().get("$baseUrl/session/$sessionId/children").body()
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> = safeCallNoRetry("deleteSession") {
        getClient().delete("$baseUrl/session/$sessionId")
    }
    
    suspend fun abortSession(sessionId: String): Result<Boolean> = safeCallNoRetry("abortSession") {
        getClient().post("$baseUrl/session/$sessionId/abort").body()
    }

    // Messages
    suspend fun getMessages(sessionId: String): Result<List<MessageResponse>> = safeCall("getMessages") {
        val response = getClient().get("$baseUrl/session/$sessionId/message")
        val rawText = response.bodyAsText()
        json.decodeFromString<List<MessageResponse>>(rawText)
    }

    suspend fun chat(
        sessionId: String,
        modelId: String,
        providerId: String,
        parts: List<ChatPart>,
        mode: String? = null
    ): Result<AssistantMessageInfo> = safeCallNoRetry("chat") {
        getClient().post("$baseUrl/session/$sessionId/message") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                modelID = modelId,
                providerID = providerId,
                parts = parts,
                mode = mode
            ))
        }.body()
    }
    
    suspend fun chatAsync(
        sessionId: String,
        modelId: String,
        providerId: String,
        parts: List<ChatPart>,
        mode: String? = null
    ): Result<Unit> = safeCallNoRetry("chatAsync") {
        val response = getClient().post("$baseUrl/session/$sessionId/prompt_async") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                modelID = modelId,
                providerID = providerId,
                parts = parts,
                mode = mode
            ))
        }
        if (response.status.value in 200..299) {
            Unit
        } else {
            throw Exception("Unexpected response: ${response.status}")
        }
    }

    // Permissions (legacy)
    suspend fun respondToPermission(
        sessionId: String,
        permissionId: String,
        allow: Boolean,
        remember: Boolean = false
    ): Result<Boolean> = safeCallNoRetry("respondToPermission") {
        getClient().post("$baseUrl/session/$sessionId/permissions/$permissionId") {
            contentType(ContentType.Application.Json)
            setBody(PermissionResponse(
                response = if (allow) "allow" else "deny",
                remember = remember
            ))
        }.body()
    }
    
    // Permissions (new API matching OpenChamber SDK)
    /**
     * Reply to a permission request using the new permission.reply API.
     * @param requestId The permission request ID
     * @param reply One of: "once", "always", "reject"
     * @param message Optional message for rejection
     */
    suspend fun replyToPermission(
        requestId: String,
        reply: PermissionResponseType,
        message: String? = null
    ): Result<Boolean> = safeCallNoRetry("replyToPermission") {
        getClient().post("$baseUrl/permission/$requestId/reply") {
            contentType(ContentType.Application.Json)
            setBody(PermissionReplyRequest(
                requestID = requestId,
                reply = reply.value,
                message = message
            ))
        }.body()
    }
    
    /**
     * List all pending permission requests.
     */
    suspend fun listPendingPermissions(): Result<List<PermissionRequest>> = safeCall("listPendingPermissions") {
        getClient().get("$baseUrl/permission").body()
    }
    
    // Questions (matching OpenChamber SDK)
    /**
     * Reply to a question request.
     * @param requestId The question request ID
     * @param answers Answers as a list of string lists (one list per question, multiple selections if multiple=true)
     */
    suspend fun replyToQuestion(
        requestId: String,
        answers: List<List<String>>
    ): Result<Boolean> = safeCallNoRetry("replyToQuestion") {
        getClient().post("$baseUrl/question/$requestId/reply") {
            contentType(ContentType.Application.Json)
            setBody(QuestionReplyRequest(
                requestID = requestId,
                answers = answers
            ))
        }.body()
    }
    
    /**
     * Reject a question request.
     */
    suspend fun rejectQuestion(requestId: String): Result<Boolean> = safeCallNoRetry("rejectQuestion") {
        getClient().post("$baseUrl/question/$requestId/reject") {
            contentType(ContentType.Application.Json)
            setBody(QuestionRejectRequest(requestID = requestId))
        }.body()
    }
    
    /**
     * List all pending question requests.
     */
    suspend fun listPendingQuestions(): Result<List<QuestionRequest>> = safeCall("listPendingQuestions") {
        getClient().get("$baseUrl/question").body()
    }

    // Session fork/revert (matching OpenChamber SDK)
    /**
     * Fork a session from a specific message.
     * Creates a new session with messages up to the specified message.
     * @param sessionId The session to fork from
     * @param messageId Optional message ID to fork from (null = current state)
     * @return The new forked session
     */
    suspend fun forkSession(
        sessionId: String,
        messageId: String? = null
    ): Result<Session> = safeCallNoRetry("forkSession") {
        getClient().post("$baseUrl/session/$sessionId/fork") {
            contentType(ContentType.Application.Json)
            setBody(ForkSessionRequest(messageID = messageId))
        }.body()
    }
    
    /**
     * Revert a session to a specific message.
     * Messages after the specified message are hidden but not deleted.
     * @param sessionId The session to revert
     * @param messageId The message ID to revert to
     * @param partId Optional part ID for partial revert
     * @return The updated session with revert state
     */
    suspend fun revertSession(
        sessionId: String,
        messageId: String,
        partId: String? = null
    ): Result<Session> = safeCallNoRetry("revertSession") {
        getClient().post("$baseUrl/session/$sessionId/revert") {
            contentType(ContentType.Application.Json)
            setBody(RevertSessionRequest(messageID = messageId, partID = partId))
        }.body()
    }
    
    /**
     * Unrevert a session - restore all hidden messages.
     * @param sessionId The session to unrevert
     * @return The updated session without revert state
     */
    suspend fun unrevertSession(sessionId: String): Result<Session> = safeCallNoRetry("unrevertSession") {
        getClient().post("$baseUrl/session/$sessionId/unrevert").body()
    }
    
    /**
     * Update session title.
     */
    suspend fun updateSession(sessionId: String, title: String): Result<Session> = safeCallNoRetry("updateSession") {
        getClient().post("$baseUrl/session/$sessionId") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("title" to title))
        }.body()
    }
    
    /**
     * Get session status for all sessions (idle/busy/retry).
     */
    suspend fun getSessionStatus(): Result<Map<String, SessionStatusInfo>> = safeCall("getSessionStatus") {
        getClient().get("$baseUrl/session/status").body()
    }

    // Config
    suspend fun getProviders(): Result<List<Provider>> = safeCall("getProviders") {
        val response: ProvidersResponse = getClient().get("$baseUrl/config/providers").body()
        response.providers
    }

    suspend fun getModes(): Result<List<Mode>> = safeCall("getModes") {
        val config: ConfigResponse = getClient().get("$baseUrl/config").body()
        config.modes
    }
    
    suspend fun getConfig(): Result<ConfigResponse> = safeCall("getConfig") {
        getClient().get("$baseUrl/config").body()
    }
    
    // Provider info (from /provider endpoint - different from /config/providers)
    suspend fun getProviderInfo(): Result<ProviderResponse> = safeCall("getProviderInfo") {
        getClient().get("$baseUrl/provider").body()
    }
    
    suspend fun getProvidersConfig(): Result<ProvidersConfig> = safeCall("getProvidersConfig") {
        getClient().get("$baseUrl/config/providers").body()
    }
    
    // Agents
    suspend fun getAgents(): Result<List<Agent>> = safeCall("getAgents") {
        getClient().get("$baseUrl/agent").body()
    }
    
    // Tools
    suspend fun getToolIds(): Result<List<String>> = safeCall("getToolIds") {
        getClient().get("$baseUrl/experimental/tool/ids").body()
    }
    
    // Slash Commands
    suspend fun getCommands(): Result<List<Command>> = safeCall("getCommands") {
        getClient().get("$baseUrl/command").body()
    }
    
    // Formatters
    suspend fun getFormatters(): Result<List<FormatterStatus>> = safeCall("getFormatters") {
        getClient().get("$baseUrl/formatter").body()
    }
    
    // LSP Status
    suspend fun getLspStatus(): Result<List<LspStatus>> = safeCall("getLspStatus") {
        getClient().get("$baseUrl/lsp").body()
    }
    
    // VCS Info
    suspend fun getVcsInfo(): Result<VcsInfo> = safeCall("getVcsInfo") {
        getClient().get("$baseUrl/vcs").body()
    }
    
    // Session Diffs
    suspend fun getSessionDiffs(sessionId: String): Result<List<FileDiff>> = safeCall("getSessionDiffs") {
        getClient().get("$baseUrl/session/$sessionId/diff").body()
    }

    // Files
    suspend fun listFiles(path: String = "."): Result<List<FileInfo>> = safeCall("listFiles") {
        getClient().get("$baseUrl/file") {
            parameter("path", path.ifEmpty { "." })
        }.body()
    }

    suspend fun getFileContent(path: String): Result<FileContent> = safeCall("getFileContent") {
        getClient().get("$baseUrl/file/content") {
            parameter("path", path)
        }.body()
    }
    
    suspend fun getFileStatus(path: String): Result<FileStatus> = safeCall("getFileStatus") {
        getClient().get("$baseUrl/file/status") {
            parameter("path", path)
        }.body()
    }
    
    /**
     * Update file content.
     * @param path Path to the file
     * @param content New file content
     * @return Result indicating success or failure
     */
    suspend fun updateFile(path: String, content: String): Result<Unit> = safeCallNoRetry("updateFile") {
        getClient().post("$baseUrl/file") {
            contentType(ContentType.Application.Json)
            setBody(FileUpdateRequest(path = path, content = content))
        }
        Unit
    }
    
    // Search
    suspend fun searchText(query: String, path: String = ""): Result<List<SearchResult>> = safeCall("searchText") {
        getClient().get("$baseUrl/find") {
            parameter("query", query)
            if (path.isNotEmpty()) {
                parameter("path", path)
            }
        }.body()
    }

    suspend fun findFiles(pattern: String): Result<List<String>> = safeCall("findFiles") {
        getClient().get("$baseUrl/find/file") {
            parameter("pattern", pattern)
        }.body()
    }

    suspend fun findSymbols(query: String): Result<List<SymbolResult>> = safeCall("findSymbols") {
        getClient().get("$baseUrl/find/symbol") {
            parameter("query", query)
        }.body()
    }

    // Git Operations
    suspend fun getGitStatus(): Result<GitStatusResponse> = safeCall("getGitStatus") {
        getClient().get("$baseUrl/git/status").body()
    }
    
    suspend fun getGitBranches(): Result<List<GitBranch>> = safeCall("getGitBranches") {
        getClient().get("$baseUrl/git/branches").body()
    }
    
    suspend fun getGitLog(
        branch: String? = null,
        limit: Int = 50,
        skip: Int = 0
    ): Result<GitLog> = safeCall("getGitLog") {
        getClient().get("$baseUrl/git/log") {
            branch?.let { parameter("branch", it) }
            parameter("limit", limit)
            parameter("skip", skip)
        }.body()
    }
    
    suspend fun getGitDiff(
        ref: String? = null,
        cached: Boolean = false
    ): Result<GitDiff> = safeCall("getGitDiff") {
        getClient().get("$baseUrl/git/diff") {
            ref?.let { parameter("ref", it) }
            if (cached) parameter("cached", true)
        }.body()
    }
    
    suspend fun gitStage(files: List<String>): Result<GitOperationResult> = safeCallNoRetry("gitStage") {
        getClient().post("$baseUrl/git/stage") {
            contentType(ContentType.Application.Json)
            setBody(GitStageRequest(files = files))
        }.body()
    }
    
    suspend fun gitUnstage(files: List<String>): Result<GitOperationResult> = safeCallNoRetry("gitUnstage") {
        getClient().post("$baseUrl/git/unstage") {
            contentType(ContentType.Application.Json)
            setBody(GitUnstageRequest(files = files))
        }.body()
    }
    
    suspend fun gitDiscard(files: List<String>): Result<GitOperationResult> = safeCallNoRetry("gitDiscard") {
        getClient().post("$baseUrl/git/discard") {
            contentType(ContentType.Application.Json)
            setBody(GitDiscardRequest(files = files))
        }.body()
    }
    
    suspend fun gitCommit(
        message: String,
        files: List<String>? = null,
        amend: Boolean = false
    ): Result<GitOperationResult> = safeCallNoRetry("gitCommit") {
        getClient().post("$baseUrl/git/commit") {
            contentType(ContentType.Application.Json)
            setBody(GitCommitRequest(message = message, files = files, amend = amend))
        }.body()
    }
    
    suspend fun gitPush(
        remote: String = "origin",
        branch: String? = null,
        force: Boolean = false,
        setUpstream: Boolean = false
    ): Result<GitOperationResult> = safeCallNoRetry("gitPush") {
        getClient().post("$baseUrl/git/push") {
            contentType(ContentType.Application.Json)
            setBody(GitPushRequest(remote = remote, branch = branch, force = force, setUpstream = setUpstream))
        }.body()
    }
    
    suspend fun gitPull(
        remote: String = "origin",
        branch: String? = null,
        rebase: Boolean = false
    ): Result<GitOperationResult> = safeCallNoRetry("gitPull") {
        getClient().post("$baseUrl/git/pull") {
            contentType(ContentType.Application.Json)
            setBody(GitPullRequest(remote = remote, branch = branch, rebase = rebase))
        }.body()
    }
    
    suspend fun gitFetch(
        remote: String = "origin",
        prune: Boolean = false,
        all: Boolean = false
    ): Result<GitOperationResult> = safeCallNoRetry("gitFetch") {
        getClient().post("$baseUrl/git/fetch") {
            contentType(ContentType.Application.Json)
            setBody(GitFetchRequest(remote = remote, prune = prune, all = all))
        }.body()
    }
    
    suspend fun gitCheckout(
        ref: String,
        create: Boolean = false,
        force: Boolean = false
    ): Result<GitOperationResult> = safeCallNoRetry("gitCheckout") {
        getClient().post("$baseUrl/git/checkout") {
            contentType(ContentType.Application.Json)
            setBody(GitCheckoutRequest(ref = ref, create = create, force = force))
        }.body()
    }
    
    suspend fun getGitRemotes(): Result<List<GitRemote>> = safeCall("getGitRemotes") {
        getClient().get("$baseUrl/git/remotes").body()
    }
    
    suspend fun getGitStashes(): Result<List<GitStash>> = safeCall("getGitStashes") {
        getClient().get("$baseUrl/git/stash").body()
    }

    // MCP Operations
    /**
     * Get status of all MCP servers.
     * Returns a map of server name to McpServerStatus.
     */
    suspend fun getMcpStatus(directory: String? = null): Result<Map<String, McpServerStatus>> = safeCall("getMcpStatus") {
        getClient().get("$baseUrl/mcp") {
            directory?.let { parameter("directory", it) }
        }.body()
    }
    
    /**
     * Connect to an MCP server by name.
     */
    suspend fun connectMcp(name: String, directory: String? = null): Result<Unit> = safeCallNoRetry("connectMcp") {
        getClient().post("$baseUrl/mcp/connect") {
            contentType(ContentType.Application.Json)
            setBody(McpConnectRequest(name = name, directory = directory))
        }
        Unit
    }
    
    /**
     * Disconnect from an MCP server by name.
     */
    suspend fun disconnectMcp(name: String, directory: String? = null): Result<Unit> = safeCallNoRetry("disconnectMcp") {
        getClient().post("$baseUrl/mcp/disconnect") {
            contentType(ContentType.Application.Json)
            setBody(McpConnectRequest(name = name, directory = directory))
        }
        Unit
    }
    
    /**
     * Configure/update an MCP server.
     */
    suspend fun configureMcp(name: String, config: McpServerConfig): Result<Unit> = safeCallNoRetry("configureMcp") {
        getClient().post("$baseUrl/mcp") {
            contentType(ContentType.Application.Json)
            setBody(McpConfigureRequest(name = name, config = config))
        }
        Unit
    }

    // Terminal
    suspend fun listTerminals(): Result<List<Terminal>> = safeCall("listTerminals") {
        getClient().get("$baseUrl/terminal").body()
    }

    suspend fun createTerminal(): Result<Terminal> = safeCallNoRetry("createTerminal") {
        getClient().post("$baseUrl/terminal").body()
    }

    suspend fun resizeTerminal(id: String, cols: Int, rows: Int): Result<Unit> = safeCallNoRetry("resizeTerminal") {
        getClient().post("$baseUrl/terminal/$id/resize") {
            contentType(ContentType.Application.Json)
            setBody(TerminalResizeRequest(cols, rows))
        }
    }

    suspend fun connectToTerminal(id: String, block: suspend DefaultClientWebSocketSession.() -> Unit) {
        val wsUrl = baseUrl.replace("http", "ws").replace("https", "wss")
        getClient().webSocket("$wsUrl/terminal/$id/socket") {
            block()
        }
    }

    // Helpers
    private suspend inline fun <reified T> safeCall(
        tag: String = "API",
        retryable: Boolean = true,
        crossinline block: suspend () -> T
    ): Result<T> {
        val policy = if (retryable) retryPolicy else RetryPolicy.None
        return withRetry(policy, tag) {
            block()
        }.mapError { error ->
            NetworkError.from(error)
        }
    }
    
    private suspend inline fun <reified T> safeCallNoRetry(
        tag: String = "API",
        crossinline block: suspend () -> T
    ): Result<T> = safeCall(tag, retryable = false, block)
    
    private inline fun <T, E : Throwable> Result<T>.mapError(transform: (Throwable) -> E): Result<T> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(transform(it)) }
        )
    }

    companion object {
        fun createHttpClient(config: ServerConfig, engine: io.ktor.client.engine.HttpClientEngine): HttpClient {
            return HttpClient(engine) {
                install(ContentNegotiation) {
                    json(Json { 
                        ignoreUnknownKeys = true 
                        isLenient = true
                    })
                }

                install(WebSockets) {
                   // Default config
                }

                install(SSE) {
                    // SSE plugin for Server-Sent Events
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = 120_000
                    connectTimeoutMillis = 10_000
                    socketTimeoutMillis = 120_000
                }

                if (config.authType == AuthType.BEARER && config.authToken != null) {
                    install(Auth) {
                        bearer {
                            loadTokens {
                                BearerTokens(config.authToken, "")
                            }
                        }
                    }
                }

                defaultRequest {
                    url(config.baseUrl)
                }
            }
        }
    }
}
