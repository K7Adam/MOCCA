package com.mocca.app.data.repository

import com.mocca.app.api.ApiExecutor
import com.mocca.app.api.ConnectionException
import com.mocca.app.api.NetworkConfig
import com.mocca.app.api.getHttpEngine
import com.mocca.app.bridge.client.BridgeTransport
import com.mocca.app.bridge.client.DirectBridgeTarget
import com.mocca.app.bridge.connection.BridgeHealth
import com.mocca.app.bridge.connection.BridgeHealthChecker
import com.mocca.app.bridge.connection.BridgeTransportFactory
import com.mocca.app.bridge.connection.KtorBridgeTransport
import com.mocca.app.domain.model.AppInfo
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.ServerConfig
import com.mocca.app.util.NetworkObserver
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpHeaders
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.math.min

/**
 * Single source of truth for connection state and HttpClient lifecycle.
 *
 * Replaces the old HttpClientProvider + AppConnectionManager dual-class system.
 * Consumers never hold an HttpClient reference — they call [execute] via [ApiExecutor].
 *
 * Responsibilities:
 * - Owns and manages the HttpClient lifecycle (create/recreate/close)
 * - Manages connection state transitions (NotConfigured → Connecting → Connected → ...)
 * - Performs periodic health checks against /global/health
 * - Handles reconnection with exponential backoff
 * - Reacts to network availability changes
 * - Triggers data refresh on successful connection
 */
class ConnectionManager(
    private val serverConfigRepository: ServerConfigRepository,
    private val networkObserver: NetworkObserver? = null
) : ApiExecutor, BridgeTransportFactory, BridgeHealthChecker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val clientMutex = Mutex()

    @Volatile
    private var currentClient: HttpClient? = null

    @Volatile
    private var bridgeClient: HttpClient? = null

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.NotConfigured)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _activeConfig = MutableStateFlow<ServerConfig?>(null)
    val activeConfig: StateFlow<ServerConfig?> = _activeConfig.asStateFlow()

    private var healthJob: Job? = null
    private var reconnectJob: Job? = null
    private var networkObserverJob: Job? = null

    private var consecutiveFailures = 0
    private var hasEverConnected = false
    private var lastSuccessfulCheckMs: Long? = null

    /**
     * Callback invoked after a successful connection is established.
     * Used by DI to wire data refresh without circular dependencies.
     */
    var onConnectionEstablished: (suspend () -> Unit)? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        observeActiveServer()
        observeNetworkState()
    }

    // ApiExecutor Implementation


    override suspend fun <T> execute(block: suspend HttpClient.() -> T): T {
        val client = currentClient ?: throw ConnectionException("Not connected to any server")
        return client.block()
    }

    override suspend fun open(target: DirectBridgeTarget): BridgeTransport {
        val client = clientMutex.withLock {
            bridgeClient ?: createBridgeClient().also { bridgeClient = it }
        }
        val session = client.webSocketSession {
            url(target.websocketUrl)
        }
        return KtorBridgeTransport(session)
    }

    override suspend fun check(target: DirectBridgeTarget): BridgeHealth {
        val client = clientMutex.withLock {
            bridgeClient ?: createBridgeClient().also { bridgeClient = it }
        }
        return client.get(target.healthUrl).body()
    }

    // Connection Lifecycle


    @Volatile
    private var isConnecting = false

    /**
     * Connect to a server with the given config.
     * Closes any existing connection first.
     * Guards against duplicate/reentrant calls for the same config.
     */
    fun connect(config: ServerConfig) {
        // Guard against redundant connection attempts to the same server
        val current = _activeConfig.value
        if (isConnecting && current?.id == config.id &&
            current.host == config.host && current.port == config.port &&
            current.username == config.username && current.password == config.password
        ) {
            Napier.d("[ConnectionManager] Already connecting to ${config.baseUrl}, skipping duplicate connect()")
            return
        }
        isConnecting = true
        scope.launch {
            // Synchronously clean up previous connection
            cancelAllJobs()
            clientMutex.withLock {
                currentClient?.close()
                currentClient = null
            }
            // Set up new connection
            clientMutex.withLock {
                _activeConfig.value = config
                _status.value = ConnectionStatus.Connecting
                currentClient = createClient(config)
            }
            // Persist as active
            serverConfigRepository.setActiveServer(config.id)
            // Run initial health check directly (NOT via checkConnection() which guards against Connecting)
            consecutiveFailures = 0
            hasEverConnected = false
            reconnectJob?.cancel()
            reconnectJob = scope.launch {
                try {
                    val (result, latencyMs) = withContext(Dispatchers.IO) {
                        val start = Clock.System.now().toEpochMilliseconds()
                        val r = performHealthCheck()
                        val elapsed = Clock.System.now().toEpochMilliseconds() - start
                        Pair(r, elapsed)
                    }
                    handleHealthResult(result, latencyMs)
                } finally {
                    isConnecting = false
                }
            }
        }
    }

    /**
     * Disconnect from the current server.
     */
    fun disconnect() {
        cancelAllJobs()
        isConnecting = false
        scope.launch {
            clientMutex.withLock {
                currentClient?.close()
                currentClient = null
                bridgeClient?.close()
                bridgeClient = null
            }
            _status.value = ConnectionStatus.Disconnected()
            _activeConfig.value = null
        }
    }

    /**
     * Trigger a manual connection check / reconnect attempt.
     */
    fun checkConnection() {
        val config = _activeConfig.value
        if (config == null) {
            _status.value = ConnectionStatus.NotConfigured
            return
        }

        // Skip if already connected or checking
        val current = _status.value
        if (current.isConnected || current.isConnecting) {
            Napier.d("[ConnectionManager] Skipping check, already ${current::class.simpleName}")
            return
        }

        if (networkObserver?.isCurrentlyOnline() == false) {
            _status.value = ConnectionStatus.WaitingForNetwork
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _status.value = ConnectionStatus.Connecting
            val (result, latencyMs) = withContext(Dispatchers.IO) {
                val start = Clock.System.now().toEpochMilliseconds()
                val r = performHealthCheck()
                val elapsed = Clock.System.now().toEpochMilliseconds() - start
                Pair(r, elapsed)
            }
            handleHealthResult(result, latencyMs)
        }
    }

    /**
     * Trigger data refresh if connected.
     */
    fun triggerDataRefresh() {
        if (_status.value.isConnected) {
            scope.launch(Dispatchers.IO) {
                onConnectionEstablished?.invoke()
            }
        }
    }

    // Server Config Observation


    private fun observeActiveServer() {
        scope.launch {
            serverConfigRepository.activeServer.collect { server ->
                val previous = _activeConfig.value
                if (server == null) {
                    cancelAllJobs()
                    _activeConfig.value = null
                    clientMutex.withLock {
                        currentClient?.close()
                        currentClient = null
                        bridgeClient?.close()
                        bridgeClient = null
                    }
                    _status.value = ConnectionStatus.NotConfigured
                    lastSuccessfulCheckMs = null
                    consecutiveFailures = 0
                    return@collect
                }

                // Only reconnect if config actually changed
                if (previous?.id != server.id || previous.host != server.host || previous.port != server.port ||
                    previous.username != server.username || previous.password != server.password
                ) {
                    Napier.i("[ConnectionManager] Active server changed: ${server.name}")
                    connect(server)
                }
            }
        }
    }

    private fun observeNetworkState() {
        networkObserverJob?.cancel()
        networkObserverJob = scope.launch {
            networkObserver?.isOnline?.collect { isOnline ->
                Napier.d("[ConnectionManager] Network state: online=$isOnline")
                if (isOnline && _status.value is ConnectionStatus.WaitingForNetwork) {
                    Napier.i("[ConnectionManager] Network restored, reconnecting...")
                    delay(2000) // Brief stability delay
                    consecutiveFailures = 0
                    checkConnection()
                } else if (!isOnline && _status.value.isConnected) {
                    _status.value = ConnectionStatus.WaitingForNetwork
                }
            }
        }
    }

    // Health Checks


    private suspend fun performHealthCheck(): Result<AppInfo?> {
        val config = _activeConfig.value
        Napier.i("[ConnectionManager] Health check starting for: ${config?.baseUrl}")
        Napier.i("[ConnectionManager] Host: ${config?.host}, Port: ${config?.port}, useHttps: ${config?.useHttps}")
        
        return try {
            Napier.i("[ConnectionManager] Executing GET /global/health...")
            val appInfo = execute { get("/global/health").body<AppInfo>() }
            Napier.i("[ConnectionManager] Health check SUCCESS: ${appInfo.version}")
            Result.success(appInfo)
        } catch (e: ClientRequestException) {
            Napier.w("[ConnectionManager] Health check HTTP ERROR: ${e.response.status} for ${config?.baseUrl}")
            if (e.response.status == HttpStatusCode.Unauthorized) {
                Result.failure(Exception("Authentication failed (401). Check username/password."))
            } else {
                Result.failure(Exception("Server returned ${e.response.status}"))
            }
        } catch (e: ServerResponseException) {
            Napier.w("[ConnectionManager] Health check SERVER ERROR: ${e.response.status} for ${config?.baseUrl}")
            Result.failure(Exception("ServerResponseException: Server returned ${e.response.status}"))
        } catch (e: CancellationException) {
            Napier.w("[ConnectionManager] Health check CANCELLED")
            throw e
        } catch (e: ConnectionException) {
            Napier.w("[ConnectionManager] Health check CONNECTION ERROR: ${e.message}")
            Result.failure(Exception("Connection failed: ${e.message}"))
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown error"
            val errorType = e::class.simpleName
            // Log full stack trace for SSL errors
            if (errorMsg.contains("SSL", ignoreCase = true) || 
                errorMsg.contains("Certificate", ignoreCase = true) ||
                errorMsg.contains("Trust", ignoreCase = true) ||
                errorMsg.contains("Handshake", ignoreCase = true)) {
                Napier.e("[ConnectionManager] SSL/TLS ERROR DETAILS: ${e.stackTraceToString()}")
            } else {
                Napier.w("[ConnectionManager] Health check failed: $errorType - $errorMsg")
            }
            Result.failure(Exception("$errorType: $errorMsg"))
        }
    }

    private fun handleHealthResult(result: Result<AppInfo?>, latencyMs: Long) {
        result.fold(
            onSuccess = { appInfo ->
                Napier.i("[ConnectionManager] Connected (${latencyMs}ms)")
                consecutiveFailures = 0
                hasEverConnected = true
                lastSuccessfulCheckMs = Clock.System.now().toEpochMilliseconds()
                _status.value = ConnectionStatus.Connected(
                    serverInfo = appInfo ?: AppInfo(version = "unknown"),
                    latencyMs = latencyMs
                )
                // Trigger data refresh on first connection
                scope.launch(Dispatchers.IO) {
                    try { onConnectionEstablished?.invoke() } catch (e: CancellationException) { throw e } catch (e: Exception) {
                        Napier.w("[ConnectionManager] Data refresh failed", e)
                    }
                }
                startPeriodicHealthCheck()
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Unknown error"
                Napier.e("[ConnectionManager] Health check FAILED: $errorMsg")
                consecutiveFailures++
                if (consecutiveFailures < MAX_RECONNECT_ATTEMPTS) {
                    scheduleReconnect()
                } else {
                    _status.value = ConnectionStatus.Disconnected(error.message)
                }
            }
        )
    }

    private fun startPeriodicHealthCheck() {
        healthJob?.cancel()
        healthJob = scope.launch {
            while (isActive) {
                delay(PERIODIC_CHECK_INTERVAL_MS)
                if (_status.value.isConnected) {
                    Napier.d("[ConnectionManager] Periodic health check...")
                    val result = withContext(Dispatchers.IO) { performHealthCheck() }
                    result.fold(
                        onSuccess = {
                            lastSuccessfulCheckMs = Clock.System.now().toEpochMilliseconds()
                        },
                        onFailure = { error ->
                            Napier.w("[ConnectionManager] Periodic check failed: ${error.message}")
                            consecutiveFailures++
                            if (consecutiveFailures >= 2) {
                                _status.value = ConnectionStatus.Disconnected(error.message)
                                checkConnection()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun scheduleReconnect() {
        _status.value = if (hasEverConnected) {
            ConnectionStatus.Reconnecting(consecutiveFailures, MAX_RECONNECT_ATTEMPTS)
        } else {
            ConnectionStatus.Connecting
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = calculateBackoff(consecutiveFailures)
            Napier.i("[ConnectionManager] Reconnecting in ${delayMs}ms (attempt $consecutiveFailures/$MAX_RECONNECT_ATTEMPTS)")
            delay(delayMs)
            if (isActive) {
                _status.value = ConnectionStatus.Connecting
                val (result, latencyMs) = withContext(Dispatchers.IO) {
                    val start = Clock.System.now().toEpochMilliseconds()
                    val r = performHealthCheck()
                    val elapsed = Clock.System.now().toEpochMilliseconds() - start
                    Pair(r, elapsed)
                }
                handleHealthResult(result, latencyMs)
            }
        }
    }

    private fun calculateBackoff(attempt: Int): Long {
        val baseDelay = 1000L
        val maxDelay = if (networkObserver?.isCurrentlyOnline() == true) 10000L else 30000L
        val exponentialDelay = baseDelay * (1L shl min(attempt - 1, 5))
        val jitter = (0..500).random().toLong()
        return min(exponentialDelay, maxDelay) + jitter
    }

    // HttpClient Factory


    private fun createClient(config: ServerConfig): HttpClient {
        Napier.i("[ConnectionManager] Creating HttpClient for: ${config.baseUrl}")
        Napier.i("[ConnectionManager] Config - Host: ${config.host}, Port: ${config.port}, useHttps: ${config.useHttps}")
        Napier.i("[ConnectionManager] Auth - Username: ${config.username}, HasPassword: ${config.password.isNotBlank()}")
        
        // Cache the Base64-encoded credentials once per connection (optimization)
        // This avoids re-encoding on every HTTP request
        val authHeader = if (config.hasCredentials) {
            val credentials = "${config.username}:${config.password}"
            
            val encoded = kotlin.io.encoding.Base64.Default.encode(credentials.encodeToByteArray())
            "Basic $encoded"
        } else null
        
        return HttpClient(getHttpEngine()) {
            expectSuccess = true
            defaultRequest {
                url(config.baseUrl + "/")
                // Use cached auth header instead of computing on every request
                if (authHeader != null) {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = NetworkConfig.REQUEST_TIMEOUT_MS
                connectTimeoutMillis = NetworkConfig.CONNECT_TIMEOUT_MS
                socketTimeoutMillis = NetworkConfig.SOCKET_TIMEOUT_MS
            }
            install(WebSockets)
            install(io.ktor.client.plugins.sse.SSE)
        }
    }

    private fun createBridgeClient(): HttpClient {
        Napier.i("[ConnectionManager] Creating MOCCA CLI bridge HttpClient")
        return HttpClient(getHttpEngine()) {
            expectSuccess = true
            install(HttpTimeout) {
                requestTimeoutMillis = NetworkConfig.REQUEST_TIMEOUT_MS
                connectTimeoutMillis = NetworkConfig.CONNECT_TIMEOUT_MS
                socketTimeoutMillis = NetworkConfig.SOCKET_TIMEOUT_MS
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets)
        }
    }

    // Utility


    fun getConnectionSummary(): String {
        return when (val state = _status.value) {
            is ConnectionStatus.NotConfigured -> "No server configured"
            is ConnectionStatus.Disconnected -> state.reason ?: "Disconnected"
            is ConnectionStatus.Connecting -> "Connecting..."
            is ConnectionStatus.WaitingForNetwork -> "Waiting for network..."
            is ConnectionStatus.Reconnecting -> "Reconnecting (${state.attempt}/${state.maxAttempts})..."
            is ConnectionStatus.Connected -> "Connected to ${_activeConfig.value?.name ?: "server"}"
            is ConnectionStatus.Error -> state.message
        }
    }

    fun getLastSyncTimeFormatted(): String? {
        val lastCheck = lastSuccessfulCheckMs ?: return null
        val ageMs = Clock.System.now().toEpochMilliseconds() - lastCheck
        val ageMinutes = (ageMs / 60000).toInt()
        return when {
            ageMinutes < 1 -> "Just now"
            ageMinutes < 60 -> "${ageMinutes}m ago"
            else -> "${ageMinutes / 60}h ago"
        }
    }

    private fun cancelAllJobs() {
        healthJob?.cancel()
        reconnectJob?.cancel()
        healthJob = null
        reconnectJob = null
    }

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5
        /**
         * Interval for periodic health checks.
         *
         * CHANGED from 5 minutes to 30 seconds for "ALWAYS FRESH" requirement.
         * The user's #1 priority is that data is ALWAYS synchronized with the server.
         * Faster health checks mean faster detection of connection issues.
         */
        private const val PERIODIC_CHECK_INTERVAL_MS = 30 * 1000L // 30 seconds
    }
}
