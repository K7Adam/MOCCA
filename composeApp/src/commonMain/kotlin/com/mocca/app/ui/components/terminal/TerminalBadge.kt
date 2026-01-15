package com.mocca.app.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import androidx.compose.material3.MaterialTheme

/**
 * Terminal-styled badges for labels and status indicators.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// ROLE BADGE (USER/AGENT labels)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Role badge - white background, black text.
 * Used for message sender labels: USER, AGENT.
 */
@Composable
fun TerminalRoleBadge(
    role: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.badgeBackground,
    textColor: Color = TerminalColors.badgeText,
    paddingHorizontal: Dp = TerminalSpacing.sm,
    paddingVertical: Dp = TerminalSpacing.xs
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RectangleShape)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
    ) {
        Text(
            text = role.uppercase(),
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Inverted badge - transparent/dark background, white text, white border.
 * Used for secondary labels.
 */
@Composable
fun TerminalOutlinedBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    textColor: Color = TerminalColors.white,
    borderColor: Color = TerminalColors.borderLight,
    paddingHorizontal: Dp = TerminalSpacing.sm,
    paddingVertical: Dp = TerminalSpacing.xs
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RectangleShape)
            .border(TerminalSpacing.borderThin, borderColor, RectangleShape)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STATUS BADGE (with color indicator)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Status badge with colored indicator dot.
 * Used for connection status, session status, etc.
 */
@Composable
fun TerminalStatusBadge(
    text: String,
    status: TerminalStatus,
    modifier: Modifier = Modifier,
    showDot: Boolean = true
) {
    val (dotColor, textColor) = when (status) {
        TerminalStatus.ONLINE -> TerminalColors.statusOnline to TerminalColors.statusOnline
        TerminalStatus.OFFLINE -> TerminalColors.statusOffline to TerminalColors.statusOffline
        TerminalStatus.WAITING -> TerminalColors.statusWaiting to TerminalColors.statusWaiting
        TerminalStatus.IDLE -> TerminalColors.grey to TerminalColors.grey
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
    ) {
        if (showDot) {
            StatusDot(color = dotColor)
        }
        Text(
            text = text.uppercase(),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

enum class TerminalStatus {
    ONLINE,
    OFFLINE,
    WAITING,
    IDLE
}

// ═══════════════════════════════════════════════════════════════════════════════
// STATUS DOT
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Simple status dot indicator.
 */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = TerminalSpacing.statusDotSize
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
    )
}

/**
 * Square status indicator (terminal style).
 */
@Composable
fun StatusSquare(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = TerminalSpacing.statusDotSize
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, RectangleShape)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAG BADGE (for categories, file types, etc.)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Small tag badge - for inline metadata.
 */
@Composable
fun TerminalTag(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.surfaceVariant,
    textColor: Color = TerminalColors.whiteMuted,
    paddingHorizontal: Dp = TerminalSpacing.xs,
    paddingVertical: Dp = 2.dp
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RectangleShape)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONNECTED/EDIT BADGES (from Settings mockup)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Connected badge - inverted white badge.
 */
@Composable
fun TerminalConnectedBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(TerminalColors.white, RectangleShape)
            .padding(horizontal = TerminalSpacing.sm, vertical = TerminalSpacing.xs)
    ) {
        Text(
            text = "CONNECTED",
            color = TerminalColors.background,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Edit badge - grey text in brackets.
 */
@Composable
fun TerminalEditBadge(
    modifier: Modifier = Modifier
) {
    Text(
        text = "[EDIT]",
        color = TerminalColors.grey,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
    )
}
