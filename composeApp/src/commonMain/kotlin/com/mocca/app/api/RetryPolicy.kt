package com.mocca.app.api

import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
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
    
    companion object {
        fun from(throwable: Throwable): NetworkError {
            return when {
                throwable is NetworkError -> throwable
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
