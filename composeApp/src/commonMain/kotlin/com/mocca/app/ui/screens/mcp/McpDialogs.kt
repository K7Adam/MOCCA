package com.mocca.app.ui.screens.mcp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.McpServerConfig
import com.mocca.app.domain.model.McpServerInfo
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
internal fun McpServerDetailsDialog(
    server: McpServerInfo,
    onDismiss: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onStartOAuth: () -> Unit,
    onViewResources: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background.copy(alpha = 0.95f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .background(AppColors.background)
                .border(AppSpacing.borderStandard, AppColors.borderLight, AppShapes.medium)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = server.name.uppercase(),
                        color = AppColors.white,
                        style = AppTypography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${server.displayType} | ${getStatusText(server.status.status)}",
                        color = AppColors.grey,
                        style = AppTypography.labelSmall
                    )
                }
                
                MoccaIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    iconColor = AppColors.greyLight
                )
            }
            
            HorizontalDivider(
                thickness = AppSpacing.borderThin,
                color = AppColors.border
            )
            
            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(AppSpacing.md)
            ) {
                // Tools section
                server.status.tools?.takeIf { it.isNotEmpty() }?.let { tools ->
                    Text(
                        text = "// TOOLS (${server.toolCount})",
                        color = AppColors.statusOnline,
                        style = AppTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    
                    tools.forEach { tool ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppSpacing.xs),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = AppColors.greyLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Column {
                                Text(
                                    text = tool.name,
                                    color = AppColors.white,
                                    style = AppTypography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                tool.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        color = AppColors.grey,
                                        style = AppTypography.labelSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                }
                
                // Resources section
                server.status.resources?.takeIf { it.isNotEmpty() }?.let { resources ->
                    Text(
                        text = "// RESOURCES (${server.resourceCount})",
                        color = AppColors.statusWaiting,
                        style = AppTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    
                    resources.forEach { resource ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppSpacing.xs),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = AppColors.greyLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Column {
                                Text(
                                    text = resource.name,
                                    color = AppColors.white,
                                    style = AppTypography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = resource.uri,
                                    color = AppColors.grey,
                                    style = AppTypography.labelSmall
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                }
                
                // Prompts section
                server.status.prompts?.takeIf { it.isNotEmpty() }?.let { prompts ->
                    Text(
                        text = "// PROMPTS (${server.promptCount})",
                        color = AppColors.whiteDim,
                        style = AppTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    
                    prompts.forEach { prompt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppSpacing.xs),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = AppColors.greyLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Column {
                                Text(
                                    text = prompt.name,
                                    color = AppColors.white,
                                    style = AppTypography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                prompt.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        color = AppColors.grey,
                                        style = AppTypography.labelSmall
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
                            .padding(AppSpacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (server.isConnected) "NO_CAPABILITIES_EXPOSED" else "CONNECT_TO_VIEW_CAPABILITIES",
                            color = AppColors.grey,
                            style = AppTypography.bodyMedium
                        )
                    }
                }
            }
            
            HorizontalDivider(
                thickness = AppSpacing.borderThin,
                color = AppColors.border
            )
            
            // Footer actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                // Resources button (only when connected and has resources)
                if (server.isConnected && server.resourceCount > 0) {
                    MoccaOutlinedButton(
                        text = "VIEW_RESOURCES (${server.resourceCount})",
                        onClick = onViewResources,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                    MoccaOutlinedButton(
                        text = "CLOSE",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    if (server.isConnected) {
                        MoccaButton(
                            text = "DISCONNECT",
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f)
                        )
                    } else if (server.status.needsAuth) {
                        MoccaButton(
                            text = "AUTHORIZE",
                            onClick = onStartOAuth,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        MoccaButton(
                            text = "CONNECT",
                            onClick = onConnect,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddMcpServerDialog(
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
            .background(AppColors.background.copy(alpha = 0.9f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(AppColors.background, AppShapes.medium)
                .border(AppSpacing.borderStandard, AppColors.borderLight, AppShapes.medium)
                .padding(AppSpacing.lg)
        ) {
            ModernHeader(text = "ADD_MCP_SERVER")
            
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            
            MoccaInput(
                value = name,
                onValueChange = { name = it },
                label = "SERVER_NAME",
                placeholder = "weather-server"
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            MoccaInput(
                value = command,
                onValueChange = { command = it },
                label = "COMMAND",
                placeholder = "npx",
                hint = "Executable to run"
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            MoccaInput(
                value = args,
                onValueChange = { args = it },
                label = "ARGUMENTS",
                placeholder = "-y @modelcontextprotocol/server-weather",
                hint = "Space separated arguments"
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            MoccaInput(
                value = env,
                onValueChange = { env = it },
                label = "ENVIRONMENT",
                placeholder = "KEY=VALUE,KEY2=VALUE2",
                hint = "Comma separated env vars (optional)"
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.xl))
            
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                MoccaOutlinedButton(
                    text = "CANCEL",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                MoccaButton(
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
