package com.mocca.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Material 3 Expressive design primitives for MOCCA.
 */

/**
 * Tonal ambient background for the app shell.
 */
@Composable
fun DynamicExpressiveBackground(
    modifier: Modifier = Modifier,
    color1: Color = AppColors.primary.copy(alpha = 0.08f),
    color2: Color = AppColors.tertiary.copy(alpha = 0.06f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    AppColors.surfaceContainerLowest,
                    AppColors.background,
                    AppColors.surfaceDim
                )
            )
        )

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(color1, Color.Transparent, color2),
                start = Offset(width * 0.12f, 0f),
                end = Offset(width, height)
            )
        )
    }
}

/**
 * Expressive Squircle Shape generator.
 */
fun createSquircleShape(smoothing: Float = 0.6f) = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    val radius = minOf(width, height) / 2f
    
    // Simple squircle approximation using cubic beziers
    val controlDist = radius * smoothing
    
    moveTo(width / 2f, 0f)
    cubicTo(width / 2f + controlDist, 0f, width, height / 2f - controlDist, width, height / 2f)
    cubicTo(width, height / 2f + controlDist, width / 2f + controlDist, height, width / 2f, height)
    cubicTo(width / 2f - controlDist, height, 0f, height / 2f + controlDist, 0f, height / 2f)
    cubicTo(0f, height / 2f - controlDist, width / 2f - controlDist, 0f, width / 2f, 0f)
}
