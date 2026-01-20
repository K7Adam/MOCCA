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

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException

/**
 * Exception types that should trigger a retry.
 */
fun Throwable.isRetryable(): Boolean = when (this) {
    is HttpRequestTimeoutException -> true
    is ConnectTimeoutException -> true
    is SocketTimeoutException -> true
    is ServerResponseException -> {
        // Retry on 5xx errors (server errors), but not 4xx (client errors)
        val code = response.status.value
        code >= 500
    }
    is ConnectException -> true
    is SocketException -> true
    is UnknownHostException -> true
    is java.io.IOException -> true
    else -> {
        // Fallback for wrapped exceptions
        val cause = cause
        if (cause != null && cause !== this) {
            cause.isRetryable()
        } else {
            false
        }
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
// END OF FILE
// ═══════════════════════════════════════════════════════════════════════════════
