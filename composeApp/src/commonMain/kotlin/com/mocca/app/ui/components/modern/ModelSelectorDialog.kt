package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.AiModelOption
import com.mocca.app.domain.model.AiProviderOption
import com.mocca.app.domain.model.ModelPickerUiState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.moccaClickable

@Composable
fun ModelSelectorDialog(
    state: ModelPickerUiState,
    onModelSelected: (providerId: String, modelId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val normalizedQuery = remember(searchQuery) { searchQuery.normalizeModelQuery() }
    val filteredProviders = remember(state.providers, normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            state.providers
        } else {
            state.providers.mapNotNull { provider ->
                val providerMatches = provider.name.normalizeModelQuery().contains(normalizedQuery) ||
                    provider.id.normalizeModelQuery().contains(normalizedQuery)
                val models = provider.models.filter { model ->
                    providerMatches ||
                        model.name.normalizeModelQuery().contains(normalizedQuery) ||
                        model.id.normalizeModelQuery().contains(normalizedQuery)
                }
                when {
                    providerMatches -> provider
                    models.isNotEmpty() -> provider.copy(models = models)
                    else -> null
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.background,
        contentColor = AppColors.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.outline) },
        shape = AppShapes.bottomSheetExpanded
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Model",
                    style = AppTypography.titleMedium,
                    color = AppColors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                MoccaIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    iconColor = AppColors.onSurfaceVariant
                )
            }

            HorizontalDivider(thickness = AppSpacing.borderThin, color = AppColors.outline)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = AppTypography.bodySmall.copy(color = AppColors.onSurface),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search models",
                                    style = AppTypography.bodySmall,
                                    color = AppColors.onSurfaceVariantDark
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    MoccaIconButton(
                        icon = Icons.Default.Close,
                        onClick = { searchQuery = "" },
                        iconColor = AppColors.onSurfaceVariant,
                        size = 16.dp
                    )
                }
            }

            HorizontalDivider(thickness = AppSpacing.borderThin, color = AppColors.outline)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(AppSpacing.sm)
            ) {
                state.current?.let { current ->
                    item(key = "current", contentType = "section-header") {
                        SectionLabel("Current")
                    }
                    item(key = "current-${current.providerId}-${current.modelId}", contentType = "model-row") {
                        ModelRow(
                            providerName = current.providerName,
                            model = AiModelOption(
                                providerId = current.providerId,
                                id = current.modelId,
                                name = current.modelName
                            ),
                            selected = true,
                            onClick = {
                                onModelSelected(current.providerId, current.modelId)
                                onDismiss()
                            }
                        )
                    }
                }

                if (state.recentModels.isNotEmpty() && normalizedQuery.isEmpty()) {
                    item(key = "recent", contentType = "section-header") { SectionLabel("Recent") }
                    items(
                        items = state.recentModels,
                        key = { recent -> "recent-${recent.providerId}-${recent.modelId}" },
                        contentType = { "model-row" }
                    ) { recent ->
                        ModelRow(
                            providerName = recent.providerName,
                            model = AiModelOption(
                                providerId = recent.providerId,
                                id = recent.modelId,
                                name = recent.displayName
                            ),
                            selected = recent.providerId == state.selectedProviderId &&
                                recent.modelId == state.selectedModelId,
                            onClick = {
                                onModelSelected(recent.providerId, recent.modelId)
                                onDismiss()
                            }
                        )
                    }
                }

                if (filteredProviders.isNotEmpty()) {
                    item(key = "providers", contentType = "section-header") { SectionLabel("Connected Providers") }
                    items(
                        items = filteredProviders,
                        key = { provider -> "provider-${provider.id}" },
                        contentType = { "provider-section" }
                    ) { provider ->
                        ProviderSection(
                            provider = provider,
                            selectedProviderId = state.selectedProviderId,
                            selectedModelId = state.selectedModelId,
                            forceExpanded = normalizedQuery.isNotEmpty(),
                            onModelSelected = { modelId ->
                                onModelSelected(provider.id, modelId)
                                onDismiss()
                            }
                        )
                    }
                } else {
                    item(key = "empty", contentType = "empty") {
                        Text(
                            text = state.errorMessage ?: "No configured provider",
                            style = AppTypography.bodySmall,
                            color = AppColors.onSurfaceVariant,
                            modifier = Modifier.padding(AppSpacing.md)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = AppTypography.labelSmall,
        color = AppColors.primary,
        modifier = Modifier.padding(start = AppSpacing.sm, top = AppSpacing.sm, bottom = AppSpacing.xs)
    )
}

@Composable
private fun ProviderSection(
    provider: AiProviderOption,
    selectedProviderId: String?,
    selectedModelId: String?,
    forceExpanded: Boolean,
    onModelSelected: (modelId: String) -> Unit
) {
    var manualExpanded by remember(provider.id) { mutableStateOf(provider.id == selectedProviderId) }
    val expanded = forceExpanded || manualExpanded

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .moccaClickable(onClick = { manualExpanded = !manualExpanded }, pressedScale = 0.99f)
                .background(
                    if (provider.id == selectedProviderId) AppColors.surface else AppColors.background
                )
                .padding(AppSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = provider.name.uppercase(),
                style = AppTypography.bodyMedium,
                color = if (provider.connected) AppColors.onSurface else AppColors.onSurfaceVariantDark,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${provider.models.size} models",
                style = AppTypography.labelSmall,
                color = AppColors.onSurfaceVariant
            )
        }

        if (expanded) {
            provider.models.forEach { model ->
                ModelRow(
                    providerName = provider.name,
                    model = model,
                    selected = provider.id == selectedProviderId && model.id == selectedModelId,
                    onClick = { onModelSelected(model.id) }
                )
            }
        }
    }
}

@Composable
private fun ModelRow(
    providerName: String,
    model: AiModelOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .moccaClickable(onClick = onClick, pressedScale = 0.99f)
            .background(if (selected) AppColors.primary.copy(alpha = 0.18f) else AppColors.background)
            .padding(start = AppSpacing.xl, end = AppSpacing.md, top = AppSpacing.xs, bottom = AppSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.name.ifBlank { model.id },
                style = AppTypography.bodySmall,
                color = if (selected) AppColors.primary else AppColors.onSurfaceVariantLight,
                maxLines = 1
            )
            Text(
                text = providerName,
                style = AppTypography.labelSmall,
                color = AppColors.outline,
                maxLines = 1
            )
        }
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = AppColors.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun String.normalizeModelQuery(): String =
    lowercase().trim().replace(Regex("[\\s_-]+"), " ")
