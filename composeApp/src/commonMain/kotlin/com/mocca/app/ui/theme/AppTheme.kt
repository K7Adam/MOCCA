package com.mocca.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * MOCCA App Theme — Material 3 Expressive with Mocha tonal palette.
 *
 * Seed: Mocha #6F4E37 (HCT H=54, C=20, T=36)
 * Palette generated via material-color-utilities TonalSpot variant.
 *
 * Features:
 * - Mocha-themed M3 tonal palette (warm brown primary).
 * - Light / dark scheme toggle via [darkTheme].
 * - Dynamic color hook for Android 12+ (pass platform-resolved scheme).
 * - Full 15+15 expressive typography scale.
 * - Expressive motion gated by [AppPerformance].
 */

// ---------------------------------------------------------------------------
// Mocha Dark ColorScheme — seed #6F4E37, TonalSpot, dark
// ---------------------------------------------------------------------------
private val MochaDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB784),
    onPrimary = Color(0xFF3D1B00),
    primaryContainer = Color(0xFF9B5E2F),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF7E4619),
    secondary = Color(0xFFE4BFA8),
    onSecondary = Color(0xFF351F10),
    secondaryContainer = Color(0xFF846653),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFC8CA94),
    onTertiary = Color(0xFF242602),
    tertiaryContainer = Color(0xFF6D6F42),
    onTertiaryContainer = Color(0xFFFFFFFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF520003),
    errorContainer = Color(0xFFCF2C27),
    onErrorContainer = Color(0xFFFFFFFF),
    background = Color(0xFF19120D),
    onBackground = Color(0xFFFBEBE1),
    surface = Color(0xFF19120D),
    onSurface = Color(0xFFFBEBE1),
    surfaceVariant = Color(0xFF52443B),
    onSurfaceVariant = Color(0xFFD7C3B7),
    outline = Color(0xFFA9978D),
    outlineVariant = Color(0xFF796960),
    surfaceDim = Color(0xFF19120D),
    surfaceBright = Color(0xFF473D37),
    surfaceContainerLowest = Color(0xFF100A06),
    surfaceContainerLow = Color(0xFF231B16),
    surfaceContainer = Color(0xFF2B221C),
    surfaceContainerHigh = Color(0xFF362C27),
    surfaceContainerHighest = Color(0xFF413731),
    inverseSurface = Color(0xFFF0DFD6),
    inverseOnSurface = Color(0xFF382F29),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFFFB784),
)

// ---------------------------------------------------------------------------
// Mocha Light ColorScheme — seed #6F4E37, TonalSpot, light
// ---------------------------------------------------------------------------
private val MochaLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF703B0E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC78250),
    onPrimaryContainer = Color(0xFF280F00),
    inversePrimary = Color(0xFFFFB784),
    secondary = Color(0xFF5E4432),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFAC8B76),
    onSecondaryContainer = Color(0xFF241104),
    tertiary = Color(0xFF4A4C22),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF939563),
    onTertiaryContainer = Color(0xFF161700),
    error = Color(0xFF98000A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF574B),
    onErrorContainer = Color(0xFF360001),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A15),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF3DED3),
    onSurfaceVariant = Color(0xFF52443B),
    outline = Color(0xFF716258),
    outlineVariant = Color(0xFFA08E84),
    surfaceDim = Color(0xFFDDCDC4),
    surfaceBright = Color(0xFFFFF8F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EA),
    surfaceContainer = Color(0xFFF9E8DF),
    surfaceContainerHigh = Color(0xFFF0DFD6),
    surfaceContainerHighest = Color(0xFFE7D7CE),
    inverseSurface = Color(0xFF382F29),
    inverseOnSurface = Color(0xFFFEEDE4),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF8A5022),
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
 * @param darkTheme Whether to use the dark color scheme. Defaults to system setting.
 * @param dynamicColorScheme Optional platform-resolved dynamic color scheme (Android 12+).
 *   When non-null, takes precedence over the static Mocha schemes.
 * @param performance Device performance tier for motion/effect gating.
 * @param codeFontFamilyKey User preference key for the code font.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColorScheme: ColorScheme? = null,
    performance: AppPerformance = AppPerformance(),
    codeFontFamilyKey: () -> String = { "jetbrains_mono" },
    content: @Composable () -> Unit,
) {
    val colorScheme = dynamicColorScheme ?: if (darkTheme) MochaDarkColorScheme else MochaLightColorScheme
    val codeFontFamily = AppTypography.monoFamilyFor(codeFontFamilyKey())

    CompositionLocalProvider(
        LocalAppPerformance provides performance,
        LocalCodeFontFamily provides codeFontFamily,
    ) {
        val motionScheme = if (performance.useExpressiveMotion) {
            MotionScheme.expressive()
        } else {
            MotionScheme.standard()
        }

        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = appTypography(),
            shapes = appShapes(),
            motionScheme = motionScheme,
            content = content,
        )
    }
}

/**
 * MOCCA Theme — Unified access to Material 3 tokens and extended app attributes.
 */
object MoccaTheme {
    val spacing: AppSpacing
        get() = AppSpacing

    val typography: AppTypography
        get() = AppTypography

    val shapes: AppShapes
        get() = AppShapes

    val colors: AppColors
        get() = AppColors
}
