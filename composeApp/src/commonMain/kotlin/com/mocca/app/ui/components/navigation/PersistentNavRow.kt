package com.mocca.app.ui.components.navigation

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
 * @param isAgentRunning Whether an agent is currently running (shows indicator on CHAT tab)
 * @param modifier Modifier for styling
 */
@Composable
fun PersistentNavRow(
    dragProgress: Float,
    onItemClick: (PanelState) -> Unit,
    showLabels: Boolean = true,
    isAgentRunning: Boolean = false,
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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Allow the row to take all available space above the indicator
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val distanceFromProgress = abs(dragProgress - item.targetProgress)
                val isSelected = distanceFromProgress < 0.25f
                val proximity = 1f - (distanceFromProgress * 2f).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .weight(1f) // 1/3 width
                        .fillMaxHeight() // Fill available height in the row
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // No ripple for cleaner look
                            onClick = { onItemClick(item.panelState) }
                        )
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
                        isAgentRunning = isAgentRunning && item.targetProgress == 0.5f // Show indicator only on CHAT tab
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
 * @param isAgentRunning Whether to show the agent running indicator (pulsing dot)
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PersistentNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    proximity: Float,
    showLabel: Boolean,
    isAgentRunning: Boolean = false
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    // Animated color transition
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.accentGreen else AppColors.outline,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "iconColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.accentGreen else AppColors.outline,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "textColor"
    )

    // Scale based on proximity - same animation regardless of label visibility
    val targetScale = 1.0f + (proximity * 0.12f)
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "scale"
    )

    val itemModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "nav_item_${item.panelState}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = itemModifier
            // CRITICAL: Fixed touch target size - NEVER changes
            .defaultMinSize(
                minWidth = NavConstants.TouchTargetMinWidth,
                minHeight = NavConstants.TouchTargetMinHeight
            )
            .padding(
                horizontal = NavConstants.NavItemPaddingHorizontal,
                vertical = NavConstants.NavItemPaddingVertical
            )
            .scale(scale)
    ) {
        // Icon with optional running indicator
        Box {
            // Icon - ALWAYS same size (22dp)
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(NavConstants.NavIconSize)
            )
            
            // Agent running indicator - pulsing dot on top-right of icon
            if (isAgentRunning) {
                val infiniteTransition = rememberInfiniteTransition(label = "agentPulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(6.dp)
                        .scale(pulseScale)
                        .background(
                            color = AppColors.accentGreen.copy(alpha = pulseAlpha),
                            shape = com.mocca.app.ui.theme.AppShapes.circle
                        )
                )
            }
        }

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
