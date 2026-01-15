package com.mocca.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.jetbrains_mono_bold
import mocca.composeapp.generated.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

/**
 * Terminal typography using monospace fonts throughout.
 * Follows mockup specifications: uppercase headers, monospace everywhere.
 */
object TerminalTypography {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FONT FAMILY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Monospace font family for terminal aesthetic */
    val fontFamily = FontFamily.Monospace
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RAW STYLES (for direct use outside MaterialTheme)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Hero/display text - Status monitor "PROBING_HOST..." */
    val displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = 2.sp
    )
    
    /** Large title - "OPENCODE_TERM" */
    val headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 1.sp
    )
    
    /** Section headers - "[ CONTEXT_INFO ]" */
    val headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    
    /** Card titles - "MCP_CONFIG" */
    val headlineSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp
    )
    
    /** Primary body text - Chat messages */
    val bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    /** Secondary body text - Session descriptions */
    val bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
    
    /** Small body text - Console logs, meta info */
    val bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
    
    /** Label - Status bar "MODEL: CLAUDE OPUS 4.5" */
    val labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 1.sp
    )
    
    /** Medium label - Button text "[ CONNECT ]" */
    val labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    )
    
    /** Small label - Timestamps, meta "SYS_BOOT_SEQ_892" */
    val labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp
    )
    
    /** Title for navigation items */
    val titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
    
    /** Title for list items */
    val titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    /** Title for compact items */
    val titleSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
    
    /** Code blocks - Monospace, small */
    val code = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
}

/**
 * Creates Material3 Typography using terminal monospace fonts.
 */
@Composable
fun terminalTypography(): Typography = Typography(
    displayLarge = TerminalTypography.displayLarge,
    displayMedium = TerminalTypography.headlineLarge, // Reuse for medium display
    displaySmall = TerminalTypography.headlineMedium,
    headlineLarge = TerminalTypography.headlineLarge,
    headlineMedium = TerminalTypography.headlineMedium,
    headlineSmall = TerminalTypography.headlineSmall,
    titleLarge = TerminalTypography.titleLarge,
    titleMedium = TerminalTypography.titleMedium,
    titleSmall = TerminalTypography.titleSmall,
    bodyLarge = TerminalTypography.bodyLarge,
    bodyMedium = TerminalTypography.bodyMedium,
    bodySmall = TerminalTypography.bodySmall,
    labelLarge = TerminalTypography.labelLarge,
    labelMedium = TerminalTypography.labelMedium,
    labelSmall = TerminalTypography.labelSmall
)
