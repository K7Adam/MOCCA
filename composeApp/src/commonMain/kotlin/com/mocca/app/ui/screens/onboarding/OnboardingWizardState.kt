package com.mocca.app.ui.screens.onboarding

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

import com.mocca.app.discovery.DiscoveryResult
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.domain.model.ServerConfig

/**
 * Steps in the progressive onboarding wizard.
 */
enum class OnboardingStep {
    WELCOME,
    DISCOVERING,
    SELECT_SERVER,
    CONNECTING,
    READY
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
    
    // Connection state
    val connectionProgress: String = "",
    val isConnected: Boolean = false,
    
    // Manual entry (fallback)
    val manualHost: String = "",
    val manualPort: String = "4096",
    val manualUsername: String = "opencode",
    val manualPassword: String = "",
    
    // Credential prompt (for mDNS-discovered servers without credentials)
    val needsCredentials: Boolean = false,
    val credentialServer: DiscoveredServer? = null
) {
    val canProceed: Boolean
        get() = when (currentStep) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.DISCOVERING -> discoveredServers.isNotEmpty() || savedServers.isNotEmpty()
            OnboardingStep.SELECT_SERVER -> selectedServer != null
            OnboardingStep.CONNECTING -> isConnected
            OnboardingStep.READY -> true
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
                    source = com.mocca.app.domain.model.DiscoverySource.SAVED
                )
            }
            return (savedAsDiscovered + discoveredServers).distinctBy { it.baseUrl }.toImmutableList()
        }
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
