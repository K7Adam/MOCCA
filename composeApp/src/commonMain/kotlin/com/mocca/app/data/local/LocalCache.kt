package com.mocca.app.data.local

import com.mocca.app.domain.model.Agent
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
    suspend fun updateSessionStatus(id: String, status: String)
    suspend fun deleteSession(id: String)
    suspend fun deleteAllSessions()

    // Messages
    suspend fun getMessages(sessionId: String): List<Message>
    suspend fun getMessagesPaged(sessionId: String, cursor: Long?, limit: Long): List<Message>
    fun observeMessages(sessionId: String): kotlinx.coroutines.flow.Flow<List<Message>>
    fun observeRecentMessages(sessionId: String, limit: Long): kotlinx.coroutines.flow.Flow<List<Message>> // Added
    suspend fun getMessage(messageId: String): Message?
    suspend fun insertMessage(message: Message)
    suspend fun insertMessages(messages: List<Message>) // Added
    suspend fun updateMessage(message: Message)
    suspend fun deleteMessages(sessionId: String)
    suspend fun deleteMessage(messageId: String)
    suspend fun pruneMessages(sessionId: String, keepCount: Long) // Added
    
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
    suspend fun getVisibleAgents(): List<Agent>
    suspend fun getAgent(name: String): Agent?
    suspend fun insertAgent(agent: Agent)
    suspend fun insertAgents(agents: List<Agent>)
    suspend fun deleteAllAgents()
    suspend fun hasAgentCache(maxAgeMs: Long): Boolean
    
    // Commands
    suspend fun getAllCommands(): List<Command>
    suspend fun getCommand(name: String): Command?
    suspend fun insertCommand(command: Command)
    suspend fun insertCommands(commands: List<Command>)
    suspend fun deleteAllCommands()
    suspend fun hasCommandCache(maxAgeMs: Long): Boolean
    
    // FileInfo (file browser cache)
    suspend fun getFilesInDirectory(parentPath: String?): List<FileInfo>
    suspend fun getFileInfo(path: String): FileInfo?
    suspend fun insertFileInfo(fileInfo: FileInfo, parentPath: String?)
    suspend fun insertFilesInDirectory(files: List<FileInfo>, parentPath: String?)
    suspend fun deleteFilesInDirectory(parentPath: String?)
    suspend fun deleteAllFiles()
    suspend fun hasFileCache(parentPath: String?, maxAgeMs: Long): Boolean
    
    // Recent Models
    suspend fun getRecentModels(): List<RecentModel>
    suspend fun insertRecentModel(recentModel: RecentModel)
    
    // App Settings
    suspend fun getSetting(key: String): String?
    suspend fun saveSetting(key: String, value: String)
    suspend fun deleteSetting(key: String)
    
    // Git Status (in-memory cache, not persisted to DB)
    fun getGitStatus(): GitStatusResponse?
    fun saveGitStatus(status: GitStatusResponse)
    fun clearGitStatus()
}

/**
 * Expected platform-specific cache factory.
 */
expect class LocalCacheFactory {
    fun create(): LocalCache
}
