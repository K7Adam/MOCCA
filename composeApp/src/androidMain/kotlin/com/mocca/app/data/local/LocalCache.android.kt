package com.mocca.app.data.local

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.mocca.app.db.AppDatabase
import com.mocca.app.db.MessageEntity
import com.mocca.app.db.ServerConfigEntity
import com.mocca.app.db.SessionEntity
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Android implementation using SQLDelight for persistent storage.
 */
actual class LocalCacheFactory(private val context: Context) {
    actual fun create(): LocalCache = AndroidLocalCache(context)
}

private class AndroidLocalCache(context: Context) : LocalCache {
    private val driver = AndroidSqliteDriver(AppDatabase.Schema, context, "opencode.db")
    private val database = AppDatabase(driver)
    private val sessionQueries get() = database.sessionQueries
    private val messageQueries get() = database.messageQueries
    private val serverConfigQueries get() = database.serverConfigQueries
    private val recentModelQueries get() = database.recentModelQueries
    private val appSettingsQueries get() = database.appSettingsQueries
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // In-memory Git status cache (not persisted)
    @Volatile
    private var cachedGitStatus: GitStatusResponse? = null

    // ==================== Sessions ====================

    override suspend fun getAllSessions(): List<Session> {
        return try {
            sessionQueries.selectAll().executeAsList().map { it.toSession() }
        } catch (e: Exception) {
            Napier.w("Failed to get cached sessions", e)
            emptyList()
        }
    }

    override suspend fun getSession(id: String): Session? {
        return try {
            sessionQueries.selectById(id).executeAsOneOrNull()?.toSession()
        } catch (e: Exception) {
            Napier.w("Failed to get cached session", e)
            null
        }
    }

    override suspend fun insertSession(session: Session) {
        try {
            sessionQueries.insertOrReplace(
                id = session.id,
                title = session.title,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt,
                status = session.status.name.lowercase(),
                cost = 0.0,
                parentId = session.effectiveParentID,
                isSynced = true
            )
        } catch (e: Exception) {
            Napier.w("Failed to cache session", e)
        }
    }
    
    override suspend fun updateSessionStatus(id: String, status: String) {
        try {
            sessionQueries.updateStatus(status = status, updatedAt = System.currentTimeMillis(), id = id)
        } catch (e: Exception) {
            Napier.w("Failed to update session status", e)
        }
    }

    override suspend fun deleteSession(id: String) {
        try {
            sessionQueries.deleteById(id)
            messageQueries.deleteBySession(id)
        } catch (e: Exception) {
            Napier.w("Failed to delete session", e)
        }
    }

    override suspend fun deleteAllSessions() {
        try {
            sessionQueries.deleteAll()
            messageQueries.deleteAll()
        } catch (e: Exception) {
            Napier.w("Failed to delete all sessions", e)
        }
    }

    // ==================== Messages ====================

    override suspend fun getMessages(sessionId: String): List<Message> {
        return try {
            messageQueries.selectBySession(sessionId).executeAsList().map { it.toMessage() }
        } catch (e: Exception) {
            Napier.w("Failed to get cached messages", e)
            emptyList()
        }
    }
    
    override suspend fun getMessage(messageId: String): Message? {
        return try {
            messageQueries.selectById(messageId).executeAsOneOrNull()?.toMessage()
        } catch (e: Exception) {
            Napier.w("Failed to get cached message", e)
            null
        }
    }

    override suspend fun insertMessage(message: Message) {
        try {
            val partsJson = json.encodeToString(ListSerializer(MessagePart.serializer()), message.parts)

            messageQueries.insertOrReplace(
                id = message.id,
                sessionId = message.sessionId,
                role = message.role.name.lowercase(),
                parts = partsJson,
                createdAt = message.createdAt,
                model = message.model,
                cost = message.cost
            )
        } catch (e: Exception) {
            Napier.w("Failed to cache message", e)
        }
    }
    
    override suspend fun updateMessage(message: Message) {
        // Use insertOrReplace for upsert behavior
        insertMessage(message)
    }

    override suspend fun deleteMessages(sessionId: String) {
        try {
            messageQueries.deleteBySession(sessionId)
        } catch (e: Exception) {
            Napier.w("Failed to delete messages", e)
        }
    }
    
    override suspend fun deleteMessage(messageId: String) {
        try {
            messageQueries.deleteById(messageId)
        } catch (e: Exception) {
            Napier.w("Failed to delete message", e)
        }
    }

    // ==================== Server Configs ====================

    override suspend fun getAllServerConfigs(): List<ServerConfig> {
        return try {
            serverConfigQueries.selectAll().executeAsList().map { it.toServerConfig() }
        } catch (e: Exception) {
            Napier.w("Failed to get server configs", e)
            emptyList()
        }
    }

    override suspend fun getServerConfig(id: String): ServerConfig? {
        return try {
            serverConfigQueries.selectById(id).executeAsOneOrNull()?.toServerConfig()
        } catch (e: Exception) {
            Napier.w("Failed to get server config", e)
            null
        }
    }

    override suspend fun getActiveServerConfig(): ServerConfig? {
        return try {
            serverConfigQueries.selectActive().executeAsOneOrNull()?.toServerConfig()
        } catch (e: Exception) {
            Napier.w("Failed to get active server config", e)
            null
        }
    }

    override suspend fun insertServerConfig(config: ServerConfig) {
        try {
            serverConfigQueries.insertOrReplace(
                id = config.id,
                name = config.name,
                baseUrl = config.baseUrl,
                connectionType = config.connectionType.name.lowercase(),
                authType = config.authType.name.lowercase(),
                authToken = config.authToken,
                isActive = config.isActive
            )
        } catch (e: Exception) {
            Napier.w("Failed to insert server config", e)
        }
    }

    override suspend fun setActiveServerConfig(id: String) {
        try {
            serverConfigQueries.deactivateAll()
            serverConfigQueries.activate(id)
        } catch (e: Exception) {
            Napier.w("Failed to set active server config", e)
        }
    }

    override suspend fun deleteServerConfig(id: String) {
        try {
            serverConfigQueries.deleteById(id)
        } catch (e: Exception) {
            Napier.w("Failed to delete server config", e)
        }
    }
    
    // ==================== Git Status (In-Memory) ====================
    
    override fun getGitStatus(): GitStatusResponse? = cachedGitStatus
    
    override fun saveGitStatus(status: GitStatusResponse) {
        cachedGitStatus = status
    }
    
    override fun clearGitStatus() {
        cachedGitStatus = null
    }

    // ==================== Agents ====================
    
    override suspend fun getAllAgents(): List<Agent> {
        return try {
            database.agentQueries.selectAll().executeAsList().map { it.toAgent() }
        } catch (e: Exception) {
            Napier.w("Failed to get cached agents", e)
            emptyList()
        }
    }
    
    override suspend fun getVisibleAgents(): List<Agent> {
        return try {
            database.agentQueries.selectVisible().executeAsList().map { it.toAgent() }
        } catch (e: Exception) {
            Napier.w("Failed to get visible agents", e)
            emptyList()
        }
    }
    
    override suspend fun getAgent(name: String): Agent? {
        return try {
            database.agentQueries.selectByName(name).executeAsOneOrNull()?.toAgent()
        } catch (e: Exception) {
            Napier.w("Failed to get cached agent", e)
            null
        }
    }
    
    override suspend fun insertAgent(agent: Agent) {
        try {
            val now = System.currentTimeMillis()
            val permissionsJson = agent.permission?.let { 
                json.encodeToString(ListSerializer(AgentPermission.serializer()), it) 
            }
            val optionsJson = agent.options?.toString()
            
            database.agentQueries.insertOrReplace(
                name = agent.name,
                mode = agent.mode,
                description = agent.description,
                prompt = agent.prompt,
                isNative = agent.native,
                isHidden = agent.hidden,
                color = agent.color,
                modelProviderId = agent.model?.providerId,
                modelId = agent.model?.modelId,
                permissionsJson = permissionsJson,
                optionsJson = optionsJson,
                cachedAt = now
            )
        } catch (e: Exception) {
            Napier.w("Failed to cache agent", e)
        }
    }
    
    override suspend fun insertAgents(agents: List<Agent>) {
        try {
            database.transaction {
                database.agentQueries.deleteAll()
                agents.forEach { agent -> 
                    val now = System.currentTimeMillis()
                    val permissionsJson = agent.permission?.let { 
                        json.encodeToString(ListSerializer(AgentPermission.serializer()), it) 
                    }
                    val optionsJson = agent.options?.toString()
                    
                    database.agentQueries.insertOrReplace(
                        name = agent.name,
                        mode = agent.mode,
                        description = agent.description,
                        prompt = agent.prompt,
                        isNative = agent.native,
                        isHidden = agent.hidden,
                        color = agent.color,
                        modelProviderId = agent.model?.providerId,
                        modelId = agent.model?.modelId,
                        permissionsJson = permissionsJson,
                        optionsJson = optionsJson,
                        cachedAt = now
                    )
                }
            }
        } catch (e: Exception) {
            Napier.w("Failed to cache agents", e)
        }
    }
    
    override suspend fun deleteAllAgents() {
        try {
            database.agentQueries.deleteAll()
        } catch (e: Exception) {
            Napier.w("Failed to delete agents", e)
        }
    }
    
    override suspend fun hasAgentCache(maxAgeMs: Long): Boolean {
        return try {
            val threshold = System.currentTimeMillis() - maxAgeMs
            database.agentQueries.hasFreshCache(threshold).executeAsOne() > 0
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Commands ====================
    
    override suspend fun getAllCommands(): List<Command> {
        return try {
            database.commandQueries.selectAll().executeAsList().map { it.toCommand() }
        } catch (e: Exception) {
            Napier.w("Failed to get cached commands", e)
            emptyList()
        }
    }
    
    override suspend fun getCommand(name: String): Command? {
        return try {
            database.commandQueries.selectByName(name).executeAsOneOrNull()?.toCommand()
        } catch (e: Exception) {
            Napier.w("Failed to get cached command", e)
            null
        }
    }
    
    override suspend fun insertCommand(command: Command) {
        try {
            val now = System.currentTimeMillis()
            val templateJson = command.template?.toString()
            val hintsJson = command.hints?.let { json.encodeToString(ListSerializer(kotlinx.serialization.serializer<String>()), it) }
            
            database.commandQueries.insertOrReplace(
                name = command.name,
                description = command.description,
                templateJson = templateJson,
                isSubtask = command.subtask,
                hintsJson = hintsJson,
                isMcp = command.mcp,
                cachedAt = now
            )
        } catch (e: Exception) {
            Napier.w("Failed to cache command", e)
        }
    }
    
    override suspend fun insertCommands(commands: List<Command>) {
        try {
            database.transaction {
                database.commandQueries.deleteAll()
                commands.forEach { command ->
                    val now = System.currentTimeMillis()
                    val templateJson = command.template?.toString()
                    val hintsJson = command.hints?.let { json.encodeToString(ListSerializer(kotlinx.serialization.serializer<String>()), it) }
                    
                    database.commandQueries.insertOrReplace(
                        name = command.name,
                        description = command.description,
                        templateJson = templateJson,
                        isSubtask = command.subtask,
                        hintsJson = hintsJson,
                        isMcp = command.mcp,
                        cachedAt = now
                    )
                }
            }
        } catch (e: Exception) {
            Napier.w("Failed to cache commands", e)
        }
    }
    
    override suspend fun deleteAllCommands() {
        try {
            database.commandQueries.deleteAll()
        } catch (e: Exception) {
            Napier.w("Failed to delete commands", e)
        }
    }
    
    override suspend fun hasCommandCache(maxAgeMs: Long): Boolean {
        return try {
            val threshold = System.currentTimeMillis() - maxAgeMs
            database.commandQueries.hasFreshCache(threshold).executeAsOne() > 0
        } catch (e: Exception) {
            false
        }
    }

    // ==================== FileInfo ====================
    
    override suspend fun getFilesInDirectory(parentPath: String?): List<FileInfo> {
        return try {
            if (parentPath.isNullOrEmpty()) {
                database.fileInfoQueries.selectRoot().executeAsList().map { it.toFileInfo() }
            } else {
                database.fileInfoQueries.selectByParent(parentPath).executeAsList().map { it.toFileInfo() }
            }
        } catch (e: Exception) {
            Napier.w("Failed to get cached files", e)
            emptyList()
        }
    }
    
    override suspend fun getFileInfo(path: String): FileInfo? {
        return try {
            database.fileInfoQueries.selectByPath(path).executeAsOneOrNull()?.toFileInfo()
        } catch (e: Exception) {
            Napier.w("Failed to get cached file info", e)
            null
        }
    }
    
    override suspend fun insertFileInfo(fileInfo: FileInfo, parentPath: String?) {
        try {
            val now = System.currentTimeMillis()
            database.fileInfoQueries.insertOrReplace(
                path = fileInfo.path,
                name = fileInfo.name,
                type = fileInfo.type,
                size = fileInfo.size,
                modifiedAt = fileInfo.modifiedAt,
                parentPath = parentPath ?: "",
                cachedAt = now
            )
        } catch (e: Exception) {
            Napier.w("Failed to cache file info", e)
        }
    }
    
    override suspend fun insertFilesInDirectory(files: List<FileInfo>, parentPath: String?) {
        try {
            database.transaction {
                // Clear existing files in this directory
                database.fileInfoQueries.deleteByParent(parentPath ?: "")
                
                val now = System.currentTimeMillis()
                files.forEach { fileInfo ->
                    database.fileInfoQueries.insertOrReplace(
                        path = fileInfo.path,
                        name = fileInfo.name,
                        type = fileInfo.type,
                        size = fileInfo.size,
                        modifiedAt = fileInfo.modifiedAt,
                        parentPath = parentPath ?: "",
                        cachedAt = now
                    )
                }
            }
        } catch (e: Exception) {
            Napier.w("Failed to cache files", e)
        }
    }
    
    override suspend fun deleteFilesInDirectory(parentPath: String?) {
        try {
            database.fileInfoQueries.deleteByParent(parentPath ?: "")
        } catch (e: Exception) {
            Napier.w("Failed to delete files", e)
        }
    }
    
    override suspend fun deleteAllFiles() {
        try {
            database.fileInfoQueries.deleteAll()
        } catch (e: Exception) {
            Napier.w("Failed to delete all files", e)
        }
    }
    
    override suspend fun hasFileCache(parentPath: String?, maxAgeMs: Long): Boolean {
        return try {
            val threshold = System.currentTimeMillis() - maxAgeMs
            database.fileInfoQueries.hasFreshCacheForPath(parentPath ?: "", threshold).executeAsOne() > 0
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Recent Models ====================
    
    override suspend fun getRecentModels(): List<com.mocca.app.domain.model.RecentModel> {
        return try {
            recentModelQueries.selectRecent().executeAsList().map { 
                com.mocca.app.domain.model.RecentModel(it.providerId, it.modelId, it.lastUsedAt) 
            }
        } catch (e: Exception) {
            Napier.w("Failed to get recent models", e)
            emptyList()
        }
    }
    
    override suspend fun insertRecentModel(recentModel: RecentModel) {
        try {
            database.transaction {
                recentModelQueries.insertRecent(
                    providerId = recentModel.providerId, 
                    modelId = recentModel.modelId, 
                    lastUsedAt = recentModel.lastUsedAt
                )
                // Cleanup old entries (keep top 5)
                recentModelQueries.deleteOldest()
            }
        } catch (e: Exception) {
            Napier.w("Failed to insert recent model", e)
        }
    }

    // ==================== App Settings ====================

    override suspend fun getSetting(key: String): String? {
        return try {
            appSettingsQueries.getSetting(key).executeAsOneOrNull()
        } catch (e: Exception) {
            Napier.w("Failed to get setting: $key", e)
            null
        }
    }

    override suspend fun saveSetting(key: String, value: String) {
        try {
            appSettingsQueries.insertSetting(key, value)
        } catch (e: Exception) {
            Napier.w("Failed to save setting: $key", e)
        }
    }

    override suspend fun deleteSetting(key: String) {
        try {
            appSettingsQueries.deleteSetting(key)
        } catch (e: Exception) {
            Napier.w("Failed to delete setting: $key", e)
        }
    }

    // ==================== Entity Mappers ====================

    private fun SessionEntity.toSession(): Session {
        return Session(
            id = id,
            title = title,
            time = SessionTime(created = createdAt, updated = updatedAt),
            status = try {
                SessionStatus.valueOf(status.uppercase())
            } catch (e: Exception) {
                SessionStatus.IDLE
            },
            parentID = parentId
        )
    }

    private fun MessageEntity.toMessage(): Message {
        return Message(
            id = id,
            sessionId = sessionId,
            role = try {
                MessageRole.valueOf(role.uppercase())
            } catch (e: Exception) {
                MessageRole.USER
            },
            parts = try {
                json.decodeFromString(ListSerializer(MessagePart.serializer()), parts)
            } catch (e: Exception) {
                // Fallback for migration or error
                listOf(MessagePart.Text(parts))
            },
            createdAt = createdAt,
            model = model,
            cost = cost
        )
    }

    private fun ServerConfigEntity.toServerConfig(): ServerConfig {
        return ServerConfig(
            id = id,
            name = name,
            baseUrl = baseUrl,
            connectionType = try {
                ConnectionType.valueOf(connectionType.uppercase())
            } catch (e: Exception) {
                ConnectionType.LOCAL
            },
            authType = try {
                AuthType.valueOf(authType.uppercase())
            } catch (e: Exception) {
                AuthType.NONE
            },
            authToken = authToken,
            isActive = isActive
        )
    }
    
    private fun com.mocca.app.db.AgentEntity.toAgent(): Agent {
        val permissions = permissionsJson?.let { 
            try {
                json.decodeFromString(ListSerializer(AgentPermission.serializer()), it)
            } catch (e: Exception) {
                null
            }
        }
        val options = optionsJson?.let {
            try {
                json.parseToJsonElement(it)
            } catch (e: Exception) {
                null
            }
        }
        return Agent(
            name = name,
            mode = mode,
            description = description,
            prompt = prompt,
            native = isNative,
            hidden = isHidden,
            color = color,
            model = if (modelProviderId != null || modelId != null) {
                AgentModel(providerId = modelProviderId, modelId = modelId)
            } else null,
            permission = permissions,
            options = options
        )
    }
    
    private fun com.mocca.app.db.CommandEntity.toCommand(): Command {
        val template = templateJson?.let {
            try {
                json.parseToJsonElement(it)
            } catch (e: Exception) {
                null
            }
        }
        val hints = hintsJson?.let {
            try {
                json.decodeFromString(ListSerializer(kotlinx.serialization.serializer<String>()), it)
            } catch (e: Exception) {
                null
            }
        }
        return Command(
            name = name,
            description = description,
            template = template,
            subtask = isSubtask,
            hints = hints,
            mcp = isMcp
        )
    }
    
    private fun com.mocca.app.db.FileInfoEntity.toFileInfo(): FileInfo {
        return FileInfo(
            name = name,
            path = path,
            type = type,
            size = size,
            modifiedAt = modifiedAt
        )
    }
}
