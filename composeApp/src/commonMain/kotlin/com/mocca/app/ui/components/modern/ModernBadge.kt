package com.mocca.app.ui.components.modern

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
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
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
fun RoleBadge(
    role: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.badgeBackground,
    textColor: Color = AppColors.badgeText,
    paddingHorizontal: Dp = AppSpacing.badgePaddingHorizontal,
    paddingVertical: Dp = AppSpacing.badgePaddingVertical,
    shape: Shape = AppShapes.badge
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
            style = AppTypography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Outlined pill badge - transparent background, subtle border.
 * Used for secondary labels.
 */
@Composable
fun OutlinedBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    textColor: Color = AppColors.textSecondary,
    borderColor: Color = AppColors.border,
    paddingHorizontal: Dp = AppSpacing.badgePaddingHorizontal,
    paddingVertical: Dp = AppSpacing.badgePaddingVertical,
    shape: Shape = AppShapes.pill
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(AppSpacing.borderThin, borderColor, shape)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            style = AppTypography.labelSmall,
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
fun StatusBadge(
    text: String,
    status: ModernStatus,
    modifier: Modifier = Modifier,
    showDot: Boolean = true
) {
    val (dotColor, textColor) = when (status) {
        ModernStatus.ONLINE -> AppColors.accent to AppColors.accent
        ModernStatus.OFFLINE -> AppColors.statusOffline to AppColors.statusOffline
        ModernStatus.WAITING -> AppColors.statusWaiting to AppColors.statusWaiting
        ModernStatus.IDLE -> AppColors.textTertiary to AppColors.textTertiary
    }
    
    Row(
        modifier = modifier
            .clip(AppShapes.pill)
            .background(AppColors.surfaceVariant, AppShapes.pill)
            .padding(horizontal = AppSpacing.badgePaddingHorizontal, vertical = AppSpacing.badgePaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        if (showDot) {
            StatusDot(color = dotColor)
        }
        Text(
            text = text.uppercase(),
            color = textColor,
            style = AppTypography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

enum class ModernStatus {
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
    size: Dp = AppSpacing.statusDotSize,
    showGlow: Boolean = true
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (showGlow) {
            // Subtle glow/outer ring
            Box(
                modifier = Modifier
                    .size(size + 2.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape)
            )
        }
        // Core dot
        Box(
            modifier = Modifier
                .size(size)
                .background(color, CircleShape)
        )
    }
}

/**
 * Square status indicator - legacy terminal style.
 * Now uses rounded corners for modern aesthetic.
 */
@Composable
fun StatusSquare(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = AppSpacing.statusDotSize,
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
                .clip(AppShapes.extraSmall)
                .background(color.copy(alpha = alpha), AppShapes.extraSmall)
                .border(1.dp, color, AppShapes.extraSmall)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(AppShapes.extraSmall)
                .background(color, AppShapes.extraSmall)
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
    backgroundColor: Color = AppColors.surfaceVariant,
    textColor: Color = AppColors.textSecondary,
    paddingHorizontal: Dp = AppSpacing.tagPaddingHorizontal,
    paddingVertical: Dp = AppSpacing.tagPaddingVertical,
    shape: Shape = AppShapes.tag
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
            style = AppTypography.labelSmall
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
    shape: Shape = AppShapes.pill
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(AppColors.accent.copy(alpha = 0.15f), shape)
            .padding(horizontal = AppSpacing.badgePaddingHorizontal, vertical = AppSpacing.badgePaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        StatusDot(color = AppColors.accent, size = AppSpacing.statusDotSizeSmall)
        Text(
            text = "CONNECTED",
            color = AppColors.accent,
            style = AppTypography.labelSmall,
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
        color = AppColors.textSecondary,
        style = AppTypography.labelSmall,
        modifier = modifier
    )
}
