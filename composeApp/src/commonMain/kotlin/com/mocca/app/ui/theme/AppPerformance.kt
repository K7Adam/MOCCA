package com.mocca.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Performance tier of the current device.
 */
enum class PerformanceTier {
    LOW,    // Low RAM, old CPU (e.g. < 4GB RAM)
    MEDIUM, // Standard device
    HIGH    // High-end device (e.g. > 8GB RAM, 120Hz screen)
}

/**
 * Global configuration for app performance and resource usage.
 */
data class AppPerformance(
    val tier: PerformanceTier = PerformanceTier.MEDIUM,
    val lowPowerMode: Boolean = false,
    val highRefreshRate: Boolean = false
) {
    /**
     * Whether to use complex expressive animations.
     */
    val useExpressiveMotion: Boolean 
        get() = tier != PerformanceTier.LOW && !lowPowerMode

    /**
     * Max number of items to keep in memory for lists.
     */
    val maxListCacheSize: Int
        get() = when (tier) {
            PerformanceTier.LOW -> 20
            PerformanceTier.MEDIUM -> 50
            PerformanceTier.HIGH -> 100
        }
}

val LocalAppPerformance = staticCompositionLocalOf { AppPerformance() }
