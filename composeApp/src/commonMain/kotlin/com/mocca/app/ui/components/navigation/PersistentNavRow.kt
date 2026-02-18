package com.mocca.app.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlin.math.abs

/**
 * Persistent navigation row that maintains consistent size and position across all modes.
 * 
 * CRITICAL DESIGN PRINCIPLE:
 * This component MUST render nav items with the EXACT SAME size and position
 * regardless of whether labels are shown or hidden. This ensures:
 * - Optimal user experience (muscle memory)
 * - Consistent touch targets (48dp minimum)
 * - No visual jumping when switching modes
 *
 * @param dragProgress Real-time drag progress (0.0 = right/TOOLS, 0.5 = center/CHAT, 1.0 = left/SESSIONS)
 * @param onItemClick Callback when a navigation item is clicked
 * @param showLabels Whether to show text labels (true for nav mode, false for chat input mode)
 * @param modifier Modifier for styling
 */
@Composable
fun PersistentNavRow(
    dragProgress: Float,
    onItemClick: (PanelState) -> Unit,
    showLabels: Boolean = true,
    modifier: Modifier = Modifier
) {
    val items = defaultBottomNavItems
    var travelDistancePx by remember { mutableFloatStateOf(0f) }
    var firstItemCenterPx by remember { mutableFloatStateOf(0f) }
    var lastItemCenterPx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Navigation items row - ALWAYS same size and position
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
                                Offset(center, 0f)
                            ).x
                            if (index == items.size - 1) lastItemCenterPx = coords.localToRoot(
                                Offset(center, 0f)
                            ).x

                            if (firstItemCenterPx != 0f && lastItemCenterPx != 0f) {
                                travelDistancePx = lastItemCenterPx - firstItemCenterPx
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    PersistentNavItem(
                        item = item,
                        isSelected = isSelected,
                        proximity = proximity,
                        showLabel = showLabels,
                        onClick = { onItemClick(item.panelState) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(NavConstants.NavItemToIndicatorSpacing))

        // Shared sliding indicator
        SharedNavIndicator(
            dragProgress = dragProgress,
            travelDistancePx = travelDistancePx
        )
    }
}

/**
 * Individual navigation item with proximity-based animations.
 * 
 * CRITICAL: Size and position are FIXED regardless of showLabel parameter.
 * Labels animate in/out without affecting the item's layout position.
 *
 * @param item The navigation item data
 * @param isSelected Whether this item is currently selected
 * @param proximity How close the current drag is to this item (0.0 to 1.0)
 * @param showLabel Whether to show the text label
 * @param onClick Callback when clicked
 */
@Composable
private fun PersistentNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    proximity: Float,
    showLabel: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Animated color transition
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.accentGreen else AppColors.textTertiary,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "iconColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.accentGreen else AppColors.textTertiary,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "textColor"
    )

    // Scale based on proximity - same animation regardless of label visibility
    val targetScale = 1.0f + (proximity * 0.12f)
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            // CRITICAL: Fixed touch target size - NEVER changes
            .defaultMinSize(
                minWidth = NavConstants.TouchTargetMinWidth,
                minHeight = NavConstants.TouchTargetMinHeight
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null, // No ripple for cleaner look
                onClick = onClick
            )
            .padding(
                horizontal = NavConstants.NavItemPaddingHorizontal,
                vertical = NavConstants.NavItemPaddingVertical
            )
            .scale(scale)
    ) {
        // Icon - ALWAYS same size (22dp)
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(NavConstants.NavIconSize)
        )

        // Label or spacer - maintains consistent layout height
        // When showLabel is false, we use a fixed-height spacer to prevent layout shift
        if (showLabel) {
            Spacer(modifier = Modifier.height(NavConstants.IconToLabelSpacing))

            Text(
                text = item.label,
                style = AppTypography.labelSmall.copy(
                    fontSize = NavConstants.NavLabelFontSize.value.sp,
                    letterSpacing = NavConstants.NavLabelLetterSpacing.value.sp,
                    lineHeight = NavConstants.NavLabelLineHeight.value.sp
                ),
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            // Fixed-height spacer to maintain consistent touch target area
            // This ensures the item height doesn't change when labels are hidden
            Spacer(modifier = Modifier.height(NavConstants.IconToLabelSpacing))
            // Invisible text placeholder to maintain layout
            Text(
                text = "",
                style = AppTypography.labelSmall.copy(
                    fontSize = NavConstants.NavLabelFontSize.value.sp,
                    lineHeight = NavConstants.NavLabelLineHeight.value.sp
                ),
                maxLines = 1
            )
        }
    }
}
