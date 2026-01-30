package com.mocca.app.ui.screens.onboarding

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.AppConnectionManager
import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.discovery.DiscoveryResult
import com.mocca.app.discovery.ServerDiscoveryManager
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.domain.model.QrConnectionPayload
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Enhanced ScreenModel for the progressive onboarding wizard.
 * 
 * Features:
 * - Step-based wizard flow (Welcome → Discovery → Selection → Connection → Ready)
 * - Automatic server discovery via mDNS
 * - QR code scanning for instant pairing
 * - Smart connection with fallback chain
 * - Manual entry as last resort
 */
class OnboardingWizardModel(
    private val serverConfigRepository: ServerConfigRepository,
    private val appConnectionManager: AppConnectionManager,
    private val serverDiscoveryManager: ServerDiscoveryManager? = null
) : ScreenModel {
    
    private val _state = MutableStateFlow(OnboardingWizardState())
    val state: StateFlow<OnboardingWizardState> = _state.asStateFlow()
    
    init {
        loadSavedServers()
        // Auto-start discovery if available
        if (serverDiscoveryManager != null) {
            startDiscovery()
        }
    }
    
    private fun loadSavedServers() {
        screenModelScope.launch {
            try {
                val saved = serverConfigRepository.getAllServers()
                _state.update { it.copy(savedServers = saved) }
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
            is OnboardingAction.ManualEntryUpdated -> updateManualEntry(action.url, action.token)
            is OnboardingAction.Connect -> connect()
            is OnboardingAction.RetryConnection -> retryConnection()
            is OnboardingAction.Back -> goBack()
            is OnboardingAction.Skip -> skipOnboarding()
            is OnboardingAction.Complete -> completeOnboarding()
            is OnboardingAction.DiscoveryCompleted -> onDiscoveryCompleted(action.result)
            is OnboardingAction.ConnectionResult -> onConnectionResult(action.success, action.error)
        }
    }
    
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
                val result = if (serverDiscoveryManager != null) {
                    serverDiscoveryManager.discoverServers(timeoutMs = 5000)
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
                discoveredServers = result.servers,
                isLoading = false,
                currentStep = OnboardingStep.SELECT_SERVER
            )
        }
        
        // Auto-proceed if we have exactly one server
        if (result.servers.size == 1) {
            selectServer(result.servers.first())
        }
    }
    
    fun onQrCodeScanned(qrContent: String) {
        Napier.d("QR code scanned: $qrContent")
        
        // Try to parse as JSON payload
        val payload = QrConnectionPayload.fromJson(qrContent)
            ?: QrConnectionPayload.fromUrl(qrContent)?.let {
                // Try to parse as URL
                it
            }
        
        if (payload != null) {
            val discovered = payload.toDiscoveredServer()
            _state.update {
                it.copy(
                    discoveredServers = it.discoveredServers + discovered,
                    selectedServer = discovered
                )
            }
            selectServer(discovered)
        } else {
            _state.update {
                it.copy(error = "Invalid QR code format")
            }
        }
    }
    
    private fun selectServer(server: DiscoveredServer) {
        _state.update {
            it.copy(
                selectedServer = server,
                currentStep = OnboardingStep.CONNECTING,
                isLoading = true,
                connectionProgress = "Connecting to ${server.name}...",
                error = null
            )
        }
        
        // Auto-connect after selection
        connect()
    }
    
    private fun updateManualEntry(url: String, token: String) {
        _state.update {
            it.copy(
                manualServerUrl = url,
                manualAuthToken = token
            )
        }
    }
    
    private fun connect() {
        val selected = _state.value.selectedServer
        
        if (selected != null) {
            // Connect to selected discovered/saved server
            val config = selected.toServerConfig()
            
            screenModelScope.launch {
                try {
                    // Save the server config
                    serverConfigRepository.saveServer(config)
                    serverConfigRepository.setActiveServer(config.id)
                    
                    // Attempt connection
                    appConnectionManager.checkConnection()
                    
                    // Wait and check result
                    delay(2000)
                    
                    val isConnected = appConnectionManager.connectionState.value.isConnected
                    onConnectionResult(isConnected, if (!isConnected) "Connection failed" else null)
                    
                } catch (e: Exception) {
                    Napier.e("Connection failed", e)
                    onConnectionResult(false, e.message)
                }
            }
        } else if (_state.value.manualServerUrl.isNotBlank()) {
            // Manual entry fallback
            connectManual()
        } else {
            // Try smart connect with all available servers
            screenModelScope.launch {
                val candidates = _state.value.allServers.map { it.toServerConfig() }
                val connected = appConnectionManager.connectWithDiscovery(candidates)
                
                if (connected) {
                    onConnectionResult(true, null)
                } else {
                    onConnectionResult(false, "Could not connect to any server")
                }
            }
        }
    }
    
    private fun connectManual() {
        val url = _state.value.manualServerUrl.trim()
        val token = _state.value.manualAuthToken.trim()
        
        if (url.isBlank()) {
            _state.update { it.copy(error = "Server URL is required") }
            return
        }
        
        screenModelScope.launch {
            try {
                val config = com.mocca.app.domain.model.ServerConfig(
                    id = "manual-${System.currentTimeMillis()}",
                    name = "Manual Server",
                    baseUrl = url,
                    authToken = token.takeIf { it.isNotEmpty() },
                    authType = if (token.isNotEmpty()) 
                        com.mocca.app.domain.model.AuthType.BEARER 
                    else 
                        com.mocca.app.domain.model.AuthType.NONE,
                    isActive = true
                )
                
                serverConfigRepository.saveServer(config)
                serverConfigRepository.setActiveServer(config.id)
                
                appConnectionManager.checkConnection()
                
                delay(2000)
                
                val isConnected = appConnectionManager.connectionState.value.isConnected
                onConnectionResult(isConnected, if (!isConnected) "Connection failed" else null)
                
            } catch (e: Exception) {
                Napier.e("Manual connection failed", e)
                onConnectionResult(false, e.message)
            }
        }
    }
    
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
            it.copy(
                error = null,
                isLoading = true
            )
        }
        connect()
    }
    
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
                isLoading = false
            )
        }
    }
    
    private fun skipOnboarding() {
        // For demo/testing: mark as connected and proceed
        _state.update {
            it.copy(
                isConnected = true,
                currentStep = OnboardingStep.READY
            )
        }
    }
    
    private fun completeOnboarding() {
        // Called when user confirms they're ready
        // Navigation to MainScreen is handled by observing isConnected
    }
    
    fun reset() {
        _state.update { OnboardingWizardState() }
        loadSavedServers()
    }
}
