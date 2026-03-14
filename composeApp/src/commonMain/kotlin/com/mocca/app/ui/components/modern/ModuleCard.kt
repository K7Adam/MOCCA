package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

import com.mocca.app.ui.theme.innerShadow

/**
 * Module card components for the right swipe dashboard panel.
 * Modern design: Rounded cards, clean headers.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// MODULE CARD (Main container for dashboard modules)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Module card for the modular dashboard.
 * Contains a header with icon and optional action button, plus content area.
 * Now using M3 Expressive Gem shape and inner shadow.
 */
@Composable
fun ModuleCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.large)
            .background(AppColors.moduleBackground, AppShapes.large)
            .innerShadow(AppShapes.large, color = AppColors.white.copy(alpha = 0.05f), blur = 2.dp)
            .border(AppSpacing.borderThin, AppColors.border.copy(alpha = 0.3f), AppShapes.large)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacing.cardPadding,
                    vertical = AppSpacing.md
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "{ }",
                        color = AppColors.textTertiary,
                        style = AppTypography.bodyMedium
                    )
                }
                Text(
                    text = title.uppercase(),
                    color = AppColors.textPrimary,
                    style = AppTypography.labelLarge, // Updated typography
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (actionButton != null) {
                actionButton()
            }
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.cardPadding)
                .padding(bottom = AppSpacing.cardPadding),
            content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODULE ROW ITEM (Item inside a module card)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Module row item with status indicator, text, and optional toggle.
 */
@Composable
fun ModuleRowItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isConnected: Boolean = true,
    isTransitioning: Boolean = false,
    showToggle: Boolean = true,
    onToggle: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isStrikethrough: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = AppColors.white.copy(alpha = 0.1f)),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator (Only show if not normal state: Disabled or Disconnected)
        if (!isEnabled || !isConnected) {
            StatusDot(
                color = when {
                    !isEnabled -> AppColors.textTertiary
                    else -> AppColors.statusOffline
                },
                size = AppSpacing.statusDotSizeLarge
            )
            Spacer(modifier = Modifier.width(AppSpacing.md))
        }
        
        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isEnabled) AppColors.textPrimary else AppColors.textSecondary,
                style = AppTypography.bodyMedium.copy(
                    textDecoration = if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None
                ),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = AppColors.textTertiary,
                style = AppTypography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Toggle switch
        if (showToggle && onToggle != null) {
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            ModernToggle(
                checked = isEnabled,
                onCheckedChange = onToggle,
                enabled = !isTransitioning
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TERMINAL TOGGLE (Rounded pill switch)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Modern toggle switch.
 */
@Composable
fun ModernToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = AppColors.background,
            checkedTrackColor = AppColors.accentGreen,
            checkedBorderColor = AppColors.accentGreen,
            uncheckedThumbColor = AppColors.textPrimary,
            uncheckedTrackColor = AppColors.surfaceVariant,
            uncheckedBorderColor = AppColors.border,
            disabledCheckedThumbColor = AppColors.textSecondary,
            disabledCheckedTrackColor = AppColors.surfaceVariant,
            disabledUncheckedThumbColor = AppColors.textSecondary,
            disabledUncheckedTrackColor = AppColors.surfaceVariant
        )
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODULE ACTION BUTTON (for card headers)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Outlined action button for module card headers.
 */
@Composable
fun ModuleActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppShapes.small)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.small)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = AppColors.white.copy(alpha = 0.1f)),
                onClick = onClick
            )
            .padding(horizontal = AppSpacing.sm, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
    }
}

