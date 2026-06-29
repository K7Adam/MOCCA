package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Server-Sent Events from OpenCode server.
 * Based on the official OpenCode SDK event types.
 */
@Serializable
sealed interface ServerEvent {
    val type: String
    
    @Serializable
    @SerialName("server.connected")
    @Immutable
    data class Connected(
        override val type: String = "server.connected",
        val properties: ConnectedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("server.heartbeat")
    @Immutable
    data class Heartbeat(
        override val type: String = "server.heartbeat",
        val properties: HeartbeatProperties = HeartbeatProperties()
    ) : ServerEvent
    
    @Serializable
    @SerialName("session.updated")
    @Immutable
    data class SessionUpdated(
        override val type: String = "session.updated",
        val properties: SessionUpdatedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("session.deleted")
    @Immutable
    data class SessionDeleted(
        override val type: String = "session.deleted",
        val properties: SessionDeletedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("session.idle")
    @Immutable
    data class SessionIdle(
        override val type: String = "session.idle",
        val properties: SessionIdleProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("session.error")
    @Immutable
    data class SessionError(
        override val type: String = "session.error",
        val properties: SessionErrorProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("message.updated")
    @Immutable
    data class MessageUpdated(
        override val type: String = "message.updated",
        val properties: MessageUpdatedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("message.removed")
    @Immutable
    data class MessageRemoved(
        override val type: String = "message.removed",
        val properties: MessageRemovedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("message.part.updated")
    @Immutable
    data class MessagePartUpdated(
        override val type: String = "message.part.updated",
        val properties: MessagePartUpdatedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("message.part.removed")
    @Immutable
    data class MessagePartRemoved(
        override val type: String = "message.part.removed",
        val properties: MessagePartRemovedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("permission.updated")
    @Immutable
    data class PermissionUpdated(
        override val type: String = "permission.updated",
        val properties: LegacyPermissionProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("permission.asked")
    @Immutable
    data class PermissionAsked(
        override val type: String = "permission.asked",
        val properties: PermissionAskedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("permission.replied")
    @Immutable
    data class PermissionReplied(
        override val type: String = "permission.replied",
        val properties: PermissionRepliedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("question.asked")
    @Immutable
    data class QuestionAsked(
        override val type: String = "question.asked",
        val properties: QuestionAskedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("question.replied")
    @Immutable
    data class QuestionReplied(
        override val type: String = "question.replied",
        val properties: QuestionRepliedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("file.edited")
    @Immutable
    data class FileEdited(
        override val type: String = "file.edited",
        val properties: FileEditedProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("file.watcher.updated")
    @Immutable
    data class FileWatcherUpdated(
        override val type: String = "file.watcher.updated",
        val properties: FileWatcherProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("installation.updated")
    @Immutable
    data class InstallationUpdated(
        override val type: String = "installation.updated",
        val properties: InstallationProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("lsp.client.diagnostics")
    @Immutable
    data class LspDiagnostics(
        override val type: String = "lsp.client.diagnostics",
        val properties: LspDiagnosticsProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("log")
    @Immutable
    data class Log(
        override val type: String = "log",
        val properties: LogProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("agent.status")
    @Immutable
    data class AgentStatus(
        override val type: String = "agent.status",
        val properties: AgentStatusProperties
    ) : ServerEvent
    
    @Serializable
    @SerialName("session.created")
    @Immutable
    data class SessionCreated(
        override val type: String = "session.created",
        val properties: SessionCreatedProperties
    ) : ServerEvent

    @Serializable
    @SerialName("session.status")
    @Immutable
    data class SessionStatus(
        override val type: String = "session.status",
        val properties: SessionStatusProperties
    ) : ServerEvent

    @Serializable
    @SerialName("session.diff")
    @Immutable
    data class SessionDiff(
        override val type: String = "session.diff",
        val properties: SessionDiffProperties
    ) : ServerEvent

    @Serializable
    @SerialName("session.compacted")
    @Immutable
    data class SessionCompacted(
        override val type: String = "session.compacted",
        val properties: SessionCompactedProperties
    ) : ServerEvent

    @Serializable
    @SerialName("todo.updated")
    @Immutable
    data class TodoUpdated(
        override val type: String = "todo.updated",
        val properties: TodoUpdatedProperties
    ) : ServerEvent

    @Serializable
    @SerialName("question.rejected")
    @Immutable
    data class QuestionRejected(
        override val type: String = "question.rejected",
        val properties: QuestionRejectedProperties
    ) : ServerEvent

    @Serializable
    @SerialName("message.part.delta")
    @Immutable
    data class MessagePartDelta(
        override val type: String = "message.part.delta",
        val properties: MessagePartDeltaProperties
    ) : ServerEvent

    @Serializable
    @SerialName("pty.created")
    @Immutable
    data class PtyCreated(
        override val type: String = "pty.created",
        val properties: PtyEventProperties
    ) : ServerEvent

    @Serializable
    @SerialName("pty.updated")
    @Immutable
    data class PtyUpdated(
        override val type: String = "pty.updated",
        val properties: PtyEventProperties
    ) : ServerEvent

    @Serializable
    @SerialName("pty.exited")
    @Immutable
    data class PtyExited(
        override val type: String = "pty.exited",
        val properties: PtyExitedProperties
    ) : ServerEvent

    @Serializable
    @SerialName("pty.deleted")
    @Immutable
    data class PtyDeleted(
        override val type: String = "pty.deleted",
        val properties: PtyEventProperties
    ) : ServerEvent

    @Serializable
    @SerialName("project.updated")
    @Immutable
    data class ProjectUpdated(
        override val type: String = "project.updated",
        val properties: ProjectUpdatedProperties
    ) : ServerEvent

    @Serializable
    @SerialName("vcs.branch.updated")
    @Immutable
    data class VcsBranchUpdated(
        override val type: String = "vcs.branch.updated",
        val properties: VcsBranchUpdatedProperties
    ) : ServerEvent

    @Serializable
    @SerialName("file.updated")
    @Immutable
    data class FileUpdated(
        override val type: String = "file.updated",
        val properties: FileUpdatedProperties
    ) : ServerEvent

    @Serializable
    @SerialName("lsp.updated")
    @Immutable
    data class LspUpdated(
        override val type: String = "lsp.updated",
        val properties: LspUpdatedProperties
    ) : ServerEvent

    @Serializable
    @SerialName("mcp.tools.changed")
    @Immutable
    data class McpToolsChanged(
        override val type: String = "mcp.tools.changed",
        val properties: McpToolsChangedProperties
    ) : ServerEvent

    @Serializable
    @SerialName("mcp.browser.open.failed")
    @Immutable
    data class McpBrowserOpenFailed(
        override val type: String = "mcp.browser.open.failed",
        val properties: McpBrowserOpenFailedProperties
    ) : ServerEvent

    @Serializable
    @SerialName("worktree.ready")
    @Immutable
    data class WorktreeReady(
        override val type: String = "worktree.ready",
        val properties: WorktreeEventProperties
    ) : ServerEvent

    @Serializable
    @SerialName("worktree.failed")
    @Immutable
    data class WorktreeFailed(
        override val type: String = "worktree.failed",
        val properties: WorktreeEventProperties
    ) : ServerEvent

    @Serializable
    @SerialName("installation.update.available")
    @Immutable
    data class InstallationUpdateAvailable(
        override val type: String = "installation.update.available",
        val properties: InstallationUpdateAvailableProperties
    ) : ServerEvent

    @Serializable
    @SerialName("server.instance.disposed")
    @Immutable
    data class ServerInstanceDisposed(
        override val type: String = "server.instance.disposed",
        val properties: DisposalProperties
    ) : ServerEvent

    @Serializable
    @SerialName("global.disposed")
    @Immutable
    data class GlobalDisposed(
        override val type: String = "global.disposed",
        val properties: DisposalProperties
    ) : ServerEvent

    @Serializable
    @Immutable
    data class Unknown(
        override val type: String,
        val rawData: String? = null
    ) : ServerEvent
}

@Serializable
@Immutable
data class ConnectedProperties(
    val status: String = "connected",
    val version: String = "unknown",
    val timestamp: Long? = null
)

@Serializable
@Immutable
data class HeartbeatProperties(
    val timestamp: Long? = null
)

@Serializable
@Immutable
data class LogProperties(
    val level: String = "info", // "debug", "info", "warn", "error"
    val message: String,
    val sessionID: String? = null,
    val timestamp: Long? = null
)

@Serializable
@Immutable
data class AgentStatusProperties(
    val sessionID: String,
    val agentName: String,
    // status can be a string ("starting", "running", "completed", "error") or an object in newer server versions
    val status: JsonElement? = null,
    val message: String? = null,
    val timestamp: Long? = null
) {
    /** Extract status string regardless of whether server sends a string or an object with a 'value' field. */
    fun statusString(): String = when (val s = status) {
        is JsonPrimitive -> s.content
        else -> s?.toString()?.trim('"') ?: "unknown"
    }
}

@Serializable
@Immutable
data class SessionUpdatedProperties(
    val info: Session
)

@Serializable
@Immutable
data class SessionDeletedProperties(
    val info: Session
)

@Serializable
@Immutable
data class SessionIdleProperties(
    val sessionID: String
)

@Serializable
@Immutable
data class SessionErrorProperties(
    val sessionID: String? = null,
    val error: SessionErrorInfo? = null
)

@Serializable
@Immutable
data class SessionErrorInfo(
    val message: String? = null,
    val code: String? = null
)

@Serializable
@Immutable
data class MessageUpdatedProperties(
    val info: AssistantMessageInfo
)

@Serializable
@Immutable
data class AssistantMessageInfo(
    val id: String,
    val role: String = "assistant",
    val sessionID: String,
    val modelID: String? = null,
    val providerID: String? = null,
    val mode: String? = null,
    val cost: Double? = null,
    val tokens: TokenInfo? = null,
    val time: MessageTimeInfo? = null,
    val system: List<String>? = null,
    val error: JsonElement? = null,
    val summary: JsonElement? = null
)

@Serializable
@Immutable
data class TokenInfo(
    val input: Int = 0,
    val output: Int = 0,
    val reasoning: Int = 0,
    val cache: CacheInfo? = null
)

@Serializable
@Immutable
data class CacheInfo(
    val read: Int = 0,
    val write: Int = 0
)

@Serializable
@Immutable
data class MessageTimeInfo(
    val created: Long = 0,
    val completed: Long? = null
)

@Serializable
@Immutable
data class MessageRemovedProperties(
    val messageID: String,
    val sessionID: String
)

@Serializable
@Immutable
data class MessagePartUpdatedProperties(
    val part: MessagePartInfo,
    val delta: String? = null
)

@Serializable
@Immutable
data class MessagePartInfo(
    val id: String,
    val type: String, // "text", "reasoning", "tool", "file"
    val messageID: String,
    val sessionID: String,
    // Text part
    val text: String? = null,
    val synthetic: Boolean? = null,
    val ignored: Boolean? = null,
    val metadata: Map<String, JsonElement>? = null,
    val time: MessagePartTime? = null,
    // Tool part
    val callID: String? = null,
    val tool: String? = null,
    val state: ToolStateInfo? = null,
    // File part
    val mime: String? = null,
    val url: String? = null,
    val filename: String? = null,
    // Step-finish part fields
    val reason: String? = null,
    val cost: Double? = null,
    val tokens: TokenInfo? = null
)

@Serializable
@Immutable
data class ToolStateInfo(
    val status: String, // "pending", "running", "completed", "error"
    val input: JsonElement? = null,
    val output: String? = null,
    val error: String? = null,
    val title: String? = null,
    val time: ToolTimeInfo? = null,
    val metadata: Map<String, JsonElement>? = null
) {
    /**
     * Convert to RichToolState for UI rendering.
     */
    fun toRichToolState(): RichToolState {
        val inputMap = input?.let { json ->
            try {
                @Suppress("UNCHECKED_CAST")
                when (json) {
                    is kotlinx.serialization.json.JsonObject -> json.mapValues { it.value }
                    else -> emptyMap()
                }
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()
        
        return when (status) {
            "pending" -> RichToolState.Pending()
            "running" -> RichToolState.Running(
                input = inputMap.takeIf { it.isNotEmpty() },
                title = title,
                metadata = metadata,
                startTime = time?.start ?: System.currentTimeMillis()
            )
            "completed" -> RichToolState.Completed(
                input = inputMap,
                output = output ?: "",
                title = title ?: "",
                metadata = metadata ?: emptyMap(),
                startTime = time?.start ?: 0L,
                endTime = time?.end ?: System.currentTimeMillis()
            )
            "error" -> RichToolState.Error(
                input = inputMap,
                error = error ?: "Unknown error",
                metadata = metadata,
                startTime = time?.start ?: 0L,
                endTime = time?.end ?: System.currentTimeMillis()
            )
            else -> RichToolState.Pending()
        }
    }
}

@Serializable
@Immutable
data class ToolTimeInfo(
    val start: Long? = null,
    val end: Long? = null
)

@Serializable
@Immutable
data class MessagePartRemovedProperties(
    val messageID: String,
    val partID: String
)

@Serializable
@Immutable
data class LegacyPermissionProperties(
    val id: String,
    val sessionID: String,
    val title: String,
    val metadata: JsonElement? = null,
    val time: PermissionTimeInfo? = null
)

/**
 * Properties for permission.asked events.
 */
@Serializable
@Immutable
data class PermissionAskedProperties(
    val id: String,
    @SerialName("sessionID")
    val sessionID: String,
    val permission: String,
    val patterns: List<String> = emptyList(),
    val metadata: Map<String, JsonElement> = emptyMap(),
    val always: List<String> = emptyList(),
    val tool: PermissionToolInfo? = null
)

/**
 * Properties for permission.replied event.
 */
@Serializable
@Immutable
data class PermissionRepliedProperties(
    @SerialName("sessionID")
    val sessionID: String,
    @SerialName("requestID")
    val requestID: String,
    val reply: String // "once", "always", "reject"
)

@Serializable
@Immutable
data class PermissionTimeInfo(
    val created: Long = 0
)

/**
 * Tool info associated with a permission request.
 */
@Serializable
@Immutable
data class PermissionToolInfo(
    @SerialName("messageID")
    val messageId: String,
    @SerialName("callID")
    val callId: String
)

// ==== Question Events ====

/**
 * Properties for question.asked events.
 */
@Serializable
@Immutable
data class QuestionAskedProperties(
    val id: String,
    @SerialName("sessionID")
    val sessionID: String,
    val questions: List<QuestionInfo> = emptyList(),
    val tool: PermissionToolInfo? = null
)

/**
 * Properties for question.replied event.
 */
@Serializable
@Immutable
data class QuestionRepliedProperties(
    @SerialName("sessionID")
    val sessionID: String,
    @SerialName("requestID")
    val requestID: String,
    val answers: List<List<String>> = emptyList()
)

/**
 * Individual question in a question request.
 */
@Serializable
@Immutable
data class QuestionInfo(
    val question: String,
    val header: String = "",
    val options: List<QuestionOption> = emptyList(),
    val multiple: Boolean = false
)

/**
 * Option for a question.
 */
@Serializable
@Immutable
data class QuestionOption(
    val label: String,
    val description: String = ""
)

/**
 * Question request shown to user for interactive input.
 * Matches OpenCode question request payloads.
 */
@Serializable
@Immutable
data class QuestionRequest(
    val id: String,
    @SerialName("sessionID")
    val sessionId: String,
    val questions: List<QuestionInfo> = emptyList(),
    val tool: PermissionToolInfo? = null
) {
    companion object {
        fun fromEvent(event: ServerEvent.QuestionAsked): QuestionRequest {
            return QuestionRequest(
                id = event.properties.id,
                sessionId = event.properties.sessionID,
                questions = event.properties.questions,
                tool = event.properties.tool
            )
        }
    }
}

@Serializable
@Immutable
data class FileEditedProperties(
    val file: String
)

@Serializable
@Immutable
data class FileWatcherProperties(
    val event: String, // "rename" or "change"
    val file: String
)

@Serializable
@Immutable
data class InstallationProperties(
    val version: String
)

@Serializable
@Immutable
data class LspDiagnosticsProperties(
    val path: String,
    val serverID: String
)

// Permission request shown to the user for tool approval.
@Serializable
@Immutable
data class PermissionRequest(
    val id: String,
    @SerialName("sessionID")
    val sessionId: String,
    val permission: String,
    val patterns: List<String> = emptyList(),
    val metadata: Map<String, JsonElement> = emptyMap(),
    val always: List<String> = emptyList(),
    val tool: PermissionToolInfo? = null
) {
    companion object {
        fun fromEvent(event: ServerEvent.PermissionAsked): PermissionRequest {
            return PermissionRequest(
                id = event.properties.id,
                sessionId = event.properties.sessionID,
                permission = event.properties.permission,
                patterns = event.properties.patterns,
                metadata = event.properties.metadata,
                always = event.properties.always,
                tool = event.properties.tool
            )
        }

        fun fromLegacyEvent(event: ServerEvent.PermissionUpdated): PermissionRequest {
            return PermissionRequest(
                id = event.properties.id,
                sessionId = event.properties.sessionID,
                permission = event.properties.title,
                patterns = emptyList(),
                metadata = emptyMap(),
                always = emptyList(),
                tool = null
            )
        }
    }
}

// Permission response types accepted by OpenCode.
enum class PermissionResponseType(val value: String) {
    ONCE("once"),
    ALWAYS("always"),
    REJECT("reject")
}

// ==== New SSE Event Properties ====

@Serializable
@Immutable
data class SessionCreatedProperties(
    val info: Session
)

@Serializable
@Immutable
data class SessionStatusProperties(
    val sessionID: String,
    // status can be a string or object in newer server versions
    val status: JsonElement? = null
) {
    fun statusString(): String = when (val s = status) {
        is JsonPrimitive -> s.content
        else -> s?.toString()?.trim('"') ?: "idle"
    }
}

@Serializable
@Immutable
data class SessionDiffProperties(
    val sessionID: String,
    val added: Int = 0,
    val removed: Int = 0
)

@Serializable
@Immutable
data class SessionCompactedProperties(
    val sessionID: String
)

@Serializable
@Immutable
data class TodoUpdatedProperties(
    val sessionID: String,
    val todos: List<TodoInfo> = emptyList(),
    // Legacy V1 field — single todo item. Kept for backward compatibility.
    val todo: TodoItem? = null
)

@Serializable
@Immutable
data class TodoInfo(
    val content: String,
    val status: String = "pending", // "pending", "in_progress", "completed", "cancelled"
    val priority: String = "medium" // "high", "medium", "low"
)

@Serializable
@Immutable
data class TodoItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: String = "pending", // "pending", "in_progress", "completed", "cancelled"
    val priority: String = "medium"
)

@Serializable
@Immutable
data class QuestionRejectedProperties(
    val sessionID: String,
    val requestID: String
)

@Serializable
@Immutable
data class MessagePartDeltaProperties(
    val sessionID: String,
    val messageID: String,
    val partID: String,
    val field: String = "text",
    val delta: String
)

@Serializable
@Immutable
data class PtyEventProperties(
    val ptyID: String,
    val sessionID: String? = null,
    val title: String? = null
)

@Serializable
@Immutable
data class PtyExitedProperties(
    val ptyID: String,
    val exitCode: Int = 0
)

@Serializable
@Immutable
data class ProjectUpdatedProperties(
    val projectID: String,
    val path: String? = null
)

@Serializable
@Immutable
data class VcsBranchUpdatedProperties(
    val branch: String,
    val projectID: String? = null
)

@Serializable
@Immutable
data class FileUpdatedProperties(
    val path: String,
    val event: String = "change" // "change", "rename"
)

@Serializable
@Immutable
data class LspUpdatedProperties(
    val serverID: String,
    val status: String
)

@Serializable
@Immutable
data class McpToolsChangedProperties(
    val serverName: String,
    val toolCount: Int = 0
)

@Serializable
@Immutable
data class McpBrowserOpenFailedProperties(
    val serverName: String,
    val error: String? = null
)

@Serializable
@Immutable
data class WorktreeEventProperties(
    val id: String,
    val path: String? = null,
    val error: String? = null
)

@Serializable
@Immutable
data class InstallationUpdateAvailableProperties(
    val version: String,
    val releaseUrl: String? = null
)

@Serializable
@Immutable
data class DisposalProperties(
    val reason: String? = null,
    val timestamp: Long? = null
)

