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
import androidx.compose.ui.graphics.RectangleShape
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
import com.mocca.app.ui.components.terminal.TerminalButton
import com.mocca.app.ui.components.terminal.TerminalCompactButton
import com.mocca.app.ui.components.terminal.TerminalHeader
import com.mocca.app.ui.components.terminal.TerminalIconButton
import com.mocca.app.ui.components.terminal.TerminalInput
import com.mocca.app.ui.components.terminal.TerminalOutlinedButton
import com.mocca.app.ui.components.terminal.TerminalTextButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import androidx.compose.ui.platform.LocalUriHandler

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
                TerminalIconButton(
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
                    TerminalButton(
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
                                            TerminalButton(
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
                                        TerminalInput(
                                            value = manualKey,
                                            onValueChange = { manualKey = it },
                                            placeholder = "API KEY",
                                            label = null
                                        )
                                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                                        TerminalCompactButton(
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
                        
                        TerminalInput(
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
                            TerminalOutlinedButton(
                                text = "SAVE TOKEN",
                                onClick = { screenModel.saveGitHubToken(tokenInput) },
                                enabled = tokenInput.isNotBlank() && tokenInput != state.githubToken,
                                modifier = Modifier.weight(1f),
                                height = AppSpacing.buttonHeightCompact
                            )
                            
                            TerminalButton(
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
            TerminalIconButton(
                icon = Icons.Default.Edit,
                onClick = onEdit,
                iconColor = AppColors.textSecondary
            )
            TerminalIconButton(
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
    var name by remember { mutableStateOf(server.name) }
    var baseUrl by remember { mutableStateOf(server.baseUrl) }
    var connectionType by remember { mutableStateOf(server.connectionType) }
    var authToken by remember { mutableStateOf(server.authToken ?: "") }
    
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
                
                TerminalInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "SERVER NAME",
                    placeholder = "My Server"
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.md))
                
                TerminalInput(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = "BASE URL",
                    placeholder = "http://100.x.x.x:4096",
                    hint = "Use Tailscale IP (100.x) or LAN IP"
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.md))
                
                // Connection Type Selector
                Text(
                    text = "CONNECTION TYPE",
                    color = AppColors.textSecondary,
                    style = AppTypography.labelMedium
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    ConnectionType.entries.take(3).forEach { type ->
                        val isSelected = connectionType == type
                        Box(
                            modifier = Modifier
                                .clip(AppShapes.pill)
                                .border(
                                    width = AppSpacing.borderThin,
                                    color = if (isSelected) AppColors.statusOnline else AppColors.border,
                                    shape = AppShapes.pill
                                )
                                .background(
                                    if (isSelected) AppColors.statusOnline.copy(alpha = 0.1f) else Color.Transparent,
                                    AppShapes.pill
                                )
                                .clickable { connectionType = type }
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
                        ) {
                            Text(
                                text = type.name,
                                color = if (isSelected) AppColors.statusOnline else AppColors.textSecondary,
                                style = AppTypography.labelSmall
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(AppSpacing.md))
                
                TerminalInput(
                    value = authToken,
                    onValueChange = { authToken = it },
                    label = "AUTH TOKEN",
                    placeholder = "sk-..."
                )
            }
        },
        confirmButton = {
            TerminalButton(
                text = "SAVE",
                onClick = {
                    onSave(
                        server.copy(
                            name = name,
                            baseUrl = baseUrl,
                            connectionType = connectionType,
                            authType = if (authToken.isNotBlank()) AuthType.BEARER else AuthType.NONE,
                            authToken = authToken.takeIf { it.isNotBlank() }
                        )
                    )
                },
                enabled = name.isNotBlank() && baseUrl.isNotBlank(),
                height = AppSpacing.buttonHeightCompact
            )
        },
        dismissButton = {
            TerminalTextButton(
                text = "CANCEL",
                onClick = onDismiss
            )
        }
    )
}
