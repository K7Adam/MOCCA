package com.mocca.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * MOCCA Color Palette - Modern sleek theme.
 * Refactored to map to ModernColors system.
 */
@Immutable
object AppColors {
    // ═══════════════════════════════════════════════════════════════════════════
    // BACKGROUNDS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Main app background (Deep Navy) */
    val background = ModernColors.background

    /** Near-black background variant */
    val backgroundVariant = ModernColors.surface

    /** Slightly elevated surface */
    val surface = ModernColors.surface

    /** Surface variant */
    val surfaceVariant = ModernColors.surfaceElevated

    /** Surface container */
    val surfaceContainer = ModernColors.surfaceElevated

    /** Surface elevated */
    val surfaceElevated = ModernColors.surfaceHighlight

    /** Card background */
    val cardBackground = ModernColors.surface

    /** Card highlight */
    val cardHighlight = ModernColors.surfaceElevated

    /** Module/tool card background */
    val moduleBackground = ModernColors.surface

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMARY CONTENT
    // ═══════════════════════════════════════════════════════════════════════════

    val white = Color(0xFFFFFFFF)
    val textPrimary = ModernColors.textPrimary
    val whiteDim = ModernColors.textSecondary
    val textSecondary = ModernColors.textSecondary
    val whiteMuted = ModernColors.textTertiary
    val textTertiary = ModernColors.textTertiary
    val textPlaceholder = ModernColors.textDisabled

    // ═══════════════════════════════════════════════════════════════════════════
    // GREY SCALE
    // ═══════════════════════════════════════════════════════════════════════════

    val greyDark = ModernColors.surfaceHighlight
    val grey = ModernColors.textDisabled
    val greyLight = ModernColors.textTertiary
    val greyExtraLight = ModernColors.textSecondary
    val border = ModernColors.border
    val borderLight = ModernColors.surfaceHighlight
    val borderWhite = Color(0xFFFFFFFF)

    // ═══════════════════════════════════════════════════════════════════════════
    // ACCENT COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Primary accent - Electric Violet */
    val accentGreen = ModernColors.primary // Remapped for logic compatibility
    
    /** Accent variant */
    val accentGreenBright = ModernColors.secondary

    /** Terminal green - Now Emerald */
    val accentGreenTerminal = ModernColors.success

    /** Indicator */
    val greenIndicator = ModernColors.success
    val emerald = ModernColors.success

    // ═══════════════════════════════════════════════════════════════════════════
    // BUTTON COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val buttonBackground = ModernColors.primary
    val buttonText = ModernColors.textPrimary
    val buttonSecondary = ModernColors.surfaceElevated
    val sendButton = ModernColors.primary

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val statusOnline = ModernColors.success
    val statusOffline = ModernColors.error
    val statusWaiting = ModernColors.warning
    val statusThinking = ModernColors.secondary // Deep Indigo for thinking
    val statusProcessing = ModernColors.primary // Violet for processing
    val success = ModernColors.success
    val error = ModernColors.error
    val warning = ModernColors.warning

    // ═══════════════════════════════════════════════════════════════════════════
    // ALERT COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val alertRed = ModernColors.error
    val alertRedDim = ModernColors.error.copy(alpha = 0.1f)
    val alertBorderStart = ModernColors.error
    val alertBorderEnd = ModernColors.warning

    // ═══════════════════════════════════════════════════════════════════════════
    // SECONDARY ACCENTS
    // ═══════════════════════════════════════════════════════════════════════════

    val primary = ModernColors.secondary // Indigo
    val primaryDim = ModernColors.surfaceElevated
    val fileTsx = Color(0xFF61DAFB)
    val fileCss = Color(0xFFBB86FC)
    val fileJson = Color(0xFFFF5252)

    // ═══════════════════════════════════════════════════════════════════════════
    // CODE COLORS (Kept consistent but updated backgrounds)
    // ═══════════════════════════════════════════════════════════════════════════

    val diffAddition = ModernColors.success.copy(alpha = 0.1f)
    val diffDeletion = ModernColors.error.copy(alpha = 0.1f)
    val diffAdditionText = ModernColors.success
    val diffDeletionText = ModernColors.error
    val syntaxKeyword = Color(0xFFC586C0)
    val syntaxFunction = Color(0xFFDCDCAA)
    val syntaxString = Color(0xFFCE9178)
    val syntaxType = Color(0xFFBF5AF2)
    val syntaxComment = Color(0xFF6A9955)
    val syntaxPunctuation = Color(0xFFD4D4D4)
    val lineNumbers = ModernColors.textTertiary

    // ═══════════════════════════════════════════════════════════════════════════
    // BADGE/GLASS
    // ═══════════════════════════════════════════════════════════════════════════

    val badgeBackground = ModernColors.surfaceHighlight
    val badgeText = ModernColors.textPrimary
    val activeIndicator = ModernColors.primary

    val glassBackground = ModernColors.surface.copy(alpha = 0.8f)
    val glassBorder = ModernColors.border.copy(alpha = 0.5f)
    val scrim = Color(0xCC000000)
    val inputGlow = ModernColors.primary.copy(alpha = 0.2f)
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
