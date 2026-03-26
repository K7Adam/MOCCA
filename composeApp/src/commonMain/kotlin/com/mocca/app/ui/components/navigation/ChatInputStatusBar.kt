package com.mocca.app.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.mocca.app.domain.model.Mode
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Status bar with model/agent/variant pill chip selectors.
 */
@Composable
internal fun ChatInputStatusBar(
    modelName: String,
    agentName: String,
    providerResponse: ProviderResponse?,
    onModelSelectorClick: () -> Unit,
    variants: List<String>,
    selectedVariantId: String?,
    onVariantSelectorClick: () -> Unit,
    modes: List<Mode>,
    selectedModeId: String?,
    onModeSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(NavConstants.StatusBarHeight)
            .padding(horizontal = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        // Model selector - styled as pill chip
        Box(
            modifier = Modifier
                .height(NavConstants.StatusBarChipHeight)
                .background(
                    color = AppColors.surface.copy(alpha = 0.5f),
                    shape = AppShapes.pill
                )
                .border(
                    width = 0.5.dp,
                    color = AppColors.outline.copy(alpha = 0.3f),
                    shape = AppShapes.pill
                )
                .clickable(enabled = providerResponse != null, onClick = onModelSelectorClick)
                .padding(horizontal = AppSpacing.sm),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(NavConstants.StatusBarIconSize),
                    tint = AppColors.onSurfaceVariant
                )
                Text(
                    text = modelName.uppercase(),
                    color = if (providerResponse != null) AppColors.onSurfaceVariant else AppColors.outline,
                    style = AppTypography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Variant selector (if available) - pill chip
        if (variants.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .height(NavConstants.StatusBarChipHeight)
                    .background(
                        color = AppColors.surface.copy(alpha = 0.5f),
                        shape = AppShapes.pill
                    )
                    .border(
                        width = 0.5.dp,
                        color = AppColors.outline.copy(alpha = 0.3f),
                        shape = AppShapes.pill
                    )
                    .clickable(onClick = onVariantSelectorClick)
                    .padding(horizontal = AppSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(NavConstants.StatusBarIconSize),
                        tint = AppColors.outline
                    )
                    Text(
                        text = (selectedVariantId ?: "DEF").take(5).uppercase(),
                        color = AppColors.onSurfaceVariant,
                        style = AppTypography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }

        // Spacer to push agent selector to the right
        Spacer(modifier = Modifier.weight(1f))

        // Agent selector - pill chip with dropdown
        AgentSelectorChip(
            agentName = agentName,
            modes = modes,
            selectedModeId = selectedModeId,
            onModeSelected = onModeSelected
        )
    }
}

/**
 * Agent selector pill chip with dropdown menu.
 */
@Composable
private fun AgentSelectorChip(
    agentName: String,
    modes: List<Mode>,
    selectedModeId: String?,
    onModeSelected: (String?) -> Unit
) {
    Box {
        var showAgentMenu by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .height(NavConstants.StatusBarChipHeight)
                .background(
                    color = AppColors.surface.copy(alpha = 0.5f),
                    shape = AppShapes.pill
                )
                .border(
                    width = 0.5.dp,
                    color = AppColors.outline.copy(alpha = 0.3f),
                    shape = AppShapes.pill
                )
                .clickable { showAgentMenu = true }
                .padding(horizontal = AppSpacing.sm),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(NavConstants.StatusBarIconSize),
                    tint = AppColors.outline
                )
                Text(
                    text = agentName.take(8).uppercase(),
                    color = AppColors.outline,
                    style = AppTypography.labelSmall,
                    maxLines = 1
                )
            }
        }
        
        // Themed dropdown menu
        DropdownMenu(
            expanded = showAgentMenu,
            onDismissRequest = { showAgentMenu = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier
                .background(AppColors.surfaceContainerHigh, AppShapes.medium)
                .border(AppSpacing.borderThin, AppColors.outline.copy(alpha = 0.5f), AppShapes.medium)
        ) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            mode.name.uppercase(),
                            style = AppTypography.labelSmall,
                            color = if (mode.id == selectedModeId) AppColors.primary else AppColors.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onModeSelected(mode.id)
                        showAgentMenu = false
                    }
                )
            }
        }
    }
}
