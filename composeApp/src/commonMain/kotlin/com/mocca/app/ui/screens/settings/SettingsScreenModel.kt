package com.mocca.app.ui.screens.settings

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.api.getHttpEngine
import com.mocca.app.bridge.client.DirectBridgeTarget
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.bridge.connection.BridgeTargetRepository
import com.mocca.app.data.repository.AiRuntimeConfigRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.serialization.json.Json

@Immutable
data class SettingsState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val activeConnectionState: ConnectionStatus = ConnectionStatus.NotConfigured,
    val bridgeTarget: DirectBridgeTarget? = null,
    val bridgeConnectionState: BridgeConnectionStatus = BridgeConnectionStatus.NotConfigured,
    val cliConnectionUi: CliConnectionUiState = CliConnectionUiState(
        headline = "MOCCA CLI",
        statusLabel = "Not configured",
        supportingText = "Pair with mocca-cli to unlock native AI, files, git and terminal."
    ),
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
    val isSyncingConfig: Boolean = false,
    val configSyncMessage: String = "Waiting to sync server config",
    val configLastSyncedAt: Long? = null,
    val configSyncFailed: Boolean = false,
    val aiConfigState: AiConfigState = AiConfigState(),
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
    private val bridgeTargetRepository: BridgeTargetRepository,
    private val bridgeConnectionManager: BridgeConnectionManager,
    private val connectionManager: ConnectionManager,
    private val updateRepository: UpdateRepository,
    private val settingsRepository: SettingsRepository,
    private val configRepository: ConfigRepository,
    private val aiRuntimeConfigRepository: AiRuntimeConfigRepository,
    private val updateNotifier: UpdateNotifier,
    private val preferencesManager: PreferencesManager,
    private val projectRepository: ProjectRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        loadGitHubToken()
        loadUserPreferences()
        observeBridgeState()
        observeAiRuntimeConfig()
        observeActiveConnectionState()
        // Load remote config if connected
        loadRemoteConfig()
        loadServerConfig()
        loadCurrentProject()
    }

    private fun loadUserPreferences() {
        screenModelScope.launch {
            val prefs = settingsRepository.getUserPreferences()
            _state.value = _state.value.copy(preferences = prefs)
        }
    }

    private fun observeBridgeState() {
        screenModelScope.launch {
            combine(
                bridgeTargetRepository.activeTarget,
                bridgeConnectionManager.status,
                aiRuntimeConfigRepository.configState
            ) { target, bridgeStatus, aiConfigState ->
                Triple(target, bridgeStatus, aiConfigState)
            }.collect { (target, bridgeStatus, aiConfigState) ->
                _state.value = _state.value.copy(
                    bridgeTarget = target,
                    bridgeConnectionState = bridgeStatus,
                    cliConnectionUi = buildCliConnectionUiState(
                        target = target,
                        bridgeStatus = bridgeStatus,
                        aiConfigState = aiConfigState
                    )
                )
            }
        }
    }

    private fun observeAiRuntimeConfig() {
        screenModelScope.launch {
            aiRuntimeConfigRepository.configState.collect { config ->
                val snapshot = config.snapshot
                val defaultProviderName = config.effectiveSelection?.displayProvider
                    ?: snapshot?.findProvider(snapshot.defaultSelection.providerId)?.name
                val defaultModelName = config.effectiveSelection?.displayModel
                    ?: snapshot?.findModel(
                        snapshot.defaultSelection.providerId,
                        snapshot.defaultSelection.modelId
                    )?.name
                val modeOptions = snapshot?.modes
                    ?.map { Mode(id = it.id, name = it.name, description = it.description) }
                    ?.ifEmpty {
                        snapshot.agents
                            .filterNot { it.hidden }
                            .map { agent ->
                                Mode(
                                    id = agent.id,
                                    name = agent.name,
                                    description = agent.description
                                )
                            }
                    }
                    .orEmpty()
                    .toImmutableList()

                val syncMessage = when (config.status) {
                    AiConfigStatus.LOADING -> "Refreshing local OpenCode runtime config via MOCCA CLI..."
                    AiConfigStatus.READY -> "Runtime config imported from MOCCA CLI"
                    AiConfigStatus.UPDATE_REQUIRED -> config.errorMessage
                        ?: "Update MOCCA CLI to expose normalized AI config."
                    AiConfigStatus.ERROR -> config.errorMessage ?: "Unable to load runtime config"
                }

                _state.value = _state.value.copy(
                    aiConfigState = config,
                    serverDefaultProvider = defaultProviderName,
                    serverDefaultModel = defaultModelName,
                    serverModes = modeOptions,
                    isSyncingConfig = config.status == AiConfigStatus.LOADING,
                    configSyncFailed = config.status == AiConfigStatus.ERROR,
                    configSyncMessage = syncMessage,
                    configLastSyncedAt = if (snapshot != null && config.status != AiConfigStatus.LOADING) {
                        Clock.System.now().toEpochMilliseconds()
                    } else {
                        _state.value.configLastSyncedAt
                    }
                )
            }
        }
    }

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
    
    fun setCodeFontFamily(fontKey: String) {
        screenModelScope.launch {
            settingsRepository.setCodeFontFamily(fontKey)
            val newPrefs = _state.value.preferences.copy(codeFontFamily = fontKey)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }

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

    fun setAutoUpdateCheckInterval(minutes: Int) {
        screenModelScope.launch {
            val clampedValue = minutes.coerceIn(UserPreferences.AUTO_UPDATE_INTERVAL_RANGE)
            settingsRepository.setAutoUpdateCheckInterval(clampedValue)
            val newPrefs = _state.value.preferences.copy(autoUpdateCheckIntervalMinutes = clampedValue)
            _state.value = _state.value.copy(preferences = newPrefs)
            preferencesManager.updatePreferences(newPrefs)
        }
    }

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
            settingsRepository.setCodeFontFamily(defaults.codeFontFamily)
            settingsRepository.setNotifyPermissions(defaults.notifyPermissions)
            settingsRepository.setNotifySessionComplete(defaults.notifySessionComplete)
            settingsRepository.setNotifyConnectionLost(defaults.notifyConnectionLost)
            
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
        if (bridgeTargetRepository.activeTarget.value != null) {
            return
        }
        screenModelScope.launch {
            val currentState = _state.value
            val loadedProviderIds = currentState.providerAuthMethods.keys.toList()
            _state.value = currentState.copy(
                isSyncingConfig = true,
                configSyncFailed = false,
                configSyncMessage = if (currentState.configLastSyncedAt == null) {
                    "Loading server config..."
                } else {
                    "Refreshing server config..."
                }
            )

            var configLoaded = false
            var providersLoaded = false
            var providerAuthRefreshed = 0
            var configError: String? = null
            var providersError: String? = null
            val providerAuthErrors = mutableListOf<String>()

            configRepository.getConfig().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val config = resource.data
                        configLoaded = true
                        _state.value = _state.value.copy(
                            serverConfig = config,
                            serverDefaultModel = config.model,
                            serverModes = config.modes.toImmutableList()
                        )
                        Napier.i { "Server config loaded: defaultModel=${config.model}, modes=${config.modes.size}" }
                    }
                    is Resource.Error -> {
                        configError = resource.message
                        Napier.w { "Failed to load server config: ${resource.message}" }
                    }
                    is Resource.Loading -> {
                        // Already handled by isLoading state
                    }
                }
            }
            configRepository.getProvidersConfig().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val config = resource.data
                        val defaultProvider = config.default.entries.firstOrNull()?.key
                        providersLoaded = true
                        _state.value = _state.value.copy(
                            serverDefaultProvider = defaultProvider
                        )
                        Napier.i { "Providers config loaded: defaultProvider=$defaultProvider, providers=${config.providers.size}" }
                    }
                    is Resource.Error -> {
                        providersError = resource.message
                        Napier.w { "Failed to load providers config: ${resource.message}" }
                    }
                    is Resource.Loading -> {
                        // Already handled by isLoading state
                    }
                }
            }

            loadedProviderIds.forEach { providerId ->
                configRepository.getProviderAuthMethods(providerId).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            providerAuthRefreshed += 1
                            _state.value = _state.value.copy(
                                providerAuthMethods = (_state.value.providerAuthMethods + (providerId to resource.data.toImmutableList())).toImmutableMap()
                            )
                        }
                        is Resource.Error -> {
                            providerAuthErrors += "$providerId: ${resource.message}"
                        }
                        is Resource.Loading -> Unit
                    }
                }
            }

            val syncedAt = if (configLoaded || providersLoaded || providerAuthRefreshed > 0) {
                Clock.System.now().toEpochMilliseconds()
            } else {
                _state.value.configLastSyncedAt
            }
            val syncMessage = when {
                configLoaded && providersLoaded -> buildString {
                    append("Imported current /config defaults and provider settings")
                    if (providerAuthRefreshed > 0) {
                        append("; refreshed auth for $providerAuthRefreshed opened provider")
                        if (providerAuthRefreshed != 1) append('s')
                    }
                }
                configLoaded -> "Imported /config defaults; provider settings were unavailable"
                providersLoaded -> "Imported provider settings; app defaults were unavailable"
                providerAuthRefreshed > 0 -> "Refreshed auth methods for $providerAuthRefreshed opened provider" + if (providerAuthRefreshed == 1) "" else "s"
                else -> listOfNotNull(configError, providersError)
                    .plus(providerAuthErrors.takeIf { it.isNotEmpty() }?.joinToString(" • "))
                    .joinToString(" • ")
                    .ifBlank { "Unable to reach server config" }
            }

            _state.value = _state.value.copy(
                isSyncingConfig = false,
                configSyncMessage = syncMessage,
                configLastSyncedAt = syncedAt,
                configSyncFailed = !configLoaded && !providersLoaded && providerAuthRefreshed == 0
            )
        }
    }

    fun syncServerConfig() {
        screenModelScope.launch {
            if (bridgeConnectionManager.status.value is BridgeConnectionStatus.Connected) {
                _state.value = _state.value.copy(
                    isSyncingConfig = true,
                    configSyncFailed = false,
                    configSyncMessage = "Refreshing local OpenCode runtime config via MOCCA CLI..."
                )
                aiRuntimeConfigRepository.refresh(force = true)
            } else {
                loadServerConfig()
            }
        }
    }
    
    fun loadRemoteConfig() {
        // Remote config loading is triggered via loadAuthMethods when user selects a provider
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
                                    append("\n\nPublic MOCCA release checks do not require a token. Add one only if this device is rate-limited or you use a private fork.")
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
                is GitHubTokenStatus.Missing -> "No token configured. Public update checks still work."
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
            val token = settingsRepository.getGitHubToken().orEmpty()
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
            }
        }
    }

    fun reconnectCliBridge() {
        screenModelScope.launch {
            _state.value = _state.value.copy(message = "Reconnecting MOCCA CLI...")
            bridgeConnectionManager.connect()
            if (bridgeConnectionManager.status.value is BridgeConnectionStatus.Connected) {
                aiRuntimeConfigRepository.refresh(force = true)
            }
        }
    }

    fun disconnectCliBridge() {
        screenModelScope.launch {
            bridgeConnectionManager.disconnect()
            _state.value = _state.value.copy(message = "MOCCA CLI disconnected")
        }
    }

    fun forgetCliBridgeTarget() {
        screenModelScope.launch {
            bridgeConnectionManager.disconnect()
            bridgeTargetRepository.clear()
            _state.value = _state.value.copy(message = "Saved MOCCA CLI target removed")
        }
    }
    
    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

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
