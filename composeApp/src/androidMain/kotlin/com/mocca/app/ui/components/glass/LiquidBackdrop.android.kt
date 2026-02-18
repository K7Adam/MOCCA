package com.mocca.app.ui.components.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.sign
import com.kyant.backdrop.backdrops.layerBackdrop as nativeLayerBackdrop

/**
 * Android implementation of LiquidBackdrop using Kyant0's backdrop library.
 * 
 * This provides true liquid glass effects with:
 * - Lens refraction (optical distortion)
 * - Blur (frosted glass effect)
 * - Vibrancy (color saturation boost)
 * - Luminance adaptation (brightness/contrast adjustment)
 * 
 * Implementation inspired by SimpMusic's beautiful liquid glass UI.
 */

// Typealias to Kyant0's LayerBackdrop
actual typealias LiquidBackdrop = LayerBackdrop

/**
 * Creates a LayerBackdrop that captures content for liquid glass effects.
 */
@Composable
actual fun rememberLiquidBackdrop(backgroundColor: Color): LiquidBackdrop =
    rememberLayerBackdrop {
        // Draw background color first (Pitch Black for MOCCA)
        drawRect(backgroundColor)
        // Then draw the content on top
        drawContent()
    }

/**
 * Marks content as the source for backdrop sampling.
 */
actual fun Modifier.liquidBackdropSource(backdrop: LiquidBackdrop): Modifier =
    this.nativeLayerBackdrop(backdrop)

/**
 * Draws backdrop with liquid glass effects using luminance adaptation.
 * 
 * Effects are applied in the critical order: color filter ⇒ blur ⇒ lens
 */
actual fun Modifier.drawLiquidGlass(
    backdrop: LiquidBackdrop,
    layer: GraphicsLayer,
    luminance: Float,
    shape: Shape
): Modifier = drawLiquidGlass(backdrop, layer, luminance, shape, 0.1f)

/**
 * Draws backdrop with liquid glass effects and custom surface alpha.
 */
actual fun Modifier.drawLiquidGlass(
    backdrop: LiquidBackdrop,
    layer: GraphicsLayer,
    luminance: Float,
    shape: Shape,
    surfaceAlpha: Float
): Modifier {
    // Calculate luminance factor with sign preservation for smooth adaptation
    // This creates a smooth transition when luminance crosses 0.5
    val l = (luminance * 2f - 1f).let { sign(it) * it * it }
    
    return this.drawBackdrop(
        backdrop = backdrop,
        effects = {
            // ═══════════════════════════════════════════════════════════════════
            // EFFECT ORDER (CRITICAL): color filter ⇒ blur ⇒ lens
            // ═══════════════════════════════════════════════════════════════════
            
            // 1. VIBRANCY - Boost saturation for vivid colors through glass
            vibrancy() // Equivalent to colorControls(saturation = 1.5f)
            
            // 2. COLOR CONTROLS - Adapt brightness/contrast based on luminance
            colorControls(
                brightness = if (l > 0f) {
                    // Bright background: increase brightness
                    lerp(0.1f, 0.5f, l)
                } else {
                    // Dark background: decrease brightness for contrast
                    lerp(0.1f, -0.2f, -l)
                },
                contrast = if (l > 0f) {
                    // Bright background: reduce contrast for softer look
                    lerp(1f, 0f, l)
                } else {
                    // Dark background: maintain contrast
                    1f
                },
                saturation = 1.5f
            )
            
            // 3. BLUR - Frosted glass effect, stronger for bright backgrounds
            blur(
                if (l > 0f) {
                    lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                } else {
                    lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                }
            )
            
            // 4. LENS - Optical refraction for true liquid glass look
            // refractionHeight: Amount of lens distortion at corners
            // refractionAmount: Distance from center for refraction effect
            lens(
                refractionHeight = 24f.dp.toPx(),
                refractionAmount = size.minDimension / 2f,
                depthEffect = true
            )
        },
        onDrawBackdrop = { drawBackdrop ->
            // Draw the backdrop with all effects
            drawBackdrop()
            // Record to graphics layer for luminance sampling
            layer.record { drawBackdrop() }
        },
        shape = { shape },
        onDrawSurface = { 
            // Draw a subtle dark overlay for text readability
            drawRect(Color.Black.copy(alpha = surfaceAlpha)) 
        }
    )
}
