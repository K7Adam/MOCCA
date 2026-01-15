package com.mocca.app.ui.components.terminal

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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ripple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import androidx.compose.material3.MaterialTheme

/**
 * Module card components for the right swipe dashboard panel.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// MODULE CARD (Main container for dashboard modules)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Module card for the modular dashboard.
 * Contains a header with icon and optional action button, plus content area.
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
            .border(TerminalSpacing.borderThin, TerminalColors.borderLight, RectangleShape)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TerminalSpacing.cardPadding,
                    vertical = TerminalSpacing.sm
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
            ) {
                if (icon != null) {
                    Text(
                        text = "{ }",
                        color = TerminalColors.white,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = title.uppercase(),
                    color = TerminalColors.white,
                    style = MaterialTheme.typography.headlineSmall,
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
                .padding(horizontal = TerminalSpacing.cardPadding)
                .padding(bottom = TerminalSpacing.cardPadding),
            content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODULE ROW ITEM (Item inside a module card)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Module row item with status indicator, text, and optional toggle.
 * Used inside ModuleCard for MCP servers, skills, etc.
 */
@Composable
fun ModuleRowItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isConnected: Boolean = true,
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
                        indication = ripple(color = TerminalColors.white.copy(alpha = 0.1f)),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(vertical = TerminalSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        StatusSquare(
            color = when {
                !isEnabled -> TerminalColors.greyDark
                isConnected -> TerminalColors.statusOnline
                else -> TerminalColors.statusOffline
            }
        )
        
        Spacer(modifier = Modifier.width(TerminalSpacing.sm))
        
        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isEnabled) TerminalColors.white else TerminalColors.grey,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None
                ),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = TerminalColors.grey,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Toggle switch
        if (showToggle && onToggle != null) {
            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
            TerminalToggle(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TERMINAL TOGGLE (Rectangular switch)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Terminal-styled toggle switch (rectangular, not rounded).
 */
@Composable
fun TerminalToggle(
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
            checkedThumbColor = TerminalColors.white,
            checkedTrackColor = TerminalColors.surfaceVariant,
            checkedBorderColor = TerminalColors.white,
            uncheckedThumbColor = TerminalColors.grey,
            uncheckedTrackColor = TerminalColors.surfaceVariant,
            uncheckedBorderColor = TerminalColors.grey,
            disabledCheckedThumbColor = TerminalColors.greyDark,
            disabledCheckedTrackColor = TerminalColors.surfaceVariant,
            disabledUncheckedThumbColor = TerminalColors.greyDark,
            disabledUncheckedTrackColor = TerminalColors.surfaceVariant
        )
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODULE ACTION BUTTON (for card headers)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Outlined action button for module card headers.
 * e.g., [JSON_CONFIG], [FILTER], [EXPAND]
 */
@Composable
fun ModuleActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(TerminalSpacing.borderThin, TerminalColors.grey, RectangleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = TerminalColors.white.copy(alpha = 0.1f)),
                onClick = onClick
            )
            .padding(horizontal = TerminalSpacing.sm, vertical = TerminalSpacing.xs)
    ) {
        Text(
            text = "[$text]".uppercase(),
            color = TerminalColors.grey,
            style = MaterialTheme.typography.labelSmall
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
        title = "MCP_CONFIG",
        modifier = modifier,
        actionButton = {
            ModuleActionButton(
                text = "JSON_CONFIG",
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
                isStrikethrough = !server.isEnabled,
                onToggle = { onServerToggle(server.id, it) }
            )
            if (index < servers.lastIndex) {
                HorizontalDivider(
                    thickness = TerminalSpacing.borderThin,
                    color = TerminalColors.border
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
    val isConnected: Boolean
)

/**
 * Git Status module preview.
 * The entire card is clickable for better UX.
 */
@Composable
fun GitStatusModule(
    branchName: String,
    changedFiles: Int,
    modifier: Modifier = Modifier,
    onExpandClick: () -> Unit = {}
) {
    ModuleCard(
        title = "GIT_STATUS",
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.1f)),
            onClick = onExpandClick
        ),
        actionButton = {
            TerminalIconButton(
                icon = Icons.Default.ChevronRight,
                onClick = onExpandClick,
                size = 32.dp,
                iconColor = TerminalColors.grey
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
                    color = TerminalColors.grey,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = branchName,
                    color = TerminalColors.white,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "CHANGES",
                    color = TerminalColors.grey,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "$changedFiles FILES",
                    color = if (changedFiles > 0) TerminalColors.statusWaiting else TerminalColors.white,
                    style = MaterialTheme.typography.bodyMedium,
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
        title = "SKILLS_ENGINE",
        modifier = modifier,
        actionButton = {
            TerminalIconButton(
                icon = Icons.Default.Settings,
                onClick = onFilterClick,
                size = 32.dp,
                iconColor = TerminalColors.grey
            )
        }
    ) {
        skills.forEach { skill ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSkillClick(skill.id) }
                    .padding(vertical = TerminalSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusSquare(
                    color = if (skill.isActive) TerminalColors.statusOnline else TerminalColors.grey
                )
                Spacer(modifier = Modifier.width(TerminalSpacing.sm))
                Text(
                    text = skill.name.uppercase(),
                    color = TerminalColors.white,
                    style = MaterialTheme.typography.bodySmall
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
