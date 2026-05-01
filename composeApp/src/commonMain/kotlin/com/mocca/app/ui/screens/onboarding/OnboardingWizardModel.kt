package com.mocca.app.ui.screens.onboarding

import cafe.adriel.voyager.core.model.ScreenModel
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
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.ServerConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the progressive onboarding wizard.
 *
 * Bridge-only 3-step flow:
 * - WELCOME: Brand intro + setup checklist with quick-start reference
 * - CONNECT: MOCCA CLI bridge pairing payload input
 * - CONNECTING: Staged progress (save → resolve → auth → runtime → config import) with auto-navigation on success
 */
class OnboardingWizardModel(
    private val serverConfigRepository: ServerConfigRepository,
    private val connectionManager: ConnectionManager,
    private val appStateStore: AppStateStore,
    private val bridgeConnectionManager: BridgeConnectionManager? = null
) : ScreenModel {

    private val _state = MutableStateFlow(OnboardingWizardState())
    val state: StateFlow<OnboardingWizardState> = _state.asStateFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.GoToConnect -> goToConnect()
            is OnboardingAction.BridgePairingPayloadChanged -> updateBridgePairingPayload(action.payload)
            is OnboardingAction.BridgePairingPayloadReceived -> connectBridgePairingPayload(action.payload)
            is OnboardingAction.ConnectBridgePairingPayload -> connectBridgePairingPayload()
            is OnboardingAction.BridgePairingError -> onBridgePairingError(action.message)
            is OnboardingAction.RetryConnection -> retryConnection()
            is OnboardingAction.Back -> goBack()
            is OnboardingAction.Skip -> skipOnboarding()
            is OnboardingAction.Complete -> completeOnboarding()
            is OnboardingAction.ConnectionResult -> onConnectionResult(action.success, action.error)
            is OnboardingAction.InitializeSetupMode -> initializeSetupMode(action.error)
        }
    }

    private fun initializeSetupMode(error: String?) {
        _state.update {
            it.copy(
                currentStep = OnboardingStep.CONNECT,
                error = error
            )
        }
    }

    // Navigation

    private fun goToConnect() {
        _state.update {
            it.copy(
                currentStep = OnboardingStep.CONNECT,
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

    // Bridge Pairing

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
                serverConfigRepository.saveActiveServer(cliServerConfig)
                waitForServerConnection()

                _state.update {
                    it.copy(
                        connectionStage = ConnectionStage.IMPORTING_CONFIG,
                        connectionProgress = "Importing local OpenCode configuration..."
                    )
                }
                val config = repository.fetchAiRuntimeConfig(forceRefresh = true)
                if (config.providers.none { it.connected && it.models.isNotEmpty() }) {
                    error("OpenCode is available, but no usable provider/model configuration was found.")
                }

                val summary = BridgeValidationSummary(
                    opencodeAvailable = true,
                    opencodeVersion = null,
                    runtimeBaseUrl = runtimeServer.baseUrl,
                    configFileCount = 0,
                    credentialCount = config.providers.count { it.connected },
                    agentCount = config.agents.size,
                    commandCount = config.modes.size,
                    mcpServerCount = 0
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
        _state.update {
            it.copy(
                error = null,
                isLoading = true,
                connectionStage = ConnectionStage.SAVING_CONFIG
            )
        }
        connectBridgePairingPayload(_state.value.bridgePairingPayload)
    }

    fun reset() {
        _state.update { OnboardingWizardState() }
    }
}
