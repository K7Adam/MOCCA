package com.mocca.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Server configuration for connecting to OpenCode server.
 * Uses HTTP Basic Auth (username + password) as defined by OpenCode's server API.
 *
 * OpenCode authenticates via environment variables:
 * - OPENCODE_SERVER_PASSWORD — protects the server with basic auth
 * - OPENCODE_SERVER_USERNAME — defaults to "opencode"
 */
@Serializable
data class ServerConfig(
    val id: String,
    val name: String,
    val host: String = "localhost",
    val port: Int = 4096,
    val username: String = "opencode",
    val password: String = "",
    val isActive: Boolean = false
) {
    /** Base URL for API requests. */
    val baseUrl: String get() = "http://$host:$port"

    /** Whether authentication credentials are configured. */
    val hasCredentials: Boolean get() = password.isNotBlank()

    /** Display-friendly connection type derived from host. */
    val displayType: String get() = when {
        host == "10.0.2.2" -> "Emulator"
        host == "localhost" || host == "127.0.0.1" -> "Local"
        host.endsWith(".ts.net") -> "Tailscale"
        host.matches(Regex("""\d+\.\d+\.\d+\.\d+""")) -> "LAN"
        else -> "Remote"
    }

    override fun toString(): String =
        "ServerConfig(id=$id, name=$name, host=$host, port=$port, username=$username, password=***)"
}

/**
 * Wrapper for API responses with cached/fresh status.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Loading<T>(val data: T? = null) : Resource<T>()
    data class Error<T>(val message: String, val data: T? = null, val cause: Throwable? = null) : Resource<T>()
    
    fun dataOrNull(): T? = when (this) {
        is Success -> data
        is Loading -> data
        is Error -> data
    }
    
    fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Success(transform(data))
        is Loading -> Loading(data?.let(transform))
        is Error -> Error(message, data?.let(transform), cause)
    }
}

/**
 * Connection status to OpenCode server.
 * Single source of truth for connection state — replaces both the old
 * AppConnectionState and ConnectionStatus dual-type system.
 */
sealed class ConnectionStatus {
    /** No server configured yet. */
    data object NotConfigured : ConnectionStatus()
    /** Disconnected from server (may have an error reason). */
    data class Disconnected(val reason: String? = null) : ConnectionStatus()
    /** Currently attempting initial connection. */
    data object Connecting : ConnectionStatus()
    /** Waiting for device network to become available. */
    data object WaitingForNetwork : ConnectionStatus()
    /** Reconnecting after a connection loss. */
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ConnectionStatus()
    /** Successfully connected to server. */
    data class Connected(val serverInfo: AppInfo, val latencyMs: Long = 0) : ConnectionStatus()
    /** Connection failed with an error. */
    data class Error(val message: String) : ConnectionStatus()

    val isConnected: Boolean get() = this is Connected
    val isConnecting: Boolean get() = this is Connecting || this is Reconnecting
    val isError: Boolean get() = this is Error
    val canRetry: Boolean get() = this is Disconnected || this is NotConfigured || this is Error || this is WaitingForNetwork
}

/**
 * Network connection quality assessment.
 * Used by UI components to display appropriate connectivity indicators.
 */
enum class ConnectionQuality {
    EXCELLENT, GOOD, POOR, OFFLINE, UNKNOWN
}
