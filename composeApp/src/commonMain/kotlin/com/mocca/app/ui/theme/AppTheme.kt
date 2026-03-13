package com.mocca.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * MOCCA App Theme — Neutral monochrome soft dark.
 *
 * Features:
 * - Soft dark (#1A1A1A) background
 * - Desaturated cool gray-blue accent (#8B9DC3)
 * - M3 tonal surface elevation hierarchy
 * - Modern rounded corners and expressive shapes
 * - Extended colors for app-specific elements
 */

// ═══════════════════════════════════════════════════════════════════════════════
// COLOR SCHEME
// ═══════════════════════════════════════════════════════════════════════════════

private val AppColorScheme: ColorScheme = darkColorScheme(
    primary = AppColors.AnchorPrimary,
    secondary = AppColors.AnchorSecondary,
    tertiary = AppColors.AnchorTertiary,
    background = AppColors.DarkBackgroundSeed,
    surface = AppColors.DarkBackgroundSeed,
    // Rely on Material 3 standard generation for the rest, anchoring on the primary and dark seed.
)

// ═══════════════════════════════════════════════════════════════════════════════
// THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * MOCCA App Theme.
 *
 * Features:
 * - Soft dark (#1A1A1A) background
 * - Desaturated cool gray-blue accent
 * - Modern rounded corners
 * - Extended colors for app-specific elements
 * - Material 3 Expressive Motion
 */

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val extendedColors = ExtendedAppColors()

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialExpressiveTheme(
            colorScheme = AppColorScheme,
            typography = appTypography(),
            shapes = appShapes(),
            motionScheme = MotionScheme.expressive(),
            content = content
        )

    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// THEME ACCESSORS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Access extended app colors and tokens from the current theme.
 *
 * Usage:
 * ```
 * val statusColor = AppTheme.extendedColors.statusOnline
 * val cardPadding = AppTheme.spacing.cardPadding
 * ```
 */
object AppTheme {
    /**
     * Extended colors not available in MaterialTheme.colorScheme
     */
    val extendedColors: ExtendedAppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current

    /**
     * Spacing tokens for consistent layout
     */
    val spacing: AppSpacing
        get() = AppSpacing

    /**
     * Raw typography styles (for use outside MaterialTheme context)
     */
    val typography: AppTypography
        get() = AppTypography

    /**
     * Shape tokens
     */
    val shapes: AppShapes
        get() = AppShapes

    /**
     * Color tokens (direct access)
     */
    val colors: AppColors
        get() = AppColors
}
