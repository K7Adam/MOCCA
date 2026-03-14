package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mocca.app.domain.model.ProviderAuthMethod
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.collections.immutable.ImmutableList

/**
 * Settings section: Provider authentication
 * 
 * Expandable provider cards for configuring OAuth or manual API keys
 * for common providers (anthropic, openai, github).
 */
@Composable
fun ProviderAuthSection(
    providerAuthMethods: Map<String, ImmutableList<ProviderAuthMethod>>,
    selectedProviderId: String?,
    authLoading: Boolean,
    onLoadAuthMethods: (String) -> Unit,
    onStartOAuth: (String, (String) -> Unit) -> Unit,
    onSaveManualKey: (String, String) -> Unit,
    onRemoveAuth: (String) -> Unit,
    openUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commonProviders = listOf("anthropic", "openai", "github")
    
    Column(modifier = modifier) {
        Text(
            text = "PROVIDER AUTHENTICATION",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "CONFIGURE PROVIDERS") {
            commonProviders.forEachIndexed { index, providerId ->
                var isExpanded by remember { mutableStateOf(false) }
                var manualKey by remember { mutableStateOf("") }
                
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            isExpanded = !isExpanded
                            if (isExpanded) {
                                onLoadAuthMethods(providerId)
                            }
                        }
                        .padding(vertical = AppSpacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = providerId.uppercase(),
                        color = AppColors.textPrimary,
                        style = AppTypography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isExpanded) "[-]" else "[+]",
                        color = AppColors.textTertiary,
                        style = AppTypography.labelSmall
                    )
                }
                
                if (isExpanded) {
                    Column(modifier = Modifier.padding(start = AppSpacing.md, bottom = AppSpacing.md)) {
                        val methods = providerAuthMethods[providerId]
                        
                        if (authLoading && selectedProviderId == providerId) {
                            Text("Loading auth methods...", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                        } else {
                            // OAuth Button
                            if (methods?.any { it.type == "oauth" } == true) {
                                MoccaButton(
                                    text = "CONNECT (OAUTH)",
                                    onClick = { 
                                        onStartOAuth(providerId) { url ->
                                            openUrl(url)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    height = AppSpacing.buttonHeightCompact
                                )
                                Spacer(modifier = Modifier.height(AppSpacing.sm))
                                Text("- OR -", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                                Spacer(modifier = Modifier.height(AppSpacing.sm))
                            }
                            
                            // Manual Key Input
                            MoccaInput(
                                value = manualKey,
                                onValueChange = { manualKey = it },
                                placeholder = "API KEY",
                                label = null
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                            MoccaCompactButton(
                                text = "SAVE KEY",
                                onClick = { onSaveManualKey(providerId, manualKey) },
                                enabled = manualKey.isNotBlank(),
                                height = AppSpacing.buttonHeightSmall
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                            MoccaCompactButton(
                                text = "REMOVE AUTH",
                                onClick = { onRemoveAuth(providerId) },
                                height = AppSpacing.buttonHeightSmall,
                                backgroundColor = AppColors.error.copy(alpha = 0.15f),
                                textColor = AppColors.error
                            )
                        }
                    }
                }
                
                if (index < commonProviders.size - 1) {
                    HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
                }
            }
        }
    }
}
