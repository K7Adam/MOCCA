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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mocca.app.ui.theme.AppShapes
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquefiable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

// ═══════════════════════════════════════════════════════════════════════════
// LIQUID GLASS - iOS 26 Inspired True Glass Effect
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Default values for Liquid Glass effects using the `liquid` library.
 * 
 * TRUE Liquid Glass requires:
 * - **Frost**: Blur effect (creates the frosted appearance)
 * - **Refraction**: Lens distortion (objects behind appear bent)
 * - **Curve**: Spherical lens effect
 * - **Edge**: Rim lighting around the edge
 * - **Saturation**: Color vibrancy boost (iOS increases saturation)
 * - **Dispersion**: Chromatic aberration (prism effect at edges)
 * - **Tint**: Optional color overlay for contrast
 */
object LiquidGlassDefaults {
    // ═════════════════════════════════════════════════════════════════════════
    // FROST (Blur)
    // ═════════════════════════════════════════════════════════════════════════

    /** Default frost blur - 12dp for smooth frosted appearance */
    val frostDefault: Dp = 12.dp

    /** Light frost - 8dp for subtle effect */
    val frostLight: Dp = 8.dp

    /** Heavy frost - 18dp for prominent glass */
    val frostHeavy: Dp = 18.dp

    // ═════════════════════════════════════════════════════════════════════════
    // REFRACTION (Lens Distortion)
    // ═════════════════════════════════════════════════════════════════════════

    /** Default refraction - moderate lens distortion */
    const val refractionDefault: Float = 0.35f

    /** Subtle refraction - minimal distortion */
    const val refractionSubtle: Float = 0.2f

    /** Strong refraction - pronounced lens effect */
    const val refractionStrong: Float = 0.5f

    // ═════════════════════════════════════════════════════════════════════════
    // CURVE (Spherical Lens)
    // ═════════════════════════════════════════════════════════════════════════

    /** Default curve - balanced spherical effect */
    const val curveDefault: Float = 0.35f

    /** Subtle curve - gentle curvature */
    const val curveSubtle: Float = 0.2f

    /** Strong curve - pronounced spherical lens */
    const val curveStrong: Float = 0.5f

    // ═════════════════════════════════════════════════════════════════════════
    // EDGE (Rim Lighting)
    // ═════════════════════════════════════════════════════════════════════════

    /** Default edge - subtle rim light */
    const val edgeDefault: Float = 0.12f

    /** Prominent edge - noticeable rim lighting */
    const val edgeProminent: Float = 0.2f

    // ═════════════════════════════════════════════════════════════════════════
    // SATURATION (Color Vibrancy)
    // ═════════════════════════════════════════════════════════════════════════

    /** Default saturation - iOS-like vibrancy boost */
    const val saturationDefault: Float = 1.3f

    /** High saturation - vivid colors through glass */
    const val saturationHigh: Float = 1.5f

    /** No saturation change */
    const val saturationNone: Float = 1f

    // ═════════════════════════════════════════════════════════════════════════
    // DISPERSION (Chromatic Aberration)
    // ═════════════════════════════════════════════════════════════════════════

    /** Default dispersion - subtle prism effect */
    const val dispersionDefault: Float = 0.15f

    /** Strong dispersion - noticeable color separation */
    const val dispersionStrong: Float = 0.3f

    /** No dispersion */
    const val dispersionNone: Float = 0f

    // ═════════════════════════════════════════════════════════════════════════
    // TINT COLORS
    // ═════════════════════════════════════════════════════════════════════════

    /** Dark tint for contrast - most common for UI */
    val tintDark: Color = Color(0x40000000) // 25% black

    /** Semi-dark tint */
    val tintSemiDark: Color = Color(0x30000000) // 19% black

    /** Light tint for bright backgrounds */
    val tintLight: Color = Color(0x20FFFFFF) // 12% white

    /** Mint accent tint */
    val tintMint: Color = Color(0x1000D9A5) // 6% mint

    // Legacy aliases for backward compatibility
    /** Primary tint - 75% opacity dark for excellent legibility */
    val tintPrimary: Color = Color(0xBF1A1A1A)

    /** Secondary tint - 65% opacity for layered elements */
    val tintSecondary: Color = Color(0xA61E1E1E)

    // ═════════════════════════════════════════════════════════════════════════
    // BORDER COLORS
    // ═════════════════════════════════════════════════════════════════════════

    /** Primary border - white with 25% opacity */
    val borderPrimary: Color = Color(0x40FFFFFF)

    /** Border highlight - top edge brighter */
    val borderHighlight: Color = Color(0x66FFFFFF)

    /** Border shadow - bottom edge darker */
    val borderShadow: Color = Color(0x1AFFFFFF)

    // ═════════════════════════════════════════════════════════════════════════
    // SPECULAR HIGHLIGHTS
    // ═════════════════════════════════════════════════════════════════════════

    /** Top edge specular - bright white highlight */
    val specularTop: Color = Color(0x40FFFFFF)

    /** Inner specular glow - subtle white */
    val specularInner: Color = Color(0x1AFFFFFF)

    /** Refraction accent - mint green glow */
    val refractionAccent: Color = Color(0x3300D9A5)

    // ═════════════════════════════════════════════════════════════════════════
    // CONTRAST
    // ═════════════════════════════════════════════════════════════════════════

    /** Default contrast */
    const val contrastDefault: Float = 1.1f

    /** High contrast */
    const val contrastHigh: Float = 1.2f
}

// ═══════════════════════════════════════════════════════════════════════════
// LEGACY GLASS MODIFIERS (Non-liquid fallbacks)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Simple glassy modifier without blur or refraction.
 * Use only as fallback when liquid glass is not available.
 */
@Composable
fun Modifier.glassy(
    shape: Shape,
    borderWidth: Dp = 0.5.dp,
    backgroundColor: Color = Color(0x80000000),
    borderColor: Color = Color(0x40FFFFFF)
): Modifier = this.then(
    Modifier
        .clip(shape)
        .background(backgroundColor)
        .border(borderWidth, borderColor.copy(alpha = 0.3f), shape)
)

/**
 * Premium glassy modifier with gradient border.
 * Fallback when liquid glass is unavailable.
 */
@Composable
fun Modifier.glassyPremium(
    shape: Shape,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = Color(0x99000000),
    borderColor: Color = Color(0x40FFFFFF),
    glowColor: Color = Color(0x2000D9A5)
): Modifier = this.then(
    Modifier
        .clip(shape)
        .drawBehind {
            // Background gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = backgroundColor.alpha * 1.1f),
                        backgroundColor,
                        backgroundColor.copy(alpha = backgroundColor.alpha * 0.9f)
                    )
                )
            )
            // Top glow
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0f),
                        glowColor,
                        glowColor.copy(alpha = 0f)
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, 2.dp.toPx())
            )
        }
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.5f),
                    borderColor.copy(alpha = 0.15f),
                    borderColor.copy(alpha = 0.05f)
                )
            ),
            shape = shape
        )
)

/**
 * Simplified glassy modifier for performance.
 */
@Composable
fun Modifier.glassySimple(
    shape: Shape,
    borderWidth: Dp = 0.5.dp,
    backgroundColor: Color = Color(0x80000000),
    borderColor: Color = Color(0x30FFFFFF)
): Modifier = this.then(
    Modifier
        .clip(shape)
        .background(backgroundColor)
        .border(borderWidth, borderColor, shape)
)

// ═══════════════════════════════════════════════════════════════════════════
// TRUE LIQUID GLASS - Using the `liquid` library
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Creates a LiquidState for use with true liquid glass effects.
 * Remember this at the screen level and pass to both source and effect components.
 * 
 * IMPORTANT: The component with `Modifier.liquid()` must NOT have ancestors
 * with `Modifier.liquefiable()` using the same LiquidState.
 */
@Composable
fun rememberLiquidGlassState(): LiquidState = rememberLiquidState()

/**
 * TRUE Liquid Glass modifier using the `liquid` library.
 * This creates authentic iOS 26-style glass with:
 * - Lens refraction (objects behind appear distorted)
 * - Chromatic aberration (color separation at edges)
 * - Frosted blur
 * - Rim lighting
 * - Saturation boost
 * 
 * IMPORTANT: Requires parent content to have `Modifier.liquefiable(liquidState)`.
 * 
 * @param liquidState The LiquidState shared with the content to sample
 * @param shape The shape of the glass (rounded corners recommended)
 * @param frost Blur amount (default: 12dp)
 * @param refraction Lens distortion strength (0-1, default: 0.35)
 * @param curve Spherical lens effect (0-1, default: 0.35)
 * @param edge Rim lighting width (0-1, default: 0.12)
 * @param saturation Color vibrancy multiplier (default: 1.3)
 * @param dispersion Chromatic aberration strength (0-1, default: 0.15)
 * @param contrast Contrast adjustment (default: 1.1)
 * @param tint Color overlay for contrast (optional)
 */
@Composable
fun Modifier.liquidGlass(
    liquidState: LiquidState,
    shape: Shape = AppShapes.rounded2xl,
    frost: Dp = LiquidGlassDefaults.frostDefault,
    refraction: Float = LiquidGlassDefaults.refractionDefault,
    curve: Float = LiquidGlassDefaults.curveDefault,
    edge: Float = LiquidGlassDefaults.edgeDefault,
    saturation: Float = LiquidGlassDefaults.saturationDefault,
    dispersion: Float = LiquidGlassDefaults.dispersionDefault,
    contrast: Float = LiquidGlassDefaults.contrastDefault,
    tint: Color = LiquidGlassDefaults.tintSemiDark
): Modifier = this.then(
    Modifier.liquid(liquidState) {
        this.frost = frost
        this.shape = shape
        this.refraction = refraction
        this.curve = curve
        this.edge = edge
        this.saturation = saturation
        this.dispersion = dispersion
        this.contrast = contrast
        this.tint = tint
    }
)

/**
 * Liquid Glass variant optimized for floating bottom bars.
 * Uses heavier frost and stronger refraction for prominent floating effect.
 */
@Composable
fun Modifier.liquidGlassFloating(
    liquidState: LiquidState,
    shape: Shape = AppShapes.rounded2xl,
    tint: Color = LiquidGlassDefaults.tintSemiDark
): Modifier = this.then(
    Modifier.liquid(liquidState) {
        this.frost = LiquidGlassDefaults.frostHeavy
        this.shape = shape
        this.refraction = LiquidGlassDefaults.refractionStrong
        this.curve = LiquidGlassDefaults.curveStrong
        this.edge = LiquidGlassDefaults.edgeProminent
        this.saturation = LiquidGlassDefaults.saturationHigh
        this.dispersion = LiquidGlassDefaults.dispersionDefault
        this.contrast = LiquidGlassDefaults.contrastHigh
        this.tint = tint
    }
)

/**
 * Liquid Glass variant for buttons and small controls.
 * Uses lighter effects suitable for smaller elements.
 */
@Composable
fun Modifier.liquidGlassButton(
    liquidState: LiquidState,
    shape: Shape = AppShapes.circle,
    tint: Color = LiquidGlassDefaults.tintDark
): Modifier = this.then(
    Modifier.liquid(liquidState) {
        this.frost = LiquidGlassDefaults.frostLight
        this.shape = shape
        this.refraction = LiquidGlassDefaults.refractionSubtle
        this.curve = LiquidGlassDefaults.curveSubtle
        this.edge = LiquidGlassDefaults.edgeDefault
        this.saturation = LiquidGlassDefaults.saturationDefault
        this.dispersion = LiquidGlassDefaults.dispersionDefault
        this.contrast = LiquidGlassDefaults.contrastDefault
        this.tint = tint
    }
)

/**
 * Marks content as the source for liquid glass effects.
 * Apply this to the background content that should show through glass.
 */
@Composable
fun Modifier.liquidGlassSource(
    liquidState: LiquidState
): Modifier = this.then(
    Modifier.liquefiable(liquidState)
)

// ═══════════════════════════════════════════════════════════════════════════
// HAZE-BASED BLUR (Alternative when liquid refraction is not needed)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Haze-based blur glass modifier.
 * Use this when you need blur but not lens refraction.
 * Simpler and more performant than full liquid glass.
 */
@Composable
fun Modifier.hazeGlass(
    hazeState: HazeState,
    shape: Shape = AppShapes.rounded2xl,
    blurRadius: Dp = 20.dp,
    backgroundColor: Color = Color(0x80000000),
    noiseFactor: Float = 0.1f
): Modifier = this.then(
    Modifier
        .clip(shape)
        .hazeEffect(hazeState) {
            style = HazeStyle(
                backgroundColor = Color.Transparent,
                tints = listOf(HazeTint(color = backgroundColor)),
                blurRadius = blurRadius,
                noiseFactor = noiseFactor
            )
        }
)

/**
 * Creates a HazeState for blur-based glass effects.
 */
@Composable
fun rememberHazeGlassState(): HazeState = rememberHazeState()
