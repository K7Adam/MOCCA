package com.mocca.app.ui.theme

import androidx.compose.material3.MaterialTheme
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

object AppTypography {

    val displayFamily: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.space_grotesk_regular)
        )

    val displayBold: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.space_grotesk_regular, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
        )

    val displayEmphasized: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.space_grotesk_regular, variationSettings = FontVariation.Settings(
                FontVariation.weight(800),
                FontVariation.Setting("XTRA", 400f),
                FontVariation.Setting("slnt", -10f)
            ))
        )

    val displaySemiBold: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.space_grotesk_regular, variationSettings = FontVariation.Settings(FontVariation.weight(600)))
        )

    val displayMediumFamily: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.space_grotesk_regular, variationSettings = FontVariation.Settings(FontVariation.weight(500)))
        )

    val bodyFamily = FontFamily.Default

    val monoFamily: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.jetbrains_mono_regular)
        )

    val headerLabel: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 2.sp
        )

    val sectionHeader: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 1.5.sp
        )

    val status: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayMediumFamily,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.5.sp
        )

    val code: TextStyle
        @Composable get() = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        )

    val codeSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp
        )

    val monoLabel: TextStyle
        @Composable get() = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.sp
        )

    val codeExtraSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            letterSpacing = 1.sp
        )

    val footer: TextStyle
        @Composable get() = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 2.sp
        )

    // Legacy M3 aliases (Use MaterialTheme.typography instead)

    val displayLargeEmphasized: TextStyle @Composable get() = TextStyle(fontFamily = displayEmphasized, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-1.0).sp)
    val titleMediumEmphasized: TextStyle @Composable get() = TextStyle(fontFamily = displayEmphasized, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp)
    val displayLarge: TextStyle @Composable get() = MaterialTheme.typography.displayLarge
    val displayMedium: TextStyle @Composable get() = MaterialTheme.typography.displayMedium
    val displaySmall: TextStyle @Composable get() = MaterialTheme.typography.displaySmall
    val headlineLarge: TextStyle @Composable get() = MaterialTheme.typography.headlineLarge
    val headlineMedium: TextStyle @Composable get() = MaterialTheme.typography.headlineMedium
    val headlineSmall: TextStyle @Composable get() = MaterialTheme.typography.headlineSmall
    val titleLarge: TextStyle @Composable get() = MaterialTheme.typography.titleLarge
    val titleMedium: TextStyle @Composable get() = MaterialTheme.typography.titleMedium
    val titleSmall: TextStyle @Composable get() = MaterialTheme.typography.titleSmall
    val bodyLarge: TextStyle @Composable get() = MaterialTheme.typography.bodyLarge
    val bodyMedium: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium
    val bodySmall: TextStyle @Composable get() = MaterialTheme.typography.bodySmall
    val labelLarge: TextStyle @Composable get() = MaterialTheme.typography.labelLarge
    val labelMedium: TextStyle @Composable get() = MaterialTheme.typography.labelMedium
    val labelSmall: TextStyle @Composable get() = MaterialTheme.typography.labelSmall
    val labelExtraSmall: TextStyle @Composable get() = TextStyle(fontFamily = displayMediumFamily, fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 1.sp)
}

@Composable
fun appTypography(): Typography {
    val baseline = Typography()
    return Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = AppTypography.displayBold, letterSpacing = (-1.0).sp),
        displayMedium = baseline.displayMedium.copy(fontFamily = AppTypography.displaySemiBold, letterSpacing = (-0.5).sp),
        displaySmall = baseline.displaySmall.copy(fontFamily = AppTypography.displayMediumFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = AppTypography.displayBold, letterSpacing = (-0.5).sp),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = AppTypography.displaySemiBold),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = AppTypography.displaySemiBold),
        titleLarge = baseline.titleLarge.copy(fontFamily = AppTypography.displayBold),
        titleMedium = baseline.titleMedium.copy(fontFamily = AppTypography.displaySemiBold),
        titleSmall = baseline.titleSmall.copy(fontFamily = AppTypography.displayMediumFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = AppTypography.bodyFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = AppTypography.bodyFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = AppTypography.bodyFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = AppTypography.displayBold, letterSpacing = 0.5.sp),
        labelMedium = baseline.labelMedium.copy(fontFamily = AppTypography.displayMediumFamily, letterSpacing = 0.5.sp),
        labelSmall = baseline.labelSmall.copy(fontFamily = AppTypography.displayMediumFamily, letterSpacing = 0.5.sp)
    )
}
