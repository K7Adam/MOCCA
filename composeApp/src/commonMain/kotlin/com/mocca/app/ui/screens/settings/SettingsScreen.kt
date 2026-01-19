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
import com.mocca.app.ui.components.terminal.StatusSquare
import com.mocca.app.ui.components.terminal.TerminalButton
import com.mocca.app.ui.components.terminal.TerminalHeader
import com.mocca.app.ui.components.terminal.TerminalIconButton
import com.mocca.app.ui.components.terminal.TerminalInput
import com.mocca.app.ui.components.terminal.TerminalOutlinedButton
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography
import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

class SettingsScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<SettingsScreenModel>()
        val state by screenModel.state.collectAsState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalColors.background)
                .padding(TerminalSpacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TerminalHeader(text = "SYSTEM_CONFIGURATION", showBrackets = true)
                
                TerminalButton(
                    text = "ADD_SERVER",
                    onClick = { screenModel.addNewServer() },
                    icon = Icons.Default.Add,
                    height = 36.dp
                )
            }
            
            Spacer(modifier = Modifier.height(TerminalSpacing.xl))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
            ) {
                // Servers Section
                item {
                    Text(
                        text = "// CONNECTED_SERVERS",
                        color = TerminalColors.grey,
                        style = TerminalTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                }
                
                if (state.servers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(TerminalSpacing.borderThin, TerminalColors.greyDark, RectangleShape)
                                .padding(TerminalSpacing.lg),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "NO_SERVERS_CONFIGURED",
                                    color = TerminalColors.grey,
                                    style = TerminalTypography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                                Text(
                                    text = "CLICK [ADD_SERVER] TO CONFIGURE",
                                    color = TerminalColors.greyDark,
                                    style = TerminalTypography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    items(state.servers, key = { it.id }) { server ->
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
                }
                
                // About Section
                item {
                    Spacer(modifier = Modifier.height(TerminalSpacing.lg))
                    Text(
                        text = "// ABOUT_APPLICATION",
                        color = TerminalColors.grey,
                        style = TerminalTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                }
                
                item {
                    ModuleCard(title = "MOCCA_CLIENT_INFO") {
                        ModuleRowItem(
                            title = "VERSION",
                            subtitle = "v1.0.3",
                            isEnabled = true,
                            showToggle = false
                        )
                        ModuleRowItem(
                            title = "BUILD_TARGET",
                            subtitle = "ANDROID_KMP",
                            isEnabled = true,
                            showToggle = false
                        )
                        ModuleRowItem(
                            title = "PROTOCOL",
                            subtitle = "HTTP/2 + SSE",
                            isEnabled = true,
                            showToggle = false
                        )
                        
                        Column(modifier = Modifier.padding(top = TerminalSpacing.md)) {
                            TerminalButton(
                                text = if (state.isLoading) "CHECKING..." else "CHECK_FOR_UPDATES",
                                onClick = { screenModel.checkForUpdates() },
                                height = 40.dp,
                                enabled = !state.isLoading
                            )
                            
                            // Display update check result or error
                            state.message?.let { message ->
                                Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                                Text(
                                    text = message,
                                    color = if (message.contains("failed", ignoreCase = true) || message.contains("Error", ignoreCase = true)) {
                                        TerminalColors.error
                                    } else {
                                        TerminalColors.statusOnline
                                    },
                                    style = TerminalTypography.bodySmall
                                )
                            }
                        }
                        
                        // GitHub Token Configuration
                        Spacer(modifier = Modifier.height(TerminalSpacing.lg))
                        
                        TerminalInput(
                            value = state.githubToken,
                            onValueChange = { screenModel.saveGitHubToken(it) },
                            label = "GITHUB_ACCESS_TOKEN",
                            placeholder = "ghp_...",
                            hint = "Required for private repositories. Stored securely."
                        )
                        
                        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                        Text(
                            text = "Repository: K7Adam/MOCCA",
                            color = TerminalColors.greyDark,
                            style = TerminalTypography.bodySmall
                        )
                    }
                }
                
                // Server Info Section (when connected)
                state.serverVersion?.let { version ->
                    item {
                        Spacer(modifier = Modifier.height(TerminalSpacing.md))
                        ModuleCard(title = "OPENCODE_SERVER_INFO") {
                            ModuleRowItem(
                                title = "SERVER_VERSION",
                                subtitle = version,
                                isEnabled = true,
                                showToggle = false
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
    val borderColor = if (isActive) TerminalColors.statusOnline else TerminalColors.borderLight
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(TerminalSpacing.borderThin, borderColor, RectangleShape)
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
            StatusSquare(
                color = if (isActive) TerminalColors.statusOnline else TerminalColors.grey,
                modifier = Modifier.size(12.dp)
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
                    color = TerminalColors.grey,
                    style = TerminalTypography.bodySmall
                )
            }
            
            if (isActive) {
                Text(
                    text = "[ACTIVE]",
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
                    ServerConnectionStatus.UNKNOWN -> Icons.Default.QuestionMark to TerminalColors.grey
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
                iconColor = TerminalColors.greyLight
            )
            TerminalIconButton(
                icon = Icons.Default.Delete,
                onClick = onDelete,
                iconColor = TerminalColors.error
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
    
    // Simple full-screen overlay or dialog
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalColors.background.copy(alpha = 0.9f))
            .clickable(enabled = false) {}, // Block clicks
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(TerminalColors.background, RectangleShape)
                .border(TerminalSpacing.borderStandard, TerminalColors.borderLight, RectangleShape)
                .padding(TerminalSpacing.lg)
        ) {
            TerminalHeader(
                text = if (isNewServer) "ADD_SERVER" else "EDIT_SERVER",
                showBrackets = true
            )
            
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            
            TerminalInput(
                value = name,
                onValueChange = { name = it },
                label = "SERVER_NAME",
                placeholder = "MY_SERVER"
            )
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            TerminalInput(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = "BASE_URL",
                placeholder = "http://100.x.x.x:4096",
                hint = "Use Tailscale IP (100.x) or LAN IP"
            )
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            // Connection Type (simplified as Text for now, or Toggle logic)
            // Ideally should be a selector. Let's stick to Text display or simple buttons
            Text(
                text = "// CONNECTION_TYPE",
                color = TerminalColors.white,
                style = TerminalTypography.labelMedium
            )
            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
                ConnectionType.entries.take(3).forEach { type ->
                    val isSelected = connectionType == type
                    Box(
                        modifier = Modifier
                            .border(
                                width = TerminalSpacing.borderThin,
                                color = if (isSelected) TerminalColors.statusOnline else TerminalColors.grey,
                                shape = RectangleShape
                            )
                            .clickable { connectionType = type }
                            .padding(horizontal = TerminalSpacing.md, vertical = TerminalSpacing.sm)
                    ) {
                        Text(
                            text = type.name,
                            color = if (isSelected) TerminalColors.statusOnline else TerminalColors.grey,
                            style = TerminalTypography.labelSmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            TerminalInput(
                value = authToken,
                onValueChange = { authToken = it },
                label = "AUTH_TOKEN",
                placeholder = "sk-..."
            )
            
            Spacer(modifier = Modifier.height(TerminalSpacing.xl))
            
            Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)) {
                TerminalOutlinedButton(
                    text = "CANCEL",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
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
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
