package com.mocca.app.data.repository

import com.mocca.app.domain.model.SyncConfig
import com.mocca.app.domain.model.SyncState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Base class for repositories that support sync state tracking.
 * 
 * This provides:
 * - Sync state tracking (Idle, Fetching, Fresh, Failed)
 * - Integration with SyncStateManager for global state aggregation
 * - Exponential backoff retry logic
 * - Thread-safe refresh operations
 * 
 * Usage:
 * ```kotlin
 * class GitRepository(
 *     private val apiClient: MoccaApiClient,
 *     private val localCache: LocalCache,
 *     syncStateManager: SyncStateManager
 * ) : SyncableRepository(syncStateManager, "git", SyncConfigs.GIT) {
 *     
 *     override suspend fun refresh() {
 *         withSyncTracking(
 *             operation = { apiClient.getVcsInfo() },
 *             onSuccess = { vcsInfo -> 
 *                 localCache.saveGitStatus(GitStatusResponse(...))
 *             }
 *         )
 *     }
 * }
 * ```
 */
abstract class SyncableRepository(
    protected val syncStateManager: SyncStateManager,
    protected val repoName: String,
    protected val syncConfig: SyncConfig
) {
    protected val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    protected val syncMutex = Mutex()
    protected var retryCount = 0
    
    /**
     * Update sync state and notify SyncStateManager.
     */
    protected suspend fun updateSyncState(state: SyncState) {
        _syncState.value = state
        syncStateManager.updateRepoState(repoName, state)
    }
    
    /**
     * Non-suspending version for use in flows.
     */
    protected fun updateSyncStateSync(state: SyncState) {
        _syncState.value = state
        syncStateManager.updateRepoStateSync(repoName, state)
    }
    
    /**
     * Execute a sync operation with state tracking and retry logic.
     * 
     * @param operation The suspend function that performs the actual sync
     * @param onSuccess Callback to process successful results
     */
    protected suspend fun <T> withSyncTracking(
        operation: suspend () -> Result<T>,
        onSuccess: (T) -> Unit
    ) {
        syncMutex.withLock {
            updateSyncState(SyncState.Fetching)
            
            val result = executeWithRetry(operation)
            
            result.fold(
                onSuccess = { data ->
                    onSuccess(data)
                    updateSyncState(SyncState.Fresh())
                    retryCount = 0
                    Napier.v("[$repoName] Sync successful")
                },
                onFailure = { error ->
                    val currentState = _syncState.value
                    val lastFresh = (currentState as? SyncState.Fresh)?.timestamp
                    
                    updateSyncState(SyncState.Failed(
                        error = error.message ?: "Unknown error",
                        lastFreshTimestamp = lastFresh,
                        retryCount = retryCount
                    ))
                    Napier.w("[$repoName] Sync failed: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Execute operation with exponential backoff retry.
     */
    private suspend fun <T> executeWithRetry(
        operation: suspend () -> Result<T>
    ): Result<T> {
        var lastError: Throwable? = null
        
        repeat(syncConfig.maxRetries) { attempt ->
            val result = operation()
            if (result.isSuccess) return result
            
            lastError = result.exceptionOrNull()
            retryCount = attempt + 1
            
            if (attempt < syncConfig.maxRetries - 1) {
                val delayMs = calculateBackoff(attempt)
                Napier.d("[$repoName] Retry ${attempt + 1}/${syncConfig.maxRetries} in ${delayMs}ms")
                kotlinx.coroutines.delay(delayMs)
            }
        }
        
        return Result.failure(lastError ?: Exception("Unknown error"))
    }
    
    /**
     * Calculate exponential backoff delay with jitter.
     */
    private fun calculateBackoff(attempt: Int): Long {
        val baseDelay = 1000L
        val maxDelay = 10000L
        val exponentialDelay = baseDelay * (1L shl attempt)
        val jitter = Random.nextLong(0, 500)
        return minOf(exponentialDelay, maxDelay) + jitter
    }
    
    /**
     * Force refresh this repository's data.
     * Must be implemented by each repository.
     */
    abstract suspend fun refresh()
    
    /**
     * Check if data is fresh and doesn't need sync.
     */
    fun isFresh(): Boolean = _syncState.value.isFresh
    
    /**
     * Get age of current data in milliseconds.
     */
    fun dataAgeMs(): Long? = _syncState.value.ageMs()
    
    /**
     * Ensure data is fresh, refresh if needed.
     * This is the "lazy sync" pattern - only sync if stale.
     */
    suspend fun ensureFresh() {
        val age = dataAgeMs()
        if (age == null || age > syncConfig.intervalMs) {
            Napier.d("[$repoName] Data stale (age=${age}ms), refreshing")
            refresh()
        } else {
            Napier.v("[$repoName] Data fresh (age=${age}ms), skipping refresh")
        }
    }
    
    /**
     * Mark repository as needing sync.
     */
    fun markStale() {
        if (_syncState.value.isFresh) {
            updateSyncStateSync(SyncState.Idle)
            Napier.d("[$repoName] Marked as stale")
        }
    }
}
