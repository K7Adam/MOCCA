package com.mocca.app.ui.components.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Mode
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Status bar row for ChatInputBar showing model, variant, and agent selectors.
 *
 * @param modelName Current model name
 * @param agentName Current agent name
 * @param providerResponse Available providers (null = model selector disabled)
 * @param variants Available variants
 * @param selectedVariantId Currently selected variant ID
 * @param modes Available modes/agents
 * @param selectedModeId Currently selected mode ID
 * @param onModeSelected Callback when mode is selected
 * @param onModelSelectorClick Callback to open model selector dialog
 * @param onVariantSelectorClick Callback to open variant selector dialog
 * @param statusBarHeight Height of the status bar
 */
@Composable
internal fun ChatInputBarStatusRow(
    modelName: String,
    agentName: String,
    providerResponse: ProviderResponse?,
    variants: List<String>,
    selectedVariantId: String?,
    modes: List<Mode>,
    selectedModeId: String?,
    onModeSelected: (String?) -> Unit,
    onModelSelectorClick: () -> Unit,
    onVariantSelectorClick: () -> Unit,
    statusBarHeight: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(statusBarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Model selector
        Row(
            modifier = Modifier.clickable(enabled = providerResponse != null) { onModelSelectorClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = AppColors.outline
            )
            val displayModelName = modelName.substringAfterLast("/").let {
                if (it.length > 14) "…" + it.takeLast(13) else it
            }
            Text(
                text = displayModelName.uppercase(),
                color = if (providerResponse != null) AppColors.onSurfaceVariant else AppColors.outline,
                style = AppTypography.labelSmall
            )
        }

        // Variant selector (if available)
        if (variants.isNotEmpty()) {
            Row(
                modifier = Modifier.clickable { onVariantSelectorClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = AppColors.outline
                )
                Text(
                    text = (selectedVariantId ?: "DEF").take(6).uppercase(),
                    color = AppColors.onSurfaceVariant,
                    style = AppTypography.labelSmall
                )
            }
        }

        // Agent selector
        var showAgentSelector by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.clickable { showAgentSelector = true },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = AppColors.outline
            )
            Text(
                text = agentName.take(10).uppercase(),
                color = AppColors.outline,
                style = AppTypography.labelSmall
            )
        }

        if (showAgentSelector) {
            AgentSelectorBottomSheet(
                modes = modes,
                selectedModeId = selectedModeId,
                onModeSelected = onModeSelected,
                onDismiss = { showAgentSelector = false }
            )
        }
    }
}
