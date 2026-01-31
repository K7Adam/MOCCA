package com.mocca.app.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.domain.model.DiscoverySource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.os.Build
import java.net.InetAddress
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android-specific implementation of server discovery using NsdManager (mDNS/Bonjour).
 * 
 * This manager:
 * 1. Discovers OpenCode servers on the local network via mDNS
 * 2. Can advertise the OpenCode server for discovery by other devices
 * 3. Provides Flow-based API for reactive discovery
 */
class ServerDiscoveryManager(private val context: Context) : ServerDiscovery {
    
    companion object {
        private const val TAG = "ServerDiscoveryManager"
        private const val SERVICE_TYPE = "_opencode._tcp"
        private const val DISCOVERY_TIMEOUT_MS = 5000L
        private const val MAX_DISCOVERED_SERVERS = 10
    }
    
    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    
    private val wifiManager: WifiManager by lazy {
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val discoveredServers = mutableMapOf<String, DiscoveredServer>()
    
    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.IDLE)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()
    
    private var discoveryJob: Job? = null
    
    /**
     * Start discovering OpenCode servers on the local network.
     * Emits DiscoveryEvent through the returned Flow.
     * Discovery automatically stops after timeout or when the Flow is cancelled.
     */
    override fun startDiscovery(): Flow<DiscoveryEvent> = callbackFlow {
        if (_discoveryState.value == DiscoveryState.SCANNING) {
            Napier.w("$TAG: Discovery already running")
            send(DiscoveryEvent.Error("Discovery already in progress"))
            close()
            return@callbackFlow
        }
        
        // Acquire multicast lock for mDNS
        acquireMulticastLock()
        
        _discoveryState.value = DiscoveryState.SCANNING
        discoveredServers.clear()
        send(DiscoveryEvent.DiscoveryStarted)
        
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Napier.i("$TAG: Discovery started for $regType")
            }
            
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Napier.d("$TAG: Service found: ${serviceInfo.serviceName}")
                
                // Resolve the service to get IP and port
                resolveService(serviceInfo) { resolvedServer ->
                    if (resolvedServer != null) {
                        discoveredServers[resolvedServer.name] = resolvedServer
                        
                        launch {
                            send(DiscoveryEvent.ServerFound(resolvedServer))
                            
                            // If we have enough servers, stop discovery
                            if (discoveredServers.size >= MAX_DISCOVERED_SERVERS) {
                                stopDiscoveryInternal()
                            }
                        }
                    }
                }
            }
            
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Napier.d("$TAG: Service lost: ${serviceInfo.serviceName}")
                discoveredServers.remove(serviceInfo.serviceName)
                
                launch {
                    send(DiscoveryEvent.ServerLost(serviceInfo.serviceName))
                }
            }
            
            override fun onDiscoveryStopped(serviceType: String) {
                Napier.i("$TAG: Discovery stopped for $serviceType")
                _discoveryState.value = DiscoveryState.STOPPED
            }
            
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Napier.e("$TAG: Discovery start failed: $errorCode")
                _discoveryState.value = DiscoveryState.ERROR
                launch {
                    send(DiscoveryEvent.Error("Discovery failed to start (code: $errorCode)"))
                    close()
                }
            }
            
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Napier.e("$TAG: Discovery stop failed: $errorCode")
            }
        }
        
        discoveryListener = listener
        
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            
            // Timeout mechanism
            launch {
                delay(DISCOVERY_TIMEOUT_MS)
                if (isActive && _discoveryState.value == DiscoveryState.SCANNING) {
                    Napier.i("$TAG: Discovery timeout reached")
                    stopDiscoveryInternal()
                    send(DiscoveryEvent.DiscoveryTimeout)
                    close()
                }
            }
            
        } catch (e: Exception) {
            Napier.e("$TAG: Error starting discovery", e)
            _discoveryState.value = DiscoveryState.ERROR
            send(DiscoveryEvent.Error("Failed to start discovery: ${e.message}"))
            releaseMulticastLock()
            close(e)
        }
        
        awaitClose {
            Napier.d("$TAG: Flow closed, stopping discovery")
            stopDiscoveryInternal()
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Start discovery as a suspend function that returns all discovered servers.
     */
    override suspend fun discoverServers(timeoutMs: Long): DiscoveryResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val servers = mutableListOf<DiscoveredServer>()
            var error: String? = null
            
            try {
                withTimeoutOrNull(timeoutMs) {
                    startDiscovery().collect { event ->
                        when (event) {
                            is DiscoveryEvent.ServerFound -> servers.add(event.server)
                            is DiscoveryEvent.Error -> error = event.message
                            is DiscoveryEvent.DiscoveryTimeout -> cancel()
                            else -> { }
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Expected on timeout
            } catch (e: Exception) {
                error = e.message
                Napier.e("$TAG: Discovery error", e)
            }
            
            DiscoveryResult(
                servers = servers,
                state = if (error != null) DiscoveryState.ERROR else if (servers.isEmpty()) DiscoveryState.STOPPED else DiscoveryState.FOUND_SERVERS,
                error = error,
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Stop the current discovery process.
     */
    override fun stopDiscovery() {
        stopDiscoveryInternal()
    }
    
    private fun stopDiscoveryInternal() {
        try {
            discoveryListener?.let { listener ->
                nsdManager.stopServiceDiscovery(listener)
                discoveryListener = null
            }
        } catch (e: Exception) {
            Napier.w("$TAG: Error stopping discovery", e)
        }
        releaseMulticastLock()
        _discoveryState.value = DiscoveryState.STOPPED
    }
    
    /**
     * Resolve a discovered service to get its IP address and port.
     */
    private fun resolveService(
        serviceInfo: NsdServiceInfo,
        onResolved: (DiscoveredServer?) -> Unit
    ) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Napier.w("$TAG: Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                onResolved(null)
            }
            
            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                // Use hostAddresses (API 34+) with fallback to deprecated host for older versions
                val host = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    resolvedInfo.hostAddresses.firstOrNull()?.hostAddress
                } else {
                    @Suppress("DEPRECATION")
                    resolvedInfo.host?.hostAddress
                }
                val port = resolvedInfo.port
                val name = resolvedInfo.serviceName
                
                if (host != null && port > 0) {
                    Napier.i("$TAG: Resolved $name -> $host:$port")
                    
                    // Extract auth token from TXT records if available
                    val authToken = resolvedInfo.attributes["token"]?.toString(Charsets.UTF_8)
                    
                    val server = DiscoveredServer(
                        name = name,
                        host = host,
                        port = port,
                        authToken = authToken,
                        source = DiscoverySource.MDNS
                    )
                    onResolved(server)
                } else {
                    Napier.w("$TAG: Resolved service missing host or port")
                    onResolved(null)
                }
            }
        }
        
        try {
            // resolveService is deprecated in API 34+ but registerServiceInfoCallback requires API 34+
            // Using @Suppress for backward compatibility with older Android versions
            @Suppress("DEPRECATION")
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Napier.e("$TAG: Error resolving service", e)
            onResolved(null)
        }
    }
    
    /**
     * Register a local OpenCode server for discovery by other devices.
     * Call this when starting the OpenCode server to advertise it.
     */
    suspend fun registerService(
        port: Int,
        name: String = "OpenCode Server",
        token: String? = null
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = name
            this.serviceType = SERVICE_TYPE
            this.port = port
            token?.let {
                setAttribute("token", it)
            }
            setAttribute("version", "1.0")
        }
        
        val registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredInfo: NsdServiceInfo) {
                Napier.i("$TAG: Service registered: ${registeredInfo.serviceName}")
                if (continuation.isActive) {
                    continuation.resume(true)
                }
            }
            
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Napier.e("$TAG: Service registration failed: $errorCode")
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
            
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Napier.i("$TAG: Service unregistered: ${serviceInfo.serviceName}")
            }
            
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Napier.e("$TAG: Service unregistration failed: $errorCode")
            }
        }
        
        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Napier.e("$TAG: Error registering service", e)
            if (continuation.isActive) {
                continuation.resume(false)
            }
        }
        
        continuation.invokeOnCancellation {
            try {
                nsdManager.unregisterService(registrationListener)
            } catch (e: Exception) {
                Napier.w("$TAG: Error unregistering service on cancel", e)
            }
        }
    }
    
    /**
     * Check if the device supports multicast (required for mDNS).
     */
    override fun isMulticastSupported(): Boolean {
        // Most modern Android devices support multicast
        // We attempt to acquire the lock and proceed regardless
        return true
    }
    
    private fun acquireMulticastLock() {
        try {
            val lock = wifiManager.createMulticastLock("ServerDiscovery")
            lock.setReferenceCounted(true)
            lock.acquire()
            multicastLock = lock
            Napier.d("$TAG: Multicast lock acquired")
        } catch (e: Exception) {
            Napier.w("$TAG: Could not acquire multicast lock", e)
        }
    }
    
    private fun releaseMulticastLock() {
        try {
            multicastLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    Napier.d("$TAG: Multicast lock released")
                }
                multicastLock = null
            }
        } catch (e: Exception) {
            Napier.w("$TAG: Error releasing multicast lock", e)
        }
    }
    
    /**
     * Get the list of currently discovered servers.
     */
    override fun getDiscoveredServers(): List<DiscoveredServer> {
        return discoveredServers.values.toList()
    }
}
