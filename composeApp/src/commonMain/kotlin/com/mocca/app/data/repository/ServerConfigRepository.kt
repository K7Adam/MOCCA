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
                
                // If no server configured, create default only for emulator (physical devices stay null)
                if (config == null) {
                    val default = createDefaultConfig()
                    if (default != null) {
                        localCache.insertServerConfig(default)
                        _activeServer.value = default
                    } else {
                        // Physical device — no default config, onboarding required
                        _activeServer.value = null
                    }
                    return@runBlocking
                }
                
                // Migration: If on physical device (empty default host) and using emulator IP,
                // clear the config to force reconfiguration via onboarding
                val defaultHost = getPlatformDefaultHost()
                val isPhysicalDevice = defaultHost.isEmpty()
                val isUsingEmulatorIp = config.host == "10.0.2.2"
                
                if (isPhysicalDevice && isUsingEmulatorIp) {
                    Napier.i("Detected physical device with emulator IP config - clearing to force onboarding")
                    localCache.deleteServerConfig(config.id)
                    _activeServer.value = null
                } else {
                    _activeServer.value = config
                }
            } catch (e: Exception) {
                Napier.w("Failed to load active server", e)
                // Set default config (null for physical devices)
                val default = createDefaultConfig()
                _activeServer.value = default
            }
        }
    }
    
    /**
     * Decrypt server config password if it's encrypted.
     * 
     * SECURITY: Checks if the password was encrypted by this app (has specific prefix)
     * and decrypts it using SecureTokenStorage. If decryption fails or password is not
     * app-encrypted, returns original config with password intact.
     */
    private fun decryptConfigIfNeeded(config: ServerConfig): ServerConfig {
        if (secureTokenStorage == null || config.password.isEmpty()) {
            return config
        }
        
        return try {
            if (secureTokenStorage.isEncrypted(config.password)) {
                // Try to decrypt - if it fails, the password might be plaintext
                val decryptedPassword = secureTokenStorage.decrypt(config.password)
                config.copy(password = decryptedPassword)
            } else {
                // Password is not encrypted (plaintext from manual entry), return as-is
                config
            }
        } catch (e: Exception) {
            // Decryption failed - password is likely plaintext, not app-encrypted
            Napier.d("Password for server ${config.id} appears to be plaintext (not app-encrypted), using as-is")
            // Return config with original password intact
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
     * SECURITY: If the config has a password, it will be encrypted using
     * SecureTokenStorage before being saved to the database.
     */
    suspend fun saveServer(config: ServerConfig) {
        try {
            // Encrypt password if present and secure storage is available
            val configToSave = if (secureTokenStorage != null && config.password.isNotEmpty()) {
                try {
                    val encryptedPassword = secureTokenStorage.encrypt(config.password)
                    config.copy(password = encryptedPassword)
                } catch (e: Exception) {
                    Napier.e("Failed to encrypt password, saving plaintext", e)
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
                _activeServer.value = localCache.getActiveServerConfig()?.let { decryptConfigIfNeeded(it) }
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
            _activeServer.value = localCache.getServerConfig(serverId)?.let { decryptConfigIfNeeded(it) }
        } catch (e: Exception) {
            Napier.e("Failed to set active server", e)
        }
    }

    /**
     * Get the current active server configuration.
     * Returns null if no server is configured (physical device without onboarding).
     * 
     * SECURITY: Returns the cached config with decrypted auth token (if applicable).
     */
    fun getActiveServerConfig(): ServerConfig? {
        return _activeServer.value?.let { decryptConfigIfNeeded(it) }
    }

    /**
     * Create a default server configuration.
     * - Android emulator: Uses 10.0.2.2 to reach host machine's localhost.
     * - Physical devices: Returns null — onboarding must be completed first.
     */
    fun createDefaultConfig(): ServerConfig? {
        val defaultHost = getPlatformDefaultHost()
        
        return if (defaultHost.isNotEmpty()) {
            // Android emulator - use 10.0.2.2
            ServerConfig(
                id = "default",
                name = "Local Server",
                host = defaultHost,
                port = NetworkConfig.OPENCODE_SERVER_PORT,
                isActive = true
            )
        } else {
            // Physical device - return null, requires onboarding
            null
        }
    }
    
    /**
     * Create a Tailscale serve configuration.
     * This is used when the OpenCode server is exposed via `tailscale serve`.
     * 
     * @param tailscaleHostname The Tailscale hostname (e.g., "mydevice.tail1234.ts.net")
     */
    fun createTailscaleServeConfig(
        tailscaleHostname: String
    ): ServerConfig {
        val cleanHostname = tailscaleHostname.removePrefix("https://").removePrefix("http://")
        
        return ServerConfig(
            id = "tailscale-serve-${cleanHostname.hashCode()}",
            name = "Tailscale ($cleanHostname)",
            host = cleanHostname,
            port = 443,
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
            host = lanIp,
            port = port,
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
