package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Repository for Session management with offline-first caching.
 * 
 * PERFORMANCE: Uses request deduplication to prevent duplicate API calls
 * when multiple consumers request the same data simultaneously.
 */
class SessionRepository(
    private val apiClient: MoccaApiClient,
    private val localCache: LocalCache
) {
    // OOM FIX: LRU eviction — keep max 3 sessions in memory cache
    private val memoryCacheMaxSize = 3
    private val memoryCache = linkedMapOf<String, List<Message>>()
    private val cacheMutex = Mutex()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Default model/provider - should be configurable from settings
    // These will be populated from /config/providers
    private var defaultModelId: String = "" // Loaded dynamically from loadDefaultConfig()
    private var defaultProviderId: String = "anthropic"
    private var defaultMode: String = "build"

    private companion object {
        const val DEFAULT_MESSAGE_FETCH_LIMIT = 80
        const val MAX_MESSAGE_FETCH_LIMIT = 200
    }
    
    /**
     * Get all sessions with offline-first strategy.
     * Returns cached data immediately, then refreshes from network.
     */
    private val sessionsRefreshMutex = Mutex()
    private var activeSessionsRefresh: kotlinx.coroutines.Deferred<Result<List<Session>>>? = null

    fun getSessions(): Flow<Resource<List<Session>>> = flow {
        emit(Resource.Loading())

        // 1. Return cached data first
        val cached = localCache.getAllSessions()
        if (cached.isNotEmpty()) {
            emit(Resource.Loading(cached))
        }

        // 2. Fetch fresh data from network (deduplicated)
        val result = coroutineScope {
            sessionsRefreshMutex.withLock {
                val current = activeSessionsRefresh
                if (current != null) {
                    current
                } else {
                    val deferred = async(Dispatchers.IO) {
                        try {
                            apiClient.listSessions().also { res ->
                                res.onSuccess { sessions ->
                                    // Use batch insert for atomic update - observer fires ONCE
                                    localCache.insertSessions(sessions)
                                }
                            }
                        } finally {
                            sessionsRefreshMutex.withLock { activeSessionsRefresh = null }
                        }
                    }
                    activeSessionsRefresh = deferred
                    deferred
                }
            }.await()
        }

        result.fold(
            onSuccess = { sessions ->
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
     * 
     * PERFORMANCE: Uses request deduplication to prevent duplicate API calls
     * when multiple consumers request the same session simultaneously.
     */
    private val sessionRequestMutex = Mutex()
    private val activeSessionRequests = mutableMapOf<String, kotlinx.coroutines.Deferred<Resource<Session>>>()

    suspend fun getSession(sessionId: String): Resource<Session> = withContext(Dispatchers.IO) {
        val deferred = coroutineScope {
            sessionRequestMutex.withLock {
                activeSessionRequests[sessionId] ?: async {
                    try {
                        fetchSessionInternal(sessionId)
                    } finally {
                        sessionRequestMutex.withLock { activeSessionRequests.remove(sessionId) }
                    }
                }.also { activeSessionRequests[sessionId] = it }
            }
        }
        deferred.await()
    }
    
    private suspend fun fetchSessionInternal(sessionId: String): Resource<Session> {
        // Check cache first
        val cached = localCache.getSession(sessionId)

        return apiClient.listSessions().fold(
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
     * @param fetchLimit Optional parameter to limit the number of messages fetched from the server.
     * If null, fetches the default history size from the server.
     */
    fun getMessages(sessionId: String, limit: Long = 50, fetchLimit: Int? = null): Flow<Resource<List<Message>>> = channelFlow {
        send(Resource.Loading())
        val boundedFetchLimit = (fetchLimit ?: limit.coerceIn(1, MAX_MESSAGE_FETCH_LIMIT.toLong()).toInt())
            .coerceIn(1, MAX_MESSAGE_FETCH_LIMIT)

        // 1. Memory cache
        cacheMutex.withLock { memoryCache[sessionId] }?.let {
            send(Resource.Success(it))
        }

        // 2. DB Observer - reactive updates from local cache
        val dbJob = launch {
            localCache.observeRecentMessages(sessionId, limit).collect { messages ->
                cacheMutex.withLock {
                    memoryCache[sessionId] = messages
                    // OOM FIX: Evict oldest entries when cache exceeds max size
                    while (memoryCache.size > memoryCacheMaxSize) {
                        val oldestKey = memoryCache.keys.first()
                        memoryCache.remove(oldestKey)
                        Napier.d("[SessionRepository] Evicted session $oldestKey from memory cache (size=${memoryCache.size})")
                    }
                }
                send(Resource.Success(messages))
            }
        }

        // 3. Network Refresh - fetch latest from server
        launch {
            val result = apiClient.getMessages(sessionId, boundedFetchLimit)
            result.fold(
                onSuccess = { responses ->
                    val messages = responses.map { Message.fromResponse(it) }
                    if (messages.isNotEmpty()) {
                        localCache.insertMessages(messages)
                    }
                },
                onFailure = { error ->
                    Napier.e("[SessionRepository] Failed to refresh messages: ${error.message}", error)
                    val current = cacheMutex.withLock { memoryCache[sessionId] } ?: emptyList()
                    send(Resource.Error(error.message ?: "Failed to refresh messages", current))
                }
            )
        }
        
        awaitClose { dbJob.cancel() }
    }.flowOn(Dispatchers.IO)

    /**
     * One-shot refresh used by screen stores that already own a DB observer.
     * This avoids adding a second permanent observer and keeps startup history bounded.
     */
    suspend fun refreshMessages(
        sessionId: String,
        fetchLimit: Int = DEFAULT_MESSAGE_FETCH_LIMIT
    ): Result<List<Message>> = withContext(Dispatchers.IO) {
        val boundedFetchLimit = fetchLimit.coerceIn(1, MAX_MESSAGE_FETCH_LIMIT)
        apiClient.getMessages(sessionId, boundedFetchLimit).map { responses ->
            val messages = responses.map { Message.fromResponse(it) }
            if (messages.isNotEmpty()) {
                localCache.insertMessages(messages)
            }
            messages
        }
    }

    /**
     * Fast-sync: Fetches only the most recent N messages and updates the local cache.
     * Use this when a session goes idle to avoid full history refetch.
     */
    suspend fun syncLatestMessages(sessionId: String, keepLimit: Int = 5): Result<Unit> = withContext(Dispatchers.IO) {
        val boundedKeepLimit = keepLimit.coerceIn(1, MAX_MESSAGE_FETCH_LIMIT)
        val result = apiClient.getMessages(sessionId, boundedKeepLimit)
        result.fold(
            onSuccess = { responses ->
                val messages = responses.map { Message.fromResponse(it) }
                if (messages.isNotEmpty()) {
                    localCache.insertMessages(messages)
                }
                Result.success(Unit)
            },
            onFailure = { error ->
                Napier.e("[SessionRepository] Failed to sync latest messages: ${error.message}", error)
                Result.failure(error)
            }
        )
    }

    /**
     * Load older messages for pagination.
     */
    suspend fun loadMoreMessages(sessionId: String, cursor: Long, limit: Long = 50): Resource<List<Message>> {
        return try {
            val messages = localCache.getMessagesPaged(sessionId, cursor, limit)
            Resource.Success(messages)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load history")
        }
    }

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
        variant: String? = null,
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
            mode = mode ?: defaultMode,
            variant = variant
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
            parts = listOf(MessagePart.Text(text = text)),
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
     * Delete a single message from a session.
     * Removes from local cache on success.
     */
    suspend fun deleteMessage(sessionId: String, messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        apiClient.deleteMessage(sessionId, messageId).also { result ->
            result.onSuccess {
                localCache.deleteMessage(messageId)
                Napier.i("Message deleted: $messageId from session $sessionId")
            }
        }
    }

    /**
     * Delete a single part from a message.
     */
    suspend fun deleteMessagePart(sessionId: String, messageId: String, partId: String): Result<Unit> = withContext(Dispatchers.IO) {
        apiClient.deleteMessagePart(sessionId, messageId, partId).also { result ->
            result.onSuccess {
                Napier.i("Message part deleted: $partId from message $messageId")
            }
        }
    }

    /**
     * Edit the content of a specific message part.
     * @param sessionId The session containing the message
     * @param messageId The message containing the part
     * @param partId The part to patch
     * @param content The new content
     */
    suspend fun patchMessagePart(
        sessionId: String,
        messageId: String,
        partId: String,
        content: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        apiClient.patchMessagePart(sessionId, messageId, partId, content).also { result ->
            result.onSuccess {
                Napier.i("Message part patched: $partId in message $messageId")
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
                // Use batch insert for atomic update
                localCache.insertSessions(sessions)
                Napier.i("Refreshed ${sessions.size} sessions from server")
            },
            onFailure = { error ->
                Napier.e("Failed to refresh sessions", error)
            }
        )
    }
    
    /**
     * Load and cache the default model/provider from server config.
     * Now properly awaits network response before returning.
     */
    suspend fun loadDefaultConfig() {
        // 0. Instant Load: Check local cache for recent models to instantly populate UI
        try {
            val recent = localCache.getRecentModels().firstOrNull()
            if (recent != null) {
                defaultProviderId = recent.providerId
                defaultModelId = recent.modelId
                Napier.i("Fast-loaded default config from cache: $defaultProviderId / $defaultModelId")
            }
        } catch (e: Exception) {
            Napier.w("Failed to read recent models from cache for fast-loading", e)
        }

        // If we already have defaults from cache, we can return early but still sync in background
        val hadCacheHit = defaultModelId.isNotEmpty()
        
        // 1. Try to load global config from server (contains default model)
        // Now we AWAIT this instead of fire-and-forget
        if (!hadCacheHit || true) { // Always sync to ensure we have latest
            apiClient.getConfig().onSuccess { config ->
                // Parse "provider/model" format (e.g. "anthropic/claude-sonnet-4-5")
                config.model?.let { modelStr ->
                    val parts = modelStr.split("/", limit = 2)
                    if (parts.size == 2) {
                        defaultProviderId = parts[0]
                        defaultModelId = parts[1]
                        Napier.i("Default config synced from server: $defaultProviderId / $defaultModelId")
                        // Save to RecentModel for future instant loading
                        addRecentModel(defaultProviderId, defaultModelId)
                    }
                }

                // Set default mode
                config.modes.firstOrNull()?.let { mode ->
                    defaultMode = mode.id
                    Napier.i("Default mode synced: $defaultMode")
                }
            }.onFailure { error ->
                Napier.w("Failed to sync server config: ${error.message}")
            }

            // 2. Fallback: If defaults still empty, try to pick first available from providers list
            if (defaultModelId.isEmpty()) {
                apiClient.getProviders().onSuccess { providers ->
                    providers.firstOrNull { it.models.isNotEmpty() }?.let { provider ->
                        defaultProviderId = provider.id
                        defaultModelId = provider.models.values.first().id
                        Napier.i("Fallback default config: $defaultProviderId / $defaultModelId")
                        // Save to RecentModel for future instant loading
                        addRecentModel(defaultProviderId, defaultModelId)
                    }
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

    // SESSION TODO LIST (Priority 2.1)


    /**
     * Get todos for a session, observing local cache and refreshing from network.
     */
    fun getSessionTodos(sessionId: String): Flow<Resource<List<Todo>>> = flow {
        emit(Resource.Loading())

        repositoryScope.launch {
            apiClient.getSessionTodos(sessionId)
                .onSuccess { remoteTodos ->
                    localCache.insertSessionTodos(sessionId, remoteTodos)
                }
                .onFailure { e ->
                    Napier.w("Background todo sync failed for session $sessionId", e)
                }
        }

        emitAll(
            localCache.observeSessionTodos(sessionId)
                .map { localTodos -> Resource.Success(localTodos) }
        )
    }.flowOn(Dispatchers.IO)

    // SESSION SHARING (Priority 2.2)


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

    // SESSION SUMMARIZATION (Priority 2.3)


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

    // SESSION INIT (Priority 2.4)


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

    // SLASH COMMAND EXECUTION (Priority 1.4)


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

    // SHELL COMMAND EXECUTION (Priority 1.5)


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
