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
import com.mocca.app.api.NetworkError

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
    
    /**
     * @deprecated This endpoint returns HTML, not JSON. Use [getHealth] instead.
     * The /app endpoint is a frontend route, not a REST API endpoint.
     * 
     * Forensic Audit Reference: OPENCODE_API_ANALYSIS.md - Endpoint Reality Map
     */
    @Deprecated(
        message = "The /app endpoint returns HTML. Use getHealth() for server status.",
        replaceWith = ReplaceWith("getHealth()"),
        level = DeprecationLevel.ERROR
    )
    suspend fun getAppInfo(): Result<AppInfo> = Result.failure(
        NetworkError.ServerError(
            statusCode = 406,
            message = "Deprecated: /app returns HTML. Use getHealth() instead."
        )
    )

    // Sessions
    suspend fun listSessions(): Result<List<Session>> = safeRequest("listSessions") {
        getClient().get("$baseUrl/session")
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
    
    // Permissions
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
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // OAUTH / AUTHENTICATION (Priority 1.1, 1.2)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get available authentication methods for a provider.
     * @param providerId The provider ID (e.g., "anthropic", "openai")
     * @return List of authentication methods (api_key, oauth, etc.)
     */
    suspend fun getProviderAuthMethods(providerId: String): Result<List<ProviderAuthMethod>> = safeCall("getProviderAuthMethods") {
        getClient().get("$baseUrl/provider/$providerId/auth").body()
    }
    
    /**
     * Initiate OAuth authorization flow for a provider.
     * @param providerId The provider ID
     * @return Authorization URL and state for OAuth redirect
     */
    suspend fun authorizeProvider(providerId: String): Result<ProviderAuthAuthorization> = safeCallNoRetry("authorizeProvider") {
        getClient().post("$baseUrl/provider/$providerId/authorize").body()
    }
    
    /**
     * Handle OAuth callback after authorization.
     * @param providerId The provider ID
     * @param code OAuth authorization code
     * @param state OAuth state for verification
     */
    suspend fun handleOAuthCallback(
        providerId: String,
        code: String,
        state: String
    ): Result<Unit> = safeCallNoRetry("handleOAuthCallback") {
        getClient().post("$baseUrl/provider/$providerId/callback") {
            contentType(ContentType.Application.Json)
            setBody(OAuthCallbackRequest(code = code, state = state))
        }
        Unit
    }
    
    /**
     * Set provider authentication credentials manually (API key).
     * @param providerId The provider ID
     * @param credentials Authentication credentials (API key, etc.)
     */
    suspend fun setProviderAuth(
        providerId: String,
        credentials: ProviderCredentials
    ): Result<Unit> = safeCallNoRetry("setProviderAuth") {
        getClient().post("$baseUrl/provider/$providerId/auth") {
            contentType(ContentType.Application.Json)
            setBody(credentials)
        }
        Unit
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONFIG WRITE (Priority 1.3)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Update configuration settings.
     * @param update Partial configuration update
     */
    suspend fun updateConfig(update: ConfigUpdate): Result<ConfigResponse> = safeCallNoRetry("updateConfig") {
        getClient().patch("$baseUrl/config") {
            contentType(ContentType.Application.Json)
            setBody(update)
        }.body()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // COMMAND EXECUTION (Priority 1.4, 1.5)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Execute a slash command.
     * @param sessionId Session to execute command in
     * @param command Command name (without leading /)
     * @param arguments Optional command arguments as string
     * @param agent Optional agent to use
     */
    suspend fun executeCommand(
        sessionId: String,
        command: String,
        arguments: String? = null,
        agent: String? = null
    ): Result<Unit> = safeCallNoRetry("executeCommand") {
        getClient().post("$baseUrl/session/$sessionId/command") {
            contentType(ContentType.Application.Json)
            setBody(CommandExecutionRequest(
                command = command, 
                arguments = arguments,
                agent = agent
            ))
        }
        Unit
    }
    
    /**
     * Execute a shell command directly.
     * @param sessionId Session context
     * @param command Shell command to execute
     * @param agent Agent to execute with
     */
    suspend fun executeShell(
        sessionId: String,
        command: String,
        agent: String = "build"
    ): Result<String> = safeCallNoRetry("executeShell") {
        getClient().post("$baseUrl/session/$sessionId/shell") {
            contentType(ContentType.Application.Json)
            setBody(ShellExecutionRequest(command = command, agent = agent))
        }.body()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SESSION TODO (Priority 2.1)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get todos for a session.
     * @param sessionId Session ID
     */
    suspend fun getSessionTodos(sessionId: String): Result<List<Todo>> = safeCall("getSessionTodos") {
        getClient().get("$baseUrl/session/$sessionId/todo").body()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SESSION SHARING (Priority 2.2)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Share a session publicly.
     * @param sessionId Session to share
     * @return Updated session with shareID
     */
    suspend fun shareSession(sessionId: String): Result<Session> = safeCallNoRetry("shareSession") {
        getClient().post("$baseUrl/session/$sessionId/share").body()
    }
    
    /**
     * Unshare a session (revoke public access).
     * @param sessionId Session to unshare
     */
    suspend fun unshareSession(sessionId: String): Result<Session> = safeCallNoRetry("unshareSession") {
        getClient().delete("$baseUrl/session/$sessionId/share").body()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SESSION SUMMARIZATION (Priority 2.3)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Summarize a session (generate title, summary).
     * @param sessionId Session to summarize
     */
    suspend fun summarizeSession(sessionId: String): Result<Session> = safeCallNoRetry("summarizeSession") {
        getClient().post("$baseUrl/session/$sessionId/summarize").body()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SESSION INIT (Priority 2.4)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Initialize a session with system prompts and configuration.
     * @param sessionId Session to initialize
     * @param request Initialization parameters
     */
    suspend fun initSession(
        sessionId: String,
        request: InitSessionRequest
    ): Result<Session> = safeCallNoRetry("initSession") {
        getClient().post("$baseUrl/session/$sessionId/init") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PROJECT MANAGEMENT (Priority 2.5)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * List available projects.
     */
    suspend fun listProjects(): Result<List<Project>> = safeCall("listProjects") {
        getClient().get("$baseUrl/project").body()
    }
    
    /**
     * Get current active project.
     */
    suspend fun getCurrentProject(): Result<Project> = safeCall("getCurrentProject") {
        getClient().get("$baseUrl/project/current").body()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DYNAMIC MCP (Priority 2.6)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Add a new MCP server dynamically.
     * @param name Server name
     * @param config Server configuration
     */
    suspend fun addMcpServer(name: String, config: McpServerConfig): Result<Unit> = safeCallNoRetry("addMcpServer") {
        getClient().post("$baseUrl/mcp") {
            contentType(ContentType.Application.Json)
            setBody(McpConfigureRequest(name = name, config = config))
        }
        Unit
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PATH ENDPOINT (Priority 2.8)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get current working directory path.
     */
    suspend fun getCurrentPath(): Result<PathInfo> = safeCall("getCurrentPath") {
        getClient().get("$baseUrl/path").body()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INSTANCE DISPOSAL (Priority 2.9)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Dispose an OpenCode instance gracefully.
     */
    suspend fun disposeInstance(): Result<Unit> = safeCallNoRetry("disposeInstance") {
        getClient().post("$baseUrl/dispose")
        Unit
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // LOGGING ENDPOINT (Priority 2.10)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Send a log entry to the server.
     * @param entry Log entry to send
     */
    suspend fun sendLog(entry: LogEntry): Result<Unit> = safeCallNoRetry("sendLog") {
        getClient().post("$baseUrl/log") {
            contentType(ContentType.Application.Json)
            setBody(entry)
        }
        Unit
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // FULL TOOL LIST (Priority 2.11)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * @deprecated This endpoint returns HTML, not JSON. Use [getToolIds] instead.
     * The /tool endpoint is a frontend route, not a REST API endpoint.
     * For tool discovery, use getToolIds() which calls /experimental/tool/ids.
     * 
     * Forensic Audit Reference: OPENCODE_API_ANALYSIS.md - Endpoint Reality Map
     */
    @Deprecated(
        message = "The /tool endpoint returns HTML. Use getToolIds() for tool discovery.",
        replaceWith = ReplaceWith("getToolIds()"),
        level = DeprecationLevel.ERROR
    )
    suspend fun getTools(): Result<ToolList> = Result.failure(
        NetworkError.ServerError(
            statusCode = 406,
            message = "Deprecated: /tool returns HTML. Use getToolIds() instead."
        )
    )
    
    // Agents
    suspend fun getAgents(): Result<List<Agent>> = safeRequest("getAgents") {
        getClient().get("$baseUrl/agent")
    }
    
    // Tools
    suspend fun getToolIds(): Result<List<String>> = safeRequest("getToolIds") {
        getClient().get("$baseUrl/experimental/tool/ids")
    }
    
    // Slash Commands
    suspend fun getCommands(): Result<List<Command>> = safeRequest("getCommands") {
        getClient().get("$baseUrl/command")
    }
    
    // Formatters
    suspend fun getFormatters(): Result<List<FormatterStatus>> = safeRequest("getFormatters") {
        getClient().get("$baseUrl/formatter")
    }
    
    // LSP Status
    suspend fun getLspStatus(): Result<List<LspStatus>> = safeRequest("getLspStatus") {
        getClient().get("$baseUrl/lsp")
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

    // ===================================================================================
    // DEPRECATED: Git Operations Removed
    // ===================================================================================
    // Legacy git operations via port 4096 have been removed.
    // Use GitApiClient.kt (Port 4097) for all git operations.
    // ===================================================================================

    // MCP Operations
    /**
     * Get status of all MCP servers.
     * Returns a map of server name to McpServerStatus.
     */
    suspend fun getMcpStatus(directory: String? = null): Result<Map<String, McpServerStatus>> = safeRequest("getMcpStatus") {
        getClient().get("$baseUrl/mcp") {
            directory?.let { parameter("directory", it) }
        }
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
    /**
     * Safer request handler that parses body as text first to handle potential error objects
     * returned with 200 OK status, or non-200 responses with error bodies.
     * 
     * Includes Content-Type validation to detect routing errors (HTML returned instead of JSON).
     * Reference: OPENCODE_API_ANALYSIS.md - Header-Based Validation recommendation
     */
    private suspend inline fun <reified T> safeRequest(
        tag: String = "API",
        retryable: Boolean = true,
        crossinline block: suspend HttpClient.() -> HttpResponse
    ): Result<T> {
        val policy = if (retryable) retryPolicy else RetryPolicy.None
        return withRetry(policy, tag) {
            val response = getClient().block()
            val bodyText = response.bodyAsText()

            // 0. Content-Type validation: Detect HTML responses (routing errors)
            val contentType = response.contentType()
            if (contentType != null) {
                val isHtml = contentType.match(ContentType.Text.Html) ||
                    contentType.contentType == "text" && contentType.contentSubtype == "html"
                if (isHtml) {
                    Napier.e("$tag: Received HTML instead of JSON. Possible routing error.")
                    throw NetworkError.ServerError(
                        statusCode = 406, // Not Acceptable
                        message = "Routing error: Expected JSON but received HTML. " +
                            "Endpoint may be a frontend route, not a REST API."
                    )
                }
            }

            // 1. Check for non-success status code
            if (!response.status.isSuccess()) {
                val message = try {
                    val error = json.decodeFromString<ServerErrorResponse>(bodyText)
                    error.message ?: error.name ?: "Server Error: ${response.status}"
                } catch (e: Exception) {
                    "Server Error: ${response.status}"
                }
                throw NetworkError.ServerError(response.status.value, message)
            }

            // 2. Try to parse successful response
            try {
                if (T::class == Unit::class) {
                    return@withRetry Unit as T
                }
                json.decodeFromString<T>(bodyText)
            } catch (e: Exception) {
                // 3. If parsing fails, check if it's actually an error object disguised as 200 OK
                try {
                    val errorResponse = json.decodeFromString<ServerErrorResponse>(bodyText)
                    // If successful and has name/message, treat as error
                    if (errorResponse.name != null || errorResponse.message != null) {
                        throw NetworkError.ServerError(
                            statusCode = response.status.value,
                            message = errorResponse.message ?: errorResponse.name ?: "Unknown Server Error"
                        )
                    }
                    throw e // Re-throw original if it looked like an error but wasn't conclusive
                } catch (errorParseError: Exception) {
                    // It wasn't an error object either. Genuine parse error.
                    throw e
                }
            }
        }.mapError { error ->
            NetworkError.from(error)
        }
    }

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
