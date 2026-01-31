package com.mocca.app.data.repository

import com.mocca.app.api.NetworkConfig
import com.mocca.app.api.getPlatformDefaultHost
import com.mocca.app.data.local.LocalCache
import com.mocca.app.data.security.SecureTokenStorage
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Repository for managing server configurations.
 * 
 * SECURITY: This repository uses SecureTokenStorage to encrypt/decrypt authentication
 * tokens before storing them in the database. Tokens are encrypted using Android Keystore
 * with AES-256-GCM encryption, providing hardware-backed security when available.
 */
class ServerConfigRepository(
    private val localCache: LocalCache,
    private val secureTokenStorage: SecureTokenStorage? = null
) {
    private val _activeServer = MutableStateFlow<ServerConfig?>(null)
    val activeServer: StateFlow<ServerConfig?> = _activeServer.asStateFlow()

    init {
        loadActiveServer()
    }

    /**
     * Load the active server from cache.
     * Also handles migration from old emulator-only default to Tailscale default for physical devices.
     * 
     * SECURITY: If the stored auth token is encrypted, it will be decrypted using
     * SecureTokenStorage before being returned.
     */
    private fun loadActiveServer() {
        runBlocking(Dispatchers.IO) {
            try {
                val config = localCache.getActiveServerConfig()?.let { decryptConfigIfNeeded(it) }
                
                // If no server configured, create default (only for emulator)
                if (config == null) {
                    val default = createDefaultConfig()
                    if (default != null) {
                        localCache.insertServerConfig(default)
                        _activeServer.value = default
                    } else {
                        // Physical device without config - leave null to trigger onboarding
                        _activeServer.value = null
                    }
                    return@runBlocking
                }
                
                // Migration: If on physical device (empty default host) and using emulator IP,
                // clear the config to force reconfiguration via onboarding
                val defaultHost = getPlatformDefaultHost()
                val isPhysicalDevice = defaultHost.isEmpty()
                val isUsingEmulatorIp = config.baseUrl.contains("10.0.2.2")
                
                if (isPhysicalDevice && isUsingEmulatorIp) {
                    Napier.i("Detected physical device with emulator IP config - clearing to force onboarding")
                    localCache.deleteServerConfig(config.id)
                    _activeServer.value = null
                } else {
                    _activeServer.value = config
                }
            } catch (e: Exception) {
                Napier.w("Failed to load active server", e)
                // Only set default if we're on emulator (createDefaultConfig returns non-null)
                val default = createDefaultConfig()
                if (default != null) {
                    _activeServer.value = default
                }
            }
        }
    }
    
    /**
     * Decrypt server config auth token if it's encrypted.
     * 
     * SECURITY: Checks if the auth token was encrypted by this app (has specific prefix)
     * and decrypts it using SecureTokenStorage. If decryption fails or token is not
     * app-encrypted, returns original config with token intact.
     */
    private fun decryptConfigIfNeeded(config: ServerConfig): ServerConfig {
        if (secureTokenStorage == null || config.authToken.isNullOrEmpty()) {
            return config
        }
        
        return try {
            if (secureTokenStorage.isEncrypted(config.authToken)) {
                // Try to decrypt - if it fails, the token might be a raw Base64 token from QR code
                val decryptedToken = secureTokenStorage.decrypt(config.authToken)
                config.copy(authToken = decryptedToken)
            } else {
                // Token is not encrypted (raw token from QR/manual entry), return as-is
                config
            }
        } catch (e: Exception) {
            // Decryption failed - token is likely raw Base64 from QR code, not app-encrypted
            Napier.d("Token for server ${config.id} appears to be raw (not app-encrypted), using as-is")
            // Return config with original token intact for QR code tokens
            config
        }
    }

    /**
     * Get all configured servers.
     * 
     * SECURITY: Auth tokens are decrypted before being returned.
     */
    suspend fun getAllServers(): List<ServerConfig> {
        return try {
            localCache.getAllServerConfigs().map { decryptConfigIfNeeded(it) }
        } catch (e: Exception) {
            Napier.w("Failed to get servers", e)
            emptyList()
        }
    }

    /**
     * Add or update a server configuration.
     * 
     * SECURITY: If the config has an authToken, it will be encrypted using
     * SecureTokenStorage before being saved to the database.
     */
    suspend fun saveServer(config: ServerConfig) {
        try {
            // Encrypt auth token if present and secure storage is available
            val configToSave = if (secureTokenStorage != null && !config.authToken.isNullOrEmpty()) {
                try {
                    val encryptedToken = secureTokenStorage.encrypt(config.authToken)
                    config.copy(authToken = encryptedToken)
                } catch (e: Exception) {
                    Napier.e("Failed to encrypt auth token, saving plaintext", e)
                    config
                }
            } else {
                config
            }
            
            localCache.insertServerConfig(configToSave)
            
            if (config.isActive) {
                localCache.setActiveServerConfig(config.id)
                _activeServer.value = config
            }
        } catch (e: Exception) {
            Napier.e("Failed to save server", e)
        }
    }

    /**
     * Delete a server configuration.
     */
    suspend fun deleteServer(serverId: String) {
        try {
            localCache.deleteServerConfig(serverId)
            if (_activeServer.value?.id == serverId) {
                _activeServer.value = localCache.getActiveServerConfig()
            }
        } catch (e: Exception) {
            Napier.e("Failed to delete server", e)
        }
    }

    /**
     * Set a server as active.
     */
    suspend fun setActiveServer(serverId: String) {
        try {
            localCache.setActiveServerConfig(serverId)
            _activeServer.value = localCache.getServerConfig(serverId)
        } catch (e: Exception) {
            Napier.e("Failed to set active server", e)
        }
    }

    /**
     * Get the current active server configuration.
     * 
     * SECURITY: Returns the cached config with decrypted auth token (if applicable).
     */
    fun getActiveServerConfig(): ServerConfig {
        return _activeServer.value?.let { decryptConfigIfNeeded(it) } ?: createDefaultConfig()
    }

    /**
     * Create a default server configuration.
     * - Android emulator: Uses 10.0.2.2 to reach host machine's localhost.
     * - Physical devices: Returns empty config that triggers onboarding.
     */
    fun createDefaultConfig(): ServerConfig {
        val defaultHost = getPlatformDefaultHost()
        
        return if (defaultHost.isNotEmpty()) {
            // Android emulator - use 10.0.2.2
            ServerConfig(
                id = "default",
                name = "Local Server",
                baseUrl = "http://$defaultHost:${NetworkConfig.OPENCODE_SERVER_PORT}",
                connectionType = ConnectionType.LOCAL,
                authType = AuthType.NONE,
                authToken = null,
                isActive = true
            )
        } else {
            // Physical device - return empty config that requires onboarding
            // Empty baseUrl signals the UI to show onboarding instead of connecting
            ServerConfig(
                id = "needs-onboarding",
                name = "Setup Required",
                baseUrl = "",  // Empty signals onboarding needed
                connectionType = ConnectionType.LOCAL,
                authType = AuthType.NONE,
                authToken = null,
                isActive = true
            )
        }
    }
    
    /**
     * Create a Tailscale serve configuration.
     * This is used when the OpenCode server is exposed via `tailscale serve`.
     * 
     * Example setup on server:
     *   tailscale serve --port 443 / http://localhost:4096
     *   tailscale serve --port 443 /git http://localhost:4097
     * 
     * @param tailscaleHostname The Tailscale hostname (e.g., "mydevice.tail1234.ts.net")
     * @param useHttps Whether to use HTTPS (default true for Tailscale)
     */
    fun createTailscaleServeConfig(
        tailscaleHostname: String,
        useHttps: Boolean = true
    ): ServerConfig {
        val protocol = if (useHttps) "https" else "http"
        val cleanHostname = tailscaleHostname.removePrefix("https://").removePrefix("http://")
        
        return ServerConfig(
            id = "tailscale-serve-${cleanHostname.hashCode()}",
            name = "Tailscale ($cleanHostname)",
            baseUrl = "$protocol://$cleanHostname",
            connectionType = ConnectionType.TAILSCALE,
            authType = AuthType.NONE,
            authToken = null,
            isActive = false
        )
    }
    
    /**
     * Create a LAN connection configuration.
     * @param lanIp The LAN IP address (e.g., "192.168.1.100")
     * @param port The port number (default 4096)
     */
    fun createLanConfig(
        lanIp: String,
        port: Int = NetworkConfig.OPENCODE_SERVER_PORT
    ): ServerConfig {
        return ServerConfig(
            id = "lan-${lanIp.replace(".", "-")}-$port",
            name = "LAN ($lanIp)",
            baseUrl = "http://$lanIp:$port",
            connectionType = ConnectionType.LAN,
            authType = AuthType.NONE,
            authToken = null,
            isActive = false
        )
    }

    suspend fun checkServerHealth(baseUrl: String): Boolean {
        return try {
            val tempClient = HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
            }
            val response: AppInfo = tempClient.get("$baseUrl/global/health").body()
            tempClient.close()
            response.healthy
        } catch (e: Exception) {
            Napier.w("Health check failed for $baseUrl", e)
            false
        }
    }
}
