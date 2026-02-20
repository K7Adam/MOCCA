package com.mocca.app.data.repository

import com.mocca.app.domain.model.GlobalSyncState
import com.mocca.app.domain.model.SyncState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Central manager for tracking sync state across all repositories.
 * 
 * Responsibilities:
 * - Aggregate per-repository SyncState into GlobalSyncState
 * - Provide reactive state for UI consumption
 * - Track sync timing and health metrics
 * 
 * Architecture:
 * ```
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                     SyncStateManager                            │
 * ├─────────────────────────────────────────────────────────────────┤
 * │                                                                 │
 * │  GitRepository ─────┐                                          │
 * │  McpRepository ─────┤                                          │
 * │  ToolRepository ────┼──▶ updateRepoState() ──▶ repoStates      │
 * │  AgentRepository ───┤                           │              │
 * │  SessionRepository ─┘                           ▼              │
 * │                                          recalculateGlobalState│
 * │                                                 │              │
 * │                                                 ▼              │
 * │                                         globalState            │
 * │                                                 │              │
 * │                                                 ▼              │
 * │                                              UI                │
 * │                                                                 │
 * └─────────────────────────────────────────────────────────────────┘
 * ```
 */
class SyncStateManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateMutex = Mutex()
    
    // Per-repository sync states
    private val _repoStates = MutableStateFlow<Map<String, SyncState>>(emptyMap())
    val repoStates: StateFlow<Map<String, SyncState>> = _repoStates.asStateFlow()
    
    // Aggregated global state
    private val _globalState = MutableStateFlow<GlobalSyncState>(GlobalSyncState.NotSynced)
    val globalState: StateFlow<GlobalSyncState> = _globalState.asStateFlow()
    
    // Known repositories to track
    private val knownRepos = setOf(
        "sessions", "git", "mcp", "tools", "agents", 
        "commands", "providers", "config"
    )
    
    /**
     * Update sync state for a specific repository.
     * Thread-safe - can be called from any coroutine.
     */
    suspend fun updateRepoState(repoName: String, state: SyncState) {
        stateMutex.withLock {
            val current = _repoStates.value.toMutableMap()
            current[repoName] = state
            _repoStates.value = current
            recalculateGlobalState()
        }
        Napier.v("[SyncStateManager] $repoName -> ${state::class.simpleName}")
    }
    
    /**
     * Non-suspending version for use in flows.
     */
    fun updateRepoStateSync(repoName: String, state: SyncState) {
        val current = _repoStates.value.toMutableMap()
        current[repoName] = state
        _repoStates.value = current
        recalculateGlobalStateInternal()
        Napier.v("[SyncStateManager] $repoName -> ${state::class.simpleName}")
    }
    
    /**
     * Get current state for a specific repository.
     */
    fun getRepoState(repoName: String): SyncState {
        return _repoStates.value[repoName] ?: SyncState.Idle
    }
    
    /**
     * Mark all repositories as needing sync (e.g., on installation update).
     */
    suspend fun invalidateAll() {
        stateMutex.withLock {
            val current = _repoStates.value.toMutableMap()
            knownRepos.forEach { repo ->
                current[repo] = SyncState.Idle
            }
            _repoStates.value = current
            recalculateGlobalState()
        }
        Napier.i("[SyncStateManager] All repositories invalidated")
    }
    
    /**
     * Mark specific repositories as needing sync.
     */
    suspend fun invalidateRepos(repoNames: Set<String>) {
        stateMutex.withLock {
            val current = _repoStates.value.toMutableMap()
            repoNames.forEach { repo ->
                current[repo] = SyncState.Idle
            }
            _repoStates.value = current
            recalculateGlobalState()
        }
        Napier.i("[SyncStateManager] Invalidated: $repoNames")
    }
    
    /**
     * Start syncing indicator for progress tracking.
     */
    fun startSync(totalRepos: Int) {
        _globalState.value = GlobalSyncState.Syncing(
            progress = 0f,
            totalRepos = totalRepos,
            completedRepos = 0
        )
    }
    
    /**
     * Update sync progress.
     */
    fun updateSyncProgress(completedRepos: Int, totalRepos: Int, currentRepo: String?) {
        val progress = if (totalRepos > 0) completedRepos.toFloat() / totalRepos else 0f
        _globalState.value = GlobalSyncState.Syncing(
            progress = progress,
            currentRepo = currentRepo,
            totalRepos = totalRepos,
            completedRepos = completedRepos
        )
    }
    
    /**
     * Mark sync as completed with all repos fresh.
     */
    fun markSyncComplete() {
        val now = Clock.System.now().toEpochMilliseconds()
        _globalState.value = GlobalSyncState.Fresh(
            lastSyncMs = now,
            repoStates = _repoStates.value
        )
    }
    
    /**
     * Mark sync as failed with errors.
     */
    fun markSyncFailed(errors: Map<String, String>) {
        val lastFreshTimes = _repoStates.value.values
            .filterIsInstance<SyncState.Fresh>()
            .map { it.timestamp }
        
        _globalState.value = GlobalSyncState.Failed(
            errors = errors,
            lastKnownGoodMs = lastFreshTimes.maxOrNull()
        )
    }
    
    /**
     * Recalculate GlobalSyncState from repository states.
     * Must be called with stateMutex held.
     */
    private fun recalculateGlobalState() {
        val states = _repoStates.value
        recalculateFromStates(states)
    }
    
    /**
     * Internal recalculation without mutex (for non-suspending context).
     */
    private fun recalculateGlobalStateInternal() {
        val states = _repoStates.value
        recalculateFromStates(states)
    }
    
    private fun recalculateFromStates(states: Map<String, SyncState>) {
        // If we don't have any states yet, we're not synced
        if (states.isEmpty()) {
            _globalState.value = GlobalSyncState.NotSynced
            return
        }
        
        val freshRepos = states.filter { it.value.isFresh }.keys
        val failedRepos = states.filter { it.value is SyncState.Failed }
            .mapValues { (it.value as SyncState.Failed).error }
        val staleRepos = states.filter { it.value.isStale && it.value !is SyncState.Failed }.keys
        
        // Calculate last sync time from fresh repos
        val lastFreshTimes = states.values
            .filterIsInstance<SyncState.Fresh>()
            .map { it.timestamp }
        val lastSyncMs = lastFreshTimes.maxOrNull() ?: 0L
        
        when {
            // All repos are fresh
            freshRepos.containsAll(knownRepos) -> {
                _globalState.value = GlobalSyncState.Fresh(
                    lastSyncMs = lastSyncMs,
                    repoStates = states
                )
            }
            // Some repos failed critically
            failedRepos.isNotEmpty() && freshRepos.isEmpty() -> {
                _globalState.value = GlobalSyncState.Failed(
                    errors = failedRepos,
                    lastKnownGoodMs = lastFreshTimes.maxOrNull()
                )
            }
            // Mixed state - some fresh, some stale
            freshRepos.isNotEmpty() -> {
                _globalState.value = GlobalSyncState.Partial(
                    freshRepos = freshRepos,
                    staleRepos = staleRepos,
                    failedRepos = failedRepos,
                    lastFullSyncMs = lastSyncMs
                )
            }
            // Nothing fresh yet
            else -> {
                _globalState.value = GlobalSyncState.NotSynced
            }
        }
    }
    
    /**
     * Get a human-readable summary for UI display.
     */
    fun getSummary(): String {
        return when (val state = _globalState.value) {
            is GlobalSyncState.NotSynced -> "Not synced"
            is GlobalSyncState.Syncing -> {
                if (state.currentRepo != null) {
                    "Syncing ${state.currentRepo}..."
                } else {
                    "Syncing..."
                }
            }
            is GlobalSyncState.Fresh -> {
                val age = Clock.System.now().toEpochMilliseconds() - state.lastSyncMs
                when {
                    age < 5000 -> "Synced"
                    age < 60000 -> "Synced ${age / 1000}s ago"
                    else -> "Synced ${age / 60000}m ago"
                }
            }
            is GlobalSyncState.Partial -> {
                val total = state.freshRepos.size + state.staleRepos.size + state.failedRepos.size
                "${state.freshRepos.size}/$total synced"
            }
            is GlobalSyncState.Failed -> "Sync failed"
        }
    }
    
    /**
     * Check if all critical repositories are fresh.
     * Critical repos: sessions, git, mcp
     */
    fun areCriticalReposFresh(): Boolean {
        val criticalRepos = setOf("sessions", "git", "mcp")
        val states = _repoStates.value
        return criticalRepos.all { repo -> 
            states[repo]?.isFresh == true 
        }
    }
    
    /**
     * Reset all state (e.g., on disconnect).
     */
    fun reset() {
        _repoStates.value = emptyMap()
        _globalState.value = GlobalSyncState.NotSynced
        Napier.i("[SyncStateManager] State reset")
    }
}
