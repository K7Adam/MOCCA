package com.mocca.app.ui.components.glass

import androidx.compose.ui.graphics.Color

/**
 * Parameters for configuring the liquid glass shader effect.
 * 
 * These parameters control all aspects of the glass simulation:
 * - Refraction (lens distortion)
 * - Chromatic aberration (color separation)
 * - Blur (frosted appearance)
 * - Specular highlights (edge lighting)
 * - Inner shadow (depth)
 * - Edge stroke (border)
 */
data class GlassShaderParams(
    // ═══════════════════════════════════════════════════════════════════════════
    // CORE EFFECTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Blur radius in pixels for the frosted glass effect */
    val blurRadius: Float = 24f,
    
    /** Refraction strength (0.0 = none, 1.0 = maximum lens distortion) */
    val refractionStrength: Float = 0.35f,
    
    /** Chromatic aberration intensity for prism effect at edges */
    val chromaticAberration: Float = 0.008f,
    
    /** Saturation multiplier for color vibrancy (1.0 = no change) */
    val saturation: Float = 1.3f,
    
    /** Contrast adjustment (1.0 = no change) */
    val contrast: Float = 1.1f,
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TINT & OVERLAY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Tint color overlay for the glass surface */
    val tintColor: Color = Color(0x1FFFFFFF),
    
    /** Dark overlay for depth and contrast */
    val darkOverlayColor: Color = Color(0x4D000000),
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EDGE EFFECTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Top edge specular highlight color */
    val highlightTopColor: Color = Color(0x40FFFFFF),
    
    /** Inner specular glow color */
    val specularInnerColor: Color = Color(0x1AFFFFFF),
    
    /** Border/stroke color */
    val strokeColor: Color = Color(0x26FFFFFF),
    
    /** Inner shadow color for depth */
    val shadowColor: Color = Color(0x4D000000),
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE & GEOMETRY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Corner radius in pixels */
    val cornerRadius: Float = 32f,
    
    /** Width of the glass element in pixels */
    val width: Float = 0f,
    
    /** Height of the glass element in pixels */
    val height: Float = 0f,
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BEHAVIOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Number of blur samples (higher = better quality, lower performance) */
    val blurSamples: Int = 4,
    
    /** Whether to apply refraction effect */
    val enableRefraction: Boolean = true,
    
    /** Whether to apply chromatic aberration */
    val enableChromaticAberration: Boolean = true,
    
    /** Whether to apply specular highlights */
    val enableSpecular: Boolean = true,
    
    /** Whether transparency is reduced (accessibility mode) */
    val reducedTransparency: Boolean = false,
    
    /** Whether motion is reduced (accessibility mode) */
    val reducedMotion: Boolean = false
) {
    companion object {
        /**
         * Default glass parameters for standard surfaces.
         */
        val Default = GlassShaderParams()
        
        /**
         * Glass parameters optimized for floating elements (bottom bars, FABs).
         */
        val Floating = GlassShaderParams(
            blurRadius = 32f,
            refractionStrength = 0.5f,
            chromaticAberration = 0.012f,
            saturation = 1.4f,
            contrast = 1.15f
        )
        
        /**
         * Glass parameters for buttons and small controls.
         */
        val Button = GlassShaderParams(
            blurRadius = 12f,
            refractionStrength = 0.2f,
            chromaticAberration = 0.005f,
            saturation = 1.2f,
            cornerRadius = 9999f // Pill shape
        )
        
        /**
         * Glass parameters for app bars (top bars).
         */
        val AppBar = GlassShaderParams(
            blurRadius = 20f,
            refractionStrength = 0.25f,
            chromaticAberration = 0.006f,
            cornerRadius = 0f // Full-width, no corner rounding
        )
        
        /**
         * Glass parameters for bottom sheets.
         */
        val Sheet = GlassShaderParams(
            blurRadius = 28f,
            refractionStrength = 0.4f,
            chromaticAberration = 0.01f,
            cornerRadius = 24f
        )
        
        /**
         * Glass parameters for dialogs.
         */
        val Dialog = GlassShaderParams(
            blurRadius = 24f,
            refractionStrength = 0.35f,
            chromaticAberration = 0.008f,
            cornerRadius = 24f
        )
        
        /**
         * Fallback parameters when full glass effect is not available.
         */
        val Fallback = GlassShaderParams(
            blurRadius = 0f,
            refractionStrength = 0f,
            chromaticAberration = 0f,
            enableRefraction = false,
            enableChromaticAberration = false
        )
        
        /**
         * Accessibility-friendly parameters with solid backgrounds.
         */
        val Accessible = GlassShaderParams(
            blurRadius = 0f,
            refractionStrength = 0f,
            chromaticAberration = 0f,
            enableRefraction = false,
            enableChromaticAberration = false,
            reducedTransparency = true
        )
    }
}

/**
 * Creates GlassShaderParams from GlassThemeTokens.
 */
fun GlassShaderParams.Companion.fromTokens(
    tokens: GlassThemeTokens,
    width: Float = 0f,
    height: Float = 0f,
    cornerRadius: Float = tokens.blurRadius.value
): GlassShaderParams = GlassShaderParams(
    blurRadius = tokens.blurRadius.value,
    refractionStrength = GlassTokens.refractionStrength,
    chromaticAberration = tokens.chromaticAberration,
    saturation = tokens.saturation,
    contrast = tokens.contrast,
    tintColor = tokens.tintColor,
    darkOverlayColor = tokens.darkOverlay,
    highlightTopColor = tokens.highlightTop,
    specularInnerColor = tokens.specularInner,
    strokeColor = tokens.strokeColor,
    shadowColor = tokens.shadowColor,
    cornerRadius = cornerRadius,
    width = width,
    height = height,
    blurSamples = tokens.blurSamples
)
