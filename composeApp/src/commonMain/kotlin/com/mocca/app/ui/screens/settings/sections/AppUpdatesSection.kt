package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.GitHubTokenStatus
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaInput
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.components.modern.ModuleCard
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Settings section: App updates
 * 
 * GitHub auto-update configuration (PAT management, token validation, update checks).
 */
@Composable
fun AppUpdatesSection(
    githubToken: String,
    githubTokenStatus: GitHubTokenStatus?,
    isValidatingToken: Boolean,
    isLoading: Boolean,
    message: String?,
    onSaveToken: (String) -> Unit,
    onValidateToken: () -> Unit,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "APP UPDATES",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "GITHUB AUTO UPDATE") {
            // Token status indicator
            val tokenStatus = githubTokenStatus
            val statusColor = when {
                tokenStatus?.isValid == true -> AppColors.statusOnline
                tokenStatus?.isMissing == true -> AppColors.onSurfaceVariant
                tokenStatus?.isError == true -> AppColors.error
                githubToken.isBlank() -> AppColors.onSurfaceVariant
                else -> AppColors.onSurfaceVariant
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Token Status:",
                    color = AppColors.onSurfaceVariant,
                    style = AppTypography.labelSmall
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = when {
                            isValidatingToken -> "Validating..."
                            tokenStatus?.isValid == true -> "Valid"
                            tokenStatus?.isMissing == true -> "Not Set"
                            tokenStatus?.isError == true -> "Invalid"
                            githubToken.isBlank() -> "Not Set"
                            else -> "Unknown"
                        },
                        color = statusColor,
                        style = AppTypography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            
            Text(
                text = "GitHub Personal Access Token for update checks. Required for private repos and higher rate limits.",
                color = AppColors.onSurfaceVariant,
                style = AppTypography.labelSmall
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            
            // GitHub PAT input
            var tokenInput by remember { mutableStateOf(githubToken) }
            
            MoccaInput(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = "GITHUB PAT",
                placeholder = "ghp_... or github_pat_..."
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                MoccaOutlinedButton(
                    text = "SAVE",
                    onClick = { onSaveToken(tokenInput) },
                    enabled = tokenInput.isNotBlank() && tokenInput != githubToken && !isValidatingToken,
                    modifier = Modifier.weight(1f),
                    height = AppSpacing.buttonHeightCompact
                )
                
                MoccaOutlinedButton(
                    text = "VALIDATE",
                    onClick = onValidateToken,
                    enabled = githubToken.isNotBlank() && !isValidatingToken && !isLoading,
                    modifier = Modifier.weight(1f),
                    height = AppSpacing.buttonHeightCompact
                )
                
                MoccaButton(
                    text = "CHECK UPDATES",
                    onClick = onCheckUpdates,
                    enabled = !isLoading && !isValidatingToken,
                    modifier = Modifier.weight(1.2f),
                    height = AppSpacing.buttonHeightCompact
                )
            }
            
            // Show message if any
            message?.let { msg ->
                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text(
                    text = msg,
                    color = when {
                        msg.contains("failed", ignoreCase = true) || 
                        msg.contains("error", ignoreCase = true) ||
                        msg.contains("invalid", ignoreCase = true) -> AppColors.error
                        msg.contains("valid", ignoreCase = true) ||
                        msg.contains("available", ignoreCase = true) -> AppColors.statusOnline
                        else -> AppColors.onSurfaceVariant
                    },
                    style = AppTypography.labelSmall
                )
            }
            
            // Help text
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(
                text = "Create a token at github.com/settings/tokens (requires 'repo' scope for private repos)",
                color = AppColors.outline,
                style = AppTypography.labelSmall
            )
        }
    }
}
