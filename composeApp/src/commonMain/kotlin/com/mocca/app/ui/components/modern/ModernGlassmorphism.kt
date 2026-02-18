package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.LocalExtendedColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * A reusable modifier that applies a premium glassmorphic effect without blurring content.
 * Uses gradients and layered borders to simulate depth and translucency.
 */
@Composable
fun Modifier.glassy(
    shape: Shape,
    borderWidth: Dp = 0.5.dp,
    backgroundColor: Color = LocalExtendedColors.current.glassBackground,
    borderColor: Color = LocalExtendedColors.current.glassBorder
): Modifier = this.then(
    Modifier
        .clip(shape)
        .drawBehind {
            // Subtle vertical gradient for depth
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = backgroundColor.alpha * 1.2f),
                        backgroundColor,
                        backgroundColor.copy(alpha = backgroundColor.alpha * 0.8f)
                    )
                )
            )
        }
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.6f), // Brighter top edge
                    borderColor.copy(alpha = 0.1f), // Fading sides
                    borderColor.copy(alpha = 0.05f) // Subtle bottom
                )
            ),
            shape = shape
        )
)

/**
 * Premium glassmorphic effect with subtle glow for floating components.
 * Enhanced version with top-edge glow and refined depth.
 */
@Composable
fun Modifier.glassyPremium(
    shape: Shape,
    borderWidth: Dp = 0.75.dp,
    backgroundColor: Color = Color(0xDD1A1A1A), // 87% opacity for better visibility
    borderColor: Color = Color(0x55FFFFFF),     // Brighter white border for contrast
    glowColor: Color = Color(0x1A00D9A5)        // Subtle mint glow
): Modifier = this.then(
    Modifier
        .clip(shape)
        .drawBehind {
            // Main background with vertical gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = backgroundColor.alpha * 1.15f),
                        backgroundColor,
                        backgroundColor.copy(alpha = backgroundColor.alpha * 0.85f)
                    )
                )
            )
            
            // Subtle top glow line
            val glowHeight = 1.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0f),
                        glowColor.copy(alpha = 0.4f),
                        glowColor.copy(alpha = 0.4f),
                        glowColor.copy(alpha = 0f)
                    ),
                    startX = 0f,
                    endX = size.width
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, glowHeight)
            )
        }
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.5f),  // Bright top edge
                    borderColor.copy(alpha = 0.15f), // Mid fade
                    borderColor.copy(alpha = 0.08f)  // Subtle bottom
                )
            ),
            shape = shape
        )
)

/**
 * Simplified version of glassy modifier for performance-critical areas.
 */
@Composable
fun Modifier.glassySimple(
    shape: Shape,
    borderWidth: Dp = 0.5.dp,
    backgroundColor: Color = LocalExtendedColors.current.glassBackground,
    borderColor: Color = LocalExtendedColors.current.glassBorder
): Modifier = this.then(
    Modifier
        .clip(shape)
        .background(backgroundColor)
        .border(
            width = borderWidth,
            color = borderColor.copy(alpha = 0.3f),
            shape = shape
        )
)

// ═══════════════════════════════════════════════════════════════════════════
// LIQUID GLASS - 2024/2025 SOTA Glassmorphism with True Blur
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Default values for Liquid Glass effects.
 * Provides optimized blur and styling configurations for modern glassmorphism.
 */
object LiquidGlassDefaults {
    /** Default blur radius for liquid glass effect (30dp for smooth, premium feel) */
    val blurRadius: Dp = 30.dp
    
    /** Default background tint color with 40% opacity */
    val backgroundColor: Color = Color(0x661A1A1A)
    
    /** Default border color with subtle white */
    val borderColor: Color = Color(0x40FFFFFF)
    
    /** Default mint accent glow color */
    val glowColor: Color = Color(0x3000D9A5)
    
    /** Default border width */
    val borderWidth: Dp = 0.75.dp
    
    /**
     * Creates a HazeStyle with liquid glass styling.
     * 
     * @param backgroundColor Background tint color
     * @param blurRadius Blur radius in Dp
     * @param noiseFactor Noise texture factor (0f-1f) for premium feel
     */
    @Composable
    fun hazeStyle(
        backgroundColor: Color = this.backgroundColor,
        blurRadius: Dp = this.blurRadius,
        noiseFactor: Float = 0.15f
    ): HazeStyle = HazeStyle(
        backgroundColor = Color.Transparent,
        tints = listOf(
            HazeTint(color = backgroundColor)
        ),
        blurRadius = blurRadius,
        noiseFactor = noiseFactor
    )
}

/**
 * Premium Liquid Glass modifier using Haze for true background blur.
 * This is the 2024/2025 state-of-the-art glassmorphism implementation.
 * 
 * Features:
 * - Real background blur using Haze library
 * - Layered gradient borders for depth
 * - Top-edge accent glow
 * - Optional noise texture for premium feel
 * 
 * IMPORTANT: Requires parent content to have `hazeSource(hazeState)` modifier
 * and this component must be overlaid on top of the blurred content.
 * 
 * Usage:
 * ```
 * val hazeState = rememberHazeState()
 * Box {
 *     // Content to blur
 *     LazyColumn(Modifier.hazeSource(hazeState)) { ... }
 *     
 *     // Liquid glass overlay
 *     Box(
 *         modifier = Modifier
 *             .liquidGlass(hazeState, shape = RoundedCornerShape(32.dp))
 *     ) { ... }
 * }
 * ```
 *
 * @param hazeState The HazeState shared with the content to blur
 * @param shape The shape of the glass container
 * @param style Custom HazeStyle (defaults to LiquidGlassDefaults.hazeStyle())
 * @param borderWidth Width of the gradient border
 * @param borderColor Border gradient colors (uses gradient if single color provided)
 * @param glowColor Top-edge glow color
 * @param showGlow Whether to show the top-edge glow effect
 */
@Composable
fun Modifier.liquidGlass(
    hazeState: HazeState,
    shape: Shape,
    style: HazeStyle = LiquidGlassDefaults.hazeStyle(),
    borderWidth: Dp = LiquidGlassDefaults.borderWidth,
    borderColor: Color = LiquidGlassDefaults.borderColor,
    glowColor: Color = LiquidGlassDefaults.glowColor,
    showGlow: Boolean = true
): Modifier = this.then(
    Modifier
        .clip(shape)
        .hazeEffect(hazeState) {
            // Apply style properties
            this@hazeEffect.style = style
        }
        .drawBehind {
            // Top-edge glow for premium liquid effect
            if (showGlow) {
                val glowHeight = 2.dp.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0f),
                            glowColor.copy(alpha = 0.6f),
                            glowColor.copy(alpha = 0.6f),
                            glowColor.copy(alpha = 0f)
                        ),
                        startX = 0f,
                        endX = size.width
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, glowHeight)
                )
            }
        }
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.6f),  // Bright top edge
                    borderColor.copy(alpha = 0.2f),  // Mid fade
                    borderColor.copy(alpha = 0.1f)   // Subtle bottom
                )
            ),
            shape = shape
        )
)

/**
 * Creates a HazeState for use with liquid glass components.
 * Remember this at the screen level and pass to both content and glass overlay.
 */
@Composable
fun rememberLiquidGlassState(): HazeState = rememberHazeState()
