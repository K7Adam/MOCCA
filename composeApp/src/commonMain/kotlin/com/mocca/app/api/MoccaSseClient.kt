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
            Napier.i(">>> Connecting to SSE at /event")
            
            api.execute {
                sse(
                    urlString = "event",
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
                    .buffer(100)
                    .collect { sseEvent ->
                    Napier.i(">>> SSE Event received: id=${sseEvent.id}, event=${sseEvent.event}, data length=${sseEvent.data?.length ?: 0}")
                    
                    val data = sseEvent.data
                    if (data.isNullOrBlank()) {
                        Napier.v(">>> SSE event has no data, skipping")
                        return@collect
                    }
                    
                    Napier.i(">>> Raw SSE Data (${data.length} bytes): ${data.take(150)}...")
                    
                    try {
                        val event = parseEvent(data)
                        Napier.i(">>> Parsed event type: ${event.type}")
                        send(event)
                        Napier.i(">>> Event sent to flow successfully")
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
     * Subscribe to global events (not session-specific).
     * Useful for monitoring system-wide events like installation updates,
     * LSP diagnostics, and agent status across all sessions.
     * Returns a Flow that emits only global ServerEvent objects.
     */
    fun subscribeToGlobalEvents(): Flow<ServerEvent> = callbackFlow {
        try {
            Napier.i(">>> Connecting to Global SSE at /event/global")
            
            api.execute {
                sse(
                    urlString = "event/global",
                    reconnectionTime = 3.seconds
                ) {
                    Napier.i(">>> Global SSE session established")
                    
                    send(ServerEvent.Connected(
                        type = "server.connected",
                        properties = ConnectedProperties(
                            status = "connected",
                            version = "unknown"
                        )
                    ))
                    
                    this.incoming
                        .buffer(100)
                        .collect { sseEvent ->
                        val data = sseEvent.data
                        if (data.isNullOrBlank()) return@collect
                        
                        try {
                            val event = parseEvent(data)
                            send(event)
                        } catch (e: Exception) {
                            Napier.w(">>> Failed to parse global SSE event: ${data.take(100)}", e)
                            send(ServerEvent.Unknown(type = "parse_error", rawData = data))
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e(">>> Global SSE connection error: ${e.message}", e)
            close(e)
        }
        
        awaitClose {
            Napier.i(">>> Global SSE connection closed")
        }
    }

    /**
     * Parse raw JSON string into ServerEvent.
     * Based on OpenCode SDK event types.
     */
    private fun parseEvent(data: String): ServerEvent {
        val jsonObject = json.decodeFromString<JsonObject>(data)
        val type = jsonObject["type"]?.jsonPrimitive?.content ?: "unknown"
        
        return when (type) {
            // Connection events
            "server.connected" -> json.decodeFromString<ServerEvent.Connected>(data)
            "server.heartbeat" -> json.decodeFromString<ServerEvent.Heartbeat>(data)
            
            // Session events
            "session.updated" -> json.decodeFromString<ServerEvent.SessionUpdated>(data)
            "session.deleted" -> json.decodeFromString<ServerEvent.SessionDeleted>(data)
            "session.idle" -> json.decodeFromString<ServerEvent.SessionIdle>(data)
            "session.error" -> json.decodeFromString<ServerEvent.SessionError>(data)
            
            // Message events
            "message.updated" -> json.decodeFromString<ServerEvent.MessageUpdated>(data)
            "message.removed" -> json.decodeFromString<ServerEvent.MessageRemoved>(data)
            "message.part.updated" -> json.decodeFromString<ServerEvent.MessagePartUpdated>(data)
            "message.part.removed" -> json.decodeFromString<ServerEvent.MessagePartRemoved>(data)
            
            // Permission events (tool approval)
            "permission.updated" -> json.decodeFromString<ServerEvent.PermissionUpdated>(data)
            "permission.asked" -> json.decodeFromString<ServerEvent.PermissionAsked>(data)
            "permission.replied" -> json.decodeFromString<ServerEvent.PermissionReplied>(data)
            
            // Question events (interactive input)
            "question.asked" -> json.decodeFromString<ServerEvent.QuestionAsked>(data)
            "question.replied" -> json.decodeFromString<ServerEvent.QuestionReplied>(data)
            
            // File events
            "file.edited" -> json.decodeFromString<ServerEvent.FileEdited>(data)
            "file.watcher.updated" -> json.decodeFromString<ServerEvent.FileWatcherUpdated>(data)
            
            // System events
            "installation.updated" -> json.decodeFromString<ServerEvent.InstallationUpdated>(data)
            "lsp.client.diagnostics" -> json.decodeFromString<ServerEvent.LspDiagnostics>(data)
            
            // Log and agent events
            "log" -> json.decodeFromString<ServerEvent.Log>(data)
            "agent.status" -> json.decodeFromString<ServerEvent.AgentStatus>(data)
            
            // Unknown event type
            else -> {
                Napier.d(">>> Unknown SSE event type: $type")
                ServerEvent.Unknown(type = type, rawData = data)
            }
        }
    }
}
