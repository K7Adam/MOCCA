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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.mocca.app.ui.components.navigation.PersistentNavRow
import com.mocca.app.ui.components.navigation.ChatInputContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.navigation.NavConstants

import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.navigation.toDragProgress
import com.mocca.app.ui.navigation.toPageIndex
import com.mocca.app.ui.screens.chat.ChatScreenModel
import com.mocca.app.ui.screens.chat.ScrollDirection
import com.mocca.app.domain.model.SearchMode
import com.mocca.app.ui.screens.files.FilesScreen
import com.mocca.app.ui.screens.panels.DashboardScreenModel
import com.mocca.app.ui.screens.onboarding.ProgressiveOnboardingScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.DynamicExpressiveBackground
import com.mocca.app.ui.theme.LocalAppPerformance
import com.mocca.app.ui.components.modern.UpdateDialog
import org.koin.core.parameter.parametersOf
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import com.mocca.app.util.FilePickerHelper
import kotlinx.coroutines.launch

/**
 * Main screen: 3-page HorizontalPager navigation.
 *
 * Page 0 – Sessions / Context History
 * Page 1 – Chat                         <- initial page
 * Page 2 – Tools / Dashboard
 *
 * Battery-conscious design:
 * - beyondViewportPageCount = 1 keeps Chat (page 1) always in composition
 *   so streaming continues even while the user browses Sessions or Tools.
 * - Expensive visual effects (DynamicExpressiveBackground) check
 *   [LocalAppPerformance] and are only composed when the performance
 *   tier permits them.
 * - userScrollEnabled = !isImeVisible prevents accidental page swipes
 *   while the keyboard is open, saving spurious composition passes.
 * - Page content that is not the current page does not run animations
 *   or heavy recompositions; only Chat keeps live SSE data flowing.
 */
data class MainScreen(val sessionId: String? = null) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<MainScreenModel> { parametersOf(sessionId) }
        val state by screenModel.state.collectAsState()
        val dashboardScreenModel = koinScreenModel<DashboardScreenModel>()
        val chatScreenModel = koinScreenModel<ChatScreenModel>()

        // Only collect aggregatedMessages when global search is open to avoid recompositions
        var showGlobalSearch by remember { mutableStateOf(false) }
        val globalAggregatedMessages by if (showGlobalSearch) {
            chatScreenModel.aggregatedMessages
        } else {
            kotlinx.coroutines.flow.flowOf<List<com.mocca.app.domain.model.Message>>(emptyList())
        }.collectAsState(initial = emptyList())

        // Reload chat session when ID changes
        LaunchedEffect(state.currentSessionId) {
            state.currentSessionId?.let { id -> chatScreenModel.loadSession(id) }
        }

        val coroutineScope = rememberCoroutineScope()
        val hapticFeedback = LocalHapticFeedback.current
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val performance = LocalAppPerformance.current

        // ---------------------------------------------------------------
        // Pager state – single source of truth for active page
        // ---------------------------------------------------------------
        val pagerState = rememberPagerState(
            initialPage = PanelState.CENTER.toPageIndex(),
            pageCount = { 3 }
        )

        // Live drag progress for the bottom nav indicator.
        // Wrapped in derivedStateOf so the lambda only triggers recomposition
        // in consumers (PersistentNavRow), not in MainScreen itself.
        val dragProgress by remember { derivedStateOf { pagerState.toDragProgress() } }

        // IME visibility – disable pager scroll while keyboard is shown to
        // prevent accidental navigation and spurious layout passes.
        val isImeVisible = WindowInsets.ime.getBottom(density) > 0

        // ---------------------------------------------------------------
        // File picker
        // ---------------------------------------------------------------
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

        // ---------------------------------------------------------------
        // Update dialog
        // ---------------------------------------------------------------
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

        // ---------------------------------------------------------------
        // Global search & message targeting state
        // ---------------------------------------------------------------
        var globalSearchQuery by remember { mutableStateOf("") }
        var targetMessageId by remember { mutableStateOf<String?>(null) }

        val trimmedGlobalSearchQuery = globalSearchQuery.trim()
        val sessionSearchResults = remember(trimmedGlobalSearchQuery, state.sessions) {
            if (trimmedGlobalSearchQuery.isBlank()) emptyList()
            else state.sessions
                .filter { it.matchesGlobalSearch(trimmedGlobalSearchQuery) }
                .take(8)
        }
        val messageSearchResults = remember(trimmedGlobalSearchQuery, globalAggregatedMessages, state.currentSessionId) {
            if (trimmedGlobalSearchQuery.isBlank() || state.currentSessionId == null) emptyList()
            else globalAggregatedMessages
                .asReversed()
                .mapNotNull { it.toGlobalSearchMatch(trimmedGlobalSearchQuery) }
                .take(8)
        }

        fun clearGlobalSearch() {
            globalSearchQuery = ""
            screenModel.searchFiles("")
        }

        fun dismissGlobalSearch() {
            showGlobalSearch = false
            clearGlobalSearch()
        }

        // ---------------------------------------------------------------
        // Side-effects on page settle
        // ---------------------------------------------------------------

        // Haptic feedback when settled on a new page.
        LaunchedEffect(pagerState.settledPage) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        // BackHandler: any non-Chat page -> navigate back to Chat.
        BackHandler(enabled = pagerState.currentPage != PanelState.CENTER.toPageIndex()) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(PanelState.CENTER.toPageIndex())
            }
        }

        val navItems = com.mocca.app.ui.components.navigation.defaultBottomNavItems

        // ---------------------------------------------------------------
        // Root layout
        // ---------------------------------------------------------------
        Box(modifier = Modifier.fillMaxSize()) {
            // Background layer (performance-gated)
            if (performance.useAmbientEffects) {
                DynamicExpressiveBackground()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.background)
                )
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                bottomBar = {
                    val chatState by chatScreenModel.state.collectAsState()
                    Box(
                        modifier = Modifier.padding(
                            horizontal = AppSpacing.screenPaddingHorizontal,
                            vertical = AppSpacing.xs
                        )
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            color = AppColors.surfaceContainer,
                            shape = AppShapes.floatingToolbar,
                            tonalElevation = 3.dp
                        ) {
                            PersistentNavRow(
                                dragProgress = dragProgress,
                                onItemClick = { panelState ->
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(panelState.toPageIndex())
                                    }
                                },
                                items = navItems,
                                showLabels = true,
                                isAgentRunning = !chatState.isSessionIdle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(NavConstants.NavigationModeHeight)
                            )
                        }
                    }
                }
            ) { paddingValues ->
                // Adjust bottom padding: when IME is visible the bottom bar
                // is covered by the keyboard, so we remove the bar clearance.
                val adjustedPadding = if (isImeVisible) {
                    PaddingValues(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        top = paddingValues.calculateTopPadding(),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                        bottom = 0.dp
                    )
                } else {
                    paddingValues
                }

                HorizontalPager(
                    state = pagerState,
                    // Keep 1 page beyond the viewport in composition.
                    // With 3 pages and Chat at index 1, this means Chat is ALWAYS
                    // in composition regardless of which page is visible – streaming
                    // stays alive, state is preserved.
                    beyondViewportPageCount = 1,
                    // Block horizontal swipe while keyboard is open to prevent
                    // accidental page changes and spurious layout recompositions.
                    userScrollEnabled = !isImeVisible,
                    // Stable key prevents unnecessary recomposition on pager updates.
                    key = { page -> page },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(adjustedPadding)
                ) { page ->
                    when (page) {
                        0 -> {
                            val chatState by chatScreenModel.state.collectAsState()
                            com.mocca.app.ui.screens.panels.ContextHistoryPanel(
                                modifier = Modifier.fillMaxSize(),
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
                                        coroutineScope.launch { pagerState.animateScrollToPage(PanelState.CENTER.toPageIndex()) }
                                    }
                                },
                                onNewSessionClick = {
                                    screenModel.createSession {
                                        coroutineScope.launch { pagerState.animateScrollToPage(PanelState.CENTER.toPageIndex()) }
                                    }
                                },
                                onRefresh = { screenModel.refreshAll() },
                                onGroupExpandToggle = { groupId ->
                                    screenModel.toggleGroupExpanded(groupId)
                                }
                            )
                        }
                        1 -> MainChatPanel(
                            isActive = pagerState.settledPage == PanelState.CENTER.toPageIndex(),
                            mainScreenModel = screenModel,
                            chatScreenModel = chatScreenModel,
                            navigator = navigator,
                            onAttachClick = { filePickerLauncher.launch() },
                            targetMessageId = targetMessageId,
                            onTargetMessageHandled = { targetMessageId = null },
                            onSearchClick = { showGlobalSearch = true }
                        )
                        2 -> com.mocca.app.ui.screens.panels.DashboardPanel(
                            screenModel = dashboardScreenModel,
                            onMcpConfigClick = { navigator.push(com.mocca.app.ui.screens.mcp.McpScreen()) },
                            onSettingsClick = { navigator.push(com.mocca.app.ui.screens.settings.SettingsScreen()) },
                            onGitClick = { navigator.push(com.mocca.app.ui.screens.git.GitScreen()) },
                            onFilesClick = { navigator.push(com.mocca.app.ui.screens.files.FilesScreen()) },
                            onSkillsClick = { },
                            onSkillClick = { },
                            onTerminalClick = { navigator.push(com.mocca.app.ui.screens.terminal.TerminalScreen()) }
                        )
                    }
                }
            }

            // Global search overlay – modal, sits above the pager
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
