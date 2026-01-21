package com.mocca.app.api

import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.domain.model.AuthType
import com.mocca.app.domain.model.ServerConfig
import com.mocca.app.util.NetworkObserver
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

// ═══════════════════════════════════════════════════════════════════════════════
// CONNECTION QUALITY (Priority 4.4)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Connection quality indicator based on recent request performance.
 */
enum class ConnectionQuality {
    /** Connection is healthy with fast responses */
    EXCELLENT,
    /** Connection is working but may have some latency */
    GOOD,
    /** Connection is degraded with high latency or occasional failures */
    POOR,
    /** Connection is offline or experiencing frequent failures */
    OFFLINE,
    /** Connection quality is unknown (no recent requests) */
    UNKNOWN
}

/**
 * Metrics for connection quality monitoring.
 */
data class ConnectionMetrics(
    val quality: ConnectionQuality = ConnectionQuality.UNKNOWN,
    val averageLatencyMs: Long = 0,
    val successRate: Float = 1f,
    val lastSuccessTime: Long = 0,
    val lastFailureTime: Long = 0,
    val consecutiveFailures: Int = 0
)


/**
 * Dynamic HttpClient provider that recreates the client when server configuration changes.
 * This solves the "stale client" bug where Auth headers and base URL become outdated
 * when user switches servers in Settings.
 * 
 * Features:
 * - P4.2: Token refresh support with automatic client recreation
 * - P4.4: Connection quality monitoring
 * - P4.5: Request deduplication support
 */
class HttpClientProvider(
    private val serverConfigRepository: ServerConfigRepository,
    private val networkObserver: NetworkObserver? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    
    @Volatile
    private var currentClient: HttpClient? = null
    
    @Volatile
    private var currentConfigId: String? = null
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONNECTION QUALITY MONITORING (Priority 4.4)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _connectionMetrics = MutableStateFlow(ConnectionMetrics())
    val connectionMetrics: StateFlow<ConnectionMetrics> = _connectionMetrics.asStateFlow()
    
    // Track recent request performance for quality calculation
    private val recentLatencies = mutableListOf<Long>()
    private val maxLatencyHistory = 20
    private var recentSuccesses = 0
    private var recentTotal = 0
    private val maxSampleSize = 50
    
    /**
     * Record a successful request for quality monitoring.
     */
    fun recordSuccess(latencyMs: Long) {
        synchronized(this) {
            recentLatencies.add(latencyMs)
            if (recentLatencies.size > maxLatencyHistory) {
                recentLatencies.removeAt(0)
            }
            
            recentSuccesses++
            recentTotal++
            if (recentTotal > maxSampleSize) {
                recentSuccesses = (recentSuccesses * 0.9).toInt()
                recentTotal = (recentTotal * 0.9).toInt()
            }
            
            updateConnectionQuality(success = true)
        }
    }
    
    /**
     * Record a failed request for quality monitoring.
     */
    fun recordFailure() {
        synchronized(this) {
            recentTotal++
            if (recentTotal > maxSampleSize) {
                recentSuccesses = (recentSuccesses * 0.9).toInt()
                recentTotal = (recentTotal * 0.9).toInt()
            }
            
            updateConnectionQuality(success = false)
        }
    }
    
    private fun updateConnectionQuality(success: Boolean) {
        val now = System.currentTimeMillis()
        val current = _connectionMetrics.value
        
        val avgLatency = if (recentLatencies.isNotEmpty()) {
            recentLatencies.average().toLong()
        } else {
            current.averageLatencyMs
        }
        
        val successRate = if (recentTotal > 0) {
            recentSuccesses.toFloat() / recentTotal
        } else {
            1f
        }
        
        val consecutiveFailures = if (success) 0 else current.consecutiveFailures + 1
        
        val quality = when {
            consecutiveFailures >= 3 -> ConnectionQuality.OFFLINE
            successRate < 0.5f -> ConnectionQuality.OFFLINE
            successRate < 0.8f || avgLatency > 5000 -> ConnectionQuality.POOR
            avgLatency > 2000 -> ConnectionQuality.GOOD
            else -> ConnectionQuality.EXCELLENT
        }
        
        _connectionMetrics.value = ConnectionMetrics(
            quality = quality,
            averageLatencyMs = avgLatency,
            successRate = successRate,
            lastSuccessTime = if (success) now else current.lastSuccessTime,
            lastFailureTime = if (!success) now else current.lastFailureTime,
            consecutiveFailures = consecutiveFailures
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // REQUEST DEDUPLICATION (Priority 4.5)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val inFlightRequests = mutableMapOf<String, kotlinx.coroutines.Deferred<Any?>>()
    private val deduplicationMutex = Mutex()
    
    /**
     * Execute a request with deduplication.
     * If an identical request is already in flight, wait for its result instead of making a new request.
     * 
     * @param key Unique key for the request (e.g., "getSession:sessionId")
     * @param block The suspend function to execute
     * @return Result of the request
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> withDeduplication(key: String, block: suspend () -> T): T {
        // Check if request is already in flight
        val existingDeferred = deduplicationMutex.withLock {
            inFlightRequests[key]
        }
        
        if (existingDeferred != null) {
            Napier.d("[Dedup] Waiting for in-flight request: $key")
            return existingDeferred.await() as T
        }
        
        // Create new deferred and register it
        val deferred = kotlinx.coroutines.CompletableDeferred<Any?>()
        deduplicationMutex.withLock {
            inFlightRequests[key] = deferred
        }
        
        return try {
            val result = block()
            deferred.complete(result)
            result
        } catch (e: Throwable) {
            deferred.completeExceptionally(e)
            throw e
        } finally {
            deduplicationMutex.withLock {
                inFlightRequests.remove(key)
            }
        }
    }
    
    /**
     * Callback invoked when the HttpClient is recreated.
     * Allows dependent components (SSE, API client) to reconnect.
     */
    var onClientRecreated: (() -> Unit)? = null
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TOKEN REFRESH (Priority 4.2)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Callback for token refresh. Set this to enable automatic token refresh.
     * Should return the new token, or null if refresh failed.
     */
    var onTokenRefresh: (suspend () -> String?)? = null
    
    /**
     * Force refresh the authentication token and recreate the client.
     * Call this when you receive a 401 Unauthorized response.
     */
    suspend fun refreshToken(): Boolean {
        val refreshCallback = onTokenRefresh ?: return false
        
        val newToken = try {
            refreshCallback()
        } catch (e: Exception) {
            Napier.e("Token refresh failed", e)
            null
        }
        
        if (newToken != null) {
            // Update the server config with the new token
            val currentConfig = serverConfigRepository.getActiveServerConfig()
            val updatedConfig = currentConfig.copy(authToken = newToken)
            serverConfigRepository.saveServer(updatedConfig)
            
            // Force client recreation
            forceRecreate()
            Napier.i("Token refreshed successfully, client recreated")
            return true
        }
        
        return false
    }
    
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
        
        // P4.4: Proactive network monitoring
        if (networkObserver != null) {
            scope.launch {
                networkObserver.isOnline.collect { isOnline ->
                    if (!isOnline) {
                        Napier.w("[HttpClientProvider] Network lost (proactive)")
                        _connectionMetrics.value = _connectionMetrics.value.copy(
                            quality = ConnectionQuality.OFFLINE
                        )
                    } else if (_connectionMetrics.value.quality == ConnectionQuality.OFFLINE) {
                        Napier.i("[HttpClientProvider] Network restored (proactive)")
                        // Reset to UNKNOWN so next request will re-evaluate quality
                        _connectionMetrics.value = _connectionMetrics.value.copy(
                            quality = ConnectionQuality.UNKNOWN,
                            consecutiveFailures = 0
                        )
                    }
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

            install(ContentEncoding) {
                gzip()
                deflate()
            }

            install(WebSockets) {
                pingIntervalMillis = 30_000
            }

            install(SSE) {
                showCommentEvents()
                showRetryEvents()
            }

            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
                retryIf { _, response ->
                    response.status.value.let { it in 500..599 }
                }
                retryOnExceptionIf { _, cause ->
                    cause is io.ktor.client.network.sockets.SocketTimeoutException ||
                    cause is io.ktor.client.network.sockets.ConnectTimeoutException
                }
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
