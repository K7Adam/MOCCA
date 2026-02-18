package com.mocca.app.ui.components.glass

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppShapes

/**
 * Liquid Glass Modifier Extensions
 * 
 * Provides two types of glass effects:
 * 
 * 1. **True Liquid Glass** (Backdrop-based) - Uses Kyant0/backdrop library
 *    - Real backdrop sampling with blur and lens refraction
 *    - Luminance adaptation for dynamic effects
 *    - Requires setting up a LiquidBackdrop at parent level
 * 
 * 2. **Gradient Glass** (Fallback) - Pure Compose gradients
 *    - No backdrop sampling required
 *    - Simpler visual effect with specular highlights
 *    - Good for static elements or when backdrop is unavailable
 * 
 * Usage (True Liquid Glass):
 * ```kotlin
 * val backdrop = rememberLiquidBackdrop()
 * val layer = rememberGraphicsLayer()
 * val luminance = rememberLuminanceAnimation(layer)
 * 
 * Box {
 *     Content(Modifier.liquidBackdropSource(backdrop))
 *     GlassBar(Modifier.drawLiquidGlass(backdrop, layer, luminance.value, shape))
 * }
 * ```
 * 
 * Usage (Gradient Glass):
 * ```kotlin
 * Box(Modifier.glassy(shape = AppShapes.card)) {
 *     // Content
 * }
 * ```
 */

// ═══════════════════════════════════════════════════════════════════════════════
// TRUE LIQUID GLASS MODIFIERS (Backdrop-based)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Draws true liquid glass with backdrop sampling and luminance adaptation.
 * 
 * This is the recommended modifier for bottom bars, FABs, and floating elements.
 * Requires a LiquidBackdrop to be set up at the parent level.
 * 
 * @param backdrop The backdrop created with rememberLiquidBackdrop()
 * @param layer GraphicsLayer for luminance sampling
 * @param luminance Current luminance value (0f-1f)
 * @param shape Shape of the glass surface
 * @param surfaceAlpha Alpha for the dark surface overlay
 */
@Composable
fun Modifier.liquidGlassSurface(
    backdrop: LiquidBackdrop,
    layer: GraphicsLayer,
    luminance: Float,
    shape: Shape = AppShapes.rounded2xl,
    surfaceAlpha: Float = 0.1f
): Modifier = this
    .clip(shape)
    .drawLiquidGlass(backdrop, layer, luminance, shape, surfaceAlpha)

/**
 * Draws liquid glass optimized for bottom navigation bars.
 */
@Composable
fun Modifier.liquidGlassNavBar(
    backdrop: LiquidBackdrop,
    layer: GraphicsLayer,
    luminance: Float
): Modifier = liquidGlassSurface(
    backdrop = backdrop,
    layer = layer,
    luminance = luminance,
    shape = AppShapes.rounded2xl,
    surfaceAlpha = 0.1f
)

/**
 * Draws liquid glass optimized for FABs and small controls.
 */
@Composable
fun Modifier.liquidGlassFab(
    backdrop: LiquidBackdrop,
    layer: GraphicsLayer,
    luminance: Float
): Modifier = liquidGlassSurface(
    backdrop = backdrop,
    layer = layer,
    luminance = luminance,
    shape = AppShapes.circle,
    surfaceAlpha = 0.15f
)

// ═══════════════════════════════════════════════════════════════════════════════
// GRADIENT GLASS MODIFIERS (Fallback, no backdrop required)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Simple glassy modifier with gradient overlays.
 * Use when backdrop sampling is not available or needed.
 * 
 * @param shape Shape of the glass
 * @param borderWidth Width of the border (default: 0.5dp)
 * @param backgroundColor Background color (default: 50% black)
 * @param borderColor Border color (default: 25% white)
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
        .drawBehind {
            drawRect(backgroundColor)
        }
        .border(borderWidth, borderColor.copy(alpha = 0.3f), shape)
)

/**
 * Floating glass modifier for prominent elements like bottom bars.
 * Uses rounded corners and enhanced specular highlights.
 */
@Composable
fun Modifier.glassFloating(
    shape: Shape = AppShapes.rounded2xl,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    reducedTransparency: Boolean = false
): Modifier = glassy(
    shape = shape,
    borderWidth = 0.75.dp,
    backgroundColor = Color(0x40000000), // 25% dark
    borderColor = Color(0x40FFFFFF)
)

/**
 * Button glass modifier for small controls.
 * Uses pill shape and subtle effects.
 */
@Composable
fun Modifier.glassButton(
    shape: Shape = AppShapes.pill,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    reducedTransparency: Boolean = false
): Modifier = glassy(
    shape = shape,
    borderWidth = 1.dp,
    backgroundColor = Color(0x40000000),
    borderColor = Color(0x50FFFFFF)
)

/**
 * Premium glass modifier with specular highlights and depth effects.
 * Creates a more sophisticated glass appearance without backdrop sampling.
 */
@Composable
fun Modifier.glassPremium(
    backgroundColor: Color,
    borderColor: Color,
    highlightColor: Color,
    shadowColor: Color,
    borderWidth: Dp = 1.dp
): Modifier = this.then(
    Modifier
        .drawBehind {
            // Base glass background with subtle gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = backgroundColor.alpha * 1.05f),
                        backgroundColor,
                        backgroundColor.copy(alpha = backgroundColor.alpha * 0.95f)
                    )
                )
            )
            
            // Top edge specular highlight
            val highlightHeight = 2.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = highlightColor.alpha * 1.5f),
                        highlightColor,
                        Color.Transparent
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, highlightHeight * 4)
            )
            
            // Inner glow from top
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 0.3f)
            )
            
            // Bottom inner shadow for depth
            val shadowHeight = size.height * 0.15f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        shadowColor.copy(alpha = shadowColor.alpha * 0.5f)
                    )
                ),
                topLeft = Offset(0f, size.height - shadowHeight),
                size = Size(size.width, shadowHeight)
            )
        }
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = borderColor.alpha * 1.2f),
                    borderColor.copy(alpha = borderColor.alpha * 0.8f),
                    borderColor.copy(alpha = borderColor.alpha * 0.3f)
                )
            ),
            shape = RoundedCornerShape(0.dp)
        )
)

/**
 * Fallback glass modifier for accessibility mode or older devices.
 */
@Composable
fun Modifier.glassFallback(
    backgroundColor: Color,
    borderColor: Color,
    highlightColor: Color,
    shadowColor: Color
): Modifier = this.then(
    Modifier
        .drawBehind {
            // Simple translucent background
            drawRect(backgroundColor)
            
            // Top edge highlight
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, 4.dp.toPx())
            )
        }
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.6f),
                    borderColor.copy(alpha = 0.2f)
                )
            ),
            shape = RoundedCornerShape(0.dp)
        )
)

// ═══════════════════════════════════════════════════════════════════════════════
// DEPRECATED LEGACY MODIFIERS (kept for backward compatibility)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * iOS 26-style Pure Liquid Glass Card modifier.
 * Creates a pure glass effect with transparent background and edge highlights.
 * 
 * @deprecated Use liquidGlassSurface() with backdrop for true liquid glass,
 *             or glassy() for simpler gradient-based effects.
 */
@Composable
@Deprecated(
    message = "Use liquidGlassSurface() with backdrop for true liquid glass, or glassy() for simpler effects",
    replaceWith = ReplaceWith("liquidGlassSurface(backdrop, layer, luminance, shape)")
)
fun Modifier.liquidGlassCard(
    shape: Shape = AppShapes.card,
    tint: Color = Color(0x40000000),
    highlightIntensity: Float = 0.2f
): Modifier = this.then(
    Modifier
        .clip(shape)
        .drawBehind {
            drawRect(tint)
            
            val highlightHeight = 1.5.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = highlightIntensity),
                        Color.White.copy(alpha = 0f)
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, highlightHeight * 3)
            )
            
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 0.3f)
            )
        }
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.03f)
                )
            ),
            shape = shape
        )
)

/**
 * iOS 26-style Pure Liquid Glass Header modifier.
 * 
 * @deprecated Use liquidGlassSurface() with backdrop for true liquid glass.
 */
@Composable
@Deprecated(
    message = "Use liquidGlassSurface() with backdrop for true liquid glass",
    replaceWith = ReplaceWith("liquidGlassSurface(backdrop, layer, luminance, shape)")
)
fun Modifier.liquidGlassHeader(
    shape: Shape = AppShapes.medium
): Modifier = this.then(
    Modifier
        .clip(shape)
        .drawBehind {
            drawRect(Color(0x40000000))
            
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, 4.dp.toPx())
            )
            
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 0.5f)
            )
        }
        .border(
            width = 0.5.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.White.copy(alpha = 0.08f)
                )
            ),
            shape = shape
        )
)

/**
 * iOS 26-style Pure Liquid Glass Button modifier.
 * 
 * @deprecated Use liquidGlassFab() for FABs with backdrop.
 */
@Composable
@Deprecated(
    message = "Use liquidGlassFab() for FABs with backdrop",
    replaceWith = ReplaceWith("liquidGlassFab(backdrop, layer, luminance)")
)
fun Modifier.liquidGlassButton(
    shape: Shape = AppShapes.circle
): Modifier = this.then(
    Modifier
        .clip(shape)
        .drawBehind {
            drawRect(Color(0x40000000))
            
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, 3.dp.toPx())
            )
            
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension / 2f
                )
            )
        }
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.5f),
                    Color.White.copy(alpha = 0.15f)
                )
            ),
            shape = shape
        )
)
