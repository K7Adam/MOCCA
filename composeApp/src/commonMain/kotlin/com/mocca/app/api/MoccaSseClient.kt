package com.mocca.app.api

import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

/**
 * SSE (Server-Sent Events) Client for real-time event streaming.
 * Handles connection to OpenCode's /event endpoint.
 * Uses [ApiExecutor] to access the current HttpClient managed by ConnectionManager.
 * Note: Retry logic is handled by EventStreamRepository for better network awareness.
 */
class MoccaSseClient(
    private val api: ApiExecutor
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    /**
     * Subscribe to the event stream from OpenCode server.
     * Returns a Flow that emits ServerEvent objects.
     * Uses Ktor SSE plugin - connection kept alive until cancelled.
     */
    fun subscribeToEvents(): Flow<ServerEvent> = callbackFlow {
        try {
            Napier.i(">>> Connecting to SSE at /global/event")
            
            api.execute {
                sse(
                    urlString = "global/event",
                    reconnectionTime = 3.seconds
                ) {
                Napier.i(">>> SSE session block entered - connection established")
                
                // Emit a synthetic connected event for UI feedback
                send(ServerEvent.Connected(
                    type = "server.connected",
                    properties = ConnectedProperties(
                        status = "connected",
                        version = "unknown"
                    )
                ))
                
                Napier.i(">>> Starting to collect from incoming channel...")
                
                this.incoming
                    .buffer(32)  // OOM FIX: Reduced from 100→32 to limit SSE buffer memory
                    .collect { sseEvent ->
                    Napier.v(">>> SSE Event received: id=${sseEvent.id}, event=${sseEvent.event}, data length=${sseEvent.data?.length ?: 0}")
                    
                    val data = sseEvent.data
                    if (data.isNullOrBlank()) {
                        Napier.v(">>> SSE event has no data, skipping")
                        return@collect
                    }
                    
                    Napier.v(">>> Raw SSE Data (${data.length} bytes): ${data.take(150)}...")
                    
                    try {
                        val event = parseEvent(data)
                        Napier.v(">>> Parsed event type: ${event.type}")
                        send(event)
                        Napier.v(">>> Event sent to flow successfully")
                    } catch (e: Exception) {
                        Napier.w(">>> Failed to parse SSE event: ${data.take(100)}", e)
                        send(ServerEvent.Unknown(type = "parse_error", rawData = data))
                    }
                }
                
                Napier.w(">>> SSE incoming.collect completed - stream may have ended")
                }
            }
            
            Napier.w(">>> SSE sse() block completed")
        } catch (e: CancellationException) {
            Napier.i(">>> SSE connection cancelled")
            throw e
        } catch (e: Exception) {
            Napier.e(">>> SSE connection error: ${e.message}", e)
            close(e)
        }
        
        awaitClose {
            Napier.i(">>> SSE connection closed by awaitClose")
        }
    }
    
    /**
     * Parse raw JSON string into ServerEvent.
     * Based on OpenCode SDK event types.
     * 
     * IMPORTANT: OpenCode server sends events with nested structure:
     * {"payload":{"type":"server.heartbeat","properties":{}}}
     * 
     * We need to extract the payload first, then parse the event.
     */
    private fun parseEvent(data: String): ServerEvent {
        val jsonObject = json.decodeFromString<JsonObject>(data)
        
        // OpenCode wraps events in a "payload" object
        // Extract payload if present, otherwise use root object (for backwards compatibility)
        val payloadObject = jsonObject["payload"] as? JsonObject
        val eventObject = payloadObject ?: jsonObject
        
        val type = eventObject["type"]?.jsonPrimitive?.content ?: "unknown"
        
        // Serialize the event object (payload or root) for decoding
        val eventData = json.encodeToString(JsonObject.serializer(), eventObject)
        
        return when (type) {
            // Connection events
            "server.connected" -> json.decodeFromString<ServerEvent.Connected>(eventData)
            "server.heartbeat" -> json.decodeFromString<ServerEvent.Heartbeat>(eventData)
            
            // Session events
            "session.updated" -> json.decodeFromString<ServerEvent.SessionUpdated>(eventData)
            "session.deleted" -> json.decodeFromString<ServerEvent.SessionDeleted>(eventData)
            "session.idle" -> json.decodeFromString<ServerEvent.SessionIdle>(eventData)
            "session.error" -> json.decodeFromString<ServerEvent.SessionError>(eventData)
            
            // Message events
            "message.updated" -> json.decodeFromString<ServerEvent.MessageUpdated>(eventData)
            "message.removed" -> json.decodeFromString<ServerEvent.MessageRemoved>(eventData)
            "message.part.updated" -> json.decodeFromString<ServerEvent.MessagePartUpdated>(eventData)
            "message.part.removed" -> json.decodeFromString<ServerEvent.MessagePartRemoved>(eventData)
            
            // Permission events (tool approval)
            "permission.updated" -> json.decodeFromString<ServerEvent.PermissionUpdated>(eventData)
            "permission.asked" -> json.decodeFromString<ServerEvent.PermissionAsked>(eventData)
            "permission.replied" -> json.decodeFromString<ServerEvent.PermissionReplied>(eventData)
            
            // Question events (interactive input)
            "question.asked" -> json.decodeFromString<ServerEvent.QuestionAsked>(eventData)
            "question.replied" -> json.decodeFromString<ServerEvent.QuestionReplied>(eventData)
            
            // File events
            "file.edited" -> json.decodeFromString<ServerEvent.FileEdited>(eventData)
            "file.watcher.updated" -> json.decodeFromString<ServerEvent.FileWatcherUpdated>(eventData)
            
            // System events
            "installation.updated" -> json.decodeFromString<ServerEvent.InstallationUpdated>(eventData)
            "lsp.client.diagnostics" -> json.decodeFromString<ServerEvent.LspDiagnostics>(eventData)
            
            // Log and agent events
            "log" -> json.decodeFromString<ServerEvent.Log>(eventData)
            "agent.status" -> json.decodeFromString<ServerEvent.AgentStatus>(eventData)
            
            
            // New session lifecycle events
            "session.created" -> json.decodeFromString<ServerEvent.SessionCreated>(eventData)
            "session.status" -> json.decodeFromString<ServerEvent.SessionStatus>(eventData)
            "session.diff" -> json.decodeFromString<ServerEvent.SessionDiff>(eventData)
            "session.compacted" -> json.decodeFromString<ServerEvent.SessionCompacted>(eventData)
            
            // Todo events
            "todo.updated" -> json.decodeFromString<ServerEvent.TodoUpdated>(eventData)
            
            // Question events (extended)
            "question.rejected" -> json.decodeFromString<ServerEvent.QuestionRejected>(eventData)
            
            // Streaming delta events
            "message.part.delta" -> json.decodeFromString<ServerEvent.MessagePartDelta>(eventData)
            
            // PTY / terminal events
            "pty.created" -> json.decodeFromString<ServerEvent.PtyCreated>(eventData)
            "pty.updated" -> json.decodeFromString<ServerEvent.PtyUpdated>(eventData)
            "pty.exited" -> json.decodeFromString<ServerEvent.PtyExited>(eventData)
            "pty.deleted" -> json.decodeFromString<ServerEvent.PtyDeleted>(eventData)
            
            // Project / VCS events
            "project.updated" -> json.decodeFromString<ServerEvent.ProjectUpdated>(eventData)
            "vcs.branch.updated" -> json.decodeFromString<ServerEvent.VcsBranchUpdated>(eventData)
            
            // File and LSP events (extended)
            "file.updated" -> json.decodeFromString<ServerEvent.FileUpdated>(eventData)
            "lsp.updated" -> json.decodeFromString<ServerEvent.LspUpdated>(eventData)
            
            // MCP events (extended)
            "mcp.tools.changed" -> json.decodeFromString<ServerEvent.McpToolsChanged>(eventData)
            "mcp.browser.open.failed" -> json.decodeFromString<ServerEvent.McpBrowserOpenFailed>(eventData)
            
            // Worktree events
            "worktree.ready" -> json.decodeFromString<ServerEvent.WorktreeReady>(eventData)
            "worktree.failed" -> json.decodeFromString<ServerEvent.WorktreeFailed>(eventData)
            
            // Installation and disposal events
            "installation.update.available" -> json.decodeFromString<ServerEvent.InstallationUpdateAvailable>(eventData)
            "server.instance.disposed" -> json.decodeFromString<ServerEvent.ServerInstanceDisposed>(eventData)
            "global.disposed" -> json.decodeFromString<ServerEvent.GlobalDisposed>(eventData)
            // Unknown event type
            else -> {
                Napier.d(">>> Unknown SSE event type: $type, payload present: ${payloadObject != null}")
                ServerEvent.Unknown(type = type, rawData = data)
            }
        }
    }
}
