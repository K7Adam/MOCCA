package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * User preferences for app-wide settings.
 * Stored via SettingsRepository using key-value storage.
 * 
 * All settings have sensible defaults that work out-of-the-box.
 */
@Serializable
@Immutable
data class UserPreferences(

    // Session State

    
    /** Last active session ID - persisted for restoration on app restart */
    val lastSessionId: String? = null,

    // Appearance

    
    /** Show input/output token counts in chat messages */
    val showTokenCounts: Boolean = true,
    
    /** Show timestamps on chat messages */
    val showTimestamps: Boolean = true,
    
    /** Use compact layout (reduced padding for information density) */
    val compactMode: Boolean = false,
    
    /** Font scale multiplier (0.8f to 1.4f) */
    val fontScale: Float = 1.0f,
    
    /** Hide/mask API keys in settings UI for security */
    val hideApiKeys: Boolean = true,

    // Chat

    
    /** Auto-scroll to bottom when new messages arrive */
    val autoScroll: Boolean = true,
    
    /** Show confirmation dialog before deleting sessions */
    val confirmDelete: Boolean = true,
    
    /** Show AI thinking/reasoning blocks in messages */
    val showThinkingBlocks: Boolean = true,

    // Connection

    
    /** Automatically reconnect when connection is lost */
    val autoReconnect: Boolean = true,
    
    /** Data saver mode - reduce network usage (disable background sync) */
    val dataSaverMode: Boolean = false,

    // Notifications

    
    /** Show notification when permission approval is required */
    val notifyPermissions: Boolean = true,
    
    /** Show notification when AI session completes */
    val notifySessionComplete: Boolean = true,
    
    /** Show notification when connection to server is lost */
    val notifyConnectionLost: Boolean = true,

    // Privacy & Security

    
    /** Enable screen security (prevent screenshots) */
    val screenSecurity: Boolean = false,
    
    /** Clear local cache when app exits */
    val clearCacheOnExit: Boolean = false,

    // Updates

    
    /** Auto-check for app updates interval in minutes (0 = disabled, default 10 min) */
    val autoUpdateCheckIntervalMinutes: Int = 10
) {
    /**
     * Font scale clamped to valid range.
     */
    val validFontScale: Float get() = fontScale.coerceIn(0.8f, 1.4f)
    
    /**
     * Font scale percentage for display (80% to 140%)
     */
    val fontScalePercent: Int get() = (validFontScale * 100).toInt()
    
    companion object {
        /** Default preferences */
        val DEFAULT = UserPreferences()
        
        /** Valid font scale range */
        val FONT_SCALE_RANGE = 0.8f..1.4f
        
        /** Valid auto-update check interval range in minutes (0 = disabled, max = 60) */
        val AUTO_UPDATE_INTERVAL_RANGE = 0..60
        
        /** Preset intervals for auto-update check (in minutes) */
        val AUTO_UPDATE_INTERVAL_PRESETS = listOf(
            0 to "Disabled",
            5 to "5 minutes",
            10 to "10 minutes (default)",
            15 to "15 minutes",
            30 to "30 minutes",
            60 to "1 hour"
        )
        
        /** Font scale presets */
        val FONT_SCALE_PRESETS = listOf(
            0.8f to "Small",
            0.9f to "Medium-Small",
            1.0f to "Default",
            1.1f to "Medium-Large",
            1.2f to "Large",
            1.3f to "Extra Large",
            1.4f to "Maximum"
        )
    }
}
