package com.mocca.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// ═══════════════════════════════════════════════════════════════════════════
// MODERN COLOR SCHEME (Dark Mode Only for now)
// ═══════════════════════════════════════════════════════════════════════════

private val ModernDarkColorScheme = darkColorScheme(
    primary = ModernColors.primary,
    onPrimary = ModernColors.textPrimary,
    primaryContainer = ModernColors.surfaceElevated,
    onPrimaryContainer = ModernColors.primary,
    
    secondary = ModernColors.secondary,
    onSecondary = ModernColors.textPrimary,
    secondaryContainer = ModernColors.surfaceElevated,
    onSecondaryContainer = ModernColors.secondary,
    
    tertiary = ModernColors.success,
    onTertiary = ModernColors.background,
    tertiaryContainer = ModernColors.surfaceElevated,
    onTertiaryContainer = ModernColors.success,
    
    background = ModernColors.background,
    onBackground = ModernColors.textPrimary,
    
    surface = ModernColors.surface,
    onSurface = ModernColors.textPrimary,
    surfaceVariant = ModernColors.surfaceElevated,
    onSurfaceVariant = ModernColors.textSecondary,
    
    error = ModernColors.error,
    onError = ModernColors.textPrimary,
    errorContainer = ModernColors.surfaceElevated,
    onErrorContainer = ModernColors.error,
    
    outline = ModernColors.border,
    outlineVariant = ModernColors.surfaceHighlight
)

// ═══════════════════════════════════════════════════════════════════════════
// MODERN TYPOGRAPHY
// ═══════════════════════════════════════════════════════════════════════════

// Use System Default Font Family for modern look (Inter-like)
private val ModernTypography = Typography() 

// ═══════════════════════════════════════════════════════════════════════════
// EXTENDED ATTRIBUTES (For consistency)
// ═══════════════════════════════════════════════════════════════════════════

@Immutable
data class ModernExtendedColors(
    val success: Color = ModernColors.success,
    val warning: Color = ModernColors.warning,
    val textTertiary: Color = ModernColors.textTertiary,
    val textDisabled: Color = ModernColors.textDisabled,
    val border: Color = ModernColors.border,
    val focusRing: Color = ModernColors.focusRing
)

val LocalModernExtendedColors = staticCompositionLocalOf { ModernExtendedColors() }

// ═══════════════════════════════════════════════════════════════════════════
// THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ModernTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Default to system, but we mainly support dark
    content: @Composable () -> Unit
) {
    // For now, force dark theme as the "Modern" look is designed around dark
    val colorScheme = ModernDarkColorScheme
    
    val extendedColors = ModernExtendedColors()

    CompositionLocalProvider(
        LocalModernExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ModernTypography,
            content = content
        )
    }
}

object ModernTheme {
    val extendedColors: ModernExtendedColors
        @Composable
        get() = LocalModernExtendedColors.current
}
