package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
