package com.mocca.app.data.repository

import com.mocca.app.api.getPlatformDefaultHost
import com.mocca.app.data.local.LocalCache
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
 */
class ServerConfigRepository(
    private val localCache: LocalCache
) {
    private val _activeServer = MutableStateFlow<ServerConfig?>(null)
    val activeServer: StateFlow<ServerConfig?> = _activeServer.asStateFlow()

    init {
        loadActiveServer()
    }

    /**
     * Load the active server from cache.
     * Also handles migration from old emulator-only default to Tailscale default for physical devices.
     */
    private fun loadActiveServer() {
        runBlocking(Dispatchers.IO) {
            try {
                val config = localCache.getActiveServerConfig()
                
                // If no server configured, create default
                if (config == null) {
                    val default = createDefaultConfig()
                    localCache.insertServerConfig(default)
                    _activeServer.value = default
                    return@runBlocking
                }
                
                // Migration: If on physical device (empty default host) and using emulator IP,
                // prompt user to reconfigure by showing the Tailscale default
                val defaultHost = getPlatformDefaultHost()
                val isPhysicalDevice = defaultHost.isEmpty()
                val isUsingEmulatorIp = config.baseUrl.contains("10.0.2.2")
                
                if (isPhysicalDevice && isUsingEmulatorIp) {
                    Napier.i("Detected physical device with emulator IP config - creating Tailscale default")
                    val tailscaleDefault = createDefaultConfig()
                    localCache.insertServerConfig(tailscaleDefault)
                    localCache.setActiveServerConfig(tailscaleDefault.id)
                    _activeServer.value = tailscaleDefault
                } else {
                    _activeServer.value = config
                }
            } catch (e: Exception) {
                Napier.w("Failed to load active server", e)
                _activeServer.value = createDefaultConfig()
            }
        }
    }

    /**
     * Get all configured servers.
     */
    suspend fun getAllServers(): List<ServerConfig> {
        return try {
            localCache.getAllServerConfigs()
        } catch (e: Exception) {
            Napier.w("Failed to get servers", e)
            emptyList()
        }
    }

    /**
     * Add or update a server configuration.
     */
    suspend fun saveServer(config: ServerConfig) {
        try {
            localCache.insertServerConfig(config)
            
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
     */
    fun getActiveServerConfig(): ServerConfig {
        return _activeServer.value ?: createDefaultConfig()
    }

    /**
     * Create a default server configuration.
     * - Android emulator: Uses 10.0.2.2 to reach host machine's localhost.
     * - Physical devices: Creates a Tailscale config with placeholder URL that user must configure.
     */
    fun createDefaultConfig(): ServerConfig {
        val defaultHost = getPlatformDefaultHost()
        
        return if (defaultHost.isNotEmpty()) {
            ServerConfig(
                id = "default",
                name = "Local Server",
                baseUrl = "http://$defaultHost:4096",
                connectionType = ConnectionType.LOCAL,
                authType = AuthType.NONE,
                authToken = null,
                isActive = true
            )
        } else {
            ServerConfig(
                id = "tailscale-default",
                name = "Tailscale Server",
                baseUrl = "https://omen.tail0b932a.ts.net",
                connectionType = ConnectionType.TAILSCALE,
                authType = AuthType.NONE,
                authToken = null,
                isActive = true
            )
        }
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
