package com.mocca.app.api

import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * OpenCode API Client for REST endpoints. Uses [ApiExecutor] to access the current HttpClient
 * managed by ConnectionManager. Consumers never hold an HttpClient reference directly.
 */
class MoccaApiClient(
        private val api: ApiExecutor,
        private val retryPolicy: RetryPolicy = RetryPolicy.Default
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val shellMutex = Mutex()

    // Health
    suspend fun getHealth(): Result<AppInfo> = safeCall("getHealth") { get("global/health").body() }

    /**
     * @deprecated This endpoint returns HTML, not JSON. Use [getHealth] instead. The /app endpoint
     * is a frontend route, not a REST API endpoint.
     *
     * Forensic Audit Reference: OPENCODE_API_ANALYSIS.md - Endpoint Reality Map
     */
    @Deprecated(
            message = "The /app endpoint returns HTML. Use getHealth() for server status.",
            replaceWith = ReplaceWith("getHealth()"),
            level = DeprecationLevel.ERROR
    )
    suspend fun getAppInfo(): Result<AppInfo> =
            Result.failure(
                    NetworkError.ServerError(
                            statusCode = 406,
                            message = "Deprecated: /app returns HTML. Use getHealth() instead."
                    )
            )

    // Sessions
    suspend fun listSessions(): Result<List<Session>> =
            safeRequest("listSessions") { get("session") }

    suspend fun createSession(): Result<Session> =
            safeCallNoRetry("createSession") { post("session").body() }

    suspend fun getChildren(sessionId: String): Result<List<Session>> =
            safeCall("getChildren") { get("session/$sessionId/children").body() }

    suspend fun deleteSession(sessionId: String): Result<Unit> =
            safeCallNoRetry("deleteSession") { delete("session/$sessionId") }

    suspend fun abortSession(sessionId: String): Result<Boolean> =
            safeCallNoRetry("abortSession") { post("session/$sessionId/abort").body() }

    // Messages
    suspend fun getMessages(sessionId: String, limit: Int? = null): Result<List<MessageResponse>> =
            safeCall("getMessages") {
                Napier.v("[MoccaApiClient] Fetching messages for session: $sessionId (limit: $limit)")
                val response = get("session/$sessionId/message") {
                    if (limit != null) {
                        parameter("limit", limit)
                    }
                }
                val rawText = response.bodyAsText()
                // OOM guard: if the response is extremely large, parse cautiously.
                // A normal 100-message response is ~200-500KB. Anything over 20MB is pathological.
                if (rawText.length > 20_000_000) {
                    Napier.w("[MoccaApiClient] getMessages response very large (\${rawText.length} chars) for $sessionId")
                }
                json.decodeFromString<List<MessageResponse>>(rawText)
            }

    suspend fun chat(
            sessionId: String,
            modelId: String,
            providerId: String,
            parts: List<ChatPart>,
            mode: String? = null,
            variant: String? = null
    ): Result<AssistantMessageInfo> =
            safeCallNoRetry("chat") {
                post("session/$sessionId/message") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                    ChatRequest(
                                            modelID = modelId,
                                            providerID = providerId,
                                            parts = parts,
                                            mode = mode,
                                            variant = variant
                                    )
                            )
                        }
                        .body()
            }

    suspend fun chatAsync(
            sessionId: String,
            modelId: String,
            providerId: String,
            parts: List<ChatPart>,
            mode: String? = null,
            variant: String? = null
    ): Result<Unit> =
            safeCallNoRetry("chatAsync") {
                val response =
                        post("session/$sessionId/prompt_async") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                    ChatRequest(
                                            modelID = modelId,
                                            providerID = providerId,
                                            parts = parts,
                                            mode = mode,
                                            variant = variant
                                    )
                            )
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
    ): Result<Boolean> =
            safeCallNoRetry("respondToPermission") {
                post("session/$sessionId/permissions/$permissionId") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                    PermissionResponse(
                                            response = if (allow) "allow" else "deny",
                                            remember = remember
                                    )
                            )
                        }
                        .body()
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
    ): Result<Boolean> =
            safeCallNoRetry("replyToPermission") {
                post("permission/$requestId/reply") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                    PermissionReplyRequest(
                                            requestID = requestId,
                                            reply = reply.value,
                                            message = message
                                    )
                            )
                        }
                        .body()
            }

    /** List all pending permission requests. */
    suspend fun listPendingPermissions(): Result<List<PermissionRequest>> =
            safeCall("listPendingPermissions") { get("permission").body() }

    // Questions (matching OpenChamber SDK)
    /**
     * Reply to a question request.
     * @param requestId The question request ID
     * @param answers Answers as a list of string lists (one list per question, multiple selections
     * if multiple=true)
     */
    suspend fun replyToQuestion(requestId: String, answers: List<List<String>>): Result<Boolean> =
            safeCallNoRetry("replyToQuestion") {
                post("question/$requestId/reply") {
                            contentType(ContentType.Application.Json)
                            setBody(QuestionReplyRequest(requestID = requestId, answers = answers))
                        }
                        .body()
            }

    /** Reject a question request. */
    suspend fun rejectQuestion(requestId: String): Result<Boolean> =
            safeCallNoRetry("rejectQuestion") {
                post("question/$requestId/reject") {
                            contentType(ContentType.Application.Json)
                            setBody(QuestionRejectRequest(requestID = requestId))
                        }
                        .body()
            }

    /** List all pending question requests. */
    /** List all pending question requests. */
    suspend fun listPendingQuestions(): Result<List<QuestionRequest>> =
            safeCall("listPendingQuestions") { get("question").body() }

    // Session fork/revert (matching OpenChamber SDK)
    /**
     * Fork a session from a specific message. Creates a new session with messages up to the
     * specified message.
     * @param sessionId The session to fork from
     * @param messageId Optional message ID to fork from (null = current state)
     * @return The new forked session
     */
    suspend fun forkSession(sessionId: String, messageId: String? = null): Result<Session> =
            safeCallNoRetry("forkSession") {
                post("session/$sessionId/fork") {
                            contentType(ContentType.Application.Json)
                            setBody(ForkSessionRequest(messageID = messageId))
                        }
                        .body()
            }

    /**
     * Revert a session to a specific message. Messages after the specified message are hidden but
     * not deleted.
     * @param sessionId The session to revert
     * @param messageId The message ID to revert to
     * @param partId Optional part ID for partial revert
     * @return The updated session with revert state
     */
    suspend fun revertSession(
            sessionId: String,
            messageId: String,
            partId: String? = null
    ): Result<Session> =
            safeCallNoRetry("revertSession") {
                post("session/$sessionId/revert") {
                            contentType(ContentType.Application.Json)
                            setBody(RevertSessionRequest(messageID = messageId, partID = partId))
                        }
                        .body()
            }

    /**
     * Unrevert a session - restore all hidden messages.
     * @param sessionId The session to unrevert
     * @return The updated session without revert state
     */
    suspend fun unrevertSession(sessionId: String): Result<Session> =
            safeCallNoRetry("unrevertSession") { post("session/$sessionId/unrevert").body() }

    /** Update session title. */
    suspend fun updateSession(sessionId: String, title: String): Result<Session> =
            safeCallNoRetry("updateSession") {
                post("session/$sessionId") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("title" to title))
                        }
                        .body()
            }

    /** Get session status for all sessions (idle/busy/retry). */
    suspend fun getSessionStatus(): Result<Map<String, SessionStatusInfo>> =
            safeCall("getSessionStatus") { get("session/status").body() }

    // Config
    suspend fun getProviders(): Result<List<Provider>> =
            safeCall("getProviders") {
                val response: ProvidersResponse = get("config/providers").body()
                response.providers
            }

    suspend fun getModes(): Result<List<Mode>> =
            safeCall("getModes") {
                val config: ConfigResponse = get("config").body()
                config.modes
            }

    suspend fun getConfig(): Result<ConfigResponse> = safeCall("getConfig") { get("config").body() }

    // Provider info (from /provider endpoint - different from /config/providers)
    suspend fun getProviderInfo(): Result<ProviderResponse> =
            safeCall("getProviderInfo") { get("provider").body() }

    suspend fun getProvidersConfig(): Result<ProvidersConfig> =
            safeCall("getProvidersConfig") { get("config/providers").body() }

    // OAUTH / AUTHENTICATION (Priority 1.1, 1.2)


    /**
     * Get available authentication methods for a provider.
     * @param providerId The provider ID (e.g., "anthropic", "openai")
     * @return List of authentication methods (api_key, oauth, etc.)
     */
    suspend fun getProviderAuthMethods(providerId: String): Result<List<ProviderAuthMethod>> =
            safeCall("getProviderAuthMethods") { get("provider/$providerId/auth").body() }

    /**
     * Initiate OAuth authorization flow for a provider.
     * @param providerId The provider ID
     * @return Authorization URL and state for OAuth redirect
     */
    suspend fun authorizeProvider(providerId: String): Result<ProviderAuthAuthorization> =
            safeCallNoRetry("authorizeProvider") { post("provider/$providerId/authorize").body() }

    /**
     * Handle OAuth callback after authorization.
     * @param providerId The provider ID
     * @param code OAuth authorization code
     * @param state OAuth state for verification
     */
    suspend fun handleOAuthCallback(providerId: String, code: String, state: String): Result<Unit> =
            safeCallNoRetry("handleOAuthCallback") {
                post("provider/$providerId/callback") {
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
    ): Result<Unit> =
            safeCallNoRetry("setProviderAuth") {
                post("provider/$providerId/auth") {
                    contentType(ContentType.Application.Json)
                    setBody(credentials)
                }
                Unit
            }

    // CONFIG WRITE (Priority 1.3)


    /**
     * Update configuration settings.
     * @param update Partial configuration update
     */
    suspend fun updateConfig(update: ConfigUpdate): Result<ConfigResponse> =
            safeCallNoRetry("updateConfig") {
                patch("config") {
                            contentType(ContentType.Application.Json)
                            setBody(update)
                        }
                        .body()
            }

    // COMMAND EXECUTION (Priority 1.4, 1.5)


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
    ): Result<Unit> =
            safeCallNoRetry("executeCommand") {
                post("session/$sessionId/command") {
                    contentType(ContentType.Application.Json)
                    setBody(
                            CommandExecutionRequest(
                                    command = command,
                                    arguments = arguments,
                                    agent = agent
                            )
                    )
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
    ): Result<String> =
        shellMutex.withLock {
            safeCallNoRetry("executeShell") {
                val response =
                        post("session/$sessionId/shell") {
                            contentType(ContentType.Application.Json)
                            setBody(ShellExecutionRequest(command = command, agent = agent))
                        }
                val rawText = response.bodyAsText()
                Napier.v("[MoccaApiClient] executeShell raw response (${rawText.length} chars)")

                try {
                    parseShellOutput(rawText)
                } catch (e: Exception) {
                    Napier.w("[MoccaApiClient] executeShell: Error parsing response, using raw text: ${e.message}")
                    rawText
                }
            }
        }

    /** Helper to extract output from shell response (SSE or JSON). */
    private fun parseShellOutput(rawText: String): String {
        val lines = rawText.split('\n')
        val isSse = lines.any { it.trim().startsWith("data: ") }
        
        return if (isSse) {
            lines.reversed().asSequence()
                .map { it.trim() }
                .filter { it.startsWith("data: ") }
                .map { it.removePrefix("data: ").trim() }
                .filter { it.isNotEmpty() && it != "[DONE]" }
                .mapNotNull { extractOutputFromJson(it) }
                .firstOrNull() ?: rawText
        } else {
            extractOutputFromJson(rawText) ?: rawText
        }
    }

    /** Helper to extract output from a parsed JSON string. */
    private fun extractOutputFromJson(jsonStr: String): String? {
        return try {
            val jsonObj = json.parseToJsonElement(jsonStr)
            val parts = jsonObj.jsonObject["parts"]?.jsonArray ?: return null
            // Find the last part with output (the tool result)
            for (i in parts.indices.reversed()) {
                val part = parts[i].jsonObject
                val state = part["state"]?.jsonObject
                val output = state?.get("output")?.jsonPrimitive?.contentOrNull
                    ?: state?.get("metadata")?.jsonObject?.get("output")?.jsonPrimitive?.contentOrNull
                if (output != null) return output
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // SESSION TODO (Priority 2.1)


    /**
     * Get todos for a session.
     * @param sessionId Session ID
     */
    suspend fun getSessionTodos(sessionId: String): Result<List<Todo>> =
            safeCall("getSessionTodos") { get("session/$sessionId/todo").body() }

    // SESSION SHARING (Priority 2.2)


    /**
     * Share a session publicly.
     * @param sessionId Session to share
     * @return Updated session with shareID
     */
    suspend fun shareSession(sessionId: String): Result<Session> =
            safeCallNoRetry("shareSession") { post("session/$sessionId/share").body() }

    /**
     * Unshare a session (revoke public access).
     * @param sessionId Session to unshare
     */
    suspend fun unshareSession(sessionId: String): Result<Session> =
            safeCallNoRetry("unshareSession") { delete("session/$sessionId/share").body() }

    // SESSION SUMMARIZATION (Priority 2.3)


    /**
     * Summarize a session (generate title, summary).
     * @param sessionId Session to summarize
     */
    suspend fun summarizeSession(sessionId: String): Result<Session> =
            safeCallNoRetry("summarizeSession") { post("session/$sessionId/summarize").body() }

    // SESSION INIT (Priority 2.4)


    /**
     * Initialize a session with system prompts and configuration.
     * @param sessionId Session to initialize
     * @param request Initialization parameters
     */
    suspend fun initSession(sessionId: String, request: InitSessionRequest): Result<Session> =
            safeCallNoRetry("initSession") {
                post("session/$sessionId/init") {
                            contentType(ContentType.Application.Json)
                            setBody(request)
                        }
                        .body()
            }

    // PROJECT MANAGEMENT (Priority 2.5)


    /** List available projects. */
    suspend fun listProjects(): Result<List<Project>> =
            safeCall("listProjects") { get("project").body() }

    /** Get current active project. */
    suspend fun getCurrentProject(): Result<Project> =
            safeCall("getCurrentProject") { get("project/current").body() }

    // DYNAMIC MCP (Priority 2.6)


    /**
     * Add a new MCP server dynamically.
     * @param name Server name
     * @param config Server configuration
     */
    suspend fun addMcpServer(name: String, config: McpServerConfig): Result<Unit> =
            safeCallNoRetry("addMcpServer") {
                post("mcp") {
                    contentType(ContentType.Application.Json)
                    setBody(McpConfigureRequest(name = name, config = config))
                }
                Unit
            }

    // PATH ENDPOINT (Priority 2.8)


    /** Get current working directory path. */
    suspend fun getCurrentPath(): Result<PathInfo> =
            safeCall("getCurrentPath") { get("path").body() }

    // INSTANCE DISPOSAL (Priority 2.9)


    /** Dispose an OpenCode instance gracefully. */
    suspend fun disposeInstance(): Result<Unit> =
            safeCallNoRetry("disposeInstance") {
                post("dispose")
                Unit
            }

    // LOGGING ENDPOINT (Priority 2.10)


    /**
     * Send a log entry to the server.
     * @param entry Log entry to send
     */
    suspend fun sendLog(entry: LogEntry): Result<Unit> =
            safeCallNoRetry("sendLog") {
                post("log") {
                    contentType(ContentType.Application.Json)
                    setBody(entry)
                }
                Unit
            }

    // FULL TOOL LIST (Priority 2.11)


    /**
     * @deprecated This endpoint returns HTML, not JSON. Use [getToolIds] instead. The /tool
     * endpoint is a frontend route, not a REST API endpoint. For tool discovery, use getToolIds()
     * which calls /experimental/tool/ids.
     *
     * Forensic Audit Reference: OPENCODE_API_ANALYSIS.md - Endpoint Reality Map
     */
    @Deprecated(
            message = "The /tool endpoint returns HTML. Use getToolIds() for tool discovery.",
            replaceWith = ReplaceWith("getToolIds()"),
            level = DeprecationLevel.ERROR
    )
    suspend fun getTools(): Result<ToolList> =
            Result.failure(
                    NetworkError.ServerError(
                            statusCode = 406,
                            message = "Deprecated: /tool returns HTML. Use getToolIds() instead."
                    )
            )

    // Agents
    suspend fun getAgents(): Result<List<Agent>> = safeRequest("getAgents") { get("agent") }

    // Tools
    suspend fun getToolIds(): Result<List<String>> =
            safeRequest("getToolIds") { get("experimental/tool/ids") }

    suspend fun getToolSchema(id: String): Result<ToolSchema> =
            safeRequest("getToolSchema") { get("experimental/tool/$id") }

    // Slash Commands
    suspend fun getCommands(): Result<List<Command>> = safeRequest("getCommands") { get("command") }

    // VCS Info
    suspend fun getVcsInfo(): Result<VcsInfo> = safeCall("getVcsInfo") { get("vcs").body() }

    // Session Diffs
    suspend fun getSessionDiffs(sessionId: String): Result<List<FileDiff>> =
            safeCall("getSessionDiffs") { get("session/$sessionId/diff").body() }

    // Files
    suspend fun listFiles(path: String = "."): Result<List<FileInfo>> =
            safeCall("listFiles") { get("file") { parameter("path", path.ifEmpty { "." }) }.body() }

    suspend fun getFileContent(path: String): Result<FileContent> =
            safeCall("getFileContent") { get("file/content") { parameter("path", path) }.body() }

    suspend fun getFileStatus(path: String): Result<FileStatus> =
            safeCall("getFileStatus") { get("file/status") { parameter("path", path) }.body() }

    /**
     * Update file content.
     * @param path Path to the file
     * @param content New file content
     * @return Result indicating success or failure
     */
    suspend fun updateFile(path: String, content: String): Result<Unit> =
            safeCallNoRetry("updateFile") {
                post("file") {
                    contentType(ContentType.Application.Json)
                    setBody(FileUpdateRequest(path = path, content = content))
                }
                Unit
            }

    // Search
    suspend fun searchText(query: String, path: String = ""): Result<List<ApiSearchResult>> =
            safeCall("searchText") {
                get("find") {
                            parameter("query", query)
                            if (path.isNotEmpty()) {
                                parameter("path", path)
                            }
                        }
                        .body()
            }

    /**
     * Grep-style content search backed by the existing /find endpoint.
     * Context enrichment is handled client-side after matches are returned.
     */
    suspend fun searchGrep(
            query: String,
            path: String = ""
    ): Result<List<ApiSearchResult>> =
            safeCall("searchGrep") {
                get("find") {
                            parameter("query", query)
                            if (path.isNotEmpty()) {
                                parameter("path", path)
                            }
                        }
                        .body()
            }

    suspend fun findFiles(pattern: String): Result<List<String>> =
            safeCall("findFiles") { get("find/file") { parameter("pattern", pattern) }.body() }

    suspend fun findSymbols(query: String): Result<List<ApiSymbolResult>> =
            safeCall("findSymbols") { get("find/symbol") { parameter("query", query) }.body() }

    // MCP Operations
    /** Get status of all MCP servers. Returns a map of server name to McpServerStatus. */
    suspend fun getMcpStatus(directory: String? = null): Result<Map<String, McpServerStatus>> =
            safeRequest("getMcpStatus") {
                get("mcp") { directory?.let { parameter("directory", it) } }
            }

    /** Connect to an MCP server by name. */
    suspend fun connectMcp(name: String, directory: String? = null): Result<Unit> =
            safeCallNoRetry("connectMcp") {
                post("mcp/connect") {
                    contentType(ContentType.Application.Json)
                    setBody(McpConnectRequest(name = name, directory = directory))
                }
                Unit
            }

    /** Disconnect from an MCP server by name. */
    suspend fun disconnectMcp(name: String, directory: String? = null): Result<Unit> =
            safeCallNoRetry("disconnectMcp") {
                post("mcp/disconnect") {
                    contentType(ContentType.Application.Json)
                    setBody(McpConnectRequest(name = name, directory = directory))
                }
                Unit
            }

    /** Configure/update an MCP server. */
    suspend fun configureMcp(name: String, config: McpServerConfig): Result<Unit> =
            safeCallNoRetry("configureMcp") {
                post("mcp") {
                    contentType(ContentType.Application.Json)
                    setBody(McpConfigureRequest(name = name, config = config))
                }
                Unit
            }

    // Terminal
    suspend fun listTerminals(): Result<List<Terminal>> =
            safeCall("listTerminals") { get("terminal").body() }

    suspend fun createTerminal(): Result<Terminal> =
            safeCallNoRetry("createTerminal") { post("terminal").body() }

    suspend fun resizeTerminal(id: String, cols: Int, rows: Int): Result<Unit> =
            safeCallNoRetry("resizeTerminal") {
                post("terminal/$id/resize") {
                    contentType(ContentType.Application.Json)
                    setBody(TerminalResizeRequest(cols, rows))
                }
            }

    /**
     * Connect to a terminal via WebSocket. Uses ApiExecutor to access the current HttpClient for
     * WebSocket connection.
     */
    suspend fun connectToTerminal(
            id: String,
            block: suspend DefaultClientWebSocketSession.() -> Unit
    ) {
        api.execute { webSocket("terminal/$id/socket") { block() } }
    }

    // GLOBAL CONFIG (GET/PATCH /global/config)


    /** Get global (cross-instance) application configuration. */
    suspend fun getGlobalConfig(): Result<GlobalAppConfig> =
        safeCall("getGlobalConfig") { get("global/config").body() }

    /** Update global (cross-instance) application configuration. */
    suspend fun updateGlobalConfig(update: AppConfigUpdate): Result<GlobalAppConfig> =
        safeCallNoRetry("updateGlobalConfig") {
            patch("global/config") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }.body()
        }

    // PROVIDER AUTH REMOVAL


    /** Remove stored authentication credentials for a provider. */
    suspend fun deleteProviderAuth(providerId: String): Result<Unit> =
        safeCallNoRetry("deleteProviderAuth") {
            delete("provider/$providerId/auth")
            Unit
        }

    // SESSION MESSAGE MANAGEMENT


    /** Delete a message from a session. */
    suspend fun deleteMessage(sessionId: String, messageId: String): Result<Unit> =
        safeCallNoRetry("deleteMessage") {
            delete("session/$sessionId/message/$messageId")
            Unit
        }

    /** Delete a specific part from a session message. */
    suspend fun deleteMessagePart(sessionId: String, messageId: String, partId: String): Result<Unit> =
        safeCallNoRetry("deleteMessagePart") {
            delete("session/$sessionId/message/$messageId/part/$partId")
            Unit
        }

    /** Patch/edit the content of a specific message part. */
    suspend fun patchMessagePart(
        sessionId: String,
        messageId: String,
        partId: String,
        content: String
    ): Result<Unit> =
        safeCallNoRetry("patchMessagePart") {
            patch("session/$sessionId/message/$messageId/part/$partId") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("content" to content))
            }
            Unit
        }

    // PROJECT UPDATE


    /** Update a project's settings. */
    suspend fun updateProject(projectId: String, update: ProjectUpdateRequest): Result<Project> =
        safeCallNoRetry("updateProject") {
            patch("project/$projectId") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }.body()
        }

    // TERMINAL UPDATE / DELETE


    /** Update terminal properties (title, dimensions). */
    suspend fun updateTerminal(id: String, update: PtyUpdateRequest): Result<Terminal> =
        safeCallNoRetry("updateTerminal") {
            patch("terminal/$id") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }.body()
        }

    /** Delete (close) a terminal session. */
    suspend fun deleteTerminal(id: String): Result<Unit> =
        safeCallNoRetry("deleteTerminal") {
            delete("terminal/$id")
            Unit
        }

    // MCP OAUTH


    /** Start MCP OAuth authentication flow for a server. */
    suspend fun startMcpAuth(name: String, request: McpAuthRequest): Result<McpOAuthState> =
        safeCallNoRetry("startMcpAuth") {
            post("mcp/auth/$name") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

    /** Handle MCP OAuth callback after authorization redirect. */
    suspend fun handleMcpAuthCallback(
        name: String,
        request: McpAuthCallbackRequest
    ): Result<Unit> =
        safeCallNoRetry("handleMcpAuthCallback") {
            post("mcp/auth/$name/callback") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Unit
        }

    /** Delete an MCP server configuration. */
    suspend fun deleteMcpServer(name: String): Result<Unit> =
        safeCallNoRetry("deleteMcpServer") {
            delete("mcp/$name")
            Unit
        }

    /** Add a new MCP server using a full server configuration request. */
    suspend fun addMcpServerConfig(request: McpAddServerRequest): Result<Unit> =
        safeCallNoRetry("addMcpServerConfig") {
            post("mcp") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Unit
        }

    // MCP RESOURCES


    /** List resources exposed by an MCP server. */
    suspend fun listMcpResources(name: String): Result<List<McpResource>> =
        safeCall("listMcpResources") { get("mcp/$name/resource").body() }

    /** Read the content of a specific MCP resource. */
    suspend fun readMcpResource(name: String, uri: String): Result<McpResourceContent> =
        safeCall("readMcpResource") { get("mcp/$name/resource/$uri").body() }

    // Helpers
    private suspend inline fun <reified T> safeRequest(
            tag: String = "API",
            retryable: Boolean = true,
            crossinline block: suspend HttpClient.() -> HttpResponse
    ): Result<T> {
        val policy = if (retryable) retryPolicy else RetryPolicy.None
        return withRetry(policy, tag) {
            val response = api.execute { block() }
            val bodyText = response.bodyAsText()

            // 0. Content-Type validation: Detect HTML responses (routing errors)
            val contentType = response.contentType()
            if (contentType != null) {
                val isHtml = contentType.match(ContentType.Text.Html) ||
                             (contentType.contentType == "text" && contentType.contentSubtype == "html")
                if (isHtml) {
                    Napier.e("$tag: Received HTML instead of JSON. Possible routing error.")
                    throw NetworkError.ServerError(
                            statusCode = 406, // Not Acceptable
                            message = "Routing error: Expected JSON but received HTML. Endpoint may be a frontend route."
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
            if (T::class == Unit::class) return@withRetry Unit as T
            
            try {
                json.decodeFromString<T>(bodyText)
            } catch (e: Exception) {
                // 3. If parsing fails, check if it's an error object in a 200 OK
                try {
                    val errorResponse = json.decodeFromString<ServerErrorResponse>(bodyText)
                    if (errorResponse.name != null || errorResponse.message != null) {
                        throw NetworkError.ServerError(
                                statusCode = response.status.value,
                                message = errorResponse.message ?: errorResponse.name ?: "Unknown Error"
                        )
                    }
                    throw e
                } catch (parseErr: Exception) {
                    throw e
                }
            }
        }.mapError { NetworkError.from(it) }
    }

    private suspend inline fun <reified T> safeCall(
            tag: String = "API",
            retryable: Boolean = true,
            crossinline block: suspend HttpClient.() -> T
    ): Result<T> {
        val policy = if (retryable) retryPolicy else RetryPolicy.None
        return withRetry(policy, tag) { 
            api.execute { block() } 
        }.mapError { NetworkError.from(it) }
    }

    private suspend inline fun <reified T> safeCallNoRetry(
            tag: String = "API",
            crossinline block: suspend HttpClient.() -> T
    ): Result<T> = safeCall(tag, retryable = false, block)

    private inline fun <T, E : Throwable> Result<T>.mapError(
            transform: (Throwable) -> E
    ): Result<T> {
        return fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.failure(transform(it)) }
        )
    }
}
