package com.mocca.app.api

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
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
import kotlinx.coroutines.withTimeout
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.system.measureTimeMillis

/**
 * Git Server health status.
 */
enum class GitServerHealthStatus {
    /** Server is running and responding normally */
    HEALTHY,
    /** Server is responding but slowly (>1000ms) */
    DEGRADED,
    /** Server is not responding (timeout or connection refused) */
    UNHEALTHY,
    /** Status unknown (not yet checked) */
    UNKNOWN
}

/**
 * Git Server health state with details.
 */
data class GitServerHealthState(
    val status: GitServerHealthStatus = GitServerHealthStatus.UNKNOWN,
    val lastCheckTime: Long = 0L,
    val responseTimeMs: Long? = null,
    val message: String = "Not checked",
    val consecutiveFailures: Int = 0,
    val url: String? = null
)

/**
 * Git Server Checker - Fast connectivity check utility with heartbeat support.
 * 
 * Enhanced per OPENCODE_API_ANALYSIS.md recommendation:
 * "Bootstrap Security: Hardcode the Git Server to port 4097 and implement a 
 * heart-beat check to ensure the background process is alive."
 */
object GitServerChecker {
    private const val TAG = "GitServerChecker"
    private const val QUICK_CHECK_TIMEOUT_MS = 500L
    private const val CONNECTION_TIMEOUT_MS = 1_000L
    
    // Heartbeat configuration
    private const val DEFAULT_HEARTBEAT_INTERVAL_MS = 30_000L  // 30 seconds
    private const val DEGRADED_THRESHOLD_MS = 1_000L
    private const val MAX_CONSECUTIVE_FAILURES = 3
    
    // Heartbeat state
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private val _healthState = MutableStateFlow(GitServerHealthState())
    
    /** Observable health state for UI binding */
    val healthState: StateFlow<GitServerHealthState> = _healthState.asStateFlow()
    
    /** Callback invoked when server becomes unhealthy after MAX_CONSECUTIVE_FAILURES */
    var onServerUnhealthy: (suspend (GitServerHealthState) -> Unit)? = null
    
    /** Callback invoked when server recovers from unhealthy state */
    var onServerRecovered: (suspend (GitServerHealthState) -> Unit)? = null

    /**
     * Start the heartbeat monitor.
     * 
     * @param httpClient Ktor HttpClient for health checks
     * @param serverUrl Base URL of the Git server (e.g., "http://127.0.0.1:4097")
     * @param intervalMs Heartbeat interval in milliseconds (default: 30s)
     */
    fun startHeartbeat(
        httpClient: HttpClient,
        serverUrl: String,
        intervalMs: Long = DEFAULT_HEARTBEAT_INTERVAL_MS
    ) {
        stopHeartbeat()  // Stop any existing heartbeat
        
        Napier.i("$TAG: Starting heartbeat monitor for $serverUrl (interval: ${intervalMs}ms)")
        
        heartbeatJob = scope.launch {
            var wasUnhealthy = false
            
            while (isActive) {
                val result = checkServerRunning(httpClient, serverUrl)
                val newState = updateHealthState(result, serverUrl)
                
                // Check for state transitions
                if (newState.status == GitServerHealthStatus.UNHEALTHY) {
                    if (newState.consecutiveFailures >= MAX_CONSECUTIVE_FAILURES && !wasUnhealthy) {
                        wasUnhealthy = true
                        Napier.e("$TAG: Server unhealthy after ${newState.consecutiveFailures} failures")
                        onServerUnhealthy?.invoke(newState)
                    }
                } else if (wasUnhealthy && newState.status == GitServerHealthStatus.HEALTHY) {
                    wasUnhealthy = false
                    Napier.i("$TAG: Server recovered!")
                    onServerRecovered?.invoke(newState)
                }
                
                delay(intervalMs)
            }
        }
    }
    
    /**
     * Stop the heartbeat monitor.
     */
    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        Napier.d("$TAG: Heartbeat monitor stopped")
    }
    
    /**
     * Check if heartbeat is currently running.
     */
    fun isHeartbeatRunning(): Boolean = heartbeatJob?.isActive == true
    
    /**
     * Perform a single health check and update state.
     */
    suspend fun performHealthCheck(httpClient: HttpClient, serverUrl: String): GitServerHealthState {
        val result = checkServerRunning(httpClient, serverUrl)
        return updateHealthState(result, serverUrl)
    }
    
    private fun updateHealthState(
        checkResult: Triple<Boolean?, String, Long?>,
        serverUrl: String
    ): GitServerHealthState {
        val currentState = _healthState.value
        val newState = when {
            isServerAvailable(checkResult) -> {
                val status = if (isServerSlow(checkResult)) {
                    GitServerHealthStatus.DEGRADED
                } else {
                    GitServerHealthStatus.HEALTHY
                }
                GitServerHealthState(
                    status = status,
                    lastCheckTime = System.currentTimeMillis(),
                    responseTimeMs = checkResult.third,
                    message = checkResult.second,
                    consecutiveFailures = 0,
                    url = serverUrl
                )
            }
            else -> {
                GitServerHealthState(
                    status = GitServerHealthStatus.UNHEALTHY,
                    lastCheckTime = System.currentTimeMillis(),
                    responseTimeMs = null,
                    message = checkResult.second,
                    consecutiveFailures = currentState.consecutiveFailures + 1,
                    url = serverUrl
                )
            }
        }
        
        _healthState.value = newState
        return newState
    }
    
    /**
     * Get current health status without performing a new check.
     */
    fun getCurrentHealth(): GitServerHealthState = _healthState.value

    suspend fun checkServerRunning(
        httpClient: HttpClient,
        url: String
    ): Triple<Boolean?, String, Long?> {
        var responseTime: Long = 0
        return try {
            withTimeout(CONNECTION_TIMEOUT_MS) {
                var status: HttpStatusCode? = null
                responseTime = measureTimeMillis {
                    val response = httpClient.get(url) {
                        timeout {
                            requestTimeoutMillis = QUICK_CHECK_TIMEOUT_MS
                            connectTimeoutMillis = QUICK_CHECK_TIMEOUT_MS
                        }
                    }
                    status = response.status
                }

                when (status) {
                    HttpStatusCode.OK -> {
                        Triple(true, "Git server is running", responseTime)
                    }
                    HttpStatusCode.NotFound -> {
                        Triple(false, "Git server not found (endpoint missing)", null)
                    }
                    else -> {
                        Triple(false, "Git server error: $status", null)
                    }
                }
            }
        } catch (e: ConnectException) {
            Napier.d("$TAG: Connection refused - server not running")
            Triple(false, "Git server is not running", null)
        } catch (e: SocketTimeoutException) {
            Napier.w("$TAG: Connection timed out - server may be slow or not listening")
            Triple(null, "Git server not responding (timeout)", null)
        } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
            // Handle Ktor's HttpRequestTimeoutException - this is expected when server is not running
            Napier.d("$TAG: Quick check timeout - server not available (${e.message})")
            Triple(false, "Git server is not running", null)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            // Handle withTimeout cancellation - server didn't respond within 1s
            Napier.d("$TAG: Connection timeout - server not responding")
            Triple(null, "Git server not responding (timeout)", null)
        } catch (e: Exception) {
            Napier.e("$TAG: Unexpected error: ${e.message}", e)
            Triple(false, "Connection failed: ${e.message}", null)
        }
    }

    fun isServerAvailable(checkResult: Triple<Boolean?, String, Long?>): Boolean {
        return checkResult.first == true
    }

    fun isServerUnavailable(checkResult: Triple<Boolean?, String, Long?>): Boolean {
        return checkResult.first == false && checkResult.second.contains("not running")
    }

    fun isServerSlow(checkResult: Triple<Boolean?, String, Long?>): Boolean {
        return checkResult.third != null && checkResult.third!! > DEGRADED_THRESHOLD_MS
    }

    fun getStatusMessage(checkResult: Triple<Boolean?, String, Long?>): String {
        return when {
            isServerAvailable(checkResult) -> {
                if (isServerSlow(checkResult)) {
                    "Git server is running (slow response: ${checkResult.third}ms)"
                } else {
                    "Git server is running and responding"
                }
            }
            isServerUnavailable(checkResult) -> "Git server is not running"
            else -> "Cannot connect to Git server (connection timeout)"
        }
    }

    suspend fun checkPort(
        httpClient: HttpClient,
        host: String,
        port: Int
    ): Triple<Boolean?, String, Long?> {
        val url = "http://$host:$port/health"
        return checkServerRunning(httpClient, url)
    }
}
