package com.mocca.app.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.navigation.BottomBarMode
import com.mocca.app.ui.components.navigation.UnifiedFloatingBottomBar
import com.mocca.app.ui.components.modern.*
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
import com.mocca.app.ui.screens.console.ConsoleScreen
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

        // Track real-time drag progress for animated indicator (0.0 = right, 0.5 = center, 1.0 = left)
        var dragProgress by remember { mutableFloatStateOf(0.5f) }

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
                .statusBarsPadding()
        ) {
            // Subtle terminal effect
            ScanlineOverlay(modifier = Modifier.fillMaxSize())
            
            // Content area - full screen, unified bottom bar floats above
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
                            onTerminalClick = { navigator.push(ConsoleScreen()) },
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
                    onPanelStateChange = { panelState.state = it },
                    onDragProgressChange = { progress -> dragProgress = progress }
                )

            // Global Activity Indicator overlay - top right corner
            // TEMPORARILY DISABLED: Activity tracking not fully implemented
            // GlobalActivityIndicator(
            //     modifier = Modifier
            //         .align(Alignment.TopEnd)
            //         .padding(top = 48.dp, end = 16.dp)
            // )

            // Unified Floating Bottom Bar - morphs between nav and chat input modes
            val inputText by chatScreenModel.inputText.collectAsState()
            
            UnifiedFloatingBottomBar(
                mode = when (panelState.state) {
                    PanelState.CENTER -> BottomBarMode.ChatInput
                    else -> BottomBarMode.Navigation
                },
                dragProgress = dragProgress,
                onItemClick = { newState -> panelState.state = newState },
                // Chat input parameters
                inputText = inputText,
                onInputTextChange = { chatScreenModel.updateInputText(it) },
                onSendClick = { chatScreenModel.sendMessage() },
                inputEnabled = chatState.connectionStatus is com.mocca.app.domain.model.ConnectionStatus.Connected && chatState.isSessionIdle,
                modelName = chatState.modelName,
                agentName = chatState.agentName,
                providerResponse = chatState.providerInfo,
                selectedProviderId = chatState.selectedProviderId,
                selectedModelId = chatState.selectedModelId,
                onModelSelected = { providerId, modelId -> chatScreenModel.selectModel(providerId, modelId) },
                variants = chatState.availableVariants,
                selectedVariantId = chatState.selectedVariantId,
                onVariantSelected = { chatScreenModel.selectVariant(it) },
                modes = chatState.modes,
                selectedModeId = chatState.selectedModeId,
                onModeSelected = { chatScreenModel.selectMode(it) },
                attachedFiles = chatState.attachedFiles,
                onRemoveAttachment = { chatScreenModel.removeAttachment(it) },
                onAttachClick = { /* File picker handled in ChatContent */ },
                commands = chatState.commands,
                onCommandSelected = { cmd -> 
                    // Commands are handled in ChatContent via coroutines
                    // This is a placeholder - actual command execution happens there
                },
                onModeSelectedForMention = { mode -> chatScreenModel.selectMode(mode.id) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}
