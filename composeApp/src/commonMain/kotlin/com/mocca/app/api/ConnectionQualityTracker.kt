package com.mocca.app.api

import com.mocca.app.domain.model.ConnectionQuality
import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tracks connection quality metrics for adaptive behavior.
 * 
 * Uses sliding window of latency samples and success/failure rates to determine
 * connection quality, which then drives adaptive heartbeat intervals and
 * reconnection behavior.
 */
class ConnectionQualityTracker {
    
    private val mutex = Mutex()
    
    // Circular buffer for latency samples
    private val latencyHistory = CircularBuffer<Long>(NetworkConfig.QUALITY_SAMPLE_SIZE)
    
    // Circular buffer for success/failure tracking (true = success)
    private val successHistory = CircularBuffer<Boolean>(50)
    
    // Track consecutive failures for quick degradation detection
    private var consecutiveFailures = 0
    
    // Last calculated quality (cached for quick access)
    @Volatile
    private var cachedQuality: ConnectionQuality = ConnectionQuality.UNKNOWN
    
    /**
     * Record a latency measurement.
     */
    suspend fun recordLatency(latencyMs: Long) {
        mutex.withLock {
            latencyHistory.add(latencyMs)
            recalculateQuality()
        }
        Napier.v("[QualityTracker] Recorded latency: ${latencyMs}ms, quality: $cachedQuality")
    }
    
    /**
     * Record a success or failure.
     */
    suspend fun recordResult(success: Boolean) {
        mutex.withLock {
            successHistory.add(success)
            if (success) {
                consecutiveFailures = 0
            } else {
                consecutiveFailures++
            }
            recalculateQuality()
        }
        Napier.v("[QualityTracker] Recorded result: $success, consecutive failures: $consecutiveFailures, quality: $cachedQuality")
    }
    
    /**
     * Get the current connection quality.
     */
    fun getQuality(): ConnectionQuality = cachedQuality
    
    /**
     * Get the recommended heartbeat interval based on current quality.
     */
    fun getRecommendedHeartbeatInterval(): Long {
        return when (cachedQuality) {
            ConnectionQuality.EXCELLENT -> NetworkConfig.HEARTBEAT_EXCELLENT_MS
            ConnectionQuality.GOOD -> NetworkConfig.HEARTBEAT_GOOD_MS
            ConnectionQuality.DEGRADED -> NetworkConfig.HEARTBEAT_DEGRADED_MS
            ConnectionQuality.POOR -> NetworkConfig.HEARTBEAT_POOR_MS
            ConnectionQuality.OFFLINE -> NetworkConfig.HEARTBEAT_POOR_MS
            ConnectionQuality.UNKNOWN -> NetworkConfig.HEARTBEAT_GOOD_MS
        }
    }
    
    /**
     * Get the recommended reconnection delay multiplier.
     * Higher quality = shorter delays, lower quality = longer delays.
     */
    fun getReconnectDelayMultiplier(): Float {
        return when (cachedQuality) {
            ConnectionQuality.EXCELLENT -> 0.5f
            ConnectionQuality.GOOD -> 1.0f
            ConnectionQuality.DEGRADED -> 1.5f
            ConnectionQuality.POOR -> 2.0f
            ConnectionQuality.OFFLINE -> 2.0f
            ConnectionQuality.UNKNOWN -> 1.0f
        }
    }
    
    /**
     * Get average latency from recent samples.
     */
    suspend fun getAverageLatency(): Long? {
        return mutex.withLock {
            if (latencyHistory.isEmpty()) null
            else latencyHistory.toList().average().toLong()
        }
    }
    
    /**
     * Get success rate from recent samples.
     */
    suspend fun getSuccessRate(): Float? {
        return mutex.withLock {
            if (successHistory.isEmpty()) null
            else successHistory.toList().count { it }.toFloat() / successHistory.size
        }
    }
    
    /**
     * Reset all tracking data.
     */
    suspend fun reset() {
        mutex.withLock {
            latencyHistory.clear()
            successHistory.clear()
            consecutiveFailures = 0
            cachedQuality = ConnectionQuality.UNKNOWN
        }
        Napier.i("[QualityTracker] Reset quality tracking")
    }
    
    /**
     * Recalculate quality based on current metrics.
     */
    private fun recalculateQuality() {
        // Quick degradation check - 3+ consecutive failures = poor
        if (consecutiveFailures >= 3) {
            cachedQuality = ConnectionQuality.POOR
            return
        }
        
        // Calculate from history
        val latencies = latencyHistory.toList()
        val successes = successHistory.toList()
        
        if (latencies.isEmpty() && successes.isEmpty()) {
            cachedQuality = ConnectionQuality.UNKNOWN
            return
        }
        
        val avgLatency = if (latencies.isNotEmpty()) latencies.average().toLong() else 0L
        val successRate = if (successes.isNotEmpty()) successes.count { it }.toFloat() / successes.size else 1.0f
        
        cachedQuality = when {
            successRate < 0.5f -> ConnectionQuality.POOR
            avgLatency > NetworkConfig.LATENCY_DEGRADED_THRESHOLD_MS -> ConnectionQuality.POOR
            avgLatency > NetworkConfig.LATENCY_GOOD_THRESHOLD_MS -> ConnectionQuality.DEGRADED
            successRate < 0.8f -> ConnectionQuality.DEGRADED
            avgLatency > NetworkConfig.LATENCY_EXCELLENT_THRESHOLD_MS -> ConnectionQuality.GOOD
            successRate > 0.95f -> ConnectionQuality.EXCELLENT
            else -> ConnectionQuality.GOOD
        }
    }
    
    /**
     * Get a summary string for debugging/logging.
     */
    suspend fun getSummary(): String {
        return mutex.withLock {
            val latencies = latencyHistory.toList()
            val successes = successHistory.toList()
            val avgLatency = if (latencies.isNotEmpty()) latencies.average().toLong() else -1L
            val successRate = if (successes.isNotEmpty()) "%.0f%%".format(successes.count { it }.toFloat() / successes.size * 100) else "N/A"
            
            "Quality: $cachedQuality, Avg Latency: ${avgLatency}ms, Success: $successRate, Consecutive Failures: $consecutiveFailures"
        }
    }
}

/**
 * Simple circular buffer implementation.
 */
private class CircularBuffer<T>(private val capacity: Int) {
    private val buffer = ArrayList<T>(capacity)
    
    fun add(item: T) {
        if (buffer.size >= capacity) {
            buffer.removeAt(0)
        }
        buffer.add(item)
    }
    
    fun toList(): List<T> = buffer.toList()
    
    fun isEmpty(): Boolean = buffer.isEmpty()
    
    val size: Int get() = buffer.size
    
    fun clear() = buffer.clear()
}
