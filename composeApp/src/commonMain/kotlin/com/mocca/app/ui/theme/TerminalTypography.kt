package com.mocca.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Modern MOCCA typography using system fonts with variable weights.
 * Based on UI overhaul designs using Space Grotesk aesthetic.
 * 
 * Design Language: 
 * - Display/Headers: Light to Bold weights, tight tracking
 * - Body: Regular weight, relaxed line height
 * - Labels: Medium weight, wide letter-spacing, UPPERCASE
 * - Code: Monospace (JetBrains Mono when available)
 */
object TerminalTypography {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FONT FAMILIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Display font - System Sans Serif (mimics Space Grotesk) */
    val displayFamily = FontFamily.SansSerif
    
    /** Body font - System Default */
    val bodyFamily = FontFamily.Default
    
    /** Monospace font - for code blocks */
    val monoFamily = FontFamily.Monospace
    
    // For backwards compatibility
    val fontFamily = FontFamily.SansSerif
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DISPLAY STYLES (Hero text, large headings)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Hero/display text - "Initialize" - Light weight */
    val displayLarge = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    )
    
    /** Display medium - Secondary hero text */
    val displayMedium = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
    
    /** Display small - Tertiary display */
    val displaySmall = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HEADLINE STYLES (Page titles, section headers)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Large headline - "Connection" - Bold */
    val headlineLarge = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    )
    
    /** Medium headline - Section headers */
    val headlineMedium = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
    
    /** Small headline - Card titles, subsections */
    val headlineSmall = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TITLE STYLES (List items, navigation)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Large title - Navigation items, major list items */
    val titleLarge = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    /** Medium title - List item primary text */
    val titleMedium = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )
    
    /** Small title - Compact list items */
    val titleSmall = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BODY STYLES (Content text)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Large body - Primary content text */
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
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
    
    /** Small body - Descriptions, metadata */
    val bodySmall = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LABEL STYLES (UI labels, buttons, status)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Large label - Button text, prominent labels */
    val labelLarge = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    )
    
    /** Medium label - Standard labels, tabs */
    val labelMedium = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    )
    
    /** Small label - Status badges, timestamps */
    val labelSmall = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    
    /** Extra small label - Very compact labels */
    val labelExtraSmall = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp
    )
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SPECIAL STYLES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Header label - UPPERCASE with wide tracking */
    val headerLabel = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 2.sp
    )
    
    /** Section header - UPPERCASE with tracking */
    val sectionHeader = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.5.sp
    )
    
    /** Status text - For status indicators */
    val status = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp
    )
    
    /** Code blocks - Monospace */
    val code = TextStyle(
        fontFamily = monoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )
    
    /** Code small - Smaller code text */
    val codeSmall = TextStyle(
        fontFamily = monoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
    
    /** Mono label - Monospace labels (commit hashes, etc.) */
    val monoLabel = TextStyle(
        fontFamily = monoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
    
    /** Footer text - Very small, muted */
    val footer = TextStyle(
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
fun terminalTypography(): Typography = Typography(
    displayLarge = TerminalTypography.displayLarge,
    displayMedium = TerminalTypography.displayMedium,
    displaySmall = TerminalTypography.displaySmall,
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
