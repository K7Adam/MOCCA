package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.mocca.app.domain.model.ProviderInfo
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.domain.model.RecentModel
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.serialization.json.JsonObject

/**
 * Terminal-styled model selection dialog.
 * Shows available providers and their models for selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorDialog(
    providerResponse: ProviderResponse,
    selectedProviderId: String,
    selectedModelId: String,
    onModelSelected: (providerId: String, modelId: String) -> Unit,
    recentModels: List<RecentModel> = emptyList(),
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.background,
        contentColor = AppColors.white,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.border) },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "// SELECT MODEL",
                    style = AppTypography.titleMedium,
                    color = AppColors.white,
                    fontWeight = FontWeight.Bold
                )
                MoccaIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    iconColor = AppColors.grey
                )
            }
            
            HorizontalDivider(
                thickness = AppSpacing.borderThin,
                color = AppColors.border
            )
            
            // Search bar
            var searchQuery by remember { mutableStateOf("") }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> ",
                    style = AppTypography.bodySmall,
                    color = AppColors.accentGreen
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = AppTypography.bodySmall.copy(color = AppColors.white),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = AppSpacing.xs),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search models...",
                                    style = AppTypography.bodySmall,
                                    color = AppColors.greyDark
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
                        iconColor = AppColors.grey,
                        size = 16.dp
                    )
                }
            }
            
            HorizontalDivider(
                thickness = AppSpacing.borderThin,
                color = AppColors.border
            )
            
            // Provider/Model list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(AppSpacing.sm)
            ) {
                // Only show authenticated providers (those in the connected list)
                val authenticatedProviders = providerResponse.all.filter { 
                    it.id in providerResponse.connected 
                }
                
                // Apply search filter with normalization (spaces = hyphens = underscores)
                val rawQuery = searchQuery.lowercase().trim()
                val query = rawQuery.replace(Regex("[\\s_-]+"), " ")
                
                fun normalizeForSearch(text: String): String = 
                    text.lowercase().replace(Regex("[\\s_-]+"), " ")
                
                val filteredProviders = if (query.isEmpty()) {
                    authenticatedProviders
                } else {
                    authenticatedProviders.filter { provider ->
                        val normalizedProvider = normalizeForSearch(provider.name)
                        val matchesProvider = normalizedProvider.contains(query) ||
                            normalizeForSearch(provider.id).contains(query)
                        val models = (provider.models as? JsonObject)?.keys?.toList() ?: emptyList()
                        val matchesModel = models.any { normalizeForSearch(it).contains(query) }
                        matchesProvider || matchesModel
                    }
                }
                
                // Show Recent Models if any
                if (recentModels.isNotEmpty() && query.isEmpty()) {
                   item {
                       Text(
                           text = "// RECENT",
                           style = AppTypography.labelSmall,
                           color = AppColors.accentGreen,
                           modifier = Modifier.padding(
                               start = AppSpacing.sm,
                               top = AppSpacing.sm,
                               bottom = AppSpacing.xs
                           )
                       )
                   }
                   
                   recentModels.forEach { recent ->
                       item {
                           val isSelected = recent.providerId == selectedProviderId && recent.modelId == selectedModelId
                           Row(
                               modifier = Modifier
                                   .fillMaxWidth()
                                   .clickable { 
                                       onModelSelected(recent.providerId, recent.modelId)
                                       onDismiss()
                                   }
                                   .background(
                                       if (isSelected) 
                                           AppColors.accentGreen.copy(alpha = 0.2f) 
                                       else 
                                           AppColors.background
                                   )
                                   .padding(
                                       horizontal = AppSpacing.md,
                                       vertical = AppSpacing.xs
                                   ),
                               horizontalArrangement = Arrangement.SpaceBetween,
                               verticalAlignment = Alignment.CenterVertically
                           ) {
                               Text(
                                   text = "> ${recent.modelId.uppercase()} [${recent.providerId.uppercase()}]",
                                   style = AppTypography.bodySmall,
                                   color = if (isSelected) AppColors.accentGreen else AppColors.white
                               )
                           }
                       }
                   }
                   
                   item {
                       HorizontalDivider(
                           thickness = AppSpacing.borderThin,
                           color = AppColors.border,
                           modifier = Modifier.padding(vertical = AppSpacing.sm)
                       )
                   }
                }

                if (filteredProviders.isNotEmpty()) {
                    item {
                        Text(
                            text = "// CONNECTED",
                            style = AppTypography.labelSmall,
                            color = AppColors.accentGreen,
                            modifier = Modifier.padding(
                                start = AppSpacing.sm,
                                top = AppSpacing.sm,
                                bottom = AppSpacing.xs
                            )
                        )
                    }
                    
                    filteredProviders.forEach { provider ->
                        item {
                            ProviderSection(
                                provider = provider,
                                selectedProviderId = selectedProviderId,
                                selectedModelId = selectedModelId,
                                onModelSelected = { modelId ->
                                    onModelSelected(provider.id, modelId)
                                    onDismiss()
                                },
                                searchQuery = query
                            )
                        }
                    }
                } else if (query.isNotEmpty()) {
                    // No search results
                    item {
                        Text(
                            text = "// NO MATCHES",
                            style = AppTypography.labelSmall,
                            color = AppColors.grey,
                            modifier = Modifier.padding(AppSpacing.md)
                        )
                    }
                } else {
                    // No authenticated providers
                    item {
                        Text(
                            text = "// NO PROVIDERS CONFIGURED",
                            style = AppTypography.labelSmall,
                            color = AppColors.grey,
                            modifier = Modifier.padding(AppSpacing.md)
                        )
                        Text(
                            text = "Configure providers in opencode settings.",
                            style = AppTypography.bodySmall,
                            color = AppColors.greyDark,
                            modifier = Modifier.padding(
                                start = AppSpacing.md,
                                top = AppSpacing.xs
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderSection(
    provider: ProviderInfo,
    selectedProviderId: String,
    selectedModelId: String,
    onModelSelected: (modelId: String) -> Unit,
    enabled: Boolean = true,
    searchQuery: String = ""
) {
    // Auto-expand when search is active, otherwise use manual toggle state
    var manualExpanded by remember { mutableStateOf(provider.id == selectedProviderId) }
    val expanded = searchQuery.isNotEmpty() || manualExpanded
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs)
    ) {
        // Provider header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { manualExpanded = !manualExpanded }
                .background(
                    if (provider.id == selectedProviderId) 
                        AppColors.surface 
                    else 
                        AppColors.background
                )
                .padding(AppSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (expanded) "[-]" else "[+]",
                    style = AppTypography.bodySmall,
                    color = AppColors.grey
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Text(
                    text = provider.name.uppercase(),
                    style = AppTypography.bodyMedium,
                    color = if (enabled) AppColors.white else AppColors.greyDark,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "${provider.modelCount} models",
                style = AppTypography.labelSmall,
                color = AppColors.grey
            )
        }
        
        // Model list (when expanded)
        if (expanded) {
            val allModels = (provider.models as? JsonObject)?.keys?.toList() ?: emptyList()
            fun normalizeForSearch(text: String): String = 
                text.lowercase().replace(Regex("[\\s_-]+"), " ")
            val normalizedQuery = searchQuery.replace(Regex("[\\s_-]+"), " ")
            val models = if (searchQuery.isEmpty()) allModels else {
                allModels.filter { normalizeForSearch(it).contains(normalizedQuery) }
            }
            models.forEach { modelId ->
                val isSelected = provider.id == selectedProviderId && modelId == selectedModelId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { onModelSelected(modelId) }
                        .background(
                            if (isSelected) 
                                AppColors.accentGreen.copy(alpha = 0.2f) 
                            else 
                                AppColors.background
                        )
                        .padding(
                            start = AppSpacing.xl,
                            end = AppSpacing.md,
                            top = AppSpacing.xs,
                            bottom = AppSpacing.xs
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "> ${modelId.replace("-", " ").uppercase()}",
                        style = AppTypography.bodySmall,
                        color = when {
                            isSelected -> AppColors.accentGreen
                            enabled -> AppColors.greyLight
                            else -> AppColors.greyDark
                        }
                    )
                    
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = AppColors.accentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
