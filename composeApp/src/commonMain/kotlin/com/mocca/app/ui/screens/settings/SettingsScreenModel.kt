package com.mocca.app.ui.screens.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.api.getHttpEngine
import com.mocca.app.data.repository.AppConnectionManager
import com.mocca.app.data.repository.AppConnectionState
import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.data.repository.SettingsRepository
import com.mocca.app.data.repository.UpdateRepository
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock as KtClock

enum class ServerConnectionStatus {
    UNKNOWN,
    CHECKING,
    CONNECTED,
    FAILED
}

data class SettingsState(
    val servers: List<ServerConfig> = emptyList(),
    val activeServerId: String? = null,
    val editingServer: ServerConfig? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val connectionStatuses: Map<String, ServerConnectionStatus> = emptyMap(),
    val activeConnectionState: AppConnectionState = AppConnectionState.NotConfigured,
    val githubToken: String = ""
) {
    /** Server version from SSE Connected event, or null if not connected */
    val serverVersion: String? get() = (activeConnectionState as? AppConnectionState.Connected)?.serverInfo?.version
}

class SettingsScreenModel(
    private val serverConfigRepository: ServerConfigRepository,
    private val appConnectionManager: AppConnectionManager,
    private val updateRepository: UpdateRepository,
    private val settingsRepository: SettingsRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        loadServers()
        loadGitHubToken()
        observeActiveServer()
        observeActiveConnectionState()
    }
    
    fun checkForUpdates() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, message = "Checking for updates...")
            
            updateRepository.checkForUpdate().fold(
                onSuccess = { updateInfo ->
                    if (updateInfo != null) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            message = "Update available: ${updateInfo.version}"
                        )
                        // Note: MainScreen should observe updateInfo globally or we trigger it via repository
                        // For now, this just notifies the user in settings.
                        // Ideally, we'd trigger the global update dialog.
                        // We can't easily trigger MainScreen dialog from here without shared state.
                        // Let's assume the user sees the message and goes back to main screen where checking happens automatically on start?
                        // No, let's make it better. The repository check itself doesn't trigger UI.
                        // We need a way to tell the app an update is ready.
                        // But for manual check, showing a toast/message is fine for now.
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            message = "No updates available"
                        )
                    }
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = "Update check failed: ${e.message}"
                    )
                }
            )
        }
    }

    private fun loadGitHubToken() {
        screenModelScope.launch {
            val token = settingsRepository.getGitHubToken()
            _state.value = _state.value.copy(githubToken = token ?: "")
        }
    }

    fun saveGitHubToken(token: String) {
        screenModelScope.launch {
            settingsRepository.saveGitHubToken(token)
            _state.value = _state.value.copy(
                githubToken = token,
                message = "GitHub token saved"
            )
        }
    }

    private fun observeActiveConnectionState() {
        screenModelScope.launch {
            appConnectionManager.connectionState.collect { state ->
                _state.value = _state.value.copy(activeConnectionState = state)
                
                val activeId = _state.value.activeServerId
                if (activeId != null) {
                    val status = when (state) {
                        is AppConnectionState.NotConfigured -> ServerConnectionStatus.UNKNOWN
                        is AppConnectionState.Checking -> ServerConnectionStatus.CHECKING
                        is AppConnectionState.WaitingForNetwork -> ServerConnectionStatus.CHECKING
                        is AppConnectionState.Connecting -> ServerConnectionStatus.CHECKING
                        is AppConnectionState.Reconnecting -> ServerConnectionStatus.CHECKING
                        is AppConnectionState.Connected -> ServerConnectionStatus.CONNECTED
                        is AppConnectionState.Disconnected -> ServerConnectionStatus.FAILED
                    }
                    _state.value = _state.value.copy(
                        connectionStatuses = _state.value.connectionStatuses + (activeId to status)
                    )
                }
            }
        }
    }
    
    private fun loadServers() {
        screenModelScope.launch {
            val servers = serverConfigRepository.getAllServers()
            _state.value = _state.value.copy(servers = servers)
            
            servers.forEach { server ->
                if (_state.value.connectionStatuses[server.id] == null) {
                    checkServerConnection(server)
                }
            }
        }
    }
    
    private fun observeActiveServer() {
        screenModelScope.launch {
            serverConfigRepository.activeServer.collect { server ->
                _state.value = _state.value.copy(activeServerId = server?.id)
            }
        }
    }
    
    fun addNewServer() {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val newServer = ServerConfig(
            id = now.toString(),
            name = "Tailscale Server",
            baseUrl = "https://omen.tail0b932a.ts.net",
            connectionType = ConnectionType.TAILSCALE,
            isActive = false
        )
        _state.value = _state.value.copy(editingServer = newServer)
    }
    
    fun editServer(server: ServerConfig) {
        _state.value = _state.value.copy(editingServer = server)
    }
    
    fun cancelEdit() {
        _state.value = _state.value.copy(editingServer = null)
    }
    
    fun saveServer(server: ServerConfig, isNewServer: Boolean = false) {
        screenModelScope.launch {
            serverConfigRepository.saveServer(server)
            
            if (isNewServer) {
                serverConfigRepository.setActiveServer(server.id)
            }
            
            loadServers()
            _state.value = _state.value.copy(
                editingServer = null,
                message = if (isNewServer) "Server added and activated" else "Server saved"
            )
            checkServerConnection(server)
        }
    }
    
    fun checkServerConnection(server: ServerConfig) {
        if (server.id == _state.value.activeServerId) {
            appConnectionManager.checkConnection()
        } else {
            checkNonActiveServer(server)
        }
    }
    
    private fun checkNonActiveServer(server: ServerConfig) {
        screenModelScope.launch {
            _state.value = _state.value.copy(
                connectionStatuses = _state.value.connectionStatuses + (server.id to ServerConnectionStatus.CHECKING)
            )
            
            val status = try {
                val testClient = HttpClient(getHttpEngine()) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true; isLenient = true })
                    }
                    install(HttpTimeout) {
                        requestTimeoutMillis = 5_000
                        connectTimeoutMillis = 3_000
                    }
                }
                
                val response = testClient.get("${server.baseUrl.trimEnd('/')}/global/health")
                testClient.close()
                
                if (response.status.value in 200..299) {
                    Napier.i("Server ${server.name} connection successful")
                    ServerConnectionStatus.CONNECTED
                } else {
                    Napier.w("Server ${server.name} returned status ${response.status}")
                    ServerConnectionStatus.FAILED
                }
            } catch (e: Exception) {
                Napier.w("Server ${server.name} connection failed: ${e.message}")
                ServerConnectionStatus.FAILED
            }
            
            _state.value = _state.value.copy(
                connectionStatuses = _state.value.connectionStatuses + (server.id to status)
            )
        }
    }
    
    fun checkAllServers() {
        screenModelScope.launch {
            _state.value.servers.forEach { server ->
                checkServerConnection(server)
            }
        }
    }
    
    fun deleteServer(serverId: String) {
        screenModelScope.launch {
            serverConfigRepository.deleteServer(serverId)
            loadServers()
            _state.value = _state.value.copy(message = "Server deleted")
        }
    }
    
    fun setActiveServer(serverId: String) {
        screenModelScope.launch {
            serverConfigRepository.setActiveServer(serverId)
            loadServers()
            _state.value = _state.value.copy(message = "Server activated")
            
            val server = _state.value.servers.find { it.id == serverId }
            if (server != null) {
                checkServerConnection(server)
            }
        }
    }
    
    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
