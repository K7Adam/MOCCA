package com.mocca.app.discovery

import com.mocca.app.domain.model.DiscoveredServer
import kotlinx.coroutines.flow.Flow

/**
 * Interface for server discovery functionality.
 * Implemented in platform-specific modules (androidMain).
 */
interface ServerDiscovery {
    /**
     * Start discovering OpenCode servers on the local network.
     * Returns a Flow of discovery events.
     */
    fun startDiscovery(): Flow<DiscoveryEvent>
    
    /**
     * Discover servers as a one-shot operation with timeout.
     */
    suspend fun discoverServers(timeoutMs: Long = 5000): DiscoveryResult
    
    /**
     * Stop the current discovery process.
     */
    fun stopDiscovery()
    
    /**
     * Get the list of currently discovered servers.
     */
    fun getDiscoveredServers(): List<DiscoveredServer>
    
    /**
     * Check if the device supports multicast (required for mDNS).
     */
    fun isMulticastSupported(): Boolean
}

/**
 * No-op implementation for platforms that don't support discovery.
 */
object NoOpServerDiscovery : ServerDiscovery {
    override fun startDiscovery(): Flow<DiscoveryEvent> = kotlinx.coroutines.flow.emptyFlow()
    override suspend fun discoverServers(timeoutMs: Long): DiscoveryResult = 
        DiscoveryResult(emptyList(), DiscoveryState.STOPPED, "Not supported on this platform")
    override fun stopDiscovery() {}
    override fun getDiscoveredServers(): List<DiscoveredServer> = emptyList()
    override fun isMulticastSupported(): Boolean = false
}
