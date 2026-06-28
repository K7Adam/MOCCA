package com.mocca.app.data.local

import com.mocca.app.domain.model.Agent
import com.mocca.app.domain.model.AiRecentModel
import com.mocca.app.domain.model.AiSelection
import com.mocca.app.domain.model.Command
import com.mocca.app.domain.model.GitStatusResponse
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.ServerConfig
import com.mocca.app.domain.model.Session
import com.mocca.app.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory fake implementation of [LocalCache] for unit tests.
 * Not thread-safe — tests run single-threaded in runTest.
 */
class FakeLocalCache : LocalCache {

    val sessions = mutableMapOf<String, Session>()
    val messages = mutableMapOf<String, MutableList<Message>>()
    val settings = mutableMapOf<String, String>()
    val serverConfigs = mutableMapOf<String, ServerConfig>()
    var activeServerConfigId: String? = null
    val agents = mutableListOf<Agent>()
    val commands = mutableListOf<Command>()
    val todos = mutableMapOf<String, MutableList<Todo>>()
    val aiSelections = mutableMapOf<String, AiSelection>()
    val aiRecentModels = mutableListOf<AiRecentModel>()
    private val gitStatusFlow = MutableStateFlow<GitStatusResponse?>(null)

    // Sessions
    override suspend fun getAllSessions(): List<Session> = sessions.values.toList().sortedBy { it.id }
    override suspend fun getSession(id: String): Session? = sessions[id]
    override suspend fun insertSession(session: Session) { sessions[session.id] = session }
    override suspend fun insertSessions(sessions: List<Session>) { sessions.forEach { insertSession(it) } }
    override suspend fun updateSessionStatus(id: String, status: String) {
        sessions[id]?.let { s ->
            sessions[id] = s.copy(status = enumValueOf(status))
        }
    }
    override suspend fun deleteSession(id: String) { sessions.remove(id) }
    override suspend fun deleteAllSessions() { sessions.clear() }
    override fun observeAllSessions(): Flow<List<Session>> = MutableStateFlow(getAllSessionsSync())

    private fun getAllSessionsSync(): List<Session> = sessions.values.toList().sortedBy { it.id }

    // Messages
    override suspend fun getMessagesPaged(sessionId: String, cursor: Long?, limit: Long): List<Message> =
        messages[sessionId]?.toList() ?: emptyList()
    override fun observeRecentMessages(sessionId: String, limit: Long): Flow<List<Message>> =
        MutableStateFlow(messages[sessionId]?.toList() ?: emptyList())
    override suspend fun getMessage(messageId: String): Message? =
        messages.values.flatten().find { it.id == messageId }
    override suspend fun insertMessages(messages: List<Message>) {
        messages.forEach { msg ->
            this.messages.getOrPut(msg.sessionId) { mutableListOf() }.add(msg)
        }
    }
    override suspend fun deleteMessage(messageId: String) {
        messages.values.forEach { list -> list.removeAll { it.id == messageId } }
    }
    override suspend fun pruneMessages(sessionId: String, keepCount: Long) {
        messages[sessionId]?.let { list ->
            if (list.size > keepCount) {
                messages[sessionId] = list.takeLast(keepCount.toInt()).toMutableList()
            }
        }
    }

    // Todos
    override suspend fun insertSessionTodos(sessionId: String, todos: List<Todo>) {
        this.todos[sessionId] = todos.toMutableList()
    }
    override fun observeSessionTodos(sessionId: String): Flow<List<Todo>> =
        MutableStateFlow(todos[sessionId]?.toList() ?: emptyList())

    // Message Parts
    override suspend fun updateMessagePart(
        messageId: String,
        partId: String,
        partType: String?,
        content: String?,
        delta: String?
    ) {
        // Simplified: find the message and update its part content
        messages.values.flatten().find { it.id == messageId }?.let { msg ->
            val sessionMsgs = messages[msg.sessionId] ?: return
            val idx = sessionMsgs.indexOfFirst { it.id == messageId }
            if (idx >= 0) {
                // In a real impl, this would update the specific part
                // For testing, we just track that it was called
            }
        }
    }

    // Server Configs
    override suspend fun getAllServerConfigs(): List<ServerConfig> = serverConfigs.values.toList()
    override suspend fun getServerConfig(id: String): ServerConfig? = serverConfigs[id]
    override suspend fun getActiveServerConfig(): ServerConfig? =
        activeServerConfigId?.let { serverConfigs[it] }
    override suspend fun insertServerConfig(config: ServerConfig) { serverConfigs[config.id] = config }
    override suspend fun setActiveServerConfig(id: String) { activeServerConfigId = id }
    override suspend fun deleteServerConfig(id: String) {
        serverConfigs.remove(id)
        if (activeServerConfigId == id) activeServerConfigId = null
    }

    // Agents
    override suspend fun getAllAgents(): List<Agent> = agents.toList()
    override suspend fun insertAgents(agents: List<Agent>) {
        this.agents.clear()
        this.agents.addAll(agents)
    }
    override suspend fun deleteAllAgents() { agents.clear() }
    override suspend fun hasAgentCache(maxAgeMs: Long): Boolean = agents.isNotEmpty()

    // Commands
    override suspend fun getAllCommands(): List<Command> = commands.toList()
    override suspend fun insertCommands(commands: List<Command>) {
        this.commands.clear()
        this.commands.addAll(commands)
    }
    override suspend fun deleteAllCommands() { commands.clear() }
    override suspend fun hasCommandCache(maxAgeMs: Long): Boolean = commands.isNotEmpty()

    // FileInfo — not used in tests
    // AI Runtime Config
    override suspend fun getAiSelection(projectKey: String): AiSelection? = aiSelections[projectKey]
    override suspend fun saveAiSelection(projectKey: String, selection: AiSelection) {
        aiSelections[projectKey] = selection
    }
    override suspend fun getAiRecentModels(projectKey: String): List<AiRecentModel> = aiRecentModels.toList()
    override suspend fun insertAiRecentModel(recentModel: AiRecentModel) { aiRecentModels.add(recentModel) }

    // App Settings
    override suspend fun getSetting(key: String): String? = settings[key]
    override suspend fun saveSetting(key: String, value: String) { settings[key] = value }
    override suspend fun deleteSetting(key: String) { settings.remove(key) }

    // Git Status
    override fun getGitStatus(): GitStatusResponse? = gitStatusFlow.value
    override fun observeGitStatus(): MutableStateFlow<GitStatusResponse?> = gitStatusFlow
    override fun saveGitStatus(status: GitStatusResponse) { gitStatusFlow.value = status }
}
