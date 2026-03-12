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
 * MOCCA Typography - System fonts with variable weights.
 * Modern design language using Space Grotesk aesthetic.
 * 
 * Note: Uses Variable Font Axes (wght) for emphasized hierarchy.
 */
object AppTypography {

    // ═══════════════════════════════════════════════════════════════════════════
    // FONT FAMILIES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Base Display font - Space Grotesk */
    val displayFamily: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.space_grotesk_regular)
        )

    /** Variable Display families for different weights */
    val displayBold: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.space_grotesk_regular, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
        )

    /** Variable Display family for Emphasized styles (Weight, XTRA, Slant) */
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

    val displayMedium: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.space_grotesk_regular, variationSettings = FontVariation.Settings(FontVariation.weight(500)))
        )

    /** Body font - System Default */
    val bodyFamily = FontFamily.Default

    /** Monospace font - JetBrains Mono Variable */
    val monoFamily: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.jetbrains_mono_regular)
        )

    // ═══════════════════════════════════════════════════════════════════════════
    // DISPLAY STYLES (Hero text, large headings)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Hero/display text - "Initialize" - High emphasis weight */
    val displayLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayBold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-1.0).sp
        )

    /** Hero/display text - Emphasized using variable axes */
    val displayLargeEmphasized: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayEmphasized,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-1.0).sp
        )

    /** Display medium - Secondary hero text */
    val displayMediumStyle: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displaySemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp
        )

    /** Display small - Tertiary display */
    val displaySmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayMedium,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.sp
        )

    // ═══════════════════════════════════════════════════════════════════════════
    // HEADLINE STYLES (Page titles, section headers)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Large headline - "Connection" - Bold */
    val headlineLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.5).sp
        )

    /** Medium headline - Section headers */
    val headlineMedium: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displaySemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp
        )

    /** Small headline - Card titles, subsections */
    val headlineSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displaySemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        )

    // ═══════════════════════════════════════════════════════════════════════════
    // TITLE STYLES (List items, navigation)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Large title - Navigation items, major list items */
    val titleLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp
        )

    /** Medium title - List item primary text */
    val titleMedium: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displaySemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        )

    /** Medium title - Emphasized list items / critical info */
    val titleMediumEmphasized: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayEmphasized,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        )

    /** Small title - Compact list items */
    val titleSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayMedium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp
        )

    // ═══════════════════════════════════════════════════════════════════════════
    // BODY STYLES (Content text)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Large body - Primary content text (chat messages, main content) */
    val bodyLarge = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )

    /** Medium body - Secondary content */
    val bodyMedium = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    /** Small body - Descriptions, metadata */
    val bodySmall = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // LABEL STYLES (UI labels, buttons, status)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Large label - Button text, prominent labels */
    val labelLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.5.sp
        )

    /** Medium label - Standard labels, tabs */
    val labelMedium: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayMedium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )

    /** Small label - Status badges, timestamps */
    val labelSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayMedium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.5.sp
        )

    /** Extra small label - Very compact labels */
    val labelExtraSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayMedium,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            letterSpacing = 1.sp
        )

    // ═══════════════════════════════════════════════════════════════════════════
    // SPECIAL STYLES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Header label - UPPERCASE with wide tracking */
    val headerLabel: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 2.sp
        )

    /** Section header - UPPERCASE with tracking */
    val sectionHeader: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 1.5.sp
        )

    /** Status text - For status indicators */
    val status: TextStyle
        @Composable get() = TextStyle(
            fontFamily = displayMedium,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.5.sp
        )

    /** Code blocks - Monospace */
    val code: TextStyle
        @Composable get() = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        )

    /** Code small - Smaller code text */
    val codeSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp
        )

    /** Mono label - Monospace labels (commit hashes, etc.) */
    val monoLabel: TextStyle
        @Composable get() = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.sp
        )

    /** Code extra small - For tiny boot sequences */
    val codeExtraSmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            letterSpacing = 1.sp
        )

    /** Footer text - Very small, muted */
    val footer: TextStyle
        @Composable get() = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 2.sp
        )
}

/**
 * Creates Material3 Typography using modern font styles.
 */
@Composable
fun appTypography(): Typography = Typography(
    displayLarge = AppTypography.displayLarge,
    displayMedium = AppTypography.displayMediumStyle,
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