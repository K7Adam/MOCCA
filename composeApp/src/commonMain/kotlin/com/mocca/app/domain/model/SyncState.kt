package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable
import kotlin.time.Clock

/**
 * Per-repository sync state.
 * Tracks the freshness and health of individual data sources.
 */
sealed class SyncState {
    /** No sync has been attempted yet */
    data object Idle : SyncState()
    
    /** Currently fetching data from server */
    data object Fetching : SyncState()
    
    /** Data is fresh and verified */
    @Immutable
    data class Fresh(
        val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
        val version: String? = null
    ) : SyncState()
    
    /** Sync failed, may have stale data */
    @Immutable
    data class Failed(
        val error: String,
        val lastFreshTimestamp: Long? = null,
        val retryCount: Int = 0
    ) : SyncState()
    
    val isFresh: Boolean get() = this is Fresh
    val isStale: Boolean get() = this is Failed || this is Idle
    val isFetching: Boolean get() = this is Fetching
    
    /** Age of data in milliseconds, null if not fresh */
    fun ageMs(): Long? = when (this) {
        is Fresh -> Clock.System.now().toEpochMilliseconds() - timestamp
        is Failed -> lastFreshTimestamp?.let { Clock.System.now().toEpochMilliseconds() - it }
        else -> null
    }
}

/**
 * Global application sync state.
 * Aggregates all repository states into a single visible state.
 */
sealed class GlobalSyncState {
    /** Never synced with server */
    data object NotSynced : GlobalSyncState()
    
    /** Currently syncing one or more repositories */
    @Immutable
    data class Syncing(
        val progress: Float,
        val currentRepo: String? = null,
        val totalRepos: Int = 0,
        val completedRepos: Int = 0
    ) : GlobalSyncState()
    
    /** All data is fresh and verified */
    @Immutable
    data class Fresh(
        val lastSyncMs: Long,
        val repoStates: Map<String, SyncState> = emptyMap()
    ) : GlobalSyncState()
    
    /** Some repositories are stale, but app is usable */
    @Immutable
    data class Partial(
        val freshRepos: Set<String>,
        val staleRepos: Set<String>,
        val failedRepos: Map<String, String>,
        val lastFullSyncMs: Long
    ) : GlobalSyncState()
    
    /** Critical failure, cannot sync */
    @Immutable
    data class Failed(
        val errors: Map<String, String>,
        val lastKnownGoodMs: Long? = null
    ) : GlobalSyncState()
    
    val isUsable: Boolean get() = this is Fresh || this is Partial
    val needsSync: Boolean get() = this is NotSynced || this is Failed || 
        (this is Partial && staleRepos.isNotEmpty())
}

/**
 * Configuration for sync behavior per data type.
 */
@Immutable
data class SyncConfig(
    val intervalMs: Long,
    val onSSEEvent: Boolean,
    val onForeground: Boolean,
    val onConnection: Boolean,
    val verifyFreshness: Boolean,
    val maxRetries: Int = 3
)

/**
 * Sync configurations for each data type.
 * 
 * CRITICAL DATA (5s interval):
 * - Sessions: Core app functionality, must always be fresh
 * - Git: VCS status changes frequently during development
 * - MCP: Server status affects tool availability
 * 
 * CONFIGURATION DATA (30s interval):
 * - Tools, Agents, Commands: Change rarely, usually on installation updates
 * 
 * PROVIDER DATA (60s interval):
 * - Providers, Models: Very rarely change during session
 */
object SyncConfigs {
    /**
     * Sessions - most critical, changes constantly
     */
    val SESSIONS = SyncConfig(
        intervalMs = 5_000L,
        onSSEEvent = true,
        onForeground = true,
        onConnection = true,
        verifyFreshness = true
    )
    
    /**
     * Git/VCS - changes during development work
     */
    val GIT = SyncConfig(
        intervalMs = 5_000L,
        onSSEEvent = true,
        onForeground = true,
        onConnection = true,
        verifyFreshness = true
    )
    
    /**
     * MCP Servers - status affects tool availability
     */
    val MCP = SyncConfig(
        intervalMs = 10_000L,
        onSSEEvent = true,
        onForeground = true,
        onConnection = true,
        verifyFreshness = true
    )
    
    /**
     * Tools - changes on plugin installation
     */
    val TOOLS = SyncConfig(
        intervalMs = 30_000L,
        onSSEEvent = false,
        onForeground = false,
        onConnection = true,
        verifyFreshness = false
    )
    
    /**
     * Agents - changes on configuration update
     */
    val AGENTS = SyncConfig(
        intervalMs = 30_000L,
        onSSEEvent = false,
        onForeground = false,
        onConnection = true,
        verifyFreshness = false
    )
    
    /**
     * Commands - changes on plugin installation
     */
    val COMMANDS = SyncConfig(
        intervalMs = 30_000L,
        onSSEEvent = false,
        onForeground = false,
        onConnection = true,
        verifyFreshness = false
    )
    
    /**
     * Providers - rarely changes during session
     */
    val PROVIDERS = SyncConfig(
        intervalMs = 60_000L,
        onSSEEvent = false,
        onForeground = true,
        onConnection = true,
        verifyFreshness = false
    )
    
    /**
     * Config - server configuration
     */
    val CONFIG = SyncConfig(
        intervalMs = 60_000L,
        onSSEEvent = false,
        onForeground = true,
        onConnection = true,
        verifyFreshness = false
    )
}

/**
 * Freshness verification result.
 */
sealed class FreshnessResult {
    @Immutable
    data class Fresh(val verifiedAt: Long) : FreshnessResult()
    @Immutable
    data class Stale(
        val serverVersion: String?,
        val localVersion: String?,
        val missingData: Boolean = false
    ) : FreshnessResult()
    @Immutable
    data class Error(val message: String) : FreshnessResult()
    
    val isFresh: Boolean get() = this is Fresh
}
