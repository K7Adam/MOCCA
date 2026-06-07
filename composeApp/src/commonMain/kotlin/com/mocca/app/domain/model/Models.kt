package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Immutable
@Serializable
data class Session(
    val id: String,
    val title: String? = null,
    val slug: String? = null,
    val time: SessionTime? = null,
    val status: SessionStatus = SessionStatus.IDLE,
    val version: String? = null,
    @SerialName("projectID")
    val projectID: String? = null,
    val directory: String? = null,
    @SerialName("parentID")
    val parentID: String? = null,
    @SerialName("parentId")
    val parentId: String? = null,
    // summary can be a SessionSummary object or null depending on server version
    val summary: JsonElement? = null,
    val permission: List<SessionPermission>? = null,
    val revert: SessionRevertInfo? = null,
    @SerialName("shareID")
    val shareID: String? = null,
    val lastFetchedAt: Long? = null
) {
    // Convenience accessors for time fields
    val createdAt: Long get() = time?.created ?: 0L
    val updatedAt: Long get() = time?.updated ?: 0L
    val effectiveParentID: String? get() = parentID ?: parentId
    val isReverted: Boolean get() = revert != null
    val isShared: Boolean get() = shareID != null

    /** Extract files count from summary JsonElement, regardless of server shape. */
    val summaryFiles: Int get() {
        val obj = summary as? kotlinx.serialization.json.JsonObject ?: return 0
        return (obj["files"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 0
    }
}

@Serializable
@Immutable
data class SessionPermission(
    val permission: String,
    val action: String,
    val pattern: String? = null
)

@Serializable
@Immutable
data class SessionTime(
    val created: Long = 0,
    val updated: Long = 0
)

@Serializable
@Immutable
data class SessionSummary(
    val additions: Int = 0,
    val deletions: Int = 0,
    val files: Int = 0
)

@Serializable
enum class SessionStatus {
    @SerialName("idle")
    IDLE,
    @SerialName("running")
    RUNNING,
    @SerialName("completed")
    COMPLETED,
    @SerialName("error")
    ERROR
}

/**
 * Message as returned from GET /session/:id/message
 * This is the response format with info + parts.
 */
@Serializable
@Immutable
data class MessageResponse(
    val info: MessageInfo,
    val parts: List<MessagePartResponse> = emptyList()
)

@Serializable
@Immutable
data class MessageInfo(
    val id: String,
    val role: MessageRole,
    @SerialName("sessionID")
    val sessionID: String,
    val time: MessageTime? = null,
    // Summary can be a MessageSummary object OR a boolean - use JsonElement to handle both
    val summary: JsonElement? = null,
    val agent: String? = null,
    val model: ModelInfo? = null,
    val variant: String? = null,
    // Tool toggles (which tools are enabled for this message)
    val tools: Map<String, Boolean>? = null,
    // For assistant messages
    val cost: Double? = null,
    val tokens: TokenUsage? = null,
    // system can be a string or List<String> depending on server version - use JsonElement to handle both
    val system: JsonElement? = null,
    val error: JsonElement? = null
) {
    /**
     * Effective variant, preferring the nested model.variant path.
     * The flat variant field is retained for backward compatibility with older server responses.
     */
    val effectiveVariant: String? get() = model?.variant ?: variant

    /**
     * Get the summary title, handling both object and boolean formats.
     * Returns null if summary is boolean or missing.
     */
    fun getSummaryTitle(): String? {
        val json = summary ?: return null
        return when {
            json is kotlinx.serialization.json.JsonObject -> {
                json["title"]?.let { 
                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null 
                }
            }
            else -> null // Boolean or other primitive
        }
    }
}

@Serializable
@Immutable
data class ModelInfo(
    @SerialName("providerID")
    val providerID: String? = null,
    @SerialName("modelID")
    val modelID: String? = null,
    /** Nested variant for current OpenCode responses (model.variant shape). */
    val variant: String? = null
)

@Serializable
@Immutable
data class MessageSummary(
    val title: String? = null,
    val diffs: List<JsonElement>? = null
)

@Serializable
@Immutable
data class TokenUsage(
    val input: Int = 0,
    val output: Int = 0,
    val reasoning: Int = 0,
    val cache: CacheUsage? = null
) {
    val hasVisibleDetails: Boolean
        get() = input > 0 ||
            output > 0 ||
            reasoning > 0 ||
            cache?.let { it.read > 0 || it.write > 0 } == true
}

@Serializable
@Immutable
data class CacheUsage(
    val read: Int = 0,
    val write: Int = 0
)

@Serializable
@Immutable
data class MessageTime(
    val created: Long = 0,
    val completed: Long? = null
)

private fun MessageTime.durationMs(): Long? =
    completed?.let { end -> created.takeIf { it > 0 }?.let { start -> end - start } }

@Serializable
@Immutable
data class MessagePartTime(
    val start: Long = 0,
    val end: Long? = null
)

private fun MessagePartTime.durationMs(): Long? =
    end?.let { finishedAt -> start.takeIf { it > 0 }?.let { startedAt -> finishedAt - startedAt } }

/**
 * Message part as returned from GET /session/:id/message.
 * This is a flat structure with type discriminator.
 * Types: "text", "reasoning", "tool", "file", "step-start", "step-finish"
 */
@Serializable
@Immutable
data class MessagePartResponse(
    val id: String,
    @SerialName("sessionID")
    val sessionID: String,
    @SerialName("messageID")
    val messageID: String,
    val type: String, // "text", "reasoning", "tool", "file", "step-start", "step-finish"
    // Text part fields
    val text: String? = null,
    // Tool part fields
    @SerialName("callID")
    val callID: String? = null,
    val tool: String? = null,
    val state: ToolStateResponse? = null,
    // File part fields
    val mime: String? = null,
    val url: String? = null,
    val filename: String? = null,
    val source: JsonElement? = null,
    // Step-finish fields
    val reason: String? = null,
    val cost: Double? = null,
    val tokens: TokenUsage? = null,
    // Time fields for part lifecycle (OpenCode uses start/end here)
    val time: MessagePartTime? = null
)

@Serializable
@Immutable
data class ToolStateResponse(
    val status: String, // "pending", "running", "completed", "error"
    val input: JsonElement? = null,
    val output: String? = null,
    val error: String? = null,
    val title: String? = null,
    val time: ToolTimeResponse? = null
)

@Serializable
@Immutable
data class ToolTimeResponse(
    val start: Long? = null,
    val end: Long? = null
)

@Serializable
enum class MessageRole {
    @SerialName("user")
    USER,
    @SerialName("assistant")
    ASSISTANT,
    @SerialName("system")
    SYSTEM
}

/**
 * Request body for POST /session/:id/message (chat).
 */
@Serializable
@Immutable
data class ChatRequest(
    @SerialName("modelID")
    val modelID: String,
    @SerialName("providerID")
    val providerID: String,
    val parts: List<ChatPart>,
    @SerialName("messageID")
    val messageID: String? = null,
    val mode: String? = null,
    val variant: String? = null,
    val system: String? = null,
    val tools: Map<String, Boolean>? = null
)

/**
 * Parts that can be sent in a chat request.
 */
@Serializable
sealed class ChatPart {
    @Serializable
    @SerialName("text")
    @Immutable
    data class Text(
        val text: String,
        val id: String? = null,
        val synthetic: Boolean? = null
    ) : ChatPart()
    
    @Serializable
    @SerialName("file")
    @Immutable
    data class File(
        val mime: String,
        val url: String,
        val filename: String? = null
    ) : ChatPart()
}

/**
 * Request body for POST /permission/:requestId/reply.
 * Matches the OpenCode permission.reply API.
 */
@Serializable
@Immutable
data class PermissionReplyRequest(
    @SerialName("requestID")
    val requestID: String,
    val reply: String, // "once", "always", "reject"
    val message: String? = null
)

/**
 * Request body for POST /question/:requestId/reply.
 * Matches the OpenCode question.reply API.
 */
@Serializable
@Immutable
data class QuestionReplyRequest(
    @SerialName("requestID")
    val requestID: String,
    val answers: List<List<String>>
)

/**
 * Request body for POST /question/:requestId/reject.
 */
@Serializable
@Immutable
data class QuestionRejectRequest(
    @SerialName("requestID")
    val requestID: String
)

/**
 * Request body for POST /session/:id/fork.
 */
@Serializable
@Immutable
data class ForkSessionRequest(
    @SerialName("messageID")
    val messageID: String? = null
)

/**
 * Request body for POST /session/:id/revert.
 */
@Serializable
@Immutable
data class RevertSessionRequest(
    @SerialName("messageID")
    val messageID: String,
    @SerialName("partID")
    val partID: String? = null
)

// ==== Legacy Message type for UI compatibility ====

/**
 * Simplified message representation for UI.
 * Converted from MessageResponse.
 */
@Immutable
@Serializable
data class Message(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val parts: List<MessagePart>,
    val createdAt: Long,
    val model: String? = null,
    val cost: Double? = null,
    val tokens: TokenUsage? = null,
    val isRead: Boolean = true,
    val metadata: String? = null,
    val isStreaming: Boolean = false
) {
    companion object {
        fun fromResponse(response: MessageResponse): Message {
            val finishPart = response.parts.lastOrNull { it.type == "step-finish" }
            return Message(
                id = response.info.id,
                sessionId = response.info.sessionID,
                role = response.info.role,
                parts = response.parts.mapNotNull { part ->
                    when (part.type) {
                        "text" -> part.text?.let { MessagePart.Text(id = part.id, text = it) }
                        "reasoning" -> part.text?.let {
                            MessagePart.Reasoning(
                                content = it,
                                timeMs = part.time?.durationMs() ?: 0L,
                                id = part.id
                            )
                        }
                        "thinking" -> part.text?.let { 
                            MessagePart.Thinking(
                                content = it,
                                durationMs = part.time?.durationMs(),
                                id = part.id
                            ) 
                        }
                        "tool" -> {
                            val richState = RichToolState.fromResponse(part.state)
                            MessagePart.ToolInvocation(
                                id = part.callID ?: part.id,
                                name = part.tool ?: "unknown",
                                input = part.state?.input?.toString() ?: "",
                                state = richState.status,
                                richState = richState,
                                output = part.state?.output,
                                error = part.state?.error,
                                title = part.state?.title
                            )
                        }
                        "file" -> MessagePart.File(
                            mediaType = part.mime ?: "application/octet-stream",
                            url = part.url,
                            filename = part.filename
                        )
                        // Skip step-start and step-finish - these are internal markers
                        "step-start", "step-finish" -> null
                        else -> null // Skip unknown types
                    }
                },
                createdAt = response.info.time?.created ?: 0L,
                model = response.info.model?.modelID,
                cost = response.info.cost ?: finishPart?.cost,
                tokens = response.info.tokens ?: finishPart?.tokens,
                isRead = true,
                metadata = null,
                isStreaming = false
            )
        }
    }
}

@Serializable
sealed interface MessagePart {
    @Serializable
    @Immutable
    data class Text(val id: String = "", val text: String) : MessagePart

    @Serializable
    @Immutable
    data class Reasoning(
        val content: String,
        val timeMs: Long,
        val id: String = ""
    ) : MessagePart

    /**
     * Legacy alias for older streams that used "thinking" instead of OpenCode's "reasoning".
     */
    @Serializable
    @Immutable
    data class Thinking(
        val content: String,
        val durationMs: Long? = null,
        val id: String = ""
    ) : MessagePart

    @Serializable
    @Immutable
    data class ToolInvocation(
        val id: String,
        val name: String,
        val input: String,
        val state: ToolState = ToolState.PENDING,
        /** Rich state with timing, output, error info - for detailed tool cards */
        @kotlinx.serialization.Transient
        val richState: RichToolState = RichToolState.Pending(),
        /** Tool output (for completed tools) */
        val output: String? = null,
        /** Error message (for error tools) */
        val error: String? = null,
        /** Display title generated by the tool */
        val title: String? = null
    ) : MessagePart

    @Serializable
    @Immutable
    data class ToolResult(
        val id: String,
        val result: String
    ) : MessagePart

    @Serializable
    @Immutable
    data class File(
        val mediaType: String,
        val url: String? = null,
        val filename: String? = null
    ) : MessagePart

    @Serializable
    @Immutable
    data class SubTask(
        val sessionId: String,
        val title: String,
        val status: SessionStatus,
        val messages: List<Message> = emptyList(),
        val streamingText: String = ""
    ) : MessagePart
}

enum class ToolState {
    PENDING,
    RUNNING,
    COMPLETED,
    ERROR
}

/**
 * Rich tool state matching OpenCode tool-state payloads.
 * Use this for detailed tool card rendering.
 */
sealed interface RichToolState {
    val status: ToolState
    
    @Immutable
    
    data class Pending(
        override val status: ToolState = ToolState.PENDING
    ) : RichToolState
    
    @Immutable
    
    data class Running(
        override val status: ToolState = ToolState.RUNNING,
        val input: Map<String, JsonElement>? = null,
        val title: String? = null,
        val metadata: Map<String, JsonElement>? = null,
        val startTime: Long
    ) : RichToolState
    
    @Immutable
    
    data class Completed(
        override val status: ToolState = ToolState.COMPLETED,
        val input: Map<String, JsonElement>,
        val output: String,
        val title: String,
        val metadata: Map<String, JsonElement> = emptyMap(),
        val startTime: Long,
        val endTime: Long
    ) : RichToolState {
        val durationMs: Long get() = endTime - startTime
    }
    
    @Immutable
    
    data class Error(
        override val status: ToolState = ToolState.ERROR,
        val input: Map<String, JsonElement>,
        val error: String,
        val metadata: Map<String, JsonElement>? = null,
        val startTime: Long,
        val endTime: Long
    ) : RichToolState {
        val durationMs: Long get() = endTime - startTime
    }
    
    companion object {
        /**
         * Parse ToolStateResponse to RichToolState.
         */
        fun fromResponse(response: ToolStateResponse?): RichToolState {
            if (response == null) return Pending()
            
            val inputMap = response.input?.let { json ->
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
            
            return when (response.status) {
                "pending" -> Pending()
                "running" -> Running(
                    input = inputMap.takeIf { it.isNotEmpty() },
                    title = response.title,
                    metadata = null,
                    startTime = response.time?.start ?: System.currentTimeMillis()
                )
                "completed" -> Completed(
                    input = inputMap,
                    output = response.output ?: "",
                    title = response.title ?: "",
                    metadata = emptyMap(),
                    startTime = response.time?.start ?: 0L,
                    endTime = response.time?.end ?: System.currentTimeMillis()
                )
                "error" -> Error(
                    input = inputMap,
                    error = response.error ?: "Unknown error",
                    metadata = null,
                    startTime = response.time?.start ?: 0L,
                    endTime = response.time?.end ?: System.currentTimeMillis()
                )
                else -> Pending()
            }
        }
    }
}

@Serializable
@Immutable
data class AppInfo(
    val version: String,
    val initialized: Boolean = false,
    val healthy: Boolean = true
)

@Serializable
@Immutable
data class Provider(
    val id: String,
    val name: String,
    val models: Map<String, Model> = emptyMap()
)

@Serializable
@Immutable
data class ProvidersResponse(
    val providers: List<Provider> = emptyList()
)

@Serializable
@Immutable
data class Model(
    val id: String,
    val name: String,
    val variants: Map<String, ModelVariant>? = null // Optional map of variant configurations
)

@Serializable
@Immutable
data class ModelVariant(
    val description: String? = null,
    // Add other variant fields if needed, but the key (e.g., "high", "low") is the main identifier
    // For now, variants seem to be defined in config as just keys with options, 
    // but the API might return them.
    // The web search indicates variants are configured in opencode.jsonc.
    // Let's assume for now we just need the IDs (keys of the map).
)

@Serializable
@Immutable
data class Mode(
    val id: String,
    val name: String,
    val description: String? = null
)

@Immutable
@Serializable
data class FileInfo(
    val name: String,
    val path: String,
    val type: String, // "file" or "directory"
    val size: Long? = null,
    /**
     * Modified timestamp in milliseconds with optional fractional part.
     * The bridge returns timestamps with fractional milliseconds (e.g., 1776372610442.404),
     * so this is typed as Double to accept both integer and decimal JSON numbers.
     * Use [modifiedAtMillis] for integer millisecond precision.
     */
    @SerialName("updated")
    val modifiedAt: Double? = null
) {
    val isDirectory: Boolean get() = type == "directory"

    /**
     * Modified timestamp as integer milliseconds (truncates fractional part).
     * Use this for display, sorting, or any logic requiring Long millisecond precision.
     */
    val modifiedAtMillis: Long? get() = modifiedAt?.toLong()
}

@Serializable
@Immutable
data class FileContent(
    val path: String = "",
    val content: String,
    val language: String? = null
)

@Serializable
@Immutable
data class FileUpdateRequest(
    val path: String,
    val content: String
)

@Serializable
@Immutable
data class FileStatus(
    val path: String,
    val gitStatus: GitStatus? = null,
    val diagnostics: List<Diagnostic> = emptyList()
)

@Serializable
@Immutable
data class GitStatus(
    val status: String,
    val staged: Boolean = false
)

@Serializable
@Immutable
data class Diagnostic(
    val severity: DiagnosticSeverity,
    val message: String,
    val line: Int,
    val column: Int
)

@Serializable
enum class DiagnosticSeverity {
    @SerialName("error")
    ERROR,
    @SerialName("warning")
    WARNING,
    @SerialName("information")
    INFORMATION,
    @SerialName("hint")
    HINT
}

@Immutable
@Serializable
data class SearchResult(
    val file: String,
    val line: Int,
    val column: Int,
    val match: String,
    val context: String
)

@Immutable
@Serializable
data class SymbolResult(
    val name: String,
    val kind: String,
    val file: String,
    val line: Int
)

@Immutable
@Serializable
data class Terminal(
    val id: String,
    val shell: String? = null
)

@Serializable
@Immutable
data class TerminalResizeRequest(
    val cols: Int,
    val rows: Int
)

/**
 * Response from /config endpoint.
 */
@Serializable
@Immutable
data class ConfigResponse(
    val model: String? = null, // Default model (e.g., "anthropic/claude-sonnet-4-5")
    val providers: List<Provider> = emptyList(),
    val agents: List<AgentConfig> = emptyList(),
    val modes: List<Mode> = emptyList()
)

/**
 * Agent configuration from OpenCode.
 */
@Serializable
@Immutable
data class AgentConfig(
    val id: String,
    val name: String,
    val description: String? = null
)

/**
 * Session status info from /session/status endpoint.
 * Matches OpenCode session status types.
 */
@Serializable
@Immutable
data class SessionStatusInfo(
    val type: String, // "idle", "busy", "retry"
    val attempt: Int? = null,
    val message: String? = null,
    val next: Long? = null
) {
    val isIdle: Boolean get() = type == "idle"
    val isBusy: Boolean get() = type == "busy"
    val isRetrying: Boolean get() = type == "retry"
}

/**
 * Session revert state (embedded in Session when reverted).
 * Used for undo/redo functionality.
 */
@Serializable
@Immutable
data class SessionRevertInfo(
    @SerialName("messageID")
    val messageID: String
)

/**
 * Generic server error response structure.
 * Used to parse error bodies when the server returns a non-200 status or unexpected JSON.
 */
@Serializable
@Immutable
data class ServerErrorResponse(
    val name: String? = null,
    val message: String? = null,
    val data: JsonElement? = null,
    val code: String? = null,
    val status: Int? = null
)

/**
 * Session todo item.
 * Used for tracking agent task progress.
 */
@Immutable
@Serializable
data class Todo(
    val id: String? = null,  // Server may not always include id
    val content: String,
    val status: TodoStatus,
    val priority: TodoPriority,
    val createdAt: Long? = null,
    val completedAt: Long? = null
)

@Serializable
enum class TodoStatus {
    @SerialName("pending") PENDING,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("completed") COMPLETED,
    @SerialName("cancelled") CANCELLED
}

@Serializable
enum class TodoPriority {
    @SerialName("high") HIGH,
    @SerialName("medium") MEDIUM,
    @SerialName("low") LOW
}

// SESSION OPERATIONS


/**
 * Request body for POST /session/:id/summarize.
 */
@Serializable
@Immutable
data class SummarizeRequest(
    @SerialName("providerID")
    val providerID: String,
    @SerialName("modelID")
    val modelID: String
)

/**
 * Request body for POST /session/:id/init.
 */
@Serializable
@Immutable
data class InitSessionRequest(
    @SerialName("messageID")
    val messageID: String,
    @SerialName("providerID")
    val providerID: String,
    @SerialName("modelID")
    val modelID: String
)

// CONFIG UPDATE


/**
 * Request body for PATCH /config.
 */
@Serializable
@Immutable
data class ConfigUpdate(
    val model: ModelConfig? = null,
    val provider: String? = null,
    val theme: String? = null,
    val autosave: Boolean? = null,
    val experimental: Map<String, JsonElement>? = null
)

@Serializable
@Immutable
data class ModelConfig(
    val default: String? = null,
    val reasoning: String? = null
)

// PROJECT MANAGEMENT


/**
 * Project information from /project endpoints.
 * Note: Fields are nullable to handle various API response formats.
 */
@Serializable
@Immutable
data class Project(
    val id: String? = null,
    val name: String? = null,
    val path: String? = null,
    val directory: String? = null,
    val worktree: String? = null,
    val vcs: VcsType? = null,
    val createdAt: Long? = null
) {
    /** Display name with fallback to path or directory */
    val displayName: String
        get() = name 
            ?: worktree?.substringAfterLast('/')?.substringAfterLast('\\') // Handle both separators
            ?: path?.substringAfterLast('/')?.substringAfterLast('\\') 
            ?: directory?.substringAfterLast('/')?.substringAfterLast('\\') 
            ?: "Unknown Project"
}

@Serializable
enum class VcsType {
    @SerialName("git") GIT,
    @SerialName("none") NONE
}

/**
 * Path information from /path endpoint.
 */
@Serializable
@Immutable
data class PathInfo(
    val cwd: String,
    val home: String? = null,
    val root: String? = null
)

// LOGGING


/**
 * Request body for POST /log.
 */
@Serializable
@Immutable
data class LogEntry(
    val service: String,
    val level: String, // "debug", "info", "warn", "error"
    val message: String,
    val extra: Map<String, String>? = null
)

// TOOLS


/**
 * Full tool list response.
 */
@Serializable
@Immutable
data class ToolList(
    val tools: List<ToolDefinition> = emptyList()
)

@Serializable
@Immutable
data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String? = null,
    val schema: JsonElement? = null,
    val category: String? = null
)

// SESSION GROUPING MODELS


/**
 * Represents a parent session with its child sessions (forks/sub-agents).
 * Used for displaying grouped session hierarchy in the conversation list.
 */
@Immutable
data class SessionGroup(
    val parent: Session,
    val children: List<Session> = emptyList(),
    val isExpanded: Boolean = false
) {
    /** Check if any session in this group (parent or children) is currently running */
    val hasRunningSession: Boolean
        get() = parent.status == SessionStatus.RUNNING || 
                children.any { it.status == SessionStatus.RUNNING }
    
    /** Get all running sessions in this group */
    val runningSessions: List<Session>
        get() = listOfNotNull(parent.takeIf { it.status == SessionStatus.RUNNING }) +
                children.filter { it.status == SessionStatus.RUNNING }
    
    /** Total count including parent and children */
    val totalCount: Int get() = 1 + children.size
    
    /** Most recent activity time across all sessions in group */
    val lastActivityTime: Long
        get() = maxOf(parent.updatedAt, children.maxOfOrNull { it.updatedAt } ?: 0L)
}

/**
 * Extended session status with real-time running state from SSE.
 * Combines static SessionStatus with live SessionStatusInfo.
 */
@Immutable
data class SessionRunningState(
    val sessionId: String,
    val isRunning: Boolean = false,
    val statusType: String = "idle", // "idle", "busy", "retry"
    val statusMessage: String? = null,
    val retryAttempt: Int? = null,
    val nextRetryTime: Long? = null
) {
    val isBusy: Boolean get() = statusType == "busy"
    val isRetrying: Boolean get() = statusType == "retry"
    val isIdle: Boolean get() = statusType == "idle"
    
    companion object {
        fun fromSessionStatusInfo(sessionId: String, info: SessionStatusInfo): SessionRunningState {
            return SessionRunningState(
                sessionId = sessionId,
                isRunning = info.isBusy || info.isRetrying,
                statusType = info.type,
                statusMessage = info.message,
                retryAttempt = info.attempt,
                nextRetryTime = info.next
            )
        }
    }
}

/**
 * Request to update a PTY/terminal (PUT /pty/:ptyID).
 */
@Serializable
@Immutable
data class PtyUpdateRequest(
    val title: String? = null,
    val cols: Int? = null,
    val rows: Int? = null
)

/**
 * Request to update a project (PATCH /project/:projectID).
 */
@Serializable
@Immutable
data class ProjectUpdateRequest(
    val path: String? = null
)

/**
 * Cross-project session from GET /experimental/session.
 */
@Serializable
@Immutable
data class CrossProjectSession(
    val id: String,
    val projectID: String,
    val projectPath: String? = null,
    val title: String? = null,
    val createdAt: Long? = null
)
