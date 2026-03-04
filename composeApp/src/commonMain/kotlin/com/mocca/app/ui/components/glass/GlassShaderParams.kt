@file:Suppress("DEPRECATION")

package com.mocca.app.ui.components.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glass Shader Parameters - LEGACY COMPATIBILITY SHIM
 * 
 * This class is deprecated. Use the new backdrop-based system instead:
 * - `rememberLiquidBackdrop()` for creating backdrops
 * - `Modifier.drawLiquidGlass()` for applying effects
 * - `rememberLuminanceAnimation()` for luminance detection
 * 
 * Kept for backward compatibility with existing glass components.
 * 
 * @deprecated Use backdrop-based liquid glass system
 */
@Deprecated(
    message = "Use backdrop-based liquid glass system. See LiquidBackdrop.kt and GlassModifier.kt",
    level = DeprecationLevel.WARNING
)
data class GlassShaderParams(
    val blurRadius: Float = 24f,
    val refractionStrength: Float = 0.35f,
    val chromaticAberration: Float = 0.008f,
    val saturation: Float = 1.3f,
    val contrast: Float = 1.1f,
    val tintColor: Color = Color.Transparent,
    val darkOverlayColor: Color = Color.Transparent,
    val highlightTopColor: Color = Color(0x33A0A3B5),
    val specularInnerColor: Color = Color(0x14A0A3B5),
    val strokeColor: Color = Color(0x26A0A3B5),
    val shadowColor: Color = Color(0x1A0D0E12),
    val cornerRadius: Float = 32f,
    val width: Float = 0f,
    val height: Float = 0f,
    val blurSamples: Int = 4,
    val enableRefraction: Boolean = true,
    val enableChromaticAberration: Boolean = true,
    val enableSpecular: Boolean = true,
    val reducedTransparency: Boolean = false,
    val reducedMotion: Boolean = false
) {
    @Deprecated("Use backdrop-based system", level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated("Use backdrop-based system", level = DeprecationLevel.WARNING)
        val Default = GlassShaderParams()
        
        @Deprecated("Use liquidGlassNavBar()", level = DeprecationLevel.WARNING)
        val Floating = GlassShaderParams(
            blurRadius = 32f,
            refractionStrength = 0.5f,
            chromaticAberration = 0.012f,
            saturation = 1.4f,
            contrast = 1.15f
        )
        
        @Deprecated("Use glassButton()", level = DeprecationLevel.WARNING)
        val Button = GlassShaderParams(
            blurRadius = 12f,
            refractionStrength = 0.2f,
            chromaticAberration = 0.005f,
            saturation = 1.2f,
            cornerRadius = 9999f
        )
        
        @Deprecated("Use glassAppBar() or glassy()", level = DeprecationLevel.WARNING)
        val AppBar = GlassShaderParams(
            blurRadius = 20f,
            refractionStrength = 0.25f,
            chromaticAberration = 0.006f,
            cornerRadius = 0f
        )
        
        @Deprecated("Use glassy() with bottomSheet shape", level = DeprecationLevel.WARNING)
        val Sheet = GlassShaderParams(
            blurRadius = 28f,
            refractionStrength = 0.4f,
            chromaticAberration = 0.01f,
            cornerRadius = 24f
        )
        
        @Deprecated("Use glassy()", level = DeprecationLevel.WARNING)
        val Dialog = GlassShaderParams(
            blurRadius = 24f,
            refractionStrength = 0.35f,
            chromaticAberration = 0.008f,
            cornerRadius = 24f
        )
        
        @Deprecated("Use glassFallback()", level = DeprecationLevel.WARNING)
        val Fallback = GlassShaderParams(
            blurRadius = 0f,
            refractionStrength = 0f,
            chromaticAberration = 0f,
            enableRefraction = false,
            enableChromaticAberration = false
        )
        
        @Deprecated("Use solid backgrounds for accessibility", level = DeprecationLevel.WARNING)
        val Accessible = GlassShaderParams(
            blurRadius = 0f,
            refractionStrength = 0f,
            chromaticAberration = 0f,
            enableRefraction = false,
            enableChromaticAberration = false,
            reducedTransparency = true
        )
        
        @Deprecated("Use backdrop-based system", level = DeprecationLevel.WARNING)
        fun fromTokens(
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
    }
}
