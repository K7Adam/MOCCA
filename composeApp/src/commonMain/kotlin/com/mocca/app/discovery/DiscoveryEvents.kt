package com.mocca.app.discovery

import com.mocca.app.domain.model.DiscoveredServer

/**
 * Events emitted during server discovery.
 */
sealed class DiscoveryEvent {
    /** Discovery process has started */
    data object DiscoveryStarted : DiscoveryEvent()
    
    /** A new server has been discovered */
    data class ServerFound(val server: DiscoveredServer) : DiscoveryEvent()
    
    /** A previously discovered server is no longer available */
    data class ServerLost(val name: String) : DiscoveryEvent()
    
    /** Server information has been updated (e.g., IP changed) */
    data class ServerUpdated(val server: DiscoveredServer) : DiscoveryEvent()
    
    /** Discovery process has stopped */
    data object DiscoveryStopped : DiscoveryEvent()
    
    /** Discovery failed with an error */
    data class Error(val message: String, val exception: Throwable? = null) : DiscoveryEvent()
    
    /** Discovery timeout reached */
    data object DiscoveryTimeout : DiscoveryEvent()
}

/**
 * Current state of the discovery process.
 */
enum class DiscoveryState {
    IDLE,
    SCANNING,
    FOUND_SERVERS,
    ERROR,
    STOPPED
}

/**
 * Result of a discovery operation.
 */
data class DiscoveryResult(
    val servers: List<DiscoveredServer>,
    val state: DiscoveryState,
    val error: String? = null,
    val durationMs: Long = 0
)
