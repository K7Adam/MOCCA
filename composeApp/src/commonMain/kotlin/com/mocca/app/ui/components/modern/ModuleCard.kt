package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.LocalAppPerformance
import com.mocca.app.ui.theme.moccaClickable

import com.mocca.app.ui.theme.innerShadow

/**
 * Module card components for the right swipe dashboard panel.
 * Modern design: Rounded cards, clean headers.
 */

// MODULE CARD (Main container for dashboard modules)


/**
 * Module card for the modular dashboard.
 * Contains a header with icon and optional action button, plus content area.
 * Uses the dedicated moduleCard shape to remain visually distinct from standard cards.
 */
@Composable
fun ModuleCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = AppColors.moduleBackground,
    shape: Shape = AppShapes.moduleCard,
    actionButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val useDecorativeShadows = LocalAppPerformance.current.useHeavyNavigationMotion

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor, shape)
            .innerShadow(
                enabled = useDecorativeShadows,
                shape = shape,
                color = AppColors.white.copy(alpha = 0.05f),
                blur = 2.dp
            )
            // Borderless: using bgRaised for visual separation instead of border
    ) {
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
                        tint = AppColors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "{ }",
                        color = AppColors.outline,
                        style = AppTypography.bodyMedium
                    )
                }
                Text(
                    text = title.uppercase(),
                    color = AppColors.onSurface,
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

// MODULE ROW ITEM (Item inside a module card)


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
            .moccaClickable(
                onClick = onClick,
                pressedScale = 0.99f,
                rippleColor = AppColors.white.copy(alpha = 0.1f)
            )
            .padding(vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator (Only show if not normal state: Disabled or Disconnected)
        if (!isEnabled || !isConnected) {
            StatusDot(
                color = when {
                    !isEnabled -> AppColors.outline
                    else -> AppColors.statusOffline
                },
                size = AppSpacing.statusDotSizeLarge
            )
            Spacer(modifier = Modifier.width(AppSpacing.md))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isEnabled) AppColors.onSurface else AppColors.onSurfaceVariant,
                style = AppTypography.bodyMedium.copy(
                    textDecoration = if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None
                ),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = AppColors.outline,
                style = AppTypography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

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

// TERMINAL TOGGLE (Rounded pill switch)


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
            checkedTrackColor = AppColors.primary,
            checkedBorderColor = AppColors.primary,
            uncheckedThumbColor = AppColors.onSurface,
            uncheckedTrackColor = AppColors.surfaceVariant,
            uncheckedBorderColor = AppColors.outline,
            disabledCheckedThumbColor = AppColors.onSurfaceVariant,
            disabledCheckedTrackColor = AppColors.surfaceVariant,
            disabledUncheckedThumbColor = AppColors.onSurfaceVariant,
            disabledUncheckedTrackColor = AppColors.surfaceVariant
        )
    )
}

// MODULE ACTION BUTTON (for card headers)


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
            .heightIn(min = 48.dp)
            .clip(AppShapes.pill)
            .background(AppColors.surfaceContainerHigh, AppShapes.pill)
            .moccaClickable(
                onClick = onClick,
                pressedScale = 0.97f,
                rippleColor = AppColors.white.copy(alpha = 0.1f)
            )
            .padding(horizontal = AppSpacing.md, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = AppColors.onSurface,
            style = AppTypography.labelSmall
        )
    }
}
