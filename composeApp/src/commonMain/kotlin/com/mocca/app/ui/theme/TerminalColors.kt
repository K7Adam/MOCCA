package com.mocca.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Modern MOCCA color palette - Pitch black with glassmorphic accents.
 * Based on UI overhaul designs from /ui_overhaul_refactoring/
 * 
 * Design Language: Modern dark mode with mint green accents, soft grays, and subtle transparency.
 */
@Immutable
object TerminalColors {
    // ═══════════════════════════════════════════════════════════════════════════
    // BACKGROUNDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Pure black background - main app background (OLED-friendly) */
    val background = Color(0xFF000000)
    
    /** Near-black background variant - for slight elevation */
    val backgroundVariant = Color(0xFF050505)
    
    /** Slightly elevated surface - for cards and panels */
    val surface = Color(0xFF0A0A0A)
    
    /** Surface variant - for nested containers, code blocks */
    val surfaceVariant = Color(0xFF111111)
    
    /** Surface container - for input fields, elevated cards */
    val surfaceContainer = Color(0xFF1A1A1A)
    
    /** Surface elevated - for floating elements, dialogs */
    val surfaceElevated = Color(0xFF1C1C1E)
    
    /** Card background - for card containers */
    val cardBackground = Color(0xFF121212)
    
    /** Card highlight - for active/highlighted cards */
    val cardHighlight = Color(0xFF1E1E1E)
    
    /** Module/tool card background */
    val moduleBackground = Color(0xFF111111)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMARY CONTENT (WHITE SCALE)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Primary white - main text, borders, active elements */
    val white = Color(0xFFFFFFFF)
    
    /** Primary text - same as white for consistency */
    val textPrimary = Color(0xFFFFFFFF)
    
    /** Slightly dimmed white - secondary text */
    val whiteDim = Color(0xFFE0E0E0)
    
    /** Secondary text - for subtitles and descriptions */
    val textSecondary = Color(0xFF888888)
    
    /** Muted white - tertiary text, placeholders */
    val whiteMuted = Color(0xFFB0B0B0)
    
    /** Tertiary text - for timestamps, meta info */
    val textTertiary = Color(0xFF666666)
    
    /** Placeholder text color */
    val textPlaceholder = Color(0xFF555555)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GREY SCALE (SECONDARY/META)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Dark grey - very dim text, meta info */
    val greyDark = Color(0xFF333333)
    
    /** Medium grey - disabled text, subtle borders */
    val grey = Color(0xFF666666)
    
    /** Light grey - secondary labels, timestamps */
    val greyLight = Color(0xFF888888)
    
    /** Extra light grey - for very subtle elements */
    val greyExtraLight = Color(0xFF9CA3AF)
    
    /** Border grey - for container outlines */
    val border = Color(0xFF2A2A2A)
    
    /** Border light - for prominent outlines */
    val borderLight = Color(0xFF333333)
    
    /** Border white - for high-contrast borders */
    val borderWhite = Color(0xFFFFFFFF)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ACCENT COLORS (GREEN - PRIMARY ACCENT)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Primary accent green - status indicators, success states */
    val accentGreen = Color(0xFF00D9A5)
    
    /** Accent green variant - for glow effects */
    val accentGreenBright = Color(0xFF10B981)
    
    /** Accent green saturated - terminal green */
    val accentGreenTerminal = Color(0xFF00FF41)
    
    /** Listening/Active indicator green */
    val greenIndicator = Color(0xFF22C55E)
    
    /** Emerald green - for status dots */
    val emerald = Color(0xFF10B981)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUTTON COLORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Primary button background - off-white/light grey */
    val buttonBackground = Color(0xFFF2F2F2)
    
    /** Primary button text - black on light button */
    val buttonText = Color(0xFF000000)
    
    /** Secondary button background */
    val buttonSecondary = Color(0xFF2A2A2A)
    
    /** Send button / FAB - solid white */
    val sendButton = Color(0xFFFFFFFF)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS COLORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Online/Connected - mint green */
    val statusOnline = accentGreen
    
    /** Offline/Disconnected - red */
    val statusOffline = Color(0xFFFF453A)
    
    /** Waiting/Connecting - amber/orange */
    val statusWaiting = Color(0xFFFFAA00)
    
    /** Thinking/Reasoning - purple/magenta for AI thinking */
    val statusThinking = Color(0xFFE879F9)
    
    /** Processing indicator */
    val statusProcessing = Color(0xFF00D9A5)
    
    /** Success - mint green */
    val success = accentGreen
    
    /** Error - alert red */
    val error = Color(0xFFFF453A)
    
    /** Warning - orange */
    val warning = Color(0xFFFFAA00)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ALERT COLORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Alert red - for error banners */
    val alertRed = Color(0xFFFF453A)
    
    /** Alert red dim - for backgrounds */
    val alertRedDim = Color(0x1AFF453A)
    
    /** Alert border gradient start */
    val alertBorderStart = Color(0xFFFF4444)
    
    /** Alert border gradient end */
    val alertBorderEnd = Color(0xFFFF8C00)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ACCENT COLORS (BLUE - SECONDARY)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Primary blue - for Git, active tabs */
    val primary = Color(0xFF3B82F6)
    
    /** Primary blue dim - for badges, containers */
    val primaryDim = Color(0xFF1E3A5F)
    
    /** React/TSX file color */
    val fileTsx = Color(0xFF61DAFB)
    
    /** CSS file color */
    val fileCss = Color(0xFFBB86FC)
    
    /** JSON file color */
    val fileJson = Color(0xFFFF5252)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CODE/SYNTAX COLORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Code addition - green background */
    val diffAddition = Color(0xFF003300)
    
    /** Code deletion - red background */
    val diffDeletion = Color(0xFF330000)
    
    /** Code addition text */
    val diffAdditionText = accentGreen
    
    /** Code deletion text */
    val diffDeletionText = Color(0xFFFF453A)
    
    /** Syntax - keywords (purple/magenta) */
    val syntaxKeyword = Color(0xFFC586C0)
    
    /** Syntax - functions (yellow) */
    val syntaxFunction = Color(0xFFDCDCAA)
    
    /** Syntax - strings (orange/coral) */
    val syntaxString = Color(0xFFCE9178)
    
    /** Syntax - types (purple) */
    val syntaxType = Color(0xFFBF5AF2)
    
    /** Syntax - comments (gray) */
    val syntaxComment = Color(0xFF6A9955)
    
    /** Syntax - punctuation */
    val syntaxPunctuation = Color(0xFFD4D4D4)
    
    /** Line numbers */
    val lineNumbers = Color(0xFF636D83)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BADGE COLORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Badge background - white */
    val badgeBackground = Color(0xFFFFFFFF)
    
    /** Badge text - black */
    val badgeText = Color(0xFF000000)
    
    /** Active indicator - left bar on selected items */
    val activeIndicator = Color(0xFFFFFFFF)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GLASSMORPHISM / OVERLAY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Glass panel background - semi-transparent dark */
    val glassBackground = Color(0x99141414)
    
    /** Glass border - subtle white */
    val glassBorder = Color(0x0DFFFFFF)
    
    /** Scrim overlay */
    val scrim = Color(0xCC000000)
    
    /** Input glow focus color */
    val inputGlow = Color(0x0DFFFFFF)
}

/**
 * Extended terminal colors accessible via CompositionLocal.
 * Use for colors not covered by MaterialTheme.colorScheme.
 */
@Immutable
data class ExtendedTerminalColors(
    // Button colors
    val buttonBackground: Color = TerminalColors.buttonBackground,
    val buttonText: Color = TerminalColors.buttonText,
    val buttonSecondary: Color = TerminalColors.buttonSecondary,
    
    // Status colors
    val statusOnline: Color = TerminalColors.statusOnline,
    val statusOffline: Color = TerminalColors.statusOffline,
    val statusWaiting: Color = TerminalColors.statusWaiting,
    val statusThinking: Color = TerminalColors.statusThinking,
    val statusProcessing: Color = TerminalColors.statusProcessing,
    
    // Diff colors
    val diffAddition: Color = TerminalColors.diffAddition,
    val diffDeletion: Color = TerminalColors.diffDeletion,
    val diffAdditionText: Color = TerminalColors.diffAdditionText,
    val diffDeletionText: Color = TerminalColors.diffDeletionText,
    
    // Badge colors
    val badgeBackground: Color = TerminalColors.badgeBackground,
    val badgeText: Color = TerminalColors.badgeText,
    val activeIndicator: Color = TerminalColors.activeIndicator,
    
    // Border colors
    val border: Color = TerminalColors.border,
    val borderLight: Color = TerminalColors.borderLight,
    val borderWhite: Color = TerminalColors.borderWhite,
    
    // Grey scale
    val greyDark: Color = TerminalColors.greyDark,
    val grey: Color = TerminalColors.grey,
    val greyLight: Color = TerminalColors.greyLight,
    
    // Accent colors
    val accentGreen: Color = TerminalColors.accentGreen,
    val accentGreenBright: Color = TerminalColors.accentGreenBright,
    val primary: Color = TerminalColors.primary,
    val primaryDim: Color = TerminalColors.primaryDim,
    
    // Alert colors
    val alertRed: Color = TerminalColors.alertRed,
    val alertRedDim: Color = TerminalColors.alertRedDim,
    
    // Card colors
    val cardBackground: Color = TerminalColors.cardBackground,
    val cardHighlight: Color = TerminalColors.cardHighlight,
    val moduleBackground: Color = TerminalColors.moduleBackground,
    
    // Glass colors
    val glassBackground: Color = TerminalColors.glassBackground,
    val glassBorder: Color = TerminalColors.glassBorder,
    
    // Syntax colors
    val syntaxKeyword: Color = TerminalColors.syntaxKeyword,
    val syntaxFunction: Color = TerminalColors.syntaxFunction,
    val syntaxString: Color = TerminalColors.syntaxString,
    val syntaxType: Color = TerminalColors.syntaxType,
    val syntaxComment: Color = TerminalColors.syntaxComment,
    
    // File type colors
    val fileTsx: Color = TerminalColors.fileTsx,
    val fileCss: Color = TerminalColors.fileCss,
    val fileJson: Color = TerminalColors.fileJson,
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedTerminalColors() }
