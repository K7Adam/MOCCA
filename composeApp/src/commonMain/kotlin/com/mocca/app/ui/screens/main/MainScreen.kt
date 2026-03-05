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
import androidx.compose.runtime.mutableStateOf
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

import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.navigation.SwipePanelLayout
import com.mocca.app.ui.navigation.rememberPanelState
import com.mocca.app.ui.screens.chat.ChatContent
import com.mocca.app.ui.screens.chat.ChatScreenModel
import com.mocca.app.ui.screens.chat.ScrollDirection
import com.mocca.app.ui.screens.files.FilesScreen
import com.mocca.app.ui.screens.git.GitScreen
import com.mocca.app.ui.screens.mcp.McpScreen
import com.mocca.app.ui.screens.panels.ContextHistoryPanel
import com.mocca.app.ui.screens.panels.DashboardPanel
import com.mocca.app.ui.screens.panels.DashboardScreenModel
import com.mocca.app.ui.screens.onboarding.ProgressiveOnboardingScreen
import com.mocca.app.ui.screens.settings.SettingsScreen
import com.mocca.app.ui.screens.terminal.TerminalScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.rememberCoroutineScope
import com.mocca.app.util.FilePickerHelper
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import com.mocca.app.ui.components.modern.ConnectionBannerStatus
import com.mocca.app.ui.components.modern.ConnectionStatusBanner
import com.mocca.app.ui.components.modern.QuoteRotator
import com.mocca.app.ui.components.modern.ScrollToBottomButton
import com.mocca.app.ui.components.modern.UpdateDialog

/**
 * Main screen with swipe panel navigation.
 * Shows chat content in center, context/history on left, dashboard on right.
 * 
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
        
        val coroutineScope = rememberCoroutineScope()
        
        val filePickerLauncher = rememberFilePickerLauncher(
            type = FilePickerHelper.createFileType(),
            mode = FileKitMode.Multiple()
        ) { files ->
            files?.forEach { file ->
                coroutineScope.launch {
                    try {
                        val attached = FilePickerHelper.toAttachedFile(file)
                        chatScreenModel.addAttachment(attached)
                    } catch (e: Exception) {
                        io.github.aakira.napier.Napier.e("Failed to attach file", e)
                    }
                }
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
        
        // Track scroll direction for chat input auto-hide
        var scrollDirection by remember { mutableStateOf(ScrollDirection.IDLE) }

        // State for scroll-to-bottom button (reported by ChatContent)
        var showScrollToBottom by remember { mutableStateOf(false) }
        var hasNewMessagesWhileScrolledUp by remember { mutableStateOf(false) }
        var scrollToBottomTrigger by remember { mutableStateOf(0L) }

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
            Box(modifier = Modifier.fillMaxSize()) {
                // ═══════════════════════════════════════════════════════════════════
                // Content area - full screen, unified bottom bar floats above
                // ═══════════════════════════════════════════════════════════════════
                SwipePanelLayout(
                    modifier = Modifier,
                        leftPanel = {
                        ContextHistoryPanel(
                            sessions = state.sessions,
                            sessionGroups = state.sessionGroups,
                            runningSessionIds = state.runningSessionIds,
                            currentSessionId = state.currentSessionId,
                            mcpStatus = state.mcpStatus,
                            model = chatState.modelName,
                            latency = state.latency,
                            port = state.port,
                            usedTokens = chatState.contextWindowUsage,
                            maxTokens = chatState.maxTokens.takeIf { it > 0 } ?: state.maxTokens,
                            agentName = chatState.agentName,
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
                                        onRetryClick = { screenModel.retryConnection() },
                                        onSetupClick = {
                                            navigator.push(
                                                ProgressiveOnboardingScreen(
                                                    isSetupMode = true,
                                                    initialError = state.connectionError
                                                )
                                            )
                                        }
                                    )
                                }

                                // Chat content (input will be disabled based on connection status)
                                ChatContent(
                                    screenModel = chatScreenModel, 
                                    onScrollDirectionChange = { direction: ScrollDirection -> scrollDirection = direction },
                                    onScrollToBottomStateChange = { show, hasNew ->
                                        showScrollToBottom = show
                                        hasNewMessagesWhileScrolledUp = hasNew
                                    },
                                    scrollToBottomTrigger = scrollToBottomTrigger
                                )
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
                                        onRetryClick = { screenModel.retryConnection() },
                                        onSetupClick = {
                                            navigator.push(
                                                ProgressiveOnboardingScreen(
                                                    isSetupMode = true,
                                                    initialError = state.connectionError
                                                )
                                            )
                                        }
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
                            onSkillsClick = { },
                            onSkillClick = { },
                            onTerminalClick = { navigator.push(TerminalScreen()) }
                        )
                    },
                    panelState = panelState.state,
                    onPanelStateChange = { panelState.state = it },
                    onDragProgressChange = { progress -> dragProgress = progress }
                )
            } // End of content wrapper



            // ═══════════════════════════════════════════════════════════════════════
            // UNIFIED FLOATING BOTTOM BAR
            // ═══════════════════════════════════════════════════════════════════════
            // Surface-based bottom bar with dynamic adaptation
            // Nav row is ALWAYS visible; only chat input auto-hides on scroll
            val inputText by chatScreenModel.inputText.collectAsState()
            val shellMode by chatScreenModel.shellMode.collectAsState()
            
            // Chat input auto-hides when scrolling up (reading older messages)
            // Nav row stays visible at all times on all screens
            val isChatInputVisible = scrollDirection != ScrollDirection.UP

            // Scroll-to-bottom button overlay
            ScrollToBottomButton(
                isVisible = showScrollToBottom,
                hasNewMessages = hasNewMessagesWhileScrolledUp,
                onClick = {
                    scrollToBottomTrigger += 1L
                    showScrollToBottom = false
                    hasNewMessagesWhileScrolledUp = false
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // We use ChatInputModeMinHeight to clear the bottom bar
                    .padding(
                        bottom = com.mocca.app.ui.components.navigation.NavConstants.ChatInputModeMinHeight + AppSpacing.sm,
                        end = AppSpacing.lg
                    )
            )
            
            UnifiedFloatingBottomBar(
                mode = BottomBarMode.ChatInput,
                dragProgress = dragProgress,
                isChatInputVisible = isChatInputVisible,
                onItemClick = { panelState.state = it },
                // Chat input parameters
                inputText = inputText,
                onInputTextChange = { chatScreenModel.updateInputText(it) },
                onSendClick = { chatScreenModel.sendMessage() },
                inputEnabled = chatState.connectionStatus is com.mocca.app.domain.model.ConnectionStatus.Connected && chatState.isSessionIdle,
                // Agent state - transforms SEND to ABORT when running
                isSessionIdle = chatState.isSessionIdle,
                onAbortClick = { chatScreenModel.abortSession() },
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
                onAttachClick = { filePickerLauncher.launch() },
                commands = chatState.commands,
                onCommandSelected = { cmd -> chatScreenModel.executeCommand(cmd) },
                onModeSelectedForMention = { mode -> chatScreenModel.selectMode(mode.id) },
                shellMode = shellMode,
                onShellModeToggle = { chatScreenModel.toggleShellMode() },
                planMode = chatState.isPlanMode,
                onPlanModeToggle = { chatScreenModel.togglePlanMode() },
                onHistoryUp = { chatScreenModel.navigateHistoryUp() },
                onHistoryDown = { chatScreenModel.navigateHistoryDown() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}
