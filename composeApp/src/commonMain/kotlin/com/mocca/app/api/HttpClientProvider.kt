package com.mocca.app.api

import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.domain.model.AuthType
import com.mocca.app.domain.model.ServerConfig
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Dynamic HttpClient provider that recreates the client when server configuration changes.
 * This solves the "stale client" bug where Auth headers and base URL become outdated
 * when user switches servers in Settings.
 */
class HttpClientProvider(
    private val serverConfigRepository: ServerConfigRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    
    @Volatile
    private var currentClient: HttpClient? = null
    
    @Volatile
    private var currentConfigId: String? = null
    
    /**
     * Callback invoked when the HttpClient is recreated.
     * Allows dependent components (SSE, API client) to reconnect.
     */
    var onClientRecreated: (() -> Unit)? = null
    
    init {
        // Observe server config changes and recreate client when needed
        scope.launch {
            serverConfigRepository.activeServer.collectLatest { newConfig ->
                if (newConfig != null && newConfig.id != currentConfigId) {
                    Napier.i("Server config changed: ${currentConfigId} -> ${newConfig.id}, recreating HttpClient")
                    recreateClient(newConfig)
                }
            }
        }
    }
    
    /**
     * Get the current HttpClient instance.
     * Creates one if it doesn't exist.
     */
    suspend fun getClient(): HttpClient {
        return mutex.withLock {
            currentClient ?: createClient(serverConfigRepository.getActiveServerConfig()).also {
                currentClient = it
                currentConfigId = serverConfigRepository.getActiveServerConfig().id
            }
        }
    }
    
    /**
     * Get the current HttpClient synchronously.
     * Uses the existing client or creates a new one with current config.
     */
    fun getClientSync(): HttpClient {
        return currentClient ?: createClient(serverConfigRepository.getActiveServerConfig()).also {
            currentClient = it
            currentConfigId = serverConfigRepository.getActiveServerConfig().id
        }
    }
    
    /**
     * Force recreation of the HttpClient with current config.
     * Call this when you need to ensure fresh Auth headers.
     */
    suspend fun forceRecreate() {
        recreateClient(serverConfigRepository.getActiveServerConfig())
    }
    
    private suspend fun recreateClient(config: ServerConfig) {
        val oldClient = mutex.withLock {
            // Get reference to old client to close it LATER
            val old = currentClient
            
            // Create new client
            currentClient = createClient(config)
            currentConfigId = config.id
            
            Napier.i("HttpClient recreated for server: ${config.name} (${config.baseUrl})")
            old
        }
        
        // Notify listeners that a NEW client is ready
        onClientRecreated?.invoke()

        // Delay closing the old client to allow in-flight requests to complete or timeout naturally
        // This prevents CancellationException on pending requests during config switch
        // INCREASED GRACE PERIOD: 5s -> 15s to match typical request timeouts
        scope.launch {
            kotlinx.coroutines.delay(15_000) 
            try {
                oldClient?.close()
                Napier.i("Old HttpClient closed after grace period")
            } catch (e: Exception) {
                Napier.w("Error closing old HttpClient", e)
            }
        }
    }
    
    private fun createClient(config: ServerConfig): HttpClient {
        return HttpClient(getHttpEngine()) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }

            install(WebSockets) {
                pingIntervalMillis = 30_000
            }

            install(SSE) {
                showCommentEvents()
                showRetryEvents()
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 120_000
            }

            // Only install Auth if Bearer token is configured
            if (config.authType == AuthType.BEARER && !config.authToken.isNullOrEmpty()) {
                install(Auth) {
                    bearer {
                        loadTokens {
                            BearerTokens(config.authToken, "")
                        }
                    }
                }
            }

            defaultRequest {
                url(config.baseUrl)
                // For non-bearer auth, add headers manually
                if (config.authType == AuthType.BASIC && !config.authToken.isNullOrEmpty()) {
                    header(HttpHeaders.Authorization, "Basic ${config.authToken}")
                }
            }
        }
    }
    
    /**
     * Close the current client and clean up resources.
     */
    fun close() {
        currentClient?.close()
        currentClient = null
        currentConfigId = null
    }
}
