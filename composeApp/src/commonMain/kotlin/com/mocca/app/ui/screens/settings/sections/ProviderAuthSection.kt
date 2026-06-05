package com.mocca.app.ui.screens.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mocca.app.domain.model.ProviderAuthMethod
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.collections.immutable.ImmutableList

/**
 * Settings section: Server-side provider credentials
 *
 * Expandable provider cards for managing LLM provider API keys via OpenCode.
 * Credentials are sent to the connected OpenCode server — they are not stored locally.
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
            text = "Server-side provider auth",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = "Provider API keys are managed by OpenCode on your server. Adding credentials here sends them to the connected OpenCode server for LLM provider access.",
            color = AppColors.outline,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "Configure providers") {
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
                        text = providerId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        color = AppColors.onSurface,
                        style = AppTypography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isExpanded) "[-]" else "[+]",
                        color = AppColors.outline,
                        style = AppTypography.labelSmall
                    )
                }
                
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                    ) + fadeIn(
                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                    ) + fadeOut(
                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
                    )
                ) {
                    Column(modifier = Modifier.padding(start = AppSpacing.md, bottom = AppSpacing.md)) {
                        val methods = providerAuthMethods[providerId]

                        if (authLoading && selectedProviderId == providerId) {
                            Text("Loading auth methods...", color = AppColors.outline, style = AppTypography.labelSmall)
                        } else {
                            // OAuth Button
                            if (methods?.any { it.type == "oauth" } == true) {
                                MoccaButton(
                                    text = "Connect (OAuth)",
                                    onClick = {
                                        onStartOAuth(providerId) { url ->
                                            openUrl(url)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    height = AppSpacing.buttonHeightCompact
                                )
                                Spacer(modifier = Modifier.height(AppSpacing.sm))
                                Text("— or —", color = AppColors.outline, style = AppTypography.labelSmall)
                                Spacer(modifier = Modifier.height(AppSpacing.sm))
                            }

                            // Manual Key Input
                            MoccaInput(
                                value = manualKey,
                                onValueChange = { manualKey = it },
                                placeholder = "API key",
                                label = null
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                            MoccaCompactButton(
                                text = "Save key",
                                onClick = { onSaveManualKey(providerId, manualKey) },
                                enabled = manualKey.isNotBlank(),
                                height = AppSpacing.buttonHeightSmall
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                            MoccaCompactButton(
                                text = "Remove auth",
                                onClick = { onRemoveAuth(providerId) },
                                height = AppSpacing.buttonHeightSmall,
                                backgroundColor = AppColors.error.copy(alpha = 0.15f),
                                textColor = AppColors.error
                            )
                        }
                    }
                }
                
                if (index < commonProviders.size - 1) {
                    HorizontalDivider(color = AppColors.outline, thickness = AppSpacing.borderThin)
                }
            }
        }
    }
}
