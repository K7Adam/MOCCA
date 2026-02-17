package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.mocca.app.ui.theme.LocalExtendedColors

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
