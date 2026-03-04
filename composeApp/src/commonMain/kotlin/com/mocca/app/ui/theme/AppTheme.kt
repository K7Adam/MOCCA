package com.mocca.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * MOCCA App Theme — Nebula Dark
 *
 * Features:
 * - Rich dark navy-charcoal (#0D0E12) background
 * - Electric Violet accents (#7C5CFC)
 * - Warm off-white text (#ECEDF3)
 * - Modern rounded corners (16dp-32dp)
 * - Extended colors for app-specific elements
 */

// ═══════════════════════════════════════════════════════════════════════════════
// COLOR SCHEME
// ═══════════════════════════════════════════════════════════════════════════════

private val AppColorScheme: ColorScheme = darkColorScheme(
    // Primary — Electric Violet for interactive elements
    primary = AppColors.primary,
    onPrimary = AppColors.buttonText,
    primaryContainer = AppColors.surfaceContainer,
    onPrimaryContainer = AppColors.white,

    // Secondary — Bright Violet for status/emphasis
    secondary = AppColors.accentGreenBright,
    onSecondary = AppColors.background,
    secondaryContainer = AppColors.surfaceContainer,
    onSecondaryContainer = AppColors.accentGreenBright,

    // Tertiary — Emerald for success states
    tertiary = AppColors.statusOnline,
    onTertiary = AppColors.background,
    tertiaryContainer = AppColors.primaryDim,
    onTertiaryContainer = AppColors.statusOnline,

    // Error
    error = AppColors.error,
    onError = AppColors.white,
    errorContainer = AppColors.alertRedDim,
    onErrorContainer = AppColors.error,

    // Background — Rich dark navy-charcoal
    background = AppColors.background,
    onBackground = AppColors.textPrimary,

    // Surface — Elevated dark navy
    surface = AppColors.surface,
    onSurface = AppColors.textPrimary,
    surfaceVariant = AppColors.surfaceVariant,
    onSurfaceVariant = AppColors.textSecondary,
    surfaceTint = AppColors.primary,

    // Outline - For borders
    outline = AppColors.border,
    outlineVariant = AppColors.borderLight,

    // Inverse colors (for contrast elements like snackbars)
    inverseSurface = AppColors.white,
    inverseOnSurface = AppColors.background,
    inversePrimary = AppColors.greyDark,

    // Scrim
    scrim = AppColors.scrim
)

// ═══════════════════════════════════════════════════════════════════════════════
// THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * MOCCA App Theme.
 *
 * Features:
 * - Nebula Dark (#0D0E12) background
 * - Electric Violet accents
 * - Warm off-white text
 * - Modern rounded corners
 * - Extended colors for app-specific elements
 */
@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val extendedColors = ExtendedAppColors()

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = AppColorScheme,
            typography = appTypography(),
            shapes = appShapes(),
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
