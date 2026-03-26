package com.mocca.app.ui.screens.mcp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

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
                    .background(AppColors.background)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(AppSpacing.lg)
            ) {
                // Header with Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.surfaceContainer, AppShapes.none),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoccaIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { navigator.pop() },
                        iconColor = AppColors.onSurface,
                        contentDescription = "Go back"
                    )
                    
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        ModernHeader(text = "MCP JSON config")
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text(
                            text = "${state.connectedCount}/${state.totalCount} servers active",
                            color = if (state.connectedCount > 0) AppColors.statusOnline else AppColors.onSurfaceVariant,
                            style = AppTypography.labelSmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        MoccaIconButton(
                            icon = Icons.Default.Refresh,
                            onClick = { screenModel.refresh() },
                            iconColor = if (state.isRefreshing) AppColors.statusWaiting else AppColors.onSurfaceVariantLight,
                            contentDescription = "Refresh MCP servers"
                        )
                        MoccaIconButton(
                            icon = Icons.Default.Add,
                            onClick = { showAddDialog = true },
                            iconColor = AppColors.primary,
                            contentDescription = "Add MCP server"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                
                // Error message
                state.error?.let { error ->
                    McpErrorBanner(
                        message = error,
                        onDismiss = { screenModel.clearError() }
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                }
                
                // Loading state
                if (state.isLoading && !state.hasServers) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LoadingIndicator(
                                color = AppColors.statusWaiting,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.md))
                            Text(
                                text = "Loading MCP status...",
                                color = AppColors.onSurfaceVariant,
                                style = AppTypography.bodyMedium
                            )
                        }
                    }
                } else if (!state.hasServers) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(AppSpacing.borderThin, AppColors.onSurfaceVariantDark, AppShapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(AppSpacing.xl)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = AppColors.onSurfaceVariantDark,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.md))
                            Text(
                                text = "No MCP servers found",
                                color = AppColors.onSurfaceVariant,
                                style = AppTypography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                            Text(
                                text = "Add a server to get started",
                                color = AppColors.onSurfaceVariantDark,
                                style = AppTypography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.lg))
                            MoccaButton(
                                text = "Add server",
                                onClick = { showAddDialog = true }
                            )
                        }
                    }
                } else {
                    // Server list
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
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
                    onDisconnect = { screenModel.disconnect(state.selectedServer!!.name) },
                    onStartOAuth = { screenModel.startOAuthFlow(state.selectedServer!!.name) },
                    onViewResources = {
                        screenModel.closeServerDetails()
                        navigator.push(McpResourceScreen(state.selectedServer!!.name))
                    }
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

            // OAuth Dialog
            if (state.showOAuthDialog && state.pendingOAuth != null) {
                McpOAuthDialog(
                    oauthState = state.pendingOAuth!!,
                    isInProgress = state.isOAuthInProgress,
                    onSubmitCode = { code -> screenModel.submitOAuthCode(code) },
                    onDismiss = { screenModel.dismissOAuthDialog() }
                )
            }
        }
    }
}