package com.mocca.app.data.repository

import com.mocca.app.domain.model.UserPreferences
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Centralized manager for user preferences.
 * 
 * Provides a single source of truth for preferences that can be accessed
 * from anywhere in the app. Changes are persisted via SettingsRepository
 * and propagated reactively via StateFlow.
 * 
 * Usage:
 * ```kotlin
 * class MyClass(private val preferencesManager: PreferencesManager) {
 *     fun checkSetting() {
 *         if (preferencesManager.preferences.value.showTokenCounts) {
 *             // Show tokens
 *         }
 *     }
 *     
 *     fun observeSettings() {
 *         preferencesManager.preferences.collect { prefs ->
 *             // React to changes
 *         }
 *     }
 * }
 * ```
 */
class PreferencesManager(
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _preferences = MutableStateFlow(UserPreferences.DEFAULT)
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()
    
    init {
        loadPreferences()
    }
    
    /**
     * Load preferences from storage.
     */
    private fun loadPreferences() {
        scope.launch {
            try {
                val prefs = settingsRepository.getUserPreferences()
                _preferences.value = prefs
                Napier.i { "[PreferencesManager] Preferences loaded: $prefs" }
            } catch (e: Exception) {
                Napier.e("[PreferencesManager] Failed to load preferences", e)
            }
        }
    }
    
    /**
     * Update a specific preference and persist it.
     */
    fun updatePreferences(newPreferences: UserPreferences) {
        scope.launch {
            try {
                // Update local state immediately
                _preferences.value = newPreferences
                Napier.d { "[PreferencesManager] Preferences updated locally" }
            } catch (e: Exception) {
                Napier.e("[PreferencesManager] Failed to update preferences", e)
            }
        }
    }
    
    /**
     * Reload preferences from storage.
     * Call this when preferences are changed externally (e.g., from SettingsScreen).
     */
    fun refresh() {
        loadPreferences()
    }

    // Convenience accessors for common preference checks

    
    /** Whether to show token counts in chat */
    val showTokenCounts: Boolean get() = _preferences.value.showTokenCounts
    
    /** Whether to show timestamps in chat */
    val showTimestamps: Boolean get() = _preferences.value.showTimestamps
    
    /** Whether permission notifications are enabled */
    val notifyPermissions: Boolean get() = _preferences.value.notifyPermissions
    
    /** Whether session complete notifications are enabled */
    val notifySessionComplete: Boolean get() = _preferences.value.notifySessionComplete
    
    /** Whether connection lost notifications are enabled */
    val notifyConnectionLost: Boolean get() = _preferences.value.notifyConnectionLost
    
    /** Auto-update check interval in minutes (0 = disabled) */
    val autoUpdateCheckIntervalMinutes: Int get() = _preferences.value.autoUpdateCheckIntervalMinutes
}
