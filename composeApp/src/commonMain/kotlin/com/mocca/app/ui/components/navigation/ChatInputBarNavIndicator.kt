package com.mocca.app.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppColors
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Nav indicator section with icons and sliding indicator for ChatInputBar.
 *
 * @param items Navigation items to display
 * @param dragProgress Current drag progress for indicator positioning
 * @param onItemClick Callback when a nav item is clicked
 * @param travelDistancePx Pixel distance between first and last item centers
 * @param firstItemCenterPx Center X of first item in root coordinates
 * @param lastItemCenterPx Center X of last item in root coordinates
 * @param onTravelDistanceChanged Callback when travel distance is recalculated
 * @param onFirstItemCenterChanged Callback when first item center is measured
 * @param onLastItemCenterChanged Callback when last item center is measured
 * @param navIndicatorHeight Height of the nav indicator section
 */
@Composable
internal fun ChatInputBarNavIndicator(
    items: List<BottomNavItem>,
    dragProgress: Float,
    onItemClick: (PanelState) -> Unit,
    travelDistancePx: Float,
    firstItemCenterPx: Float,
    lastItemCenterPx: Float,
    onTravelDistanceChanged: (Float) -> Unit,
    onFirstItemCenterChanged: (Float) -> Unit,
    onLastItemCenterChanged: (Float) -> Unit,
    navIndicatorHeight: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(navIndicatorHeight),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val distanceFromProgress = abs(dragProgress - item.targetProgress)
                val isSelected = distanceFromProgress < 0.25f

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 32.dp)
                        .onGloballyPositioned { coords ->
                            val center = coords.size.width / 2f
                            if (index == 0) onFirstItemCenterChanged(
                                coords.localToRoot(
                                    androidx.compose.ui.geometry.Offset(center, 0f)
                                ).x
                            )
                            if (index == items.size - 1) onLastItemCenterChanged(
                                coords.localToRoot(
                                    androidx.compose.ui.geometry.Offset(center, 0f)
                                ).x
                            )
                            if (firstItemCenterPx != 0f && lastItemCenterPx != 0f) {
                                onTravelDistanceChanged(lastItemCenterPx - firstItemCenterPx)
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemClick(item.panelState) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Nav icon
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) AppColors.accentGreen else AppColors.textTertiary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Indicator dot
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 4.dp else 3.dp)
                            .background(
                                color = if (isSelected) AppColors.accentGreen else AppColors.textTertiary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }

        // Sliding indicator line
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .offset {
                    val xOffsetPx = (travelDistancePx / 2f) * (1.0f - 2.0f * dragProgress)
                    IntOffset(xOffsetPx.roundToInt(), 0)
                }
                .background(
                    color = AppColors.accentGreen.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(1.dp)
                )
        )
    }
}
