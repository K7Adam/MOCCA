@file:Suppress("DEPRECATION")

package com.mocca.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.api.NetworkConfig
import com.mocca.app.domain.model.*
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.collections.immutable.ImmutableList

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
                    
                    item {
                        Column {
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
                
                // Server Info Section
                state.serverVersion?.let { version ->
                    item {
                        SettingsCard(title = "OPENCODE SERVER INFO") {
                            SettingsRowItem(
                                title = "SERVER VERSION",
                                subtitle = version,
                                isEnabled = true,
                                showToggle = false
                            )
                        }
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
                            color = AppColors.primary,
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
        
        SettingsCard(title = "GLOBAL DEFAULTS") {
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
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        SettingsCard(title = "GITHUB AUTO UPDATE") {
            // Token status indicator
            val tokenStatus = githubTokenStatus
            val statusColor = when {
                tokenStatus?.isValid == true -> AppColors.statusOnline
                tokenStatus?.isMissing == true -> AppColors.textSecondary
                tokenStatus?.isError == true -> AppColors.error
                githubToken.isBlank() -> AppColors.textSecondary
                else -> AppColors.textSecondary
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Token Status:",
                    color = AppColors.textSecondary,
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
                color = AppColors.textSecondary,
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
                        else -> AppColors.textSecondary
                    },
                    style = AppTypography.labelSmall
                )
            }
            
            // Help text
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(
                text = "Create a token at github.com/settings/tokens (requires 'repo' scope for private repos)",
                color = AppColors.textTertiary,
                style = AppTypography.labelSmall
            )
        }
    }
}
@Composable
fun AppearanceSection(
    preferences: UserPreferences,
    onSetShowTokenCounts: (Boolean) -> Unit,
    onSetShowTimestamps: (Boolean) -> Unit,
    onSetCompactMode: (Boolean) -> Unit,
    onSetHideApiKeys: (Boolean) -> Unit,
    onSetFontScale: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "APPEARANCE",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        SettingsCard(title = "DISPLAY") {
            // Show Token Counts
            SettingsRowItem(
                title = "SHOW TOKEN COUNTS",
                subtitle = "Display input/output tokens in chat",
                isEnabled = preferences.showTokenCounts,
                onToggle = { onSetShowTokenCounts(!preferences.showTokenCounts) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Show Timestamps
            SettingsRowItem(
                title = "SHOW TIMESTAMPS",
                subtitle = "Display message timestamps",
                isEnabled = preferences.showTimestamps,
                onToggle = { onSetShowTimestamps(!preferences.showTimestamps) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Compact Mode
            SettingsRowItem(
                title = "COMPACT MODE",
                subtitle = "Reduced padding for higher density",
                isEnabled = preferences.compactMode,
                onToggle = { onSetCompactMode(!preferences.compactMode) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Hide API Keys
            SettingsRowItem(
                title = "HIDE API KEYS",
                subtitle = "Mask sensitive keys in settings",
                isEnabled = preferences.hideApiKeys,
                onToggle = { onSetHideApiKeys(!preferences.hideApiKeys) }
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.cardGap))
        
        // Font Scale Slider
        SettingsCard(title = "FONT SIZE") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEXT SCALE",
                        color = AppColors.textPrimary,
                        style = AppTypography.bodyMedium
                    )
                    Text(
                        text = "${preferences.fontScalePercent}%",
                        color = AppColors.primary,
                        style = AppTypography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                
                // Font scale slider
                var sliderValue by remember { mutableStateOf(preferences.fontScale) }
                
                androidx.compose.material3.Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onSetFontScale(sliderValue) },
                    valueRange = 0.8f..1.4f,
                    steps = 5,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = AppColors.primary,
                        activeTrackColor = AppColors.primary,
                        inactiveTrackColor = AppColors.textSecondaryDark
                    )
                )
                
                // Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Small", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                    Text("Default", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                    Text("Large", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                }
            }
        }
    }
}
@Composable
fun ChatSection(
    preferences: UserPreferences,
    onSetAutoScroll: (Boolean) -> Unit,
    onSetConfirmDelete: (Boolean) -> Unit,
    onSetShowThinkingBlocks: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "CHAT",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        SettingsCard(title = "MESSAGING") {
            // Auto Scroll
            SettingsRowItem(
                title = "AUTO SCROLL",
                subtitle = "Scroll to bottom on new messages",
                isEnabled = preferences.autoScroll,
                onToggle = { onSetAutoScroll(!preferences.autoScroll) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Confirm Delete
            SettingsRowItem(
                title = "CONFIRM DELETE",
                subtitle = "Ask before deleting sessions",
                isEnabled = preferences.confirmDelete,
                onToggle = { onSetConfirmDelete(!preferences.confirmDelete) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Show Thinking Blocks
            SettingsRowItem(
                title = "SHOW THINKING",
                subtitle = "Display AI reasoning blocks",
                isEnabled = preferences.showThinkingBlocks,
                onToggle = { onSetShowThinkingBlocks(!preferences.showThinkingBlocks) }
            )
        }
    }
}
@Composable
fun ConnectionSection(
    preferences: UserPreferences,
    onSetAutoReconnect: (Boolean) -> Unit,
    onSetDataSaverMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "CONNECTION",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        SettingsCard(title = "NETWORK") {
            // Auto Reconnect
            SettingsRowItem(
                title = "AUTO RECONNECT",
                subtitle = "Reconnect when connection drops",
                isEnabled = preferences.autoReconnect,
                onToggle = { onSetAutoReconnect(!preferences.autoReconnect) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Data Saver Mode
            SettingsRowItem(
                title = "DATA SAVER",
                subtitle = "Reduce background network usage",
                isEnabled = preferences.dataSaverMode,
                onToggle = { onSetDataSaverMode(!preferences.dataSaverMode) }
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            
            Text(
                text = "Data Saver disables background sync and reduces network calls.",
                color = AppColors.textTertiary,
                style = AppTypography.labelSmall
            )
        }
    }
}
@Composable
fun ExperimentalSection(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "EXPERIMENTAL",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        SettingsCard(title = "FEATURE FLAGS") {
            Text(
                text = "Manage server-wide experimental feature flags and global config options.",
                style = AppTypography.labelSmall,
                color = AppColors.textSecondary
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            MoccaButton(
                text = "MANAGE FLAGS",
                onClick = { navigator.push(FeatureFlagsScreen) },
                height = AppSpacing.buttonHeightCompact
            )
        }
    }
}
@Composable
fun NotificationsSection(
    preferences: UserPreferences,
    onSetNotifyPermissions: (Boolean) -> Unit,
    onSetNotifySessionComplete: (Boolean) -> Unit,
    onSetNotifyConnectionLost: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "NOTIFICATIONS",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        SettingsCard(title = "ALERTS") {
            // Permission Notifications
            SettingsRowItem(
                title = "PERMISSION REQUESTS",
                subtitle = "Alert when AI needs approval",
                isEnabled = preferences.notifyPermissions,
                onToggle = { onSetNotifyPermissions(!preferences.notifyPermissions) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Session Complete
            SettingsRowItem(
                title = "SESSION COMPLETE",
                subtitle = "Alert when AI finishes task",
                isEnabled = preferences.notifySessionComplete,
                onToggle = { onSetNotifySessionComplete(!preferences.notifySessionComplete) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Connection Lost
            SettingsRowItem(
                title = "CONNECTION LOST",
                subtitle = "Alert on server disconnect",
                isEnabled = preferences.notifyConnectionLost,
                onToggle = { onSetNotifyConnectionLost(!preferences.notifyConnectionLost) }
            )
        }
    }
}
@Composable
fun PrivacySecuritySection(
    preferences: UserPreferences,
    onSetScreenSecurity: (Boolean) -> Unit,
    onSetClearCacheOnExit: (Boolean) -> Unit,
    onShowClearCacheDialog: () -> Unit,
    onResetPreferences: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "PRIVACY & SECURITY",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        SettingsCard(title = "SECURITY") {
            // Screen Security
            SettingsRowItem(
                title = "SCREEN SECURITY",
                subtitle = "Prevent screenshots",
                isEnabled = preferences.screenSecurity,
                onToggle = { onSetScreenSecurity(!preferences.screenSecurity) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Clear Cache on Exit
            SettingsRowItem(
                title = "CLEAR CACHE ON EXIT",
                subtitle = "Remove local data when app closes",
                isEnabled = preferences.clearCacheOnExit,
                onToggle = { onSetClearCacheOnExit(!preferences.clearCacheOnExit) }
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.cardGap))
        
        // Data Management
        SettingsCard(title = "DATA") {
            // Clear Cache Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CLEAR ALL CACHE",
                        color = AppColors.textPrimary,
                        style = AppTypography.bodyMedium
                    )
                    Text(
                        text = "Remove cached sessions and messages",
                        color = AppColors.textTertiary,
                        style = AppTypography.labelSmall
                    )
                }
                
                MoccaOutlinedButton(
                    text = "CLEAR",
                    onClick = onShowClearCacheDialog,
                    height = AppSpacing.buttonHeightSmall
                )
            }
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Reset Preferences
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RESET PREFERENCES",
                        color = AppColors.textPrimary,
                        style = AppTypography.bodyMedium
                    )
                    Text(
                        text = "Restore all settings to defaults",
                        color = AppColors.textTertiary,
                        style = AppTypography.labelSmall
                    )
                }
                
                MoccaOutlinedButton(
                    text = "RESET",
                    onClick = onResetPreferences,
                    height = AppSpacing.buttonHeightSmall
                )
            }
        }
    }
}
@Composable
fun ProjectSection(
    currentProject: Project,
    editingProjectPath: String,
    onSetEditingProjectPath: (String) -> Unit,
    onSaveProjectPath: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "PROJECT",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        SettingsCard(title = "CURRENT PROJECT") {
            Column(modifier = Modifier.padding(AppSpacing.sm)) {
                SettingsRowItem(
                    title = "NAME",
                    subtitle = currentProject.displayName,
                    isEnabled = true,
                    showToggle = false
                )
                HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Text(
                    text = "PROJECT PATH",
                    color = AppColors.textSecondary,
                    style = AppTypography.labelSmall,
                    modifier = Modifier.padding(horizontal = AppSpacing.sm)
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                OutlinedTextField(
                    value = editingProjectPath,
                    onValueChange = onSetEditingProjectPath,
                    placeholder = { Text("/path/to/project", style = AppTypography.bodySmall, color = AppColors.textTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = AppTypography.bodySmall.copy(color = AppColors.textPrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onSaveProjectPath() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.primary,
                        unfocusedBorderColor = AppColors.border,
                        cursorColor = AppColors.primary,
                        focusedContainerColor = AppColors.background,
                        unfocusedContainerColor = AppColors.background
                    ),
                    shape = AppShapes.medium
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                MoccaCompactButton(
                    text = "UPDATE PATH",
                    onClick = onSaveProjectPath,
                    enabled = editingProjectPath.isNotBlank() &&
                        editingProjectPath != (currentProject.path ?: currentProject.directory ?: ""),
                    height = AppSpacing.buttonHeightSmall
                )
            }
        }
    }
}
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
        
        SettingsCard(title = "CONFIGURE PROVIDERS") {
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
                        color = AppColors.primary,
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
@Composable
fun ServersSection(
    servers: ImmutableList<ServerConfig>,
    activeServerId: String?,
    connectionStatuses: Map<String, ServerConnectionStatus>,
    onActivate: (String) -> Unit,
    onEdit: (ServerConfig) -> Unit,
    onDelete: (String) -> Unit,
    onCheckConnection: (ServerConfig) -> Unit,
    onAddNewServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "SERVERS",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        servers.forEach { server ->
            TerminalServerCard(
                server = server,
                isActive = server.id == activeServerId,
                connectionStatus = connectionStatuses[server.id] ?: ServerConnectionStatus.UNKNOWN,
                onActivate = { onActivate(server.id) },
                onEdit = { onEdit(server) },
                onDelete = { onDelete(server.id) },
                onCheckConnection = { onCheckConnection(server) }
            )
            Spacer(modifier = Modifier.height(AppSpacing.cardGap))
        }
        
        MoccaButton(
            text = "ADD SERVER",
            onClick = onAddNewServer,
            modifier = Modifier.fillMaxWidth(),
            height = AppSpacing.buttonHeightCompact
        )
    }
}
@Composable
fun SkillsSection(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "SKILLS",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        SettingsCard(title = "SERVER SKILLS") {
            Text(
                text = "View agent skills registered on the OpenCode server.",
                style = AppTypography.labelSmall,
                color = AppColors.textSecondary
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            MoccaButton(
                text = "BROWSE SKILLS",
                onClick = { navigator.push(com.mocca.app.ui.screens.skills.SkillsScreen) },
                height = AppSpacing.buttonHeightCompact
            )
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
            .background(AppColors.surfaceVariant, AppShapes.moduleCard)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.moduleCard)
    ) {
        // Header
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
                    text = title.uppercase(),
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
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(color = AppColors.white.copy(alpha = 0.1f)),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        if (!isEnabled || !isConnected) {
            StatusDot(
                color = when {
                    !isEnabled -> AppColors.textTertiary
                    else -> AppColors.statusOffline
                },
                size = AppSpacing.statusDotSizeLarge
            )
            Spacer(modifier = Modifier.width(AppSpacing.md))
        }
        
        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isEnabled) AppColors.textPrimary else AppColors.textSecondary,
                style = AppTypography.bodyMedium.copy(
                    textDecoration = if (isStrikethrough) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                ),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = AppColors.textTertiary,
                style = AppTypography.bodySmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        // Toggle switch
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
                    uncheckedThumbColor = AppColors.textPrimary,
                    uncheckedTrackColor = AppColors.surfaceVariant,
                    uncheckedBorderColor = AppColors.border,
                    disabledCheckedThumbColor = AppColors.textSecondary,
                    disabledCheckedTrackColor = AppColors.surfaceVariant,
                    disabledUncheckedThumbColor = AppColors.textSecondary,
                    disabledUncheckedTrackColor = AppColors.surfaceVariant
                )
            )
        }
    }
}
