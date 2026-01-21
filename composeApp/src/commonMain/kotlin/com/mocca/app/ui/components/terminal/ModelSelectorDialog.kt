package com.mocca.app.ui.components.terminal

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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mocca.app.domain.model.ProviderInfo
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.domain.model.RecentModel
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography
import kotlinx.serialization.json.JsonObject

/**
 * Terminal-styled model selection dialog.
 * Shows available providers and their models for selection.
 */
@Composable
fun ModelSelectorDialog(
    providerResponse: ProviderResponse,
    selectedProviderId: String,
    selectedModelId: String,
    onModelSelected: (providerId: String, modelId: String) -> Unit,
    recentModels: List<RecentModel> = emptyList(),
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .background(TerminalColors.background, RectangleShape)
                .border(TerminalSpacing.borderStandard, TerminalColors.borderLight, RectangleShape)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalColors.surface)
                    .padding(TerminalSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "// SELECT MODEL",
                    style = TerminalTypography.titleMedium,
                    color = TerminalColors.white,
                    fontWeight = FontWeight.Bold
                )
                TerminalIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    iconColor = TerminalColors.grey
                )
            }
            
            HorizontalDivider(
                thickness = TerminalSpacing.borderThin,
                color = TerminalColors.border
            )
            
            // Search bar
            var searchQuery by remember { mutableStateOf("") }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalColors.surface)
                    .padding(horizontal = TerminalSpacing.md, vertical = TerminalSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> ",
                    style = TerminalTypography.bodySmall,
                    color = TerminalColors.accentGreen
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TerminalTypography.bodySmall.copy(color = TerminalColors.white),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = TerminalSpacing.xs),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search models...",
                                    style = TerminalTypography.bodySmall,
                                    color = TerminalColors.greyDark
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    TerminalIconButton(
                        icon = Icons.Default.Close,
                        onClick = { searchQuery = "" },
                        iconColor = TerminalColors.grey,
                        size = 16.dp
                    )
                }
            }
            
            HorizontalDivider(
                thickness = TerminalSpacing.borderThin,
                color = TerminalColors.border
            )
            
            // Provider/Model list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(TerminalSpacing.sm)
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
                           style = TerminalTypography.labelSmall,
                           color = TerminalColors.accentGreen,
                           modifier = Modifier.padding(
                               start = TerminalSpacing.sm,
                               top = TerminalSpacing.sm,
                               bottom = TerminalSpacing.xs
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
                                           TerminalColors.accentGreen.copy(alpha = 0.2f) 
                                       else 
                                           TerminalColors.background
                                   )
                                   .padding(
                                       horizontal = TerminalSpacing.md,
                                       vertical = TerminalSpacing.xs
                                   ),
                               horizontalArrangement = Arrangement.SpaceBetween,
                               verticalAlignment = Alignment.CenterVertically
                           ) {
                               Text(
                                   text = "> ${recent.modelId.uppercase()} [${recent.providerId.uppercase()}]",
                                   style = TerminalTypography.bodySmall,
                                   color = if (isSelected) TerminalColors.accentGreen else TerminalColors.white
                               )
                           }
                       }
                   }
                   
                   item {
                       HorizontalDivider(
                           thickness = TerminalSpacing.borderThin,
                           color = TerminalColors.border,
                           modifier = Modifier.padding(vertical = TerminalSpacing.sm)
                       )
                   }
                }

                if (filteredProviders.isNotEmpty()) {
                    item {
                        Text(
                            text = "// CONNECTED",
                            style = TerminalTypography.labelSmall,
                            color = TerminalColors.accentGreen,
                            modifier = Modifier.padding(
                                start = TerminalSpacing.sm,
                                top = TerminalSpacing.sm,
                                bottom = TerminalSpacing.xs
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
                            style = TerminalTypography.labelSmall,
                            color = TerminalColors.grey,
                            modifier = Modifier.padding(TerminalSpacing.md)
                        )
                    }
                } else {
                    // No authenticated providers
                    item {
                        Text(
                            text = "// NO PROVIDERS CONFIGURED",
                            style = TerminalTypography.labelSmall,
                            color = TerminalColors.grey,
                            modifier = Modifier.padding(TerminalSpacing.md)
                        )
                        Text(
                            text = "Configure providers in opencode settings.",
                            style = TerminalTypography.bodySmall,
                            color = TerminalColors.greyDark,
                            modifier = Modifier.padding(
                                start = TerminalSpacing.md,
                                top = TerminalSpacing.xs
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
            .padding(vertical = TerminalSpacing.xs)
    ) {
        // Provider header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { manualExpanded = !manualExpanded }
                .background(
                    if (provider.id == selectedProviderId) 
                        TerminalColors.surface 
                    else 
                        TerminalColors.background
                )
                .padding(TerminalSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (expanded) "[-]" else "[+]",
                    style = TerminalTypography.bodySmall,
                    color = TerminalColors.grey
                )
                Spacer(modifier = Modifier.width(TerminalSpacing.sm))
                Text(
                    text = provider.name.uppercase(),
                    style = TerminalTypography.bodyMedium,
                    color = if (enabled) TerminalColors.white else TerminalColors.greyDark,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "${provider.modelCount} models",
                style = TerminalTypography.labelSmall,
                color = TerminalColors.grey
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
                                TerminalColors.accentGreen.copy(alpha = 0.2f) 
                            else 
                                TerminalColors.background
                        )
                        .padding(
                            start = TerminalSpacing.xl,
                            end = TerminalSpacing.md,
                            top = TerminalSpacing.xs,
                            bottom = TerminalSpacing.xs
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "> ${modelId.replace("-", " ").uppercase()}",
                        style = TerminalTypography.bodySmall,
                        color = when {
                            isSelected -> TerminalColors.accentGreen
                            enabled -> TerminalColors.greyLight
                            else -> TerminalColors.greyDark
                        }
                    )
                    
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = TerminalColors.accentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
