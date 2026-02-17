package com.mocca.app.ui.components.modern

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.NetworkWifi1Bar
import androidx.compose.material.icons.filled.NetworkWifi2Bar
import androidx.compose.material.icons.filled.NetworkWifi3Bar
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.ConnectionQuality
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Modern Glassmorphic Top Bar and Divider components.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// MODERN TOP BAR
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Modern top bar with optional navigation icon and actions.
 */
@Composable
fun ModernTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.background, AppShapes.none)
    ) {
        // Content row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.topBarHeight)
                .padding(horizontal = AppSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                if (navigationIcon != null && onNavigationClick != null) {
                    MoccaIconButton(
                        icon = navigationIcon,
                        onClick = onNavigationClick,
                        iconColor = AppColors.white
                    )
                }
                
                Text(
                    text = title.uppercase(),
                    color = AppColors.white,
                    style = AppTypography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
        
        // Bottom divider
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                thickness = AppSpacing.borderThin,
                color = AppColors.border
            )
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
                color = AppColors.grey,
                style = AppTypography.labelSmall
            )
            Text(
                text = centerText.uppercase(),
                color = AppColors.white,
                style = AppTypography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Right - Connection status
        rightContent()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TERMINAL DIVIDERS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Standard horizontal divider.
 */
@Composable
fun TerminalDivider(
    modifier: Modifier = Modifier,
    color: Color = AppColors.border,
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
    color: Color = AppColors.white,
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
    textColor: Color = AppColors.grey,
    lineColor: Color = AppColors.border
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

// ═══════════════════════════════════════════════════════════════════════════════
// PANEL HEADER (for swipe panels)
// ═══════════════════════════════════════════════════════════════════════════════

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
                color = AppColors.white,
                style = AppTypography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    color = AppColors.grey,
                    style = AppTypography.labelSmall,
                    modifier = Modifier.padding(top = AppSpacing.xl + AppSpacing.xs)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONNECTION QUALITY INDICATOR
// ═══════════════════════════════════════════════════════════════════════════════

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
    val (icon, color, description) = when (quality) {
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
            AppColors.grey,
            "UNKNOWN"
        )
    }
    
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
