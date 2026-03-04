package com.mocca.app.ui.components.modern

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import com.mocca.app.ui.theme.AppColors
import kotlin.random.Random

/**
 * Static premium backdrop — battery-friendly replacement for animated ASCII shader + scanlines.
 * Zero per-frame cost: one radial gradient fill + pre-computed static noise points.
 */
@Composable
fun StaticPremiumBackdrop(modifier: Modifier = Modifier) {
    val noiseOffsets = remember {
        List(150) { Offset(Random.nextFloat() * 4000f, Random.nextFloat() * 8000f) }
    }
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF001413), AppColors.background),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.75f
            )
        )
        drawPoints(
            points = noiseOffsets,
            pointMode = PointMode.Points,
            color = AppColors.accent.copy(alpha = 0.04f),
            strokeWidth = 1f
        )
    }
}
