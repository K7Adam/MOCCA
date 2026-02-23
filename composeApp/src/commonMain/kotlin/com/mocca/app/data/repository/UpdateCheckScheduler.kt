package com.mocca.app.data.repository

import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.UpdateCheckResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages periodic update checks for the app.
 * 
 * This scheduler:
 * - Only checks when the server is connected (no wasted API calls)
 * - Is lifecycle-aware via start/stop methods
 * - Uses the global UpdateNotifier to notify when updates are available
 * - Respects the configured check interval from preferences
 * 
 * Usage:
 * ```kotlin
 * // Start periodic checks (typically in MainScreenModel init)
 * updateCheckScheduler.start()
 * 
 * // Stop checks (typically in MainScreenModel onDispose)
 * updateCheckScheduler.stop()
 * ```
 */
class UpdateCheckScheduler(
    private val updateRepository: UpdateRepository,
    private val updateNotifier: UpdateNotifier,
    private val connectionManager: ConnectionManager,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "UpdateCheckScheduler"
        // Default interval in minutes (used if preferences not loaded)
        private const val DEFAULT_INTERVAL_MINUTES = 10
        // Minimum interval to prevent API abuse (in minutes)
        private const val MIN_INTERVAL_MINUTES = 5
    }
    
    private val isRunning = AtomicBoolean(false)
    
    private var checkJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _lastCheckTime = MutableStateFlow<Long?>(null)
    val lastCheckTime: StateFlow<Long?> = _lastCheckTime.asStateFlow()
    
    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()
    
    /**
     * Start periodic update checks.
     * Safe to call multiple times - will not create duplicate jobs.
     */
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            Napier.i("Starting periodic update checks", tag = TAG)
            startPeriodicCheck()
        } else {
            Napier.d("Update checks already running", tag = TAG)
        }
    }
    
    /**
     * Stop periodic update checks.
     * Safe to call when not running.
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            Napier.i("Stopping periodic update checks", tag = TAG)
            checkJob?.cancel()
            checkJob = null
        }
    }
    
    /**
     * Force an immediate update check, ignoring the schedule.
     * Useful for manual refresh scenarios.
     */
    suspend fun checkNow(): UpdateCheckResult {
        return performCheck()
    }
    
    private fun startPeriodicCheck() {
        checkJob?.cancel()
        checkJob = scope.launch {
            Napier.d("Periodic check coroutine started", tag = TAG)
            
            while (isActive && isRunning.get()) {
                // Get interval from preferences
                val intervalMinutes = getCheckIntervalMinutes()
                
                if (intervalMinutes <= 0) {
                    // Update checks disabled, wait and check again later
                    Napier.d("Update checks disabled in preferences", tag = TAG)
                    delay(60_000) // Check again in 1 minute
                    continue
                }
                
                // Check if connected before making API call
                val isConnected = connectionManager.status.value is ConnectionStatus.Connected
                
                if (isConnected) {
                    Napier.d("Performing scheduled update check", tag = TAG)
                    performCheck()
                } else {
                    Napier.d("Skipping update check - not connected to server", tag = TAG)
                }
                
                // Wait for the configured interval
                val delayMs = intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES) * 60_000L
                Napier.d("Next check in $intervalMinutes minutes", tag = TAG)
                delay(delayMs)
            }
            
            Napier.d("Periodic check coroutine ended", tag = TAG)
        }
    }
    
    private suspend fun performCheck(): UpdateCheckResult {
        _isChecking.value = true
        
        return try {
            Napier.d("Checking for updates...", tag = TAG)
            
            val result = updateRepository.checkForUpdateDetailed()
            
            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Napier.i("Update available: ${result.updateInfo.version}", tag = TAG)
                    updateNotifier.notifyUpdateAvailable(result.updateInfo)
                }
                is UpdateCheckResult.NoUpdate -> {
                    Napier.d("No update available", tag = TAG)
                }
                is UpdateCheckResult.Error -> {
                    Napier.w("Update check failed: ${result.message}", tag = TAG)
                }
            }
            
            _lastCheckTime.value = System.currentTimeMillis()
            result
        } catch (e: Exception) {
            Napier.e("Exception during update check: ${e.message}", e, TAG)
            UpdateCheckResult.Error(e.message ?: "Unknown error")
        } finally {
            _isChecking.value = false
        }
    }
    
    private fun getCheckIntervalMinutes(): Int {
        return preferencesManager.preferences.value.autoUpdateCheckIntervalMinutes
            .coerceIn(0, 60)
    }
    
    /**
     * Clean up resources.
     */
    fun destroy() {
        stop()
        scope.cancel()
    }
}
