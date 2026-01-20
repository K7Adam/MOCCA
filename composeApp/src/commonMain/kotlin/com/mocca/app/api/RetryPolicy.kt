package com.mocca.app.api

import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Retry policy for network requests.
 * Implements exponential backoff with jitter.
 */
data class RetryPolicy(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val factor: Double = 2.0,
    val jitterPercent: Double = 0.1
) {
    companion object {
        val Default = RetryPolicy()
        val Aggressive = RetryPolicy(maxRetries = 5, initialDelayMs = 500)
        val Relaxed = RetryPolicy(maxRetries = 2, initialDelayMs = 2000)
        val None = RetryPolicy(maxRetries = 0)
    }
    
    /**
     * Calculate delay for a given retry attempt (0-indexed).
     */
    fun delayForAttempt(attempt: Int): Long {
        val baseDelay = initialDelayMs * factor.pow(attempt.toDouble())
        val cappedDelay = min(baseDelay.toLong(), maxDelayMs)
        val jitter = (cappedDelay * jitterPercent * Random.nextDouble()).toLong()
        return cappedDelay + jitter
    }
}

/**
 * Exception types that should trigger a retry.
 */
fun Throwable.isRetryable(): Boolean = when (this) {
    is java.net.SocketTimeoutException -> true
    is java.net.ConnectException -> true
    is java.net.UnknownHostException -> true
    is java.io.IOException -> true
    else -> {
        // Check for Ktor-specific exceptions via message
        message?.contains("timeout", ignoreCase = true) == true ||
        message?.contains("connection", ignoreCase = true) == true ||
        message?.contains("network", ignoreCase = true) == true
    }
}

/**
 * Execute a block with retry logic.
 */
suspend fun <T> withRetry(
    policy: RetryPolicy = RetryPolicy.Default,
    tag: String = "API",
    block: suspend () -> T
): Result<T> {
    var lastException: Throwable? = null
    
    repeat(policy.maxRetries + 1) { attempt ->
        try {
            return Result.success(block())
        } catch (e: Throwable) {
            // CancellationException should propagate immediately, not be treated as error
            if (e is kotlinx.coroutines.CancellationException) {
                Napier.d("[$tag] Cancelled")
                throw e
            }
            
            lastException = e
            
            if (attempt < policy.maxRetries && e.isRetryable()) {
                val delayMs = policy.delayForAttempt(attempt)
                Napier.w("[$tag] Attempt ${attempt + 1}/${policy.maxRetries + 1} failed, retrying in ${delayMs}ms: ${e.message}")
                delay(delayMs)
            } else if (!e.isRetryable()) {
                Napier.e("[$tag] Non-retryable error", e)
                return Result.failure(e)
            }
        }
    }
    
    Napier.e("[$tag] All ${policy.maxRetries + 1} attempts failed", lastException)
    return Result.failure(lastException ?: Exception("Unknown error after retries"))
}

// ═══════════════════════════════════════════════════════════════════════════════
// CIRCUIT BREAKER (Priority 4.1)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Circuit breaker state machine for network resilience.
 * 
 * States:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Circuit is tripped, requests fail fast without attempting network
 * - HALF_OPEN: Testing recovery, allows limited requests through
 * 
 * Transitions:
 * - CLOSED -> OPEN: When failure threshold is reached
 * - OPEN -> HALF_OPEN: After reset timeout expires
 * - HALF_OPEN -> CLOSED: On successful request
 * - HALF_OPEN -> OPEN: On failed request
 */
enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

/**
 * Configuration for the circuit breaker.
 */
data class CircuitBreakerConfig(
    /** Number of consecutive failures to trip the circuit */
    val failureThreshold: Int = 5,
    /** Time in milliseconds before attempting recovery */
    val resetTimeoutMs: Long = 30_000,
    /** Number of successful requests in HALF_OPEN to close the circuit */
    val successThreshold: Int = 2,
    /** Maximum requests allowed in HALF_OPEN state for testing */
    val halfOpenMaxRequests: Int = 3
) {
    companion object {
        val Default = CircuitBreakerConfig()
        val Aggressive = CircuitBreakerConfig(failureThreshold = 3, resetTimeoutMs = 15_000)
        val Relaxed = CircuitBreakerConfig(failureThreshold = 10, resetTimeoutMs = 60_000)
    }
}

/**
 * Thread-safe circuit breaker implementation.
 * 
 * Usage:
 * ```
 * val circuitBreaker = CircuitBreaker("MyService")
 * 
 * suspend fun fetchData(): Result<Data> {
 *     return circuitBreaker.execute {
 *         // Network call here
 *         apiClient.fetchData()
 *     }
 * }
 * ```
 */
class CircuitBreaker(
    private val name: String,
    private val config: CircuitBreakerConfig = CircuitBreakerConfig.Default
) {
    private val mutex = Mutex()
    
    @Volatile
    private var state: CircuitState = CircuitState.CLOSED
    
    @Volatile
    private var failureCount: Int = 0
    
    @Volatile
    private var successCount: Int = 0
    
    @Volatile
    private var lastFailureTime: Long = 0
    
    @Volatile
    private var halfOpenRequestCount: Int = 0
    
    /**
     * Current circuit state (for monitoring).
     */
    fun currentState(): CircuitState = state
    
    /**
     * Check if the circuit is allowing requests.
     */
    suspend fun isAllowed(): Boolean = mutex.withLock {
        when (state) {
            CircuitState.CLOSED -> true
            CircuitState.OPEN -> {
                val now = System.currentTimeMillis()
                if (now - lastFailureTime >= config.resetTimeoutMs) {
                    transitionTo(CircuitState.HALF_OPEN)
                    true
                } else {
                    false
                }
            }
            CircuitState.HALF_OPEN -> {
                halfOpenRequestCount < config.halfOpenMaxRequests
            }
        }
    }
    
    /**
     * Execute a block with circuit breaker protection.
     * 
     * @return Result.success if the call succeeded, Result.failure if circuit is open or call failed
     */
    suspend fun <T> execute(block: suspend () -> T): Result<T> {
        // Check if request is allowed
        val allowed = mutex.withLock {
            when (state) {
                CircuitState.CLOSED -> true
                CircuitState.OPEN -> {
                    val now = System.currentTimeMillis()
                    if (now - lastFailureTime >= config.resetTimeoutMs) {
                        transitionTo(CircuitState.HALF_OPEN)
                        halfOpenRequestCount++
                        true
                    } else {
                        false
                    }
                }
                CircuitState.HALF_OPEN -> {
                    if (halfOpenRequestCount < config.halfOpenMaxRequests) {
                        halfOpenRequestCount++
                        true
                    } else {
                        false
                    }
                }
            }
        }
        
        if (!allowed) {
            Napier.w("[$name] Circuit OPEN - request rejected (fast-fail)")
            return Result.failure(CircuitBreakerOpenException(name))
        }
        
        // Execute the block
        return try {
            val result = block()
            onSuccess()
            Result.success(result)
        } catch (e: Throwable) {
            // CancellationException should propagate, not count as failure
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }
            onFailure(e)
            Result.failure(e)
        }
    }
    
    /**
     * Record a successful request.
     */
    private suspend fun onSuccess() = mutex.withLock {
        when (state) {
            CircuitState.HALF_OPEN -> {
                successCount++
                if (successCount >= config.successThreshold) {
                    transitionTo(CircuitState.CLOSED)
                }
            }
            CircuitState.CLOSED -> {
                // Reset failure count on success in closed state
                failureCount = 0
            }
            CircuitState.OPEN -> {
                // Shouldn't happen, but handle gracefully
            }
        }
    }
    
    /**
     * Record a failed request.
     */
    private suspend fun onFailure(error: Throwable) = mutex.withLock {
        lastFailureTime = System.currentTimeMillis()
        
        when (state) {
            CircuitState.CLOSED -> {
                failureCount++
                if (failureCount >= config.failureThreshold) {
                    transitionTo(CircuitState.OPEN)
                }
            }
            CircuitState.HALF_OPEN -> {
                // Any failure in half-open trips the circuit again
                transitionTo(CircuitState.OPEN)
            }
            CircuitState.OPEN -> {
                // Already open, just update failure time
            }
        }
        
        Napier.w("[$name] Failure recorded: ${error.message} (failures=$failureCount, state=$state)")
    }
    
    /**
     * Transition to a new state.
     */
    private fun transitionTo(newState: CircuitState) {
        val oldState = state
        state = newState
        
        when (newState) {
            CircuitState.CLOSED -> {
                failureCount = 0
                successCount = 0
                halfOpenRequestCount = 0
                Napier.i("[$name] Circuit CLOSED - normal operation resumed")
            }
            CircuitState.OPEN -> {
                successCount = 0
                halfOpenRequestCount = 0
                Napier.w("[$name] Circuit OPEN - failing fast for ${config.resetTimeoutMs}ms")
            }
            CircuitState.HALF_OPEN -> {
                successCount = 0
                halfOpenRequestCount = 0
                Napier.i("[$name] Circuit HALF_OPEN - testing recovery")
            }
        }
    }
    
    /**
     * Force reset the circuit breaker to closed state.
     * Use for manual recovery or testing.
     */
    suspend fun reset() = mutex.withLock {
        transitionTo(CircuitState.CLOSED)
    }
    
    /**
     * Get current metrics for monitoring.
     */
    fun metrics(): CircuitBreakerMetrics = CircuitBreakerMetrics(
        name = name,
        state = state,
        failureCount = failureCount,
        successCount = successCount,
        lastFailureTime = lastFailureTime
    )
}

/**
 * Metrics for circuit breaker monitoring.
 */
data class CircuitBreakerMetrics(
    val name: String,
    val state: CircuitState,
    val failureCount: Int,
    val successCount: Int,
    val lastFailureTime: Long
)

/**
 * Exception thrown when circuit breaker is open.
 */
class CircuitBreakerOpenException(
    serviceName: String
) : Exception("Circuit breaker '$serviceName' is OPEN - request rejected")

/**
 * Execute a block with both circuit breaker and retry logic.
 */
suspend fun <T> withCircuitBreakerAndRetry(
    circuitBreaker: CircuitBreaker,
    policy: RetryPolicy = RetryPolicy.Default,
    tag: String = "API",
    block: suspend () -> T
): Result<T> {
    return circuitBreaker.execute {
        withRetry(policy, tag, block).getOrThrow()
    }
}

/**
 * Network error wrapper with additional context.
 */
sealed class NetworkError : Exception() {
    data class ConnectionFailed(override val message: String, override val cause: Throwable? = null) : NetworkError()
    data class Timeout(override val message: String, override val cause: Throwable? = null) : NetworkError()
    data class ServerError(val statusCode: Int, override val message: String) : NetworkError()
    data class AuthenticationFailed(override val message: String) : NetworkError()
    data class ParseError(override val message: String, override val cause: Throwable? = null) : NetworkError()
    data class Unknown(override val message: String, override val cause: Throwable? = null) : NetworkError()
    
    /**
     * Specialized error for when the Git HTTP server (port 4097) is not running.
     * This is NOT wrapped in Unknown so it can be detected by UI to show the start server dialog.
     */
    data class GitServerUnavailable(
        override val message: String = "Git server is not running",
        override val cause: Throwable? = null
    ) : NetworkError()
    
    companion object {
        fun from(throwable: Throwable): NetworkError {
            return when {
                throwable is NetworkError -> throwable
                // Preserve GitServerNotRunningException as GitServerUnavailable
                throwable::class.simpleName == "GitServerNotRunningException" ||
                throwable.message?.contains("Git server is not running", ignoreCase = true) == true ->
                    GitServerUnavailable(throwable.message ?: "Git server is not running", throwable)
                throwable.message?.contains("timeout", ignoreCase = true) == true -> 
                    Timeout("Request timed out", throwable)
                throwable.message?.contains("connection", ignoreCase = true) == true ->
                    ConnectionFailed("Connection failed", throwable)
                throwable.message?.contains("401") == true || 
                throwable.message?.contains("unauthorized", ignoreCase = true) == true ->
                    AuthenticationFailed("Authentication required")
                throwable.message?.contains("parse", ignoreCase = true) == true ||
                throwable.message?.contains("json", ignoreCase = true) == true ->
                    ParseError("Failed to parse response", throwable)
                else -> Unknown(throwable.message ?: "Unknown network error", throwable)
            }
        }
    }
}
