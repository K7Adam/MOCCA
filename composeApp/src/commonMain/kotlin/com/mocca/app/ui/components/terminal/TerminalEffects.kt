package com.mocca.app.ui.components.terminal

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors

/**
 * High-end terminal visual effects.
 */

@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.02f),
    lineSpacing: Float = 8f,
    scanSpeed: Int = 4000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(scanSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val height = size.height
        val width = size.width
        
        // Static scanlines
        var y = 0f
        while (y < height) {
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += lineSpacing
        }
        
        // Moving highlight scanline
        val scanY = height * offset
        drawLine(
            color = AppColors.accentGreen.copy(alpha = 0.05f),
            start = Offset(0f, scanY),
            end = Offset(width, scanY),
            strokeWidth = 2f
        )
    }
}

@Composable
fun CRTNoiseOverlay(modifier: Modifier = Modifier) {
    // Optional: Add very subtle noise/grain effect
}
