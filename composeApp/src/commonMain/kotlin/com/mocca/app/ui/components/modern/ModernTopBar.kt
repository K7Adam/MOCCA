package com.mocca.app.ui.components.modern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.NetworkWifi1Bar
import androidx.compose.material.icons.filled.NetworkWifi2Bar
import androidx.compose.material.icons.filled.NetworkWifi3Bar
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.ConnectionQuality
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope

/**
 * Modern Top Bar and Divider components.
 */

// MODERN TOP BAR


/**
 * Modern Top Bar reconstructed as an Expressive Morphing Floating Toolbar.
 */

@Composable
fun ModernTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    navigationContentDescription: String? = null,
    showDivider: Boolean = true,
    sessionId: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    var expanded by remember { mutableStateOf(true) }
    
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.sm)
            .background(Color.Transparent),
        contentAlignment = Alignment.TopCenter
    ) {
        val toolbarModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && sessionId != null) {
            with(sharedTransitionScope) {
                Modifier
                    .padding(horizontal = AppSpacing.md)
                    .sharedBounds(
                        rememberSharedContentState(key = "session_card_$sessionId"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            }
        } else {
            Modifier.padding(horizontal = AppSpacing.md)
        }

        HorizontalFloatingToolbar(
            expanded = expanded,
            modifier = toolbarModifier,
            colors = androidx.compose.material3.FloatingToolbarDefaults.standardFloatingToolbarColors(),
            shape = AppShapes.pill,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                modifier = Modifier.padding(horizontal = AppSpacing.md)
            ) {
                if (navigationIcon != null && onNavigationClick != null) {
                    MoccaIconButton(
                        icon = navigationIcon,
                        onClick = onNavigationClick,
                        contentDescription = navigationContentDescription,
                        iconColor = AppColors.onSurface
                    )
                }

                Text(
                    text = title.uppercase(),
                    color = AppColors.onSurface,
                    style = AppTypography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

/**
 * Simple status line for top of screen (like onboarding).
 * Shows system info on left and connection status on right.
 */
@Composable
fun TerminalStatusLine(
    leftText: String,
    centerText: String,
    rightContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppSpacing.lg,
                vertical = AppSpacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left - System info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Text(
                text = leftText.uppercase(),
                color = AppColors.onSurfaceVariant,
                style = AppTypography.labelSmall
            )
            Text(
                text = centerText.uppercase(),
                color = AppColors.onSurface,
                style = AppTypography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Right - Connection status
        rightContent()
    }
}

// TERMINAL DIVIDERS


/**
 * Standard horizontal divider.
 */
@Composable
fun TerminalDivider(
    modifier: Modifier = Modifier,
    color: Color = AppColors.outline,
    thickness: Dp = AppSpacing.borderThin
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color
    )
}

/**
 * Prominent horizontal divider (white, thicker).
 */
@Composable
fun TerminalDividerProminent(
    modifier: Modifier = Modifier,
    color: Color = AppColors.onSurface,
    thickness: Dp = AppSpacing.borderStandard
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color
    )
}

/**
 * Section divider with extending line (from Settings mockup).
 * Shows header text on left with line extending to right edge.
 */
@Composable
fun TerminalSectionDivider(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = AppColors.onSurfaceVariant,
    lineColor: Color = AppColors.outline
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Text(
            text = "> ${text.uppercase()}",
            color = textColor,
            style = AppTypography.labelMedium
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = AppSpacing.borderThin,
            color = lineColor
        )
    }
}

// PANEL HEADER (for swipe panels)


/**
 * Header for left/right swipe panels.
 */
@Composable
fun PanelHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        if (icon != null) {
            icon()
        }
        
        Box {
            Text(
                text = title.uppercase(),
                color = AppColors.onSurface,
                style = AppTypography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    color = AppColors.onSurfaceVariant,
                    style = AppTypography.labelSmall,
                    modifier = Modifier.padding(top = AppSpacing.xl + AppSpacing.xs)
                )
            }
        }
    }
}

// CONNECTION QUALITY INDICATOR


/**
 * Connection quality indicator showing network status.
 * 
 * @param quality The current connection quality
 * @param modifier Modifier for styling
 */
@Composable
fun ConnectionQualityIndicator(
    quality: ConnectionQuality,
    modifier: Modifier = Modifier
) {
    val triple: Triple<ImageVector, Color, String> = when (quality) {
        ConnectionQuality.EXCELLENT -> Triple(
            Icons.Filled.NetworkWifi,
            AppColors.success,
            "EXCELLENT"
        )
        ConnectionQuality.GOOD -> Triple(
            Icons.Filled.NetworkWifi3Bar,
            AppColors.success,
            "GOOD"
        )
        ConnectionQuality.DEGRADED -> Triple(
            Icons.Filled.NetworkWifi2Bar,
            AppColors.warning,
            "DEGRADED"
        )
        ConnectionQuality.POOR -> Triple(
            Icons.Filled.NetworkWifi2Bar,
            AppColors.warning,
            "POOR"
        )
        ConnectionQuality.OFFLINE -> Triple(
            Icons.Filled.WifiOff,
            AppColors.error,
            "OFFLINE"
        )
        ConnectionQuality.UNKNOWN -> Triple(
            Icons.Filled.NetworkWifi1Bar,
            AppColors.onSurfaceVariant,
            "UNKNOWN"
        )
    }
    val (icon, color, description) = triple
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Connection: $description",
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = description,
            color = color,
            style = AppTypography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
