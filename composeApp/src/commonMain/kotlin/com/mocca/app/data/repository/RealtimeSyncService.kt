package com.mocca.app.data.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import com.mocca.app.domain.model.*
import com.mocca.app.api.*
import com.mocca.app.data.local.*

/**
 * Service for SILENT background synchronization of data that doesn't have SSE events.
 * 
 * CRITICAL UX PRINCIPLE: Background sync is COMPLETELY SILENT.
 * The user should NEVER see loading animations or UI flicker during automatic sync.
 * Only manual/user-triggered refreshes should show UI feedback.
 * 
 * The OpenCode server sends SSE events for session/message changes but NOT for:
 * - MCP server status
 * - Provider/model availability
 * - Agent configurations
 * - Tools/commands
 * - Git status
 * 
 * This service provides:
 * 1. Silent periodic polling at reasonable intervals (30s)
 * 2. Silent sync on connection established
 * 3. Silent sync on app resume/foreground
 * 4. Manual sync with UI feedback (user-triggered only)
 * 
 * Architecture:
 * ```
 * RealtimeSyncService
 *     ├── Periodic Sync Job (silent background)
 *     ├── Connection Observer (silent)
 *     └── Manual Trigger (shows UI feedback)
 *           ↓
 *     Repositories update silently
 *           ↓
 *     StateFlows emit ONLY if data changed
 *           ↓
 *     UI updates only when data actually differs
 * ```
 * 
 * KEY DIFFERENCE FROM PREVIOUS VERSION:
 * - NO SyncStateManager emissions during automatic sync
 * - NO progress tracking for background operations
 * - NO "Syncing..." indicators during periodic refresh
 * - Only manual sync shows UI state changes
 * - Sessions removed from polling (SSE handles them)
 */
class RealtimeSyncService(
    private val stateCoordinator: StateCoordinator,
    private val connectionManager: ConnectionManager,
    private val mcpRepository: McpRepository,
    private val gitRepository: GitRepository,
    private val toolRepository: ToolRepository,
    private val agentRepository: AgentRepository,
    private val commandRepository: CommandRepository,
    private val providerRepository: ProviderRepository
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Sync configuration - 30 seconds is reasonable for non-SSE data
    private val syncIntervalMs = SYNC_INTERVAL_MS
    private var connectionObserverJob: Job? = null
    private var sessionObserverJob: Job? = null
    
    // Internal sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private var lastGitSyncMs: Long? = null
    
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
        
        Napier.i("[RealtimeSync] Starting silent background sync service")
        
        // Observe connection state for immediate sync (SILENT)
        startConnectionObserver()
        
        // Do initial sync if connected (SILENT)
        if (connectionManager.status.value.isConnected) {
            serviceScope.launch {
                delay(CONNECTION_SYNC_START_DELAY_MS)
                performSilentSync("initial")
            }
        }
    }
    
    /**
     * Stop the realtime sync service.
     * Should be called when app disconnects.
     */
    fun stop() {
        Napier.i("[RealtimeSync] Stopping sync service")
        isStarted = false
        connectionObserverJob?.cancel()
        connectionObserverJob = null
        sessionObserverJob?.cancel()
        sessionObserverJob = null
    }
    
    /**
     * Trigger a MANUAL sync with UI feedback.
     * This is the ONLY sync that should show UI indicators.
     * Use when user explicitly requests refresh.
     */
    fun syncNow(reason: String = "manual") {
        serviceScope.launch {
            performManualSync(reason)
        }
    }
    
    /**
     * Called when app comes to foreground.
     * Triggers SILENT sync if data is stale.
     */
    fun onAppForeground() {
        val lastSync = _lastSyncTime.value
        val now = Clock.System.now().toEpochMilliseconds()
        val staleThreshold = syncIntervalMs * 2 // Double interval for foreground
        
        if (lastSync == null || (now - lastSync) > staleThreshold) {
            Napier.i("[RealtimeSync] App foreground - data stale, silent sync")
            serviceScope.launch {
                performSilentSync("foreground")
            }
        } else {
            Napier.v("[RealtimeSync] App foreground - data fresh, skipping")
        }
    }
    
    /**
     * Force a full sync of all data.
     * This is a MANUAL sync with UI feedback.
     */
    fun forceFullSync() {
        serviceScope.launch {
            performManualSync("forced")
        }
    }
    
    /**
     * Sync specific repositories (e.g., after installation update).
     * This is SILENT - triggered by SSE events.
     */
    fun syncRepos(repoNames: Set<String>) {
        serviceScope.launch {
            performSilentSync("partial-${repoNames.joinToString()}", repoNames)
        }
    }
    
    private fun startConnectionObserver() {
        connectionObserverJob?.cancel()
        connectionObserverJob = serviceScope.launch {
            connectionManager.status
                .map { it.isConnected }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    delay(CONNECTION_SYNC_START_DELAY_MS)
                    Napier.i("[RealtimeSync] Connection established - silent sync")
                    performSilentSync("connection")
                }
        }
        
        sessionObserverJob?.cancel()
        sessionObserverJob = serviceScope.launch {
            stateCoordinator.activeSessionId
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { sessionId ->
                    Napier.i("[RealtimeSync] Active session ID received ($sessionId) - deferred silent Git sync")
                    delay(ACTIVE_SESSION_GIT_SYNC_DELAY_MS)
                    try {
                        syncGitSilent()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Napier.w("[RealtimeSync] Deferred Git sync failed: ${e.message}")
                    }
                }
        }
    }
    
    /**
     * SILENT sync - no UI emissions, just update data.
     * This is for automatic background sync.
     */
    private suspend fun performSilentSync(reason: String, specificRepos: Set<String>? = null) {
        if (_isSyncing.value) {
            Napier.v("[RealtimeSync] Already syncing, skipping ($reason)")
            return
        }
        val now = Clock.System.now().toEpochMilliseconds()
        val lastSync = _lastSyncTime.value
        if (lastSync != null && now - lastSync < MIN_SYNC_INTERVAL_MS) {
            Napier.v("[RealtimeSync] Recent sync exists, skipping ($reason)")
            return
        }
        
        _isSyncing.value = true
        Napier.v("[RealtimeSync] Silent sync started (reason=$reason)")
        
        try {
            // Determine which repos to sync
            // NOTE: Sessions are handled by SSE, NOT by polling
            val repoNames = specificRepos ?: listOf(
                // "sessions" removed - handled by SSE (SessionUpdated, MessageUpdated events)
                // "git" is synced after an active session settles because it requires a live CLI bridge.
                "mcp", "tools", "agents", "commands", "providers"
            )
            
            // Sync each repository silently
            // NO SyncStateManager emissions - completely silent
            repoNames.forEach { repoName ->
                try {
                    syncRepositorySilent(repoName)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.w("[RealtimeSync] $repoName silent sync failed: ${e.message}")
                }
            }
            
            _lastSyncTime.value = Clock.System.now().toEpochMilliseconds()
            Napier.v("[RealtimeSync] Silent sync completed")
            
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("[RealtimeSync] Silent sync failed", e)
        } finally {
            _isSyncing.value = false
        }
    }
    
    /**
     * MANUAL sync - with UI feedback via SyncStateManager.
     * This is for user-triggered refreshes only.
     */
    private suspend fun performManualSync(reason: String) {
        if (_isSyncing.value) {
            Napier.v("[RealtimeSync] Already syncing, skipping manual ($reason)")
            return
        }
        
        _isSyncing.value = true
        Napier.i("[RealtimeSync] Manual sync started (reason=$reason)")
        
        // For manual sync, include sessions too
        val repoNames = listOf("sessions", "git", "mcp", "tools", "agents", "commands", "providers")
        val errors = mutableMapOf<String, String>()
        
        try {
            // Only show UI state for MANUAL sync
            // Removed syncStateManager calls
            
            repoNames.forEachIndexed { index, repoName ->
                try {
                    syncRepositorySilent(repoName)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.w("[RealtimeSync] $repoName manual sync failed", e)
                    errors[repoName] = e.message ?: "Unknown error"
                }
            }
            
            _lastSyncTime.value = Clock.System.now().toEpochMilliseconds()
            Napier.i("[RealtimeSync] Manual sync completed")
            
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("[RealtimeSync] Manual sync failed", e)
        } finally {
            _isSyncing.value = false
        }
    }
    
    /**
     * Sync a specific repository silently (no UI state emissions).
     */
    private suspend fun syncRepositorySilent(repoName: String) {
        when (repoName) {
            "sessions" -> syncSessionsSilent()
            "git" -> syncGitSilent()
            "mcp" -> syncMcpSilent()
            "tools" -> syncToolsSilent()
            "agents" -> syncAgentsSilent()
            "commands" -> syncCommandsSilent()
            "providers" -> syncProvidersSilent()
        }
    }

    // Silent Per-Repository Sync Functions
    // These update data WITHOUT emitting sync state to UI

    
    private suspend fun syncSessionsSilent() {
        try {
            stateCoordinator.syncFromServer()
            Napier.v("[RealtimeSync] Sessions synced (silent)")
        } catch (e: Exception) {
            Napier.w("[RealtimeSync] Sessions sync failed: ${e.message}")
            throw e
        }
    }
    
    private suspend fun syncGitSilent() {
        val now = Clock.System.now().toEpochMilliseconds()
        val lastGitSync = lastGitSyncMs
        if (lastGitSync != null && now - lastGitSync < MIN_GIT_SYNC_INTERVAL_MS) {
            Napier.v("[RealtimeSync] Recent Git sync exists, skipping")
            return
        }
        lastGitSyncMs = now

        try {
            gitRepository.refresh()
            
            // Also fetch full status if we have an active session
            stateCoordinator.activeSessionId.value?.let { activeSessionId ->
                try {
                    gitRepository.getStatus(activeSessionId)
                        .filter { it !is com.mocca.app.domain.model.Resource.Loading }
                        .firstOrNull()
                } catch (e: Exception) {
                    Napier.w("[RealtimeSync] Full Git status sync failed: ${e.message}")
                }
            }
            
            Napier.v("[RealtimeSync] Git synced (silent)")
        } catch (e: Exception) {
            Napier.w("[RealtimeSync] Git sync failed: ${e.message}")
            throw e
        }
    }
    
    private suspend fun syncMcpSilent() {
        try {
            mcpRepository.refresh()
            Napier.v("[RealtimeSync] MCP synced (silent)")
        } catch (e: Exception) {
            Napier.w("[RealtimeSync] MCP sync failed: ${e.message}")
            throw e
        }
    }
    
    private suspend fun syncToolsSilent() {
        try {
            toolRepository.refreshCache()
            Napier.v("[RealtimeSync] Tools synced (silent)")
        } catch (e: Exception) {
            Napier.w("[RealtimeSync] Tools sync failed: ${e.message}")
            throw e
        }
    }
    
    private suspend fun syncAgentsSilent() {
        try {
            agentRepository.refresh()
            Napier.v("[RealtimeSync] Agents synced (silent)")
        } catch (e: Exception) {
            Napier.w("[RealtimeSync] Agents sync failed: ${e.message}")
            throw e
        }
    }
    
    private suspend fun syncCommandsSilent() {
        try {
            commandRepository.refresh()
            Napier.v("[RealtimeSync] Commands synced (silent)")
        } catch (e: Exception) {
            Napier.w("[RealtimeSync] Commands sync failed: ${e.message}")
            throw e
        }
    }
    
    private suspend fun syncProvidersSilent() {
        try {
            providerRepository.refresh()
            Napier.v("[RealtimeSync] Providers synced (silent)")
        } catch (e: Exception) {
            Napier.w("[RealtimeSync] Providers sync failed: ${e.message}")
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
         * Interval for periodic silent sync.
         * 
         * 30 seconds is a reasonable balance between freshness and efficiency.
         * Sessions/messages are handled by SSE (real-time), so this only syncs
         * MCP, Git, Tools, Agents, Commands, Providers.
         * 
         * These data types change rarely compared to session messages,
         * so 30s is perfectly adequate for "always fresh" without UI churn.
         */
        private const val SYNC_INTERVAL_MS = 30_000L
        
        /**
         * Minimum interval between syncs (prevents spam).
         */
        const val MIN_SYNC_INTERVAL_MS = 5_000L

        private const val MIN_GIT_SYNC_INTERVAL_MS = 10_000L
        private const val CONNECTION_SYNC_START_DELAY_MS = 15_000L
        private const val ACTIVE_SESSION_GIT_SYNC_DELAY_MS = 15_000L
    }
}
