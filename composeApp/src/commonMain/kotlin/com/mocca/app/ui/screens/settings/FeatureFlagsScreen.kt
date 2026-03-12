package com.mocca.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.modern.MoccaIconButton
import com.mocca.app.ui.components.modern.ModernHeader
import com.mocca.app.ui.components.modern.ModuleCard
import com.mocca.app.ui.components.modern.ModuleRowItem
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

// ═══════════════════════════════════════════════════════════════════════
// SCREEN
// ═══════════════════════════════════════════════════════════════════════

object FeatureFlagsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<FeatureFlagsScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .padding(AppSpacing.lg)
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoccaIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { navigator.pop() },
                    iconColor = AppColors.textPrimary
                )
                Spacer(modifier = Modifier.width(AppSpacing.md))
                ModernHeader(text = "FEATURE FLAGS", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // ── Error Banner ───────────────────────────────────────────────────
            val error = uiState.error ?: uiState.saveError
            if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.medium)
                        .background(AppColors.error.copy(alpha = 0.12f))
                        .border(AppSpacing.borderThin, AppColors.error, AppShapes.medium)
                        .padding(AppSpacing.md)
                ) {
                    Text(
                        text = error,
                        color = AppColors.error,
                        style = AppTypography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.md))
            }

            // ── Success Banner ─────────────────────────────────────────────────
            if (uiState.successMessage != null) {
                LaunchedEffect(uiState.successMessage) {
                    kotlinx.coroutines.delay(2000)
                    screenModel.clearMessages()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.medium)
                        .background(AppColors.accentGreen.copy(alpha = 0.12f))
                        .border(AppSpacing.borderThin, AppColors.accentGreen, AppShapes.medium)
                        .padding(AppSpacing.md)
                ) {
                    Text(
                        text = uiState.successMessage!!,
                        color = AppColors.accentGreen,
                        style = AppTypography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.md))
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(color = AppColors.accentGreen)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    contentPadding = PaddingValues(bottom = AppSpacing.xl)
                ) {
                    // ── Global Config Toggles ──────────────────────────────────
                    item {
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        Text(
                            text = "GLOBAL SETTINGS",
                            color = AppColors.textSecondary,
                            style = AppTypography.labelSmall
                        )
                    }

                    item {
                        ModuleCard(title = "APP BEHAVIOR") {
                            val config = uiState.config

                            // Autoshare
                            ModuleRowItem(
                                title = "AUTO SHARE",
                                subtitle = "Automatically share sessions with collaborators",
                                isEnabled = config?.autoshare ?: false,
                                onToggle = { screenModel.setAutoshare(it) }
                            )

                            HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)

                            // Autoupdate
                            ModuleRowItem(
                                title = "AUTO UPDATE",
                                subtitle = "Automatically update OpenCode when new versions are available",
                                isEnabled = config?.autoupdate ?: false,
                                onToggle = { screenModel.setAutoupdate(it) }
                            )

                            HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)

                            // Telemetry
                            ModuleRowItem(
                                title = "TELEMETRY",
                                subtitle = "Send anonymous usage data to improve OpenCode",
                                isEnabled = config?.telemetry ?: false,
                                onToggle = { screenModel.setTelemetry(it) }
                            )
                        }
                    }

                    // ── Experimental Flags (read-only from server) ─────────────
                    item {
                        Spacer(modifier = Modifier.height(AppSpacing.md))
                        Text(
                            text = "EXPERIMENTAL FLAGS",
                            color = AppColors.textSecondary,
                            style = AppTypography.labelSmall
                        )
                    }

                    item {
                        ModuleCard(title = "SERVER-SIDE FLAGS") {
                            Text(
                                text = "Experimental flags are configured server-side in the OpenCode config file. Enable them by setting experimental keys in your opencode.json.",
                                style = AppTypography.bodySmall,
                                color = AppColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.md))

                            val expFlags = uiState.config?.experimental ?: emptyMap()

                            if (expFlags.isEmpty()) {
                                ExperimentalFlagRow(
                                    key = "worktrees",
                                    label = "WORKTREES",
                                    description = "Git worktree management",
                                    enabled = false
                                )
                                HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                                ExperimentalFlagRow(
                                    key = "plan",
                                    label = "PLAN MODE",
                                    description = "Agent planning mode before execution",
                                    enabled = false
                                )
                                HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                                ExperimentalFlagRow(
                                    key = "exa",
                                    label = "EXA SEARCH",
                                    description = "Exa neural web search integration",
                                    enabled = false
                                )
                                HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                                ExperimentalFlagRow(
                                    key = "lsp",
                                    label = "LSP INTEGRATION",
                                    description = "Language Server Protocol diagnostics",
                                    enabled = false
                                )
                                HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                                ExperimentalFlagRow(
                                    key = "disableTerminals",
                                    label = "DISABLE TERMINALS",
                                    description = "Hide terminal sessions from UI",
                                    enabled = false
                                )
                                HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                                ExperimentalFlagRow(
                                    key = "disableFileBrowser",
                                    label = "DISABLE FILE BROWSER",
                                    description = "Hide file browser from UI",
                                    enabled = false
                                )
                            } else {
                                val knownFlags = mapOf(
                                    "worktrees" to Pair("WORKTREES", "Git worktree management"),
                                    "plan" to Pair("PLAN MODE", "Agent planning mode before execution"),
                                    "exa" to Pair("EXA SEARCH", "Exa neural web search integration"),
                                    "lsp" to Pair("LSP INTEGRATION", "Language Server Protocol diagnostics"),
                                    "disableTerminals" to Pair("DISABLE TERMINALS", "Hide terminal sessions from UI"),
                                    "disableFileBrowser" to Pair("DISABLE FILE BROWSER", "Hide file browser from UI")
                                )

                                val sortedFlags = expFlags.entries.toList()
                                sortedFlags.forEachIndexed { index, (key, value) ->
                                    val (label, description) = knownFlags[key]
                                        ?: Pair(key.uppercase(), "Experimental server flag")
                                    ExperimentalFlagRow(
                                        key = key,
                                        label = label,
                                        description = description,
                                        enabled = value
                                    )
                                    if (index < sortedFlags.lastIndex) {
                                        HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }

                    // ── Saving indicator ──────────────────────────────────────
                    if (uiState.isSaving) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                                ) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = AppColors.accentGreen
                                    )
                                    Text(
                                        text = "Saving...",
                                        style = AppTypography.labelSmall,
                                        color = AppColors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ExperimentalFlagRow(
    key: String,
    label: String,
    description: String,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Text(
                    text = label,
                    style = AppTypography.labelMedium,
                    color = AppColors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                // Read-only badge
                Box(
                    modifier = Modifier
                        .clip(AppShapes.pill)
                        .background(AppColors.border)
                        .padding(horizontal = AppSpacing.xs, vertical = 2.dp)
                ) {
                    Text(
                        text = "SERVER",
                        style = AppTypography.labelSmall,
                        color = AppColors.textTertiary
                    )
                }
            }
            Text(
                text = description,
                style = AppTypography.bodySmall,
                color = AppColors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        // Status indicator (read-only)
        Box(
            modifier = Modifier
                .clip(AppShapes.pill)
                .background(
                    if (enabled) AppColors.accentGreen.copy(alpha = 0.15f)
                    else AppColors.border
                )
                .border(
                    AppSpacing.borderThin,
                    if (enabled) AppColors.accentGreen.copy(alpha = 0.5f)
                    else AppColors.borderLight,
                    AppShapes.pill
                )
                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
        ) {
            Text(
                text = if (enabled) "ON" else "OFF",
                style = AppTypography.labelSmall,
                color = if (enabled) AppColors.accentGreen else AppColors.textTertiary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
