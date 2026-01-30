package com.mocca.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Represents a server discovered through various methods.
 */
data class DiscoveredServer(
    val name: String,
    val host: String,
    val port: Int,
    val authToken: String? = null,
    val source: DiscoverySource,
    val discoveredAt: Long = System.currentTimeMillis()
) {
    val baseUrl: String
        get() = "http://$host:$port"
    
    fun toServerConfig(id: String = name.lowercase().replace(" ", "-")): ServerConfig {
        return ServerConfig(
            id = id,
            name = name,
            baseUrl = baseUrl,
            authType = if (authToken != null) AuthType.BEARER else AuthType.NONE,
            authToken = authToken,
            isActive = true
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
data class QrConnectionPayload(
    val host: String,
    val port: Int = 4096,
    val token: String? = null,
    val version: String = "1.0",
    val name: String = "OpenCode Server"
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
        
        fun fromUrl(url: String): QrConnectionPayload? {
            return try {
                val regex = Regex("""http://([^:/]+):(\d+)""")
                val match = regex.find(url)
                if (match != null) {
                    QrConnectionPayload(
                        host = match.groupValues[1],
                        port = match.groupValues[2].toInt()
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun toDiscoveredServer(): DiscoveredServer {
        return DiscoveredServer(
            name = name,
            host = host,
            port = port,
            authToken = token,
            source = DiscoverySource.QR_CODE
        )
    }
}

/**
 * Discovery preferences for the user.
 */
data class DiscoveryPreferences(
    val autoDiscoveryEnabled: Boolean = true,
    val preferSavedServers: Boolean = true,
    val lastSuccessfulServerId: String? = null,
    val discoveryTimeoutMs: Long = 5000,
    val maxDiscoveredServers: Int = 10
)
