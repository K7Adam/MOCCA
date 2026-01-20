package com.mocca.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Modern MOCCA theme - Pitch black with glassmorphic accents.
 * Based on UI overhaul designs.
 * 
 * Features:
 * - Pitch black (#000000) OLED-friendly background
 * - Mint green accent colors (#00D9A5, #30D158)
 * - Modern rounded corners (16dp-32dp)
 * - Space Grotesk-inspired typography
 * - Extended colors for app-specific elements
 */

// ═══════════════════════════════════════════════════════════════════════════════
// COLOR SCHEME
// ═══════════════════════════════════════════════════════════════════════════════

private val TerminalColorScheme: ColorScheme = darkColorScheme(
    // Primary - Off-white for main interactive elements (buttons)
    primary = TerminalColors.buttonBackground,
    onPrimary = TerminalColors.buttonText,
    primaryContainer = TerminalColors.surfaceContainer,
    onPrimaryContainer = TerminalColors.white,
    
    // Secondary - Accent green for status/success
    secondary = TerminalColors.accentGreen,
    onSecondary = TerminalColors.background,
    secondaryContainer = TerminalColors.surfaceContainer,
    onSecondaryContainer = TerminalColors.accentGreen,
    
    // Tertiary - Blue for git/tabs
    tertiary = TerminalColors.primary,
    onTertiary = TerminalColors.white,
    tertiaryContainer = TerminalColors.primaryDim,
    onTertiaryContainer = TerminalColors.primary,
    
    // Error
    error = TerminalColors.error,
    onError = TerminalColors.white,
    errorContainer = TerminalColors.alertRedDim,
    onErrorContainer = TerminalColors.error,
    
    // Background - Pure black
    background = TerminalColors.background,
    onBackground = TerminalColors.textPrimary,
    
    // Surface - Slightly elevated black
    surface = TerminalColors.surface,
    onSurface = TerminalColors.textPrimary,
    surfaceVariant = TerminalColors.surfaceVariant,
    onSurfaceVariant = TerminalColors.textSecondary,
    surfaceTint = TerminalColors.white,
    
    // Outline - For borders
    outline = TerminalColors.border,
    outlineVariant = TerminalColors.borderLight,
    
    // Inverse colors (for contrast elements like snackbars)
    inverseSurface = TerminalColors.white,
    inverseOnSurface = TerminalColors.background,
    inversePrimary = TerminalColors.greyDark,
    
    // Scrim
    scrim = TerminalColors.scrim
)

// ═══════════════════════════════════════════════════════════════════════════════
// THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Modern MOCCA theme.
 * 
 * Features:
 * - Pitch black (#000000) background
 * - Mint green accents
 * - Modern rounded corners
 * - Space Grotesk-inspired typography
 * - Extended colors for terminal-specific elements
 */
@Composable
fun TerminalTheme(
    content: @Composable () -> Unit
) {
    val extendedColors = ExtendedTerminalColors()
    
    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = TerminalColorScheme,
            typography = terminalTypography(),
            shapes = terminalShapes(),
            content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// THEME ACCESSORS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Access extended terminal colors and tokens from the current theme.
 * 
 * Usage:
 * ```
 * val statusColor = TerminalTheme.extendedColors.statusOnline
 * val cardPadding = TerminalTheme.spacing.cardPadding
 * ```
 */
object TerminalTheme {
    /**
     * Extended colors not available in MaterialTheme.colorScheme
     */
    val extendedColors: ExtendedTerminalColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
    
    /**
     * Spacing tokens for consistent layout
     */
    val spacing: TerminalSpacing
        get() = TerminalSpacing
    
    /**
     * Raw typography styles (for use outside MaterialTheme context)
     */
    val typography: TerminalTypography
        get() = TerminalTypography
    
    /**
     * Shape tokens
     */
    val shapes: TerminalShapes
        get() = TerminalShapes
    
    /**
     * Color tokens (direct access)
     */
    val colors: TerminalColors
        get() = TerminalColors
}
