package com.mocca.app.api

/**
 * Centralized network configuration constants.
 * 
 * This object contains all network-related magic numbers to ensure consistency
 * across the application and make configuration changes easier.
 */
object NetworkConfig {
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TIMEOUT CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /** Request timeout in milliseconds (2 minutes) */
    const val REQUEST_TIMEOUT_MS = 120_000L
    
    /** Connect timeout in milliseconds (10 seconds) */
    const val CONNECT_TIMEOUT_MS = 10_000L
    
    /** Socket timeout in milliseconds (2 minutes) */
    const val SOCKET_TIMEOUT_MS = 120_000L
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // HTTP CLIENT LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /** Grace period before closing old HttpClient during recreation */
    const val CLIENT_GRACE_PERIOD_MS = 5_000L
    
    /** Timeout for force-closing old client */
    const val CLIENT_CLOSE_TIMEOUT_MS = 2_000L
    
    /** WebSocket ping interval (30 seconds) */
    const val WEBSOCKET_PING_INTERVAL_MS = 30_000L
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SSE (SERVER-SENT EVENTS) CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /** SharedFlow buffer capacity for SSE events (increased for high-throughput sessions) */
    const val SSE_BUFFER_CAPACITY = 512
    
    /** Maximum SSE reconnection attempts */
    const val SSE_MAX_RECONNECT_ATTEMPTS = 10
    
    /** Base delay for exponential backoff (1 second) */
    const val SSE_RECONNECT_BASE_DELAY_MS = 1_000L
    
    /** Maximum delay for exponential backoff (30 seconds) */
    const val SSE_RECONNECT_MAX_DELAY_MS = 30_000L
    
    /** Jitter range for reconnection delay (0-500ms) */
    const val SSE_RECONNECT_JITTER_MS = 500L
    
    /** Heartbeat timeout for SSE connection (60 seconds - increased for mobile networks) */
    const val SSE_HEARTBEAT_TIMEOUT_MS = 60_000L
    
    /** Heartbeat check interval (15 seconds) */
    const val SSE_HEARTBEAT_CHECK_INTERVAL_MS = 15_000L
    
    /** Event deduplication TTL (1 minute) */
    const val EVENT_DEDUP_TTL_MS = 60_000L
    
    /** Streaming text buffer max size before forced flush (OOM FIX: reduced from 100K→50K) */
    const val STREAMING_TEXT_MAX_SIZE = 50_000
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONNECTION QUALITY THRESHOLDS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    object ConnectionQuality {
        /** Number of consecutive failures before marking as offline */
        const val OFFLINE_FAILURE_THRESHOLD = 3
        
        /** Success rate below this is considered poor */
        const val POOR_SUCCESS_RATE = 0.5f
        
        /** Success rate below this is considered degraded */
        const val DEGRADED_SUCCESS_RATE = 0.8f
        
        /** Latency above this (ms) is considered poor */
        const val POOR_LATENCY_MS = 5_000L
        
        /** Latency above this (ms) is considered degraded */
        const val DEGRADED_LATENCY_MS = 2_000L
        
        /** Maximum samples for connection quality tracking */
        const val MAX_SAMPLE_SIZE = 50
        
        /** Maximum latency history size */
        const val MAX_LATENCY_HISTORY = 20
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // REQUEST DEDUPLICATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /** Maximum commands per minute for rate limiting */
    const val MAX_COMMANDS_PER_MINUTE = 10
    
    /** Rate limiting window in milliseconds (1 minute) */
    const val RATE_LIMIT_WINDOW_MS = 60_000L
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // RETRY POLICY
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /** Default max retries for idempotent operations */
    const val DEFAULT_MAX_RETRIES = 3
    
    /** Aggressive max retries for critical operations */
    const val AGGRESSIVE_MAX_RETRIES = 5
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SERVER PORTS & ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /** OpenCode server default port */
    const val OPENCODE_SERVER_PORT = 4242
    
    /** OpenCode Server Host (formerly EMULATOR_HOST_IP) */
    const val DEFAULT_HOST_IP = "100.73.93.117"

    /** OpenCode Default Username */
    const val DEFAULT_USERNAME = "adamk7"

    /** OpenCode Default Password */
    const val DEFAULT_PASSWORD = "znvQ1lLbZ4LDvR/ieanta+WSlXP2Mo+feK7Snf+rBZkBur8k+lPmLFDRrX2dumaf"
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CIRCUIT BREAKER
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /** Failure threshold before opening circuit */
    const val CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5
    
    /** Timeout duration for circuit breaker (30 seconds) */
    const val CIRCUIT_BREAKER_TIMEOUT_MS = 30_000L
    
    /** Recovery timeout for half-open state (5 seconds) */
    const val CIRCUIT_BREAKER_RECOVERY_MS = 5_000L
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // APP LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /** Delay before pausing SSE when app goes to background (allows for quick returns) */
    const val BACKGROUND_PAUSE_DELAY_MS = 5_000L
    
    /** Reduced health check interval when backgrounded (30 seconds) */
    const val BACKGROUND_HEALTH_CHECK_INTERVAL_MS = 30_000L
    
    /** Normal health check interval when foregrounded (5 minutes) */
    const val FOREGROUND_HEALTH_CHECK_INTERVAL_MS = 5 * 60_000L
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONNECTION QUALITY ADAPTIVE BEHAVIOR
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /** Heartbeat interval for excellent connection quality (60 seconds) */
    const val HEARTBEAT_EXCELLENT_MS = 60_000L
    
    /** Heartbeat interval for good connection quality (45 seconds) */
    const val HEARTBEAT_GOOD_MS = 45_000L
    
    /** Heartbeat interval for degraded connection quality (30 seconds) */
    const val HEARTBEAT_DEGRADED_MS = 30_000L
    
    /** Heartbeat interval for poor connection quality (15 seconds) */
    const val HEARTBEAT_POOR_MS = 15_000L
    
    /** Number of samples to keep for connection quality tracking */
    const val QUALITY_SAMPLE_SIZE = 20
    
    /** Latency threshold for excellent quality (ms) */
    const val LATENCY_EXCELLENT_THRESHOLD_MS = 500L
    
    /** Latency threshold for good quality (ms) */
    const val LATENCY_GOOD_THRESHOLD_MS = 2_000L
    
    /** Latency threshold for degraded quality (ms) */
    const val LATENCY_DEGRADED_THRESHOLD_MS = 5_000L
}
