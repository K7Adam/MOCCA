package com.mocca.app.data.repository

import com.mocca.app.api.getHttpEngine
import com.mocca.app.domain.model.AppInfo
import com.mocca.app.domain.model.AuthType
import com.mocca.app.domain.model.ServerConfig
import com.mocca.app.util.NetworkObserver
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.math.min

sealed class AppConnectionState {
    data object NotConfigured : AppConnectionState()
    data object Checking : AppConnectionState()
    data object WaitingForNetwork : AppConnectionState()
    data class Connecting(val attempt: Int, val maxAttempts: Int) : AppConnectionState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : AppConnectionState()
    data class Connected(
        val serverInfo: AppInfo?,
        val checkedAt: Long,
        val latencyMs: Long = 0
    ) : AppConnectionState()
    data class Disconnected(val error: String?, val checkedAt: Long) : AppConnectionState()
    
    val isConnected: Boolean get() = this is Connected
    val isChecking: Boolean get() = this is Checking || this is Connecting || this is Reconnecting
    val canRetry: Boolean get() = this is Disconnected || this is NotConfigured || this is WaitingForNetwork
}

data class ConnectionHealth(
    val state: AppConnectionState,
    val isNetworkAvailable: Boolean,
    val lastSuccessfulCheck: Long?,
    val consecutiveFailures: Int
) {
    val dataFreshness: DataFreshness get() {
        val lastCheck = lastSuccessfulCheck ?: return DataFreshness.UNKNOWN
        val ageMs = Clock.System.now().toEpochMilliseconds() - lastCheck
        return when {
            ageMs < 5 * 60 * 1000 -> DataFreshness.FRESH           // <5 min
            ageMs < 30 * 60 * 1000 -> DataFreshness.SLIGHTLY_STALE // 5-30 min
            else -> DataFreshness.STALE                             // >30 min
        }
    }
}

enum class DataFreshness {
    FRESH,
    SLIGHTLY_STALE,
    STALE,
    UNKNOWN
}

class AppConnectionManager(
    private val serverConfigRepository: ServerConfigRepository,
    private val sessionRepository: SessionRepository,
    private val eventStreamRepository: EventStreamRepository,
    private val networkObserver: NetworkObserver? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _connectionState = MutableStateFlow<AppConnectionState>(AppConnectionState.NotConfigured)
    val connectionState: StateFlow<AppConnectionState> = _connectionState.asStateFlow()
    
    private val _activeServer = MutableStateFlow<ServerConfig?>(null)
    val activeServer: StateFlow<ServerConfig?> = _activeServer.asStateFlow()
    
    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()
    
    private val _lastSuccessfulCheck = MutableStateFlow<Long?>(null)
    private val _consecutiveFailures = MutableStateFlow(0)
    private val _hasEverConnected = MutableStateFlow(false)
    
    val connectionHealth: StateFlow<ConnectionHealth> = combine(
        _connectionState,
        _isNetworkAvailable,
        _lastSuccessfulCheck,
        _consecutiveFailures
    ) { state, networkAvailable, lastCheck, failures ->
        ConnectionHealth(state, networkAvailable, lastCheck, failures)
    }.stateIn(scope, SharingStarted.Eagerly, ConnectionHealth(
        AppConnectionState.NotConfigured, true, null, 0
    ))
    
    private var httpClient: HttpClient? = null
    private var reconnectJob: Job? = null
    private var periodicCheckJob: Job? = null
    private var networkObserverJob: Job? = null
    private var dataRefreshJob: Job? = null
    
    private val maxReconnectAttempts = 5
    private val periodicCheckIntervalMs = 5 * 60 * 1000L // 5 minutes
    
    init {
        observeActiveServer()
        observeNetworkState()
    }
    
    private fun observeActiveServer() {
        scope.launch {
            serverConfigRepository.activeServer.collect { server ->
                val previousServer = _activeServer.value
                _activeServer.value = server
                
                if (server == null) {
                    cancelAllJobs()
                    _connectionState.value = AppConnectionState.NotConfigured
                    _lastSuccessfulCheck.value = null
                    _consecutiveFailures.value = 0
                    return@collect
                }
                
                if (previousServer?.id != server.id || previousServer.baseUrl != server.baseUrl) {
                    Napier.i("Active server changed: ${server.name}")
                    _lastSuccessfulCheck.value = null
                    _consecutiveFailures.value = 0
                    _hasEverConnected.value = false
                    checkConnection()
                }
            }
        }
    }
    
    private fun observeNetworkState() {
        networkObserverJob?.cancel()
        networkObserverJob = scope.launch {
            networkObserver?.isOnline?.collect { isOnline ->
                val wasOffline = !_isNetworkAvailable.value
                _isNetworkAvailable.value = isOnline
                Napier.d("Network state changed: online=$isOnline")
                
                if (isOnline && wasOffline) {
                    handleNetworkRestored()
                } else if (!isOnline && _connectionState.value.isConnected) {
                    _connectionState.value = AppConnectionState.WaitingForNetwork
                }
            }
        }
    }
    
    private fun handleNetworkRestored() {
        val currentState = _connectionState.value
        if (currentState is AppConnectionState.WaitingForNetwork ||
            currentState is AppConnectionState.Disconnected) {
            Napier.i("Network restored, scheduling reconnection...")
            scope.launch {
                delay(2000) // Brief delay to ensure connection stability
                if (_isNetworkAvailable.value) {
                    _consecutiveFailures.value = 0
                    checkConnection()
                }
            }
        }
    }
    
    fun checkConnection() {
        val server = _activeServer.value
        if (server == null) {
            _connectionState.value = AppConnectionState.NotConfigured
            return
        }
        
        // Skip if already connected or checking to prevent double connections
        val currentState = _connectionState.value
        if (currentState.isConnected || currentState.isChecking) {
            Napier.d("Skipping checkConnection, already ${currentState::class.simpleName}")
            return
        }
        
        if (networkObserver?.isCurrentlyOnline() == false) {
            _connectionState.value = AppConnectionState.WaitingForNetwork
            return
        }
        
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _connectionState.value = AppConnectionState.Checking
            
            // Perform health check on IO dispatcher to avoid blocking Main thread
            val result = withContext(Dispatchers.IO) {
                val startTime = Clock.System.now().toEpochMilliseconds()
                val healthResult = performHealthCheck(server)
                val latencyMs = Clock.System.now().toEpochMilliseconds() - startTime
                Pair(healthResult, latencyMs)
            }
            
            val (checkResult, latencyMs) = result
            
            checkResult.fold(
                onSuccess = { appInfo ->
                    Napier.i("Connection to ${server.name} successful (${latencyMs}ms)")
                    _consecutiveFailures.value = 0
                    _hasEverConnected.value = true
                    _lastSuccessfulCheck.value = Clock.System.now().toEpochMilliseconds()
                    _connectionState.value = AppConnectionState.Connected(
                        serverInfo = appInfo,
                        checkedAt = Clock.System.now().toEpochMilliseconds(),
                        latencyMs = latencyMs
                    )
                    // Only start new data refresh if not already running
                    if (dataRefreshJob?.isActive != true) {
                        dataRefreshJob = scope.launch(Dispatchers.IO) {
                            try {
                                onConnectionEstablished()
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                Napier.d("Data refresh cancelled")
                                throw e
                            }
                        }
                    }
                    startPeriodicHealthCheck()
                },
                onFailure = { error ->
                    Napier.w("Connection to ${server.name} failed: ${error.message}")
                    _consecutiveFailures.value = _consecutiveFailures.value + 1
                    
                    if (_consecutiveFailures.value < maxReconnectAttempts) {
                        scheduleReconnect()
                    } else {
                        _connectionState.value = AppConnectionState.Disconnected(
                            error = error.message,
                            checkedAt = Clock.System.now().toEpochMilliseconds()
                        )
                    }
                }
            )
        }
    }
    
    private fun scheduleReconnect() {
        val attempt = _consecutiveFailures.value
        _connectionState.value = if (_hasEverConnected.value) {
            AppConnectionState.Reconnecting(attempt, maxReconnectAttempts)
        } else {
            AppConnectionState.Connecting(attempt, maxReconnectAttempts)
        }
        
        reconnectJob = scope.launch {
            val delayMs = calculateBackoff(attempt)
            Napier.i("Reconnecting in ${delayMs}ms (attempt $attempt/$maxReconnectAttempts)")
            delay(delayMs)
            
            if (isActive && _isNetworkAvailable.value) {
                _connectionState.value = AppConnectionState.Checking
                checkConnection()
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
    
    private fun startPeriodicHealthCheck() {
        periodicCheckJob?.cancel()
        periodicCheckJob = scope.launch {
            while (isActive) {
                delay(periodicCheckIntervalMs)
                
                if (_connectionState.value.isConnected && _isNetworkAvailable.value) {
                    Napier.d("Performing periodic health check...")
                    performSilentHealthCheck()
                }
            }
        }
    }
    
    private suspend fun performSilentHealthCheck() {
        val server = _activeServer.value ?: return
        
        // Move network call to IO
        val result = withContext(Dispatchers.IO) {
            performHealthCheck(server)
        }
        
        result.fold(
            onSuccess = { appInfo ->
                _lastSuccessfulCheck.value = Clock.System.now().toEpochMilliseconds()
                val currentState = _connectionState.value
                if (currentState is AppConnectionState.Connected) {
                    _connectionState.value = currentState.copy(
                        checkedAt = Clock.System.now().toEpochMilliseconds()
                    )
                }
            },
            onFailure = { error ->
                Napier.w("Periodic health check failed: ${error.message}")
                _consecutiveFailures.value = _consecutiveFailures.value + 1
                if (_consecutiveFailures.value >= 2) {
                    _connectionState.value = AppConnectionState.Disconnected(
                        error = error.message,
                        checkedAt = Clock.System.now().toEpochMilliseconds()
                    )
                    checkConnection() // Trigger reconnection flow
                }
            }
        )
    }
    
    private suspend fun performHealthCheck(server: ServerConfig): Result<AppInfo?> {
        return try {
            val client = getOrCreateClient()
            val response = client.get("${server.baseUrl.trimEnd('/')}/global/health") {
                // Add authentication headers if configured
                if (server.authType == AuthType.BASIC && !server.authToken.isNullOrEmpty()) {
                    header(HttpHeaders.Authorization, "Basic ${server.authToken}")
                } else if (server.authType == AuthType.BEARER && !server.authToken.isNullOrEmpty()) {
                    header(HttpHeaders.Authorization, "Bearer ${server.authToken}")
                }
            }
            
            if (response.status.value in 200..299) {
                val appInfo = try {
                    response.body<AppInfo>()
                } catch (e: Exception) {
                    null
                }
                Result.success(appInfo)
            } else {
                Result.failure(Exception("Server returned ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getOrCreateClient(): HttpClient {
        if (httpClient == null) {
            httpClient = HttpClient(getHttpEngine()) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true; isLenient = true })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 5_000
                    connectTimeoutMillis = 3_000
                    socketTimeoutMillis = 5_000
                }
            }
        }
        return httpClient!!
    }
    
    private suspend fun onConnectionEstablished() {
        Napier.i("Connection established, refreshing data...")
        sessionRepository.refreshSessions()
        sessionRepository.loadDefaultConfig()
    }
    
    private fun cancelAllJobs() {
        reconnectJob?.cancel()
        periodicCheckJob?.cancel()
        dataRefreshJob?.cancel()
        reconnectJob = null
        periodicCheckJob = null
        dataRefreshJob = null
    }
    
    fun triggerDataRefresh() {
        if (_connectionState.value.isConnected) {
            scope.launch(Dispatchers.IO) {
                sessionRepository.refreshSessions()
            }
        }
    }
    
    fun getConnectionSummary(): String {
        return when (val state = _connectionState.value) {
            is AppConnectionState.NotConfigured -> "No server configured"
            is AppConnectionState.Checking -> "Checking connection..."
            is AppConnectionState.WaitingForNetwork -> "Waiting for network..."
            is AppConnectionState.Connecting -> "Connecting (attempt ${state.attempt}/${state.maxAttempts})..."
            is AppConnectionState.Reconnecting -> "Reconnecting (${state.attempt}/${state.maxAttempts})..."
            is AppConnectionState.Connected -> "Connected to ${_activeServer.value?.name ?: "server"}"
            is AppConnectionState.Disconnected -> state.error ?: "Connection failed"
        }
    }
    
    fun getLastSyncTimeFormatted(): String? {
        val lastCheck = _lastSuccessfulCheck.value ?: return null
        val ageMs = Clock.System.now().toEpochMilliseconds() - lastCheck
        val ageMinutes = (ageMs / 60000).toInt()
        return when {
            ageMinutes < 1 -> "Just now"
            ageMinutes < 60 -> "${ageMinutes}m ago"
            else -> "${ageMinutes / 60}h ago"
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SMART CONNECTION RECOVERY WITH DISCOVERY (New for Zero-Config UX)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Try to connect using a list of potential servers.
     * This implements the smart recovery that tries discovered servers first,
     * then saved servers, then falls back to defaults.
     * 
     * @param candidateServers List of servers to try in order
     * @return true if connected to any server, false otherwise
     */
    suspend fun connectWithDiscovery(candidateServers: List<ServerConfig>): Boolean {
        if (candidateServers.isEmpty()) {
            Napier.w("No candidate servers provided for discovery connection")
            return false
        }
        
        Napier.i("Attempting smart connection with ${candidateServers.size} candidate servers")
        
        for ((index, server) in candidateServers.withIndex()) {
            if (!_isNetworkAvailable.value) {
                Napier.w("Network unavailable, aborting discovery connection")
                _connectionState.value = AppConnectionState.WaitingForNetwork
                return false
            }
            
            Napier.d("Trying server ${index + 1}/${candidateServers.size}: ${server.name} at ${server.baseUrl}")
            _connectionState.value = AppConnectionState.Connecting(
                attempt = index + 1,
                maxAttempts = candidateServers.size
            )
            
            // Set this server as active temporarily
            serverConfigRepository.setActiveServer(server.id)
            
            val result = withContext(Dispatchers.IO) {
                performHealthCheck(server)
            }
            
            result.fold(
                onSuccess = { appInfo ->
                    Napier.i("Successfully connected to ${server.name} via discovery")
                    _consecutiveFailures.value = 0
                    _hasEverConnected.value = true
                    _lastSuccessfulCheck.value = Clock.System.now().toEpochMilliseconds()
                    _connectionState.value = AppConnectionState.Connected(
                        serverInfo = appInfo,
                        checkedAt = Clock.System.now().toEpochMilliseconds(),
                        latencyMs = 0
                    )
                    
                    // Save this successful server if not already saved
                    scope.launch {
                        try {
                            val existingServers = serverConfigRepository.getAllServers()
                            if (existingServers.none { it.baseUrl == server.baseUrl }) {
                                serverConfigRepository.saveServer(server)
                                Napier.d("Saved new discovered server: ${server.name}")
                            }
                        } catch (e: Exception) {
                            Napier.w("Could not save discovered server", e)
                        }
                    }
                    
                    startPeriodicHealthCheck()
                    return true
                },
                onFailure = { error ->
                    Napier.d("Failed to connect to ${server.name}: ${error.message}")
                    // Continue to next server
                }
            )
        }
        
        // All servers failed
        Napier.w("All ${candidateServers.size} candidate servers failed")
        _consecutiveFailures.value = maxReconnectAttempts
        _connectionState.value = AppConnectionState.Disconnected(
            error = "Could not connect to any discovered server. Please check your OpenCode server.",
            checkedAt = Clock.System.now().toEpochMilliseconds()
        )
        return false
    }
    
    /**
     * Get a list of candidate servers to try in order of priority:
     * 1. Last successful server (if any)
     * 2. Saved servers
     * 3. Auto-detected emulator localhost
     * 4. Default configurations
     */
    suspend fun getCandidateServers(): List<ServerConfig> {
        val candidates = mutableListOf<ServerConfig>()
        
        // Add saved servers first
        try {
            val savedServers = serverConfigRepository.getAllServers()
            candidates.addAll(savedServers)
            Napier.d("Added ${savedServers.size} saved servers to candidates")
        } catch (e: Exception) {
            Napier.w("Could not load saved servers", e)
        }
        
        // Add auto-detected emulator config if not already present
        val emulatorConfig = ServerConfig(
            id = "emulator-auto",
            name = "Android Emulator",
            baseUrl = "http://10.0.2.2:4096",
            connectionType = com.mocca.app.domain.model.ConnectionType.LOCAL,
            authType = com.mocca.app.domain.model.AuthType.NONE,
            isActive = false
        )
        if (candidates.none { it.baseUrl == emulatorConfig.baseUrl }) {
            candidates.add(emulatorConfig)
            Napier.d("Added auto-detected emulator config")
        }
        
        return candidates.distinctBy { it.baseUrl }
    }
    
    /**
     * Smart connect that uses discovery, saved servers, and defaults in sequence.
     * This is the main entry point for the new zero-config onboarding.
     */
    fun smartConnect(discoveredServers: List<ServerConfig> = emptyList()) {
        scope.launch {
            val candidates = mutableListOf<ServerConfig>()
            
            // Prioritize discovered servers
            candidates.addAll(discoveredServers)
            
            // Add other candidates
            candidates.addAll(getCandidateServers())
            
            // Remove duplicates
            val uniqueCandidates = candidates.distinctBy { it.baseUrl }
            
            Napier.i("Smart connect with ${uniqueCandidates.size} unique candidates (${discoveredServers.size} discovered)")
            
            val connected = connectWithDiscovery(uniqueCandidates)
            
            if (!connected) {
                Napier.w("Smart connect failed to find any working server")
            }
        }
    }
    
    /**
     * Check if we're running on an Android emulator.
     * Useful for auto-configuring the 10.0.2.2 localhost mapping.
     */
    fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT?.startsWith("google/sdk_gphone") == true ||
                android.os.Build.FINGERPRINT?.startsWith("generic") == true ||
                android.os.Build.HARDWARE?.contains("goldfish") == true ||
                android.os.Build.HARDWARE?.contains("ranchu") == true ||
                android.os.Build.BRAND == "generic" ||
                android.os.Build.DEVICE?.startsWith("generic") == true)
    }
}
