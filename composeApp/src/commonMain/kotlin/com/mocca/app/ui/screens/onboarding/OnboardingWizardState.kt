package com.mocca.app.ui.screens.onboarding

import com.mocca.app.api.NetworkConfig

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

import com.mocca.app.discovery.DiscoveryResult
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.domain.model.ServerConfig

/**
 * Steps in the progressive onboarding wizard (discovery-first 3-step flow).
 */
enum class OnboardingStep {
    /** Welcome screen with branding and quick-start setup checklist */
    WELCOME,
    /** Server discovery (primary) + manual entry (fallback) */
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
    
    // Discovery results
    val discoveredServers: ImmutableList<DiscoveredServer> = persistentListOf(),
    val savedServers: ImmutableList<ServerConfig> = persistentListOf(),
    val selectedServer: DiscoveredServer? = null,
    val isDiscovering: Boolean = false,
    
    // Connection state
    val connectionProgress: String = "",
    val connectionStage: ConnectionStage = ConnectionStage.SAVING_CONFIG,
    val isConnected: Boolean = false,
    val isSuccess: Boolean = false,
    
    // Manual entry (fallback)
    val manualHost: String = NetworkConfig.DEFAULT_HOST_IP,
    val manualPort: String = NetworkConfig.OPENCODE_SERVER_PORT.toString(),
    val manualUsername: String = NetworkConfig.DEFAULT_USERNAME,
    val manualPassword: String = NetworkConfig.DEFAULT_PASSWORD,
    
    // Credential prompt (for mDNS-discovered servers without credentials)
    val needsCredentials: Boolean = false,
    val credentialServer: DiscoveredServer? = null,
    
    // Manual entry expansion state
    val showManualEntry: Boolean = false
) {
    val canProceed: Boolean
        get() = when (currentStep) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.CONNECT -> selectedServer != null
            OnboardingStep.CONNECTING -> isConnected
        }
    
    val hasServers: Boolean
        get() = discoveredServers.isNotEmpty() || savedServers.isNotEmpty()
    
    val allServers: ImmutableList<DiscoveredServer>
        get() {
            val savedAsDiscovered = savedServers.map { config ->
                DiscoveredServer(
                    name = config.name,
                    host = config.host,
                    port = config.port,
                    username = config.username,
                    password = config.password,
                    source = com.mocca.app.domain.model.DiscoverySource.SAVED,
                    useHttps = config.useHttps
                )
            }
            return (savedAsDiscovered + discoveredServers).distinctBy { it.baseUrl }.toImmutableList()
        }
}

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
    data object StartDiscovery : OnboardingAction()
    data class ServerSelected(val server: DiscoveredServer) : OnboardingAction()
    data class ManualConnect(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val useHttps: Boolean = false
    ) : OnboardingAction()
    data class CredentialsProvided(val username: String, val password: String) : OnboardingAction()
    data object GoToConnect : OnboardingAction()
    data object GoToManualEntry : OnboardingAction()
    data object Connect : OnboardingAction()
    data object RetryConnection : OnboardingAction()
    data object Back : OnboardingAction()
    data object Skip : OnboardingAction()
    data object Complete : OnboardingAction()
    data class DiscoveryCompleted(val result: DiscoveryResult) : OnboardingAction()
    data class ConnectionResult(val success: Boolean, val error: String? = null) : OnboardingAction()
    data class InitializeSetupMode(val error: String?) : OnboardingAction()
}
