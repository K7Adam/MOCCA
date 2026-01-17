package com.mocca.app.ui.screens.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import com.mocca.app.domain.model.Resource
import com.mocca.app.ui.components.terminal.GitStatusModule
import com.mocca.app.ui.components.terminal.McpConfigModule
import com.mocca.app.ui.components.terminal.McpServerItem
import com.mocca.app.ui.components.terminal.ModuleCard
import com.mocca.app.ui.components.terminal.ModuleRowItem
import com.mocca.app.ui.components.terminal.SkillItem
import com.mocca.app.ui.components.terminal.SkillsEngineModule
import com.mocca.app.ui.components.terminal.TerminalOutlinedButton
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing

/**
 * Right swipe panel: Modular tools dashboard.
 * Matches mockup: mockups_screens/modular_tools_dashboard/screen.png
 * 
 * This version uses DashboardScreenModel for real data.
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
) {
    val state by screenModel.state.collectAsState()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalColors.background)
            .padding(TerminalSpacing.lg)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.lg)
    ) {
        // MCP Config Module
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
        
        // Git Status Module
        GitStatusModule(
            branchName = state.gitBranch,
            changedFiles = state.gitChangeCount,
            onExpandClick = onGitClick
        )
        
        // Agents Module (new - shows available agents)
        AgentsModule(
            agents = state.agents,
            onAgentClick = { /* Could navigate to agent detail */ }
        )
        
        // Tools Module (new - shows available tools count)
        ToolsModule(
            tools = state.tools
        )
        
        // Commands Module (new - shows slash commands)
        CommandsModule(
            commands = state.commands,
            onCommandClick = { /* Could insert command into chat */ }
        )
        
        // Skills Engine Module (using commands as skills for now)
        val skills = state.commands.toSkillItems()
        if (skills.isNotEmpty()) {
            SkillsEngineModule(
                skills = skills,
                onFilterClick = onSkillsClick,
                onSkillClick = onSkillClick
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Settings Button
        TerminalOutlinedButton(
            text = "[SETTINGS]",
            onClick = onSettingsClick,
            showBrackets = false,
            icon = Icons.Default.Settings,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
        
        // Open Terminal Button
        TerminalOutlinedButton(
            text = ">_ OPEN_TERMINAL",
            onClick = onTerminalClick,
            showBrackets = false,
            icon = Icons.Default.Terminal,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Agents Module - displays available AI agents
 */
@Composable
private fun AgentsModule(
    agents: Resource<List<com.mocca.app.domain.model.Agent>>,
    onAgentClick: (String) -> Unit = {}
) {
    ModuleCard(title = "AGENTS") {
        when (agents) {
            is Resource.Loading -> {
                ModuleRowItem(
                    title = "Loading...",
                    subtitle = "Fetching agents",
                    isEnabled = false,
                    showToggle = false
                )
            }
            is Resource.Success -> {
                if (agents.data.isEmpty()) {
                    ModuleRowItem(
                        title = "No agents configured",
                        subtitle = "Add agents in opencode config",
                        isEnabled = false,
                        showToggle = false
                    )
                } else {
                    agents.data.take(5).forEach { agent ->
                        ModuleRowItem(
                            title = agent.name,
                            subtitle = agent.description ?: "Agent",
                            isEnabled = true,
                            isConnected = true,
                            showToggle = false,
                            onClick = { onAgentClick(agent.id) }
                        )
                    }
                    if (agents.data.size > 5) {
                        ModuleRowItem(
                            title = "+${agents.data.size - 5} more",
                            subtitle = "agents available",
                            isEnabled = true,
                            showToggle = false
                        )
                    }
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
 * Tools Module - displays available tools count
 */
@Composable
private fun ToolsModule(
    tools: Resource<List<String>>
) {
    ModuleCard(title = "TOOLS") {
        when (tools) {
            is Resource.Loading -> {
                ModuleRowItem(
                    title = "Loading...",
                    subtitle = "Fetching tools",
                    isEnabled = false,
                    showToggle = false
                )
            }
            is Resource.Success -> {
                ModuleRowItem(
                    title = "${tools.data.size} tools available",
                    subtitle = tools.data.take(3).joinToString(", ").ifEmpty { "No tools" },
                    isEnabled = true,
                    isConnected = true,
                    showToggle = false
                )
            }
            is Resource.Error -> {
                ModuleRowItem(
                    title = "Error",
                    subtitle = tools.message,
                    isEnabled = false,
                    isConnected = false,
                    showToggle = false
                )
            }
        }
    }
}

/**
 * Commands Module - displays slash commands
 */
@Composable
private fun CommandsModule(
    commands: Resource<List<com.mocca.app.domain.model.Command>>,
    onCommandClick: (String) -> Unit = {}
) {
    ModuleCard(title = "COMMANDS") {
        when (commands) {
            is Resource.Loading -> {
                ModuleRowItem(
                    title = "Loading...",
                    subtitle = "Fetching commands",
                    isEnabled = false,
                    showToggle = false
                )
            }
            is Resource.Success -> {
                if (commands.data.isEmpty()) {
                    ModuleRowItem(
                        title = "No commands",
                        subtitle = "No slash commands available",
                        isEnabled = false,
                        showToggle = false
                    )
                } else {
                    commands.data.take(5).forEach { cmd ->
                        ModuleRowItem(
                            title = "/${cmd.name}",
                            subtitle = cmd.description ?: "",
                            isEnabled = true,
                            isConnected = true,
                            showToggle = false,
                            onClick = { onCommandClick(cmd.name) }
                        )
                    }
                    if (commands.data.size > 5) {
                        ModuleRowItem(
                            title = "+${commands.data.size - 5} more",
                            subtitle = "commands available",
                            isEnabled = true,
                            showToggle = false
                        )
                    }
                }
            }
            is Resource.Error -> {
                ModuleRowItem(
                    title = "Error",
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

/**
 * Convert commands to skill items for SkillsEngineModule
 */
private fun Resource<List<com.mocca.app.domain.model.Command>>.toSkillItems(): List<SkillItem> {
    return when (this) {
        is Resource.Success -> data.take(8).map { cmd ->
            SkillItem(
                id = cmd.name,
                name = cmd.name,
                isActive = true
            )
        }
        is Resource.Loading -> emptyList()
        is Resource.Error -> emptyList()
    }
}
