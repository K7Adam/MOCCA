package com.mocca.app.domain.model

import com.mocca.app.api.NetworkConfig

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Server configuration for connecting to OpenCode server. Uses HTTP Basic Auth (username +
 * password) as defined by OpenCode's server API.
 *
 * OpenCode authenticates via environment variables:
 * - OPENCODE_SERVER_PASSWORD — protects the server with basic auth
 * - OPENCODE_SERVER_USERNAME — defaults to "opencode" (our server uses "adamk7")
 */
@Serializable
@Immutable
data class ServerConfig(
        val id: String,
        val name: String,
        val host: String = "localhost",
        val port: Int = 4242,
        val username: String = NetworkConfig.DEFAULT_USERNAME,
        val password: String = "",
        val isActive: Boolean = false,
        val useHttps: Boolean = false
) {
    /** Base URL for API requests. Supports HTTPS for Tailscale connections. */
    val baseUrl: String
        get() {
            val protocol = if (useHttps) "https" else "http"
            // For HTTPS on default port 443, omit the port
            return if (useHttps && port == 443) {
                "$protocol://$host"
            } else {
                "$protocol://$host:$port"
            }
        }

    /** Whether authentication credentials are configured. */
    val hasCredentials: Boolean
        get() = password.isNotBlank()

    /** Display-friendly connection type derived from host. */
    val displayType: String
        get() =
                when {
                    host == NetworkConfig.DEFAULT_HOST_IP -> "Emulator"
                    host == "localhost" || host == "127.0.0.1" -> "Local"
                    host.endsWith(".ts.net") -> "Tailscale"
                    host.matches(Regex("""\d+\.\d+\.\d+\.\d+""")) -> "LAN"
                    else -> "Remote"
                }

    override fun toString(): String =
            "ServerConfig(id=$id, name=$name, host=$host, port=$port, username=$username, password=***)"
}

/** Wrapper for API responses with cached/fresh status. */
sealed class Resource<out T> {
    @Immutable data class Success<T>(val data: T) : Resource<T>()
    @Immutable data class Loading<T>(val data: T? = null) : Resource<T>()
    @Immutable
    data class Error<T>(val message: String, val data: T? = null, val cause: Throwable? = null) :
            Resource<T>()

    fun dataOrNull(): T? =
            when (this) {
                is Success -> data
                is Loading -> data
                is Error -> data
            }

    fun <R> map(transform: (T) -> R): Resource<R> =
            when (this) {
                is Success -> Success(transform(data))
                is Loading -> Loading(data?.let(transform))
                is Error -> Error(message, data?.let(transform), cause)
            }
}

/**
 * Connection status to OpenCode server. Single source of truth for connection state — replaces both
 * the old AppConnectionState and ConnectionStatus dual-type system.
 */
sealed class ConnectionStatus {
    /** No server configured yet. */
    data object NotConfigured : ConnectionStatus()
    /** Disconnected from server (may have an error reason). */
    @Immutable data class Disconnected(val reason: String? = null) : ConnectionStatus()
    /** Currently attempting initial connection. */
    data object Connecting : ConnectionStatus()
    /** Waiting for device network to become available. */
    data object WaitingForNetwork : ConnectionStatus()
    /** Reconnecting after a connection loss. */
    @Immutable data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ConnectionStatus()
    /** Successfully connected to server. */
    @Immutable
    data class Connected(val serverInfo: AppInfo, val latencyMs: Long = 0) : ConnectionStatus()
    /** Connection failed with an error. */
    @Immutable data class Error(val message: String) : ConnectionStatus()

    val isConnected: Boolean
        get() = this is Connected
    val isConnecting: Boolean
        get() = this is Connecting || this is Reconnecting
    val isError: Boolean
        get() = this is Error
    val canRetry: Boolean
        get() =
                this is Disconnected ||
                        this is NotConfigured ||
                        this is Error ||
                        this is WaitingForNetwork
}

/**
 * Network connection quality assessment. Used by UI components to display appropriate connectivity
 * indicators.
 */
enum class ConnectionQuality {
    EXCELLENT,
    GOOD,
    DEGRADED,
    POOR,
    OFFLINE,
    UNKNOWN
}

/**
 * Global application configuration (GET/PATCH /global/config).
 * Distinct from per-instance config at GET/PATCH /config.
 */
@Serializable
@Immutable
data class GlobalAppConfig(
    val autoshare: Boolean? = null,
    val autoupdate: Boolean? = null,
    val telemetry: Boolean? = null,
    val theme: String? = null,
    val experimental: Map<String, Boolean>? = null
)

/**
 * Partial update for global config (PATCH /global/config).
 */
@Serializable
@Immutable
data class AppConfigUpdate(
    val autoshare: Boolean? = null,
    val autoupdate: Boolean? = null,
    val telemetry: Boolean? = null,
    val theme: String? = null
)

/**
 * Feature flags parsed from server config or env vars.
 * Used to gate experimental features in the UI.
 */
@Immutable
data class FeatureFlags(
    val experimentalWorktrees: Boolean = false,
    val experimentalPlanMode: Boolean = false,
    val experimentalExa: Boolean = false,
    val experimentalLsp: Boolean = false,
    val disableTerminals: Boolean = false,
    val disableFileBrowser: Boolean = false
)
