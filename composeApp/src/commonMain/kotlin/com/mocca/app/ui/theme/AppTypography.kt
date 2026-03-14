package com.mocca.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.space_grotesk_regular
import mocca.composeapp.generated.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

/**
 * MOCCA Typography — Material 3 Expressive 15-token scale.
 * 
 * Uses Space Grotesk for display/headlines and JetBrains Mono for code.
 */
object AppTypography {

    // ---------------------------------------------------------------------------
    // FONT FAMILIES
    // ---------------------------------------------------------------------------

    val displayFamily: FontFamily
        @Composable get() = FontFamily(Font(Res.font.space_grotesk_regular))

    val monoFamily: FontFamily
        @Composable get() = FontFamily(Font(Res.font.jetbrains_mono_regular))

    // ---------------------------------------------------------------------------
    // M3 TYPOGRAPHY TOKENS (15-token scale)
    // ---------------------------------------------------------------------------

    val displayLarge: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W800,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp
    )

    val displayMedium: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W700,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.25).sp
    )

    val displaySmall: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W600,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp
    )

    val headlineLarge: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W700,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )

    val headlineMedium: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )

    val headlineSmall: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W500,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )

    val titleLarge: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W600,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )

    val titleMedium: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )

    val titleSmall: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    val bodyLarge: TextStyle @Composable get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )

    val bodyMedium: TextStyle @Composable get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )

    val bodySmall: TextStyle @Composable get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )

    val labelLarge: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    val labelMedium: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )

    val labelSmall: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )

    val displayLargeEmphasized: TextStyle @Composable get() = displayLarge.copy(
        fontWeight = FontWeight.W900,
        letterSpacing = (-1.5).sp
    )

    // ---------------------------------------------------------------------------
    // APP SPECIFIC STYLES (Custom Tokens)
    // ---------------------------------------------------------------------------

    val code: TextStyle @Composable get() = TextStyle(
        fontFamily = monoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

    val codeSmall: TextStyle @Composable get() = TextStyle(
        fontFamily = monoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )

    val status: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W500,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp
    )

    val labelExtraSmall: TextStyle @Composable get() = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.W500,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp
    )

    val monoLabel: TextStyle @Composable get() = TextStyle(
        fontFamily = monoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
}

@Composable
fun appTypography(): Typography {
    return Typography(
        displayLarge = AppTypography.displayLarge,
        displayMedium = AppTypography.displayMedium,
        displaySmall = AppTypography.displaySmall,
        headlineLarge = AppTypography.headlineLarge,
        headlineMedium = AppTypography.headlineMedium,
        headlineSmall = AppTypography.headlineSmall,
        titleLarge = AppTypography.titleLarge,
        titleMedium = AppTypography.titleMedium,
        titleSmall = AppTypography.titleSmall,
        bodyLarge = AppTypography.bodyLarge,
        bodyMedium = AppTypography.bodyMedium,
        bodySmall = AppTypography.bodySmall,
        labelLarge = AppTypography.labelLarge,
        labelMedium = AppTypography.labelMedium,
        labelSmall = AppTypography.labelSmall
    )
}
