package com.mocca.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Terminal/TUI theme for the MOCCA app.
 * Pitch black background, white primary content, monospace typography, 0dp corners.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// COLOR SCHEME
// ═══════════════════════════════════════════════════════════════════════════════

private val TerminalColorScheme: ColorScheme = darkColorScheme(
    // Primary - White for main interactive elements
    primary = TerminalColors.white,
    onPrimary = TerminalColors.background,
    primaryContainer = TerminalColors.surfaceVariant,
    onPrimaryContainer = TerminalColors.white,
    
    // Secondary - Grey for secondary elements
    secondary = TerminalColors.greyLight,
    onSecondary = TerminalColors.background,
    secondaryContainer = TerminalColors.surfaceContainer,
    onSecondaryContainer = TerminalColors.whiteDim,
    
    // Tertiary - Status green for connected/success states
    tertiary = TerminalColors.statusOnline,
    onTertiary = TerminalColors.background,
    tertiaryContainer = TerminalColors.surfaceVariant,
    onTertiaryContainer = TerminalColors.statusOnline,
    
    // Error
    error = TerminalColors.error,
    onError = TerminalColors.background,
    errorContainer = TerminalColors.surfaceVariant,
    onErrorContainer = TerminalColors.error,
    
    // Background - Pure black
    background = TerminalColors.background,
    onBackground = TerminalColors.whiteDim,
    
    // Surface - Slightly elevated black
    surface = TerminalColors.surface,
    onSurface = TerminalColors.whiteDim,
    surfaceVariant = TerminalColors.surfaceVariant,
    onSurfaceVariant = TerminalColors.whiteMuted,
    surfaceTint = TerminalColors.white,
    
    // Outline - For borders
    outline = TerminalColors.border,
    outlineVariant = TerminalColors.greyDark,
    
    // Inverse colors (for contrast elements like snackbars)
    inverseSurface = TerminalColors.white,
    inverseOnSurface = TerminalColors.background,
    inversePrimary = TerminalColors.greyDark,
    
    // Scrim
    scrim = TerminalColors.background
)

// ═══════════════════════════════════════════════════════════════════════════════
// THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Terminal theme for the MOCCA app.
 * 
 * Features:
 * - Pitch black (#000000) background
 * - White primary content
 * - Monospace typography throughout
 * - 0dp corners on all shapes (blocky/rectangular)
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
 * Access extended terminal colors from the current theme.
 * 
 * Usage:
 * ```
 * val statusColor = TerminalTheme.extendedColors.statusOnline
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
}
