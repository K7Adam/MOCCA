package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.LocalExtendedColors

/**
 * A reusable modifier that applies a glassmorphic effect.
 * Works best on top of content or backgrounds with varied colors/patterns.
 * On OLED black, it provides a subtle elevation and "frosted" feel.
 */
@Composable
fun Modifier.glassy(
    shape: Shape,
    blurRadius: Dp = 12.dp,
    borderWidth: Dp = 0.5.dp,
    backgroundColor: Color = LocalExtendedColors.current.glassBackground,
    borderColor: Color = LocalExtendedColors.current.glassBorder
): Modifier = this.then(
    Modifier
        .clip(shape)
        .blur(blurRadius)
        .background(backgroundColor)
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.1f)
                )
            ),
            shape = shape
        )
)

/**
 * Simplified version of glassy modifier without the blur effect for 
 * performance-sensitive areas or where blur isn't desired.
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
            color = borderColor,
            shape = shape
        )
)
