package com.mocca.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Common implementation of an inner shadow effect.
 * Uses multiple transparent strokes to create a pseudo-blur inner shadow
 * that works across all Kotlin Multiplatform targets.
 */
fun Modifier.innerShadow(
    shape: Shape,
    color: Color = Color.Black.copy(alpha = 0.3f),
    blur: Dp = 4.dp,
    offsetY: Dp = 2.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0.0.dp
) = this.drawWithContent {
    drawContent()
    
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = when (outline) {
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Generic -> outline.path
    }
    
    val shadowAlpha = color.alpha
    val blurPx = blur.toPx()
    val steps = 4
    
    for (i in 1..steps) {
        val strokeWidth = (blurPx / steps) * i
        val stepAlpha = shadowAlpha / i
        
        drawPath(
            path = path,
            color = color.copy(alpha = stepAlpha),
            style = Stroke(width = strokeWidth)
        )
    }
}

fun Modifier.innerShadow(
    enabled: Boolean,
    shape: Shape,
    color: Color = Color.Black.copy(alpha = 0.3f),
    blur: Dp = 4.dp,
    offsetY: Dp = 2.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0.0.dp
): Modifier {
    return if (enabled) {
        innerShadow(
            shape = shape,
            color = color,
            blur = blur,
            offsetY = offsetY,
            offsetX = offsetX,
            spread = spread
        )
    } else {
        this
    }
}
