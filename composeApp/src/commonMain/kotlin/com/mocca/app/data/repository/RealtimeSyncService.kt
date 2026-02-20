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
 * 1. Periodic polling at configurable intervals (DEFAULT: 5 seconds for ALWAYS FRESH)
 * 2. Immediate sync on connection established
 * 3. Immediate sync on app resume/foreground
 * 4. Intelligent backoff when offline
 * 5. Respects network availability
 * 6. Atomic sync with progress tracking via SyncStateManager
 * 
 * Architecture:
 * ```
 * RealtimeSyncService
 *     ├── Periodic Sync Job (every 5s)
 *     ├── Connection Observer (sync on connect)
 *     └── Manual Trigger (syncNow)
 *           ↓
 *     SyncStateManager.startSync()
 *           ↓
 *     For each repository:
 *         - SyncStateManager.updateRepoState(repo, Fetching)
 *         - repository.refresh()
 *         - SyncStateManager.updateRepoState(repo, Fresh/Failed)
 *           ↓
 *     SyncStateManager.markSyncComplete/Failed()
 *           ↓
 *     StateFlows emit new data
 *           ↓
 *     UI updates automatically
 * ```
 */
class RealtimeSyncService(
    private val stateCoordinator: StateCoordinator,
    private val connectionManager: ConnectionManager,
    private val syncStateManager: SyncStateManager,
    private val mcpRepository: McpRepository,
    private val gitRepository: GitRepository,
    private val toolRepository: ToolRepository,
    private val agentRepository: AgentRepository,
    private val commandRepository: CommandRepository,
    private val providerRepository: ProviderRepository
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
        val staleThreshold = syncIntervalMs / 2 // 2.5 seconds for 5s interval
        
        if (lastSync == null || (now - lastSync) > staleThreshold) {
            Napier.i("[RealtimeSync] App foreground - data stale, syncing")
            syncNow("foreground")
        } else {
            Napier.v("[RealtimeSync] App foreground - data fresh, skipping sync")
        }
    }
    
    /**
     * Force a full sync of all data.
     * Use when user explicitly requests refresh or on installation updates.
     */
    fun forceFullSync() {
        serviceScope.launch {
            // Invalidate all states first
            syncStateManager.invalidateAll()
            // Then perform full sync
            performSync("forced")
        }
    }
    
    /**
     * Sync specific repositories (e.g., after installation update).
     */
    fun syncRepos(repoNames: Set<String>) {
        serviceScope.launch {
            syncStateManager.invalidateRepos(repoNames)
            performSync("partial-${repoNames.joinToString()}")
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
        Napier.i("[RealtimeSync] Starting atomic sync (reason=$reason)")
        
        val repoNames = listOf("sessions", "git", "mcp", "tools", "agents", "commands", "providers")
        val errors = mutableMapOf<String, String>()
        
        try {
            // Start progress tracking
            syncStateManager.startSync(repoNames.size)
            
            // Sync each repository atomically
            repoNames.forEachIndexed { index, repoName ->
                try {
                    syncStateManager.updateSyncProgress(index, repoNames.size, repoName)
                    syncRepository(repoName)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.w("[RealtimeSync] $repoName sync failed", e)
                    errors[repoName] = e.message ?: "Unknown error"
                }
            }
            
            // Mark completion
            if (errors.isEmpty()) {
                syncStateManager.markSyncComplete()
                Napier.i("[RealtimeSync] Atomic sync completed successfully")
            } else {
                syncStateManager.markSyncFailed(errors)
                Napier.w("[RealtimeSync] Atomic sync completed with errors: ${errors.keys}")
            }
            
            _lastSyncTime.value = Clock.System.now().toEpochMilliseconds()
            _syncCount.value += 1
            
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("[RealtimeSync] Atomic sync failed critically", e)
            syncStateManager.markSyncFailed(mapOf("global" to (e.message ?: "Critical failure")))
        } finally {
            _isSyncing.value = false
        }
    }
    
    /**
     * Sync a specific repository by name.
     */
    private suspend fun syncRepository(repoName: String) {
        when (repoName) {
            "sessions" -> syncSessions()
            "git" -> syncGit()
            "mcp" -> syncMcp()
            "tools" -> syncTools()
            "agents" -> syncAgents()
            "commands" -> syncCommands()
            "providers" -> syncProviders()
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // Per-Repository Sync Functions with SyncStateManager Integration
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private suspend fun syncSessions() {
        syncStateManager.updateRepoState("sessions", com.mocca.app.domain.model.SyncState.Fetching)
        try {
            stateCoordinator.syncFromServer()
            syncStateManager.updateRepoState("sessions", com.mocca.app.domain.model.SyncState.Fresh())
            Napier.v("[RealtimeSync] Sessions synced")
        } catch (e: Exception) {
            syncStateManager.updateRepoState("sessions", com.mocca.app.domain.model.SyncState.Failed(e.message ?: "Unknown error"))
            throw e
        }
    }
    
    private suspend fun syncGit() {
        syncStateManager.updateRepoState("git", com.mocca.app.domain.model.SyncState.Fetching)
        try {
            gitRepository.refresh()
            syncStateManager.updateRepoState("git", com.mocca.app.domain.model.SyncState.Fresh())
            Napier.v("[RealtimeSync] Git synced")
        } catch (e: Exception) {
            syncStateManager.updateRepoState("git", com.mocca.app.domain.model.SyncState.Failed(e.message ?: "Unknown error"))
            throw e
        }
    }
    
    private suspend fun syncMcp() {
        syncStateManager.updateRepoState("mcp", com.mocca.app.domain.model.SyncState.Fetching)
        try {
            mcpRepository.refresh()
            syncStateManager.updateRepoState("mcp", com.mocca.app.domain.model.SyncState.Fresh())
            Napier.v("[RealtimeSync] MCP synced")
        } catch (e: Exception) {
            syncStateManager.updateRepoState("mcp", com.mocca.app.domain.model.SyncState.Failed(e.message ?: "Unknown error"))
            throw e
        }
    }
    
    private suspend fun syncTools() {
        syncStateManager.updateRepoState("tools", com.mocca.app.domain.model.SyncState.Fetching)
        try {
            toolRepository.refreshCache()
            syncStateManager.updateRepoState("tools", com.mocca.app.domain.model.SyncState.Fresh())
            Napier.v("[RealtimeSync] Tools synced")
        } catch (e: Exception) {
            syncStateManager.updateRepoState("tools", com.mocca.app.domain.model.SyncState.Failed(e.message ?: "Unknown error"))
            throw e
        }
    }
    
    private suspend fun syncAgents() {
        syncStateManager.updateRepoState("agents", com.mocca.app.domain.model.SyncState.Fetching)
        try {
            agentRepository.refresh()
            syncStateManager.updateRepoState("agents", com.mocca.app.domain.model.SyncState.Fresh())
            Napier.v("[RealtimeSync] Agents synced")
        } catch (e: Exception) {
            syncStateManager.updateRepoState("agents", com.mocca.app.domain.model.SyncState.Failed(e.message ?: "Unknown error"))
            throw e
        }
    }
    
    private suspend fun syncCommands() {
        syncStateManager.updateRepoState("commands", com.mocca.app.domain.model.SyncState.Fetching)
        try {
            commandRepository.refresh()
            syncStateManager.updateRepoState("commands", com.mocca.app.domain.model.SyncState.Fresh())
            Napier.v("[RealtimeSync] Commands synced")
        } catch (e: Exception) {
            syncStateManager.updateRepoState("commands", com.mocca.app.domain.model.SyncState.Failed(e.message ?: "Unknown error"))
            throw e
        }
    }
    
    private suspend fun syncProviders() {
        syncStateManager.updateRepoState("providers", com.mocca.app.domain.model.SyncState.Fetching)
        try {
            providerRepository.refresh()
            syncStateManager.updateRepoState("providers", com.mocca.app.domain.model.SyncState.Fresh())
            Napier.v("[RealtimeSync] Providers synced")
        } catch (e: Exception) {
            syncStateManager.updateRepoState("providers", com.mocca.app.domain.model.SyncState.Failed(e.message ?: "Unknown error"))
            throw e
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
         * 
         * CRITICAL: Changed from 30s to 5s for "ALWAYS FRESH" requirement.
         * The user's #1 priority is that data is ALWAYS synchronized with the server.
         * Battery/performance is secondary to data freshness.
         */
        private const val SYNC_INTERVAL_MS = 5_000L
        
        /**
         * Minimum interval between syncs (prevents spam).
         */
        const val MIN_SYNC_INTERVAL_MS = 1_000L
    }
}
