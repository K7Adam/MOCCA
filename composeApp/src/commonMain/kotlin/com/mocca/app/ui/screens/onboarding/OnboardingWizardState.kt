package com.mocca.app.ui.screens.onboarding

import com.mocca.app.bridge.client.DirectBridgeNetwork

import androidx.compose.runtime.Immutable
import com.mocca.app.domain.model.ServerConfig

/**
 * Steps in the progressive onboarding wizard (bridge-only 3-step flow).
 */
enum class OnboardingStep {
    /** Welcome screen with branding and quick-start setup checklist */
    WELCOME,
    /** MOCCA CLI bridge pairing */
    CONNECT,
    /** Staged connection with config import */
    CONNECTING
}

/**
 * State for the progressive onboarding wizard.
 */
@Immutable
data class OnboardingWizardState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val isLoading: Boolean = false,
    val error: String? = null,

    // Connection state
    val connectionProgress: String = "",
    val connectionStage: ConnectionStage = ConnectionStage.SAVING_CONFIG,
    val isConnected: Boolean = false,
    val isSuccess: Boolean = false,

    // MOCCA CLI direct bridge pairing
    val bridgePairingPayload: String = "",
    val bridgePairingNetwork: DirectBridgeNetwork? = null,
    val bridgeValidationSummary: BridgeValidationSummary? = null
) {
    val canProceed: Boolean
        get() = when (currentStep) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.CONNECT -> bridgePairingPayload.isNotBlank()
            OnboardingStep.CONNECTING -> isConnected
        }
}

@Immutable
data class BridgeValidationSummary(
    val opencodeAvailable: Boolean,
    val opencodeVersion: String?,
    val runtimeBaseUrl: String?,
    val configFileCount: Int,
    val credentialCount: Int,
    val agentCount: Int,
    val commandCount: Int,
    val mcpServerCount: Int
)

/**
 * Connection stages for the staged progress indicator.
 */
enum class ConnectionStage {
    SAVING_CONFIG,
    RESOLVING_SERVER,
    AUTHENTICATING,
    TESTING_API,
    IMPORTING_CONFIG,
    CONNECTED,
    FAILED
}

/**
 * Actions that can be performed in the onboarding wizard.
 */
sealed class OnboardingAction {
    data object GoToConnect : OnboardingAction()
    data class BridgePairingPayloadChanged(val payload: String) : OnboardingAction()
    data class BridgePairingPayloadReceived(val payload: String) : OnboardingAction()
    data object ConnectBridgePairingPayload : OnboardingAction()
    data class BridgePairingError(val message: String) : OnboardingAction()
    data object RetryConnection : OnboardingAction()
    data object Back : OnboardingAction()
    data object Skip : OnboardingAction()
    data object Complete : OnboardingAction()
    data class ConnectionResult(val success: Boolean, val error: String? = null) : OnboardingAction()
    data class InitializeSetupMode(val error: String?) : OnboardingAction()
}
