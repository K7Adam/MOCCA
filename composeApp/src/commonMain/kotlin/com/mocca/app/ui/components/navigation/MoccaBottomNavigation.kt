package com.mocca.app.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlin.math.roundToInt

/**
 * Data class representing a bottom navigation item.
 *
 * @property panelState The panel state this item represents
 * @property icon The icon to display
 * @property label The label text
 * @property targetProgress The progress value (0.0-1.0) this item corresponds to
 */
data class BottomNavItem(
    val panelState: PanelState,
    val icon: ImageVector,
    val label: String,
    val targetProgress: Float
)

/**
 * Default bottom navigation items for the 3-panel layout.
 *
 * Order: Left to right on screen (SESSIONS, CHAT, TOOLS)
 * Progress mapping: 0.0 (TOOLS/RIGHT) -> 0.5 (CHAT/CENTER) -> 1.0 (SESSIONS/LEFT)
 */
val defaultBottomNavItems = listOf(
    BottomNavItem(
        panelState = PanelState.LEFT_OPEN,
        icon = Icons.Default.Computer,
        label = "SESSIONS",
        targetProgress = 1.0f  // LEFT_OPEN is at progress 1.0
    ),
    BottomNavItem(
        panelState = PanelState.CENTER,
        icon = Icons.AutoMirrored.Filled.Chat,
        label = "CHAT",
        targetProgress = 0.5f  // CENTER is at progress 0.5
    ),
    BottomNavItem(
        panelState = PanelState.RIGHT_OPEN,
        icon = Icons.Default.Dashboard,
        label = "TOOLS",
        targetProgress = 0.0f  // RIGHT_OPEN is at progress 0.0
    )
)

/**
 * Ultra-modern, animated bottom navigation bar with glassmorphic design.
 *
 * Features:
 * - Real-time synchronization with panel state via dragProgress
 * - Animated indicator that follows swipe gesture in real-time
 * - Glassmorphic terminal aesthetic
 * - Bidirectional sync with swipe navigation
 * - Smooth color and scale animations
 *
 * @param currentState The current panel state
 * @param dragProgress Real-time drag progress: 0.0 (right) -> 0.5 (center) -> 1.0 (left)
 * @param onItemClick Callback when an item is clicked
 * @param items List of navigation items (defaults to 3-panel layout)
 * @param modifier Modifier for styling
 */
@Composable
fun MoccaBottomNavigation(
    currentState: PanelState,
    dragProgress: Float = when (currentState) {
        PanelState.RIGHT_OPEN -> 0.0f
        PanelState.CENTER -> 0.5f
        PanelState.LEFT_OPEN -> 1.0f
    },
    onItemClick: (PanelState) -> Unit,
    items: List<BottomNavItem> = defaultBottomNavItems,
    modifier: Modifier = Modifier
) {
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .widthIn(min = 280.dp, max = 360.dp)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
            .onGloballyPositioned { coordinates ->
                containerWidthPx = coordinates.size.width.toFloat()
            }
            .background(
                color = AppColors.glassBackground,
                shape = AppShapes.rounded2xl
            )
            .border(
                width = AppSpacing.borderThin,
                color = AppColors.glassBorder,
                shape = AppShapes.rounded2xl
            )
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Navigation items row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    // Calculate distance from current progress to this item's target
                    val distanceFromProgress = kotlin.math.abs(dragProgress - item.targetProgress)
                    // Item is selected if we're closest to it (within 0.25 threshold)
                    val isSelected = distanceFromProgress < 0.25f
                    // Proximity for scale animation (1.0 = exactly at this item)
                    val proximity = 1f - (distanceFromProgress * 2f).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomNavItemComponent(
                            item = item,
                            isSelected = isSelected,
                            proximity = proximity,
                            onClick = { onItemClick(item.panelState) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Animated indicator pill that follows drag progress in real-time
            AnimatedIndicatorPill(
                progress = dragProgress,
                containerWidthPx = containerWidthPx,
                modifier = Modifier.padding(horizontal = AppSpacing.md)
            )
        }
    }
}

/**
 * Animated indicator pill that moves smoothly between items based on drag progress.
 *
 * @param progress Current drag progress (0.0 to 1.0)
 * @param containerWidthPx Width of the navigation container for accurate offset calculation
 * @param modifier Modifier for styling
 */
@Composable
private fun AnimatedIndicatorPill(
    progress: Float,
    containerWidthPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp),
        contentAlignment = Alignment.Center
    ) {
        // The indicator pill - moves from right (0.0) to left (1.0)
        // Using lambda offset for performance (60fps updates)
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(3.dp)
                .offset {
                    // Calculate the max travel distance (from center to edges)
                    // The items are spaced evenly. For 3 items, the centers are at:
                    // 1/6, 1/2, 5/6 of the width.
                    // Distance from center (1/2) to edges is 1/3 of the width.
                    val maxOffsetPx = containerWidthPx / 3f
                    
                    // Map progress 0.0 (right) -> 1.0 (left) to offset +maxOffset -> -maxOffset
                    val xOffsetPx = maxOffsetPx * (1.0f - 2.0f * progress)
                    
                    IntOffset(xOffsetPx.roundToInt(), 0)
                }
                .background(
                    color = AppColors.accentGreen,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

/**
 * Individual bottom navigation item with proximity-based animations.
 *
 * @param item The navigation item data
 * @param isSelected Whether this item is currently selected
 * @param proximity How close the current drag is to this item (0.0 to 1.0)
 * @param onClick Callback when clicked
 */
@Composable
private fun BottomNavItemComponent(
    item: BottomNavItem,
    isSelected: Boolean,
    proximity: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Animated color transition based on selection
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

    // Scale based on proximity to current drag position
    // Items closer to the drag position appear slightly larger
    val targetScale = 1.0f + (proximity * 0.15f)
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
            .clickable(
                interactionSource = interactionSource,
                indication = null, // No ripple for cleaner look
                onClick = onClick
            )
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
            .scale(scale)
    ) {
        // Icon
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Label
        Text(
            text = item.label,
            style = AppTypography.labelSmall,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
