package com.mocca.app.ui.components.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes

/**
 * Shared sliding indicator component for bottom navigation.
 * Uses graphicsLayer for offset — skips recomposition during drag.
 */
@Composable
fun SharedNavIndicator(
    dragProgress: Float,
    travelDistancePx: Float,
    isSettled: Boolean,
    modifier: Modifier = Modifier
) {
    val rawOffsetPx = (travelDistancePx / 2f) * (1.0f - 2.0f * dragProgress)
    val animatedOffsetPx by animateFloatAsState(
        targetValue = rawOffsetPx,
        animationSpec = if (isSettled) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        } else {
            snap()
        },
        label = "indicatorOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(NavConstants.IndicatorHeight),
        contentAlignment = Alignment.Center
    ) {
        // Active indicator pill — follows drag directly, then springs on settle
        Box(
            modifier = Modifier
                .width(NavConstants.IndicatorWidth)
                .height(NavConstants.IndicatorHeight)
                .graphicsLayer { translationX = animatedOffsetPx }
                .background(
                    color = AppColors.primary,
                    shape = AppShapes.pill
                )
        )
    }
}
