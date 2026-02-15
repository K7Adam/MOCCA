package com.mocca.app.data.repository

import com.mocca.app.api.ApiExecutor
import com.mocca.app.api.ConnectionException
import com.mocca.app.api.NetworkConfig
import com.mocca.app.api.getHttpEngine
import com.mocca.app.domain.model.AppInfo
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.ServerConfig
import com.mocca.app.util.NetworkObserver
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import io.ktor.client.plugins.ClientRequestException
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
) : ApiExecutor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val clientMutex = Mutex()

    @Volatile
    private var currentClient: HttpClient? = null

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

    // ═══════════════════════════════════════════════════════════════════════════════
    // ApiExecutor Implementation
    // ═══════════════════════════════════════════════════════════════════════════════

    override suspend fun <T> execute(block: suspend HttpClient.() -> T): T {
        val client = currentClient ?: throw ConnectionException("Not connected to any server")
        return client.block()
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Connection Lifecycle
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Connect to a server with the given config.
     * Closes any existing connection first.
     */
    fun connect(config: ServerConfig) {
        scope.launch {
            disconnect()
            clientMutex.withLock {
                _activeConfig.value = config
                _status.value = ConnectionStatus.Connecting
                currentClient = createClient(config)
            }
            // Persist as active
            serverConfigRepository.setActiveServer(config.id)
            // Run initial health check
            consecutiveFailures = 0
            hasEverConnected = false
            checkConnection()
        }
    }

    /**
     * Disconnect from the current server.
     */
    fun disconnect() {
        cancelAllJobs()
        scope.launch {
            clientMutex.withLock {
                currentClient?.close()
                currentClient = null
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

    // ═══════════════════════════════════════════════════════════════════════════════
    // Server Config Observation
    // ═══════════════════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════════════════
    // Health Checks
    // ═══════════════════════════════════════════════════════════════════════════════

    private suspend fun performHealthCheck(): Result<AppInfo?> {
        return try {
            val appInfo = execute { get("/global/health").body<AppInfo>() }
            Result.success(appInfo)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                Result.failure(Exception("Authentication failed (401). Check username/password."))
            } else {
                Result.failure(Exception("Server returned ${e.response.status}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ConnectionException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
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
                Napier.w("[ConnectionManager] Health check failed: ${error.message}")
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
        val maxDelay = 30000L
        val exponentialDelay = baseDelay * (1L shl min(attempt - 1, 5))
        val jitter = (0..500).random().toLong()
        return min(exponentialDelay, maxDelay) + jitter
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HttpClient Factory
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun createClient(config: ServerConfig): HttpClient {
        return HttpClient(getHttpEngine()) {
            defaultRequest {
                url(config.baseUrl + "/")
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

            // Configure HTTP Basic Auth if credentials are present
            if (config.hasCredentials) {
                install(Auth) {
                    basic {
                        credentials {
                            BasicAuthCredentials(
                                username = config.username,
                                password = config.password
                            )
                        }
                        sendWithoutRequest { true }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Utility
    // ═══════════════════════════════════════════════════════════════════════════════

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
        private const val PERIODIC_CHECK_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }
}
