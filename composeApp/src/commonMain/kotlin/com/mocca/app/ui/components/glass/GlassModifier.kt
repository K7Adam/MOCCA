package com.mocca.app.ui.components.glass

import android.os.Build
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppShapes

/**
 * Modifier extension for applying liquid glass effect to existing components.
 * 
 * This allows applying glass effects to any composable without wrapping in GlassSurface.
 * 
 * Usage:
 * ```kotlin
 * Box(
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .height(56.dp)
 *         .glass(shape = RoundedCornerShape(16.dp))
 * ) {
 *     // Content
 * }
 * ```
 * 
 * @param shape Shape of the glass (rounded corners recommended)
 * @param tokens Theme-aware glass tokens
 * @param params Shader parameters for customization
 * @param reducedTransparency Accessibility mode - use solid background
 */
@Composable
fun Modifier.glass(
    shape: Shape = AppShapes.large,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    params: GlassShaderParams = GlassShaderParams.fromTokens(tokens),
    reducedTransparency: Boolean = false
): Modifier {
    return when {
        reducedTransparency -> {
            this
                .clip(shape)
                .glassFallback(
                    backgroundColor = tokens.fallbackBackground,
                    borderColor = tokens.strokeColor,
                    highlightColor = tokens.highlightTop,
                    shadowColor = tokens.shadowColor
                )
        }
        
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            this
                .clip(shape)
                .glassPremium(
                    backgroundColor = params.tintColor,
                    borderColor = params.strokeColor,
                    highlightColor = params.highlightTopColor,
                    shadowColor = params.shadowColor,
                    borderWidth = GlassTokens.strokeWidth
                )
        }
        
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            this
                .clip(shape)
                .glassPremium(
                    backgroundColor = tokens.tintColor,
                    borderColor = tokens.strokeColor,
                    highlightColor = tokens.highlightTop,
                    shadowColor = tokens.shadowColor,
                    borderWidth = GlassTokens.strokeWidth
                )
        }
        
        else -> {
            this
                .clip(shape)
                .glassFallback(
                    backgroundColor = tokens.tintColor,
                    borderColor = tokens.strokeColor,
                    highlightColor = tokens.highlightTop,
                    shadowColor = tokens.shadowColor
                )
        }
    }
}

/**
 * Modifier extension for floating glass effect.
 * Uses heavier blur and stronger effects for prominent floating appearance.
 */
@Composable
fun Modifier.glassFloating(
    shape: Shape = AppShapes.rounded2xl,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    reducedTransparency: Boolean = false
): Modifier = glass(
    shape = shape,
    tokens = tokens,
    params = GlassShaderParams.Floating,
    reducedTransparency = reducedTransparency
)

/**
 * Modifier extension for button glass effect.
 * Uses lighter effects suitable for touch targets.
 */
@Composable
fun Modifier.glassButton(
    shape: Shape = AppShapes.pill,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    reducedTransparency: Boolean = false
): Modifier = glass(
    shape = shape,
    tokens = tokens,
    params = GlassShaderParams.Button,
    reducedTransparency = reducedTransparency
)

/**
 * Modifier extension for app bar glass effect.
 * Optimized for full-width header bars.
 */
@Composable
fun Modifier.glassAppBar(
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    reducedTransparency: Boolean = false
): Modifier = glass(
    shape = RoundedCornerShape(0.dp),
    tokens = tokens,
    params = GlassShaderParams.AppBar,
    reducedTransparency = reducedTransparency
)

/**
 * Modifier extension for sheet glass effect.
 * Optimized for bottom sheets.
 */
@Composable
fun Modifier.glassSheet(
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    reducedTransparency: Boolean = false
): Modifier = glass(
    shape = AppShapes.bottomSheet,
    tokens = tokens,
    params = GlassShaderParams.Sheet,
    reducedTransparency = reducedTransparency
)

/**
 * Premium glass modifier with full visual effects.
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
            // Base glass background
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
            
            // Inner glow
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
            
            // Bottom inner shadow
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
 * Fallback glass modifier without advanced effects.
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

/**
 * Simple glassy modifier without blur or refraction.
 * Use only as fallback when full glass effect is not available.
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
 * iOS 26-style Pure Liquid Glass Card modifier.
 * Creates a pure glass effect with transparent background and edge highlights.
 * 
 * @param shape The shape of the glass card
 * @param tint Optional tint color to overlay on the glass background
 * @param highlightIntensity Intensity of the top edge highlight (0.0 to 1.0)
 */
@Composable
fun Modifier.liquidGlassCard(
    shape: Shape = AppShapes.card,
    tint: Color = Color(0x99000000), // Default: 60% dark, transparent
    highlightIntensity: Float = 0.18f
): Modifier = this.then(
    Modifier
        .clip(shape)
        .drawBehind {
            // Pure liquid-glass: semi-transparent background with optional tint
            drawRect(tint)
            
            // Top edge specular highlight
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
            
            // Subtle inner glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f),
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
 * Optimized for header bars with prominent edge highlights.
 */
@Composable
fun Modifier.liquidGlassHeader(
    shape: Shape = AppShapes.medium
): Modifier = this.then(
    Modifier
        .clip(shape)
        .drawBehind {
            // Semi-transparent dark background
            drawRect(Color(0xAA000000))
            
            // Strong top highlight
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, 4.dp.toPx())
            )
            
            // Inner glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.03f),
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
 * For small controls like scroll-to-bottom, FABs, etc.
 */
@Composable
fun Modifier.liquidGlassButton(
    shape: Shape = AppShapes.circle
): Modifier = this.then(
    Modifier
        .clip(shape)
        .drawBehind {
            // Semi-transparent dark background
            drawRect(Color(0x88000000))
            
            // Strong edge highlight
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
            
            // Inner glow
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
