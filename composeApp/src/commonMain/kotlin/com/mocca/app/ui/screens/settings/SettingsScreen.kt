@file:Suppress("DEPRECATION")

package com.mocca.app.ui.screens.settings

import com.mocca.app.api.NetworkConfig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.mocca.app.domain.model.*
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.screens.settings.sections.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

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
        ) {
            // Header
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
                    iconColor = AppColors.textSecondary
                )
                Spacer(modifier = Modifier.width(AppSpacing.md))
                Text(
                    text = "SETTINGS",
                    color = AppColors.white,
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
                // Servers Section
                item {
                    ServersSection(
                        servers = state.servers,
                        activeServerId = state.activeServerId,
                        connectionStatuses = state.connectionStatuses,
                        onActivate = { screenModel.setActiveServer(it) },
                        onEdit = { screenModel.editServer(it) },
                        onDelete = { screenModel.deleteServer(it) },
                        onCheckConnection = { screenModel.checkServerConnection(it) },
                        onAddNewServer = { screenModel.addNewServer() }
                    )
                }
                
                // Provider Authentication Section
                item {
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
                
                // App Configuration Section
                item {
                    AppConfigSection(
                        serverDefaultProvider = state.serverDefaultProvider,
                        serverDefaultModel = state.serverDefaultModel,
                        serverModes = state.serverModes
                    )
                }
                
                // Project Section
                state.currentProject?.let { project ->
                    item {
                        ProjectSection(
                            currentProject = project,
                            editingProjectPath = state.editingProjectPath,
                            onSetEditingProjectPath = { screenModel.setEditingProjectPath(it) },
                            onSaveProjectPath = { screenModel.saveProjectPath() }
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
                    ) }
                }
                
                // Server Info Section
                state.serverVersion?.let { version ->
                    item {
                        SettingsCard(title = "OPENCODE SERVER INFO") { SettingsRowItem(title = "SERVER VERSION",
                        subtitle = version,
                        isEnabled = true,
                        showToggle = false) }
                    }
                }
                
                // Appearance Section
                item {
                    AppearanceSection(
                        preferences = state.preferences,
                        onSetShowTokenCounts = { screenModel.setShowTokenCounts(it) },
                        onSetShowTimestamps = { screenModel.setShowTimestamps(it) },
                        onSetCompactMode = { screenModel.setCompactMode(it) },
                        onSetHideApiKeys = { screenModel.setHideApiKeys(it) },
                        onSetFontScale = { screenModel.setFontScale(it) }
                    )
                }
                
                // Chat Section
                item {
                    ChatSection(
                        preferences = state.preferences,
                        onSetAutoScroll = { screenModel.setAutoScroll(it) },
                        onSetConfirmDelete = { screenModel.setConfirmDelete(it) },
                        onSetShowThinkingBlocks = { screenModel.setShowThinkingBlocks(it) }
                    )
                }
                
                // Notifications Section
                item {
                    NotificationsSection(
                        preferences = state.preferences,
                        onSetNotifyPermissions = { screenModel.setNotifyPermissions(it) },
                        onSetNotifySessionComplete = { screenModel.setNotifySessionComplete(it) },
                        onSetNotifyConnectionLost = { screenModel.setNotifyConnectionLost(it) }
                    )
                }
                
                // Connection Section
                item {
                    ConnectionSection(
                        preferences = state.preferences,
                        onSetAutoReconnect = { screenModel.setAutoReconnect(it) },
                        onSetDataSaverMode = { screenModel.setDataSaverMode(it) }
                    )
                }
                
                // Privacy & Security Section
                item {
                    PrivacySecuritySection(
                        preferences = state.preferences,
                        onSetScreenSecurity = { screenModel.setScreenSecurity(it) },
                        onSetClearCacheOnExit = { screenModel.setClearCacheOnExit(it) },
                        onShowClearCacheDialog = { screenModel.showClearCacheDialog() },
                        onResetPreferences = { screenModel.resetPreferencesToDefaults() }
                    )
                }
                
                // App Updates Section
                item {
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
                
                // Skills Section
                item {
                    SkillsSection(navigator = navigator)
                }
                
                // Experimental Section
                item {
                    ExperimentalSection(navigator = navigator)
                }
            }
        
            // Edit Server Dialog (Overlay)
            state.editingServer?.let { server ->
                val isNewServer = state.servers.none { it.id == server.id }
                TerminalServerEditDialog(
                    server = server,
                    isNewServer = isNewServer,
                    onSave = { screenModel.saveServer(it, isNewServer) },
                    onDismiss = { screenModel.cancelEdit() }
                )
            }
            
            // Clear Cache Confirmation Dialog
            if (state.showClearCacheDialog) {
                AlertDialog(
                    onDismissRequest = { screenModel.hideClearCacheDialog() },
                    containerColor = AppColors.surfaceContainerHigh,
                    shape = AppShapes.dialog,
                    title = {
                        Text(
                            text = "CLEAR ALL CACHE",
                            color = AppColors.white,
                            style = AppTypography.headlineSmall
                        )
                    },
                    text = {
                        Text(
                            text = "This will remove all cached sessions, messages, and local data. You will need to re-download data from the server.\n\nThis action cannot be undone.",
                            color = AppColors.textSecondary,
                            style = AppTypography.bodyMedium
                        )
                    },
                    confirmButton = {
                        MoccaButton(
                            text = "CLEAR",
                            onClick = { screenModel.confirmClearCache() },
                            height = AppSpacing.buttonHeightCompact
                        )
                    },
                    dismissButton = {
                        MoccaTextButton(
                            text = "CANCEL",
                            onClick = { screenModel.hideClearCacheDialog() }
                        )
                    }
                )
            }
        }
    }
}

