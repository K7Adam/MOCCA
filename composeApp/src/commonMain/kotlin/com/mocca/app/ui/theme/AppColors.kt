package com.mocca.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * MOCCA Color Palette — Nebula Dark
 *
 * Rich dark navy-charcoal surfaces with Electric Violet accents.
 * Warm off-white text with subtle purple undertone.
 * Designed for readability, depth, and modern AI-companion aesthetics.
 */
@Immutable
object AppColors {
    // ═══════════════════════════════════════════════════════════════════════════
    // BACKGROUNDS — Rich dark navy-charcoal (NOT pure black)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Main app background — deep navy-charcoal */
    val background = Color(0xFF0D0E12)

    /** Background variant — slightly lighter for layering */
    val backgroundVariant = Color(0xFF111218)

    /** Slightly elevated surface */
    val surface = Color(0xFF16171E)

    /** Surface variant */
    val surfaceVariant = Color(0xFF1C1D26)

    /** Surface container */
    val surfaceContainer = Color(0xFF21222D)

    /** Surface elevated — highest non-modal elevation */
    val surfaceElevated = Color(0xFF282A36)

    /** Card background */
    val cardBackground = Color(0xFF1C1D26)

    /** Card highlight — hover/pressed state */
    val cardHighlight = Color(0xFF282A36)

    /** Module/tool card background */
    val moduleBackground = Color(0xFF21222D)

    // ═══════════════════════════════════════════════════════════════════════════
    // TEXT — Warm off-whites with purple undertone
    // ═══════════════════════════════════════════════════════════════════════════

    val white = Color(0xFFECEDF3)
    val textPrimary = Color(0xFFECEDF3)
    val whiteDim = Color(0xFFA0A3B5)
    val textSecondary = Color(0xFFA0A3B5)
    val whiteMuted = Color(0xFF6C6F82)
    val textTertiary = Color(0xFF6C6F82)
    val textPlaceholder = Color(0xFF545670)

    // ═══════════════════════════════════════════════════════════════════════════
    // GREY SCALE — Navy-tinted greys
    // ═══════════════════════════════════════════════════════════════════════════

    val greyDark = Color(0xFF2E3042)
    val grey = Color(0xFF545670)
    val greyLight = Color(0xFF8385A0)
    val greyExtraLight = Color(0xFFA0A3B5)
    val border = Color(0xFF2E3042)
    val borderLight = Color(0xFF3A3D52)
    val borderWhite = Color(0xFFECEDF3)

    // ═══════════════════════════════════════════════════════════════════════════
    // ACCENT COLORS — Electric Violet
    // ═══════════════════════════════════════════════════════════════════════════

    /** Primary accent — Electric Violet */
    val accentGreen = Color(0xFF7C5CFC)

    /** Accent variant — Bright Violet */
    val accentGreenBright = Color(0xFFA78BFA)

    /** Terminal accent — kept name for compat */
    val accentGreenTerminal = Color(0xFF7C5CFC)

    /** Indicator */
    val greenIndicator = Color(0xFF7C5CFC)
    val emerald = Color(0xFF7C5CFC)

    // ═══════════════════════════════════════════════════════════════════════════
    // BUTTON COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val buttonBackground = Color(0xFF7C5CFC)
    val buttonText = Color(0xFFFFFFFF)
    val buttonSecondary = Color(0xFF21222D)
    val sendButton = Color(0xFF7C5CFC)

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val statusOnline = Color(0xFF34D399)
    val statusOffline = Color(0xFFF87171)
    val statusWaiting = Color(0xFFFBBF24)
    val statusThinking = Color(0xFFA78BFA)
    val statusProcessing = Color(0xFF7C5CFC)
    val success = Color(0xFF34D399)
    val error = Color(0xFFF87171)
    val warning = Color(0xFFFBBF24)

    // ═══════════════════════════════════════════════════════════════════════════
    // ALERT COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    val alertRed = Color(0xFFF87171)
    val alertRedDim = Color(0x33F87171)
    val alertBorderStart = Color(0xFFF87171)
    val alertBorderEnd = Color(0xFFFBBF24)

    // ═══════════════════════════════════════════════════════════════════════════
    // SECONDARY ACCENTS
    // ═══════════════════════════════════════════════════════════════════════════

    val primary = Color(0xFF7C5CFC)
    val primaryDim = Color(0xFF21222D)
    val fileTsx = Color(0xFF61DAFB)
    val fileCss = Color(0xFFA78BFA)
    val fileJson = Color(0xFFF87171)

    // ═══════════════════════════════════════════════════════════════════════════
    // CODE COLORS — One Dark Pro inspired
    // ═══════════════════════════════════════════════════════════════════════════

    val diffAddition = Color(0x1A34D399)
    val diffDeletion = Color(0x1AF87171)
    val diffAdditionText = Color(0xFF34D399)
    val diffDeletionText = Color(0xFFF87171)
    val syntaxKeyword = Color(0xFFC586C0)
    val syntaxFunction = Color(0xFF61DAFB)
    val syntaxString = Color(0xFFCE9178)
    val syntaxType = Color(0xFFA78BFA)
    val syntaxComment = Color(0xFF6A9955)
    val syntaxPunctuation = Color(0xFFD4D4D4)
    val lineNumbers = Color(0xFF6C6F82)

    // ═══════════════════════════════════════════════════════════════════════════
    // BADGE / GLASS
    // ═══════════════════════════════════════════════════════════════════════════

    val badgeBackground = Color(0xFF282A36)
    val badgeText = Color(0xFFECEDF3)
    val activeIndicator = Color(0xFF7C5CFC)

    val glassBackground = Color(0x4D16171E)
    val glassBorder = Color(0x33A0A3B5)
    val scrim = Color(0xCC0D0E12)
    val inputGlow = Color(0x407C5CFC)

    // ═══════════════════════════════════════════════════════════════════════════
    // LIQUID GLASS — Recolored for Nebula Dark
    // ═══════════════════════════════════════════════════════════════════════════

    /** Liquid glass primary tint — navy-dark for legibility */
    val liquidGlassTint = Color(0x400D0E12)

    /** Liquid glass secondary tint — layered effects */
    val liquidGlassTintSecondary = Color(0x3316171E)

    /** Liquid glass light tint — elevated elements */
    val liquidGlassTintLight = Color(0x261C1D26)

    /** Top edge specular highlight */
    val liquidGlassSpecular = Color(0x33A0A3B5)

    /** Inner specular glow */
    val liquidGlassSpecularInner = Color(0x14A0A3B5)

    /** Refraction accent — violet glow */
    val liquidGlassRefraction = Color(0x337C5CFC)

    /** Primary border — off-white with 20% opacity */
    val liquidGlassBorder = Color(0x33A0A3B5)

    /** Border highlight — top edge brighter */
    val liquidGlassBorderHighlight = Color(0x4DA0A3B5)

    /** Border shadow — bottom edge darker */
    val liquidGlassBorderShadow = Color(0x1AA0A3B5)

    /** Premium glass background */
    val glassPremium = Color(0x400D0E12)

    /** Premium glass border */
    val glassBorderPremium = Color(0x33A0A3B5)

    /** Premium glass glow — violet for focused state */
    val glassGlowMint = Color(0x337C5CFC)

    /** Noise texture alpha */
    const val liquidGlassNoiseFactor = 0.10f

    // ═══════════════════════════════════════════════════════════════════════════
    // SHIMMER COLORS (Loading Animation)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Shimmer base */
    val shimmerBase = Color(0xFF1C1D26)

    /** Shimmer highlight */
    val shimmerHighlight = Color(0xFF2E3042)

    /** Shimmer accent — violet tint */
    val shimmerAccent = Color(0x157C5CFC)
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
    // Liquid Glass colors
    val liquidGlassTint: Color = AppColors.liquidGlassTint,
    val liquidGlassTintSecondary: Color = AppColors.liquidGlassTintSecondary,
    val liquidGlassSpecular: Color = AppColors.liquidGlassSpecular,
    val liquidGlassRefraction: Color = AppColors.liquidGlassRefraction,
    val liquidGlassBorder: Color = AppColors.liquidGlassBorder,
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
