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
    val useHttps: Boolean = false
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
