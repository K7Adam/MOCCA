package com.mocca.app.ui.components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.ui.graphics.vector.ImageVector
import com.mocca.app.ui.navigation.PanelState

/**
 * Data class representing a single item in the bottom navigation bar.
 *
 * @property panelState The semantic panel state this item navigates to.
 * @property icon The icon to display.
 * @property label The label text for the item.
 * @property targetProgress The progress value (0.0–1.0) used by [PersistentNavRow]
 *   to position the sliding indicator. Maps to page position via [PanelNavigation]:
 *   - 1.0f = page 0 / Sessions (LEFT_OPEN)
 *   - 0.5f = page 1 / Chat     (CENTER)
 *   - 0.0f = page 2 / Tools    (RIGHT_OPEN)
 */
data class BottomNavItem(
    val panelState: PanelState,
    val icon: ImageVector,
    val label: String,
    val targetProgress: Float
)

/**
 * Default set of bottom navigation items for the 3-panel layout.
 * Order: left-to-right on screen (Sessions, Chat, Tools).
 */
val defaultBottomNavItems: List<BottomNavItem> = listOf(
    BottomNavItem(
        panelState = PanelState.LEFT_OPEN,
        icon = Icons.Default.Computer,
        label = "Sessions",
        targetProgress = 1.0f
    ),
    BottomNavItem(
        panelState = PanelState.CENTER,
        icon = Icons.AutoMirrored.Filled.Chat,
        label = "Chat",
        targetProgress = 0.5f
    ),
    BottomNavItem(
        panelState = PanelState.RIGHT_OPEN,
        icon = Icons.Default.Dashboard,
        label = "Tools",
        targetProgress = 0.0f
    )
)
