package com.mocca.app.data.local

import com.mocca.app.domain.model.Agent
import com.mocca.app.domain.model.AiRecentModel
import com.mocca.app.domain.model.AiSelection
import com.mocca.app.domain.model.Command
import com.mocca.app.domain.model.FileInfo
import com.mocca.app.domain.model.GitStatusResponse
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.RecentModel
import com.mocca.app.domain.model.ServerConfig
import com.mocca.app.domain.model.Session

/**
 * Platform-agnostic local cache interface.
 * Android uses SQLDelight for persistent storage.
 */
interface LocalCache {
    // Sessions
    suspend fun getAllSessions(): List<Session>
    suspend fun getSession(id: String): Session?
    suspend fun insertSession(session: Session)
    suspend fun insertSessions(sessions: List<Session>)  // Batch insert for atomic updates
    suspend fun updateSessionStatus(id: String, status: String)
    suspend fun deleteSession(id: String)
    suspend fun deleteAllSessions()
    fun observeAllSessions(): kotlinx.coroutines.flow.Flow<List<Session>>

    // Messages
    suspend fun getMessagesPaged(sessionId: String, cursor: Long?, limit: Long): List<Message>
    fun observeRecentMessages(sessionId: String, limit: Long): kotlinx.coroutines.flow.Flow<List<Message>>
    suspend fun getMessage(messageId: String): Message?
    suspend fun insertMessages(messages: List<Message>)

    suspend fun deleteMessage(messageId: String)
    suspend fun pruneMessages(sessionId: String, keepCount: Long) // Added
    
    // Todos
    suspend fun insertSessionTodos(sessionId: String, todos: List<com.mocca.app.domain.model.Todo>)
    fun observeSessionTodos(sessionId: String): kotlinx.coroutines.flow.Flow<List<com.mocca.app.domain.model.Todo>>
    
    /**
     * IMPROVED: Incrementally update a message part's content.
     * This is more efficient than fetching and re-inserting entire messages.
     * 
     * @param messageId The message ID
     * @param partId The part ID to update
     * @param content New content (replaces existing)
     * @param delta Content to append (for streaming)
     */
    suspend fun updateMessagePart(
        messageId: String,
        partId: String,
        content: String? = null,
        delta: String? = null
    )

    // Server Configs
    suspend fun getAllServerConfigs(): List<ServerConfig>
    suspend fun getServerConfig(id: String): ServerConfig?
    suspend fun getActiveServerConfig(): ServerConfig?
    suspend fun insertServerConfig(config: ServerConfig)
    suspend fun setActiveServerConfig(id: String)
    suspend fun deleteServerConfig(id: String)
    
    // Agents
    suspend fun getAllAgents(): List<Agent>
    suspend fun insertAgents(agents: List<Agent>)
    suspend fun deleteAllAgents()
    suspend fun hasAgentCache(maxAgeMs: Long): Boolean
    
    // Commands
    suspend fun getAllCommands(): List<Command>
    suspend fun insertCommands(commands: List<Command>)
    suspend fun deleteAllCommands()
    suspend fun hasCommandCache(maxAgeMs: Long): Boolean
    
    // FileInfo (file browser cache)
    
    // Recent Models
    suspend fun getRecentModels(): List<RecentModel>
    suspend fun insertRecentModel(recentModel: RecentModel)

    // AI Runtime Config
    suspend fun getAiSelection(projectKey: String): AiSelection?
    suspend fun saveAiSelection(projectKey: String, selection: AiSelection)
    suspend fun getAiRecentModels(projectKey: String): List<AiRecentModel>
    suspend fun insertAiRecentModel(recentModel: AiRecentModel)
    
    // App Settings
    suspend fun getSetting(key: String): String?
    suspend fun saveSetting(key: String, value: String)
    suspend fun deleteSetting(key: String)
    
    // Git Status (in-memory cache, not persisted to DB)
    fun getGitStatus(): GitStatusResponse?
    fun observeGitStatus(): kotlinx.coroutines.flow.StateFlow<GitStatusResponse?>
    fun saveGitStatus(status: GitStatusResponse)
}

/**
 * Expected platform-specific cache factory.
 */
expect class LocalCacheFactory {
    fun create(): LocalCache
}
