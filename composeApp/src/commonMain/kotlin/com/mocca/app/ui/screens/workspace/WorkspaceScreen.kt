package com.mocca.app.ui.screens.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.Tab
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

import com.mocca.app.ui.components.*
import com.mocca.app.ui.screens.chat.ChatScreen
import com.mocca.app.ui.screens.files.FilesScreen
import com.mocca.app.ui.screens.git.GitScreen
import com.mocca.app.ui.theme.*

// Use GridOn as GridView, Terminal as Code
private val GridView get() = Icons.Filled.GridOn
private val Code get() = Icons.Filled.Terminal

data class WorkspaceScreen(val sessionId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        TabNavigator(DashboardTab(sessionId)) { tabNavigator ->
            Scaffold(
                topBar = {
                    val currentTitle = tabNavigator.current.options.title
                    val isDashboard = tabNavigator.current is DashboardTab
                    GodHeader(
                        title = if (isDashboard) "Workspace_01" else currentTitle,
                        onBackClick = { navigator.pop() },
                        subtitle = if (isDashboard) null else "SESSION: " + sessionId,
                        actions = {
                            IconButton(onClick = { /* Settings */ }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppColors.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    )
                },
                containerColor = AppColors.background,
                bottomBar = {
                    GodBottomNavBar(tabNavigator, sessionId)
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    AnimatedContent(
                        targetState = tabNavigator.current,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                        },
                        label = "tab_transition"
                    ) { tab ->
                        tabNavigator.saveableState(key = "tab_" + tab.options.index.toString(), tab = tab) {
                            tab.Content()
                        }
                    }
                }
            }
        }
    }
}


@Composable
internal fun DashboardContent(sessionId: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // MCP Server Status (1x1)
                GodModuleCard(
                    modifier = Modifier.weight(1f).height(AppSpacing.moduleCardHeight),
                    title = "MCP SERVER",
                    icon = Icons.Default.Dns,
                    status = "Online",
                    subtitle = "12ms latency",
                    statusColor = AppColors.accentGreen
                )
                
                // Skills (1x1)
                GodModuleCard(
                    modifier = Modifier.weight(1f).height(AppSpacing.moduleCardHeight),
                    title = "SKILLS",
                    icon = Icons.Default.Extension,
                    content = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(AppColors.surfaceVariant, AppShapes.medium)
                                        .border(1.dp, AppColors.outline, AppShapes.medium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (it == 0) Icons.Default.Code else if (it == 1) Icons.Default.Terminal else Icons.Default.Dataset,
                                        contentDescription = null,
                                        tint = AppColors.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(AppColors.surfaceVariant, AppShapes.medium),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+3", style = AppTypography.labelSmall, color = AppColors.onSurface.copy(alpha = 0.4f))
                            }
                        }
                    }
                )
            }
        }

        // Git Activity (2x1)
        item {
            GodModuleCard(
                modifier = Modifier.fillMaxWidth().height(AppSpacing.moduleCardHeight + 20.dp), // Slightly taller variant
                title = "GIT ACTIVITY",
                icon = Icons.Default.Commit,
                subtitle = "main • Last commit 2m ago",
                content = {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                        Text(
                            text = "feat: modular grid",
                            style = AppTypography.monoLabel,
                            color = AppColors.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(AppColors.surfaceContainer, AppShapes.small)
                                .border(1.dp, AppColors.outline, AppShapes.small)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        // TODO: Add SVG-like path drawing for git graph
                    }
                }
            )
        }

    }
}

@Composable
private fun GodModuleCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    status: String? = null,
    subtitle: String? = null,
    statusColor: Color = AppColors.onSurface,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        color = AppColors.surfaceVariant,
        shape = AppShapes.extraLarge,
        border = BorderStroke(1.dp, AppColors.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AppColors.surfaceVariant, AppShapes.circle)
                        .border(1.dp, AppColors.outline, AppShapes.circle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = AppColors.onSurface, modifier = Modifier.size(20.dp))
                    if (status == "Online") {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .background(AppColors.accentGreen, AppShapes.circle)
                                .border(2.dp, AppColors.surfaceVariant, AppShapes.circle)
                        )
                    }
                }
                Icon(
                    Icons.Default.DragIndicator,
                    contentDescription = null,
                    tint = AppColors.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = title,
                style = AppTypography.labelSmall,
                color = AppColors.onSurface.copy(alpha = 0.4f)
            )
            
            if (status != null) {
                Text(
                    text = status,
                    style = AppTypography.titleLarge,
                    color = AppColors.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = if (title == "MCP SERVER") AppTypography.codeSmall else AppTypography.bodySmall,
                    color = if (title == "MCP SERVER") AppColors.accentGreen else AppColors.onSurface.copy(alpha = 0.4f)
                )
            }
            
            content?.invoke(this)
        }
    }
}

@Composable
private fun GodBottomNavBar(
    tabNavigator: TabNavigator,
    sessionId: String
) {
    val items = listOf(
        DashboardTab(sessionId),
        ChatTab(sessionId),
        ExplorerTab,
        GitTab
    )

    Surface(
        color = AppColors.background.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, AppColors.outline)
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { tab ->
                val isSelected = tabNavigator.current == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = { tabNavigator.current = tab })
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Extract ImageVector safely or fallback to GridOn if something goes wrong
                    // Note: Voyager TabOptions only has Painter, not ImageVector natively, but we know the mapping
                    val iconVector = when(tab) {
                        is DashboardTab -> GridView
                        is ChatTab -> Icons.AutoMirrored.Filled.Chat
                        is ExplorerTab -> Icons.Default.Folder
                        is GitTab -> Code
                        else -> GridView
                    }
                    
                    Icon(
                        imageVector = iconVector,
                        contentDescription = tab.options.title,
                        tint = if (isSelected) AppColors.onSurface else AppColors.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.options.title.uppercase(),
                        style = AppTypography.labelSmall,
                        color = if (isSelected) AppColors.onSurface else AppColors.onSurface.copy(alpha = 0.3f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
