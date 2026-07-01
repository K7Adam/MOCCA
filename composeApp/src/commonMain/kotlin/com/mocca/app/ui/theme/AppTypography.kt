package com.mocca.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.roboto_flex_variable
import mocca.composeapp.generated.resources.roboto_regular
import mocca.composeapp.generated.resources.jetbrains_mono_regular
import mocca.composeapp.generated.resources.jetbrains_mono_bold
import mocca.composeapp.generated.resources.fira_code_regular
import mocca.composeapp.generated.resources.fira_code_bold
import mocca.composeapp.generated.resources.source_code_pro_regular
import org.jetbrains.compose.resources.Font

/**
 * MOCCA Typography — Material 3 Expressive 15 + 15 emphasized type scale.
 *
 * Brand typeface : Roboto Flex (variable) — display, headline, title, label.
 * Plain typeface : Roboto — body text.
 * Code typeface  : User-selected monospace via [LocalCodeFontFamily].
 */
@Suppress("TooManyFunctions")
object AppTypography {

    // -- Typeface families ---------------------------------------------------

    val brandFamily: FontFamily
        @Composable get() = FontFamily(Font(Res.font.roboto_flex_variable))

    val plainFamily: FontFamily
        @Composable get() = FontFamily(Font(Res.font.roboto_regular))

    val monoFamily: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.jetbrains_mono_regular),
            Font(Res.font.jetbrains_mono_bold, FontWeight.Bold),
        )

    @Composable
    fun monoFamilyFor(fontKey: String): FontFamily = when (fontKey) {
        "fira_code" -> FontFamily(
            Font(Res.font.fira_code_regular),
            Font(Res.font.fira_code_bold, FontWeight.Bold),
        )
        "source_code_pro" -> FontFamily(
            Font(Res.font.source_code_pro_regular),
        )
        "system_mono" -> FontFamily.Monospace
        else -> monoFamily
    }

    val fontPreviewText: String = "fun main() { println(42) }"

    // =========================================================================
    // M3 STANDARD — 15 type roles
    // =========================================================================

    // -- Display (brand) -----------------------------------------------------

    val displayLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W400,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
        )

    val displayMedium: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W400,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
        )

    val displaySmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W400,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
        )

    // -- Headline (brand) ----------------------------------------------------

    val headlineLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W400,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
        )

    val headlineMedium: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W400,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
        )

    val headlineSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W400,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
        )

    // -- Title (brand) -------------------------------------------------------

    val titleLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W400,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        )

    val titleMedium: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W500,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
        )

    val titleSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W500,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        )

    // -- Body (plain) --------------------------------------------------------

    val bodyLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = plainFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        )

    val bodyMedium: TextStyle
        @Composable get() = TextStyle(
            fontFamily = plainFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        )

    val bodySmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = plainFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        )

    // -- Label (brand) -------------------------------------------------------

    val labelLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W500,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        )

    val labelMedium: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W500,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        )

    val labelSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W500,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        )

    // =========================================================================
    // M3 EMPHASIZED — 15 heavier-weight variants
    // =========================================================================

    val displayLargeEmphasized: TextStyle
        @Composable get() = displayLarge.copy(fontWeight = FontWeight.W700)

    val displayMediumEmphasized: TextStyle
        @Composable get() = displayMedium.copy(fontWeight = FontWeight.W700)

    val displaySmallEmphasized: TextStyle
        @Composable get() = displaySmall.copy(fontWeight = FontWeight.W700)

    val headlineLargeEmphasized: TextStyle
        @Composable get() = headlineLarge.copy(fontWeight = FontWeight.W700)

    val headlineMediumEmphasized: TextStyle
        @Composable get() = headlineMedium.copy(fontWeight = FontWeight.W700)

    val headlineSmallEmphasized: TextStyle
        @Composable get() = headlineSmall.copy(fontWeight = FontWeight.W700)

    val titleLargeEmphasized: TextStyle
        @Composable get() = titleLarge.copy(fontWeight = FontWeight.W700)

    val titleMediumEmphasized: TextStyle
        @Composable get() = titleMedium.copy(fontWeight = FontWeight.W700)

    val titleSmallEmphasized: TextStyle
        @Composable get() = titleSmall.copy(fontWeight = FontWeight.W700)

    val bodyLargeEmphasized: TextStyle
        @Composable get() = bodyLarge.copy(fontWeight = FontWeight.W700)

    val bodyMediumEmphasized: TextStyle
        @Composable get() = bodyMedium.copy(fontWeight = FontWeight.W700)

    val bodySmallEmphasized: TextStyle
        @Composable get() = bodySmall.copy(fontWeight = FontWeight.W700)

    val labelLargeEmphasized: TextStyle
        @Composable get() = labelLarge.copy(fontWeight = FontWeight.W700)

    val labelMediumEmphasized: TextStyle
        @Composable get() = labelMedium.copy(fontWeight = FontWeight.W700)

    val labelSmallEmphasized: TextStyle
        @Composable get() = labelSmall.copy(fontWeight = FontWeight.W700)

    // =========================================================================
    // EXTENDED — code, status, and micro styles
    // =========================================================================

    val code: TextStyle
        @Composable get() = TextStyle(
            fontFamily = LocalCodeFontFamily.current,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
        )

    val codeSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = LocalCodeFontFamily.current,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp,
        )

    val status: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W500,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.5.sp,
        )

    val labelExtraSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.W500,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.5.sp,
        )

    val monoLabel: TextStyle
        @Composable get() = TextStyle(
            fontFamily = LocalCodeFontFamily.current,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.sp,
        )

    // -- Legacy alias --------------------------------------------------------

    @Deprecated("Use brandFamily", replaceWith = ReplaceWith("brandFamily"))
    val displayFamily: FontFamily
        @Composable get() = brandFamily
}

@Composable
fun appTypography(): Typography = Typography(
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
    labelSmall = AppTypography.labelSmall,
)
