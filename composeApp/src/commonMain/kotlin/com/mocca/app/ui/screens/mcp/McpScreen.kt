package com.mocca.app.ui.screens.mcp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.McpConnectionStatus
import com.mocca.app.domain.model.McpServerInfo
import com.mocca.app.ui.components.terminal.*
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography
import com.mocca.app.domain.model.McpServerConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class McpScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<McpScreenModel>()
        val state by screenModel.state.collectAsState()
        var showAddDialog by remember { mutableStateOf(false) }
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TerminalColors.background)
                    .padding(TerminalSpacing.lg)
            ) {
                // Header with Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TerminalIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { navigator.pop() },
                        iconColor = TerminalColors.white
                    )
                    
                    Spacer(modifier = Modifier.width(TerminalSpacing.md))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        TerminalHeader(text = "MCP_JSON_CONFIG", showBrackets = true)
                        Spacer(modifier = Modifier.height(TerminalSpacing.xs))
                        Text(
                            text = "${state.connectedCount}/${state.totalCount} SERVERS_ACTIVE",
                            color = if (state.connectedCount > 0) TerminalColors.statusOnline else TerminalColors.grey,
                            style = TerminalTypography.labelSmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
                        TerminalIconButton(
                            icon = Icons.Default.Refresh,
                            onClick = { screenModel.refresh() },
                            iconColor = if (state.isRefreshing) TerminalColors.statusWaiting else TerminalColors.greyLight
                        )
                        TerminalIconButton(
                            icon = Icons.Default.Add,
                            onClick = { showAddDialog = true },
                            iconColor = TerminalColors.statusOnline
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(TerminalSpacing.lg))
                
                // Error message
                state.error?.let { error ->
                    McpErrorBanner(
                        message = error,
                        onDismiss = { screenModel.clearError() }
                    )
                    Spacer(modifier = Modifier.height(TerminalSpacing.md))
                }
                
                // Loading state
                if (state.isLoading && !state.hasServers) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = TerminalColors.statusWaiting,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(TerminalSpacing.md))
                            Text(
                                text = "LOADING_MCP_STATUS...",
                                color = TerminalColors.grey,
                                style = TerminalTypography.bodyMedium
                            )
                        }
                    }
                } else if (!state.hasServers) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(TerminalSpacing.borderThin, TerminalColors.greyDark, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(TerminalSpacing.xl)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = TerminalColors.greyDark,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(TerminalSpacing.md))
                            Text(
                                text = "NO_MCP_SERVERS_FOUND",
                                color = TerminalColors.grey,
                                style = TerminalTypography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                            Text(
                                text = "Add a server to get started",
                                color = TerminalColors.greyDark,
                                style = TerminalTypography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
                            TerminalButton(
                                text = "ADD_SERVER",
                                onClick = { showAddDialog = true }
                            )
                        }
                    }
                } else {
                    // Server list
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
                    ) {
                        items(state.servers, key = { it.name }) { server ->
                            McpServerCard(
                                server = server,
                                isOperationInProgress = state.operationInProgress == server.name,
                                onToggle = { connect ->
                                    screenModel.toggleConnection(server.name, connect)
                                },
                                onClick = { screenModel.selectServer(server) }
                            )
                        }
                    }
                }
            }
            
            // Server Details Bottom Sheet / Dialog
            if (state.showServerDetails && state.selectedServer != null) {
                McpServerDetailsDialog(
                    server = state.selectedServer!!,
                    onDismiss = { screenModel.closeServerDetails() },
                    onConnect = { screenModel.connect(state.selectedServer!!.name) },
                    onDisconnect = { screenModel.disconnect(state.selectedServer!!.name) }
                )
            }
            
            // Add Server Dialog
            if (showAddDialog) {
                AddMcpServerDialog(
                    onDismiss = { showAddDialog = false },
                    onAdd = { name, config ->
                        screenModel.addServer(name, config)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun McpServerCard(
    server: McpServerInfo,
    isOperationInProgress: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val statusColor = when {
        server.status.isConnected -> TerminalColors.statusOnline
        server.status.needsAuth -> TerminalColors.statusWaiting
        server.status.hasFailed -> TerminalColors.error
        else -> TerminalColors.grey
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalColors.surfaceVariant.copy(alpha = 0.5f))
            .border(TerminalSpacing.borderThin, statusColor.copy(alpha = 0.3f), RectangleShape)
            .clickable(onClick = onClick)
            .padding(TerminalSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Code-like syntax for the server name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "{ ",
                    color = TerminalColors.grey,
                    style = TerminalTypography.code
                )
                Text(
                    text = "\"${server.name}\"",
                    color = TerminalColors.syntaxString,
                    style = TerminalTypography.code,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = " }",
                    color = TerminalColors.grey,
                    style = TerminalTypography.code
                )
            }
            
            // Toggle or loading
            if (isOperationInProgress) {
                CircularProgressIndicator(
                    color = TerminalColors.statusWaiting,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                TerminalToggle(
                    checked = server.isConnected,
                    onCheckedChange = onToggle
                )
            }
        }
        
        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
        
        // Metadata Row (JSON-style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.lg)
        ) {
            McpMetaField(label = "type", value = server.displayType, color = TerminalColors.syntaxKeyword)
            if (server.toolCount > 0) {
                McpMetaField(label = "tools", value = server.toolCount.toString(), color = TerminalColors.syntaxFunction)
            }
            McpMetaField(label = "status", value = getStatusText(server.status.status), color = statusColor)
        }
        
        // Error message if any
        server.status.error?.let { error ->
            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalColors.error.copy(alpha = 0.05f))
                    .padding(TerminalSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "!! error: ",
                    color = TerminalColors.error,
                    style = TerminalTypography.codeSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "\"$error\"",
                    color = TerminalColors.error.copy(alpha = 0.8f),
                    style = TerminalTypography.codeSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun McpMetaField(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            color = TerminalColors.grey,
            style = TerminalTypography.codeSmall
        )
        Text(
            text = value,
            color = color,
            style = TerminalTypography.codeSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun McpErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalColors.error.copy(alpha = 0.1f))
            .border(TerminalSpacing.borderThin, TerminalColors.error, RectangleShape)
            .padding(TerminalSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = TerminalColors.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(TerminalSpacing.sm))
        Text(
            text = message,
            color = TerminalColors.error,
            style = TerminalTypography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = TerminalColors.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun McpServerDetailsDialog(
    server: McpServerInfo,
    onDismiss: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalColors.background.copy(alpha = 0.95f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .background(TerminalColors.background)
                .border(TerminalSpacing.borderStandard, TerminalColors.borderLight, RectangleShape)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TerminalSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = server.name.uppercase(),
                        color = TerminalColors.white,
                        style = TerminalTypography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${server.displayType} | ${getStatusText(server.status.status)}",
                        color = TerminalColors.grey,
                        style = TerminalTypography.labelSmall
                    )
                }
                
                TerminalIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    iconColor = TerminalColors.greyLight
                )
            }
            
            HorizontalDivider(
                thickness = TerminalSpacing.borderThin,
                color = TerminalColors.border
            )
            
            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(TerminalSpacing.md)
            ) {
                // Tools section
                server.status.tools?.takeIf { it.isNotEmpty() }?.let { tools ->
                    Text(
                        text = "// TOOLS (${server.toolCount})",
                        color = TerminalColors.statusOnline,
                        style = TerminalTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                    
                    tools.forEach { tool ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = TerminalSpacing.xs),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = TerminalColors.greyLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
                            Column {
                                Text(
                                    text = tool.name,
                                    color = TerminalColors.white,
                                    style = TerminalTypography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                tool.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        color = TerminalColors.grey,
                                        style = TerminalTypography.labelSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(TerminalSpacing.lg))
                }
                
                // Resources section
                server.status.resources?.takeIf { it.isNotEmpty() }?.let { resources ->
                    Text(
                        text = "// RESOURCES (${server.resourceCount})",
                        color = TerminalColors.statusWaiting,
                        style = TerminalTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                    
                    resources.forEach { resource ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = TerminalSpacing.xs),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = TerminalColors.greyLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
                            Column {
                                Text(
                                    text = resource.name,
                                    color = TerminalColors.white,
                                    style = TerminalTypography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = resource.uri,
                                    color = TerminalColors.grey,
                                    style = TerminalTypography.labelSmall
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(TerminalSpacing.lg))
                }
                
                // Prompts section
                server.status.prompts?.takeIf { it.isNotEmpty() }?.let { prompts ->
                    Text(
                        text = "// PROMPTS (${server.promptCount})",
                        color = TerminalColors.whiteDim,
                        style = TerminalTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                    
                    prompts.forEach { prompt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = TerminalSpacing.xs),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = TerminalColors.greyLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
                            Column {
                                Text(
                                    text = prompt.name,
                                    color = TerminalColors.white,
                                    style = TerminalTypography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                prompt.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        color = TerminalColors.grey,
                                        style = TerminalTypography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Empty state if no capabilities
                if (server.toolCount == 0 && server.resourceCount == 0 && server.promptCount == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(TerminalSpacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (server.isConnected) "NO_CAPABILITIES_EXPOSED" else "CONNECT_TO_VIEW_CAPABILITIES",
                            color = TerminalColors.grey,
                            style = TerminalTypography.bodyMedium
                        )
                    }
                }
            }
            
            HorizontalDivider(
                thickness = TerminalSpacing.borderThin,
                color = TerminalColors.border
            )
            
            // Footer actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TerminalSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
            ) {
                TerminalOutlinedButton(
                    text = "CLOSE",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                
                if (server.isConnected) {
                    TerminalButton(
                        text = "DISCONNECT",
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    TerminalButton(
                        text = "CONNECT",
                        onClick = onConnect,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMcpServerDialog(
    onDismiss: () -> Unit,
    onAdd: (String, McpServerConfig) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var args by remember { mutableStateOf("") }
    var env by remember { mutableStateOf("") }
    
    // Default to stdio for now as it's the most common for local
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalColors.background.copy(alpha = 0.9f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(TerminalColors.background, RectangleShape)
                .border(TerminalSpacing.borderStandard, TerminalColors.borderLight, RectangleShape)
                .padding(TerminalSpacing.lg)
        ) {
            TerminalHeader(text = "ADD_MCP_SERVER", showBrackets = true)
            
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            
            TerminalInput(
                value = name,
                onValueChange = { name = it },
                label = "SERVER_NAME",
                placeholder = "weather-server"
            )
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            TerminalInput(
                value = command,
                onValueChange = { command = it },
                label = "COMMAND",
                placeholder = "npx",
                hint = "Executable to run"
            )
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            TerminalInput(
                value = args,
                onValueChange = { args = it },
                label = "ARGUMENTS",
                placeholder = "-y @modelcontextprotocol/server-weather",
                hint = "Space separated arguments"
            )
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            TerminalInput(
                value = env,
                onValueChange = { env = it },
                label = "ENVIRONMENT",
                placeholder = "KEY=VALUE,KEY2=VALUE2",
                hint = "Comma separated env vars (optional)"
            )
            
            Spacer(modifier = Modifier.height(TerminalSpacing.xl))
            
            Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)) {
                TerminalOutlinedButton(
                    text = "CANCEL",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                TerminalButton(
                    text = "ADD",
                    onClick = {
                        val argsList = args.split(" ").filter { it.isNotBlank() }
                        val envMap = if (env.isNotBlank()) {
                            env.split(",").associate { 
                                val parts = it.split("=", limit = 2)
                                (parts.getOrNull(0) ?: "") to (parts.getOrNull(1) ?: "")
                            }
                        } else emptyMap()
                        
                        val config = McpServerConfig(
                            command = listOf(command) + argsList,
                            environment = envMap
                        )
                        onAdd(name, config)
                    },
                    enabled = name.isNotBlank() && command.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Get human-readable status text.
 */
private fun getStatusText(status: McpConnectionStatus): String {
    return when (status) {
        McpConnectionStatus.CONNECTED -> "CONNECTED"
        McpConnectionStatus.DISCONNECTED -> "OFFLINE"
        McpConnectionStatus.CONNECTING -> "CONNECTING"
        McpConnectionStatus.DISCONNECTING -> "DISCONNECTING"
        McpConnectionStatus.FAILED -> "FAILED"
        McpConnectionStatus.NEEDS_AUTH -> "AUTH_REQUIRED"
        McpConnectionStatus.NEEDS_CLIENT_REGISTRATION -> "REGISTRATION_REQUIRED"
        McpConnectionStatus.DISABLED -> "DISABLED"
    }
}