package com.mocca.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.font.FontFamily

/**
 * MOCCA App Theme — Material 3 Expressive.
 *
 * Features:
 * - True M3 tonal palette with soft dark foundation.
 * - Desaturated cool gray-blue accent.
 * - Full 15-token expressive typography scale.
 * - Expressive motion and squircle-based shapes.
 */

private val AppColorScheme: ColorScheme = darkColorScheme(
    primary = AppColors.primary,
    onPrimary = AppColors.onPrimary,
    primaryContainer = AppColors.primaryContainer,
    onPrimaryContainer = AppColors.onPrimaryContainer,
    inversePrimary = AppColors.inversePrimary,
    
    secondary = AppColors.secondary,
    onSecondary = AppColors.onSecondary,
    secondaryContainer = AppColors.secondaryContainer,
    onSecondaryContainer = AppColors.onSecondaryContainer,
    
    tertiary = AppColors.AnchorTertiary,
    onTertiary = AppColors.onPrimary,
    tertiaryContainer = AppColors.primaryContainer,
    onTertiaryContainer = AppColors.onPrimaryContainer,
    
    background = AppColors.background,
    onBackground = AppColors.onBackground,
    
    surface = AppColors.surface,
    onSurface = AppColors.onSurface,
    surfaceVariant = AppColors.surfaceVariant,
    onSurfaceVariant = AppColors.onSurfaceVariant,
    
    surfaceDim = AppColors.surfaceDim,
    surfaceBright = AppColors.surfaceBright,
    surfaceContainerLowest = AppColors.surfaceContainerLowest,
    surfaceContainerLow = AppColors.surfaceContainerLow,
    surfaceContainer = AppColors.surfaceContainer,
    surfaceContainerHigh = AppColors.surfaceContainerHigh,
    surfaceContainerHighest = AppColors.surfaceContainerHighest,
    
    outline = AppColors.outline,
    outlineVariant = AppColors.outlineVariant,
    
    error = AppColors.error,
    onError = AppColors.onError,
    errorContainer = AppColors.errorContainer,
    onErrorContainer = AppColors.onErrorContainer,
    
    scrim = AppColors.scrim
)

/**
 * CompositionLocal for user-selected code font family.
 * Provided by AppTheme based on user preferences.
 */
val LocalCodeFontFamily = androidx.compose.runtime.staticCompositionLocalOf<FontFamily> {
    FontFamily.Monospace
}

/**
 * MOCCA App Theme.
 * 
 * @param codeFontFamilyKey The user preference key for the code font (e.g., "jetbrains_mono", "fira_code")
 */
@Composable
fun AppTheme(
    performance: AppPerformance = AppPerformance(),
    codeFontFamilyKey: () -> String = { "jetbrains_mono" },
    content: @Composable () -> Unit
) {
    val extendedColors = ExtendedAppColors()
    val codeFontFamily = AppTypography.monoFamilyFor(codeFontFamilyKey())

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalAppPerformance provides performance,
        LocalCodeFontFamily provides codeFontFamily
    ) {
        val motionScheme = if (performance.useExpressiveMotion) {
            MotionScheme.expressive()
        } else {
            MotionScheme.standard()
        }

        MaterialExpressiveTheme(
            colorScheme = AppColorScheme,
            typography = appTypography(),
            shapes = appShapes(),
            motionScheme = motionScheme,
            content = content
        )
    }
}

/**
 * MOCCA Theme — Unified access to Material 3 tokens and extended app attributes.
 */
object MoccaTheme {
    val extendedColors: ExtendedAppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current

    val spacing: AppSpacing
        get() = AppSpacing

    val typography: AppTypography
        get() = AppTypography

    val shapes: AppShapes
        get() = AppShapes

    val colors: AppColors
        get() = AppColors
}
