package com.mocca.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.*
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.screens.settings.sections.AppConfigSection
import com.mocca.app.ui.screens.settings.sections.AppUpdatesSection
import com.mocca.app.ui.screens.settings.sections.AppearanceSection
import com.mocca.app.ui.screens.settings.sections.ExperimentalSection
import com.mocca.app.ui.screens.settings.sections.NotificationsSection
import com.mocca.app.ui.screens.settings.sections.PrivacySecuritySection
import com.mocca.app.ui.screens.settings.sections.ProjectSection
import com.mocca.app.ui.screens.settings.sections.ProviderAuthSection
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.moccaClickable
import com.mocca.app.ui.TestTags

class SettingsScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<SettingsScreenModel>()
        val state by screenModel.state.collectAsState()
        val uriHandler = LocalUriHandler.current
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag(TestTags.Settings.screen)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surfaceContainer, AppShapes.none)
                    .padding(horizontal = AppSpacing.screenPaddingHorizontal, vertical = AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoccaIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { navigator.pop() },
                    iconColor = AppColors.onSurfaceVariant,
                    contentDescription = "Back",
                    modifier = Modifier.testTag(TestTags.Settings.backButton)
                )
                Spacer(modifier = Modifier.width(AppSpacing.md))
                Text(
                    text = "Settings",
                    color = AppColors.primary,
                    style = AppTypography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenPaddingHorizontal),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap),
                contentPadding = PaddingValues(top = AppSpacing.lg, bottom = AppSpacing.screenPaddingBottom)
            ) {
                item {
                    Box(
                        modifier = Modifier.animateItem(
                            fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                    ) {
                        CliConnectionSection(
                            uiState = state.cliConnectionUi,
                            aiConfigState = state.aiConfigState,
                            onRefreshRuntime = { screenModel.syncServerConfig() },
                            onReconnect = { screenModel.reconnectCliBridge() },
                            onDisconnect = { screenModel.disconnectCliBridge() },
                            onForget = { screenModel.forgetCliBridgeTarget() }
                        )
                    }
                }

                
                // Provider Authentication Section
                item {
                    Box(
                        modifier = Modifier.animateItem(
                            fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                    ) {
                        ProviderAuthSection(
                            providerAuthMethods = state.providerAuthMethods,
                            selectedProviderId = state.selectedProviderId,
                            authLoading = state.authLoading,
                            onLoadAuthMethods = { screenModel.loadAuthMethods(it) },
                            onStartOAuth = { providerId, openUrl -> screenModel.startOAuth(providerId, openUrl) },
                            onSaveManualKey = { providerId, key -> screenModel.setManualKey(providerId, key) },
                            onRemoveAuth = { screenModel.removeProviderAuth(it) },
                            openUrl = { uriHandler.openUri(it) }
                        )
                    }
                }
                
                // App Configuration Section
                item {
                    Box(
                        modifier = Modifier.animateItem(
                            fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                    ) {
                        AppConfigSection(
                            activeConnectionState = state.activeConnectionState,
                            serverDefaultProvider = state.serverDefaultProvider,
                            serverDefaultModel = state.serverDefaultModel,
                            serverModes = state.serverModes,
                            isSyncingConfig = state.isSyncingConfig,
                            configSyncMessage = state.configSyncMessage,
                            configLastSyncedAt = state.configLastSyncedAt,
                            configSyncFailed = state.configSyncFailed,
                            loadedProviderAuthCount = state.providerAuthMethods.size,
                            canSyncConfig = state.activeConnectionState.isConnected,
                            onSyncConfig = { screenModel.syncServerConfig() }
                        )
                    }
                }
                
                // Project Section
                state.currentProject?.let { project ->
                    item {
                        Box(
                            modifier = Modifier.animateItem(
                                fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                            )
                        ) {
                            ProjectSection(
                                currentProject = project,
                                editingProjectPath = state.editingProjectPath,
                                onSetEditingProjectPath = { screenModel.setEditingProjectPath(it) },
                                onSaveProjectPath = { screenModel.saveProjectPath() }
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier.animateItem(
                                fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                            )
                        ) {
                            Text(
                                text = "The active MOCCA CLI project controls native files, git, terminal and AI runtime state.",
                                color = AppColors.outline,
                                style = AppTypography.labelSmall
                            )
                        }
                    }
                }
                
                
                // Appearance Section
                item {
                    Box(
                        modifier = Modifier.animateItem(
                            fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                    ) {
                        AppearanceSection(
                            preferences = state.preferences,
                            onSetShowTokenCounts = { screenModel.setShowTokenCounts(it) },
                            onSetShowTimestamps = { screenModel.setShowTimestamps(it) },

                            onSetCodeFontFamily = { screenModel.setCodeFontFamily(it) }
                        )
                    }
                }
                

                
                // Notifications Section
                item {
                    Box(
                        modifier = Modifier.animateItem(
                            fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                    ) {
                        NotificationsSection(
                            preferences = state.preferences,
                            onSetNotifyPermissions = { screenModel.setNotifyPermissions(it) },
                            onSetNotifySessionComplete = { screenModel.setNotifySessionComplete(it) },
                            onSetNotifyConnectionLost = { screenModel.setNotifyConnectionLost(it) }
                        )
                    }
                }
                
                // Privacy & Security Section
                item {
                    Box(
                        modifier = Modifier.animateItem(
                            fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                    ) {
                        PrivacySecuritySection(
                            onShowClearCacheDialog = { screenModel.showClearCacheDialog() },
                            onResetPreferences = { screenModel.resetPreferencesToDefaults() }
                        )
                    }
                }
                
                // App Updates Section
                item {
                    Box(
                        modifier = Modifier.animateItem(
                            fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                    ) {
                        AppUpdatesSection(
                            githubToken = state.githubToken,
                            githubTokenStatus = state.githubTokenStatus,
                            isValidatingToken = state.isValidatingToken,
                            isLoading = state.isLoading,
                            message = state.message,
                            onSaveToken = { screenModel.saveGitHubToken(it) },
                            onValidateToken = { screenModel.validateGitHubToken() },
                            onCheckUpdates = { screenModel.checkForUpdates() }
                        )
                    }
                }
                
                // Experimental Section
                item {
                    Box(
                        modifier = Modifier.animateItem(
                            fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                    ) {
                        ExperimentalSection(navigator = navigator)
                    }
                }
            }
        
            // Clear Cache Confirmation Dialog
            if (state.showClearCacheDialog) {
                AlertDialog(
                    onDismissRequest = { screenModel.hideClearCacheDialog() },
                    containerColor = AppColors.surfaceContainerHigh,
                    shape = AppShapes.dialog,
                    modifier = Modifier.testTag(TestTags.Settings.clearCacheDialog),
                    title = {
                        Text(
                            text = "Clear All Cache",
                            color = AppColors.primary,
                            style = AppTypography.headlineSmall
                        )
                    },
                    text = {
                        Text(
                            text = "This will remove all cached sessions, messages, and local data. You will need to re-download data from the server.\n\nThis action cannot be undone.",
                            color = AppColors.onSurfaceVariant,
                            style = AppTypography.bodyMedium
                        )
                    },
                    confirmButton = {
                        MoccaButton(
                            text = "Clear",
                            onClick = { screenModel.confirmClearCache() },
                            height = AppSpacing.buttonHeightCompact,
                            modifier = Modifier.testTag(TestTags.Settings.clearCacheConfirmButton)
                        )
                    },
                    dismissButton = {
                        MoccaTextButton(
                            text = "Cancel",
                            onClick = { screenModel.hideClearCacheDialog() },
                            modifier = Modifier.testTag(TestTags.Settings.clearCacheCancelButton)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun CliConnectionSection(
    uiState: CliConnectionUiState,
    aiConfigState: AiConfigState,
    onRefreshRuntime: () -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.testTag(TestTags.Settings.cliConnectionSection)) {
        Text(
            text = "MOCCA CLI",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        SettingsCard(title = uiState.headline) {
            Text(
                text = uiState.statusLabel,
                color = when {
                    uiState.statusLabel.contains("Connected", ignoreCase = true) -> AppColors.statusOnline
                    uiState.statusLabel.contains("Connecting", ignoreCase = true) -> AppColors.statusWaiting
                    uiState.statusLabel.contains("failed", ignoreCase = true) -> AppColors.error
                    else -> AppColors.onSurfaceVariant
                },
                style = AppTypography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = uiState.supportingText,
                color = AppColors.onSurfaceVariant,
                style = AppTypography.bodySmall
            )

            uiState.endpointLabel?.let { SettingsRowItem(title = "Endpoint", subtitle = it, showToggle = false) }
            uiState.networkLabel?.let { SettingsRowItem(title = "Network", subtitle = it, showToggle = false) }
            uiState.projectLabel?.let { SettingsRowItem(title = "Project", subtitle = it, showToggle = false) }
            if (uiState.capabilitySummary.isNotBlank()) {
                SettingsRowItem(title = "Capabilities", subtitle = uiState.capabilitySummary, showToggle = false)
            }
            aiConfigState.errorMessage?.let {
                SettingsRowItem(title = "Runtime", subtitle = it, showToggle = false)
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                MoccaButton(
                    text = "Refresh",
                    onClick = onRefreshRuntime,
                    modifier = Modifier.weight(1f).testTag(TestTags.Settings.refreshButton),
                    height = AppSpacing.buttonHeightCompact
                )
                if (uiState.canReconnect) {
                    MoccaOutlinedButton(
                        text = "Reconnect",
                        onClick = onReconnect,
                        modifier = Modifier.weight(1f).testTag(TestTags.Settings.reconnectButton)
                    )
                }
            }

            if (uiState.canForget) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    MoccaOutlinedButton(
                        text = "Disconnect",
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f).testTag(TestTags.Settings.disconnectButton)
                    )
                    MoccaOutlinedButton(
                        text = "Forget",
                        onClick = onForget,
                        modifier = Modifier.weight(1f).testTag(TestTags.Settings.forgetButton)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    actionButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.moduleCard)
            .background(AppColors.bgRaised, AppShapes.moduleCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacing.cardPadding,
                    vertical = AppSpacing.md
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "{ }",
                        color = AppColors.primary,
                        style = AppTypography.bodyMedium
                    )
                }
                Text(
                    text = title,
                    color = AppColors.primary,
                    style = AppTypography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (actionButton != null) {
                actionButton()
            }
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.cardPadding)
                .padding(bottom = AppSpacing.cardPadding),
            content = content
        )
    }
}

@Composable
fun SettingsRowItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isConnected: Boolean = true,
    isTransitioning: Boolean = false,
    showToggle: Boolean = true,
    onToggle: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isStrikethrough: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .moccaClickable(
                onClick = onClick,
                pressedScale = 0.99f,
                rippleColor = AppColors.white.copy(alpha = 0.1f)
            )
            .padding(vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isEnabled || !isConnected) {
            StatusDot(
                color = when {
                    !isEnabled -> AppColors.outline
                    else -> AppColors.statusOffline
                },
                size = AppSpacing.statusDotSizeLarge
            )
            Spacer(modifier = Modifier.width(AppSpacing.md))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isEnabled) AppColors.onSurface else AppColors.onSurfaceVariant,
                style = AppTypography.bodyMedium.copy(
                    textDecoration = if (isStrikethrough) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                ),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = AppColors.outline,
                style = AppTypography.bodySmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        if (showToggle && onToggle != null) {
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                enabled = !isTransitioning,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = AppColors.background,
                    checkedTrackColor = AppColors.primary,
                    checkedBorderColor = AppColors.primary,
                    uncheckedThumbColor = AppColors.onSurface,
                    uncheckedTrackColor = AppColors.surfaceVariant,
                    uncheckedBorderColor = AppColors.outline,
                    disabledCheckedThumbColor = AppColors.onSurfaceVariant,
                    disabledCheckedTrackColor = AppColors.surfaceVariant,
                    disabledUncheckedThumbColor = AppColors.onSurfaceVariant,
                    disabledUncheckedTrackColor = AppColors.surfaceVariant
                )
            )
        }
    }
}
