package com.mocca.app.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Bleeding-edge Material 3 Expressive Design System Components for MOCCA.
 * Designed by Pickle Rick. Jerry-free zone.
 */

@Composable
fun ExpressiveBentoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = AppShapes.rounded2xl,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            content = content
        )
    }
}

/**
 * GPU-efficient Dynamic Mesh Background for premium M3 Expressive aesthetics.
 */
@Composable
fun DynamicExpressiveBackground(
    modifier: Modifier = Modifier,
    color1: Color = AppColors.primary.copy(alpha = 0.15f),
    color2: Color = AppColors.AnchorTertiary.copy(alpha = 0.1f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(AppColors.background, AppColors.surfaceDim)
            )
        )

        // Draw animated blobs
        val x1 = width * (0.5f + 0.3f * sin(time * 2 * Math.PI.toFloat()))
        val y1 = height * (0.3f + 0.2f * sin(time * 2 * Math.PI.toFloat() + 1f))
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color1, Color.Transparent),
                center = Offset(x1, y1),
                radius = width * 0.8f
            ),
            center = Offset(x1, y1),
            radius = width * 0.8f
        )

        val x2 = width * (0.3f + 0.4f * sin(time * 2 * Math.PI.toFloat() + 3f))
        val y2 = height * (0.7f + 0.2f * sin(time * 2 * Math.PI.toFloat() + 2f))
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color2, Color.Transparent),
                center = Offset(x2, y2),
                radius = width * 0.7f
            ),
            center = Offset(x2, y2),
            radius = width * 0.7f
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
