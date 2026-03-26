package com.mocca.app.ui.components.modern

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ripple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

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
                    color = AppColors.outline
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
 * Git Status module preview - God Mode redesign.
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
                iconColor = AppColors.onSurfaceVariant,
                contentDescription = "Open Git status"
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(AppShapes.small)
                    .background(AppColors.outline.copy(alpha=0.3f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                StatusDot(color = AppColors.accentGreen)
                Text(
                    text = branchName.uppercase(),
                    color = AppColors.onSurface,
                    style = AppTypography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "UNCOMMITTED",
                    color = AppColors.outline,
                    style = AppTypography.labelSmall
                )
                Text(
                    text = if (changedFiles > 0) String.format("%02d", changedFiles) else "00",
                    color = if (changedFiles > 0) AppColors.statusWaiting else AppColors.onSurfaceVariant,
                    style = AppTypography.headlineSmall,
                    fontWeight = FontWeight.Black
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
                iconColor = AppColors.onSurfaceVariant,
                contentDescription = "Filter skills"
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
                        color = AppColors.onSurfaceVariant,
                        size = 6.dp,
                        showGlow = false
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                }
                Text(
                    text = skill.name.uppercase(),
                    color = if (skill.isActive) AppColors.onSurface else AppColors.outline,
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
