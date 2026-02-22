package com.mocca.app.ui.screens.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Resource
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontWeight
import com.mocca.app.ui.theme.AppTypography

/**
 * Right swipe panel: Modular tools dashboard.
 * Compact design: merged modules to reduce vertical space.
 */
@Composable
fun DashboardPanel(
    screenModel: DashboardScreenModel,
    modifier: Modifier = Modifier,
    // Navigation callbacks
    onSettingsClick: () -> Unit = {},
    onFilesClick: () -> Unit = {},
    onGitClick: () -> Unit = {},
    onMcpConfigClick: () -> Unit = {},
    onSkillsClick: () -> Unit = {},
    onSkillClick: (String) -> Unit = {}
    // NOTE: onRefreshAll removed - SSE drives all live state, no manual refresh needed
) {
    val state by screenModel.state.collectAsState()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.lg)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        // ─── SYNC STATUS ───────────────────────────────────────────────────
        SyncStatusCard(
            globalSyncState = state.globalSyncState,
            repoSyncStates = state.repoSyncStates,
            onRefreshClick = { screenModel.forceFullSync() }
        )
        
        // ─── SSE STATUS INDICATOR ─────────────────────────────────────────
        // Show SSE connection status for real-time event streaming
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REAL-TIME EVENTS",
                style = AppTypography.labelSmall,
                color = AppColors.textSecondary,
                fontWeight = FontWeight.Bold
            )
            SseStatusIndicator(isConnected = state.isSseConnected)
        }
        
        // ─── MCP Servers ─────────────────────────────────────────────────
        McpConfigModule(
            servers = state.mcpServers.toMcpServerItems(),
            onConfigClick = onMcpConfigClick,
            onServerToggle = { name, connect ->
                if (connect) {
                    screenModel.connectMcpServer(name)
                } else {
                    screenModel.disconnectMcpServer(name)
                }
            }
        )
        
        // ─── Git Status ──────────────────────────────────────────────────
        GitStatusModule(
            branchName = state.gitBranch,
            changedFiles = state.gitChangeCount,
            onExpandClick = onGitClick
        )
        
        // ─── WORKSPACE (Projects + Agents merged) ────────────────────────
        WorkspaceModule(
            currentProject = state.currentProject,
            projects = state.projects,
            agents = state.agents
        )
        
        // ─── CAPABILITIES (Tools + Commands merged) ──────────────────────
        CapabilitiesModule(
            tools = state.tools,
            commands = state.commands
        )
        
        // ─── Quick Actions (centered at bottom) ───────────────────────────────
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg, Alignment.CenterHorizontally)
        ) {
            MoccaIconButton(
                icon = Icons.Default.Settings,
                onClick = onSettingsClick,
                contentDescription = "Settings",
                iconColor = AppColors.white
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom padding for floating bottom bar clearance
        Spacer(modifier = Modifier.height(80.dp))
    }
}

/**
 * WORKSPACE module — merged Projects + Agents into a single card with God Mode aesthetics.
 */
@Composable
private fun WorkspaceModule(
    currentProject: Resource<com.mocca.app.domain.model.Project>,
    projects: Resource<List<com.mocca.app.domain.model.Project>>,
    agents: Resource<List<com.mocca.app.domain.model.Agent>>
) {
    val currentId = (currentProject as? Resource.Success)?.data?.id
    
    ModuleCard(title = "WORKSPACE") {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Projects
            when (projects) {
                is Resource.Loading -> Text("Bootstrapping workspace...", color = AppColors.textTertiary, style = AppTypography.labelMedium)
                is Resource.Success -> {
                    val active = projects.data.find { it.id == currentId } ?: projects.data.firstOrNull()
                    if (active != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(color = AppColors.accentGreen)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(active.displayName.uppercase(), color = AppColors.white, style = AppTypography.labelLarge, fontWeight = FontWeight.Black)
                                Text(active.path ?: active.directory ?: "/", color = AppColors.textSecondary, style = AppTypography.labelSmall)
                            }
                        }
                    }
                    if (projects.data.size > 1) {
                        Text("+${projects.data.size - 1} standby environments", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                    }
                }
                is Resource.Error -> Text("ENV INIT FAILED", color = AppColors.error, style = AppTypography.labelMedium)
            }
            
            HorizontalDivider(color = AppColors.border.copy(alpha = 0.5f))
            
            // Agents
            when (agents) {
                is Resource.Loading -> Text("Loading agents...", color = AppColors.textTertiary, style = AppTypography.labelMedium)
                is Resource.Success -> {
                    if (agents.data.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${agents.data.size} ACTIVE AGENTS", color = AppColors.textSecondary, style = AppTypography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(agents.data.joinToString(" • ") { it.name.uppercase() }, color = AppColors.white, style = AppTypography.bodySmall, maxLines = 2)
                    } else {
                        Text("NO AGENTS ONLINE", color = AppColors.textTertiary, style = AppTypography.labelMedium)
                    }
                }
                is Resource.Error -> Text("AGENT LINK FAILED", color = AppColors.error, style = AppTypography.labelMedium)
            }
        }
    }
}

/**
 * CAPABILITIES module — merged Tools + Commands into a single card with refined aesthetics.
 */
@Composable
private fun CapabilitiesModule(
    tools: Resource<List<String>>,
    commands: Resource<List<com.mocca.app.domain.model.Command>>
) {
    ModuleCard(title = "CAPABILITIES") {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Tools Column
            Column(modifier = Modifier.weight(1f)) {
                Text("INTEGRATED TOOLS", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                Spacer(Modifier.height(4.dp))
                when (tools) {
                    is Resource.Loading -> Text("SCANNING...", color = AppColors.textSecondary, style = AppTypography.bodySmall)
                    is Resource.Success -> {
                        Text("${tools.data.size} SYSTEM TOOLS", color = AppColors.accentGreen, style = AppTypography.labelLarge, fontWeight = FontWeight.Bold)
                        val preview = tools.data.take(3).joinToString("\n") { "• $it" }
                        if (preview.isNotEmpty()) {
                            Text(preview, color = AppColors.white, style = AppTypography.bodySmall, maxLines = 3)
                        }
                    }
                    is Resource.Error -> Text("OFFLINE", color = AppColors.error, style = AppTypography.labelLarge)
                }
            }
            
            // Commands Column
            Column(modifier = Modifier.weight(1f)) {
                Text("SLASH COMMANDS", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                Spacer(Modifier.height(4.dp))
                when (commands) {
                    is Resource.Loading -> Text("SCANNING...", color = AppColors.textSecondary, style = AppTypography.bodySmall)
                    is Resource.Success -> {
                        Text("${commands.data.size} AVAILABLE", color = AppColors.accentGreen, style = AppTypography.labelLarge, fontWeight = FontWeight.Bold)
                        val preview = commands.data.take(3).joinToString("\n") { "• /${it.name}" }
                        if (preview.isNotEmpty()) {
                            Text(preview, color = AppColors.white, style = AppTypography.bodySmall, maxLines = 3)
                        }
                    }
                    is Resource.Error -> Text("OFFLINE", color = AppColors.error, style = AppTypography.labelLarge)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS FOR DATA CONVERSION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Convert MCP server status to UI items
 */
private fun Resource<Map<String, com.mocca.app.domain.model.McpServerStatus>>.toMcpServerItems(): List<McpServerItem> {
    return when (this) {
        is Resource.Success -> data.map { (name, status) ->
            McpServerItem(
                id = name,
                name = name,
                type = if (status.isConnected) "CONNECTED" else "DISCONNECTED",
                isEnabled = status.isEnabled,
                isConnected = status.isConnected,
                isTransitioning = status.isTransitioning
            )
        }
        is Resource.Loading -> data?.map { (name, status) ->
            McpServerItem(
                id = name,
                name = name,
                type = if (status.isConnected) "CONNECTED" else "DISCONNECTED",
                isEnabled = status.isEnabled,
                isConnected = status.isConnected,
                isTransitioning = status.isTransitioning
            )
        } ?: emptyList()
        is Resource.Error -> emptyList()
    }
}
