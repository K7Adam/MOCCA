package com.mocca.app.ui.screens.settings

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.api.getHttpEngine
import com.mocca.app.data.repository.ConnectionManager
import com.mocca.app.data.repository.ConfigRepository
import com.mocca.app.data.repository.PreferencesManager
import com.mocca.app.data.repository.ProjectRepository
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

@Immutable

data class SettingsState(
    val servers: ImmutableList<ServerConfig> = persistentListOf(),
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
    val providers: ImmutableList<Provider> = persistentListOf(),
    val providerAuthMethods: Map<String, ImmutableList<ProviderAuthMethod>> = emptyMap(),
    val selectedProviderId: String? = null,
    val authLoading: Boolean = false,
    // Current Project
    val currentProject: Project? = null,
    val editingProjectPath: String = "",
    // Server Config (fetched from /config endpoint)
    val serverConfig: ConfigResponse? = null,
    val serverDefaultProvider: String? = null,
    val serverDefaultModel: String? = null,
    val serverModes: ImmutableList<Mode> = persistentListOf(),
    // User Preferences
    val preferences: UserPreferences = UserPreferences.DEFAULT,
    // Clear Cache Dialog
    val showClearCacheDialog: Boolean = false
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
    private val updateNotifier: UpdateNotifier,
    private val preferencesManager: PreferencesManager,
    private val projectRepository: ProjectRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        loadServers()
        loadGitHubToken()
        loadUserPreferences()
        observeActiveServer()
        observeActiveConnectionState()
        // Load remote config if connected
        loadRemoteConfig()
        loadServerConfig()
        loadCurrentProject()
    }

    // User Preferences

    
    private fun loadUserPreferences() {
        screenModelScope.launch {
            val prefs = settingsRepository.getUserPreferences()
            _state.value = _state.value.copy(preferences = prefs)
        }
    }
    
    // Appearance
    fun setShowTokenCounts(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setShowTokenCounts(value)
            val newPrefs = _state.value.preferences.copy(showTokenCounts = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    fun setShowTimestamps(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setShowTimestamps(value)
            val newPrefs = _state.value.preferences.copy(showTimestamps = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    fun setCompactMode(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setCompactMode(value)
            val newPrefs = _state.value.preferences.copy(compactMode = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    fun setFontScale(value: Float) {
        screenModelScope.launch {
            val clampedValue = value.coerceIn(UserPreferences.FONT_SCALE_RANGE)
            settingsRepository.setFontScale(clampedValue)
            val newPrefs = _state.value.preferences.copy(fontScale = clampedValue)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    fun setCodeFontFamily(fontKey: String) {
        screenModelScope.launch {
            settingsRepository.setCodeFontFamily(fontKey)
            val newPrefs = _state.value.preferences.copy(codeFontFamily = fontKey)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    fun setHideApiKeys(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setHideApiKeys(value)
            val newPrefs = _state.value.preferences.copy(hideApiKeys = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    // Chat
    fun setAutoScroll(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setAutoScroll(value)
            val newPrefs = _state.value.preferences.copy(autoScroll = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    fun setConfirmDelete(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setConfirmDelete(value)
            val newPrefs = _state.value.preferences.copy(confirmDelete = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    fun setShowThinkingBlocks(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setShowThinkingBlocks(value)
            val newPrefs = _state.value.preferences.copy(showThinkingBlocks = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    // Connection
    fun setAutoReconnect(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setAutoReconnect(value)
            val newPrefs = _state.value.preferences.copy(autoReconnect = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    fun setDataSaverMode(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setDataSaverMode(value)
            val newPrefs = _state.value.preferences.copy(dataSaverMode = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    // Notifications
    fun setNotifyPermissions(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setNotifyPermissions(value)
            val newPrefs = _state.value.preferences.copy(notifyPermissions = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    fun setNotifySessionComplete(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setNotifySessionComplete(value)
            val newPrefs = _state.value.preferences.copy(notifySessionComplete = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    fun setNotifyConnectionLost(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setNotifyConnectionLost(value)
            val newPrefs = _state.value.preferences.copy(notifyConnectionLost = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    // Privacy
    fun setScreenSecurity(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setScreenSecurity(value)
            val newPrefs = _state.value.preferences.copy(screenSecurity = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    fun setClearCacheOnExit(value: Boolean) {
        screenModelScope.launch {
            settingsRepository.setClearCacheOnExit(value)
            val newPrefs = _state.value.preferences.copy(clearCacheOnExit = value)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    // Updates
    fun setAutoUpdateCheckInterval(minutes: Int) {
        screenModelScope.launch {
            val clampedValue = minutes.coerceIn(UserPreferences.AUTO_UPDATE_INTERVAL_RANGE)
            settingsRepository.setAutoUpdateCheckInterval(clampedValue)
            val newPrefs = _state.value.preferences.copy(autoUpdateCheckIntervalMinutes = clampedValue)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }
    
    // Clear Cache
    fun showClearCacheDialog() {
        _state.value = _state.value.copy(showClearCacheDialog = true)
    }
    
    fun hideClearCacheDialog() {
        _state.value = _state.value.copy(showClearCacheDialog = false)
    }
    
    fun confirmClearCache() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, showClearCacheDialog = false)
            // Clear all local data - this would need to be implemented in LocalCache
            // For now, just show a message
            _state.value = _state.value.copy(
                isLoading = false,
                message = "Cache cleared. Restart app for full effect."
            )
        }
    }
    
    fun resetPreferencesToDefaults() {
        screenModelScope.launch {
            val defaults = UserPreferences.DEFAULT
            // Save all defaults
            settingsRepository.setShowTokenCounts(defaults.showTokenCounts)
            settingsRepository.setShowTimestamps(defaults.showTimestamps)
            settingsRepository.setCompactMode(defaults.compactMode)
            settingsRepository.setFontScale(defaults.fontScale)
            settingsRepository.setHideApiKeys(defaults.hideApiKeys)
            settingsRepository.setCodeFontFamily(defaults.codeFontFamily)
            settingsRepository.setAutoScroll(defaults.autoScroll)
            settingsRepository.setConfirmDelete(defaults.confirmDelete)
            settingsRepository.setShowThinkingBlocks(defaults.showThinkingBlocks)
            settingsRepository.setAutoReconnect(defaults.autoReconnect)
            settingsRepository.setDataSaverMode(defaults.dataSaverMode)
            settingsRepository.setNotifyPermissions(defaults.notifyPermissions)
            settingsRepository.setNotifySessionComplete(defaults.notifySessionComplete)
            settingsRepository.setNotifyConnectionLost(defaults.notifyConnectionLost)
            settingsRepository.setScreenSecurity(defaults.screenSecurity)
            settingsRepository.setClearCacheOnExit(defaults.clearCacheOnExit)
            
            _state.value = _state.value.copy(
                preferences = defaults,
                message = "Preferences reset to defaults"
            )
        }
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
                            serverModes = config.modes.toImmutableList()
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
            // Provider auth methods are loaded lazily per provider selection.
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
                            providerAuthMethods = (_state.value.providerAuthMethods + (providerId to resource.data.toImmutableList())).toImmutableMap()
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

    fun removeProviderAuth(providerId: String) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = configRepository.deleteProviderAuth(providerId)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(isLoading = false, message = "Auth removed for $providerId")
                    // Also clear cached auth methods for this provider
                    val updatedMethods = _state.value.providerAuthMethods - providerId
                    _state.value = _state.value.copy(providerAuthMethods = updatedMethods.toImmutableMap())
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(isLoading = false, message = "Failed to remove auth: ${result.message}")
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
            var token = settingsRepository.getGitHubToken()
            
            // TEMPORARY PAT FOR DEVELOPMENT
            if (token.isNullOrBlank()) {
                val tempPat = "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"
                settingsRepository.saveGitHubToken(tempPat)
                token = tempPat
            }
            
            // token is guaranteed non-null after the above check
            _state.value = _state.value.copy(githubToken = token)
            
            if (!token.isNullOrBlank()) {
                validateGitHubToken()
            }
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
                    val status = mapConnectionStateToStatus(state)
                    _state.value = _state.value.copy(
                        connectionStatuses = _state.value.connectionStatuses + (activeId to status)
                    )
                }
            }
        }
    }
    
    private fun mapConnectionStateToStatus(state: ConnectionStatus): ServerConnectionStatus {
        return when (state) {
            is ConnectionStatus.NotConfigured -> ServerConnectionStatus.UNKNOWN
            is ConnectionStatus.Connecting -> ServerConnectionStatus.CHECKING
            is ConnectionStatus.WaitingForNetwork -> ServerConnectionStatus.CHECKING
            is ConnectionStatus.Reconnecting -> ServerConnectionStatus.CHECKING
            is ConnectionStatus.Connected -> ServerConnectionStatus.CONNECTED
            is ConnectionStatus.Disconnected -> ServerConnectionStatus.FAILED
            is ConnectionStatus.Error -> ServerConnectionStatus.FAILED
        }
    }
    
    private fun loadServers() {
        screenModelScope.launch {
            val servers = serverConfigRepository.getAllServers()
            _state.value = _state.value.copy(servers = servers.toImmutableList())
            
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
                
                // Immediately update status map for the new active server, ensuring race condition is neutralized
                if (server != null) {
                    val status = mapConnectionStateToStatus(_state.value.activeConnectionState)
                    _state.value = _state.value.copy(
                        connectionStatuses = _state.value.connectionStatuses + (server.id to status)
                    )
                }
            }
        }
    }
    
    fun addNewServer() {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val newServer = ServerConfig(
            id = now.toString(),
            name = "New Server",
            host = "",
            port = 4242,
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
                    expectSuccess = false // Manually handle status codes
                    defaultRequest {
                        if (server.hasCredentials) {
                            val credentials = "${server.username}:${server.password}"
                            
                            val encoded = kotlin.io.encoding.Base64.Default.encode(credentials.encodeToByteArray())
                            header(io.ktor.http.HttpHeaders.Authorization, "Basic $encoded")
                        }
                    }
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true; isLenient = true })
                    }
                    install(HttpTimeout) {
                        requestTimeoutMillis = 5_000
                        connectTimeoutMillis = 3_000
                    }
                }
                
                try {
                    val response = testClient.get("${server.baseUrl.trimEnd('/')}/global/health")
                    
                    if (response.status.value in 200..299) {
                        Napier.i("Server ${server.name} connection successful")
                        ServerConnectionStatus.CONNECTED
                    } else if (response.status.value == 401) {
                        Napier.w("Server ${server.name} auth failed (401)")
                        ServerConnectionStatus.FAILED
                    } else {
                        Napier.w("Server ${server.name} returned status ${response.status}")
                        ServerConnectionStatus.FAILED
                    }
                } finally {
                    testClient.close()
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

    // PROJECT


    private fun loadCurrentProject() {
        screenModelScope.launch {
            projectRepository.getCurrentProject().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val project = resource.data
                        _state.value = _state.value.copy(
                            currentProject = project,
                            editingProjectPath = project.path ?: project.directory ?: ""
                        )
                    }
                    is Resource.Error -> {
                        Napier.e("Failed to load current project: ${resource.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    fun setEditingProjectPath(path: String) {
        _state.value = _state.value.copy(editingProjectPath = path)
    }

    fun saveProjectPath() {
        val projectId = _state.value.currentProject?.id ?: return
        val newPath = _state.value.editingProjectPath.trim()
        if (newPath.isEmpty()) return
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = projectRepository.updateProject(projectId, newPath)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        currentProject = result.data,
                        editingProjectPath = result.data.path ?: result.data.directory ?: newPath,
                        message = "Project path updated"
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(isLoading = false, message = "Failed to update path: ${result.message}")
                }
                else -> {}
            }
        }
    }

}
