package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.Mode
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.ModuleCard
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.util.TimeFormatter
import kotlinx.collections.immutable.ImmutableList

/**
 * Settings section: App configuration
 *
 * Displays server-side global defaults (provider, model, modes) with config
 * import status and refresh action. Read-only display backed by the active
 * CLI bridge or legacy HTTP config snapshot.
 */
@Composable
fun AppConfigSection(
    activeConnectionState: ConnectionStatus,
    serverDefaultProvider: String?,
    serverDefaultModel: String?,
    serverModes: ImmutableList<Mode>,
    isSyncingConfig: Boolean,
    configSyncMessage: String,
    configLastSyncedAt: Long?,
    configSyncFailed: Boolean,
    loadedProviderAuthCount: Int,
    canSyncConfig: Boolean,
    onSyncConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "App configuration",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        ModuleCard(title = "Global defaults") {
            val defaultProvider = serverDefaultProvider ?: "Not set"
            val defaultModel = serverDefaultModel ?: "Not set"
            val syncColor = when {
                isSyncingConfig -> AppColors.statusWaiting
                configSyncFailed -> AppColors.error
                configLastSyncedAt != null -> AppColors.statusOnline
                else -> AppColors.onSurfaceVariant
            }
            val syncStatus = when {
                isSyncingConfig -> "Syncing..."
                configSyncFailed -> "Needs attention"
                configLastSyncedAt != null -> "Imported"
                else -> "Idle"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Config import",
                        color = AppColors.outline,
                        style = AppTypography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(syncColor, CircleShape)
                        )
                        Text(
                            text = syncStatus,
                            color = syncColor,
                            style = AppTypography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = configSyncMessage,
                        color = AppColors.onSurfaceVariant,
                        style = AppTypography.labelSmall
                    )
                    configLastSyncedAt?.let {
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text(
                            text = "Last synced ${TimeFormatter.formatTimeAgo(it)} \u2022 ${TimeFormatter.formatTimeWithSeconds(it)}",
                            color = AppColors.outline,
                            style = AppTypography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AppSpacing.md))

                MoccaButton(
                    text = if (isSyncingConfig) "Syncing" else "Refresh Runtime",
                    onClick = onSyncConfig,
                    enabled = canSyncConfig && !isSyncingConfig,
                    height = AppSpacing.buttonHeightCompact
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text(
                text = "Loaded provider auth methods: $loadedProviderAuthCount",
                color = AppColors.outline,
                style = AppTypography.labelSmall
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            // Display current defaults from server
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Default provider",
                        color = AppColors.outline,
                        style = AppTypography.labelSmall
                    )
                    Text(
                        text = defaultProvider.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        color = if (serverDefaultProvider != null) AppColors.statusOnline else AppColors.onSurfaceVariant,
                        style = AppTypography.bodyMedium
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Default model",
                        color = AppColors.outline,
                        style = AppTypography.labelSmall
                    )
                    Text(
                        text = defaultModel,
                        color = if (serverDefaultModel != null) AppColors.statusOnline else AppColors.onSurfaceVariant,
                        style = AppTypography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Show available modes from server
            if (serverModes.isNotEmpty()) {
                Text(
                    text = "Available modes",
                    color = AppColors.outline,
                    style = AppTypography.labelSmall
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = serverModes.joinToString(", ") { it.name },
                    color = AppColors.onSurfaceVariant,
                    style = AppTypography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Info note about server-side configuration
            Text(
                text = "This configuration is read from your local OpenCode setup through MOCCA CLI.",
                color = AppColors.outline,
                style = AppTypography.labelSmall
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "Refresh after editing local opencode.json/jsonc or switching the active CLI project.",
                color = AppColors.outline,
                style = AppTypography.labelSmall
            )
            if (!canSyncConfig) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = if (activeConnectionState.isConnected) {
                        "Config sync is available."
                    } else {
                        "Connect to the active server above to sync the latest config snapshot. This action only refreshes existing server-backed config and does not change local display preferences."
                    },
                    color = AppColors.outline,
                    style = AppTypography.labelSmall
                )
            }
        }
    }
}
