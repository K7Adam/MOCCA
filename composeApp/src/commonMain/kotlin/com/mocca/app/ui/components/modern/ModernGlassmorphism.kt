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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.LocalExtendedColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

// ═══════════════════════════════════════════════════════════════════════════
// LIQUID GLASS - 2024/2025 iOS 26 Inspired Glassmorphism
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Default values for Liquid Glass effects.
 * Optimized for authentic iOS 26-style glassmorphism with:
 * - Proper blur radius for depth perception
 * - Tint colors that maintain text contrast
 * - Noise texture for tactile premium feel
 */
object LiquidGlassDefaults {
    // ═════════════════════════════════════════════════════════════════════════
    // BLUR PARAMETERS
    // ═════════════════════════════════════════════════════════════════════════

    /** Default blur radius - 25dp for smooth, readable blur */
    val blurRadius: Dp = 25.dp

    /** Thick blur for floating elements */
    val blurRadiusThick: Dp = 35.dp

    /** Thin blur for subtle effects */
    val blurRadiusThin: Dp = 15.dp

    // ═════════════════════════════════════════════════════════════════════════
    // TINT COLORS - High opacity for text contrast
    // ═════════════════════════════════════════════════════════════════════════

    /** Primary tint - 75% opacity dark for excellent legibility */
    val tintPrimary: Color = Color(0xBF1A1A1A)

    /** Secondary tint - 65% opacity for layered elements */
    val tintSecondary: Color = Color(0xA61E1E1E)

    /** Light tint - 55% opacity for elevated surfaces */
    val tintLight: Color = Color(0x8C252525)

    /** Ultra-thin tint - 45% opacity for overlays */
    val tintUltraThin: Color = Color(0x73333333)

    // ═════════════════════════════════════════════════════════════════════════
    // SPECULAR HIGHLIGHTS - Simulates light reflection
    // ═════════════════════════════════════════════════════════════════════════

    /** Top edge specular - bright white highlight */
    val specularTop: Color = Color(0x40FFFFFF)

    /** Inner specular glow - subtle white */
    val specularInner: Color = Color(0x1AFFFFFF)

    /** Refraction accent - mint green glow */
    val refractionAccent: Color = Color(0x3300D9A5)

    // ═════════════════════════════════════════════════════════════════════════
    // BORDER STYLING
    // ═════════════════════════════════════════════════════════════════════════

    /** Primary border - white with 25% opacity */
    val borderPrimary: Color = Color(0x40FFFFFF)

    /** Border highlight - top edge brighter */
    val borderHighlight: Color = Color(0x66FFFFFF)

    /** Border shadow - bottom edge darker */
    val borderShadow: Color = Color(0x1AFFFFFF)

    /** Default border width */
    val borderWidth: Dp = 1.dp

    /** Thin border for subtle effects */
    val borderWidthThin: Dp = 0.5.dp

    // ═════════════════════════════════════════════════════════════════════════
    // NOISE TEXTURE
    // ═════════════════════════════════════════════════════════════════════════

    /** Default noise factor - 12% for premium tactile feel */
    const val noiseFactor: Float = 0.12f

    /** Subtle noise - 8% */
    const val noiseFactorSubtle: Float = 0.08f

    /** Pronounced noise - 18% */
    const val noiseFactorPronounced: Float = 0.18f

    // ═════════════════════════════════════════════════════════════════════════
    // PRESET STYLES
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Primary liquid glass style - for main floating elements.
     * High tint opacity ensures excellent text/icon contrast.
     */
    @Composable
    fun primary(
        blurRadius: Dp = this.blurRadius,
        noiseFactor: Float = this.noiseFactor
    ): HazeStyle = HazeStyle(
        backgroundColor = Color.Transparent,
        tints = listOf(
            HazeTint(color = tintPrimary)
        ),
        blurRadius = blurRadius,
        noiseFactor = noiseFactor
    )

    /**
     * Secondary liquid glass style - for layered elements.
     * Slightly more transparent for depth hierarchy.
     */
    @Composable
    fun secondary(
        blurRadius: Dp = blurRadiusThin,
        noiseFactor: Float = noiseFactorSubtle
    ): HazeStyle = HazeStyle(
        backgroundColor = Color.Transparent,
        tints = listOf(
            HazeTint(color = tintSecondary)
        ),
        blurRadius = blurRadius,
        noiseFactor = noiseFactor
    )

    /**
     * Thick liquid glass style - for prominent floating elements.
     * Maximum blur for dramatic depth effect.
     */
    @Composable
    fun thick(
        blurRadius: Dp = blurRadiusThick,
        noiseFactor: Float = noiseFactorPronounced
    ): HazeStyle = HazeStyle(
        backgroundColor = Color.Transparent,
        tints = listOf(
            HazeTint(color = tintPrimary)
        ),
        blurRadius = blurRadius,
        noiseFactor = noiseFactor
    )

    /**
     * Ultra-thin liquid glass style - for overlays and modals.
     * Light tint for subtle presence.
     */
    @Composable
    fun ultraThin(
        blurRadius: Dp = blurRadiusThin,
        noiseFactor: Float = noiseFactorSubtle
    ): HazeStyle = HazeStyle(
        backgroundColor = Color.Transparent,
        tints = listOf(
            HazeTint(color = tintUltraThin)
        ),
        blurRadius = blurRadius,
        noiseFactor = noiseFactor
    )

    /**
     * Creates a custom HazeStyle with liquid glass styling.
     *
     * @param backgroundColor Background tint color (recommend 65-80% opacity for contrast)
     * @param blurRadius Blur radius in Dp (15-35dp recommended)
     * @param noiseFactor Noise texture factor (0f-0.2f recommended)
     */
    @Composable
    fun hazeStyle(
        backgroundColor: Color = tintPrimary,
        blurRadius: Dp = this.blurRadius,
        noiseFactor: Float = this.noiseFactor
    ): HazeStyle = HazeStyle(
        backgroundColor = Color.Transparent,
        tints = listOf(
            HazeTint(color = backgroundColor)
        ),
        blurRadius = blurRadius,
        noiseFactor = noiseFactor
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// LEGACY GLASS MODIFIERS (Non-blur fallbacks)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A reusable modifier that applies a premium glassmorphic effect without blurring content.
 * Uses gradients and layered borders to simulate depth and translucency.
 * Use this when Haze blur is not available or for performance-critical areas.
 */
@Composable
fun Modifier.glassy(
    shape: Shape,
    borderWidth: Dp = LiquidGlassDefaults.borderWidthThin,
    backgroundColor: Color = LiquidGlassDefaults.tintPrimary,
    borderColor: Color = LiquidGlassDefaults.borderPrimary
): Modifier = this.then(
    Modifier
        .clip(shape)
        .drawBehind {
            // Vertical gradient for depth simulation
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = backgroundColor.alpha * 1.1f),
                        backgroundColor,
                        backgroundColor.copy(alpha = backgroundColor.alpha * 0.9f)
                    )
                )
            )

            // Subtle top specular highlight
            drawSpecularHighlight(
                width = size.width,
                height = size.height,
                specularColor = LiquidGlassDefaults.specularInner
            )
        }
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.5f), // Brighter top
                    borderColor.copy(alpha = 0.2f), // Mid fade
                    borderColor.copy(alpha = 0.08f) // Subtle bottom
                )
            ),
            shape = shape
        )
)

/**
 * Premium glassmorphic effect with specular highlights and refined depth.
 * Enhanced version with top-edge glow for floating components.
 */
@Composable
fun Modifier.glassyPremium(
    shape: Shape,
    borderWidth: Dp = LiquidGlassDefaults.borderWidth,
    backgroundColor: Color = LiquidGlassDefaults.tintPrimary,
    borderColor: Color = LiquidGlassDefaults.borderPrimary,
    specularColor: Color = LiquidGlassDefaults.specularTop,
    refractionColor: Color = LiquidGlassDefaults.refractionAccent
): Modifier = this.then(
    Modifier
        .clip(shape)
        .drawBehind {
            // Multi-layer background for depth
            // Layer 1: Primary gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = backgroundColor.alpha * 1.08f),
                        backgroundColor,
                        backgroundColor.copy(alpha = backgroundColor.alpha * 0.92f)
                    )
                )
            )

            // Layer 2: Top specular highlight (simulates light reflection)
            drawSpecularHighlight(
                width = size.width,
                height = size.height,
                specularColor = specularColor
            )

            // Layer 3: Refraction accent (subtle color shift)
            drawRefractionGlow(
                width = size.width,
                refractionColor = refractionColor
            )
        }
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.6f),  // Bright top edge
                    borderColor.copy(alpha = 0.25f), // Mid fade
                    borderColor.copy(alpha = 0.1f)   // Subtle bottom
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
    borderWidth: Dp = LiquidGlassDefaults.borderWidthThin,
    backgroundColor: Color = LiquidGlassDefaults.tintSecondary,
    borderColor: Color = LiquidGlassDefaults.borderPrimary
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
// LIQUID GLASS - True Blur with Haze
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Premium Liquid Glass modifier using Haze for true background blur.
 * This is the 2024/2025 state-of-the-art glassmorphism implementation.
 *
 * Features:
 * - Real background blur using Haze library
 * - Specular highlights for light reflection simulation
 * - Refraction glow for color depth
 * - Layered gradient borders for edge definition
 * - Noise texture for premium tactile feel
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
 * @param style Custom HazeStyle (defaults to LiquidGlassDefaults.primary())
 * @param borderWidth Width of the gradient border
 * @param borderColor Border gradient colors
 * @param specularColor Top-edge specular highlight color
 * @param refractionColor Refraction accent color
 * @param showSpecular Whether to show specular highlights
 * @param showRefraction Whether to show refraction glow
 */
@Composable
fun Modifier.liquidGlass(
    hazeState: HazeState,
    shape: Shape,
    style: HazeStyle = LiquidGlassDefaults.primary(),
    borderWidth: Dp = LiquidGlassDefaults.borderWidth,
    borderColor: Color = LiquidGlassDefaults.borderPrimary,
    specularColor: Color = LiquidGlassDefaults.specularTop,
    refractionColor: Color = LiquidGlassDefaults.refractionAccent,
    showSpecular: Boolean = true,
    showRefraction: Boolean = true
): Modifier = this.then(
    Modifier
        .clip(shape)
        .hazeEffect(hazeState) {
            this@hazeEffect.style = style
        }
        .drawBehind {
            // Specular highlight for premium liquid effect
            if (showSpecular) {
                drawSpecularHighlight(
                    width = size.width,
                    height = size.height,
                    specularColor = specularColor
                )
            }

            // Refraction glow for color depth
            if (showRefraction) {
                drawRefractionGlow(
                    width = size.width,
                    refractionColor = refractionColor
                )
            }
        }
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.6f),  // Bright top edge
                    borderColor.copy(alpha = 0.25f), // Mid fade
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

// ═══════════════════════════════════════════════════════════════════════════
// INTERNAL DRAW HELPERS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Draws a specular highlight at the top of the component.
 * Simulates light reflection for authentic glass appearance.
 */
private fun DrawScope.drawSpecularHighlight(
    width: Float,
    height: Float,
    specularColor: Color
) {
    val highlightHeight = 1.5.dp.toPx()
    val gradientWidth = width * 0.7f
    val startX = (width - gradientWidth) / 2f

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                specularColor.copy(alpha = 0f),
                specularColor,
                specularColor,
                specularColor.copy(alpha = 0f)
            ),
            startX = startX,
            endX = startX + gradientWidth
        ),
        topLeft = Offset(startX, 0f),
        size = Size(gradientWidth, highlightHeight)
    )

    // Secondary inner glow
    val innerGlowHeight = 4.dp.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                specularColor.copy(alpha = specularColor.alpha * 0.5f),
                specularColor.copy(alpha = 0f)
            )
        ),
        topLeft = Offset(0f, highlightHeight),
        size = Size(width, innerGlowHeight)
    )
}

/**
 * Draws a refraction glow at the top edge.
 * Simulates color shift from light passing through glass.
 */
private fun DrawScope.drawRefractionGlow(
    width: Float,
    refractionColor: Color
) {
    val glowHeight = 2.dp.toPx()
    val gradientWidth = width * 0.8f
    val startX = (width - gradientWidth) / 2f

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                refractionColor.copy(alpha = 0f),
                refractionColor.copy(alpha = 0.6f),
                refractionColor.copy(alpha = 0.6f),
                refractionColor.copy(alpha = 0f)
            ),
            startX = startX,
            endX = startX + gradientWidth
        ),
        topLeft = Offset(startX, 0f),
        size = Size(gradientWidth, glowHeight)
    )
}
