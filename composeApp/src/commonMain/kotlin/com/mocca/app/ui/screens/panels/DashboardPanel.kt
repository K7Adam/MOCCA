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
import androidx.compose.material.icons.filled.Terminal
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
    onTerminalClick: () -> Unit = {},
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
            .background(AppColors.background)
            .padding(AppSpacing.lg)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
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
            MoccaIconButton(
                icon = Icons.Default.Terminal,
                onClick = onTerminalClick,
                contentDescription = "Terminal",
                iconColor = AppColors.accentGreen
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom padding for floating bottom bar clearance
        Spacer(modifier = Modifier.height(80.dp))
    }
}

/**
 * WORKSPACE module — merged Projects + Agents into a single card.
 */
@Composable
private fun WorkspaceModule(
    currentProject: Resource<com.mocca.app.domain.model.Project>,
    projects: Resource<List<com.mocca.app.domain.model.Project>>,
    agents: Resource<List<com.mocca.app.domain.model.Agent>>
) {
    val currentId = (currentProject as? Resource.Success)?.data?.id
    
    ModuleCard(title = "WORKSPACE") {
        // Current project (compact: just the active one)
        when (projects) {
            is Resource.Loading -> {
                ModuleRowItem(
                    title = "Loading projects...",
                    subtitle = "",
                    isEnabled = false,
                    showToggle = false
                )
            }
            is Resource.Success -> {
                val active = projects.data.find { it.id == currentId } ?: projects.data.firstOrNull()
                if (active != null) {
                    ModuleRowItem(
                        title = active.displayName,
                        subtitle = active.path ?: active.directory ?: "Active project",
                        isEnabled = true,
                        isConnected = true,
                        showToggle = false
                    )
                }
                if (projects.data.size > 1) {
                    ModuleRowItem(
                        title = "+${projects.data.size - 1} more projects",
                        subtitle = "",
                        isEnabled = true,
                        showToggle = false
                    )
                }
            }
            is Resource.Error -> {
                ModuleRowItem(
                    title = "Error",
                    subtitle = projects.message,
                    isEnabled = false,
                    isConnected = false,
                    showToggle = false
                )
            }
        }
        
        // Agents (compact: count + top names)
        when (agents) {
            is Resource.Loading -> {
                ModuleRowItem(
                    title = "Loading agents...",
                    subtitle = "",
                    isEnabled = false,
                    showToggle = false
                )
            }
            is Resource.Success -> {
                if (agents.data.isNotEmpty()) {
                    val agentNames = agents.data.take(4).joinToString(", ") { it.name }
                    val suffix = if (agents.data.size > 4) " +${agents.data.size - 4}" else ""
                    ModuleRowItem(
                        title = "${agents.data.size} AGENTS",
                        subtitle = "$agentNames$suffix",
                        isEnabled = true,
                        isConnected = true,
                        showToggle = false
                    )
                } else {
                    ModuleRowItem(
                        title = "No agents",
                        subtitle = "Configure in opencode",
                        isEnabled = false,
                        showToggle = false
                    )
                }
            }
            is Resource.Error -> {
                ModuleRowItem(
                    title = "Error",
                    subtitle = agents.message,
                    isEnabled = false,
                    isConnected = false,
                    showToggle = false
                )
            }
        }
    }
}

/**
 * CAPABILITIES module — merged Tools + Commands into a single card.
 */
@Composable
private fun CapabilitiesModule(
    tools: Resource<List<String>>,
    commands: Resource<List<com.mocca.app.domain.model.Command>>
) {
    ModuleCard(title = "CAPABILITIES") {
        // Tools row (compact summary)
        when (tools) {
            is Resource.Loading -> {
                ModuleRowItem(
                    title = "Loading tools...",
                    subtitle = "",
                    isEnabled = false,
                    showToggle = false
                )
            }
            is Resource.Success -> {
                val preview = tools.data.take(3).joinToString(", ").ifEmpty { "None" }
                ModuleRowItem(
                    title = "${tools.data.size} TOOLS",
                    subtitle = preview,
                    isEnabled = true,
                    isConnected = true,
                    showToggle = false
                )
            }
            is Resource.Error -> {
                ModuleRowItem(
                    title = "Tools error",
                    subtitle = tools.message,
                    isEnabled = false,
                    isConnected = false,
                    showToggle = false
                )
            }
        }
        
        // Commands row (compact summary)
        when (commands) {
            is Resource.Loading -> {
                ModuleRowItem(
                    title = "Loading commands...",
                    subtitle = "",
                    isEnabled = false,
                    showToggle = false
                )
            }
            is Resource.Success -> {
                if (commands.data.isNotEmpty()) {
                    val preview = commands.data.take(3).joinToString(", ") { "/${it.name}" }
                    val suffix = if (commands.data.size > 3) " +${commands.data.size - 3}" else ""
                    ModuleRowItem(
                        title = "${commands.data.size} COMMANDS",
                        subtitle = "$preview$suffix",
                        isEnabled = true,
                        isConnected = true,
                        showToggle = false
                    )
                } else {
                    ModuleRowItem(
                        title = "No commands",
                        subtitle = "No slash commands available",
                        isEnabled = false,
                        showToggle = false
                    )
                }
            }
            is Resource.Error -> {
                ModuleRowItem(
                    title = "Commands error",
                    subtitle = commands.message,
                    isEnabled = false,
                    isConnected = false,
                    showToggle = false
                )
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
