package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Repository for Session management with offline-first caching.
 */
class SessionRepository(
    private val apiClient: MoccaApiClient,
    private val localCache: LocalCache
) {
    // Default model/provider - should be configurable from settings
    // These will be populated from /config/providers
    private var defaultModelId: String = "" // Loaded dynamically from loadDefaultConfig()
    private var defaultProviderId: String = "anthropic"
    private var defaultMode: String = "build"
    
    /**
     * Get all sessions with offline-first strategy.
     * Returns cached data immediately, then refreshes from network.
     */
    fun getSessions(): Flow<Resource<List<Session>>> = flow {
        emit(Resource.Loading())

        // 1. Return cached data first
        val cached = localCache.getAllSessions()
        if (cached.isNotEmpty()) {
            emit(Resource.Loading(cached))
        }

        // 2. Fetch fresh data from network
        val result = apiClient.listSessions()
        result.fold(
            onSuccess = { sessions ->
                // Cache the fresh data
                sessions.forEach { session ->
                    localCache.insertSession(session)
                }
                emit(Resource.Success(sessions))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch sessions", error)
                emit(Resource.Error(error.message ?: "Failed to fetch sessions", cached))
            }
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Get a single session by ID.
     */
    suspend fun getSession(sessionId: String): Resource<Session> = withContext(Dispatchers.IO) {
        // Check cache first
        val cached = localCache.getSession(sessionId)

        apiClient.listSessions().fold(
            onSuccess = { sessions ->
                val session = sessions.find { it.id == sessionId }
                if (session != null) {
                    localCache.insertSession(session)
                    Resource.Success(session)
                } else {
                    cached?.let { Resource.Success(it) }
                        ?: Resource.Error("Session not found")
                }
            },
            onFailure = { error ->
                cached?.let { Resource.Error(error.message ?: "Error", it) }
                    ?: Resource.Error(error.message ?: "Failed to fetch session")
            }
        )
    }

    /**
     * Create a new session.
     */
    suspend fun createSession(): Result<Session> = withContext(Dispatchers.IO) {
        apiClient.createSession().also { result ->
            result.onSuccess { session ->
                localCache.insertSession(session)
            }
        }
    }

    /**
     * Get children for a session.
     */
    suspend fun getChildren(sessionId: String): Result<List<Session>> {
        return apiClient.getChildren(sessionId)
    }

    /**
     * Delete a session.
     */
    suspend fun deleteSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        apiClient.deleteSession(sessionId).also { result ->
            result.onSuccess {
                localCache.deleteSession(sessionId)
            }
        }
    }
    
    /**
     * Delete all sessions (clear history).
     * Deletes from server first, then clears local cache.
     */
    suspend fun deleteAllSessions(): Result<Unit> = withContext(Dispatchers.IO) {
        // Get all sessions and delete each from server
        val sessions = localCache.getAllSessions()
        val errors = mutableListOf<Throwable>()
        
        sessions.forEach { session ->
            apiClient.deleteSession(session.id).onFailure { error ->
                Napier.w("Failed to delete session ${session.id}: ${error.message}")
                errors.add(error)
            }
        }
        
        // Always clear local cache, even if some server deletes failed
        localCache.deleteAllSessions()
        
        if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to delete ${errors.size} sessions"))
        }
    }

    /**
     * Abort a running session.
     */
    suspend fun abortSession(sessionId: String): Result<Boolean> {
        return apiClient.abortSession(sessionId)
    }

    /**
     * Get recently used models.
     */
    suspend fun getRecentModels(): List<RecentModel> {
        return localCache.getRecentModels()
    }
    
    /**
     * Add or update a recent model.
     */
    suspend fun addRecentModel(providerId: String, modelId: String) {
        val now = Clock.System.now().toEpochMilliseconds() // or System.currentTimeMillis() if using java.lang.System
        // Clock is imported from kotlinx.datetime
        localCache.insertRecentModel(RecentModel(providerId, modelId, now))
    }

    /**
     * Get messages for a session with caching.
     */
    fun getMessages(sessionId: String): Flow<Resource<List<Message>>> = flow {
        emit(Resource.Loading())

        // 1. Return cached messages first
        val cached = localCache.getMessages(sessionId)
        if (cached.isNotEmpty()) {
            emit(Resource.Loading(cached))
        }

        // 2. Fetch fresh messages from network
        val result = apiClient.getMessages(sessionId)
        result.fold(
            onSuccess = { responses ->
                // Convert MessageResponse to Message for UI
                val messages = responses.map { Message.fromResponse(it) }
                // Cache the fresh messages
                messages.forEach { message ->
                    localCache.insertMessage(message)
                }
                emit(Resource.Success(messages))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch messages", error)
                emit(Resource.Error(error.message ?: "Failed to fetch messages", cached))
            }
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Send a message to a session (async - uses SSE for streaming response).
     * Returns immediately, use SSE events to track progress.
     * @param sessionId The session ID
     * @param text The message text
     * @param mode Optional mode override (uses default if null)
     * @param attachments Optional file attachments
     * @param modelId Optional model override
     * @param providerId Optional provider override
     */
    suspend fun sendMessageAsync(
        sessionId: String,
        text: String,
        mode: String? = null,
        attachments: List<AttachedFile> = emptyList(),
        modelId: String? = null,
        providerId: String? = null
    ): Result<Unit> {
        // Build parts list: text + any file attachments
        val parts = buildList {
            add(ChatPart.Text(text = text))
            attachments.forEach { file ->
                add(file.toChatPart())
            }
        }
        return apiClient.chatAsync(
            sessionId = sessionId,
            modelId = modelId ?: defaultModelId,
            providerId = providerId ?: defaultProviderId,
            parts = parts,
            mode = mode ?: defaultMode
        )
    }

    /**
     * Send a message to a session (blocking - waits for response).
     * Use this if you need the response immediately.
     */
    suspend fun sendMessage(sessionId: String, text: String): Result<Message> {
        val parts = listOf(ChatPart.Text(text = text))
        return apiClient.chat(
            sessionId = sessionId,
            modelId = defaultModelId,
            providerId = defaultProviderId,
            parts = parts,
            mode = defaultMode
        ).map { info ->
            // Convert to Message (without parts, since chat returns just info)
            Message(
                id = info.id,
                sessionId = info.sessionID,
                role = MessageRole.ASSISTANT,
                parts = emptyList(), // Parts come via SSE
                createdAt = info.time?.created ?: Clock.System.now().toEpochMilliseconds(),
                model = info.modelID,
                cost = info.cost
            )
        }
    }
    
    /**
     * Send a user message to session, optimistically add to local list.
     * This is for UI purposes - creates a local user message.
     */
    fun createLocalUserMessage(sessionId: String, text: String): Message {
        val now = Clock.System.now().toEpochMilliseconds()
        return Message(
            id = "local-$now",
            sessionId = sessionId,
            role = MessageRole.USER,
            parts = listOf(MessagePart.Text(text)),
            createdAt = now
        )
    }
    
    /**
     * Respond to a permission request (tool approval) - legacy API.
     */
    suspend fun respondToPermission(
        sessionId: String,
        permissionId: String,
        allow: Boolean,
        remember: Boolean = false
    ): Result<Boolean> {
        return apiClient.respondToPermission(sessionId, permissionId, allow, remember)
    }
    
    /**
     * Reply to a permission request using the new API.
     * @param requestId The permission request ID
     * @param response One of: once, always, reject
     * @param message Optional rejection message
     */
    suspend fun replyToPermission(
        requestId: String,
        response: PermissionResponseType,
        message: String? = null
    ): Result<Boolean> {
        return apiClient.replyToPermission(requestId, response, message)
    }
    
    /**
     * Get all pending permission requests.
     */
    suspend fun listPendingPermissions(): Result<List<PermissionRequest>> {
        return apiClient.listPendingPermissions()
    }
    
    /**
     * Reply to a question request.
     * @param requestId The question request ID
     * @param answers Answers as list of string lists
     */
    suspend fun replyToQuestion(
        requestId: String,
        answers: List<List<String>>
    ): Result<Boolean> {
        return apiClient.replyToQuestion(requestId, answers)
    }
    
    /**
     * Reply to a single-answer question.
     */
    suspend fun replyToQuestionSingle(
        requestId: String,
        answer: String
    ): Result<Boolean> {
        return replyToQuestion(requestId, listOf(listOf(answer)))
    }
    
    /**
     * Reject a question request.
     */
    suspend fun rejectQuestion(requestId: String): Result<Boolean> {
        return apiClient.rejectQuestion(requestId)
    }
    
    /**
     * Get all pending question requests.
     */
    suspend fun listPendingQuestions(): Result<List<QuestionRequest>> {
        return apiClient.listPendingQuestions()
    }
    
    /**
     * Fork a session from a specific message.
     * Creates a new session with messages up to the specified message.
     * @param sessionId The session to fork from
     * @param messageId The message ID to fork from (null = current state)
     * @return The new forked session
     */
    suspend fun forkFromMessage(
        sessionId: String,
        messageId: String? = null
    ): Result<Session> = withContext(Dispatchers.IO) {
        apiClient.forkSession(sessionId, messageId).also { result ->
            result.onSuccess { newSession ->
                localCache.insertSession(newSession)
                Napier.i("Session forked: ${sessionId} -> ${newSession.id}")
            }
        }
    }
    
    /**
     * Revert a session to a specific message.
     * Messages after the specified message are hidden but not deleted.
     * @param sessionId The session to revert
     * @param messageId The message ID to revert to
     * @return The updated session with revert state
     */
    suspend fun revertToMessage(
        sessionId: String,
        messageId: String
    ): Result<Session> = withContext(Dispatchers.IO) {
        apiClient.revertSession(sessionId, messageId).also { result ->
            result.onSuccess { updatedSession ->
                localCache.insertSession(updatedSession)
                Napier.i("Session reverted: ${sessionId} to ${messageId}")
            }
        }
    }
    
    /**
     * Unrevert a session - restore all hidden messages.
     * @param sessionId The session to unrevert
     * @return The updated session without revert state
     */
    suspend fun unrevertSession(sessionId: String): Result<Session> = withContext(Dispatchers.IO) {
        apiClient.unrevertSession(sessionId).also { result ->
            result.onSuccess { updatedSession ->
                localCache.insertSession(updatedSession)
                Napier.i("Session unreverted: ${sessionId}")
            }
        }
    }
    
    /**
     * Update session title.
     */
    suspend fun updateSessionTitle(sessionId: String, title: String): Result<Session> = withContext(Dispatchers.IO) {
        apiClient.updateSession(sessionId, title).also { result ->
            result.onSuccess { updatedSession ->
                localCache.insertSession(updatedSession)
            }
        }
    }
    
    /**
     * Get session status for all sessions.
     */
    suspend fun getSessionStatus(): Result<Map<String, SessionStatusInfo>> {
        return apiClient.getSessionStatus()
    }
    
    /**
     * Force refresh sessions from network.
     */
    suspend fun refreshSessions() {
        apiClient.listSessions().fold(
            onSuccess = { sessions ->
                sessions.forEach { session ->
                    localCache.insertSession(session)
                }
                Napier.i("Refreshed ${sessions.size} sessions from server")
            },
            onFailure = { error ->
                Napier.e("Failed to refresh sessions", error)
            }
        )
    }
    
    /**
     * Load and cache the default model/provider from server config.
     */
    suspend fun loadDefaultConfig() {
        // 1. Try to load global config first (contains default model)
        apiClient.getConfig().onSuccess { config ->
            // Parse "provider/model" format (e.g. "anthropic/claude-sonnet-4-5")
            config.model?.let { modelStr ->
                val parts = modelStr.split("/", limit = 2)
                if (parts.size == 2) {
                    defaultProviderId = parts[0]
                    defaultModelId = parts[1]
                    Napier.i("Default config loaded from server: $defaultProviderId / $defaultModelId")
                }
            }

            // Set default mode
            config.modes.firstOrNull()?.let { mode ->
                defaultMode = mode.id
                Napier.i("Default mode: $defaultMode")
            }
        }.onFailure { error ->
            Napier.w("Failed to load server config: ${error.message}")
        }

        // 2. Fallback: If defaults still empty, try to pick first available from providers list
        if (defaultModelId.isEmpty()) {
            apiClient.getProviders().onSuccess { providers ->
                providers.firstOrNull { it.models.isNotEmpty() }?.let { provider ->
                    defaultProviderId = provider.id
                    defaultModelId = provider.models.values.first().id
                    Napier.i("Fallback default config: $defaultProviderId / $defaultModelId")
                }
            }
        }
    }
    
    /**
     * Set the default model and provider for chat.
     */
    fun setDefaultModel(modelId: String, providerId: String) {
        defaultModelId = modelId
        defaultProviderId = providerId
    }
    
    /**
     * Set the default mode for chat.
     */
    fun setDefaultMode(mode: String) {
        defaultMode = mode
    }
    
    /**
     * Get available providers with their models.
     */
    suspend fun getProviderInfo(): Result<ProviderResponse> {
        return apiClient.getProviderInfo()
    }
    
    /**
     * Get available modes.
     */
    suspend fun getModes(): Result<List<Mode>> {
        return apiClient.getModes()
    }
    
    /**
     * Get current default model and provider IDs.
     */
    fun getDefaultModelProvider(): Pair<String, String> {
        return Pair(defaultModelId, defaultProviderId)
    }
    
    /**
     * Get current default mode.
     */
    fun getDefaultMode(): String {
        return defaultMode
    }
    
    /**
     * Get current model/agent display info.
     * Returns pair of (modelName, agentName) for UI display.
     */
    fun getCurrentModelInfo(): Pair<String, String>? {
        return if (defaultModelId.isNotEmpty()) {
            val modelDisplay = defaultModelId.uppercase().replace("-", " ").take(30)
            val agentDisplay = defaultMode.uppercase()
            Pair(modelDisplay, agentDisplay)
        } else {
            null
        }
    }
    
    /**
     * Get session diffs (file changes made during session).
     * @param sessionId The session ID
     * @return Flow of file diffs for the session
     */
    fun getSessionDiffs(sessionId: String): Flow<Resource<List<FileDiff>>> = flow {
        emit(Resource.Loading())
        
        apiClient.getSessionDiffs(sessionId).fold(
            onSuccess = { diffs ->
                emit(Resource.Success(diffs))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch session diffs", error)
                emit(Resource.Error(error.message ?: "Failed to fetch session diffs"))
            }
        )
    }.flowOn(Dispatchers.IO)

    // ═══════════════════════════════════════════════════════════════════════
    // SESSION TODO LIST (Priority 2.1)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get todos for a session.
     */
    fun getSessionTodos(sessionId: String): Flow<Resource<List<Todo>>> = flow {
        emit(Resource.Loading())
        apiClient.getSessionTodos(sessionId).fold(
            onSuccess = { todos ->
                emit(Resource.Success(todos))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch todos for session $sessionId", error)
                emit(Resource.Error(error.message ?: "Failed to fetch todos"))
            }
        )
    }.flowOn(Dispatchers.IO)

    // ═══════════════════════════════════════════════════════════════════════
    // SESSION SHARING (Priority 2.2)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Share a session publicly.
     */
    suspend fun shareSession(sessionId: String): Resource<Session> = withContext(Dispatchers.IO) {
        apiClient.shareSession(sessionId).fold(
            onSuccess = { session ->
                localCache.insertSession(session)
                Napier.i("Session $sessionId shared successfully")
                Resource.Success(session)
            },
            onFailure = { error ->
                Napier.e("Failed to share session $sessionId", error)
                Resource.Error(error.message ?: "Failed to share session")
            }
        )
    }

    /**
     * Unshare a session (revoke public access).
     */
    suspend fun unshareSession(sessionId: String): Resource<Session> = withContext(Dispatchers.IO) {
        apiClient.unshareSession(sessionId).fold(
            onSuccess = { session ->
                localCache.insertSession(session)
                Napier.i("Session $sessionId unshared successfully")
                Resource.Success(session)
            },
            onFailure = { error ->
                Napier.e("Failed to unshare session $sessionId", error)
                Resource.Error(error.message ?: "Failed to unshare session")
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SESSION SUMMARIZATION (Priority 2.3)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Summarize a session (generate title and summary).
     */
    suspend fun summarizeSession(sessionId: String): Resource<Session> = withContext(Dispatchers.IO) {
        apiClient.summarizeSession(sessionId).fold(
            onSuccess = { session ->
                localCache.insertSession(session)
                Napier.i("Session $sessionId summarized successfully")
                Resource.Success(session)
            },
            onFailure = { error ->
                Napier.e("Failed to summarize session $sessionId", error)
                Resource.Error(error.message ?: "Failed to summarize session")
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SESSION INIT (Priority 2.4)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Initialize a session with system prompts and configuration.
     */
    suspend fun initializeProject(
        sessionId: String,
        messageId: String,
        providerId: String,
        modelId: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        val request = InitSessionRequest(
            messageID = messageId,
            providerID = providerId,
            modelID = modelId
        )
        apiClient.initSession(sessionId, request).fold(
            onSuccess = {
                Napier.i("Session $sessionId initialized successfully")
                Resource.Success(Unit)
            },
            onFailure = { error ->
                Napier.e("Failed to initialize session $sessionId", error)
                Resource.Error(error.message ?: "Failed to initialize session")
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SLASH COMMAND EXECUTION (Priority 1.4)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Execute a slash command.
     */
    suspend fun executeCommand(
        sessionId: String,
        command: String,
        arguments: String? = null,
        agent: String? = null
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        apiClient.executeCommand(sessionId, command, arguments, agent).fold(
            onSuccess = {
                Napier.i("Command /$command executed in session $sessionId")
                Resource.Success(Unit)
            },
            onFailure = { error ->
                Napier.e("Failed to execute command /$command in session $sessionId", error)
                Resource.Error(error.message ?: "Failed to execute command")
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHELL COMMAND EXECUTION (Priority 1.5)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Execute a shell command directly.
     */
    suspend fun executeShellCommand(
        sessionId: String,
        command: String,
        agent: String = "build"
    ): Resource<String> = withContext(Dispatchers.IO) {
        apiClient.executeShell(sessionId, command, agent).fold(
            onSuccess = { output ->
                Napier.i("Shell command executed in session $sessionId")
                Resource.Success(output)
            },
            onFailure = { error ->
                Napier.e("Failed to execute shell command in session $sessionId", error)
                Resource.Error(error.message ?: "Failed to execute shell command")
            }
        )
    }
}
