package com.mocca.app.ui.screens.panels

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Resource
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.GridView
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.text.font.FontWeight
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.TestTags
import androidx.compose.ui.platform.testTag

/**
 * Right swipe panel: Modular tools dashboard.
 * Compact design: merged modules to reduce vertical space.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
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
    onSkillClick: (String) -> Unit = {},
    onTerminalClick: () -> Unit = {}
) {
    val state by screenModel.state.collectAsState()
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    val headerModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "nav_item_RIGHT"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        Modifier
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg),
        contentPadding = PaddingValues(bottom = AppSpacing.bottomBarClearance + AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        item(key = "dashboard-header", contentType = "dashboard-header") {
            PanelHeader(
                title = "Dashboard",
                modifier = headerModifier,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(AppShapes.medium)
                            .background(AppColors.surfaceContainer, AppShapes.medium)
                            .border(AppSpacing.borderThin, AppColors.outline, AppShapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = AppColors.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }

        item(key = "dashboard-realtime", contentType = "dashboard-status") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Real-time events",
                    style = AppTypography.labelSmall,
                    color = AppColors.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                SseStatusIndicator(isConnected = state.isSseConnected)
            }
        }

        item(key = "dashboard-mcp", contentType = "dashboard-module") {
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
        }

        item(key = "dashboard-git", contentType = "dashboard-module") {
            GitStatusModule(
                branchName = state.gitBranch,
                changedFiles = state.changedFilesCount,
                onExpandClick = onGitClick
            )
        }

        item(key = "dashboard-workspace", contentType = "dashboard-module") {
            WorkspaceModule(
                currentProject = state.currentProject,
                projects = state.projects,
                agents = state.agents
            )
        }

        item(key = "dashboard-capabilities", contentType = "dashboard-module") {
            CapabilitiesModule(
                tools = state.tools,
                commands = state.commands
            )
        }

        item(key = "dashboard-process", contentType = "dashboard-module") {
            ProcessModule(
                processes = state.systemMonitor.processes,
                hasActiveSession = state.systemMonitor.isAvailable,
                isRefreshing = state.systemMonitor.isRefreshing,
                lastUpdatedAt = state.systemMonitor.lastUpdatedAt
            )
        }

        item(key = "dashboard-ports", contentType = "dashboard-module") {
            PortModule(
                ports = state.systemMonitor.ports,
                hasActiveSession = state.systemMonitor.isAvailable,
                isRefreshing = state.systemMonitor.isRefreshing,
                lastUpdatedAt = state.systemMonitor.lastUpdatedAt
            )
        }

        item(key = "dashboard-resources", contentType = "dashboard-module") {
            ResourceModule(
                resources = state.systemMonitor.resources,
                hasActiveSession = state.systemMonitor.isAvailable,
                isRefreshing = state.systemMonitor.isRefreshing,
                lastUpdatedAt = state.systemMonitor.lastUpdatedAt,
                refreshInterval = state.systemMonitor.refreshInterval,
                onRefreshIntervalClick = screenModel::cycleSystemMonitorRefreshInterval
            )
        }

        item(key = "dashboard-actions", contentType = "dashboard-actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                MoccaCompactButton(
                    text = "Settings",
                    icon = Icons.Default.Settings,
                    onClick = onSettingsClick,
                    modifier = Modifier.weight(1f).testTag(TestTags.Dashboard.settingsNav),
                    textColor = AppColors.onSurface,
                    backgroundColor = AppColors.surfaceContainerHigh
                )
                MoccaCompactButton(
                    text = "Files",
                    icon = Icons.Default.Folder,
                    onClick = onFilesClick,
                    modifier = Modifier.weight(1f).testTag(TestTags.Dashboard.filesNav),
                    textColor = AppColors.onSurface,
                    backgroundColor = AppColors.surfaceContainerHigh
                )
                MoccaCompactButton(
                    text = "Terminal",
                    icon = Icons.Default.Terminal,
                    onClick = onTerminalClick,
                    modifier = Modifier.weight(1f).testTag(TestTags.Dashboard.terminalNav),
                    textColor = AppColors.primary,
                    backgroundColor = AppColors.surfaceContainerHigh
                )
            }
        }
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
    
    ModuleCard(title = "Workspace") {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Projects
            when (projects) {
                is Resource.Loading -> Text("Bootstrapping workspace...", color = AppColors.outline, style = AppTypography.labelMedium)
                is Resource.Success -> {
                    val active = projects.data.find { it.id == currentId } ?: projects.data.firstOrNull()
                    if (active != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(color = AppColors.primary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(active.displayName.uppercase(), color = AppColors.onSurface, style = AppTypography.labelLarge, fontWeight = FontWeight.Black)
                                Text(active.path ?: active.directory ?: "/", color = AppColors.onSurfaceVariant, style = AppTypography.labelSmall)
                            }
                        }
                    }
                    if (projects.data.size > 1) {
                        Text("+${projects.data.size - 1} standby environments", color = AppColors.outline, style = AppTypography.labelSmall)
                    }
                }
                is Resource.Error -> Text("Env init failed", color = AppColors.error, style = AppTypography.labelMedium)
            }
            
            HorizontalDivider(color = AppColors.outline.copy(alpha = 0.5f))
            
            // Agents
            when (agents) {
                is Resource.Loading -> Text("Loading agents...", color = AppColors.outline, style = AppTypography.labelMedium)
                is Resource.Success -> {
                    if (agents.data.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${agents.data.size} active agents", color = AppColors.onSurfaceVariant, style = AppTypography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(agents.data.joinToString(" • ") { it.name.uppercase() }, color = AppColors.onSurface, style = AppTypography.bodySmall, maxLines = 2)
                    } else {
                        Text("No agents online", color = AppColors.outline, style = AppTypography.labelMedium)
                    }
                }
                is Resource.Error -> Text("Agent link failed", color = AppColors.error, style = AppTypography.labelMedium)
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
    ModuleCard(title = "Capabilities") {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Tools Column
            Column(modifier = Modifier.weight(1f)) {
                Text("Integrated tools", color = AppColors.outline, style = AppTypography.labelSmall)
                Spacer(Modifier.height(4.dp))
                when (tools) {
                    is Resource.Loading -> Text("Scanning...", color = AppColors.onSurfaceVariant, style = AppTypography.bodySmall)
                    is Resource.Success -> {
                        Text("${tools.data.size} system tools", color = AppColors.primary, style = AppTypography.labelLarge, fontWeight = FontWeight.Bold)
                        val preview = tools.data.take(3).joinToString("\n") { "• $it" }
                        if (preview.isNotEmpty()) {
                            Text(preview, color = AppColors.onSurface, style = AppTypography.bodySmall, maxLines = 3)
                        }
                    }
                    is Resource.Error -> Text("Offline", color = AppColors.error, style = AppTypography.labelLarge)
                }
            }
            
            // Commands Column
            Column(modifier = Modifier.weight(1f)) {
                Text("Slash commands", color = AppColors.outline, style = AppTypography.labelSmall)
                Spacer(Modifier.height(4.dp))
                when (commands) {
                    is Resource.Loading -> Text("Scanning...", color = AppColors.onSurfaceVariant, style = AppTypography.bodySmall)
                    is Resource.Success -> {
                        Text("${commands.data.size} available", color = AppColors.primary, style = AppTypography.labelLarge, fontWeight = FontWeight.Bold)
                        val preview = commands.data.take(3).joinToString("\n") { "• /${it.name}" }
                        if (preview.isNotEmpty()) {
                            Text(preview, color = AppColors.onSurface, style = AppTypography.bodySmall, maxLines = 3)
                        }
                    }
                    is Resource.Error -> Text("Offline", color = AppColors.error, style = AppTypography.labelLarge)
                }
            }
        }
    }
}

// EXTENSION FUNCTION

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
