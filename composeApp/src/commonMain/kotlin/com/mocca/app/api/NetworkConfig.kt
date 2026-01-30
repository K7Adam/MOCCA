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
    
    /** Git diff operation timeout (60 seconds) */
    const val GIT_DIFF_TIMEOUT_MS = 60_000L
    
    /** Git server polling interval when waiting for startup */
    const val GIT_SERVER_POLL_INTERVAL_MS = 500L
    
    /** Maximum wait time for Git server startup */
    const val GIT_SERVER_MAX_WAIT_MS = 10_000L
    
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
    
    /** Git HTTP server port */
    const val GIT_SERVER_PORT = 4097
    
    /** OpenCode server default port */
    const val OPENCODE_SERVER_PORT = 4096
    
    /** Android emulator host IP */
    const val EMULATOR_HOST_IP = "10.0.2.2"
    
    /** Default tailscale hostname pattern */
    const val TAILSCALE_HOST_PATTERN = "*.tail*.ts.net"
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TAILSCALE SERVE / FUNNEL SUPPORT
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Tailscale serve configuration paths.
     * These are the path mappings when using `tailscale serve` command.
     * Example: tailscale serve --port 4096 / http://localhost:4096
     */
    object TailscaleServe {
        /** Path for OpenCode API server */
        const val OPENCODE_PATH = "/"
        
        /** Path for Git HTTP server */
        const val GIT_PATH = "/git"
        
        /** Default serve port for Tailscale HTTPS */
        const val DEFAULT_HTTPS_PORT = 443
        
        /** Default serve port for Tailscale HTTP */
        const val DEFAULT_HTTP_PORT = 80
    }
    
    /**
     * Service endpoints for multi-port architecture.
     * When using Tailscale serve with different paths, these define the endpoint mapping.
     */
    object ServiceEndpoints {
        /**
         * Get the Git server endpoint URL based on the base URL.
         * 
         * Connection scenarios:
         * 1. Tailscale HTTPS (https://host.tail.ts.net) → Append /git path
         * 2. Tailscale with explicit port (https://host.tail.ts.net:4096) → Swap port to 4097
         * 3. Localhost/Emulator (http://10.0.2.2:4096) → Swap port to 4097
         * 4. LAN (http://192.168.x.x:4096) → Swap port to 4097
         * 5. Direct IP without port → Use HTTP with port 4097
         */
        fun getGitEndpoint(baseUrl: String): String {
            val trimmedUrl = baseUrl.trimEnd('/')
            
            // Check if this is a Tailscale URL using path-based routing (HTTPS without explicit port)
            if (isTailscaleUrl(trimmedUrl) && usesTailscalePaths(trimmedUrl)) {
                return "$trimmedUrl${TailscaleServe.GIT_PATH}"
            }
            
            // For all other cases, swap the port from 4096 to 4097
            // This handles:
            // - Explicit port URLs: http://host:4096 → http://host:4097
            // - Tailscale with port: https://host.tail.ts.net:4096 → https://host.tail.ts.net:4097
            // - Any URL with OpenCode port
            val portSwapped = trimmedUrl.replace(":$OPENCODE_SERVER_PORT", ":$GIT_SERVER_PORT")
            
            // If port wasn't in URL (e.g., http://192.168.1.5), append it
            if (portSwapped == trimmedUrl && !trimmedUrl.contains(":$GIT_SERVER_PORT")) {
                // Extract host and build HTTP URL with Git port
                val hostRegex = Regex("""https?://([^:/]+)""")
                val match = hostRegex.find(trimmedUrl)
                if (match != null) {
                    return "http://${match.groupValues[1]}:$GIT_SERVER_PORT"
                }
            }
            
            return portSwapped
        }
        
        /** Check if URL is a Tailscale hostname */
        fun isTailscaleUrl(url: String): Boolean {
            return url.contains(".tail") && url.contains(".ts.net")
        }
        
        /**
         * Check if URL should use Tailscale serve path-based routing.
         * 
         * Path-based routing is used when:
         * - URL is a Tailscale URL
         * - URL does NOT have an explicit port (meaning it's using default HTTPS 443)
         * 
         * This indicates the user is using `tailscale serve` with path routing:
         *   / → localhost:4096 (OpenCode)
         *   /git → localhost:4097 (Git Server)
         */
        fun usesTailscalePaths(url: String): Boolean {
            if (!isTailscaleUrl(url)) return false
            
            // Check if URL has explicit port (port swap mode) or no port (path mode)
            val hasExplicitPort = url.contains(":$OPENCODE_SERVER_PORT") || 
                                  url.contains(":$GIT_SERVER_PORT") ||
                                  Regex(""":(\d{4,5})""").containsMatchIn(url)
            
            return !hasExplicitPort
        }
        
        /**
         * Check if this is a local connection (emulator or localhost).
         */
        fun isLocalConnection(url: String): Boolean {
            return url.contains(EMULATOR_HOST_IP) || 
                   url.contains("127.0.0.1") || 
                   url.contains("localhost")
        }
    }
    
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
