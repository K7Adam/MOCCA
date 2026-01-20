package com.mocca.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalShapes
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography
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
                .background(TerminalColors.background)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalColors.surface)
                    .padding(horizontal = TerminalSpacing.screenPaddingHorizontal, vertical = TerminalSpacing.md)
            ) {
                Text(
                    text = "SETTINGS",
                    color = TerminalColors.white,
                    style = TerminalTypography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TerminalSpacing.screenPaddingHorizontal),
                verticalArrangement = Arrangement.spacedBy(TerminalSpacing.lg),
                contentPadding = PaddingValues(top = TerminalSpacing.lg, bottom = TerminalSpacing.screenPaddingBottom)
            ) {
                // Servers Section
                item {
                    Text(
                        text = "SERVERS",
                        color = TerminalColors.textSecondary,
                        style = TerminalTypography.labelSmall
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Provider Authentication Section
                item {
                    Spacer(modifier = Modifier.height(TerminalSpacing.md))
                    Text(
                        text = "PROVIDER AUTHENTICATION",
                        color = TerminalColors.textSecondary,
                        style = TerminalTypography.labelSmall
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
                                    .padding(vertical = TerminalSpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = providerId.uppercase(),
                                    color = TerminalColors.white,
                                    style = TerminalTypography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isExpanded) "[-]" else "[+]",
                                    color = TerminalColors.textTertiary,
                                    style = TerminalTypography.labelSmall
                                )
                            }
                            
                            if (isExpanded) {
                                Column(modifier = Modifier.padding(start = TerminalSpacing.md, bottom = TerminalSpacing.md)) {
                                    val methods = state.providerAuthMethods[providerId]
                                    
                                    if (state.authLoading && state.selectedProviderId == providerId) {
                                        Text("Loading auth methods...", color = TerminalColors.textTertiary, style = TerminalTypography.labelSmall)
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
                                                height = 40.dp
                                            )
                                            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                                            Text("- OR -", color = TerminalColors.textTertiary, style = TerminalTypography.labelSmall)
                                            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                                        }
                                        
                                        // Manual Key Input
                                        TerminalInput(
                                            value = manualKey,
                                            onValueChange = { manualKey = it },
                                            placeholder = "API KEY",
                                            label = null
                                        )
                                        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                                        TerminalCompactButton(
                                            text = "SAVE KEY",
                                            onClick = { screenModel.setManualKey(providerId, manualKey) },
                                            enabled = manualKey.isNotBlank(),
                                            height = 36.dp
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = TerminalColors.border, thickness = 0.5.dp)
                        }
                    }
                }

                // App Configuration Section
                item {
                    Spacer(modifier = Modifier.height(TerminalSpacing.md))
                    Text(
                        text = "APP CONFIGURATION",
                        color = TerminalColors.textSecondary,
                        style = TerminalTypography.labelSmall
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
                                    color = TerminalColors.textTertiary,
                                    style = TerminalTypography.labelSmall
                                )
                                Text(
                                    text = defaultProvider.uppercase(),
                                    color = if (state.serverDefaultProvider != null) TerminalColors.statusOnline else TerminalColors.textSecondary,
                                    style = TerminalTypography.bodyMedium
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DEFAULT MODEL",
                                    color = TerminalColors.textTertiary,
                                    style = TerminalTypography.labelSmall
                                )
                                Text(
                                    text = defaultModel,
                                    color = if (state.serverDefaultModel != null) TerminalColors.statusOnline else TerminalColors.textSecondary,
                                    style = TerminalTypography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(TerminalSpacing.md))

                        // Show available modes from server
                        if (state.serverModes.isNotEmpty()) {
                            Text(
                                text = "AVAILABLE MODES",
                                color = TerminalColors.textTertiary,
                                style = TerminalTypography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(TerminalSpacing.xs))
                            Text(
                                text = state.serverModes.joinToString(", ") { it.name },
                                color = TerminalColors.textSecondary,
                                style = TerminalTypography.labelSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(TerminalSpacing.lg))

                        // Info note about server-side configuration
                        Text(
                            text = "Provider and model are configured on the OpenCode server.",
                            color = TerminalColors.textTertiary,
                            style = TerminalTypography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(TerminalSpacing.xs))
                        Text(
                            text = "Update these settings via /config command in OpenCode.",
                            color = TerminalColors.textTertiary,
                            style = TerminalTypography.labelSmall
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
                    Spacer(modifier = Modifier.height(TerminalSpacing.md))
                    Text(
                        text = "APP UPDATES",
                        color = TerminalColors.textSecondary,
                        style = TerminalTypography.labelSmall
                    )
                }
                
                item {
                    ModuleCard(title = "GITHUB AUTO UPDATE") {
                        // GitHub PAT input for private repo access
                        var tokenInput by remember { mutableStateOf(state.githubToken) }
                        
                        Text(
                            text = "GitHub Personal Access Token (for private repo access)",
                            color = TerminalColors.textSecondary,
                            style = TerminalTypography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                        
                        TerminalInput(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = "GITHUB PAT",
                            placeholder = "ghp_..."
                        )
                        
                        Spacer(modifier = Modifier.height(TerminalSpacing.md))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
                        ) {
                            TerminalOutlinedButton(
                                text = "SAVE TOKEN",
                                onClick = { screenModel.saveGitHubToken(tokenInput) },
                                enabled = tokenInput.isNotBlank() && tokenInput != state.githubToken,
                                modifier = Modifier.weight(1f)
                            )
                            
                            TerminalButton(
                                text = "CHECK FOR UPDATES",
                                onClick = { screenModel.checkForUpdates() },
                                enabled = !state.isLoading,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Show message if any
                        state.message?.let { message ->
                            Spacer(modifier = Modifier.height(TerminalSpacing.md))
                            Text(
                                text = message,
                                color = if (message.contains("failed", ignoreCase = true) || message.contains("error", ignoreCase = true)) 
                                    TerminalColors.error else TerminalColors.statusOnline,
                                style = TerminalTypography.labelSmall
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
    val borderColor = if (isActive) TerminalColors.statusOnline else TerminalColors.border
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TerminalShapes.card)
            .background(TerminalColors.surfaceContainer, TerminalShapes.card)
            .border(TerminalSpacing.borderThin, borderColor, TerminalShapes.card)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onActivate)
                .background(if (isActive) TerminalColors.statusOnline.copy(alpha = 0.1f) else Color.Transparent)
                .padding(TerminalSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(
                color = if (isActive) TerminalColors.statusOnline else TerminalColors.grey,
                size = 12.dp
            )
            Spacer(modifier = Modifier.width(TerminalSpacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name.uppercase(),
                    color = if (isActive) TerminalColors.statusOnline else TerminalColors.white,
                    style = TerminalTypography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = server.baseUrl,
                    color = TerminalColors.textSecondary,
                    style = TerminalTypography.bodySmall
                )
            }
            
            if (isActive) {
                Text(
                    text = "ACTIVE",
                    color = TerminalColors.statusOnline,
                    style = TerminalTypography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        HorizontalDivider(
            thickness = TerminalSpacing.borderThin,
            color = TerminalColors.border
        )
        
        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TerminalSpacing.sm),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCheckConnection)
                    .padding(horizontal = TerminalSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusIcon, statusColor) = when (connectionStatus) {
                    ServerConnectionStatus.UNKNOWN -> Icons.Default.QuestionMark to TerminalColors.textTertiary
                    ServerConnectionStatus.CHECKING -> Icons.Default.Sync to TerminalColors.statusWaiting
                    ServerConnectionStatus.CONNECTED -> Icons.Default.CheckCircle to TerminalColors.statusOnline
                    ServerConnectionStatus.FAILED -> Icons.Default.Error to TerminalColors.error
                }
                
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(TerminalSpacing.xs))
                Text(
                    text = connectionStatus.name,
                    color = statusColor,
                    style = TerminalTypography.labelSmall
                )
            }
            
            // Edit/Delete
            TerminalIconButton(
                icon = Icons.Default.Edit,
                onClick = onEdit,
                iconColor = TerminalColors.textSecondary
            )
            TerminalIconButton(
                icon = Icons.Default.Delete,
                onClick = onDelete,
                iconColor = TerminalColors.alertRed
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
        containerColor = TerminalColors.surfaceElevated,
        shape = TerminalShapes.dialog,
        title = {
            Text(
                text = if (isNewServer) "ADD SERVER" else "EDIT SERVER",
                color = TerminalColors.white,
                style = TerminalTypography.headlineSmall
            )
        },
        text = {
            Column {
                Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                
                TerminalInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "SERVER NAME",
                    placeholder = "My Server"
                )
                
                Spacer(modifier = Modifier.height(TerminalSpacing.md))
                
                TerminalInput(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = "BASE URL",
                    placeholder = "http://100.x.x.x:4096",
                    hint = "Use Tailscale IP (100.x) or LAN IP"
                )
                
                Spacer(modifier = Modifier.height(TerminalSpacing.md))
                
                // Connection Type Selector
                Text(
                    text = "CONNECTION TYPE",
                    color = TerminalColors.textSecondary,
                    style = TerminalTypography.labelMedium
                )
                Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
                    ConnectionType.entries.take(3).forEach { type ->
                        val isSelected = connectionType == type
                        Box(
                            modifier = Modifier
                                .clip(TerminalShapes.pill)
                                .border(
                                    width = TerminalSpacing.borderThin,
                                    color = if (isSelected) TerminalColors.statusOnline else TerminalColors.border,
                                    shape = TerminalShapes.pill
                                )
                                .background(
                                    if (isSelected) TerminalColors.statusOnline.copy(alpha = 0.1f) else Color.Transparent,
                                    TerminalShapes.pill
                                )
                                .clickable { connectionType = type }
                                .padding(horizontal = TerminalSpacing.md, vertical = TerminalSpacing.sm)
                        ) {
                            Text(
                                text = type.name,
                                color = if (isSelected) TerminalColors.statusOnline else TerminalColors.textSecondary,
                                style = TerminalTypography.labelSmall
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(TerminalSpacing.md))
                
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
                enabled = name.isNotBlank() && baseUrl.isNotBlank()
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
