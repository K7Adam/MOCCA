package com.mocca.app.ui.screens.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.api.getHttpEngine
import com.mocca.app.data.repository.ConnectionManager
import com.mocca.app.data.repository.ConfigRepository
import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.data.repository.SettingsRepository
import com.mocca.app.data.repository.UpdateNotifier
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
    val activeConnectionState: ConnectionStatus = ConnectionStatus.NotConfigured,
    val githubToken: String = "",
    val githubTokenStatus: GitHubTokenStatus? = null,
    val isValidatingToken: Boolean = false,
    // Provider Auth
    val providers: List<Provider> = emptyList(),
    val providerAuthMethods: Map<String, List<ProviderAuthMethod>> = emptyMap(),
    val selectedProviderId: String? = null,
    val authLoading: Boolean = false,
    // Server Config (fetched from /config endpoint)
    val serverConfig: ConfigResponse? = null,
    val serverDefaultProvider: String? = null,
    val serverDefaultModel: String? = null,
    val serverModes: List<Mode> = emptyList()
) {
    /** Server version from SSE Connected event, or null if not connected */
    val serverVersion: String? get() = (activeConnectionState as? ConnectionStatus.Connected)?.serverInfo?.version
}

class SettingsScreenModel(
    private val serverConfigRepository: ServerConfigRepository,
    private val connectionManager: ConnectionManager,
    private val updateRepository: UpdateRepository,
    private val settingsRepository: SettingsRepository,
    private val configRepository: ConfigRepository,
    private val updateNotifier: UpdateNotifier
) : ScreenModel {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        loadServers()
        loadGitHubToken()
        observeActiveServer()
        observeActiveConnectionState()
        // Load remote config if connected
        loadRemoteConfig()
        loadServerConfig()
    }

    /**
     * Load server configuration (default provider, model, modes) from /config endpoint.
     */
    private fun loadServerConfig() {
        screenModelScope.launch {
            // Load config for default model
            configRepository.getConfig().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val config = resource.data
                        _state.value = _state.value.copy(
                            serverConfig = config,
                            serverDefaultModel = config.model,
                            serverModes = config.modes
                        )
                        Napier.i { "Server config loaded: defaultModel=${config.model}, modes=${config.modes.size}" }
                    }
                    is Resource.Error -> {
                        Napier.w { "Failed to load server config: ${resource.message}" }
                    }
                    is Resource.Loading -> {
                        // Already handled by isLoading state
                    }
                }
            }
        }

        // Load providers config for default provider
        screenModelScope.launch {
            configRepository.getProvidersConfig().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val config = resource.data
                        val defaultProvider = config.default.entries.firstOrNull()?.key
                        _state.value = _state.value.copy(
                            serverDefaultProvider = defaultProvider
                        )
                        Napier.i { "Providers config loaded: defaultProvider=$defaultProvider, providers=${config.providers.size}" }
                    }
                    is Resource.Error -> {
                        Napier.w { "Failed to load providers config: ${resource.message}" }
                    }
                    is Resource.Loading -> {
                        // Already handled by isLoading state
                    }
                }
            }
        }
    }
    
    // Remote Config Logic
    fun loadRemoteConfig() {
        screenModelScope.launch {
            // Only try if connected
            if (_state.value.activeConnectionState is ConnectionStatus.Connected) {
                // Load providers for auth configuration
                loadProviders()
            }
        }
    }
    
    private fun loadProviders() {
        screenModelScope.launch {
            // We need a way to get providers. 
            // Since we don't have direct access to MoccaApiClient here (it's inside Repos),
            // we should probably add getProviders to ConfigRepository or similar.
            // For now, let's assume we can add it to ConfigRepository or use existing one.
            // Wait, ConfigRepository doesn't have getProviders. 
            // But SessionRepository does (via getProviderInfo). 
            // Let's rely on ConfigRepository having it, or add it.
            // Actually, let's just fetch auth methods for known providers if we can't list them easily.
            // BETTER: Add getProviders to ConfigRepository or inject ProviderRepository if it exists.
            // Modules.kt has singleOf(::ProviderRepository). Let's use that if possible, but I can't change constructor easily without breaking DI.
            // I'll skip fetching the full provider list for now and just allow user to type provider ID or 
            // use a hardcoded list of common ones (anthropic, openai, github) for the UI prototype.
            // Ideally, we'd fetch from /config/providers.
        }
    }
    
    fun loadAuthMethods(providerId: String) {
        screenModelScope.launch {
            _state.value = _state.value.copy(selectedProviderId = providerId, authLoading = true)
            configRepository.getProviderAuthMethods(providerId).collect { resource ->
                when(resource) {
                    is Resource.Success -> {
                        _state.value = _state.value.copy(
                            authLoading = false, 
                            providerAuthMethods = _state.value.providerAuthMethods + (providerId to resource.data)
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(authLoading = false, message = "Failed to load auth methods: ${resource.message}")
                    }
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(authLoading = true)
                    }
                }
            }
        }
    }
    
    fun startOAuth(providerId: String, openUrl: (String) -> Unit) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = configRepository.startOAuthFlow(providerId)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(isLoading = false)
                    openUrl(result.data.url)
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(isLoading = false, message = "OAuth failed: ${result.message}")
                }
                else -> {}
            }
        }
    }
    
    fun setManualKey(providerId: String, key: String) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val credentials = ProviderCredentials.ApiKey(key) // Default to ApiKey type
            // Note: For OpenAI/Anthropic specific types, we might need logic to choose.
            // But ApiKey is usually generic enough or we check providerId.
            val specificCredentials = when(providerId) {
                "openai" -> ProviderCredentials.OpenAI(key)
                "anthropic" -> ProviderCredentials.Anthropic(key)
                else -> ProviderCredentials.ApiKey(key)
            }
            
            when (val result = configRepository.setProviderCredentials(providerId, specificCredentials)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(isLoading = false, message = "Credentials saved for $providerId")
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(isLoading = false, message = "Failed to save credentials: ${result.message}")
                }
                else -> {}
            }
        }
    }

    fun updateRemoteConfig(update: ConfigUpdate) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, message = "Updating config...")
            when(val resource = configRepository.updateConfig(update)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(isLoading = false, message = "Configuration updated")
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(isLoading = false, message = "Config update failed: ${resource.message}")
                }
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }

    
    fun checkForUpdates() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, message = "Checking for updates...")

            when (val result = updateRepository.checkForUpdateDetailed()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    // Notify global update notifier so MainScreen shows the dialog
                    updateNotifier.notifyUpdateAvailable(result.updateInfo)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = "Update available: ${result.updateInfo.version}. Return to main screen to install."
                    )
                }
                is UpdateCheckResult.NoUpdate -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = "No updates available - you have the latest version"
                    )
                }
                is UpdateCheckResult.Error -> {
                    val friendlyMsg = buildString {
                        append("Update check failed: ")
                        append(result.message)
                        
                        // Add specific token-related guidance
                        result.tokenStatus?.let { status ->
                            when (status) {
                                is GitHubTokenStatus.Invalid -> {
                                    append("\n\nYour GitHub token is invalid or expired. Please update it below.")
                                }
                                is GitHubTokenStatus.Missing -> {
                                    append("\n\nNo GitHub token configured. Add a token for better rate limits.")
                                }
                                else -> {}
                            }
                        }
                    }
                    
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = friendlyMsg,
                        githubTokenStatus = result.tokenStatus
                    )
                }
            }
        }
    }

    /**
     * Validates the GitHub token and updates the UI with the status.
     */
    fun validateGitHubToken() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isValidatingToken = true, message = "Validating token...")
            
            val status = updateRepository.validateGitHubToken()
            
            val message = when (status) {
                is GitHubTokenStatus.Valid -> "Token is valid and working"
                is GitHubTokenStatus.Missing -> "No token configured. Add a GitHub PAT for update checks."
                is GitHubTokenStatus.Invalid -> "Token is invalid: ${status.reason}"
                is GitHubTokenStatus.Error -> "Validation error: ${status.message}"
            }
            
            _state.value = _state.value.copy(
                isValidatingToken = false,
                githubTokenStatus = status,
                message = message
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
                message = "Token saved. Validating..."
            )
            
            // Validate the token after saving
            val status = updateRepository.validateGitHubToken()
            val message = when (status) {
                is GitHubTokenStatus.Valid -> "GitHub token saved and validated successfully"
                is GitHubTokenStatus.Missing -> "Token cleared"
                is GitHubTokenStatus.Invalid -> "Token saved but appears invalid: ${status.reason}"
                is GitHubTokenStatus.Error -> "Token saved but validation failed: ${status.message}"
            }
            
            _state.value = _state.value.copy(
                githubTokenStatus = status,
                message = message
            )
        }
    }

    private fun observeActiveConnectionState() {
        screenModelScope.launch {
            connectionManager.status.collect { state ->
                _state.value = _state.value.copy(activeConnectionState = state)
                
                val activeId = _state.value.activeServerId
                if (activeId != null) {
                    val status = when (state) {
                        is ConnectionStatus.NotConfigured -> ServerConnectionStatus.UNKNOWN
                        is ConnectionStatus.Connecting -> ServerConnectionStatus.CHECKING
                        is ConnectionStatus.WaitingForNetwork -> ServerConnectionStatus.CHECKING
                        is ConnectionStatus.Reconnecting -> ServerConnectionStatus.CHECKING
                        is ConnectionStatus.Connected -> ServerConnectionStatus.CONNECTED
                        is ConnectionStatus.Disconnected -> ServerConnectionStatus.FAILED
                        is ConnectionStatus.Error -> ServerConnectionStatus.FAILED
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
            name = "New Server",
            host = "",
            port = 4096,
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
            connectionManager.checkConnection()
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
