package com.mocca.app.ui.screens.settings

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
import com.mocca.app.domain.model.*
import com.mocca.app.ui.components.terminal.ModuleCard
import com.mocca.app.ui.components.terminal.ModuleRowItem
import com.mocca.app.ui.components.terminal.StatusDot
import com.mocca.app.ui.components.terminal.StatusSquare
import com.mocca.app.ui.components.terminal.MoccaButton
import com.mocca.app.ui.components.terminal.MoccaCompactButton
import com.mocca.app.ui.components.terminal.TerminalHeader
import com.mocca.app.ui.components.terminal.MoccaIconButton
import com.mocca.app.ui.components.terminal.MoccaInput
import com.mocca.app.ui.components.terminal.MoccaOutlinedButton
import com.mocca.app.ui.components.terminal.MoccaTextButton
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
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
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
                        // GitHub PAT input for private repo access
                        var tokenInput by remember { mutableStateOf(state.githubToken) }
                        
                        Text(
                            text = "GitHub Personal Access Token (for private repo access)",
                            color = AppColors.textSecondary,
                            style = AppTypography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        
                        MoccaInput(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = "GITHUB PAT",
                            placeholder = "ghp_..."
                        )
                        
                        Spacer(modifier = Modifier.height(AppSpacing.md))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                        ) {
                            MoccaOutlinedButton(
                                text = "SAVE TOKEN",
                                onClick = { screenModel.saveGitHubToken(tokenInput) },
                                enabled = tokenInput.isNotBlank() && tokenInput != state.githubToken,
                                modifier = Modifier.weight(1f),
                                height = AppSpacing.buttonHeightCompact
                            )
                            
                            MoccaButton(
                                text = "CHECK FOR UPDATES",
                                onClick = { screenModel.checkForUpdates() },
                                enabled = !state.isLoading,
                                modifier = Modifier.weight(1f),
                                height = AppSpacing.buttonHeightCompact
                            )
                        }
                        
                        // Show message if any
                        state.message?.let { message ->
                            Spacer(modifier = Modifier.height(AppSpacing.md))
                            Text(
                                text = message,
                                color = if (message.contains("failed", ignoreCase = true) || message.contains("error", ignoreCase = true)) 
                                    AppColors.error else AppColors.statusOnline,
                                style = AppTypography.labelSmall
                            )
                        }
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
    val borderColor = if (isActive) AppColors.statusOnline else AppColors.border
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceContainer, AppShapes.card)
            .border(AppSpacing.borderThin, borderColor, AppShapes.card)
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
                    text = "ACTIVE",
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
    var name by remember { mutableStateOf(server.name.ifBlank { "Omen" }) } // TEMPORARY
    var host by remember { mutableStateOf(server.host.ifBlank { "omen.tail0b932a.ts.net" }) } // TEMPORARY
    var port by remember { mutableStateOf(if (server.port == 4096) "443" else server.port.toString()) } // TEMPORARY
    var username by remember { mutableStateOf(server.username.ifBlank { "adamk7" }) } // TEMPORARY
    var password by remember { mutableStateOf(server.password.ifBlank { "Victory&Bliss4ever" }) } // TEMPORARY
    
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
                    placeholder = "10.0.2.2 or mydevice.ts.net",
                    hint = "Tailscale hostname, LAN IP, or localhost",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.md))
                
                MoccaInput(
                    value = port,
                    onValueChange = { port = it },
                    label = "PORT",
                    placeholder = "4096",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.md))
                
                MoccaInput(
                    value = username,
                    onValueChange = { username = it },
                    label = "USERNAME",
                    placeholder = "opencode",
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
                                port = port.toIntOrNull() ?: 4096,
                                username = username.ifBlank { "opencode" },
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
                            port = port.toIntOrNull() ?: 4096,
                            username = username.ifBlank { "opencode" },
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
