@file:Suppress("DEPRECATION")

package com.mocca.app.ui.screens.settings

import com.mocca.app.api.NetworkConfig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

import androidx.compose.foundation.shape.CircleShape
import com.mocca.app.domain.model.*
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager

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
            // Header with iOS 26 liquid glass effect
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
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                contentPadding = PaddingValues(top = AppSpacing.lg, bottom = AppSpacing.screenPaddingBottom)
            ) {
                // Servers Section
                item {
                    Text(
                        text = "SERVERS",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }
                
                items(state.servers) { server ->
                    TerminalServerCard(
                        server = server,
                        isActive = server.id == state.activeServerId,
                        connectionStatus = state.connectionStatuses[server.id] ?: ServerConnectionStatus.UNKNOWN,
                        onActivate = { screenModel.setActiveServer(server.id) },
                        onEdit = { screenModel.editServer(server) },
                        onDelete = { screenModel.deleteServer(server.id) },
                        onCheckConnection = { screenModel.checkServerConnection(server) }
                    )
                }
                
                item {
                    MoccaButton(
                        text = "ADD SERVER",
                        onClick = { screenModel.addNewServer() },
                        modifier = Modifier.fillMaxWidth(),
                        height = AppSpacing.buttonHeightCompact
                    )
                }
                
                // Provider Authentication Section
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "PROVIDER AUTHENTICATION",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }
                
                item {
                    ModuleCard(title = "CONFIGURE PROVIDERS") {
                        val commonProviders = listOf("anthropic", "openai", "github")
                        
                        commonProviders.forEach { providerId ->
                            var isExpanded by remember { mutableStateOf(false) }
                            var manualKey by remember { mutableStateOf("") }
                            
                            // Header row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        isExpanded = !isExpanded
                                        if (isExpanded) {
                                            screenModel.loadAuthMethods(providerId)
                                        }
                                    }
                                    .padding(vertical = AppSpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = providerId.uppercase(),
                                    color = AppColors.white,
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
                                    val methods = state.providerAuthMethods[providerId]
                                    
                                    if (state.authLoading && state.selectedProviderId == providerId) {
                                        Text("Loading auth methods...", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                                    } else {
                                        // OAuth Button
                                        if (methods?.any { it.type == "oauth" } == true) {
                                            MoccaButton(
                                                text = "CONNECT (OAUTH)",
                                                onClick = { 
                                                    screenModel.startOAuth(providerId) { url ->
                                                        uriHandler.openUri(url)
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
                                            onClick = { screenModel.setManualKey(providerId, manualKey) },
                                            enabled = manualKey.isNotBlank(),
                                            height = AppSpacing.buttonHeightSmall
                                        )
                                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                                        MoccaCompactButton(
                                            text = "REMOVE AUTH",
                                            onClick = { screenModel.removeProviderAuth(providerId) },
                                            height = AppSpacing.buttonHeightSmall,
                                            backgroundColor = AppColors.error.copy(alpha = 0.15f),
                                            textColor = AppColors.error
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                        }
                    }
                }

                // App Configuration Section
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "APP CONFIGURATION",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }

                item {
                    ModuleCard(title = "GLOBAL DEFAULTS") {
                        val defaultProvider = state.serverDefaultProvider ?: "Not set"
                        val defaultModel = state.serverDefaultModel ?: "Not set"

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
                                    color = if (state.serverDefaultProvider != null) AppColors.statusOnline else AppColors.textSecondary,
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
                                    color = if (state.serverDefaultModel != null) AppColors.statusOnline else AppColors.textSecondary,
                                    style = AppTypography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        // Show available modes from server
                        if (state.serverModes.isNotEmpty()) {
                            Text(
                                text = "AVAILABLE MODES",
                                color = AppColors.textTertiary,
                                style = AppTypography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Text(
                                text = state.serverModes.joinToString(", ") { it.name },
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
                
                // ═══════════════════════════════════════════════════════════════════════════
                // PROJECT SECTION
                // ═══════════════════════════════════════════════════════════════════════════
                state.currentProject?.let { project ->
                    item {
                        Spacer(modifier = Modifier.height(AppSpacing.md))
                        Text(
                            text = "PROJECT",
                            color = AppColors.textSecondary,
                            style = AppTypography.labelSmall
                        )
                    }
                    item {
                        ModuleCard(title = "CURRENT PROJECT") {
                            Column(modifier = Modifier.padding(AppSpacing.sm)) {
                                ModuleRowItem(
                                    title = "NAME",
                                    subtitle = project.displayName,
                                    isEnabled = true,
                                    showToggle = false
                                )
                                HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(AppSpacing.sm))
                                Text(
                                    text = "PROJECT PATH",
                                    color = AppColors.textSecondary,
                                    style = AppTypography.labelSmall,
                                    modifier = Modifier.padding(horizontal = AppSpacing.sm)
                                )
                                Spacer(modifier = Modifier.height(AppSpacing.xs))
                                OutlinedTextField(
                                    value = state.editingProjectPath,
                                    onValueChange = { screenModel.setEditingProjectPath(it) },
                                    placeholder = { Text("/path/to/project", style = AppTypography.bodySmall, color = AppColors.textTertiary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = AppTypography.bodySmall.copy(color = AppColors.white),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { screenModel.saveProjectPath() }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.accent,
                                        unfocusedBorderColor = AppColors.border,
                                        cursorColor = AppColors.accent,
                                        focusedContainerColor = AppColors.background,
                                        unfocusedContainerColor = AppColors.background
                                    ),
                                    shape = AppShapes.medium
                                )
                                Spacer(modifier = Modifier.height(AppSpacing.sm))
                                MoccaCompactButton(
                                    text = "UPDATE PATH",
                                    onClick = { screenModel.saveProjectPath() },
                                    enabled = state.editingProjectPath.isNotBlank() &&
                                        state.editingProjectPath != (project.path ?: project.directory ?: ""),
                                    height = AppSpacing.buttonHeightSmall
                                )
                            }
                        }
                    }
                }
                // Server Info Section (when connected)
                state.serverVersion?.let { version ->
                    item {
                        ModuleCard(title = "OPENCODE SERVER INFO") {
                            ModuleRowItem(
                                title = "SERVER VERSION",
                                subtitle = version,
                                isEnabled = true,
                                showToggle = false
                            )
                        }
                    }
                }
                
                // ═══════════════════════════════════════════════════════════════════════════
                // APPEARANCE SECTION
                // ═══════════════════════════════════════════════════════════════════════════
                
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "APPEARANCE",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }
                
                item {
                    ModuleCard(title = "DISPLAY") {
                        // Show Token Counts
                        ModuleRowItem(
                            title = "SHOW TOKEN COUNTS",
                            subtitle = "Display input/output tokens in chat",
                            isEnabled = state.preferences.showTokenCounts,
                            onToggle = { screenModel.setShowTokenCounts(!state.preferences.showTokenCounts) }
                        )
                        
                        HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                        
                        // Show Timestamps
                        ModuleRowItem(
                            title = "SHOW TIMESTAMPS",
                            subtitle = "Display message timestamps",
                            isEnabled = state.preferences.showTimestamps,
                            onToggle = { screenModel.setShowTimestamps(!state.preferences.showTimestamps) }
                        )
                        
                        HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                        
                        // Compact Mode
                        ModuleRowItem(
                            title = "COMPACT MODE",
                            subtitle = "Reduced padding for higher density",
                            isEnabled = state.preferences.compactMode,
                            onToggle = { screenModel.setCompactMode(!state.preferences.compactMode) }
                        )
                        
                        HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                        
                        // Hide API Keys
                        ModuleRowItem(
                            title = "HIDE API KEYS",
                            subtitle = "Mask sensitive keys in settings",
                            isEnabled = state.preferences.hideApiKeys,
                            onToggle = { screenModel.setHideApiKeys(!state.preferences.hideApiKeys) }
                        )
                    }
                }
                
                // Font Scale Slider
                item {
                    ModuleCard(title = "FONT SIZE") {
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
                                    text = "${state.preferences.fontScalePercent}%",
                                    color = AppColors.accent,
                                    style = AppTypography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                            
                            // Font scale slider
                            var sliderValue by remember { mutableStateOf(state.preferences.fontScale) }
                            
                            androidx.compose.material3.Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = { screenModel.setFontScale(sliderValue) },
                                valueRange = 0.8f..1.4f,
                                steps = 5,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = AppColors.accent,
                                    activeTrackColor = AppColors.accent,
                                    inactiveTrackColor = AppColors.greyDark
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
                
                // ═══════════════════════════════════════════════════════════════════════════
                // CHAT SECTION
                // ═══════════════════════════════════════════════════════════════════════════
                
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "CHAT",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }
                
                item {
                    ModuleCard(title = "MESSAGING") {
                        // Auto Scroll
                        ModuleRowItem(
                            title = "AUTO SCROLL",
                            subtitle = "Scroll to bottom on new messages",
                            isEnabled = state.preferences.autoScroll,
                            onToggle = { screenModel.setAutoScroll(!state.preferences.autoScroll) }
                        )
                        
                        HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                        
                        // Confirm Delete
                        ModuleRowItem(
                            title = "CONFIRM DELETE",
                            subtitle = "Ask before deleting sessions",
                            isEnabled = state.preferences.confirmDelete,
                            onToggle = { screenModel.setConfirmDelete(!state.preferences.confirmDelete) }
                        )
                        
                        HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                        
                        // Show Thinking Blocks
                        ModuleRowItem(
                            title = "SHOW THINKING",
                            subtitle = "Display AI reasoning blocks",
                            isEnabled = state.preferences.showThinkingBlocks,
                            onToggle = { screenModel.setShowThinkingBlocks(!state.preferences.showThinkingBlocks) }
                        )
                    }
                }
                
                // ═══════════════════════════════════════════════════════════════════════════
                // NOTIFICATIONS SECTION
                // ═══════════════════════════════════════════════════════════════════════════
                
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "NOTIFICATIONS",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }
                
                item {
                    ModuleCard(title = "ALERTS") {
                        // Permission Notifications
                        ModuleRowItem(
                            title = "PERMISSION REQUESTS",
                            subtitle = "Alert when AI needs approval",
                            isEnabled = state.preferences.notifyPermissions,
                            onToggle = { screenModel.setNotifyPermissions(!state.preferences.notifyPermissions) }
                        )
                        
                        HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                        
                        // Session Complete
                        ModuleRowItem(
                            title = "SESSION COMPLETE",
                            subtitle = "Alert when AI finishes task",
                            isEnabled = state.preferences.notifySessionComplete,
                            onToggle = { screenModel.setNotifySessionComplete(!state.preferences.notifySessionComplete) }
                        )
                        
                        HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                        
                        // Connection Lost
                        ModuleRowItem(
                            title = "CONNECTION LOST",
                            subtitle = "Alert on server disconnect",
                            isEnabled = state.preferences.notifyConnectionLost,
                            onToggle = { screenModel.setNotifyConnectionLost(!state.preferences.notifyConnectionLost) }
                        )
                    }
                }
                
                // ═══════════════════════════════════════════════════════════════════════════
                // CONNECTION SECTION
                // ═══════════════════════════════════════════════════════════════════════════
                
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "CONNECTION",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }
                
                item {
                    ModuleCard(title = "NETWORK") {
                        // Auto Reconnect
                        ModuleRowItem(
                            title = "AUTO RECONNECT",
                            subtitle = "Reconnect when connection drops",
                            isEnabled = state.preferences.autoReconnect,
                            onToggle = { screenModel.setAutoReconnect(!state.preferences.autoReconnect) }
                        )
                        
                        HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                        
                        // Data Saver Mode
                        ModuleRowItem(
                            title = "DATA SAVER",
                            subtitle = "Reduce background network usage",
                            isEnabled = state.preferences.dataSaverMode,
                            onToggle = { screenModel.setDataSaverMode(!state.preferences.dataSaverMode) }
                        )
                        
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        
                        Text(
                            text = "Data Saver disables background sync and reduces network calls.",
                            color = AppColors.textTertiary,
                            style = AppTypography.labelSmall
                        )
                    }
                }
                
                // ═══════════════════════════════════════════════════════════════════════════
                // PRIVACY SECTION
                // ═══════════════════════════════════════════════════════════════════════════
                
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "PRIVACY & SECURITY",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }
                
                item {
                    ModuleCard(title = "SECURITY") {
                        // Screen Security
                        ModuleRowItem(
                            title = "SCREEN SECURITY",
                            subtitle = "Prevent screenshots",
                            isEnabled = state.preferences.screenSecurity,
                            onToggle = { screenModel.setScreenSecurity(!state.preferences.screenSecurity) }
                        )
                        
                        HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                        
                        // Clear Cache on Exit
                        ModuleRowItem(
                            title = "CLEAR CACHE ON EXIT",
                            subtitle = "Remove local data when app closes",
                            isEnabled = state.preferences.clearCacheOnExit,
                            onToggle = { screenModel.setClearCacheOnExit(!state.preferences.clearCacheOnExit) }
                        )
                    }
                }
                
                // Data Management
                item {
                    ModuleCard(title = "DATA") {
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
                                onClick = { screenModel.showClearCacheDialog() },
                                height = AppSpacing.buttonHeightSmall
                            )
                        }
                        
                        HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                        
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
                                onClick = { screenModel.resetPreferencesToDefaults() },
                                height = AppSpacing.buttonHeightSmall
                            )
                        }
                    }
                }
                
                // ═══════════════════════════════════════════════════════════════════════════
                // APP UPDATES SECTION
                // ═══════════════════════════════════════════════════════════════════════════
                
                // Auto Update Section
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "APP UPDATES",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }
                
                item {
                    ModuleCard(title = "GITHUB AUTO UPDATE") {
                        // Token status indicator
                        val tokenStatus = state.githubTokenStatus
                        val statusColor = when {
                            tokenStatus?.isValid == true -> AppColors.statusOnline
                            tokenStatus?.isMissing == true -> AppColors.textSecondary
                            tokenStatus?.isError == true -> AppColors.error
                            state.githubToken.isBlank() -> AppColors.textSecondary
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
                                        state.isValidatingToken -> "Validating..."
                                        tokenStatus?.isValid == true -> "Valid"
                                        tokenStatus?.isMissing == true -> "Not Set"
                                        tokenStatus?.isError == true -> "Invalid"
                                        state.githubToken.isBlank() -> "Not Set"
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
                        var tokenInput by remember { mutableStateOf(state.githubToken) }
                        
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
                                onClick = { screenModel.saveGitHubToken(tokenInput) },
                                enabled = tokenInput.isNotBlank() && tokenInput != state.githubToken && !state.isValidatingToken,
                                modifier = Modifier.weight(1f),
                                height = AppSpacing.buttonHeightCompact
                            )
                            
                            MoccaOutlinedButton(
                                text = "VALIDATE",
                                onClick = { screenModel.validateGitHubToken() },
                                enabled = state.githubToken.isNotBlank() && !state.isValidatingToken && !state.isLoading,
                                modifier = Modifier.weight(1f),
                                height = AppSpacing.buttonHeightCompact
                            )
                            
                            MoccaButton(
                                text = "CHECK UPDATES",
                                onClick = { screenModel.checkForUpdates() },
                                enabled = !state.isLoading && !state.isValidatingToken,
                                modifier = Modifier.weight(1.2f),
                                height = AppSpacing.buttonHeightCompact
                            )
                        }
                        
                        // Show message if any
                        state.message?.let { message ->
                            Spacer(modifier = Modifier.height(AppSpacing.md))
                            Text(
                                text = message,
                                color = when {
                                    message.contains("failed", ignoreCase = true) || 
                                    message.contains("error", ignoreCase = true) ||
                                    message.contains("invalid", ignoreCase = true) -> AppColors.error
                                    message.contains("valid", ignoreCase = true) ||
                                    message.contains("available", ignoreCase = true) -> AppColors.statusOnline
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
                // ═══════════════════════════════════════════════════════════════════════════
                // SKILLS SECTION
                // ═══════════════════════════════════════════════════════════════════════════

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "SKILLS",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }

                item {
                    ModuleCard(title = "SERVER SKILLS") {
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

                // ═══════════════════════════════════════════════════════════════════════════
                // EXPERIMENTAL SECTION
                // ═══════════════════════════════════════════════════════════════════════════

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "EXPERIMENTAL",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }

                item {
                    ModuleCard(title = "FEATURE FLAGS") {
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
                    containerColor = AppColors.surfaceElevated,
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

@Composable
private fun TerminalServerCard(
    server: ServerConfig,
    isActive: Boolean,
    connectionStatus: ServerConnectionStatus,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCheckConnection: () -> Unit
) {
    val borderColor = if (isActive) AppColors.statusOnline else null
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surfaceContainerHigh, AppShapes.card)
            .then(
                if (borderColor != null) {
                    Modifier.border(AppSpacing.borderThin, borderColor, AppShapes.card)
                } else Modifier
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onActivate)
                .background(if (isActive) AppColors.statusOnline.copy(alpha = 0.1f) else Color.Transparent)
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(
                color = if (isActive) AppColors.statusOnline else AppColors.grey,
                size = 12.dp
            )
            Spacer(modifier = Modifier.width(AppSpacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name.uppercase(),
                    color = if (isActive) AppColors.statusOnline else AppColors.white,
                    style = AppTypography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = server.baseUrl,
                    color = AppColors.textSecondary,
                    style = AppTypography.bodySmall
                )
            }
            
            if (isActive) {
                Text(
                    text = "SELECTED",
                    color = AppColors.statusOnline,
                    style = AppTypography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        HorizontalDivider(
            thickness = AppSpacing.borderThin,
            color = AppColors.border
        )
        
        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCheckConnection)
                    .padding(horizontal = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusIcon, statusColor) = when (connectionStatus) {
                    ServerConnectionStatus.UNKNOWN -> Icons.Default.QuestionMark to AppColors.textTertiary
                    ServerConnectionStatus.CHECKING -> Icons.Default.Sync to AppColors.statusWaiting
                    ServerConnectionStatus.CONNECTED -> Icons.Default.CheckCircle to AppColors.statusOnline
                    ServerConnectionStatus.FAILED -> Icons.Default.Error to AppColors.error
                }
                
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(
                    text = connectionStatus.name,
                    color = statusColor,
                    style = AppTypography.labelSmall
                )
            }
            
            // Edit/Delete
            MoccaIconButton(
                icon = Icons.Default.Edit,
                onClick = onEdit,
                iconColor = AppColors.textSecondary
            )
            MoccaIconButton(
                icon = Icons.Default.Delete,
                onClick = onDelete,
                iconColor = AppColors.alertRed
            )
        }
    }
}

@Composable
private fun TerminalServerEditDialog(
    server: ServerConfig,
    isNewServer: Boolean,
    onSave: (ServerConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(server.name.ifBlank { "DigitalOcean OpenCode" }) }
    var host by remember { mutableStateOf(server.host.ifBlank { NetworkConfig.DEFAULT_HOST_IP }) }
    var port by remember { mutableStateOf(if (server.port == 0) NetworkConfig.OPENCODE_SERVER_PORT.toString() else server.port.toString()) }
    var username by remember { mutableStateOf(server.username.ifBlank { NetworkConfig.DEFAULT_USERNAME }) }
    var password by remember { mutableStateOf(server.password.ifBlank { NetworkConfig.DEFAULT_PASSWORD }) }
    
    val focusManager = LocalFocusManager.current
    
    // Modern dialog using AlertDialog or custom rounded overlay
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceElevated,
        shape = AppShapes.dialog,
        title = {
            Text(
                text = if (isNewServer) "ADD SERVER" else "EDIT SERVER",
                color = AppColors.white,
                style = AppTypography.headlineSmall
            )
        },
        text = {
            Column {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                
                MoccaInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "SERVER NAME",
                    placeholder = "My Server",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.md))
                
                MoccaInput(
                    value = host,
                    onValueChange = { host = it },
                    label = "HOST",
                    placeholder = NetworkConfig.DEFAULT_HOST_IP,
                    hint = "Tailscale hostname, LAN IP, or localhost",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.md))
                
                MoccaInput(
                    value = port,
                    onValueChange = { port = it },
                    label = "PORT",
                    placeholder = "4242",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.md))
                
                MoccaInput(
                    value = username,
                    onValueChange = { username = it },
                    label = "USERNAME",
                    placeholder = NetworkConfig.DEFAULT_USERNAME,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.md))
                
                MoccaInput(
                    value = password,
                    onValueChange = { password = it },
                    label = "PASSWORD",
                    placeholder = "Leave empty if none",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { 
                        focusManager.clearFocus()
                        onSave(
                            server.copy(
                                name = name,
                                host = host,
                                port = port.toIntOrNull() ?: 4242,
                                username = username.ifBlank { NetworkConfig.DEFAULT_USERNAME },
                                password = password
                            )
                        )
                    })
                )
            }
        },
        confirmButton = {
            MoccaButton(
                text = "SAVE",
                onClick = {
                    onSave(
                        server.copy(
                            name = name,
                            host = host,
                            port = port.toIntOrNull() ?: 4242,
                            username = username.ifBlank { NetworkConfig.DEFAULT_USERNAME },
                            password = password
                        )
                    )
                },
                enabled = name.isNotBlank() && host.isNotBlank(),
                height = AppSpacing.buttonHeightCompact
            )
        },
        dismissButton = {
            MoccaTextButton(
                text = "CANCEL",
                onClick = onDismiss
            )
        }
    )
}
