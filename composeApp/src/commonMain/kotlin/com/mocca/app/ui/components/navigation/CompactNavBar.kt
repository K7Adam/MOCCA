package com.mocca.app.ui.components.navigation

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.moccaClickable
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Compact navigation bar for non-chat screens.
 * Displays 3 navigation items with animated indicator.
 *
 * @param dragProgress Real-time drag progress (0.0 = right, 0.5 = center, 1.0 = left)
 * @param onItemClick Callback when a navigation item is clicked
 * @param alpha Alpha value for crossfade animation
 * @param modifier Modifier for styling
 */
@Composable
fun CompactNavBar(
    dragProgress: Float,
    onItemClick: (PanelState) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val items = defaultBottomNavItems
    var travelDistancePx by remember { mutableFloatStateOf(0f) }
    var firstItemCenterPx by remember { mutableFloatStateOf(0f) }
    var lastItemCenterPx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .alpha(alpha)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Navigation items row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val distanceFromProgress = abs(dragProgress - item.targetProgress)
                val isSelected = distanceFromProgress < 0.25f
                val proximity = 1f - (distanceFromProgress * 2f).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { coords ->
                            val center = coords.size.width / 2f
                            if (index == 0) firstItemCenterPx = coords.localToRoot(
                                androidx.compose.ui.geometry.Offset(center, 0f)
                            ).x
                            if (index == items.size - 1) lastItemCenterPx = coords.localToRoot(
                                androidx.compose.ui.geometry.Offset(center, 0f)
                            ).x

                            if (firstItemCenterPx != 0f && lastItemCenterPx != 0f) {
                                travelDistancePx = lastItemCenterPx - firstItemCenterPx
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CompactNavItem(
                        item = item,
                        isSelected = isSelected,
                        proximity = proximity,
                        onClick = { onItemClick(item.panelState) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Animated indicator with track background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
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
            // Active indicator
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(4.dp)
                    .offset {
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
}

/**
 * Individual navigation item with proximity-based animations.
 */
@Composable
private fun CompactNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    proximity: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Animated color transition
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.primary else AppColors.outline,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "iconColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.primary else AppColors.outline,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "textColor"
    )

    // Scale based on proximity
    val targetScale = 1.0f + (proximity * 0.12f)
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .moccaClickable(
                onClick = onClick,
                interactionSource = interactionSource,
                pressedScale = 0.96f
            )
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
            .scale(scale)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = item.label,
            style = AppTypography.labelSmall.copy(
                
                lineHeight = 12.sp
            ),
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
