package com.mocca.app.data.repository

import com.mocca.app.api.NetworkConfig
import com.mocca.app.api.getPlatformDefaultHost
import com.mocca.app.api.getHttpEngine
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
    private data class PasswordReadResult(
        val password: String,
        val shouldMigrate: Boolean
    )

    private val _activeServer = MutableStateFlow<ServerConfig?>(null)
    val activeServer: StateFlow<ServerConfig?> = _activeServer.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        loadActiveServer()
    }

    /**
     * Load the active server from cache.
     * A missing server is a valid first-run state: QR pairing or manual onboarding must create one.
     * 
     * SECURITY: If the stored auth token is encrypted, it will be decrypted using
     * SecureTokenStorage before being returned.
     */
    private fun loadActiveServer() {
        runBlocking(Dispatchers.IO) {
            try {
                val rawConfig = localCache.getActiveServerConfig()
                
                // No implicit default: stale host presets can race QR pairing and flood health checks.
                if (rawConfig == null) {
                    _activeServer.value = null
                    return@runBlocking
                }
                
                val passwordRead = readPasswordCompatibility(rawConfig.password, rawConfig.id)
                val config = rawConfig.copy(password = passwordRead.password)

                if (passwordRead.shouldMigrate) {
                    migratePasswordToNewFormat(config)
                }
                
                // Migration: If on physical device (empty default host) and using emulator IP,
                // clear the config to force reconfiguration via onboarding
                val defaultHost = getPlatformDefaultHost()
                val isPhysicalDevice = defaultHost.isEmpty()
                val isUsingEmulatorIp = config.host == NetworkConfig.DEFAULT_HOST_IP || config.host == "10.0.2.2"
                
                if (isPhysicalDevice && isUsingEmulatorIp) {
                    Napier.i("Detected physical device with emulator IP config - clearing to force onboarding")
                    localCache.deleteServerConfig(config.id)
                    _activeServer.value = null
                } else {
                    _activeServer.value = config
                }
            } catch (e: Exception) {
                Napier.w("Failed to load active server", e)
                _activeServer.value = null
            } finally {
                _isLoaded.value = true
            }
        }
    }
    
    /**
     * Compatibility read path for stored passwords.
     *
     * Supports three states:
     * 1. ENC: prefix → decrypt and fail soft to empty password on true failure
     * 2. Legacy encrypted without prefix → one controlled decrypt probe, then mark for migration
     * 3. Plaintext legacy value → return as-is and mark for migration to ENC:
     */
    private fun readPasswordCompatibility(storedPassword: String, serverId: String): PasswordReadResult {
        if (secureTokenStorage == null || storedPassword.isEmpty()) {
            return PasswordReadResult(
                password = storedPassword,
                shouldMigrate = false
            )
        }

        if (secureTokenStorage.isEncrypted(storedPassword)) {
            return try {
                PasswordReadResult(
                    password = secureTokenStorage.decrypt(storedPassword),
                    shouldMigrate = false
                )
            } catch (e: Exception) {
                Napier.w("Failed to decrypt ENC: password for server $serverId, clearing", e)
                PasswordReadResult(
                    password = "",
                    shouldMigrate = false
                )
            }
        }

        return try {
            val decryptedPassword = secureTokenStorage.decrypt(storedPassword)
            Napier.i("Detected legacy encrypted password for server $serverId; scheduling ENC: migration")
            PasswordReadResult(
                password = decryptedPassword,
                shouldMigrate = true
            )
        } catch (_: Exception) {
            PasswordReadResult(
                password = storedPassword,
                shouldMigrate = true
            )
        }
    }
    
    /**
     * Re-encrypt a decrypted password using the new ENC: prefix scheme.
     * Called when a config is read from DB without the ENC: prefix — covers both
     * plaintext values and legacy encrypted values that were already decrypted by
     * decryptConfigIfNeeded().
     * Silently logs failures — the password still works in memory.
     */
    private suspend fun migratePasswordToNewFormat(config: ServerConfig) {
        if (secureTokenStorage == null || config.password.isEmpty()) return
        
        try {
            val encryptedPassword = secureTokenStorage.encrypt(config.password)
            localCache.insertServerConfig(config.copy(password = encryptedPassword))
            Napier.i("Migrated password to ENC: format for server ${config.id}")
        } catch (e: Exception) {
            Napier.w("Failed to migrate password for server ${config.id}", e)
        }
    }

    /**
     * Get all configured servers.
     * Decrypts encrypted passwords and migrates plaintext ones.
     */
    suspend fun getAllServers(): List<ServerConfig> {
        return try {
            localCache.getAllServerConfigs().map { raw ->
                val passwordRead = readPasswordCompatibility(raw.password, raw.id)
                val config = raw.copy(password = passwordRead.password)
                if (passwordRead.shouldMigrate) {
                    migratePasswordToNewFormat(config)
                }
                config
            }
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
                val raw = localCache.getActiveServerConfig()
                _activeServer.value = raw?.let {
                    val passwordRead = readPasswordCompatibility(it.password, it.id)
                    val config = it.copy(password = passwordRead.password)
                    if (passwordRead.shouldMigrate) {
                        migratePasswordToNewFormat(config)
                    }
                    config
                }
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
            val raw = localCache.getServerConfig(serverId)
            if (raw != null) {
                val passwordRead = readPasswordCompatibility(raw.password, raw.id)
                val config = raw.copy(password = passwordRead.password)
                if (passwordRead.shouldMigrate) {
                    migratePasswordToNewFormat(config)
                }
                _activeServer.value = config
            }
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
        return _activeServer.value
    }

    /**
     * Create a default server configuration.
     *
     * MOCCA no longer creates an implicit OpenCode server target. QR pairing with
     * the MOCCA CLI is the canonical zero-config path, and direct server profiles
     * should be created explicitly through onboarding/settings.
     */
    fun createDefaultConfig(): ServerConfig? {
        return null
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
            isActive = false,
            useHttps = true
        )
    }
    
    /**
     * Create a LAN connection configuration.
     * @param lanIp The LAN IP address (e.g., "192.168.1.100")
     * @param port The port number (default 443)
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
            isActive = false,
            useHttps = true
        )
    }

    suspend fun checkServerHealth(baseUrl: String): Boolean {
        return try {
            val tempClient = HttpClient(getHttpEngine()) {
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
