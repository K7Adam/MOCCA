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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalShapes
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.getValue

/**
 * Modern MOCCA badge components with pill-shaped design.
 * Based on UI overhaul designs - rounded corners, subtle backgrounds.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// ROLE BADGE (USER/AGENT labels - pill shaped)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Role badge - pill-shaped with light background.
 * Used for message sender labels: USER, AGENT.
 */
@Composable
fun TerminalRoleBadge(
    role: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.badgeBackground,
    textColor: Color = TerminalColors.badgeText,
    paddingHorizontal: Dp = TerminalSpacing.badgePaddingHorizontal,
    paddingVertical: Dp = TerminalSpacing.badgePaddingVertical,
    shape: Shape = TerminalShapes.badge
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
    ) {
        Text(
            text = role.uppercase(),
            color = textColor,
            style = TerminalTypography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Outlined pill badge - transparent background, subtle border.
 * Used for secondary labels.
 */
@Composable
fun TerminalOutlinedBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    textColor: Color = TerminalColors.textSecondary,
    borderColor: Color = TerminalColors.border,
    paddingHorizontal: Dp = TerminalSpacing.badgePaddingHorizontal,
    paddingVertical: Dp = TerminalSpacing.badgePaddingVertical,
    shape: Shape = TerminalShapes.pill
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(TerminalSpacing.borderThin, borderColor, shape)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            style = TerminalTypography.labelSmall,
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
        TerminalStatus.IDLE -> TerminalColors.textTertiary to TerminalColors.textTertiary
    }
    
    Row(
        modifier = modifier
            .clip(TerminalShapes.pill)
            .background(TerminalColors.surfaceVariant, TerminalShapes.pill)
            .padding(horizontal = TerminalSpacing.badgePaddingHorizontal, vertical = TerminalSpacing.badgePaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
    ) {
        if (showDot) {
            StatusDot(color = dotColor)
        }
        Text(
            text = text.uppercase(),
            color = textColor,
            style = TerminalTypography.labelSmall,
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
 * Square status indicator - legacy terminal style.
 * Now uses rounded corners for modern aesthetic.
 */
@Composable
fun StatusSquare(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = TerminalSpacing.statusDotSize,
    isTransitioning: Boolean = false
) {
    if (isTransitioning) {
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable<Float>(
                animation = tween<Float>(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        
        Box(
            modifier = modifier
                .size(size)
                .clip(TerminalShapes.extraSmall)
                .background(color.copy(alpha = alpha), TerminalShapes.extraSmall)
                .border(1.dp, color, TerminalShapes.extraSmall)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(TerminalShapes.extraSmall)
                .background(color, TerminalShapes.extraSmall)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAG BADGE (for categories, file types, etc.)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Small tag badge - pill-shaped for inline metadata.
 */
@Composable
fun TerminalTag(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.surfaceVariant,
    textColor: Color = TerminalColors.textSecondary,
    paddingHorizontal: Dp = TerminalSpacing.tagPaddingHorizontal,
    paddingVertical: Dp = TerminalSpacing.tagPaddingVertical,
    shape: Shape = TerminalShapes.tag
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            style = TerminalTypography.labelSmall
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONNECTED/EDIT BADGES (modern pill-shaped)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Connected badge - pill-shaped success indicator.
 */
@Composable
fun TerminalConnectedBadge(
    modifier: Modifier = Modifier,
    shape: Shape = TerminalShapes.pill
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(TerminalColors.statusOnline.copy(alpha = 0.15f), shape)
            .padding(horizontal = TerminalSpacing.badgePaddingHorizontal, vertical = TerminalSpacing.badgePaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
    ) {
        StatusDot(color = TerminalColors.statusOnline, size = TerminalSpacing.statusDotSizeSmall)
        Text(
            text = "CONNECTED",
            color = TerminalColors.statusOnline,
            style = TerminalTypography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Edit badge - subtle text button style.
 */
@Composable
fun TerminalEditBadge(
    modifier: Modifier = Modifier
) {
    Text(
        text = "EDIT",
        color = TerminalColors.textSecondary,
        style = TerminalTypography.labelSmall,
        modifier = modifier
    )
}
