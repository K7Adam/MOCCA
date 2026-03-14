package com.mocca.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * MOCCA Color Palette — Material 3 Expressive Tonal Hierarchy.
 *
 * Seed: #8B9DC3 (AnchorPrimary)
 * Neutral Seed: #1A1A1A (DarkBackgroundSeed)
 */
@Immutable
object AppColors {
    // ---------------------------------------------------------------------------
    // BRAND ANCHOR COLORS
    // ---------------------------------------------------------------------------
    val AnchorPrimary = Color(0xFF8B9DC3)
    val AnchorSecondary = Color(0xFF8B9DC3)
    val AnchorTertiary = Color(0xFF8B9DC3)
    val DarkBackgroundSeed = Color(0xFF1A1A1A)

    // ═══════════════════════════════════════════════════════════════════════════
    // M3 PRIMARY TONAL SCALE (Accents)
    // ═══════════════════════════════════════════════════════════════════════════
    
    val primary = Color(0xFF8B9DC3) // Tone 80
    val onPrimary = Color(0xFF1A2C4D) // Tone 20
    val primaryContainer = Color(0xFF324465) // Tone 30
    val onPrimaryContainer = Color(0xFFC2D1F0) // Tone 90
    val inversePrimary = Color(0xFF3F517E) // Tone 40

    // ═══════════════════════════════════════════════════════════════════════════
    // M3 SECONDARY TONAL SCALE
    // ═══════════════════════════════════════════════════════════════════════════
    
    val secondary = Color(0xFF8B9DC3) // Shared with primary for monochrome feel
    val onSecondary = Color(0xFF1A2C4D)
    val secondaryContainer = Color(0xFF272727) // Tone 20 Neutral
    val onSecondaryContainer = Color(0xFFA0A0A0) // Tone 70 Neutral

    // ═══════════════════════════════════════════════════════════════════════════
    // M3 NEUTRAL TONAL SCALE (Surfaces)
    // ═══════════════════════════════════════════════════════════════════════════

    val background = Color(0xFF1A1A1A) // Tone 10
    val onBackground = Color(0xFFE8E8E8) // Tone 90
    
    val surface = Color(0xFF1A1A1A) // Tone 10
    val onSurface = Color(0xFFE8E8E8) // Tone 90
    
    val surfaceVariant = Color(0xFF272727) // Tone 20
    val onSurfaceVariant = Color(0xFFA0A0A0) // Tone 70
    
    val surfaceDim = Color(0xFF0F0F0F) // Tone 6
    val surfaceBright = Color(0xFF383838) // Tone 24
    
    val surfaceContainerLowest = Color(0xFF121212) // Tone 8
    val surfaceContainerLow = Color(0xFF1A1A1A) // Tone 10
    val surfaceContainer = Color(0xFF202020) // Tone 12
    val surfaceContainerHigh = Color(0xFF272727) // Tone 17
    val surfaceContainerHighest = Color(0xFF303030) // Tone 22

    val outline = Color(0xFF333333) // Tone 25
    val outlineVariant = Color(0xFF444444) // Tone 35

    // ═══════════════════════════════════════════════════════════════════════════
    // SEMANTIC STATUS COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val error = Color(0xFFEF5350)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)

    val success = Color(0xFF4CAF50)
    val warning = Color(0xFFFFB74D)
    
    val statusOnline = Color(0xFF4CAF50)
    val statusOffline = Color(0xFFEF5350)
    val statusWaiting = Color(0xFFFFB74D)
    val statusThinking = Color(0xFF8B9DC3)
    val statusProcessing = Color(0xFF8B9DC3)

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPATIBILITY ALIASES (Mapping legacy names to M3 tokens)
    // ═══════════════════════════════════════════════════════════════════════════

    val textPrimary = onSurface
    val textSecondary = onSurfaceVariant
    val textTertiary = onSurfaceVariant.copy(alpha = 0.6f)
    val textPlaceholder = onSurfaceVariant.copy(alpha = 0.4f)
    
    val surfaceElevated = surfaceContainerHigh
    val cardBackground = surfaceContainer
    val cardHighlight = surfaceContainerHigh
    val moduleBackground = surfaceContainer
    
    val alertRed = error
    val alertRedDim = error.copy(alpha = 0.1f)
    val alertBorderStart = error
    val alertBorderEnd = warning

    val indicator = primary
    val activeIndicator = primary
    val buttonBackground = primary
    val buttonText = onPrimary
    val buttonSecondary = surfaceVariant
    val sendButton = primary
    
    val input = surfaceContainerLow

    val white = Color(0xFFFFFFFF)
    val whiteDim = Color(0xFFA0A0A0)
    val whiteMuted = Color(0xFF666666)
    
    val greyDark = Color(0xFF333333)
    val grey = Color(0xFF666666)
    val greyLight = Color(0xFF999999)
    val greyExtraLight = Color(0xFFB0B0B0)
    
    val border = Color(0xFF333333)
    val borderLight = Color(0xFF444444)
    val borderWhite = Color(0xFFE8E8E8)

    val accent = Color(0xFF8B9DC3)
    val accentBright = Color(0xFFA3B4D4)
    val accentGreen = Color(0xFF4CAF50)
    
    val badgeBackground = Color(0xFF303030)
    val badgeText = Color(0xFFE8E8E8)
    
    val scrim = Color(0xCC000000)
    val inputGlow = Color(0x408B9DC3)

    // Code Colors
    val syntaxKeyword = Color(0xFFC586C0)
    val syntaxFunction = Color(0xFF61DAFB)
    val syntaxString = Color(0xFFCE9178)
    val syntaxType = Color(0xFF9E9E9E)
    val syntaxComment = Color(0xFF6A9955)
    val syntaxPunctuation = Color(0xFFD4D4D4)
    val diffAddition = Color(0x1A4CAF50)
    val diffDeletion = Color(0x1AEF5350)
    val diffAdditionText = Color(0xFF4CAF50)
    val diffDeletionText = Color(0xFFEF5350)
    val lineNumbers = Color(0xFF666666)
    
    val fileTsx = Color(0xFF61DAFB)
    val fileCss = Color(0xFF9E9E9E)
    val fileJson = Color(0xFFEF5350)

    // Shimmer
    val shimmerBase = Color(0xFF202020)
    val shimmerHighlight = Color(0xFF383838)
    val shimmerAccent = Color(0x158B9DC3)
}

/**
 * Extended app colors accessible via CompositionLocal.
 */
@Immutable
data class ExtendedAppColors(
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
    val greyDark: Color = AppColors.greyDark,
    val grey: Color = AppColors.grey,
    val greyLight: Color = AppColors.greyLight,
    val accent: Color = AppColors.accent,
    val accentBright: Color = AppColors.accentBright,
    val accentGreen: Color = AppColors.accentGreen,
    val syntaxKeyword: Color = AppColors.syntaxKeyword,
    val syntaxFunction: Color = AppColors.syntaxFunction,
    val syntaxString: Color = AppColors.syntaxString,
    val syntaxType: Color = AppColors.syntaxType,
    val syntaxComment: Color = AppColors.syntaxComment,
    val fileTsx: Color = AppColors.fileTsx,
    val fileCss: Color = AppColors.fileCss,
    val fileJson: Color = AppColors.fileJson,
    val shimmerBase: Color = AppColors.shimmerBase,
    val shimmerHighlight: Color = AppColors.shimmerHighlight,
    val shimmerAccent: Color = AppColors.shimmerAccent,
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedAppColors() }
