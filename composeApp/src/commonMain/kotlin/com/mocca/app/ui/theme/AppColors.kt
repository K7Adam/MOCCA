package com.mocca.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * MOCCA Color Palette — Neutral Monochrome Soft Dark
 *
 * Design principles:
 * - Soft dark background (#1A1A1A) — easy on the eyes, not OLED black
 * - M3 tonal surface elevation hierarchy
 * - Desaturated cool gray-blue accent (#8B9DC3)
 * - Semantic status colors (green/red/amber)
 */
@Immutable
object AppColors {
    // ═══════════════════════════════════════════════════════════════════════════
    // BACKGROUNDS — M3 Tonal Surface Hierarchy
    // ═══════════════════════════════════════════════════════════════════════════

    /** Main app background — soft dark */
    val background = Color(0xFF1A1A1A)

    /** Dim surface — deepest layer */
    val surfaceDim = Color(0xFF0F0F0F)

    /** Surface container lowest — near-black */
    val surfaceContainerLowest = Color(0xFF121212)

    /** Surface container low — matches background */
    val surfaceContainerLow = Color(0xFF1A1A1A)

    /** Standard surface — slightly elevated */
    val surface = Color(0xFF1A1A1A)

    /** Surface container — default elevated layer */
    val surfaceContainer = Color(0xFF202020)

    /** Surface container high — cards, inputs */
    val surfaceContainerHigh = Color(0xFF272727)

    /** Surface container highest — top-level elevated elements */
    val surfaceContainerHighest = Color(0xFF303030)

    /** Surface bright — maximum elevation */
    val surfaceBright = Color(0xFF383838)

    /** Surface variant — for differentiated areas */
    val surfaceVariant = Color(0xFF272727)

    /** Surface elevated — for dropdown menus and overlays */
    val surfaceElevated = Color(0xFF2D2D2D)

    /** Card background */
    val cardBackground = Color(0xFF202020)

    /** Card highlight — hover/pressed state */
    val cardHighlight = Color(0xFF272727)

    /** Module/tool card background */
    val moduleBackground = Color(0xFF202020)

    // ═══════════════════════════════════════════════════════════════════════════
    // TEXT — High contrast hierarchy
    // ═══════════════════════════════════════════════════════════════════════════

    val white = Color(0xFFFFFFFF)
    val textPrimary = Color(0xFFE8E8E8)
    val textSecondary = Color(0xFFA0A0A0)
    val textTertiary = Color(0xFF666666)
    val textPlaceholder = Color(0xFF666666)

    // Legacy aliases for compatibility
    val whiteDim = Color(0xFFA0A0A0)
    val whiteMuted = Color(0xFF666666)

    // ═══════════════════════════════════════════════════════════════════════════
    // GREY SCALE — Navy-tinted greys
    // ═══════════════════════════════════════════════════════════════════════════

    val greyDark = Color(0xFF333333)
    val grey = Color(0xFF666666)
    val greyLight = Color(0xFF999999)
    val greyExtraLight = Color(0xFFB0B0B0)
    val border = Color(0xFF333333)
    val borderLight = Color(0xFF444444)
    val borderWhite = Color(0xFFE8E8E8)

    // ═══════════════════════════════════════════════════════════════════════════
    // ACCENT — Desaturated cool gray-blue
    // ═══════════════════════════════════════════════════════════════════════════

    /** Primary accent — desaturated cool gray-blue */
    val accent = Color(0xFF8B9DC3)

    /** Accent bright variant */
    val accentBright = Color(0xFFA3B4D4)

    /** Indicator color */
    val indicator = Color(0xFF8B9DC3)

    // ═══════════════════════════════════════════════════════════════════════════
    // BUTTON COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val buttonBackground = Color(0xFF8B9DC3)
    val buttonText = Color(0xFF0F0F0F)
    val buttonSecondary = Color(0xFF272727)
    val sendButton = Color(0xFF8B9DC3)

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS COLORS — Semantic (green/red/amber)
    // ═══════════════════════════════════════════════════════════════════════════

    val statusOnline = Color(0xFF4CAF50)
    val statusOffline = Color(0xFFEF5350)
    val statusWaiting = Color(0xFFFFB74D)
    val statusThinking = Color(0xFF8B9DC3)
    val statusProcessing = Color(0xFF8B9DC3)
    val success = Color(0xFF4CAF50)
    val error = Color(0xFFEF5350)
    val warning = Color(0xFFFFB74D)

    // ═══════════════════════════════════════════════════════════════════════════
    // ALERT COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val alertRed = Color(0xFFEF5350)
    val alertRedDim = Color(0x33EF5350)
    val alertBorderStart = Color(0xFFEF5350)
    val alertBorderEnd = Color(0xFFFFB74D)

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMARY / SECONDARY ACCENTS
    // ═══════════════════════════════════════════════════════════════════════════

    val primary = Color(0xFF8B9DC3)
    val primaryDim = Color(0xFF272727)
    val fileTsx = Color(0xFF61DAFB)
    val fileCss = Color(0xFF9E9E9E)
    val fileJson = Color(0xFFEF5350)

    // ═══════════════════════════════════════════════════════════════════════════
    // CODE COLORS — One Dark Pro inspired
    // ═══════════════════════════════════════════════════════════════════════════

    val diffAddition = Color(0x1A4CAF50)
    val diffDeletion = Color(0x1AEF5350)
    val diffAdditionText = Color(0xFF4CAF50)
    val diffDeletionText = Color(0xFFEF5350)
    val syntaxKeyword = Color(0xFFC586C0)
    val syntaxFunction = Color(0xFF61DAFB)
    val syntaxString = Color(0xFFCE9178)
    val syntaxType = Color(0xFF9E9E9E)
    val syntaxComment = Color(0xFF6A9955)
    val syntaxPunctuation = Color(0xFFD4D4D4)
    val lineNumbers = Color(0xFF666666)

    // ═══════════════════════════════════════════════════════════════════════════
    // BADGE / MISC
    // ═══════════════════════════════════════════════════════════════════════════

    val badgeBackground = Color(0xFF303030)
    val badgeText = Color(0xFFE8E8E8)
    val activeIndicator = Color(0xFF8B9DC3)

    val scrim = Color(0xCC000000)
    val inputGlow = Color(0x408B9DC3)

    // ═══════════════════════════════════════════════════════════════════════════
    // SHIMMER COLORS (Loading Animation)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Shimmer base color — dark gray for loading placeholders */
    val shimmerBase = Color(0xFF202020)

    /** Shimmer highlight color — subtle sweep effect */
    val shimmerHighlight = Color(0xFF383838)

    /** Shimmer accent — optional tint for branded shimmer */
    val shimmerAccent = Color(0x158B9DC3)
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
    val accent: Color = AppColors.accent,
    val accentBright: Color = AppColors.accentBright,
    val primary: Color = AppColors.primary,
    val primaryDim: Color = AppColors.primaryDim,
    val alertRed: Color = AppColors.alertRed,
    val alertRedDim: Color = AppColors.alertRedDim,
    val cardBackground: Color = AppColors.cardBackground,
    val cardHighlight: Color = AppColors.cardHighlight,
    val moduleBackground: Color = AppColors.moduleBackground,
    val syntaxKeyword: Color = AppColors.syntaxKeyword,
    val syntaxFunction: Color = AppColors.syntaxFunction,
    val syntaxString: Color = AppColors.syntaxString,
    val syntaxType: Color = AppColors.syntaxType,
    val syntaxComment: Color = AppColors.syntaxComment,
    val fileTsx: Color = AppColors.fileTsx,
    val fileCss: Color = AppColors.fileCss,
    val fileJson: Color = AppColors.fileJson,
    // Shimmer colors
    val shimmerBase: Color = AppColors.shimmerBase,
    val shimmerHighlight: Color = AppColors.shimmerHighlight,
    val shimmerAccent: Color = AppColors.shimmerAccent,
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedAppColors() }
