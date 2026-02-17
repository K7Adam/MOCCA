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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Data class representing a bottom navigation item.
 *
 * @property panelState The panel state this item represents
 * @property icon The icon to display
 * @property label The label text
 */
data class BottomNavItem(
    val panelState: PanelState,
    val icon: ImageVector,
    val label: String
)

/**
 * Default bottom navigation items for the 3-panel layout.
 */
val defaultBottomNavItems = listOf(
    BottomNavItem(
        panelState = PanelState.LEFT_OPEN,
        icon = Icons.Default.Computer,
        label = "SESSIONS"
    ),
    BottomNavItem(
        panelState = PanelState.CENTER,
        icon = Icons.AutoMirrored.Filled.Chat,
        label = "CHAT"
    ),
    BottomNavItem(
        panelState = PanelState.RIGHT_OPEN,
        icon = Icons.Default.Dashboard,
        label = "TOOLS"
    )
)

/**
 * Ultra-modern, animated bottom navigation bar with glassmorphic design.
 *
 * Features:
 * - Real-time synchronization with panel state
 * - Smooth color and scale animations
 * - Glassmorphic terminal aesthetic
 * - Bidirectional sync with swipe navigation
 *
 * @param currentState The current panel state
 * @param onItemClick Callback when an item is clicked
 * @param items List of navigation items (defaults to 3-panel layout)
 * @param modifier Modifier for styling
 */
@Composable
fun MoccaBottomNavigation(
    currentState: PanelState,
    onItemClick: (PanelState) -> Unit,
    items: List<BottomNavItem> = defaultBottomNavItems,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(min = 280.dp, max = 360.dp)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
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
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                val isSelected = currentState == item.panelState

                BottomNavItemComponent(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemClick(item.panelState) }
                )
            }
        }
    }
}

/**
 * Individual bottom navigation item with animations.
 *
 * @param item The navigation item data
 * @param isSelected Whether this item is currently selected
 * @param onClick Callback when clicked
 */
@Composable
private fun BottomNavItemComponent(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Animated color transition
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.accentGreen else AppColors.textTertiary,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "iconColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.accentGreen else AppColors.textTertiary,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "textColor"
    )

    // Animated scale with spring physics
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // Indicator alpha
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 200),
        label = "indicatorAlpha"
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

        Spacer(modifier = Modifier.height(2.dp))

        // Selected indicator dot
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(3.dp)
                .background(
                    color = AppColors.accentGreen.copy(alpha = indicatorAlpha),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}
