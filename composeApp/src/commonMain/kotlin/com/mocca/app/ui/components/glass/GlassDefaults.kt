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
 * Default values and factory methods for Liquid Glass components.
 * 
 * Provides preset configurations for different use cases:
 * - Standard glass surfaces
 * - Floating elements (bottom bars, FABs)
 * - Buttons and controls
 * - App bars
 * - Sheets and dialogs
 * 
 * Usage:
 * ```kotlin
 * GlassSurface(
 *     tokens = GlassDefaults.tokens(),
 *     shape = GlassDefaults.shapeFloating()
 * )
 * ```
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
    
    /**
     * Creates shader parameters from tokens.
     */
    @Composable
    @ReadOnlyComposable
    fun shaderParams(
        tokens: GlassThemeTokens = tokens(),
        width: Float = 0f,
        height: Float = 0f,
        cornerRadius: Float = tokens.blurRadius.value
    ): GlassShaderParams = GlassShaderParams.fromTokens(tokens, width, height, cornerRadius)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE FACTORIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Standard glass surface shape.
     */
    fun shapeStandard(): Shape = AppShapes.large
    
    /**
     * Floating element shape (more rounded).
     */
    fun shapeFloating(): Shape = AppShapes.rounded2xl
    
    /**
     * Card shape.
     */
    fun shapeCard(): Shape = AppShapes.card
    
    /**
     * Button shape (pill).
     */
    fun shapeButton(): Shape = AppShapes.pill
    
    /**
     * App bar shape (no rounding - full width).
     */
    fun shapeAppBar(): Shape = RoundedCornerShape(0.dp)
    
    /**
     * Bottom sheet shape (rounded top).
     */
    fun shapeSheet(): Shape = AppShapes.bottomSheet
    
    /**
     * Dialog shape.
     */
    fun shapeDialog(): Shape = AppShapes.dialog
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TINT COLOR FACTORIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Primary tint color - standard dark overlay.
     */
    fun tintPrimary(): Color = GlassTokens.tintPrimary
    
    /**
     * Secondary tint color - for layered elements.
     */
    fun tintSecondary(): Color = GlassTokens.tintSecondary
    
    /**
     * Light tint color - for active/highlighted surfaces.
     */
    fun tintLight(): Color = GlassTokens.tintLight
    
    /**
     * Dark tint color - for prominent surfaces.
     */
    fun tintDark(): Color = GlassTokens.tintDarkOverlayDark
    
    /**
     * Mint accent tint - brand color overlay.
     */
    fun tintMint(): Color = GlassTokens.tintMintAccent
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BORDER COLOR FACTORIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Primary border color.
     */
    fun borderPrimary(): Color = GlassTokens.strokeColorDark
    
    /**
     * Highlighted border color.
     */
    fun borderHighlight(): Color = GlassTokens.borderHighlight
    
    /**
     * Subtle border color.
     */
    fun borderSubtle(): Color = GlassTokens.borderShadow
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRESET CONFIGURATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Standard glass configuration for general surfaces.
     */
    @Composable
    @ReadOnlyComposable
    fun standard(
        isDark: Boolean = true,
        cornerRadius: Dp = 16.dp
    ): GlassPreset = GlassPreset(
        tokens = tokens(isDark),
        shape = RoundedCornerShape(cornerRadius),
        shaderParams = GlassShaderParams.Default.copy(
            cornerRadius = cornerRadius.value
        )
    )
    
    /**
     * Floating glass configuration for bottom bars and FABs.
     * Uses heavier blur and stronger effects for prominent floating appearance.
     */
    @Composable
    @ReadOnlyComposable
    fun floating(
        isDark: Boolean = true,
        cornerRadius: Dp = 32.dp
    ): GlassPreset = GlassPreset(
        tokens = tokens(isDark),
        shape = RoundedCornerShape(cornerRadius),
        shaderParams = GlassShaderParams.Floating.copy(
            cornerRadius = cornerRadius.value
        )
    )
    
    /**
     * Button glass configuration for small controls.
     * Uses lighter effects suitable for touch targets.
     */
    @Composable
    @ReadOnlyComposable
    fun button(
        isDark: Boolean = true
    ): GlassPreset = GlassPreset(
        tokens = tokens(isDark),
        shape = shapeButton(),
        shaderParams = GlassShaderParams.Button
    )
    
    /**
     * App bar glass configuration.
     * Optimized for full-width header bars.
     */
    @Composable
    @ReadOnlyComposable
    fun appBar(
        isDark: Boolean = true
    ): GlassPreset = GlassPreset(
        tokens = tokens(isDark),
        shape = shapeAppBar(),
        shaderParams = GlassShaderParams.AppBar
    )
    
    /**
     * Sheet glass configuration for bottom sheets.
     */
    @Composable
    @ReadOnlyComposable
    fun sheet(
        isDark: Boolean = true
    ): GlassPreset = GlassPreset(
        tokens = tokens(isDark),
        shape = shapeSheet(),
        shaderParams = GlassShaderParams.Sheet
    )
    
    /**
     * Dialog glass configuration.
     */
    @Composable
    @ReadOnlyComposable
    fun dialog(
        isDark: Boolean = true
    ): GlassPreset = GlassPreset(
        tokens = tokens(isDark),
        shape = shapeDialog(),
        shaderParams = GlassShaderParams.Dialog
    )
    
    /**
     * Accessibility-friendly configuration.
     * Uses solid backgrounds instead of transparency.
     */
    @Composable
    @ReadOnlyComposable
    fun accessible(
        isDark: Boolean = true
    ): GlassPreset = GlassPreset(
        tokens = tokens(isDark),
        shape = shapeStandard(),
        shaderParams = GlassShaderParams.Accessible
    )
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION DEFAULTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Default animation duration for glass transitions.
     */
    val animationDurationMs: Int = GlassTokens.animationDurationMs
    
    /**
     * Parallax multiplier for subtle motion effects.
     */
    const val parallaxMultiplier: Float = GlassTokens.parallaxMultiplier
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PERFORMANCE DEFAULTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Default blur sample count for quality/performance balance.
     */
    val blurSamplesDefault: Int = GlassTokens.blurSamplesDefault
    
    /**
     * Light blur sample count for better performance.
     */
    val blurSamplesLight: Int = GlassTokens.blurSamplesLight
    
    /**
     * Heavy blur sample count for maximum quality.
     */
    val blurSamplesHeavy: Int = GlassTokens.blurSamplesHeavy
}

/**
 * Preset configuration for a glass surface.
 * 
 * Combines tokens, shape, and shader parameters into a single configuration.
 */
data class GlassPreset(
    val tokens: GlassThemeTokens,
    val shape: Shape,
    val shaderParams: GlassShaderParams
) {
    companion object {
        /**
         * Default glass preset.
         */
        val Default = GlassPreset(
            tokens = GlassThemeTokens(),
            shape = AppShapes.large,
            shaderParams = GlassShaderParams.Default
        )
    }
}
