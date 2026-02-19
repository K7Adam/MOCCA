package com.mocca.app.data.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

/**
 * Service for periodic synchronization of data that doesn't have SSE events.
 * 
 * The OpenCode server sends SSE events for session/message changes but NOT for:
 * - MCP server status
 * - Provider/model availability
 * - Agent configurations
 * - Tools/commands
 * - Git status
 * 
 * This service provides:
 * 1. Periodic polling at configurable intervals
 * 2. Immediate sync on connection established
 * 3. Immediate sync on app resume/foreground
 * 4. Intelligent backoff when offline
 * 5. Respects network availability
 * 
 * Architecture:
 * ```
 * RealtimeSyncService
 *     ├── Periodic Sync Job (every 30s)
 *     ├── Connection Observer (sync on connect)
 *     └── Manual Trigger (syncNow)
 *           ↓
 *     StateCoordinator.syncFromServer()
 *           ↓
 *     All repositories refresh
 *           ↓
 *     StateFlows emit new data
 *           ↓
 *     UI updates automatically
 * ```
 */
class RealtimeSyncService(
    private val stateCoordinator: StateCoordinator,
    private val connectionManager: ConnectionManager,
    private val mcpRepository: McpRepository,
    private val gitRepository: GitRepository
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Sync configuration
    private val syncIntervalMs = SYNC_INTERVAL_MS
    private var periodicSyncJob: Job? = null
    private var connectionObserverJob: Job? = null
    
    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()
    
    private val _syncCount = MutableStateFlow(0)
    val syncCount: StateFlow<Int> = _syncCount.asStateFlow()
    
    // Track if service is started
    private var isStarted = false
    
    /**
     * Start the realtime sync service.
     * Should be called when app connects to server.
     */
    fun start() {
        if (isStarted) {
            Napier.v("[RealtimeSync] Already started")
            return
        }
        isStarted = true
        
        Napier.i("[RealtimeSync] Starting realtime sync service")
        
        // Start periodic sync
        startPeriodicSync()
        
        // Observe connection state for immediate sync
        startConnectionObserver()
        
        // Do initial sync if connected
        if (connectionManager.status.value.isConnected) {
            syncNow("initial")
        }
    }
    
    /**
     * Stop the realtime sync service.
     * Should be called when app disconnects.
     */
    fun stop() {
        Napier.i("[RealtimeSync] Stopping realtime sync service")
        isStarted = false
        periodicSyncJob?.cancel()
        periodicSyncJob = null
        connectionObserverJob?.cancel()
        connectionObserverJob = null
    }
    
    /**
     * Trigger an immediate sync.
     * @param reason Why the sync is being triggered (for logging)
     */
    fun syncNow(reason: String = "manual") {
        serviceScope.launch {
            performSync(reason)
        }
    }
    
    /**
     * Called when app comes to foreground.
     * Triggers sync if data is stale.
     */
    fun onAppForeground() {
        val lastSync = _lastSyncTime.value
        val now = Clock.System.now().toEpochMilliseconds()
        val staleThreshold = syncIntervalMs / 2 // 15 seconds
        
        if (lastSync == null || (now - lastSync) > staleThreshold) {
            Napier.i("[RealtimeSync] App foreground - data stale, syncing")
            syncNow("foreground")
        } else {
            Napier.v("[RealtimeSync] App foreground - data fresh, skipping sync")
        }
    }
    
    private fun startPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = serviceScope.launch {
            Napier.i("[RealtimeSync] Starting periodic sync (interval=${syncIntervalMs}ms)")
            
            while (isActive) {
                delay(syncIntervalMs)
                
                // Only sync if connected
                if (connectionManager.status.value.isConnected && !_isSyncing.value) {
                    performSync("periodic")
                }
            }
        }
    }
    
    private fun startConnectionObserver() {
        connectionObserverJob?.cancel()
        connectionObserverJob = serviceScope.launch {
            connectionManager.status.collect { status ->
                if (status.isConnected) {
                    Napier.i("[RealtimeSync] Connection established - triggering sync")
                    delay(500) // Brief delay to let connection stabilize
                    performSync("connection")
                }
            }
        }
    }
    
    private suspend fun performSync(reason: String) {
        if (_isSyncing.value) {
            Napier.v("[RealtimeSync] Already syncing, skipping ($reason)")
            return
        }
        
        _isSyncing.value = true
        Napier.i("[RealtimeSync] Starting sync (reason=$reason)")
        
        try {
            // Use coroutineScope to ensure all launches complete
            coroutineScope {
                // Sync sessions via StateCoordinator (which handles session repo)
                launch {
                    try {
                        stateCoordinator.syncFromServer()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Napier.w("[RealtimeSync] Session sync failed", e)
                    }
                }
                
                // Sync MCP servers
                launch {
                    try {
                        mcpRepository.refresh()
                        Napier.v("[RealtimeSync] MCP servers synced")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Napier.w("[RealtimeSync] MCP sync failed", e)
                    }
                }
                
                // Sync Git status
                launch {
                    try {
                        gitRepository.getVcsInfo().first()
                        Napier.v("[RealtimeSync] Git status synced")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Napier.w("[RealtimeSync] Git sync failed", e)
                    }
                }
            }
            
            _lastSyncTime.value = Clock.System.now().toEpochMilliseconds()
            _syncCount.value += 1
            Napier.i("[RealtimeSync] Sync completed (count=${_syncCount.value})")
            
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("[RealtimeSync] Sync failed", e)
        } finally {
            _isSyncing.value = false
        }
    }
    
    fun dispose() {
        stop()
        serviceScope.cancel()
        Napier.i("[RealtimeSync] Disposed")
    }
    
    companion object {
        /**
         * Interval for periodic sync.
         * 30 seconds is a good balance between freshness and server load.
         */
        private const val SYNC_INTERVAL_MS = 30_000L
        
        /**
         * Minimum interval between syncs (prevents spam).
         */
        const val MIN_SYNC_INTERVAL_MS = 5_000L
    }
}
