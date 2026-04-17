package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import com.mocca.app.api.NetworkConfig

/**
 * Represents a server discovered through various methods.
 */
@Immutable
data class DiscoveredServer(
    val name: String,
    val host: String,
    val port: Int,
    val username: String = NetworkConfig.DEFAULT_USERNAME,
    val password: String = "",
    val source: DiscoverySource,
    val discoveredAt: Long = System.currentTimeMillis(),
    val useHttps: Boolean = true
) {
    /**
     * Generates the base URL for this server.
     * 
     * For Tailscale HTTPS connections (port 443), omits the port from URL.
     * For standard connections, includes the port.
     */
    val baseUrl: String
        get() {
            val protocol = if (useHttps) "https" else "http"
            return if (useHttps && port == 443) {
                "$protocol://$host"
            } else {
                "$protocol://$host:$port"
            }
        }
    
    /**
     * Check if this is a Tailscale server based on hostname.
     */
    val isTailscale: Boolean
        get() = host.contains(".tail") && host.contains(".ts.net")

    /** Whether a password is already available for direct connection. */
    val hasCredentials: Boolean
        get() = password.isNotBlank()

    /** Display-friendly connection type derived from the discovered host. */
    val displayType: String
        get() = when {
            host == NetworkConfig.DEFAULT_HOST_IP -> "Emulator"
            host == "localhost" || host == "127.0.0.1" -> "Local"
            host.endsWith(".ts.net") -> "Tailscale"
            host.matches(Regex("""\d+\.\d+\.\d+\.\d+""")) -> "LAN"
            else -> "Remote"
        }

    /** Human-readable protocol label for onboarding/status surfaces. */
    val protocolLabel: String
        get() = if (useHttps) "HTTPS" else "HTTP"

    /** Human-readable discovery source label for onboarding/status surfaces. */
    val sourceLabel: String
        get() = when (source) {
            DiscoverySource.MDNS -> "mDNS"
            DiscoverySource.MANUAL -> "Manual"
            DiscoverySource.SAVED -> "Saved"
            DiscoverySource.EMULATOR_AUTO -> "Emulator"
        }
     
    fun toServerConfig(id: String = name.lowercase().replace(" ", "-")): ServerConfig {
        return ServerConfig(
            id = id,
            name = name,
            host = host,
            port = port,
            username = username,
            password = password,
            isActive = true,
            useHttps = useHttps
        )
    }
}

/**
 * Source of server discovery.
 */
enum class DiscoverySource {
    /** Auto-discovered via mDNS/Bonjour */
    MDNS,
    /** User entered manually */
    MANUAL,
    /** From local database/cache */
    SAVED,
    /** Auto-detected emulator localhost */
    EMULATOR_AUTO
}

/**
 * Discovery preferences for the user.
 */
@Immutable
data class DiscoveryPreferences(
    val autoDiscoveryEnabled: Boolean = true,
    val preferSavedServers: Boolean = true,
    val lastSuccessfulServerId: String? = null,
    val discoveryTimeoutMs: Long = 5000,
    val maxDiscoveredServers: Int = 10
)
