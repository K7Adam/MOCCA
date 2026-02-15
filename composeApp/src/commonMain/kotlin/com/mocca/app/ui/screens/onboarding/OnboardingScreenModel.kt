package com.mocca.app.ui.screens.onboarding

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.ConnectionManager
import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.domain.model.ServerConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Boot sequence log entries for terminal effect.
 */
data class BootLogEntry(
    val message: String,
    val status: BootStatus = BootStatus.PENDING
)

enum class BootStatus {
    PENDING,
    RUNNING,
    DONE,
    ERROR,
    WAIT
}

/**
 * Connection probing state.
 */
enum class ProbeState {
    IDLE,
    PROBING,
    SUCCESS,
    FAILED
}

/**
 * State for the onboarding screen.
 */
data class OnboardingState(
    // Boot sequence
    val bootLogs: List<BootLogEntry> = emptyList(),
    val bootComplete: Boolean = false,
    
    // Connection probing
    val probeState: ProbeState = ProbeState.IDLE,
    val probeMessage: String = "PROBING_HOST...",
    
    // Form inputs
    val serverAddress: String = "http://localhost:4096",
    val authToken: String = "",
    
    // Connection state
    val isConnecting: Boolean = false,
    val connectionError: String? = null,
    val isConnected: Boolean = false,
    
    // Existing servers
    val savedServers: List<ServerConfig> = emptyList(),
    val hasExistingConnection: Boolean = false
)

/**
 * ScreenModel for the onboarding screen.
 */
class OnboardingScreenModel(
    private val serverConfigRepository: ServerConfigRepository,
    private val connectionManager: ConnectionManager
) : ScreenModel {
    
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()
    
    init {
        checkExistingConnections()
        startBootSequence()
    }
    
    private fun checkExistingConnections() {
        screenModelScope.launch {
            val servers = serverConfigRepository.getAllServers()
            val active = serverConfigRepository.getActiveServerConfig()
            
            _state.update {
                it.copy(
                    savedServers = servers,
                    hasExistingConnection = true,
                    serverAddress = active.baseUrl,
                    authToken = active.password
                )
            }
        }
    }
    
    private fun startBootSequence() {
        screenModelScope.launch {
            val bootMessages = listOf(
                "Initializing core modules..." to 300L,
                "Loading AI subsystems..." to 400L,
                "Configuring network stack..." to 350L,
                "Mounting virtual filesystem..." to 250L,
                "Starting agent runtime..." to 500L
            )
            
            bootMessages.forEachIndexed { index, (message, delayMs) ->
                // Add as pending
                _state.update {
                    it.copy(
                        bootLogs = it.bootLogs + BootLogEntry(message, BootStatus.RUNNING)
                    )
                }
                
                delay(delayMs)
                
                // Mark as done
                _state.update {
                    val logs = it.bootLogs.toMutableList()
                    logs[index] = logs[index].copy(status = BootStatus.DONE)
                    it.copy(bootLogs = logs)
                }
            }
            
            delay(200)
            _state.update { it.copy(bootComplete = true) }
            
            // Start probing if we have an existing connection
            if (_state.value.hasExistingConnection) {
                startProbing()
            }
        }
    }
    
    private fun startProbing() {
        screenModelScope.launch {
            _state.update { it.copy(probeState = ProbeState.PROBING) }
            
            delay(1500) // Simulate probing
            
            // Try to connect with existing config
            try {
                connectionManager.checkConnection()
                
                // Wait a bit and check connection status
                delay(2000)
                
                val isConnected = connectionManager.status.value.isConnected
                _state.update {
                    it.copy(
                        probeState = if (isConnected) ProbeState.SUCCESS else ProbeState.FAILED,
                        isConnected = isConnected,
                        probeMessage = if (isConnected) "HOST_FOUND" else "NO_RESPONSE"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        probeState = ProbeState.FAILED,
                        probeMessage = "CONNECTION_REFUSED"
                    )
                }
            }
        }
    }
    
    fun updateServerAddress(address: String) {
        _state.update { it.copy(serverAddress = address, connectionError = null) }
    }
    
    fun updateAuthToken(token: String) {
        _state.update { it.copy(authToken = token, connectionError = null) }
    }
    
    fun connect() {
        val currentState = _state.value
        if (currentState.serverAddress.isBlank()) {
            _state.update { it.copy(connectionError = "Server address is required") }
            return
        }
        
        screenModelScope.launch {
            _state.update { it.copy(isConnecting = true, connectionError = null) }
            
            try {
                // Save or update server config
                // Parse host and port from the address string
                val cleanAddress = currentState.serverAddress.trim()
                    .removePrefix("http://").removePrefix("https://")
                val addressParts = cleanAddress.split(":")
                val host = addressParts.firstOrNull() ?: "localhost"
                val port = addressParts.getOrNull(1)?.toIntOrNull() ?: 4096
                
                val config = ServerConfig(
                    id = "default",
                    name = "OpenCode Server",
                    host = host,
                    port = port,
                    password = currentState.authToken.trim(),
                    isActive = true
                )
                
                serverConfigRepository.saveServer(config)
                serverConfigRepository.setActiveServer(config.id)
                
                // Connect
                connectionManager.checkConnection()
                
                // Wait for connection
                delay(2000)
                
                val isConnected = connectionManager.status.value.isConnected
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = isConnected,
                        connectionError = if (!isConnected) "Failed to connect. Check server address." else null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        connectionError = e.message ?: "Connection failed"
                    )
                }
            }
        }
    }
    
    fun skipOnboarding() {
        // Mark as connected to proceed (for demo/testing)
        _state.update { it.copy(isConnected = true) }
    }
}
