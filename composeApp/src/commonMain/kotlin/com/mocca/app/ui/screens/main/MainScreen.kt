package com.mocca.app.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.navigation.NavConstants
import com.mocca.app.ui.theme.AppShapes

import com.mocca.app.ui.navigation.PanelProgressHolder
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.navigation.rememberPanelState
import com.mocca.app.ui.screens.chat.ChatScreenModel
import com.mocca.app.ui.screens.chat.ScrollDirection
import com.mocca.app.domain.model.SearchMode
import com.mocca.app.ui.screens.files.FilesScreen
import com.mocca.app.ui.screens.git.GitScreen
import com.mocca.app.ui.screens.mcp.McpScreen
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
import com.mocca.app.ui.components.modern.ModernTopBar
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import com.mocca.app.ui.components.modern.UpdateDialog
import com.mocca.app.ui.theme.LocalAppPerformance

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
        val aggregatedMessages by chatScreenModel.aggregatedMessages.collectAsState()
        val inputText by chatScreenModel.inputText.collectAsState()
        val shellMode by chatScreenModel.shellMode.collectAsState()
        val voicePermissionRequestToken by chatScreenModel.voicePermissionRequestToken.collectAsState()
        val voiceInputState by chatScreenModel.voiceInputState.collectAsState()

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
        val progressHolder = remember { PanelProgressHolder() }
        val hapticFeedback = LocalHapticFeedback.current
        var lastSnappedPanelState by remember { mutableStateOf(panelState.state) }

        // Track scroll direction for chat input auto-hide
        var scrollDirection by remember { mutableStateOf(ScrollDirection.IDLE) }

        // State for jump-to-latest button (reported by ChatContent)
        var showScrollToBottom by remember { mutableStateOf(false) }
        var hasNewMessagesWhileScrolledUp by remember { mutableStateOf(false) }
        var scrollToBottomTrigger by remember { mutableStateOf(0L) }
        var showGlobalSearch by remember { mutableStateOf(false) }
        var globalSearchQuery by remember { mutableStateOf("") }
        var targetMessageId by remember { mutableStateOf<String?>(null) }

        val trimmedGlobalSearchQuery = globalSearchQuery.trim()
        val sessionSearchResults = remember(trimmedGlobalSearchQuery, state.sessions) {
            if (trimmedGlobalSearchQuery.isBlank()) {
                emptyList()
            } else {
                state.sessions
                    .filter { session -> session.matchesGlobalSearch(trimmedGlobalSearchQuery) }
                    .take(8)
            }
        }
        val messageSearchResults = remember(trimmedGlobalSearchQuery, aggregatedMessages, state.currentSessionId) {
            if (trimmedGlobalSearchQuery.isBlank() || state.currentSessionId == null) {
                emptyList()
            } else {
                aggregatedMessages
                    .asReversed()
                    .mapNotNull { message -> message.toGlobalSearchMatch(trimmedGlobalSearchQuery) }
                    .take(8)
            }
        }

        fun clearGlobalSearch() {
            globalSearchQuery = ""
            screenModel.searchFiles("")
        }

        fun dismissGlobalSearch() {
            showGlobalSearch = false
            clearGlobalSearch()
        }

        // Reset chat-specific state
        LaunchedEffect(panelState.state) {
            if (panelState.state != lastSnappedPanelState) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastSnappedPanelState = panelState.state
            }

            if (panelState.state != PanelState.CENTER) {
                scrollDirection = ScrollDirection.IDLE
                showScrollToBottom = false
                hasNewMessagesWhileScrolledUp = false
            }
        }

        val mainPanels = rememberMainScreenPanels(
            MainScreenPanelScope(
                navigator = navigator,
                screenModel = screenModel,
                chatScreenModel = chatScreenModel,
                dashboardScreenModel = dashboardScreenModel,
                state = state,
                chatState = chatState,
                inputText = inputText,
                shellMode = shellMode,
                voicePermissionRequestToken = voicePermissionRequestToken,
                voiceInputState = voiceInputState,
                panelState = panelState,
                onAttachClick = { filePickerLauncher.launch() },
                scrollToBottomTrigger = scrollToBottomTrigger,
                showScrollToBottom = showScrollToBottom,
                hasNewMessagesWhileScrolledUp = hasNewMessagesWhileScrolledUp,
                onScrollDirectionChange = { direction -> scrollDirection = direction },
                onScrollToBottomStateChange = { show, hasNew ->
                    showScrollToBottom = show
                    hasNewMessagesWhileScrolledUp = hasNew
                },
                onScrollToBottomClick = {
                    scrollToBottomTrigger += 1L
                    showScrollToBottom = false
                    hasNewMessagesWhileScrolledUp = false
                },
                onRetryConnection = { screenModel.retryConnection() },
                onOpenSetup = {
                    navigator.push(
                        ProgressiveOnboardingScreen(
                            isSetupMode = true,
                            connectionError = state.connectionError
                        )
                    )
                },
                onSessionTabSelected = { sessionId -> screenModel.selectSession(sessionId) },
                onSearchClick = { showGlobalSearch = true },
                targetMessageId = targetMessageId,
                onTargetMessageHandled = { targetMessageId = null }
            )
        )
        val navItems = remember(mainPanels) {
            MainScreenPanelRegistry.navigationItems(mainPanels)
        }
        val performance = LocalAppPerformance.current

        Box(modifier = Modifier.fillMaxSize()) {
            if (performance.useAmbientEffects) {
                DynamicExpressiveBackground()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.background)
                )
            }

            // Content area - full screen, unified bottom bar floats above

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                bottomBar = {
                    val bottomBarModifier = Modifier.navigationBarsPadding()

                    Box(
                        modifier = Modifier.padding(
                            horizontal = AppSpacing.screenPaddingHorizontal,
                            vertical = AppSpacing.xs
                        )
                    ) {
                        Surface(
                            modifier = bottomBarModifier.fillMaxWidth(),
                            color = AppColors.bgBase,
                            shape = AppShapes.extraLarge,
                            tonalElevation = 0.dp
                        ) {
                        PersistentNavRow(
                                dragProgress = progressHolder.dragProgress,
                                onItemClick = { panelState.state = it },
                                items = navItems,
                                showLabels = true,
                                isAgentRunning = !chatState.isSessionIdle,
                                modifier = Modifier.fillMaxWidth().height(NavConstants.NavigationModeHeight)
                            )
                        }
                    }
                }
            ) { paddingValues ->
                MainScreenPanelHost(
                    panels = mainPanels,
                    panelState = panelState.state,
                    onPanelStateChange = { panelState.state = it },
                    progressHolder = progressHolder,
                    paddingValues = paddingValues
                )
            }

            if (showGlobalSearch) {
                GlobalSearchOverlay(
                    query = globalSearchQuery,
                    sessionResults = sessionSearchResults,
                    messageResults = messageSearchResults,
                    fileSearchMode = state.fileSearchMode,
                    fileResults = state.fileSearchResults,
                    fileContentResults = state.fileContentSearchResults,
                    isFileSearchLoading = state.isFileSearchLoading,
                    fileSearchError = state.fileSearchError,
                    currentSessionTitle = state.sessions
                        .find { it.id == state.currentSessionId }
                        ?.title,
                    onQueryChange = { nextQuery ->
                        globalSearchQuery = nextQuery
                        screenModel.searchFiles(nextQuery)
                    },
                    onFileSearchModeChange = { mode ->
                        if (mode != SearchMode.SYMBOL) {
                            screenModel.updateFileSearchMode(mode, globalSearchQuery)
                        }
                    },
                    onDismiss = { dismissGlobalSearch() },
                    onSessionClick = { session ->
                        targetMessageId = null
                        screenModel.selectSession(session.id)
                        dismissGlobalSearch()
                    },
                    onMessageClick = { result ->
                        targetMessageId = result.messageId
                        if (state.currentSessionId != result.sessionId) {
                            screenModel.selectSession(result.sessionId)
                        }
                        dismissGlobalSearch()
                    },
                    onFileClick = {
                        navigator.push(FilesScreen())
                        dismissGlobalSearch()
                    }
                )
            }
        }
    }
}
