package com.mocca.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
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
 * - Modern rounded corners (12dp-24dp)
 * - Extended colors for app-specific elements
 */

// ═══════════════════════════════════════════════════════════════════════════════
// COLOR SCHEME
// ═══════════════════════════════════════════════════════════════════════════════

private val AppColorScheme: ColorScheme = darkColorScheme(
    // Primary — accent color for main interactive elements
    primary = AppColors.accentGreen,
    onPrimary = AppColors.buttonText,
    primaryContainer = AppColors.surfaceContainerHigh,
    onPrimaryContainer = AppColors.textPrimary,

    // Secondary — accent for status/emphasis
    secondary = AppColors.accentGreen,
    onSecondary = AppColors.background,
    secondaryContainer = AppColors.surfaceContainer,
    onSecondaryContainer = AppColors.accentGreen,

    // Tertiary
    tertiary = AppColors.primary,
    onTertiary = AppColors.textPrimary,
    tertiaryContainer = AppColors.primaryDim,
    onTertiaryContainer = AppColors.statusOnline,

    // Error
    error = AppColors.error,
    onError = AppColors.white,
    errorContainer = AppColors.alertRedDim,
    onErrorContainer = AppColors.error,

    // Background
    background = AppColors.background,
    onBackground = AppColors.textPrimary,

    // Surface — M3 tonal elevation hierarchy
    surface = AppColors.surface,
    onSurface = AppColors.textPrimary,
    surfaceVariant = AppColors.surfaceVariant,
    onSurfaceVariant = AppColors.textSecondary,
    surfaceTint = AppColors.accentGreen,
    surfaceBright = AppColors.surfaceBright,
    surfaceDim = AppColors.surfaceDim,
    surfaceContainer = AppColors.surfaceContainer,
    surfaceContainerHigh = AppColors.surfaceContainerHigh,
    surfaceContainerHighest = AppColors.surfaceContainerHighest,
    surfaceContainerLow = AppColors.surfaceContainerLow,
    surfaceContainerLowest = AppColors.surfaceContainerLowest,

    // Outline
    outline = AppColors.border,
    outlineVariant = AppColors.borderLight,

    // Inverse (for contrast elements like snackbars)
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
 * - Soft dark (#1A1A1A) background
 * - Desaturated cool gray-blue accent
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
