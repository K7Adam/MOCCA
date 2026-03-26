package com.mocca.app.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.runtime.CompositionLocalProvider
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import com.mocca.app.ui.components.navigation.PersistentNavRow
import com.mocca.app.ui.components.navigation.ChatInputContent
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.navigation.BottomBarMode
import com.mocca.app.ui.components.navigation.UnifiedFloatingBottomBar
import com.mocca.app.ui.components.navigation.NavConstants
import com.mocca.app.ui.theme.AppShapes

import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.navigation.SwipePanelLayout
import com.mocca.app.ui.navigation.rememberPanelState
import com.mocca.app.ui.navigation.ModernTransitions
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
import com.mocca.app.ui.theme.DynamicExpressiveBackground
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.rememberCoroutineScope
import com.mocca.app.util.FilePickerHelper
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import com.mocca.app.ui.components.modern.ConnectionBannerStatus
import com.mocca.app.ui.components.modern.ConnectionStatusBanner
import com.mocca.app.ui.components.modern.ModernTopBar
import com.mocca.app.ui.components.modern.QuoteRotator
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
        val inputText by chatScreenModel.inputText.collectAsState()
        val shellMode by chatScreenModel.shellMode.collectAsState()

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

        // Reset chat-specific state
        LaunchedEffect(panelState.state) {
            if (panelState.state != PanelState.CENTER) {
                scrollDirection = ScrollDirection.IDLE
                showScrollToBottom = false
                hasNewMessagesWhileScrolledUp = false
            }
        }

        val panelTransition = ModernTransitions.panelTransition()

        AnimatedContent(
            targetState = panelState.state,
            label = "panelTransition",
            transitionSpec = { panelTransition }
        ) { targetPanelState ->
            CompositionLocalProvider(
                LocalNavAnimatedVisibilityScope provides this
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    DynamicExpressiveBackground()
                
                // ═══════════════════════════════════════════════════════════════════
                // Content area - full screen, unified bottom bar floats above
                // ═══════════════════════════════════════════════════════════════════
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    bottomBar = {
                        val sharedTransitionScope = LocalSharedTransitionScope.current
                        val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
            
                        val bottomBarModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                Modifier
                                    .navigationBarsPadding()
                                    .sharedBounds(
                                        rememberSharedContentState(key = "bottom_bar"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                            }
                        } else {
                            Modifier
                                .navigationBarsPadding()
                        }
                        
                        Box(
                            modifier = Modifier.padding(horizontal = AppSpacing.screenPaddingHorizontalCompact, vertical = AppSpacing.sm)
                        ) {
                            Surface(
                                modifier = bottomBarModifier.fillMaxWidth(),
                                color = AppColors.surfaceContainer,
                                shape = AppShapes.extraLarge
                            ) {
                                PersistentNavRow(
                                    dragProgress = dragProgress,
                                    onItemClick = { panelState.state = it },
                                    showLabels = true,
                                    isAgentRunning = !chatState.isSessionIdle,
                                    modifier = Modifier.fillMaxWidth().height(NavConstants.NavigationModeHeight)
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                SwipePanelLayout(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                        leftPanel = {
                        ContextHistoryPanel(
                            modifier = Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)),
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
                                val currentSession = state.sessions.find { it.id == state.currentSessionId }
                                ModernTopBar(
                                    title = currentSession?.title ?: "Chat",
                                    sessionId = state.currentSessionId
                                )

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
                                                    connectionError = state.connectionError
                                                )
                                            )
                                        }
                                    )
                                }

                                // Chat content (input will be disabled based on connection status)
                                Box(modifier = Modifier.weight(1f)) {
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
                                
                                Surface(
                                    modifier = Modifier.fillMaxWidth().imePadding(),
                                    color = Color.Transparent
                                ) {
                                    ChatInputContent(
                                        inputText = inputText,
                                        onInputTextChange = { chatScreenModel.updateInputText(it) },
                                        onSendClick = { chatScreenModel.sendMessage() },
                                        inputEnabled = chatState.connectionStatus is com.mocca.app.domain.model.ConnectionStatus.Connected && chatState.isSessionIdle,
                                        placeholder = "Type a message...",
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
                                        onCommandSelected = { chatScreenModel.executeCommand(it) },
                                        onModeSelectedForMention = { chatScreenModel.selectMode(it.id) },
                                        shellMode = shellMode,
                                        onShellModeToggle = { chatScreenModel.toggleShellMode() },
                                        onHistoryUp = { chatScreenModel.navigateHistoryUp() },
                                        onHistoryDown = { chatScreenModel.navigateHistoryDown() }
                                    )
                                }
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
                                                    connectionError = state.connectionError
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
                                        serverText = if (state.isConnected) "Local server" else null,
                                        isLoading = state.isLoadingSession || state.isCreatingSession,
                                        loadingText = if (state.isCreatingSession) "Creating session..." else "Loading session..."
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
                }
            // End of content wrapper



            // ═══════════════════════════════════════════════════════════════════════
            // UNIFIED FLOATING BOTTOM BAR
            // ═══════════════════════════════════════════════════════════════════════
            // Surface-based bottom bar with dynamic adaptation
            // Nav row is ALWAYS visible; only chat input auto-hides on scroll
            
            // Chat input auto-hides when scrolling up (reading older messages)
            // Nav row stays visible at all times on all screens
            val isChatInputVisible = panelState.state == PanelState.CENTER && scrollDirection != ScrollDirection.UP

            // Action FAB Menu overlay
            // Dynamically positioned above the bottom bar based on its current height
            val bottomBarHeight = if (isChatInputVisible) {
                NavConstants.ChatInputModeMinHeight
            } else {
                NavConstants.NavigationModeHeight
            }
            
            var fabMenuExpanded by remember { mutableStateOf(false) }
            
            androidx.compose.animation.AnimatedVisibility(
                visible = panelState.state == PanelState.CENTER,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.material3.MaterialTheme.motionScheme.fastSpatialSpec()
                ) + androidx.compose.animation.scaleIn(
                    animationSpec = androidx.compose.material3.MaterialTheme.motionScheme.fastSpatialSpec(),
                    initialScale = 0.8f
                ),
                exit = androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.material3.MaterialTheme.motionScheme.fastSpatialSpec()
                ) + androidx.compose.animation.scaleOut(
                    animationSpec = androidx.compose.material3.MaterialTheme.motionScheme.fastSpatialSpec(),
                    targetScale = 0.8f
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(bottom = bottomBarHeight + AppSpacing.sm, end = AppSpacing.md)
            ) {
                FloatingActionButtonMenu(
                    expanded = fabMenuExpanded,
                    button = {
                        ToggleFloatingActionButton(
                            checked = fabMenuExpanded,
                            onCheckedChange = { fabMenuExpanded = it },
                            containerColor = { AppColors.primary }
                        ) {
                            androidx.compose.animation.AnimatedContent(
                                targetState = fabMenuExpanded, 
                                label = "fabIcon"
                            ) { expanded ->
                                if (expanded) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Menu",
                                        tint = AppColors.onPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Open Menu",
                                        tint = AppColors.onPrimary
                                    )
                                }
                            }
                        }
                    }
                ) {
                    FloatingActionButtonMenuItem(
                        onClick = {
                            fabMenuExpanded = false
                            chatScreenModel.openShareDialog()
                        },
                        icon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                        text = { Text("Share Session") },
                        containerColor = AppColors.surfaceContainer,
                        contentColor = AppColors.onSurface
                    )

                    if (showScrollToBottom) {
                        FloatingActionButtonMenuItem(
                            onClick = {
                                fabMenuExpanded = false
                                scrollToBottomTrigger += 1L
                                showScrollToBottom = false
                                hasNewMessagesWhileScrolledUp = false
                            },
                            icon = { 
                                Box {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll to Bottom")
                                    if (hasNewMessagesWhileScrolledUp) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-4).dp, y = 4.dp)
                                                .clip(AppShapes.circle)
                                                .background(AppColors.accentGreen)
                                                .border(2.dp, AppColors.surfaceContainer, AppShapes.circle)
                                        )
                                    }
                                }
                            },
                            text = { Text("Scroll to Bottom") },
                            containerColor = AppColors.surfaceContainer,
                            contentColor = AppColors.onSurface
                        )
                    }
                }
            }
            
            // Bottom bar is now handled by Scaffold
        }
    }
}
}
}
