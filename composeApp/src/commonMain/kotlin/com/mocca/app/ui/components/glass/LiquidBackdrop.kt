package com.mocca.app.ui.components.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer

/**
 * Platform-agnostic backdrop interface for liquid glass effects.
 * 
 * On Android, this uses Kyant0's backdrop library for true liquid glass
 * with lens refraction, blur, and luminance adaptation.
 * 
 * Architecture inspired by SimpMusic's implementation.
 * 
 * IMPORTANT: MOCCA is Android-only (per AGENTS.md), so the commonMain
 * declarations are primarily for code organization.
 */
expect class LiquidBackdrop

/**
 * Creates a backdrop that captures content for liquid glass effects.
 * 
 * The backdrop captures the background content that will be rendered
 * through glass surfaces. Apply [liquidBackdropSource] to the content
 * that should appear through the glass.
 * 
 * @param backgroundColor The background color to draw before content
 *        (default: Black for MOCCA's Pitch Black theme)
 * 
 * Usage:
 * ```kotlin
 * val backdrop = rememberLiquidBackdrop()
 * 
 * Box {
 *     // Content that appears through glass
 *     BackgroundContent(Modifier.liquidBackdropSource(backdrop))
 *     
 *     // Glass surface showing backdrop with effects
 *     GlassBar(Modifier.drawLiquidGlass(backdrop, ...))
 * }
 * ```
 */
@Composable
expect fun rememberLiquidBackdrop(
    backgroundColor: Color = Color.Black
): LiquidBackdrop

/**
 * Marks content as the source for backdrop sampling.
 * 
 * Apply this modifier to the composable that should be captured
 * and displayed through glass surfaces.
 * 
 * @param backdrop The backdrop to capture content into
 */
expect fun Modifier.liquidBackdropSource(backdrop: LiquidBackdrop): Modifier

/**
 * Draws backdrop with liquid glass effects.
 * 
 * This is the core modifier that creates the liquid glass appearance:
 * - Captures backdrop content
 * - Applies blur for frosted effect
 * - Applies lens refraction for optical distortion
 * - Applies vibrancy for color saturation
 * - Adapts brightness/contrast based on luminance
 * 
 * @param backdrop The backdrop to render
 * @param layer GraphicsLayer for luminance sampling
 * @param luminance Current luminance value (0f-1f) for effect adaptation
 * @param shape The shape of the glass surface
 * 
 * Effect order (CRITICAL): color filter ⇒ blur ⇒ lens
 */
expect fun Modifier.drawLiquidGlass(
    backdrop: LiquidBackdrop,
    layer: GraphicsLayer,
    luminance: Float,
    shape: Shape
): Modifier

/**
 * Draws backdrop with liquid glass effects and custom surface.
 * 
 * @param backdrop The backdrop to render
 * @param layer GraphicsLayer for luminance sampling
 * @param luminance Current luminance value (0f-1f)
 * @param shape The shape of the glass surface
 * @param surfaceAlpha Alpha for the dark surface overlay (default: 0.1f)
 */
expect fun Modifier.drawLiquidGlass(
    backdrop: LiquidBackdrop,
    layer: GraphicsLayer,
    luminance: Float,
    shape: Shape,
    surfaceAlpha: Float
): Modifier
