package com.mocca.app.ui.screens.onboarding

import cafe.adriel.voyager.core.model.ScreenModel
import com.mocca.app.api.NetworkConfig
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.bridge.client.DirectBridgeNetwork
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.bridge.connection.BridgePairingPayloadException
import com.mocca.app.bridge.connection.BridgePairingPayloadParser
import com.mocca.app.bridge.opencode.BridgeFeatureUnavailableException
import com.mocca.app.bridge.opencode.BridgeResponseException
import com.mocca.app.bridge.opencode.OpenCodeBridgeRepository
import com.mocca.app.data.repository.AppStateStore
import com.mocca.app.data.repository.ConnectionManager
import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.discovery.DiscoveryResult
import com.mocca.app.discovery.ServerDiscovery
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.domain.model.DiscoverySource
import com.mocca.app.domain.model.ServerConfig
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the progressive onboarding wizard.
 *
 * Discovery-first 3-step flow:
 * - WELCOME: Brand intro + setup checklist with quick-start reference
 * - CONNECT: Auto-discovery (background) + server list + manual entry fallback
 * - CONNECTING: Staged progress (save → resolve → auth → config import → API test) with auto-navigation on success
 */
class OnboardingWizardModel(
    private val serverConfigRepository: ServerConfigRepository,
    private val connectionManager: ConnectionManager,
    private val serverDiscovery: ServerDiscovery? = null,
    private val appStateStore: AppStateStore,
    private val bridgeConnectionManager: BridgeConnectionManager? = null
) : ScreenModel {

    private val _state = MutableStateFlow(OnboardingWizardState())
    val state: StateFlow<OnboardingWizardState> = _state.asStateFlow()

    init {
        loadSavedServers()
    }

    private fun loadSavedServers() {
        screenModelScope.launch {
            try {
                var saved = serverConfigRepository.getAllServers()
                    .filter { it.host.isNotBlank() }
                
                // Add default emulator server if missing, ensuring emulator developers always have a quick option
                val defaultConfig = serverConfigRepository.createDefaultConfig()
                if (defaultConfig != null && saved.none { it.host == defaultConfig.host && it.port == defaultConfig.port }) {
                    saved = listOf(defaultConfig) + saved
                }
                
                _state.update { it.copy(savedServers = saved.toImmutableList()) }
                Napier.d("Loaded ${saved.size} saved servers")
            } catch (e: Exception) {
                Napier.w("Could not load saved servers", e)
            }
        }
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.StartDiscovery -> startDiscovery()
            is OnboardingAction.ServerSelected -> selectServer(action.server)
            is OnboardingAction.ManualConnect -> connectManual(
                action.host, action.port, action.username, action.password, action.useHttps
            )
            is OnboardingAction.CredentialsProvided -> onCredentialsProvided(
                action.username, action.password
            )
            is OnboardingAction.GoToConnect -> goToConnect()
            is OnboardingAction.GoToManualEntry -> goToManualEntry()
            is OnboardingAction.Connect -> connect()
            is OnboardingAction.BridgePairingPayloadChanged -> updateBridgePairingPayload(action.payload)
            is OnboardingAction.BridgePairingPayloadReceived -> connectBridgePairingPayload(action.payload)
            is OnboardingAction.ConnectBridgePairingPayload -> connectBridgePairingPayload()
            is OnboardingAction.BridgePairingError -> onBridgePairingError(action.message)
            is OnboardingAction.RetryConnection -> retryConnection()
            is OnboardingAction.Back -> goBack()
            is OnboardingAction.Skip -> skipOnboarding()
            is OnboardingAction.Complete -> completeOnboarding()
            is OnboardingAction.DiscoveryCompleted -> onDiscoveryCompleted(action.result)
            is OnboardingAction.ConnectionResult -> onConnectionResult(action.success, action.error)
            is OnboardingAction.InitializeSetupMode -> initializeSetupMode(action.error)
        }
    }

    private fun initializeSetupMode(error: String?) {
        _state.update {
            it.copy(
                currentStep = OnboardingStep.CONNECT,
                error = error,
                isDiscovering = true
            )
        }
        
        // Start discovery in background
        screenModelScope.launch {
            try {
                val result = if (serverDiscovery != null) {
                    serverDiscovery.discoverServers(timeoutMs = 5000)
                } else {
                    DiscoveryResult(emptyList(), com.mocca.app.discovery.DiscoveryState.STOPPED)
                }
                onDiscoveryCompleted(result)
            } catch (e: Exception) {
                Napier.e("Discovery failed", e)
                _state.update {
                    it.copy(
                        isDiscovering = false,
                        error = "Discovery failed: ${e.message}"
                    )
                }
            }
        }
    }

    // Discovery (runs in background on CONNECT step)


    private fun startDiscovery() {
        _state.update {
            it.copy(
                isDiscovering = true,
                error = null
            )
        }

        screenModelScope.launch {
            try {
                val result = if (serverDiscovery != null) {
                    serverDiscovery.discoverServers(timeoutMs = 5000)
                } else {
                    DiscoveryResult(emptyList(), com.mocca.app.discovery.DiscoveryState.STOPPED)
                }
                onDiscoveryCompleted(result)
            } catch (e: Exception) {
                Napier.e("Discovery failed", e)
                _state.update {
                    it.copy(
                        isDiscovering = false,
                        error = "Discovery failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun onDiscoveryCompleted(result: DiscoveryResult) {
        Napier.i("Discovery completed: ${result.servers.size} servers found")

        _state.update {
            it.copy(
                discoveredServers = result.servers.toImmutableList(),
                isDiscovering = false
            )
        }

        // Auto-select and connect if exactly one server with credentials
        if (result.servers.size == 1) {
            val server = result.servers.first()
            if (server.password.isNotBlank()) {
                selectServer(server)
            }
        }
    }

    // Server Selection


    private fun selectServer(server: DiscoveredServer) {
        // mDNS-discovered servers don't have credentials — prompt user
        if (server.source == DiscoverySource.MDNS && server.password.isBlank()) {
            _state.update {
                it.copy(
                    selectedServer = server,
                    needsCredentials = true,
                    credentialServer = server,
                    error = null
                )
            }
            return
        }

        // Server has credentials (saved, or manual) → connect directly
        _state.update {
            it.copy(
                selectedServer = server,
                needsCredentials = false,
                credentialServer = null,
                currentStep = OnboardingStep.CONNECTING,
                isLoading = true,
                connectionStage = ConnectionStage.SAVING_CONFIG,
                connectionMode = OnboardingConnectionMode.OPENCODE_SERVER,
                connectionProgress = "Connecting to ${server.name}...",
                error = null
            )
        }

        connectWithConfig(server.toServerConfig())
    }

    private fun onCredentialsProvided(username: String, password: String) {
        val server = _state.value.credentialServer ?: return
        val withCreds = server.copy(
            username = username.ifBlank { "opencode" },
            password = password
        )

        _state.update {
            it.copy(
                selectedServer = withCreds,
                needsCredentials = false,
                credentialServer = null,
                currentStep = OnboardingStep.CONNECTING,
                isLoading = true,
                connectionStage = ConnectionStage.SAVING_CONFIG,
                connectionMode = OnboardingConnectionMode.OPENCODE_SERVER,
                connectionProgress = "Connecting to ${withCreds.name}...",
                error = null
            )
        }

        connectWithConfig(withCreds.toServerConfig())
    }

    // Navigation


    private fun goToConnect() {
        _state.update {
            it.copy(
                currentStep = OnboardingStep.CONNECT,
                error = null
            )
        }
        // Start discovery in background when entering Connect step
        startDiscovery()
    }

    private fun goToManualEntry() {
        _state.update {
            it.copy(
                currentStep = OnboardingStep.CONNECT,
                showManualEntry = true,
                error = null
            )
        }
    }

    private fun goBack() {
        val currentStep = _state.value.currentStep
        val previousStep = when (currentStep) {
            OnboardingStep.CONNECT -> OnboardingStep.WELCOME
            OnboardingStep.CONNECTING -> OnboardingStep.CONNECT
            else -> currentStep
        }

        _state.update {
            it.copy(
                currentStep = previousStep,
                error = null,
                isLoading = false,
                needsCredentials = false,
                credentialServer = null,
                isSuccess = false,
                bridgePairingNetwork = parseBridgeNetworkHint(it.bridgePairingPayload),
                bridgeValidationSummary = null,
                connectionStage = ConnectionStage.SAVING_CONFIG
            )
        }
    }

    private fun skipOnboarding() {
        _state.update {
            it.copy(
                isConnected = true,
                isSuccess = true,
                currentStep = OnboardingStep.CONNECTING
            )
        }
    }

    private fun completeOnboarding() {
        // Navigation to MainScreen is handled by observing isSuccess in the UI
    }

    // Manual Entry


    private fun connectManual(host: String, port: Int, username: String, password: String, useHttps: Boolean = false) {
        Napier.i("[OnboardingWizard] connectManual called - host: $host, port: $port, useHttps: $useHttps")
        
        if (host.isBlank()) {
            _state.update { it.copy(error = "Host is required") }
            return
        }

        val effectiveUseHttps = useHttps
        val effectivePort = port
        
        Napier.i("[OnboardingWizard] Effective settings - useHttps: $effectiveUseHttps, port: $effectivePort")

        val config = ServerConfig(
            id = "manual-${System.currentTimeMillis()}",
            name = "OpenCode ($host)",
            host = host,
            port = effectivePort,
            username = username.ifBlank { NetworkConfig.DEFAULT_USERNAME },
            password = password,
            isActive = true,
            useHttps = effectiveUseHttps
        )

        val discovered = DiscoveredServer(
            name = config.name,
            host = host,
            port = effectivePort,
            username = config.username,
            password = password,
            source = DiscoverySource.MANUAL,
            useHttps = effectiveUseHttps
        )

        val protocol = if (effectiveUseHttps) "https" else "http"
        _state.update {
            it.copy(
                selectedServer = discovered,
                currentStep = OnboardingStep.CONNECTING,
                isLoading = true,
                connectionStage = ConnectionStage.SAVING_CONFIG,
                connectionMode = OnboardingConnectionMode.OPENCODE_SERVER,
                connectionProgress = "Connecting to $protocol://$host:$effectivePort...",
                error = null
            )
        }

        connectWithConfig(config)
    }

    // Connection (with staged progress)


    private fun connectWithConfig(config: ServerConfig) {
        Napier.i("[OnboardingWizard] connectWithConfig starting for: ${config.baseUrl}")
        screenModelScope.launch {
            try {
                // Stage 1: Save config
                _state.update {
                    it.copy(
                        connectionStage = ConnectionStage.SAVING_CONFIG,
                        connectionProgress = "Saving server configuration..."
                    )
                }
                Napier.i("[OnboardingWizard] Saving server config to DB...")
                serverConfigRepository.saveServer(config)
                Napier.i("[OnboardingWizard] Server config saved")

                // Stage 2: Resolve server
                delay(300) // Brief pause for animation
                _state.update {
                    it.copy(
                        connectionStage = ConnectionStage.RESOLVING_SERVER,
                        connectionProgress = "Resolving server..."
                    )
                }

                // Stage 3: Authenticate
                delay(300)
                _state.update {
                    it.copy(
                        connectionStage = ConnectionStage.AUTHENTICATING,
                        connectionProgress = "Authenticating..."
                    )
                }
                
                Napier.i("[OnboardingWizard] Calling connectionManager.connect()...")
                connectionManager.connect(config)
                Napier.i("[OnboardingWizard] connectionManager.connect() returned")

                // Stage 4: Test API
                _state.update {
                    it.copy(
                        connectionStage = ConnectionStage.TESTING_API,
                        connectionProgress = "Testing API connection..."
                    )
                }

                // Poll connection status with timeout
                val maxAttempts = 15  // 15 * 500ms = 7.5s timeout
                var attempts = 0
                var isConnected = false
                while (attempts < maxAttempts) {
                    delay(500)
                    val status = connectionManager.status.value
                    Napier.d("[OnboardingWizard] Poll attempt $attempts - Status: ${status::class.simpleName}")
                    when (status) {
                        is ConnectionStatus.Connected -> {
                            Napier.i("[OnboardingWizard] CONNECTED to ${config.name}")
                            isConnected = true
                            break
                        }
                        is ConnectionStatus.Error -> {
                            Napier.e("[OnboardingWizard] Connection ERROR: ${status.message}")
                            onConnectionResult(false, status.message)
                            return@launch
                        }
                        is ConnectionStatus.Disconnected -> {
                            Napier.e("[OnboardingWizard] Connection DISCONNECTED: ${status.reason}")
                            onConnectionResult(
                                false,
                                status.reason ?: "Connection failed"
                            )
                            return@launch
                        }
                        else -> {
                            attempts++
                        }
                    }
                }

                if (!isConnected) {
                    // Timeout
                    onConnectionResult(false, "Connection timed out. Is OpenCode running?")
                    return@launch
                }

                // Stage 5: Import config (providers, models, agents)
                // Now that we are connected, wait for providers to be loaded by AppStateStore
                _state.update {
                    it.copy(
                        connectionStage = ConnectionStage.IMPORTING_CONFIG,
                        connectionProgress = "Importing providers and models..."
                    )
                }
                
                var providerAttempts = 0
                while (providerAttempts < 10) {
                    delay(500)
                    val providers = appStateStore.providers.value
                    if (providers is com.mocca.app.domain.model.Resource.Success) {
                        val providerCount = providers.data.all.size
                        _state.update {
                            it.copy(
                                connectionProgress = "Imported $providerCount providers!"
                            )
                        }
                        delay(800)
                        break
                    }
                    providerAttempts++
                }

                onConnectionResult(true, null)
            } catch (e: Exception) {
                Napier.e("Connection failed", e)
                val friendlyMessage = when {
                    e.message?.contains("401") == true ->
                        "Authentication failed. Check username and password."
                    e.message?.contains("Connection refused") == true ->
                        "Server not reachable. Is OpenCode running?"
                    else -> e.message ?: "Connection failed"
                }
                onConnectionResult(false, friendlyMessage)
            }
        }
    }

    private fun connect() {
        val selected = _state.value.selectedServer
        if (selected != null) {
            connectWithConfig(selected.toServerConfig())
        } else {
            _state.update { it.copy(error = "No server selected") }
        }
    }

    private fun updateBridgePairingPayload(payload: String) {
        _state.update {
            it.copy(
                bridgePairingPayload = payload,
                bridgePairingNetwork = parseBridgeNetworkHint(payload),
                error = null
            )
        }
    }

    private fun onBridgePairingError(message: String) {
        _state.update {
            it.copy(
                error = message,
                bridgeValidationSummary = null
            )
        }
    }

    private fun connectBridgePairingPayload(payload: String = _state.value.bridgePairingPayload) {
        val pairingPayload = payload.trim()
        if (pairingPayload.isBlank()) {
            _state.update { it.copy(error = "MOCCA CLI pairing link is required") }
            return
        }

        val manager = bridgeConnectionManager
        if (manager == null) {
            _state.update { it.copy(error = "MOCCA CLI bridge is not available on this build") }
            return
        }

        _state.update {
            it.copy(
                currentStep = OnboardingStep.CONNECTING,
                isLoading = true,
                error = null,
                connectionMode = OnboardingConnectionMode.MOCCA_CLI_BRIDGE,
                connectionStage = ConnectionStage.SAVING_CONFIG,
                connectionProgress = "Reading MOCCA CLI pairing link...",
                bridgePairingPayload = pairingPayload,
                bridgePairingNetwork = parseBridgeNetworkHint(pairingPayload),
                bridgeValidationSummary = null
            )
        }

        screenModelScope.launch {
            try {
                delay(150)
                _state.update {
                    it.copy(
                        connectionStage = ConnectionStage.RESOLVING_SERVER,
                        connectionProgress = "Checking MOCCA CLI health..."
                    )
                }

                manager.connectFromPairingPayload(pairingPayload)

                val status = manager.status.value
                if (status !is BridgeConnectionStatus.Connected) {
                    val message = when (status) {
                        is BridgeConnectionStatus.Error -> status.message
                        BridgeConnectionStatus.NotConfigured -> "MOCCA CLI pairing target is missing"
                        else -> "MOCCA CLI bridge connection failed"
                    }
                    onConnectionResult(false, message)
                    return@launch
                }

                _state.update {
                    it.copy(
                        connectionStage = ConnectionStage.AUTHENTICATING,
                        connectionProgress = "Verifying pairing code..."
                    )
                }

                val client = manager.client.value
                    ?: error("MOCCA CLI bridge connected without an active client")
                val repository = OpenCodeBridgeRepository(client)

                _state.update {
                    it.copy(
                        connectionStage = ConnectionStage.TESTING_API,
                        connectionProgress = "Starting OpenCode runtime..."
                    )
                }
                val capabilities = status.capabilities
                if (!capabilities.ai.opencodeConfigSnapshot) {
                    throw BridgeFeatureUnavailableException("ai.config.snapshot")
                }
                if (!capabilities.ai.opencodeRuntime) {
                    throw BridgeFeatureUnavailableException("ai.runtime.ensure")
                }

                val runtime = repository.ensureOpenCodeRuntime()
                val runtimeServer = runtime.server
                val cliServerConfig = ServerConfig(
                    id = "mocca-cli-${runtimeServer.host}-${runtimeServer.port}",
                    name = "MOCCA CLI (${runtimeServer.host})",
                    host = runtimeServer.host,
                    port = runtimeServer.port,
                    username = runtimeServer.username,
                    password = runtimeServer.password,
                    isActive = true,
                    useHttps = runtimeServer.useHttps
                )
                serverConfigRepository.saveServer(cliServerConfig)
                connectionManager.connect(cliServerConfig)
                waitForServerConnection()

                _state.update {
                    it.copy(
                        connectionStage = ConnectionStage.IMPORTING_CONFIG,
                        connectionProgress = "Importing local OpenCode configuration..."
                    )
                }
                val snapshot = repository.fetchOpenCodeConfigSnapshot()
                if (!snapshot.installed.available) {
                    val detail = snapshot.installed.error?.let { ": $it" }.orEmpty()
                    error("OpenCode is not available on your computer$detail")
                }
                val credentials = repository.fetchCredentials()
                val agents = repository.fetchAgents()
                val commands = repository.fetchCommands()
                val mcpServers = repository.fetchMcpServers()

                val summary = BridgeValidationSummary(
                    opencodeAvailable = snapshot.installed.available,
                    opencodeVersion = snapshot.installed.version,
                    runtimeBaseUrl = runtimeServer.baseUrl,
                    configFileCount = snapshot.configFiles.size,
                    credentialCount = credentials.size,
                    agentCount = agents.size,
                    commandCount = commands.size,
                    mcpServerCount = mcpServers.size
                )

                _state.update {
                    it.copy(
                        bridgeValidationSummary = summary,
                        connectionProgress = "Imported ${summary.credentialCount} providers, " +
                            "${summary.agentCount} agents, ${summary.commandCount} commands, " +
                            "${summary.mcpServerCount} MCP servers."
                    )
                }
                delay(800)
                onConnectionResult(true, null)
            } catch (error: BridgeResponseException) {
                onConnectionResult(false, error.message ?: "MOCCA CLI bridge request failed")
            } catch (error: BridgeFeatureUnavailableException) {
                onConnectionResult(false, "MOCCA CLI does not expose ${error.feature}")
            } catch (error: Exception) {
                Napier.e("MOCCA CLI bridge connection failed", error)
                onConnectionResult(false, bridgeFriendlyMessage(error))
            }
        }
    }

    private suspend fun waitForServerConnection() {
        val maxAttempts = 24
        repeat(maxAttempts) { attempt ->
            delay(250)
            when (val status = connectionManager.status.value) {
                is ConnectionStatus.Connected -> return
                is ConnectionStatus.Error -> error(status.message)
                is ConnectionStatus.Disconnected -> error(
                    status.reason ?: "OpenCode runtime disconnected"
                )
                else -> Napier.d("[OnboardingWizard] Waiting for CLI OpenCode runtime ($attempt/$maxAttempts)")
            }
        }
        error("MOCCA CLI started OpenCode, but the app could not reach it. Check that both devices are on the same network.")
    }

    private fun bridgeFriendlyMessage(error: Exception): String {
        val message = error.message ?: return "MOCCA CLI bridge connection failed"
        return when {
            message.contains("Connection refused", ignoreCase = true) ->
                "MOCCA CLI is not reachable. Is npx mocca-cli running on your computer?"
            message.contains("timeout", ignoreCase = true) ->
                "MOCCA CLI did not respond in time. Check that both devices are on the same network."
            message.contains("pairing", ignoreCase = true) ->
                message
            else -> message
        }
    }

    private fun parseBridgeNetworkHint(payload: String): DirectBridgeNetwork? {
        if (payload.isBlank()) return null
        return try {
            BridgePairingPayloadParser.parse(payload).network
        } catch (_: BridgePairingPayloadException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    // Connection Result — success shows brief animation then auto-completes


    private fun onConnectionResult(success: Boolean, error: String?) {
        if (success) {
            _state.update {
                it.copy(
                    isConnected = true,
                    isLoading = false,
                    connectionStage = ConnectionStage.CONNECTED,
                    connectionProgress = "Connected!",
                    error = null
                )
            }
            // Auto-navigate after success animation
            screenModelScope.launch {
                delay(1200) // Brief celebration
                _state.update { it.copy(isSuccess = true) }
            }
        } else {
            _state.update {
                it.copy(
                    isLoading = false,
                    connectionStage = ConnectionStage.FAILED,
                    error = error ?: "Connection failed",
                    currentStep = OnboardingStep.CONNECTING
                )
            }
        }
    }

    private fun retryConnection() {
        val current = _state.value
        _state.update {
            it.copy(
                error = null,
                isLoading = true,
                connectionStage = ConnectionStage.SAVING_CONFIG
            )
        }
        if (current.connectionMode == OnboardingConnectionMode.MOCCA_CLI_BRIDGE) {
            connectBridgePairingPayload(current.bridgePairingPayload)
        } else {
            connect()
        }
    }

    fun reset() {
        _state.update { OnboardingWizardState() }
        loadSavedServers()
    }
}
