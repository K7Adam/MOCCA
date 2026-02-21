package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import com.mocca.app.api.NetworkConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Represents a server discovered through various methods.
 */
@Immutable
data class DiscoveredServer(
    val name: String,
    val host: String,
    val port: Int,
    val username: String = "opencode",
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
    /** Scanned from QR code */
    QR_CODE,
    /** User entered manually */
    MANUAL,
    /** From local database/cache */
    SAVED,
    /** Auto-detected emulator localhost */
    EMULATOR_AUTO
}

/**
 * QR code payload for instant server connection.
 * This is what the OpenCode server displays as a QR code.
 */
@Serializable
@Immutable
data class QrConnectionPayload(
    val host: String,
    val port: Int = NetworkConfig.OPENCODE_SERVER_PORT,
    val username: String = "opencode",
    val password: String = "",
    val version: String = "1.0",
    val name: String = "OpenCode Server",
    val useHttps: Boolean = false
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        
        fun fromJson(jsonString: String): QrConnectionPayload? {
            return try {
                json.decodeFromString(serializer(), jsonString)
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Parse a URL into a QrConnectionPayload.
         * 
         * Supports:
         * - http://host:port (standard)
         * - https://host:port (HTTPS with explicit port)
         * - https://host.tail.ts.net (Tailscale HTTPS, port defaults to 443)
         * - http://10.0.2.2:4096 (Emulator)
         * - http://192.168.x.x:4096 (LAN)
         */
        fun fromUrl(url: String): QrConnectionPayload? {
            return try {
                // Regex to match: protocol://host[:port]
                // Groups: (1) protocol, (2) host, (3) optional port
                val regex = Regex("""(https?)://([^:/]+)(?::(\d+))?""")
                val match = regex.find(url)
                
                if (match != null) {
                    val protocol = match.groupValues[1]
                    val host = match.groupValues[2]
                    val portString = match.groupValues[3]
                    val isHttps = protocol == "https"
                    
                    // Determine port: explicit port > default based on protocol
                    val port = when {
                        portString.isNotEmpty() -> portString.toInt()
                        isHttps -> 443
                        else -> NetworkConfig.OPENCODE_SERVER_PORT
                    }
                    
                    QrConnectionPayload(
                        host = host,
                        port = port,
                        useHttps = isHttps
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Convert this QR payload to a DiscoveredServer.
     * Properly propagates HTTPS setting for Tailscale connections.
     */
    fun toDiscoveredServer(): DiscoveredServer {
        return DiscoveredServer(
            name = name,
            host = host,
            port = port,
            username = username,
            password = password,
            source = DiscoverySource.QR_CODE,
            useHttps = useHttps
        )
    }
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
