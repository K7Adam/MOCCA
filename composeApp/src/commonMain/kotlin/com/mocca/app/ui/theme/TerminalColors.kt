package com.mocca.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Terminal/TUI color palette for the MOCCA app.
 * Follows mockup specifications: pitch black background, white primary, grey accents.
 */
@Immutable
object TerminalColors {
    // ═══════════════════════════════════════════════════════════════════════════
    // BACKGROUNDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Pure black background - main app background */
    val background = Color(0xFF000000)
    
    /** Slightly elevated surface - for cards and panels */
    val surface = Color(0xFF0A0A0A)
    
    /** Surface variant - for nested containers */
    val surfaceVariant = Color(0xFF111111)
    
    /** Surface container - for input fields, code blocks */
    val surfaceContainer = Color(0xFF1A1A1A)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMARY CONTENT (WHITE SCALE)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Primary white - main text, borders, active elements */
    val white = Color(0xFFFFFFFF)
    
    /** Slightly dimmed white - secondary text */
    val whiteDim = Color(0xFFE0E0E0)
    
    /** Muted white - tertiary text, placeholders */
    val whiteMuted = Color(0xFFB0B0B0)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GREY SCALE (SECONDARY/META)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Dark grey - very dim text, meta info */
    val greyDark = Color(0xFF333333)
    
    /** Medium grey - disabled text, subtle borders */
    val grey = Color(0xFF666666)
    
    /** Light grey - secondary labels, timestamps */
    val greyLight = Color(0xFF888888)
    
    /** Border grey - for container outlines */
    val border = Color(0xFF2A2A2A)
    
    /** Border light - for prominent outlines (2dp white border in mockups) */
    val borderLight = Color(0xFFFFFFFF)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUTTON COLORS (from mockups)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Button background - light grey as per mockup */
    val buttonBackground = Color(0xFFD9D9D9)
    
    /** Button text - black on light grey button */
    val buttonText = Color(0xFF000000)
    
    /** Send button - solid white */
    val sendButton = Color(0xFFFFFFFF)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS COLORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Online/Connected - green indicator */
    val statusOnline = Color(0xFF00FF00)
    
    /** Offline/Disconnected - red */
    val statusOffline = Color(0xFFFF3333)
    
    /** Waiting/Connecting - yellow/amber */
    val statusWaiting = Color(0xFFFFAA00)
    
    /** Success - bright green */
    val success = Color(0xFF44FF44)
    
    /** Error - red */
    val error = Color(0xFFFF4444)
    
    /** Warning - orange */
    val warning = Color(0xFFFFAA00)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CODE/SYNTAX COLORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Code addition - green background */
    val diffAddition = Color(0xFF003300)
    
    /** Code deletion - red background */
    val diffDeletion = Color(0xFF330000)
    
    /** Code addition text */
    val diffAdditionText = Color(0xFF44FF44)
    
    /** Code deletion text */
    val diffDeletionText = Color(0xFFFF4444)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BADGE COLORS (from mockups - USER/AGENT labels)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Badge background - white */
    val badgeBackground = Color(0xFFFFFFFF)
    
    /** Badge text - black */
    val badgeText = Color(0xFF000000)
    
    /** Active indicator - left bar on selected items */
    val activeIndicator = Color(0xFFFFFFFF)
}

/**
 * Extended terminal colors accessible via CompositionLocal.
 * Use for colors not covered by MaterialTheme.colorScheme.
 */
@Immutable
data class ExtendedTerminalColors(
    val buttonBackground: Color = TerminalColors.buttonBackground,
    val buttonText: Color = TerminalColors.buttonText,
    val statusOnline: Color = TerminalColors.statusOnline,
    val statusOffline: Color = TerminalColors.statusOffline,
    val statusWaiting: Color = TerminalColors.statusWaiting,
    val diffAddition: Color = TerminalColors.diffAddition,
    val diffDeletion: Color = TerminalColors.diffDeletion,
    val diffAdditionText: Color = TerminalColors.diffAdditionText,
    val diffDeletionText: Color = TerminalColors.diffDeletionText,
    val badgeBackground: Color = TerminalColors.badgeBackground,
    val badgeText: Color = TerminalColors.badgeText,
    val activeIndicator: Color = TerminalColors.activeIndicator,
    val border: Color = TerminalColors.border,
    val borderLight: Color = TerminalColors.borderLight,
    val greyDark: Color = TerminalColors.greyDark,
    val grey: Color = TerminalColors.grey,
    val greyLight: Color = TerminalColors.greyLight,
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedTerminalColors() }
