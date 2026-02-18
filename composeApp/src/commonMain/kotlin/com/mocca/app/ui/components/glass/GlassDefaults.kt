package com.mocca.app.ui.components.glass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppShapes

/**
 * Default values for Liquid Glass components.
 * 
 * Provides factory methods and presets for glass effects.
 * 
 * NEW: Use backdrop-based system for true liquid glass:
 * - `rememberLiquidBackdrop()` - Create backdrop
 * - `Modifier.liquidGlassSurface()` - Apply glass effect
 * - `rememberLuminanceAnimation()` - Dynamic adaptation
 */
object GlassDefaults {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TOKEN FACTORIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates theme-aware glass tokens.
     * 
     * @param isDark Whether the app is in dark mode (default: true for MOCCA)
     */
    @Composable
    @ReadOnlyComposable
    fun tokens(isDark: Boolean = true): GlassThemeTokens = GlassThemeTokens(isDark)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE FACTORIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    fun shapeStandard(): Shape = AppShapes.large
    fun shapeFloating(): Shape = AppShapes.rounded2xl
    fun shapeCard(): Shape = AppShapes.card
    fun shapeButton(): Shape = AppShapes.pill
    fun shapeAppBar(): Shape = RoundedCornerShape(0.dp)
    fun shapeSheet(): Shape = AppShapes.bottomSheet
    fun shapeDialog(): Shape = AppShapes.dialog
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TINT COLOR FACTORIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    fun tintPrimary(): Color = GlassTokens.tintPrimary
    fun tintSecondary(): Color = GlassTokens.tintSecondary
    fun tintLight(): Color = GlassTokens.tintLight
    fun tintDark(): Color = Color.Transparent
    fun tintMint(): Color = GlassTokens.tintMintAccent
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BORDER COLOR FACTORIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    fun borderPrimary(): Color = GlassTokens.strokeColorDark
    fun borderHighlight(): Color = GlassTokens.borderHighlight
    fun borderSubtle(): Color = GlassTokens.borderShadow
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION DEFAULTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    val animationDurationMs: Int = GlassTokens.animationDurationMs
    const val parallaxMultiplier: Float = GlassTokens.parallaxMultiplier
}

/**
 * Preset configuration for a glass surface.
 * 
 * @deprecated Use backdrop-based liquid glass system instead.
 */
@Deprecated(
    message = "Use backdrop-based liquid glass system. See LiquidBackdrop.kt",
    level = DeprecationLevel.WARNING
)
data class GlassPreset(
    val tokens: GlassThemeTokens,
    val shape: Shape,
    @Suppress("DEPRECATION")
    val shaderParams: GlassShaderParams
) {
    @Suppress("DEPRECATION")
    companion object {
        @Deprecated("Use backdrop-based system", level = DeprecationLevel.WARNING)
        val Default = GlassPreset(
            tokens = GlassThemeTokens(),
            shape = AppShapes.large,
            shaderParams = GlassShaderParams.Default
        )
    }
}
