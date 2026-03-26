package com.mocca.app.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import kotlin.math.roundToInt

/**
 * Shared sliding indicator component for bottom navigation.
 * 
 * This indicator slides smoothly in real-time as the user swipes between panels,
 * providing visual feedback for the current position in the 3-panel layout.
 * 
 * The indicator is used in both Navigation and ChatInput modes to ensure
 * consistent visual behavior.
 *
 * @param dragProgress Real-time drag progress: 0.0 (TOOLS/RIGHT) -> 0.5 (CHAT/CENTER) -> 1.0 (SESSIONS/LEFT)
 * @param travelDistancePx Total pixel distance between first and last nav item centers
 * @param modifier Modifier for styling
 */
@Composable
fun SharedNavIndicator(
    dragProgress: Float,
    travelDistancePx: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(NavConstants.IndicatorHeight),
        contentAlignment = Alignment.Center
    ) {
        // Subtle track background for visibility
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(1.dp)
                .background(
                    color = AppColors.outline.copy(alpha = 0.3f),
                    shape = AppShapes.extraSmall
                )
        )
        
        // Active indicator pill that slides with drag progress
        Box(
            modifier = Modifier
                .width(NavConstants.IndicatorWidth)
                .height(NavConstants.IndicatorHeight)
                .offset {
                    // Calculate offset: 
                    // - At progress 0.0 (TOOLS/Right): indicator is at +travelDistance/2
                    // - At progress 0.5 (CHAT/Center): indicator is at 0
                    // - At progress 1.0 (SESSIONS/Left): indicator is at -travelDistance/2
                    val xOffsetPx = (travelDistancePx / 2f) * (1.0f - 2.0f * dragProgress)
                    IntOffset(xOffsetPx.roundToInt(), 0)
                }
                .background(
                    color = AppColors.primary,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}
