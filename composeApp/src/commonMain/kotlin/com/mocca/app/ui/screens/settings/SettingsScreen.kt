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

import androidx.compose.ui.platform.LocalUriHandler

// ... imports

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
                .padding(TerminalSpacing.lg)
        ) {
            // ... (Header remains same)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
            ) {
                // ... (Servers section remains same)
                
                // Provider Authentication Section
                item {
                    Spacer(modifier = Modifier.height(TerminalSpacing.lg))
                    Text(
                        text = "// PROVIDER_AUTHENTICATION",
                        color = TerminalColors.grey,
                        style = TerminalTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                }
                
                item {
                    ModuleCard(title = "CONFIGURE_PROVIDERS") {
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
                                    color = TerminalColors.greyLight,
                                    style = TerminalTypography.labelSmall
                                )
                            }
                            
                            if (isExpanded) {
                                Column(modifier = Modifier.padding(start = TerminalSpacing.md, bottom = TerminalSpacing.md)) {
                                    val methods = state.providerAuthMethods[providerId]
                                    
                                    if (state.authLoading && state.selectedProviderId == providerId) {
                                        Text("Loading auth methods...", color = TerminalColors.grey, style = TerminalTypography.labelSmall)
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
                                                height = 36.dp
                                            )
                                            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                                            Text("- OR -", color = TerminalColors.greyDark, style = TerminalTypography.labelSmall)
                                            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                                        }
                                        
                                        // Manual Key Input
                                        TerminalInput(
                                            value = manualKey,
                                            onValueChange = { manualKey = it },
                                            placeholder = "API_KEY",
                                            label = null
                                        )
                                        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                                        TerminalOutlinedButton(
                                            text = "SAVE KEY",
                                            onClick = { screenModel.setManualKey(providerId, manualKey) },
                                            enabled = manualKey.isNotBlank(),
                                            height = 32.dp
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = TerminalColors.borderLight, thickness = 0.5.dp)
                        }
                    }
                }

                // App Configuration Section
                // ... (rest remains same)
                item {
                    Spacer(modifier = Modifier.height(TerminalSpacing.lg))
                    Text(
                        text = "// APP_CONFIGURATION",
                        color = TerminalColors.grey,
                        style = TerminalTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                }

                item {
                    ModuleCard(title = "GLOBAL_DEFAULTS") {
                        var defaultModel by remember { mutableStateOf("") }
                        var defaultProvider by remember { mutableStateOf("") }
                        
                        TerminalInput(
                            value = defaultProvider,
                            onValueChange = { defaultProvider = it },
                            label = "DEFAULT_PROVIDER",
                            placeholder = "anthropic"
                        )
                        Spacer(modifier = Modifier.height(TerminalSpacing.md))
                        TerminalInput(
                            value = defaultModel,
                            onValueChange = { defaultModel = it },
                            label = "DEFAULT_MODEL",
                            placeholder = "claude-3-5-sonnet-latest"
                        )
                        
                        Spacer(modifier = Modifier.height(TerminalSpacing.lg))
                        
                        TerminalButton(
                            text = "SAVE_CONFIGURATION",
                            onClick = { 
                                screenModel.updateRemoteConfig(
                                    ConfigUpdate(
                                        provider = defaultProvider.ifBlank { null },
                                        model = if (defaultModel.isNotBlank()) ModelConfig(default = defaultModel) else null
                                    )
                                ) 
                            },
                            enabled = !state.isLoading && (defaultModel.isNotBlank() || defaultProvider.isNotBlank())
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
