package com.mocca.app.data.repository

import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SettingsRepository(
    private val localCache: LocalCache
) {
    companion object {
        // Session State
        const val KEY_LAST_SESSION_ID = "last_session_id"
        
        // GitHub / Updates
        const val KEY_GITHUB_TOKEN = "github_token"
        
        // Appearance
        const val KEY_SHOW_TOKEN_COUNTS = "show_token_counts"
        const val KEY_SHOW_TIMESTAMPS = "show_timestamps"
        const val KEY_COMPACT_MODE = "compact_mode"
        const val KEY_FONT_SCALE = "font_scale"
        const val KEY_HIDE_API_KEYS = "hide_api_keys"
        
        // Chat
        const val KEY_AUTO_SCROLL = "auto_scroll"
        const val KEY_CONFIRM_DELETE = "confirm_delete"
        const val KEY_SHOW_THINKING_BLOCKS = "show_thinking_blocks"
        
        // Connection
        const val KEY_AUTO_RECONNECT = "auto_reconnect"
        const val KEY_DATA_SAVER_MODE = "data_saver_mode"
        
        // Notifications
        const val KEY_NOTIFY_PERMISSIONS = "notify_permissions"
        const val KEY_NOTIFY_SESSION_COMPLETE = "notify_session_complete"
        const val KEY_NOTIFY_CONNECTION_LOST = "notify_connection_lost"
        
        // Privacy
        const val KEY_SCREEN_SECURITY = "screen_security"
        const val KEY_CLEAR_CACHE_ON_EXIT = "clear_cache_on_exit"
        
        // Updates
        const val KEY_AUTO_UPDATE_CHECK_INTERVAL = "auto_update_check_interval"
    }

    // Session State


    /**
     * Get the last active session ID for restoration.
     */
    suspend fun getLastSessionId(): String? = withContext(Dispatchers.IO) {
        localCache.getSetting(KEY_LAST_SESSION_ID)
    }

    /**
     * Save the last active session ID.
     */
    suspend fun saveLastSessionId(sessionId: String?) = withContext(Dispatchers.IO) {
        if (sessionId.isNullOrBlank()) {
            localCache.deleteSetting(KEY_LAST_SESSION_ID)
        } else {
            localCache.saveSetting(KEY_LAST_SESSION_ID, sessionId)
        }
    }

    // GitHub Token


    suspend fun getGitHubToken(): String? = withContext(Dispatchers.IO) {
        localCache.getSetting(KEY_GITHUB_TOKEN)
    }

    suspend fun saveGitHubToken(token: String) = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            localCache.deleteSetting(KEY_GITHUB_TOKEN)
        } else {
            localCache.saveSetting(KEY_GITHUB_TOKEN, token)
        }
    }

    suspend fun clearGitHubToken() = withContext(Dispatchers.IO) {
        localCache.deleteSetting(KEY_GITHUB_TOKEN)
    }

    // User Preferences (typed getters/setters)


    /**
     * Load all user preferences with defaults.
     */
    suspend fun getUserPreferences(): UserPreferences = withContext(Dispatchers.IO) {
        UserPreferences(
            lastSessionId = getLastSessionId(),
            showTokenCounts = getBoolean(KEY_SHOW_TOKEN_COUNTS, true),
            showTimestamps = getBoolean(KEY_SHOW_TIMESTAMPS, true),
            compactMode = getBoolean(KEY_COMPACT_MODE, false),
            fontScale = getFloat(KEY_FONT_SCALE, 1.0f),
            hideApiKeys = getBoolean(KEY_HIDE_API_KEYS, true),
            autoScroll = getBoolean(KEY_AUTO_SCROLL, true),
            confirmDelete = getBoolean(KEY_CONFIRM_DELETE, true),
            showThinkingBlocks = getBoolean(KEY_SHOW_THINKING_BLOCKS, true),
            autoReconnect = getBoolean(KEY_AUTO_RECONNECT, true),
            dataSaverMode = getBoolean(KEY_DATA_SAVER_MODE, false),
            notifyPermissions = getBoolean(KEY_NOTIFY_PERMISSIONS, true),
            notifySessionComplete = getBoolean(KEY_NOTIFY_SESSION_COMPLETE, true),
            notifyConnectionLost = getBoolean(KEY_NOTIFY_CONNECTION_LOST, true),
            screenSecurity = getBoolean(KEY_SCREEN_SECURITY, false),
            clearCacheOnExit = getBoolean(KEY_CLEAR_CACHE_ON_EXIT, false),
            autoUpdateCheckIntervalMinutes = getInt(KEY_AUTO_UPDATE_CHECK_INTERVAL, 10)
        )
    }

    // Appearance Settings


    suspend fun getShowTokenCounts(): Boolean = getBoolean(KEY_SHOW_TOKEN_COUNTS, true)
    suspend fun setShowTokenCounts(value: Boolean) = setBoolean(KEY_SHOW_TOKEN_COUNTS, value)

    suspend fun getShowTimestamps(): Boolean = getBoolean(KEY_SHOW_TIMESTAMPS, true)
    suspend fun setShowTimestamps(value: Boolean) = setBoolean(KEY_SHOW_TIMESTAMPS, value)

    suspend fun getCompactMode(): Boolean = getBoolean(KEY_COMPACT_MODE, false)
    suspend fun setCompactMode(value: Boolean) = setBoolean(KEY_COMPACT_MODE, value)

    suspend fun getFontScale(): Float = getFloat(KEY_FONT_SCALE, 1.0f)
    suspend fun setFontScale(value: Float) = setFloat(KEY_FONT_SCALE, value)

    suspend fun getHideApiKeys(): Boolean = getBoolean(KEY_HIDE_API_KEYS, true)
    suspend fun setHideApiKeys(value: Boolean) = setBoolean(KEY_HIDE_API_KEYS, value)

    // Chat Settings


    suspend fun getAutoScroll(): Boolean = getBoolean(KEY_AUTO_SCROLL, true)
    suspend fun setAutoScroll(value: Boolean) = setBoolean(KEY_AUTO_SCROLL, value)

    suspend fun getConfirmDelete(): Boolean = getBoolean(KEY_CONFIRM_DELETE, true)
    suspend fun setConfirmDelete(value: Boolean) = setBoolean(KEY_CONFIRM_DELETE, value)

    suspend fun getShowThinkingBlocks(): Boolean = getBoolean(KEY_SHOW_THINKING_BLOCKS, true)
    suspend fun setShowThinkingBlocks(value: Boolean) = setBoolean(KEY_SHOW_THINKING_BLOCKS, value)

    // Connection Settings


    suspend fun getAutoReconnect(): Boolean = getBoolean(KEY_AUTO_RECONNECT, true)
    suspend fun setAutoReconnect(value: Boolean) = setBoolean(KEY_AUTO_RECONNECT, value)

    suspend fun getDataSaverMode(): Boolean = getBoolean(KEY_DATA_SAVER_MODE, false)
    suspend fun setDataSaverMode(value: Boolean) = setBoolean(KEY_DATA_SAVER_MODE, value)

    // Notification Settings


    suspend fun getNotifyPermissions(): Boolean = getBoolean(KEY_NOTIFY_PERMISSIONS, true)
    suspend fun setNotifyPermissions(value: Boolean) = setBoolean(KEY_NOTIFY_PERMISSIONS, value)

    suspend fun getNotifySessionComplete(): Boolean = getBoolean(KEY_NOTIFY_SESSION_COMPLETE, true)
    suspend fun setNotifySessionComplete(value: Boolean) = setBoolean(KEY_NOTIFY_SESSION_COMPLETE, value)

    suspend fun getNotifyConnectionLost(): Boolean = getBoolean(KEY_NOTIFY_CONNECTION_LOST, true)
    suspend fun setNotifyConnectionLost(value: Boolean) = setBoolean(KEY_NOTIFY_CONNECTION_LOST, value)

    // Privacy Settings


    suspend fun getScreenSecurity(): Boolean = getBoolean(KEY_SCREEN_SECURITY, false)
    suspend fun setScreenSecurity(value: Boolean) = setBoolean(KEY_SCREEN_SECURITY, value)

    suspend fun getClearCacheOnExit(): Boolean = getBoolean(KEY_CLEAR_CACHE_ON_EXIT, false)
    suspend fun setClearCacheOnExit(value: Boolean) = setBoolean(KEY_CLEAR_CACHE_ON_EXIT, value)

    // Update Settings


    suspend fun getAutoUpdateCheckInterval(): Int = getInt(KEY_AUTO_UPDATE_CHECK_INTERVAL, 10)
    suspend fun setAutoUpdateCheckInterval(value: Int) = setInt(KEY_AUTO_UPDATE_CHECK_INTERVAL, value)

    // Helper Methods


    private suspend fun getBoolean(key: String, default: Boolean): Boolean = withContext(Dispatchers.IO) {
        localCache.getSetting(key)?.toBooleanStrictOrNull() ?: default
    }

    private suspend fun setBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        localCache.saveSetting(key, value.toString())
    }

    private suspend fun getFloat(key: String, default: Float): Float = withContext(Dispatchers.IO) {
        localCache.getSetting(key)?.toFloatOrNull() ?: default
    }

    private suspend fun setFloat(key: String, value: Float) = withContext(Dispatchers.IO) {
        localCache.saveSetting(key, value.toString())
    }

    private suspend fun getInt(key: String, default: Int): Int = withContext(Dispatchers.IO) {
        localCache.getSetting(key)?.toIntOrNull() ?: default
    }

    private suspend fun setInt(key: String, value: Int) = withContext(Dispatchers.IO) {
        localCache.saveSetting(key, value.toString())
    }
}
