package com.mocca.app.ui.components.modern

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.LocalAppPerformance
import com.mocca.app.ui.theme.PerformanceTier

@Composable
fun SnakeDotsLoader(
    modifier: Modifier = Modifier,
    dotColor: Color = AppColors.primary,
    dotRadius: Dp = 3.dp,
    gridSpacing: Dp = 8.dp
) {
    val performance = LocalAppPerformance.current
    
    if (performance.tier == PerformanceTier.LOW) {
        PulsingDotsLoader(
            modifier = modifier,
            dotColor = dotColor
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SnakeDotsTransition")
    
    // 8 positions in the outer ring of a 3x3 grid
    val currentPos by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SnakePosition"
    )

    Canvas(modifier = modifier) {
        val radiusPx = dotRadius.toPx()
        val spacingPx = gridSpacing.toPx()
        
        // Calculate total size to center the grid
        val gridWidth = 2 * spacingPx
        val gridHeight = 2 * spacingPx
        
        val startX = (size.width - gridWidth) / 2f
        val startY = (size.height - gridHeight) / 2f

        // Path indices for the outer ring (clockwise)
        // 0: (0,0), 1: (1,0), 2: (2,0)
        // 7: (0,1),          3: (2,1)
        // 6: (0,2), 5: (1,2), 4: (2,2)
        val path = listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0),
            Pair(2, 1), Pair(2, 2), Pair(1, 2),
            Pair(0, 2), Pair(0, 1)
        )

        for (y in 0..2) {
            for (x in 0..2) {
                val cx = startX + x * spacingPx
                val cy = startY + y * spacingPx
                
                var opacity = 0.15f
                
                if (x == 1 && y == 1) {
                    // Center dot is never in the path
                    opacity = 0.15f
                } else {
                    val dotIndex = path.indexOf(Pair(x, y))
                    if (dotIndex != -1) {
                        // Calculate distance from current head position
                        val dist = (currentPos - dotIndex + 8f) % 8f
                        
                        opacity = when {
                            dist < 1f -> 1.0f - 0.3f * dist // 1.0 to 0.7
                            dist < 2f -> 0.7f - 0.3f * (dist - 1f) // 0.7 to 0.4
                            dist < 3f -> 0.4f - 0.25f * (dist - 2f) // 0.4 to 0.15
                            else -> 0.15f
                        }
                    }
                }
                
                drawCircle(
                    color = dotColor.copy(alpha = opacity),
                    radius = radiusPx,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
fun PulsingDotsLoader(
    modifier: Modifier = Modifier,
    dotColor: Color = AppColors.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulsingDotsTransition")
    
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulsingProgress"
    )

    Canvas(modifier = modifier) {
        val radiusPx = 3.dp.toPx()
        val spacingPx = 8.dp.toPx()
        
        val totalWidth = 2 * spacingPx
        val startX = (size.width - totalWidth) / 2f
        val cy = size.height / 2f

        for (i in 0..2) {
            val cx = startX + i * spacingPx
            
            val dist = (progress - i + 3f) % 3f
            val opacity = when {
                dist < 1f -> 1.0f - 0.85f * dist // 1.0 to 0.15
                else -> 0.15f
            }
            
            drawCircle(
                color = dotColor.copy(alpha = opacity),
                radius = radiusPx,
                center = Offset(cx, cy)
            )
        }
    }
}