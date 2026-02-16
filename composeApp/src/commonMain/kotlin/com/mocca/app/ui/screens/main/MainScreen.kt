package com.mocca.app.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.terminal.ConnectionBannerStatus
import com.mocca.app.ui.components.terminal.ConnectionStatusBanner
import com.mocca.app.ui.components.terminal.GlobalActivityIndicator
import com.mocca.app.ui.components.terminal.QuoteRotator
import com.mocca.app.ui.components.terminal.UpdateDialog
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.navigation.SwipePanelLayout
import com.mocca.app.ui.navigation.rememberPanelState
import com.mocca.app.ui.screens.chat.ChatContent
import com.mocca.app.ui.screens.chat.ChatScreenModel
import com.mocca.app.ui.screens.files.FilesScreen
import com.mocca.app.ui.screens.git.GitScreen
import com.mocca.app.ui.screens.mcp.McpScreen
import com.mocca.app.ui.screens.panels.ContextHistoryPanel
import com.mocca.app.ui.screens.panels.DashboardPanel
import com.mocca.app.ui.screens.panels.DashboardScreenModel
import com.mocca.app.ui.screens.settings.SettingsScreen
import com.mocca.app.ui.screens.terminal.TerminalScreen
import com.mocca.app.ui.theme.AppColors
import org.koin.core.parameter.parametersOf

/**
 * Main screen with swipe panel navigation.
 * Shows chat content in center, context/history on left, dashboard on right.
 * Refactored for modern UI/UX.
 */
data class MainScreen(val sessionId: String? = null) : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<MainScreenModel> { parametersOf(sessionId) }
        val state by screenModel.state.collectAsState()
        val dashboardScreenModel = koinScreenModel<DashboardScreenModel>()
        // Use a stable ChatScreenModel instance and reload content when session changes
        val chatScreenModel = koinScreenModel<ChatScreenModel>()
        val chatState by chatScreenModel.state.collectAsState()

        // Reload chat session when ID changes
        androidx.compose.runtime.LaunchedEffect(state.currentSessionId) {
            state.currentSessionId?.let { id ->
                chatScreenModel.loadSession(id)
            }
        }
        
        // Show Update Dialog
        if (state.isUpdateAvailable && state.updateInfo != null) {
            UpdateDialog(
                updateInfo = state.updateInfo!!,
                isDownloading = state.isDownloadingUpdate,
                progress = state.downloadProgress,
                error = state.updateError,
                onUpdate = { screenModel.startUpdate() },
                onDismiss = { screenModel.dismissUpdate() },
                onRetry = { screenModel.retryUpdate() }
            )
        }
        
        val panelState = rememberPanelState()
        
        // PERFORMANCE FIX: BackHandler to close panels before exiting app
        BackHandler(enabled = panelState.state != PanelState.CENTER) {
            panelState.closePanel()
        }
        
        // Main content wrapped in Box for GlobalActivityIndicator overlay
        // Use Pitch Black background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
        ) {
            SwipePanelLayout(
                leftPanel = {
                    ContextHistoryPanel(
                        sessions = state.sessions,
                        sessionGroups = state.sessionGroups,
                        runningSessionIds = state.runningSessionIds,
                        currentSessionId = state.currentSessionId,
                        mcpStatus = state.mcpStatus,
                        model = chatState.modelName.takeIf { it != "--" } ?: state.modelName,
                        latency = state.latency,
                        port = state.port,
                        usedTokens = chatState.totalInputTokens + chatState.totalOutputTokens,
                        maxTokens = chatState.maxTokens.takeIf { it > 0 } ?: state.maxTokens,
                        agentName = chatState.agentName.takeIf { it != "--" } ?: state.agentName,
                        appVersion = state.appVersion,
                        isCreatingSession = state.isCreatingSession,
                        loadingSessionId = state.loadingSessionId,
                        newlyCreatedSessionId = state.newlyCreatedSessionId,
                        isRefreshing = state.isLoading,
                        onSessionClick = { session ->
                            screenModel.selectSession(session.id) {
                                panelState.closePanel()
                            }
                        },
                        onNewSessionClick = {
                            screenModel.createSession {
                                panelState.closePanel()
                            }
                        },
                        onRefresh = { screenModel.refreshAll() },
                        onGroupExpandToggle = { groupId ->
                            screenModel.toggleGroupExpanded(groupId)
                        }
                    )
                },
                centerPanel = {
                    // Show ChatContent if session is selected (input disabled when not connected)
                    if (state.currentSessionId != null) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Connection status banner at top when not connected
                            if (!state.isConnected) {
                                ConnectionStatusBanner(
                                    status = when {
                                        state.isConnecting -> ConnectionBannerStatus.Connecting(
                                            attempt = state.connectionAttempt,
                                            maxAttempts = state.maxConnectionAttempts
                                        )
                                        state.isWaitingForNetwork -> ConnectionBannerStatus.WaitingForNetwork
                                        state.connectionError != null -> ConnectionBannerStatus.Error(state.connectionError!!)
                                        else -> ConnectionBannerStatus.Disconnected()
                                    },
                                    onRetryClick = { screenModel.retryConnection() }
                                )
                            }
                            
                            // Chat content (input will be disabled based on connection status)
                            ChatContent(chatScreenModel)
                        }
                    } else {
                        // Empty state - no session selected
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (!state.isConnected) {
                                ConnectionStatusBanner(
                                    status = when {
                                        state.isConnecting -> ConnectionBannerStatus.Connecting(
                                            attempt = state.connectionAttempt,
                                            maxAttempts = state.maxConnectionAttempts
                                        )
                                        state.isWaitingForNetwork -> ConnectionBannerStatus.WaitingForNetwork
                                        state.connectionError != null -> ConnectionBannerStatus.Error(state.connectionError!!)
                                        else -> ConnectionBannerStatus.Disconnected()
                                    },
                                    onRetryClick = { screenModel.retryConnection() }
                                )
                            }
                            
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                QuoteRotator(
                                    versionText = state.appVersion,
                                    serverText = if (state.isConnected) "LOCAL SERVER" else null,
                                    isLoading = state.isLoadingSession || state.isCreatingSession,
                                    loadingText = if (state.isCreatingSession) "CREATING_SESSION..." else "LOADING_SESSION..."
                                )
                            }
                        }
                    }
                },
                rightPanel = {
                    DashboardPanel(
                        screenModel = dashboardScreenModel,
                        onMcpConfigClick = { navigator.push(McpScreen()) },
                        onSettingsClick = { navigator.push(SettingsScreen()) },
                        onGitClick = { navigator.push(GitScreen()) },
                        onFilesClick = { navigator.push(FilesScreen()) },
                        onTerminalClick = { navigator.push(TerminalScreen()) },
                        onSkillsClick = { },
                        onSkillClick = { },
                        onRefreshAll = {
                            // Trigger global refresh: sessions, messages, config, and SSE reconnection
                            screenModel.refreshAll()
                            // Also refresh chat data
                            chatScreenModel.refreshData()
                            // Refresh dashboard data
                            dashboardScreenModel.refresh()
                        }
                    )
                },
                panelState = panelState.state,
                onPanelStateChange = { panelState.state = it }
            )
            
            // Global Activity Indicator overlay - top right corner
            GlobalActivityIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            )
        }
    }
}
