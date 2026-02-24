package com.mocca.app.ui.screens.onboarding

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.ConnectionManager
import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.discovery.DiscoveryResult
import com.mocca.app.discovery.ServerDiscovery
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.domain.model.DiscoverySource
import com.mocca.app.domain.model.QrConnectionPayload
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
 * Features:
 * - Step-based wizard flow (Welcome → Discovery → Selection → Connection → Ready)
 * - Automatic server discovery via mDNS
 * - QR code scanning for instant pairing
 * - Manual entry with Host/Port/Username/Password
 * - Robust connection via ConnectionManager.connect() (no race conditions)
 */
class OnboardingWizardModel(
    private val serverConfigRepository: ServerConfigRepository,
    private val connectionManager: ConnectionManager,
    private val serverDiscovery: ServerDiscovery? = null
) : ScreenModel {

    private val _state = MutableStateFlow(OnboardingWizardState())
    val state: StateFlow<OnboardingWizardState> = _state.asStateFlow()

    init {
        loadSavedServers()
        // Auto-start discovery if available
        if (serverDiscovery != null) {
            startDiscovery()
        }
    }

    private fun loadSavedServers() {
        screenModelScope.launch {
            try {
                val saved = serverConfigRepository.getAllServers()
                    .filter { it.host.isNotBlank() } // Filter out invalid configs (BUG 5 safety)
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
            is OnboardingAction.GoToManualEntry -> goToManualEntry()
            is OnboardingAction.Connect -> connect()
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
                currentStep = OnboardingStep.DISCOVERING,
                error = error,
                isLoading = true // Starting discovery immediately
            )
        }
        
        // Start discovery as if the user clicked "Start"
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
                        isLoading = false,
                        error = "Discovery failed: ${e.message}",
                        currentStep = OnboardingStep.SELECT_SERVER
                    )
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Discovery
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun startDiscovery() {
        _state.update {
            it.copy(
                currentStep = OnboardingStep.DISCOVERING,
                isLoading = true,
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
                        isLoading = false,
                        error = "Discovery failed: ${e.message}",
                        currentStep = OnboardingStep.SELECT_SERVER
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
                isLoading = false,
                currentStep = OnboardingStep.SELECT_SERVER
            )
        }

        // Auto-proceed if we have exactly one server with credentials
        if (result.servers.size == 1) {
            val server = result.servers.first()
            if (server.password.isNotBlank()) {
                selectServer(server)
            }
            // If no credentials, let user tap to trigger credential prompt
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // QR Code
    // ═══════════════════════════════════════════════════════════════════════════════

    fun onQrCodeScanned(qrContent: String) {
        Napier.d("QR code scanned: $qrContent")

        val payload = QrConnectionPayload.fromJson(qrContent)
            ?: QrConnectionPayload.fromUrl(qrContent)

        if (payload != null) {
            val discovered = payload.toDiscoveredServer()
            _state.update {
                it.copy(
                    discoveredServers = (it.discoveredServers + discovered).toImmutableList(),
                    selectedServer = discovered
                )
            }
            // QR codes include credentials → connect directly
            selectServer(discovered)
        } else {
            _state.update {
                it.copy(error = "Invalid QR code format")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Server Selection
    // ═══════════════════════════════════════════════════════════════════════════════

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

        // Server has credentials (QR, saved, or manual) → connect directly
        _state.update {
            it.copy(
                selectedServer = server,
                needsCredentials = false,
                credentialServer = null,
                currentStep = OnboardingStep.CONNECTING,
                isLoading = true,
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
                connectionProgress = "Connecting to ${withCreds.name}...",
                error = null
            )
        }

        connectWithConfig(withCreds.toServerConfig())
    }

    private fun goToManualEntry() {
        _state.update {
            it.copy(currentStep = OnboardingStep.SELECT_SERVER, error = null)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Manual Entry
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun connectManual(host: String, port: Int, username: String, password: String, useHttps: Boolean = false) {
        Napier.i("[OnboardingWizard] connectManual called - host: $host, port: $port, useHttps: $useHttps")
        
        if (host.isBlank()) {
            _state.update { it.copy(error = "Host is required") }
            return
        }

        // Use provided useHttps, or auto-detect Tailscale (.ts.net) if not specified
        val isTailscale = host.endsWith(".ts.net")
        val effectiveUseHttps = if (useHttps) true else isTailscale
        val effectivePort = when {
            useHttps && port == 4096 -> 443  // User toggled HTTPS on, use 443
            !useHttps && port == 443 -> 4096  // User toggled HTTPS off, use 4096
            else -> port
        }
        
        Napier.i("[OnboardingWizard] Effective settings - useHttps: $effectiveUseHttps, port: $effectivePort")

        val config = ServerConfig(
            id = "manual-${System.currentTimeMillis()}",
            name = "OpenCode ($host)",
            host = host,
            port = effectivePort,
            username = username.ifBlank { "opencode" },
            password = password,
            isActive = true,
            useHttps = effectiveUseHttps
        )
        
        Napier.i("[OnboardingWizard] ServerConfig created - baseUrl: ${config.baseUrl}")

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
                connectionProgress = "Connecting to $protocol://$host:$effectivePort...",
                error = null
            )
        }

        connectWithConfig(config)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Connection (Core fix for BUG 2 — no more race condition)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Connect using ConnectionManager.connect() directly.
     *
     * This avoids the race condition where checkConnection() reads _activeConfig.value
     * before the async observeActiveServer() has propagated the new config.
     *
     * ConnectionManager.connect(config) directly:
     * 1. Sets _activeConfig.value = config
     * 2. Creates a new HttpClient with auth
     * 3. Calls setActiveServer() to persist
     * 4. Runs checkConnection() internally
     */
    private fun connectWithConfig(config: ServerConfig) {
        Napier.i("[OnboardingWizard] connectWithConfig starting for: ${config.baseUrl}")
        screenModelScope.launch {
            try {
                _state.update {
                    it.copy(connectionProgress = "Saving server configuration...")
                }

                // 1. Save to DB first
                Napier.i("[OnboardingWizard] Saving server config to DB...")
                serverConfigRepository.saveServer(config)
                Napier.i("[OnboardingWizard] Server config saved")

                // 2. Connect DIRECTLY — this sets _activeConfig, creates client, runs health check
                _state.update { it.copy(connectionProgress = "Connecting to ${config.name}...") }
                Napier.i("[OnboardingWizard] Calling connectionManager.connect()...")
                connectionManager.connect(config)
                Napier.i("[OnboardingWizard] connectionManager.connect() returned")

                // 3. Poll connection status with timeout
                val maxAttempts = 15  // 15 * 500ms = 7.5s timeout
                var attempts = 0
                Napier.i("[OnboardingWizard] Starting connection status polling (max $maxAttempts attempts)")
                while (attempts < maxAttempts) {
                    delay(500)
                    val status = connectionManager.status.value
                    Napier.d("[OnboardingWizard] Poll attempt $attempts - Status: ${status::class.simpleName}")
                    when (status) {
                        is ConnectionStatus.Connected -> {
                            Napier.i("[OnboardingWizard] CONNECTED to ${config.name}")
                            onConnectionResult(true, null)
                            return@launch
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
                            // Still connecting/reconnecting — keep polling
                            attempts++
                            _state.update {
                                it.copy(
                                    connectionProgress = "Connecting... (${attempts}s)"
                                )
                            }
                        }
                    }
                }

                // Timeout
                onConnectionResult(false, "Connection timed out. Is OpenCode running?")
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

    /**
     * Simple connect using currently selected server.
     */
    private fun connect() {
        val selected = _state.value.selectedServer
        if (selected != null) {
            connectWithConfig(selected.toServerConfig())
        } else {
            _state.update { it.copy(error = "No server selected") }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Connection Result Handling
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun onConnectionResult(success: Boolean, error: String?) {
        if (success) {
            _state.update {
                it.copy(
                    isConnected = true,
                    isLoading = false,
                    currentStep = OnboardingStep.READY,
                    error = null
                )
            }
        } else {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = error ?: "Connection failed",
                    currentStep = OnboardingStep.SELECT_SERVER
                )
            }
        }
    }

    private fun retryConnection() {
        _state.update {
            it.copy(error = null, isLoading = true)
        }
        connect()
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Navigation
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun goBack() {
        val currentStep = _state.value.currentStep
        val previousStep = when (currentStep) {
            OnboardingStep.DISCOVERING -> OnboardingStep.WELCOME
            OnboardingStep.SELECT_SERVER -> OnboardingStep.WELCOME
            OnboardingStep.CONNECTING -> OnboardingStep.SELECT_SERVER
            OnboardingStep.READY -> OnboardingStep.CONNECTING
            else -> currentStep
        }

        _state.update {
            it.copy(
                currentStep = previousStep,
                error = null,
                isLoading = false,
                needsCredentials = false,
                credentialServer = null
            )
        }
    }

    private fun skipOnboarding() {
        _state.update {
            it.copy(
                isConnected = true,
                currentStep = OnboardingStep.READY
            )
        }
    }

    private fun completeOnboarding() {
        // Navigation to MainScreen is handled by observing isConnected in the UI
    }

    fun reset() {
        _state.update { OnboardingWizardState() }
        loadSavedServers()
    }
}
