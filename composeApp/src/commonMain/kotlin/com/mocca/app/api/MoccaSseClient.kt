package com.mocca.app.api

import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

/**
 * SSE (Server-Sent Events) Client for real-time event streaming.
 * Handles connection to OpenCode's /event endpoint.
 * Note: Retry logic is handled by EventStreamRepository for better network awareness.
 */
class MoccaSseClient(
    private var httpClient: HttpClient,
    private val serverConfigProvider: () -> ServerConfig,
    private val retryPolicy: RetryPolicy = RetryPolicy.Aggressive,
    private val httpClientProvider: HttpClientProvider? = null
) {
    init {
        // Subscribe to client recreation events
        httpClientProvider?.onClientRecreated = {
            httpClientProvider.let { provider ->
                httpClient = provider.getClientSync()
            }
        }
    }
    
    /**
     * Get the current HttpClient, ensuring it's up-to-date with server config.
     */
    private fun getClient(): HttpClient {
        return httpClientProvider?.getClientSync() ?: httpClient
    }
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    private val baseUrl: String
        get() = serverConfigProvider().baseUrl.trimEnd('/')

    /**
     * Subscribe to the event stream from OpenCode server.
     * Returns a Flow that emits ServerEvent objects.
     * Uses Ktor SSE plugin - connection kept alive until cancelled.
     */
    fun subscribeToEvents(): Flow<ServerEvent> = callbackFlow {
        try {
            val url = "$baseUrl/event"
            Napier.i(">>> Connecting to SSE at $url")
            
            getClient().sse(
                urlString = url,
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
                
                this.incoming.collect { sseEvent ->
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
     */
    private fun parseEvent(data: String): ServerEvent {
        val jsonObject = json.decodeFromString<JsonObject>(data)
        val type = jsonObject["type"]?.jsonPrimitive?.content ?: "unknown"
        
        return when (type) {
            // Connection events
            "server.connected" -> json.decodeFromString<ServerEvent.Connected>(data)
            "server.heartbeat" -> ServerEvent.Unknown(type = type, rawData = data) // Heartbeat from server
            
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
