package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mocca.app.domain.model.Mode
import com.mocca.app.ui.components.modern.ModuleCard
import com.mocca.app.ui.components.modern.ModuleRowItem
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.collections.immutable.ImmutableList

/**
 * Settings section: App configuration
 * 
 * Displays server-side global defaults (provider, model, modes).
 * Read-only display with hint to configure via /config command.
 */
@Composable
fun AppConfigSection(
    serverDefaultProvider: String?,
    serverDefaultModel: String?,
    serverModes: ImmutableList<Mode>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "APP CONFIGURATION",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "GLOBAL DEFAULTS") {
            val defaultProvider = serverDefaultProvider ?: "Not set"
            val defaultModel = serverDefaultModel ?: "Not set"

            // Display current defaults from server
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DEFAULT PROVIDER",
                        color = AppColors.textTertiary,
                        style = AppTypography.labelSmall
                    )
                    Text(
                        text = defaultProvider.uppercase(),
                        color = if (serverDefaultProvider != null) AppColors.statusOnline else AppColors.textSecondary,
                        style = AppTypography.bodyMedium
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DEFAULT MODEL",
                        color = AppColors.textTertiary,
                        style = AppTypography.labelSmall
                    )
                    Text(
                        text = defaultModel,
                        color = if (serverDefaultModel != null) AppColors.statusOnline else AppColors.textSecondary,
                        style = AppTypography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Show available modes from server
            if (serverModes.isNotEmpty()) {
                Text(
                    text = "AVAILABLE MODES",
                    color = AppColors.textTertiary,
                    style = AppTypography.labelSmall
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = serverModes.joinToString(", ") { it.name },
                    color = AppColors.textSecondary,
                    style = AppTypography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Info note about server-side configuration
            Text(
                text = "Provider and model are configured on the OpenCode server.",
                color = AppColors.textTertiary,
                style = AppTypography.labelSmall
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "Update these settings via /config command in OpenCode.",
                color = AppColors.textTertiary,
                style = AppTypography.labelSmall
            )
        }
    }
}
