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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ripple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

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
 * 28dp rounded corners.
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
            .clip(AppShapes.moduleCard)
            .background(AppColors.moduleBackground, AppShapes.moduleCard)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.moduleCard)
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
                    color = AppColors.white,
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
                color = if (isEnabled) AppColors.white else AppColors.textSecondary,
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
            uncheckedThumbColor = AppColors.white,
            uncheckedTrackColor = AppColors.surfaceVariant,
            uncheckedBorderColor = AppColors.border,
            disabledCheckedThumbColor = AppColors.grey,
            disabledCheckedTrackColor = AppColors.surfaceVariant,
            disabledUncheckedThumbColor = AppColors.grey,
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

// ═══════════════════════════════════════════════════════════════════════════════
// QUICK MODULE PREVIEWS (for specific module types)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * MCP Config module preview.
 */
@Composable
fun McpConfigModule(
    servers: List<McpServerItem>,
    modifier: Modifier = Modifier,
    onConfigClick: () -> Unit = {},
    onServerToggle: (String, Boolean) -> Unit = { _, _ -> }
) {
    ModuleCard(
        title = "MCP CONFIG",
        modifier = modifier,
        actionButton = {
            ModuleActionButton(
                text = "JSON CONFIG",
                onClick = onConfigClick
            )
        }
    ) {
        servers.forEachIndexed { index, server ->
            ModuleRowItem(
                title = server.name,
                subtitle = server.type,
                isEnabled = server.isEnabled,
                isConnected = server.isConnected,
                isTransitioning = server.isTransitioning,
                isStrikethrough = !server.isEnabled,
                onToggle = { onServerToggle(server.id, it) }
            )
            if (index < servers.lastIndex) {
                HorizontalDivider(
                    thickness = AppSpacing.borderThin,
                    color = AppColors.border
                )
            }
        }
    }
}
data class McpServerItem(
    val id: String,
    val name: String,
    val type: String,
    val isEnabled: Boolean,
    val isConnected: Boolean,
    val isTransitioning: Boolean = false
)

/**
 * Git Status module preview.
 */
@Composable
fun GitStatusModule(
    branchName: String,
    changedFiles: Int,
    modifier: Modifier = Modifier,
    onExpandClick: () -> Unit = {}
) {
    ModuleCard(
        title = "GIT STATUS",
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.1f)),
            onClick = onExpandClick
        ),
        actionButton = {
            MoccaIconButton(
                icon = Icons.Default.ChevronRight,
                onClick = onExpandClick,
                size = 32.dp,
                iconColor = AppColors.textSecondary
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "BRANCH",
                    color = AppColors.textTertiary,
                    style = AppTypography.labelSmall
                )
                Text(
                    text = branchName,
                    color = AppColors.white,
                    style = AppTypography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "CHANGES",
                    color = AppColors.textTertiary,
                    style = AppTypography.labelSmall
                )
                Text(
                    text = "$changedFiles FILES",
                    color = if (changedFiles > 0) AppColors.statusWaiting else AppColors.white,
                    style = AppTypography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Skills Engine module preview.
 */
@Composable
fun SkillsEngineModule(
    skills: List<SkillItem>,
    modifier: Modifier = Modifier,
    onFilterClick: () -> Unit = {},
    onSkillClick: (String) -> Unit = {}
) {
    ModuleCard(
        title = "SKILLS ENGINE",
        modifier = modifier,
        actionButton = {
            MoccaIconButton(
                icon = Icons.Default.Settings,
                onClick = onFilterClick,
                size = 32.dp,
                iconColor = AppColors.textSecondary
            )
        }
    ) {
        skills.forEach { skill ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSkillClick(skill.id) }
                    .padding(vertical = AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!skill.isActive) {
                    StatusDot(
                        color = AppColors.grey,
                        size = 6.dp,
                        showGlow = false
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                }
                Text(
                    text = skill.name.uppercase(),
                    color = if (skill.isActive) AppColors.white else AppColors.textTertiary,
                    style = AppTypography.bodySmall
                )
            }
        }
    }
}

data class SkillItem(
    val id: String,
    val name: String,
    val isActive: Boolean
)
