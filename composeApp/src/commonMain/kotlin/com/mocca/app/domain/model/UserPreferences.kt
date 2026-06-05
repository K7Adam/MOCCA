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
    /** Last active session ID - persisted for restoration on app restart */
    val lastSessionId: String? = null,

    /** Show input/output token counts in chat messages */
    val showTokenCounts: Boolean = true,
    
    /** Show timestamps on chat messages */
    val showTimestamps: Boolean = true,
    
    /** Code font family key — matches a curated list in AppTypography */
    val codeFontFamily: String = DEFAULT_CODE_FONT,

    /** Show notification when permission approval is required */
    val notifyPermissions: Boolean = true,
    
    /** Show notification when AI session completes */
    val notifySessionComplete: Boolean = true,
    
    /** Show notification when connection to server is lost */
    val notifyConnectionLost: Boolean = true,

    /** Auto-check for app updates interval in minutes (0 = disabled, default 10 min) */
    val autoUpdateCheckIntervalMinutes: Int = 10
) {
    companion object {
        /** Default code font identifier */
        const val DEFAULT_CODE_FONT = "jetbrains_mono"
        
        /** All available code font identifiers */
        val CODE_FONT_OPTIONS = listOf(
            "jetbrains_mono" to "JetBrains Mono",
            "fira_code" to "Fira Code",
            "source_code_pro" to "Source Code Pro",
            "system_mono" to "System Monospace"
        )
        
        /** Default preferences */
        val DEFAULT = UserPreferences()
        
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
    }
}
