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
    
    /** SharedFlow buffer capacity for SSE events */
    const val SSE_BUFFER_CAPACITY = 256
    
    /** Maximum SSE reconnection attempts */
    const val SSE_MAX_RECONNECT_ATTEMPTS = 10
    
    /** Base delay for exponential backoff (1 second) */
    const val SSE_RECONNECT_BASE_DELAY_MS = 1_000L
    
    /** Maximum delay for exponential backoff (30 seconds) */
    const val SSE_RECONNECT_MAX_DELAY_MS = 30_000L
    
    /** Jitter range for reconnection delay (0-500ms) */
    const val SSE_RECONNECT_JITTER_MS = 500L
    
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
    const val OPENCODE_SERVER_PORT = 4096
    
    /** Android emulator host IP */
    const val EMULATOR_HOST_IP = "10.0.2.2"
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CIRCUIT BREAKER
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /** Failure threshold before opening circuit */
    const val CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5
    
    /** Timeout duration for circuit breaker (30 seconds) */
    const val CIRCUIT_BREAKER_TIMEOUT_MS = 30_000L
    
    /** Recovery timeout for half-open state (5 seconds) */
    const val CIRCUIT_BREAKER_RECOVERY_MS = 5_000L
}
