package com.mocca.app.ui.components.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass Design Tokens - First Principles Implementation
 * 
 * Liquid Glass is a UI material that creates visual separation between layers
 * by simulating the optical properties of real glass:
 * 
 * 1. REFRACTION — underlying content appears distorted/magnified through the surface
 * 2. BLUR / FROST — background is blurred (frosted), not just transparent
 * 3. CHROMATIC ABERRATION — subtle RGB channel separation at glass edges (prismatic fringing)
 * 4. SPECULAR HIGHLIGHT — bright edge/rim at the top of the glass surface (light reflection)
 * 5. INNER SHADOW — soft shadow at the bottom edge (depth cue)
 * 6. EDGE STROKE — thin, translucent 1dp border that separates glass from content
 * 
 * These tokens are designed for MOCCA's Pitch Black OLED-friendly theme
 * with Mint Green accent (#00D9A5).
 */
object GlassTokens {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BLUR / FROST
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Background blur kernel - dark mode */
    val blurRadiusDark: Dp = 24.dp
    
    /** Background blur kernel - light mode */
    val blurRadiusLight: Dp = 20.dp
    
    /** Light blur for buttons/controls */
    val blurRadiusLightVariant: Dp = 12.dp
    
    /** Heavy blur for prominent surfaces */
    val blurRadiusHeavy: Dp = 32.dp
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TINT COLORS - COLORLESS GLASS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Surface tint - TRANSPARENT for colorless glass */
    val tintColorDark: Color = Color.Transparent
    
    /** Surface tint - light mode */
    val tintColorLight: Color = Color.Transparent
    
    /** Dark overlay - TRANSPARENT (depth from geometry, not color) */
    val tintDarkOverlayDark: Color = Color.Transparent
    
    /** Dark overlay - light mode */
    val tintDarkOverlayLight: Color = Color.Transparent
    
    /** Mint accent tint - subtle brand color */
    val tintMintAccent: Color = Color(0x1A00D9A5) // Mint 10% alpha
    
    /** Primary tint - very subtle dark for text contrast on glass */
    val tintPrimary: Color = Color(0x40000000) // 25% dark for text legibility
    
    /** Secondary tint - for layered elements */
    val tintSecondary: Color = Color(0x33000000) // 20% dark
    
    /** Light tint - for active/highlighted surfaces */
    val tintLight: Color = Color(0x26000000) // 15% dark
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SPECULAR HIGHLIGHTS - GEOMETRY-BASED DEPTH
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Top edge specular highlight - pure white for light source */
    val highlightTopDark: Color = Color(0x33FFFFFF) // White 20% alpha
    
    /** Top edge specular highlight - light mode */
    val highlightTopLight: Color = Color(0x4DFFFFFF) // White 30% alpha
    
    /** Inner specular glow - subtle white */
    val specularInnerDark: Color = Color(0x14FFFFFF) // White 8% alpha
    
    /** Inner specular glow - light mode */
    val specularInnerLight: Color = Color(0x1AFFFFFF) // White 10% alpha
    
    /** Refraction accent - mint green glow */
    val refractionAccent: Color = Color(0x3300D9A5) // Mint 20% alpha
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EDGE STROKE / BORDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Standard border - dark mode */
    val strokeColorDark: Color = Color(0x26FFFFFF) // White 15% alpha
    
    /** Standard border - light mode */
    val strokeColorLight: Color = Color(0x4DFFFFFF) // White 30% alpha
    
    /** Border highlight - top edge brighter */
    val borderHighlight: Color = Color(0x66FFFFFF) // White 40% alpha
    
    /** Border shadow - bottom edge darker */
    val borderShadow: Color = Color(0x1AFFFFFF) // White 10% alpha
    
    /** Border width */
    val strokeWidth: Dp = 1.dp
    
    /** Border width for prominent elements */
    val strokeWidthProminent: Dp = 1.5.dp
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INNER SHADOW - SUBTLE DEPTH
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Inner shadow color - very subtle for depth cue */
    val shadowColorDark: Color = Color(0x1A000000) // Black 10% alpha
    
    /** Inner shadow color - light mode */
    val shadowColorLight: Color = Color(0x14000000) // Black 8% alpha
    
    /** Inner shadow blur radius */
    val shadowBlur: Dp = 8.dp
    
    /** Inner shadow offset Y */
    val shadowOffsetY: Dp = 2.dp
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REFRACTION (Lens Distortion)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Refractive index - dark mode (subtle magnification) */
    const val refractiveIndexDark: Float = 1.2f
    
    /** Refractive index - light mode */
    const val refractiveIndexLight: Float = 1.3f
    
    /** Refraction strength multiplier */
    const val refractionStrength: Float = 0.35f
    
    /** Refraction strength for buttons/controls */
    const val refractionStrengthLight: Float = 0.2f
    
    /** Refraction strength for prominent surfaces */
    const val refractionStrengthHeavy: Float = 0.5f
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CHROMATIC ABERRATION (Prism Effect)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Chromatic aberration strength - dark mode (subtle) */
    const val chromaticAberrationDark: Float = 0.008f
    
    /** Chromatic aberration strength - light mode */
    const val chromaticAberrationLight: Float = 0.012f
    
    /** Chromatic aberration for heavy effect */
    const val chromaticAberrationHeavy: Float = 0.02f
    
    /** Chromatic aberration disabled */
    const val chromaticAberrationNone: Float = 0f
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLOR ADJUSTMENTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Saturation multiplier - dark mode (vibrancy boost) */
    const val saturationDark: Float = 1.3f
    
    /** Saturation multiplier - light mode */
    const val saturationLight: Float = 1.2f
    
    /** Contrast adjustment - dark mode */
    const val contrastDark: Float = 1.1f
    
    /** Contrast adjustment - light mode */
    const val contrastLight: Float = 1.05f
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Standard corner radius for glass surfaces */
    val cornerRadius: Dp = 32.dp
    
    /** Corner radius for cards */
    val cornerRadiusCard: Dp = 16.dp
    
    /** Corner radius for buttons */
    val cornerRadiusButton: Dp = 9999.dp // Pill
    
    /** Corner radius for bottom sheets */
    val cornerRadiusSheet: Dp = 24.dp
    
    /** Corner radius for app bars */
    val cornerRadiusAppBar: Dp = 0.dp // No rounding for full-width bars
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Parallax animation multiplier */
    const val parallaxMultiplier: Float = 0.02f
    
    /** Animation duration for glass transitions */
    const val animationDurationMs: Int = 300
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PERFORMANCE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Blur sample count (4 = 9x9 = 81 samples) */
    const val blurSamplesDefault: Int = 4
    
    /** Blur sample count for light mode */
    const val blurSamplesLight: Int = 3
    
    /** Blur sample count for heavy blur */
    const val blurSamplesHeavy: Int = 5
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ACCESSIBILITY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Minimum touch target size */
    val minTouchTarget: Dp = 48.dp
    
    /** WCAG AA contrast ratio requirement */
    const val minContrastRatio: Float = 4.5f
    
    /** Fallback solid background when transparency reduced */
    val fallbackSolidBackgroundDark: Color = Color(0xFF1A1A1A)
    
    /** Fallback solid background - light mode */
    val fallbackSolidBackgroundLight: Color = Color(0xFFF5F5F5)
}

/**
 * Theme-aware glass tokens that adapt to light/dark mode.
 * 
 * @param isDark Whether the app is in dark mode
 */
data class GlassThemeTokens(
    val isDark: Boolean = true
) {
    val blurRadius: Dp get() = if (isDark) GlassTokens.blurRadiusDark else GlassTokens.blurRadiusLight
    val tintColor: Color get() = if (isDark) GlassTokens.tintColorDark else GlassTokens.tintColorLight
    val darkOverlay: Color get() = if (isDark) GlassTokens.tintDarkOverlayDark else GlassTokens.tintDarkOverlayLight
    val highlightTop: Color get() = if (isDark) GlassTokens.highlightTopDark else GlassTokens.highlightTopLight
    val specularInner: Color get() = if (isDark) GlassTokens.specularInnerDark else GlassTokens.specularInnerLight
    val strokeColor: Color get() = if (isDark) GlassTokens.strokeColorDark else GlassTokens.strokeColorLight
    val shadowColor: Color get() = if (isDark) GlassTokens.shadowColorDark else GlassTokens.shadowColorLight
    val refractiveIndex: Float get() = if (isDark) GlassTokens.refractiveIndexDark else GlassTokens.refractiveIndexLight
    val chromaticAberration: Float get() = if (isDark) GlassTokens.chromaticAberrationDark else GlassTokens.chromaticAberrationLight
    val saturation: Float get() = if (isDark) GlassTokens.saturationDark else GlassTokens.saturationLight
    val contrast: Float get() = if (isDark) GlassTokens.contrastDark else GlassTokens.contrastLight
    val blurSamples: Int get() = if (isDark) GlassTokens.blurSamplesDefault else GlassTokens.blurSamplesLight
    val fallbackBackground: Color get() = if (isDark) GlassTokens.fallbackSolidBackgroundDark else GlassTokens.fallbackSolidBackgroundLight
}
