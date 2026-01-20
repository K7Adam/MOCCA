package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
    val summary: SessionSummary? = null,
    val permission: List<SessionPermission>? = null, // Tool permissions
    val revert: SessionRevertInfo? = null, // Revert state for undo/redo
    @SerialName("shareID")
    val shareID: String? = null // Share ID for public sharing
) {
    // Convenience accessors for time fields
    val createdAt: Long get() = time?.created ?: 0L
    val updatedAt: Long get() = time?.updated ?: 0L
    val effectiveParentID: String? get() = parentID ?: parentId
    val isReverted: Boolean get() = revert != null
    val isShared: Boolean get() = shareID != null
}

@Serializable
data class SessionPermission(
    val permission: String,
    val action: String,
    val pattern: String? = null
)

@Serializable
data class SessionTime(
    val created: Long = 0,
    val updated: Long = 0
)

@Serializable
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
data class MessageResponse(
    val info: MessageInfo,
    val parts: List<MessagePartResponse> = emptyList()
)

@Serializable
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
    val system: List<String>? = null,
    val error: JsonElement? = null
) {
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
data class ModelInfo(
    @SerialName("providerID")
    val providerID: String? = null,
    @SerialName("modelID")
    val modelID: String? = null
)

@Serializable
data class MessageSummary(
    val title: String? = null,
    val diffs: List<JsonElement>? = null
)

@Serializable
data class TokenUsage(
    val input: Int = 0,
    val output: Int = 0,
    val reasoning: Int = 0,
    val cache: CacheUsage? = null
)

@Serializable
data class CacheUsage(
    val read: Int = 0,
    val write: Int = 0
)

@Serializable
data class MessageTime(
    val created: Long = 0,
    val completed: Long? = null
)

/**
 * Message part as returned from GET /session/:id/message.
 * This is a flat structure with type discriminator.
 * Types: "text", "tool", "file", "step-start", "step-finish"
 */
@Serializable
data class MessagePartResponse(
    val id: String,
    @SerialName("sessionID")
    val sessionID: String,
    @SerialName("messageID")
    val messageID: String,
    val type: String, // "text", "tool", "file", "step-start", "step-finish"
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
    // Time fields (for step-start/finish)
    val time: MessageTime? = null
)

@Serializable
data class ToolStateResponse(
    val status: String, // "pending", "running", "completed", "error"
    val input: JsonElement? = null,
    val output: String? = null,
    val error: String? = null,
    val title: String? = null,
    val time: ToolTimeResponse? = null
)

@Serializable
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
data class ChatRequest(
    @SerialName("modelID")
    val modelID: String,
    @SerialName("providerID")
    val providerID: String,
    val parts: List<ChatPart>,
    @SerialName("messageID")
    val messageID: String? = null,
    val mode: String? = null,
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
    data class Text(
        val text: String,
        val id: String? = null,
        val synthetic: Boolean? = null
    ) : ChatPart()
    
    @Serializable
    @SerialName("file")
    data class File(
        val mime: String,
        val url: String,
        val filename: String? = null
    ) : ChatPart()
}

/**
 * Request body for POST /session/:id/permissions/:permissionId (legacy format).
 */
@Serializable
data class PermissionResponse(
    val response: String, // "allow" or "deny"
    val remember: Boolean = false
)

/**
 * Request body for POST /permission/:requestId/reply.
 * Matches OpenChamber SDK permission.reply API.
 */
@Serializable
data class PermissionReplyRequest(
    @SerialName("requestID")
    val requestID: String,
    val reply: String, // "once", "always", "reject"
    val message: String? = null
)

/**
 * Request body for POST /question/:requestId/reply.
 * Matches OpenChamber SDK question.reply API.
 */
@Serializable
data class QuestionReplyRequest(
    @SerialName("requestID")
    val requestID: String,
    val answers: List<List<String>>
)

/**
 * Request body for POST /question/:requestId/reject.
 */
@Serializable
data class QuestionRejectRequest(
    @SerialName("requestID")
    val requestID: String
)

/**
 * Request body for POST /session/:id/fork.
 */
@Serializable
data class ForkSessionRequest(
    @SerialName("messageID")
    val messageID: String? = null
)

/**
 * Request body for POST /session/:id/revert.
 */
@Serializable
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
    val cost: Double? = null
) {
    companion object {
        fun fromResponse(response: MessageResponse): Message {
            return Message(
                id = response.info.id,
                sessionId = response.info.sessionID,
                role = response.info.role,
                parts = response.parts.mapNotNull { part ->
                    when (part.type) {
                        "text" -> part.text?.let { MessagePart.Text(it) }
                        "thinking" -> part.text?.let { 
                            MessagePart.Thinking(
                                content = it,
                                durationMs = part.time?.completed?.let { end -> 
                                    part.time.created.takeIf { start -> start > 0 }?.let { start -> end - start }
                                }
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
                cost = response.info.cost
            )
        }
    }
}

@Serializable
sealed class MessagePart {
    @Serializable
    data class Text(val text: String) : MessagePart()

    @Serializable
    data class Reasoning(
        val content: String,
        val timeMs: Long
    ) : MessagePart()

    /**
     * Extended thinking content from Claude/o1 and other reasoning models.
     * Displayed with a distinct "thinking" visualization before text response.
     */
    @Serializable
    data class Thinking(
        val content: String,
        val durationMs: Long? = null
    ) : MessagePart()

    @Serializable
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
    ) : MessagePart()

    @Serializable
    data class ToolResult(
        val id: String,
        val result: String
    ) : MessagePart()

    @Serializable
    data class File(
        val mediaType: String,
        val url: String? = null,
        val filename: String? = null
    ) : MessagePart()

    @Serializable
    data class SubTask(
        val sessionId: String,
        val title: String,
        val status: SessionStatus,
        val messages: List<Message> = emptyList(),
        val streamingText: String = ""
    ) : MessagePart()
}

enum class ToolState {
    PENDING,
    RUNNING,
    COMPLETED,
    ERROR
}

/**
 * Rich tool state matching OpenChamber's ToolStateUnion.
 * Use this for detailed tool card rendering.
 */
sealed class RichToolState {
    abstract val status: ToolState
    
    data class Pending(
        override val status: ToolState = ToolState.PENDING
    ) : RichToolState()
    
    data class Running(
        override val status: ToolState = ToolState.RUNNING,
        val input: Map<String, JsonElement>? = null,
        val title: String? = null,
        val metadata: Map<String, JsonElement>? = null,
        val startTime: Long
    ) : RichToolState()
    
    data class Completed(
        override val status: ToolState = ToolState.COMPLETED,
        val input: Map<String, JsonElement>,
        val output: String,
        val title: String,
        val metadata: Map<String, JsonElement> = emptyMap(),
        val startTime: Long,
        val endTime: Long
    ) : RichToolState() {
        val durationMs: Long get() = endTime - startTime
    }
    
    data class Error(
        override val status: ToolState = ToolState.ERROR,
        val input: Map<String, JsonElement>,
        val error: String,
        val metadata: Map<String, JsonElement>? = null,
        val startTime: Long,
        val endTime: Long
    ) : RichToolState() {
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
data class AppInfo(
    val version: String,
    val initialized: Boolean = false,
    val healthy: Boolean = true
)

@Serializable
data class Provider(
    val id: String,
    val name: String,
    val models: Map<String, Model> = emptyMap()
)

@Serializable
data class ProvidersResponse(
    val providers: List<Provider> = emptyList()
)

@Serializable
data class Model(
    val id: String,
    val name: String
)

@Serializable
data class Mode(
    val id: String,
    val name: String,
    val description: String? = null
)

@Serializable
data class FileInfo(
    val name: String,
    val path: String,
    val type: String, // "file" or "directory"
    val size: Long? = null,
    @SerialName("updated")
    val modifiedAt: Long? = null
) {
    val isDirectory: Boolean get() = type == "directory"
}

@Serializable
data class FileContent(
    val path: String,
    val content: String,
    val language: String? = null
)

@Serializable
data class FileUpdateRequest(
    val path: String,
    val content: String
)

@Serializable
data class FileStatus(
    val path: String,
    val gitStatus: GitStatus? = null,
    val diagnostics: List<Diagnostic> = emptyList()
)

@Serializable
data class GitStatus(
    val status: String,
    val staged: Boolean = false
)

@Serializable
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

@Serializable
data class SearchResult(
    val file: String,
    val line: Int,
    val column: Int,
    val match: String,
    val context: String
)

@Serializable
data class SymbolResult(
    val name: String,
    val kind: String,
    val file: String,
    val line: Int
)

@Serializable
data class Terminal(
    val id: String,
    val shell: String? = null
)

@Serializable
data class TerminalResizeRequest(
    val cols: Int,
    val rows: Int
)

/**
 * Response from /config endpoint.
 */
@Serializable
data class ConfigResponse(
    val providers: List<Provider> = emptyList(),
    val agents: List<AgentConfig> = emptyList(),
    val modes: List<Mode> = emptyList()
)

/**
 * Agent configuration from OpenCode.
 */
@Serializable
data class AgentConfig(
    val id: String,
    val name: String,
    val description: String? = null
)

/**
 * Session status info from /session/status endpoint.
 * Matches OpenChamber's session status types.
 */
@Serializable
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
data class SessionRevertInfo(
    @SerialName("messageID")
    val messageID: String
)

/**
 * Recent model entry for UI persistence.
 */
@Serializable
data class RecentModel(
    val providerId: String,
    val modelId: String,
    val lastUsedAt: Long
)

/**
 * Generic server error response structure.
 * Used to parse error bodies when the server returns a non-200 status or unexpected JSON.
 */
@Serializable
data class ServerErrorResponse(
    val name: String? = null,
    val message: String? = null,
    val data: JsonElement? = null,
    val code: String? = null,
    val status: Int? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// TODO LIST MODELS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Session todo item.
 * Used for tracking agent task progress.
 */
@Serializable
data class Todo(
    val id: String,
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

// ═══════════════════════════════════════════════════════════════════════════
// SESSION OPERATIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Request body for POST /session/:id/summarize.
 */
@Serializable
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
data class InitSessionRequest(
    @SerialName("messageID")
    val messageID: String,
    @SerialName("providerID")
    val providerID: String,
    @SerialName("modelID")
    val modelID: String
)

// ═══════════════════════════════════════════════════════════════════════════
// CONFIG UPDATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Request body for PATCH /config.
 */
@Serializable
data class ConfigUpdate(
    val model: ModelConfig? = null,
    val provider: String? = null,
    val theme: String? = null,
    val autosave: Boolean? = null,
    val experimental: Map<String, JsonElement>? = null
)

@Serializable
data class ModelConfig(
    val default: String? = null,
    val reasoning: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// PROJECT MANAGEMENT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Project information from /project endpoints.
 */
@Serializable
data class Project(
    val id: String,
    val name: String,
    val path: String,
    val directory: String,
    val vcs: VcsType? = null,
    val createdAt: Long? = null
)

@Serializable
enum class VcsType {
    @SerialName("git") GIT,
    @SerialName("none") NONE
}

/**
 * Path information from /path endpoint.
 */
@Serializable
data class PathInfo(
    val cwd: String,
    val home: String? = null,
    val root: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// LOGGING
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Request body for POST /log.
 */
@Serializable
data class LogEntry(
    val service: String,
    val level: String, // "debug", "info", "warn", "error"
    val message: String,
    val extra: Map<String, String>? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// TOOLS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Full tool list response.
 */
@Serializable
data class ToolList(
    val tools: List<ToolDefinition> = emptyList()
)

@Serializable
data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String? = null,
    val schema: JsonElement? = null,
    val category: String? = null
)
