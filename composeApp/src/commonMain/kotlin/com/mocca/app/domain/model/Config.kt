package com.mocca.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Server configuration for connecting to OpenCode server.
 */
@Serializable
data class ServerConfig(
    val id: String,
    val name: String,
    val baseUrl: String = "http://localhost:4096",
    val connectionType: ConnectionType = ConnectionType.LOCAL,
    val authType: AuthType = AuthType.NONE,
    val authToken: String? = null,
    val isActive: Boolean = false
)

@Serializable
enum class ConnectionType {
    LOCAL,       // Local machine (localhost)
    LAN,         // Local network IP
    TAILSCALE,   // Via Tailscale VPN
    CLOUDFLARE,  // Via Cloudflare Tunnel
    CUSTOM       // Custom reverse proxy
}

@Serializable
enum class AuthType {
    NONE,
    BASIC,
    BEARER,
    OAUTH
}

/**
 * Wrapper for API responses with cached/fresh status.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Loading<T>(val data: T? = null) : Resource<T>()
    data class Error<T>(val message: String, val data: T? = null) : Resource<T>()
    
    fun dataOrNull(): T? = when (this) {
        is Success -> data
        is Loading -> data
        is Error -> data
    }
    
    fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Success(transform(data))
        is Loading -> Loading(data?.let(transform))
        is Error -> Error(message, data?.let(transform))
    }
}

/**
 * Connection status to OpenCode server.
 */
sealed class ConnectionStatus {
    data object Disconnected : ConnectionStatus()
    data object Connecting : ConnectionStatus()
    data object WaitingForNetwork : ConnectionStatus()
    data class Reconnecting(val attempt: Int, val delayMs: Long) : ConnectionStatus()
    data class Connected(val serverInfo: AppInfo) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
    
    val isConnected: Boolean get() = this is Connected
    val isConnecting: Boolean get() = this is Connecting || this is Reconnecting
    val isError: Boolean get() = this is Error
}
