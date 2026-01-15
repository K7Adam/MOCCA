package com.mocca.app.ui.components.terminal

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing

/**
 * Terminal-styled top bar and divider components.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// TERMINAL TOP BAR
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Terminal-styled top bar with optional navigation icon and actions.
 */
@Composable
fun TerminalTopBar(
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
            .background(TerminalColors.background, RectangleShape)
    ) {
        // Content row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TerminalSpacing.topBarHeight)
                .padding(horizontal = TerminalSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
            ) {
                if (navigationIcon != null && onNavigationClick != null) {
                    TerminalIconButton(
                        icon = navigationIcon,
                        onClick = onNavigationClick,
                        iconColor = TerminalColors.white
                    )
                }
                
                Text(
                    text = "// ${title.uppercase()}",
                    color = TerminalColors.white,
                    style = MaterialTheme.typography.headlineMedium,
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
                thickness = TerminalSpacing.borderStandard,
                color = TerminalColors.white
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
                horizontal = TerminalSpacing.lg,
                vertical = TerminalSpacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left - System info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
        ) {
            Text(
                text = leftText.uppercase(),
                color = TerminalColors.grey,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = centerText.uppercase(),
                color = TerminalColors.white,
                style = MaterialTheme.typography.labelLarge,
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
    color: Color = TerminalColors.border,
    thickness: Dp = TerminalSpacing.borderThin
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
    color: Color = TerminalColors.white,
    thickness: Dp = TerminalSpacing.borderStandard
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
    textColor: Color = TerminalColors.grey,
    lineColor: Color = TerminalColors.border
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
    ) {
        Text(
            text = "> ${text.uppercase()}",
            color = textColor,
            style = MaterialTheme.typography.labelMedium
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = TerminalSpacing.borderThin,
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
            .padding(TerminalSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
    ) {
        if (icon != null) {
            icon()
        }
        
        Box {
            Text(
                text = title.uppercase(),
                color = TerminalColors.white,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    color = TerminalColors.grey,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = TerminalSpacing.xl + TerminalSpacing.xs)
                )
            }
        }
    }
}
