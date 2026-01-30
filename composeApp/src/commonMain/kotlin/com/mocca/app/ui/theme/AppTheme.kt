package com.mocca.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * MOCCA App Theme - Modern dark theme with accent colors.
 *
 * Features:
 * - Pitch black (#000000) OLED-friendly background
 * - Mint green accent colors (#00D9A5)
 * - Modern rounded corners (16dp-32dp)
 * - Extended colors for app-specific elements
 */

// ═══════════════════════════════════════════════════════════════════════════════
// COLOR SCHEME
// ═══════════════════════════════════════════════════════════════════════════════

private val AppColorScheme: ColorScheme = darkColorScheme(
    // Primary - Off-white for main interactive elements (buttons)
    primary = AppColors.buttonBackground,
    onPrimary = AppColors.buttonText,
    primaryContainer = AppColors.surfaceContainer,
    onPrimaryContainer = AppColors.white,

    // Secondary - Accent green for status/success
    secondary = AppColors.accentGreen,
    onSecondary = AppColors.background,
    secondaryContainer = AppColors.surfaceContainer,
    onSecondaryContainer = AppColors.accentGreen,

    // Tertiary - Blue for git/tabs
    tertiary = AppColors.primary,
    onTertiary = AppColors.white,
    tertiaryContainer = AppColors.primaryDim,
    onTertiaryContainer = AppColors.primary,

    // Error
    error = AppColors.error,
    onError = AppColors.white,
    errorContainer = AppColors.alertRedDim,
    onErrorContainer = AppColors.error,

    // Background - Pure black
    background = AppColors.background,
    onBackground = AppColors.textPrimary,

    // Surface - Slightly elevated black
    surface = AppColors.surface,
    onSurface = AppColors.textPrimary,
    surfaceVariant = AppColors.surfaceVariant,
    onSurfaceVariant = AppColors.textSecondary,
    surfaceTint = AppColors.white,

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
 * - Pitch black (#000000) background
 * - Mint green accents
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
