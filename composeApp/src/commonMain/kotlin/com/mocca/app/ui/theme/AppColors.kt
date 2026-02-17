package com.mocca.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * MOCCA Color Palette - Terminal Theme
 * Pitch Black OLED theme with Mint Green accents
 */
@Immutable
object AppColors {
    // ═══════════════════════════════════════════════════════════════════════════
    // BACKGROUNDS (Pitch Black for OLED)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Main app background - Pure black for OLED */
    val background = Color(0xFF000000)

    /** Near-black background variant */
    val backgroundVariant = Color(0xFF0A0A0A)

    /** Slightly elevated surface */
    val surface = Color(0xFF121212)

    /** Surface variant */
    val surfaceVariant = Color(0xFF1A1A1A)

    /** Surface container */
    val surfaceContainer = Color(0xFF1E1E1E)

    /** Surface elevated */
    val surfaceElevated = Color(0xFF252525)

    /** Card background */
    val cardBackground = Color(0xFF1A1A1A)

    /** Card highlight */
    val cardHighlight = Color(0xFF252525)

    /** Module/tool card background */
    val moduleBackground = Color(0xFF1E1E1E)

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMARY CONTENT
    // ═══════════════════════════════════════════════════════════════════════════

    val white = Color(0xFFFFFFFF)
    val textPrimary = Color(0xFFFFFFFF)
    val whiteDim = Color(0xFFB0B0B0)
    val textSecondary = Color(0xFFB0B0B0)
    val whiteMuted = Color(0xFF808080)
    val textTertiary = Color(0xFF808080)
    val textPlaceholder = Color(0xFF666666)

    // ═══════════════════════════════════════════════════════════════════════════
    // GREY SCALE
    // ═══════════════════════════════════════════════════════════════════════════

    val greyDark = Color(0xFF333333)
    val grey = Color(0xFF666666)
    val greyLight = Color(0xFF999999)
    val greyExtraLight = Color(0xFFB0B0B0)
    val border = Color(0xFF333333)
    val borderLight = Color(0xFF444444)
    val borderWhite = Color(0xFFFFFFFF)

    // ═══════════════════════════════════════════════════════════════════════════
    // ACCENT COLORS (Mint Green Terminal Theme)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Primary accent - Mint Green */
    val accentGreen = Color(0xFF00D9A5)
    
    /** Accent variant - Bright Mint */
    val accentGreenBright = Color(0xFF00FFB3)

    /** Terminal green */
    val accentGreenTerminal = Color(0xFF00D9A5)

    /** Indicator */
    val greenIndicator = Color(0xFF00D9A5)
    val emerald = Color(0xFF00D9A5)

    // ═══════════════════════════════════════════════════════════════════════════
    // BUTTON COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val buttonBackground = Color(0xFF00D9A5)
    val buttonText = Color(0xFF000000)
    val buttonSecondary = Color(0xFF1E1E1E)
    val sendButton = Color(0xFF00D9A5)

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val statusOnline = Color(0xFF00D9A5)
    val statusOffline = Color(0xFFFF4444)
    val statusWaiting = Color(0xFFFFAA00)
    val statusThinking = Color(0xFF00D9A5)
    val statusProcessing = Color(0xFF00D9A5)
    val success = Color(0xFF00D9A5)
    val error = Color(0xFFFF4444)
    val warning = Color(0xFFFFAA00)

    // ═══════════════════════════════════════════════════════════════════════════
    // ALERT COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val alertRed = Color(0xFFFF4444)
    val alertRedDim = Color(0x33FF4444)
    val alertBorderStart = Color(0xFFFF4444)
    val alertBorderEnd = Color(0xFFFFAA00)

    // ═══════════════════════════════════════════════════════════════════════════
    // SECONDARY ACCENTS
    // ═══════════════════════════════════════════════════════════════════════════

    val primary = Color(0xFF00D9A5)
    val primaryDim = Color(0xFF1E1E1E)
    val fileTsx = Color(0xFF61DAFB)
    val fileCss = Color(0xFFBB86FC)
    val fileJson = Color(0xFFFF5252)

    // ═══════════════════════════════════════════════════════════════════════════
    // CODE COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val diffAddition = Color(0x1A00D9A5)
    val diffDeletion = Color(0x1AFF4444)
    val diffAdditionText = Color(0xFF00D9A5)
    val diffDeletionText = Color(0xFFFF4444)
    val syntaxKeyword = Color(0xFFC586C0)
    val syntaxFunction = Color(0xFFDCDCAA)
    val syntaxString = Color(0xFFCE9178)
    val syntaxType = Color(0xFFBF5AF2)
    val syntaxComment = Color(0xFF6A9955)
    val syntaxPunctuation = Color(0xFFD4D4D4)
    val lineNumbers = Color(0xFF808080)

    // ═══════════════════════════════════════════════════════════════════════════
    // BADGE/GLASS
    // ═══════════════════════════════════════════════════════════════════════════

    val badgeBackground = Color(0xFF252525)
    val badgeText = Color(0xFFFFFFFF)
    val activeIndicator = Color(0xFF00D9A5)

    val glassBackground = Color(0xCC1A1A1A)
    val glassBorder = Color(0x80444444)
    val scrim = Color(0xCC000000)
    val inputGlow = Color(0x4000D9A5)
}

/**
 * Extended app colors accessible via CompositionLocal.
 */
@Immutable
data class ExtendedAppColors(
    val buttonBackground: Color = AppColors.buttonBackground,
    val buttonText: Color = AppColors.buttonText,
    val buttonSecondary: Color = AppColors.buttonSecondary,
    val statusOnline: Color = AppColors.statusOnline,
    val statusOffline: Color = AppColors.statusOffline,
    val statusWaiting: Color = AppColors.statusWaiting,
    val statusThinking: Color = AppColors.statusThinking,
    val statusProcessing: Color = AppColors.statusProcessing,
    val diffAddition: Color = AppColors.diffAddition,
    val diffDeletion: Color = AppColors.diffDeletion,
    val diffAdditionText: Color = AppColors.diffAdditionText,
    val diffDeletionText: Color = AppColors.diffDeletionText,
    val badgeBackground: Color = AppColors.badgeBackground,
    val badgeText: Color = AppColors.badgeText,
    val activeIndicator: Color = AppColors.activeIndicator,
    val border: Color = AppColors.border,
    val borderLight: Color = AppColors.borderLight,
    val borderWhite: Color = AppColors.borderWhite,
    val greyDark: Color = AppColors.greyDark,
    val grey: Color = AppColors.grey,
    val greyLight: Color = AppColors.greyLight,
    val accentGreen: Color = AppColors.accentGreen,
    val accentGreenBright: Color = AppColors.accentGreenBright,
    val primary: Color = AppColors.primary,
    val primaryDim: Color = AppColors.primaryDim,
    val alertRed: Color = AppColors.alertRed,
    val alertRedDim: Color = AppColors.alertRedDim,
    val cardBackground: Color = AppColors.cardBackground,
    val cardHighlight: Color = AppColors.cardHighlight,
    val moduleBackground: Color = AppColors.moduleBackground,
    val glassBackground: Color = AppColors.glassBackground,
    val glassBorder: Color = AppColors.glassBorder,
    val syntaxKeyword: Color = AppColors.syntaxKeyword,
    val syntaxFunction: Color = AppColors.syntaxFunction,
    val syntaxString: Color = AppColors.syntaxString,
    val syntaxType: Color = AppColors.syntaxType,
    val syntaxComment: Color = AppColors.syntaxComment,
    val fileTsx: Color = AppColors.fileTsx,
    val fileCss: Color = AppColors.fileCss,
    val fileJson: Color = AppColors.fileJson,
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedAppColors() }
