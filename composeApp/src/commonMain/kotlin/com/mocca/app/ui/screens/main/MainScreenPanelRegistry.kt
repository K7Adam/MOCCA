package com.mocca.app.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.mocca.app.domain.model.VoiceInputState
import com.mocca.app.ui.components.navigation.BottomNavItem
import com.mocca.app.ui.navigation.PanelStateHolder
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppTypography

internal enum class MainPanelPlacement {
    LEFT,
    CENTER,
    RIGHT
}

internal data class MainPanelDefinition(
    val id: String,
    val placement: MainPanelPlacement,
    val panelState: PanelState,
    val title: String,
    val iconKey: String,
    val navLabel: String,
    val navIcon: ImageVector,
    val targetProgress: Float,
    val content: @Composable () -> Unit
)

internal data class MainScreenPanelScope(
    val navigator: Navigator,
    val screenModel: MainScreenModel,
    val chatScreenModel: com.mocca.app.ui.screens.chat.ChatScreenModel,
    val dashboardScreenModel: com.mocca.app.ui.screens.panels.DashboardScreenModel,
    val state: MainScreenState,
    val chatState: com.mocca.app.ui.screens.chat.ChatState,
    val inputText: String,
    val shellMode: Boolean,
    val voicePermissionRequestToken: Int,
    val voiceInputState: VoiceInputState,
    val panelState: PanelStateHolder,
    val onAttachClick: () -> Unit,
    val scrollToBottomTrigger: Long,
    val showScrollToBottom: Boolean,
    val hasNewMessagesWhileScrolledUp: Boolean,
    val onScrollDirectionChange: (com.mocca.app.ui.screens.chat.ScrollDirection) -> Unit,
    val onScrollToBottomStateChange: (Boolean, Boolean) -> Unit,
    val onScrollToBottomClick: () -> Unit,
    val onRetryConnection: () -> Unit,
    val onOpenSetup: () -> Unit,
    val onSessionTabSelected: (String) -> Unit,
    val onSearchClick: () -> Unit,
    val targetMessageId: String?,
    val onTargetMessageHandled: () -> Unit
)

internal object MainScreenPanelRegistry {
    const val CONTEXT_HISTORY_PANEL_ID = "context_history"
    const val CHAT_PANEL_ID = "chat"
    const val DASHBOARD_PANEL_ID = "dashboard"

    fun getAll(scope: MainScreenPanelScope): List<MainPanelDefinition> {
        return listOf(
            MainPanelDefinition(
                id = CONTEXT_HISTORY_PANEL_ID,
                placement = MainPanelPlacement.LEFT,
                panelState = PanelState.LEFT_OPEN,
                title = "Context & History",
                iconKey = "history",
                navLabel = "Sessions",
                navIcon = Icons.Default.Computer,
                targetProgress = 1f,
                content = {
                    com.mocca.app.ui.screens.panels.ContextHistoryPanel(
                        modifier = Modifier.fillMaxHeight(),
                        sessions = scope.state.sessions,
                        sessionGroups = scope.state.sessionGroups,
                        runningSessionIds = scope.state.runningSessionIds,
                        currentSessionId = scope.state.currentSessionId,
                        mcpStatus = scope.state.mcpStatus,
                        model = scope.chatState.modelName,
                        latency = scope.state.latency,
                        port = scope.state.port,
                        usedTokens = scope.chatState.contextWindowUsage,
                        maxTokens = scope.chatState.maxTokens.takeIf { it > 0 } ?: scope.state.maxTokens,
                        agentName = scope.chatState.agentName,
                        appVersion = scope.state.appVersion,
                        isCreatingSession = scope.state.isCreatingSession,
                        loadingSessionId = scope.state.loadingSessionId,
                        newlyCreatedSessionId = scope.state.newlyCreatedSessionId,
                        isRefreshing = scope.state.isLoading,
                        onSessionClick = { session ->
                            scope.screenModel.selectSession(session.id) {
                                scope.panelState.closePanel()
                            }
                        },
                        onNewSessionClick = {
                            scope.screenModel.createSession {
                                scope.panelState.closePanel()
                            }
                        },
                        onRefresh = { scope.screenModel.refreshAll() },
                        onGroupExpandToggle = { groupId ->
                            scope.screenModel.toggleGroupExpanded(groupId)
                        }
                    )
                }
            ),
            MainPanelDefinition(
                id = CHAT_PANEL_ID,
                placement = MainPanelPlacement.CENTER,
                panelState = PanelState.CENTER,
                title = "Chat",
                iconKey = "chat",
                navLabel = "Chat",
                navIcon = Icons.AutoMirrored.Filled.Chat,
                targetProgress = 0.5f,
                content = {
                    MainChatPanel(scope = scope)
                }
            ),
            MainPanelDefinition(
                id = DASHBOARD_PANEL_ID,
                placement = MainPanelPlacement.RIGHT,
                panelState = PanelState.RIGHT_OPEN,
                title = "Dashboard",
                iconKey = "dashboard",
                navLabel = "Tools",
                navIcon = Icons.Default.Dashboard,
                targetProgress = 0f,
                content = {
                    com.mocca.app.ui.screens.panels.DashboardPanel(
                        screenModel = scope.dashboardScreenModel,
                        onMcpConfigClick = { scope.navigator.push(com.mocca.app.ui.screens.mcp.McpScreen()) },
                        onSettingsClick = { scope.navigator.push(com.mocca.app.ui.screens.settings.SettingsScreen()) },
                        onGitClick = { scope.navigator.push(com.mocca.app.ui.screens.git.GitScreen()) },
                        onFilesClick = { scope.navigator.push(com.mocca.app.ui.screens.files.FilesScreen()) },
                        onSkillsClick = { },
                        onSkillClick = { },
                        onTerminalClick = { scope.navigator.push(com.mocca.app.ui.screens.terminal.TerminalScreen()) }
                    )
                }
            )
        )
    }

    fun getById(panels: List<MainPanelDefinition>, panelId: String): MainPanelDefinition? {
        return panels.firstOrNull { it.id == panelId }
    }

    fun getByPanelState(panels: List<MainPanelDefinition>, panelState: PanelState): MainPanelDefinition? {
        return panels.firstOrNull { it.panelState == panelState }
    }

    fun navigationItems(panels: List<MainPanelDefinition>): List<BottomNavItem> {
        return panels.map { panel ->
            BottomNavItem(
                panelState = panel.panelState,
                icon = panel.navIcon,
                label = panel.navLabel,
                targetProgress = panel.targetProgress
            )
        }
    }
}

@Composable
internal fun rememberMainScreenPanels(scope: MainScreenPanelScope): List<MainPanelDefinition> {
    return remember(
        scope.navigator,
        scope.screenModel,
        scope.chatScreenModel,
        scope.dashboardScreenModel,
        scope.state,
        scope.chatState,
        scope.inputText,
        scope.shellMode,
        scope.voicePermissionRequestToken,
        scope.voiceInputState,
        scope.scrollToBottomTrigger,
        scope.showScrollToBottom,
        scope.hasNewMessagesWhileScrolledUp,
        scope.targetMessageId
    ) {
        MainScreenPanelRegistry.getAll(scope)
    }
}

@Composable
private fun MainChatPanel(scope: MainScreenPanelScope) {
    if (scope.state.currentSessionId != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            val currentSession = scope.state.sessions.find { it.id == scope.state.currentSessionId }
            MainSessionTabs(
                sessions = scope.state.sessionGroups.map { it.parent },
                selectedSessionId = currentSession?.effectiveParentID ?: currentSession?.id,
                onSessionSelected = scope.onSessionTabSelected
            )
            com.mocca.app.ui.components.modern.ModernTopBar(
                title = currentSession?.title ?: "Chat",
                sessionId = scope.state.currentSessionId,
                actions = {
                    com.mocca.app.ui.components.modern.MoccaIconButton(
                        icon = Icons.Default.Search,
                        onClick = scope.onSearchClick,
                        contentDescription = "Open global search",
                        iconColor = com.mocca.app.ui.theme.AppColors.onSurface,
                        borderColor = com.mocca.app.ui.theme.AppColors.outline.copy(alpha = 0.35f)
                    )
                    com.mocca.app.ui.components.modern.MoccaIconButton(
                        icon = Icons.Default.Share,
                        onClick = { scope.chatScreenModel.openShareDialog() },
                        contentDescription = if (scope.chatState.session?.shareID != null) {
                            "Shared session options"
                        } else {
                            "Share session"
                        },
                        iconColor = if (scope.chatState.session?.shareID != null) {
                            com.mocca.app.ui.theme.AppColors.primary
                        } else {
                            com.mocca.app.ui.theme.AppColors.onSurface
                        },
                        borderColor = if (scope.chatState.session?.shareID != null) {
                            com.mocca.app.ui.theme.AppColors.primary.copy(alpha = 0.45f)
                        } else {
                            com.mocca.app.ui.theme.AppColors.outline.copy(alpha = 0.35f)
                        }
                    )
                }
            )

            if (!scope.state.isConnected) {
                com.mocca.app.ui.components.modern.ConnectionStatusBanner(
                    status = when {
                        scope.state.isConnecting -> com.mocca.app.ui.components.modern.ConnectionBannerStatus.Connecting(
                            attempt = scope.state.connectionAttempt,
                            maxAttempts = scope.state.maxConnectionAttempts
                        )

                        scope.state.isWaitingForNetwork -> com.mocca.app.ui.components.modern.ConnectionBannerStatus.WaitingForNetwork
                        scope.state.connectionError != null -> com.mocca.app.ui.components.modern.ConnectionBannerStatus.Error(scope.state.connectionError)
                        else -> com.mocca.app.ui.components.modern.ConnectionBannerStatus.Disconnected()
                    },
                    onRetryClick = scope.onRetryConnection,
                    onSetupClick = scope.onOpenSetup
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                com.mocca.app.ui.screens.chat.ChatContent(
                    screenModel = scope.chatScreenModel,
                    onScrollDirectionChange = scope.onScrollDirectionChange,
                    onScrollToBottomStateChange = scope.onScrollToBottomStateChange,
                    scrollToBottomTrigger = scope.scrollToBottomTrigger,
                    targetMessageId = scope.targetMessageId,
                    onTargetMessageHandled = scope.onTargetMessageHandled
                )

                com.mocca.app.ui.components.modern.ScrollToBottomButton(
                    isVisible = scope.showScrollToBottom,
                    hasNewMessages = scope.hasNewMessagesWhileScrolledUp,
                    onClick = scope.onScrollToBottomClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = AppSpacing.lg)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().imePadding(),
                color = Color.Transparent
            ) {
                com.mocca.app.ui.components.navigation.ChatInputContent(
                    voicePermissionRequestToken = scope.voicePermissionRequestToken,
                    onVoicePermissionResult = { scope.chatScreenModel.onVoicePermissionResult(it) },
                    inputText = scope.inputText,
                    onInputTextChange = { scope.chatScreenModel.updateInputText(it) },
                    onSendClick = { scope.chatScreenModel.sendMessage() },
                    onMicClick = { scope.chatScreenModel.toggleVoiceInput() },
                    inputEnabled = scope.chatState.connectionStatus is com.mocca.app.domain.model.ConnectionStatus.Connected &&
                        scope.chatState.isSessionIdle &&
                        scope.chatState.aiConfigState.isReady,
                    placeholder = "Type a message...",
                    isSessionIdle = scope.chatState.isSessionIdle,
                    isVoiceListening = scope.voiceInputState is VoiceInputState.Listening,
                    isVoiceAvailable = scope.voiceInputState !is VoiceInputState.NotAvailable,
                    onAbortClick = { scope.chatScreenModel.abortSession() },
                    modelName = scope.chatState.modelName,
                    agentName = scope.chatState.agentName,
                    modelPickerState = scope.chatState.modelPickerState,
                    onModelSelected = { providerId, modelId ->
                        scope.chatScreenModel.selectModel(providerId, modelId)
                    },
                    variants = scope.chatState.variantPickerState.variants,
                    selectedVariantId = scope.chatState.selectedVariantId,
                    onVariantSelected = { scope.chatScreenModel.selectVariant(it) },
                    modes = scope.chatState.modes,
                    selectedModeId = scope.chatState.selectedModeId,
                    onModeSelected = { scope.chatScreenModel.selectMode(it) },
                    attachedFiles = scope.chatState.attachedFiles,
                    onRemoveAttachment = { scope.chatScreenModel.removeAttachment(it) },
                    onAttachClick = scope.onAttachClick,
                    commands = scope.chatState.commands,
                    onCommandSelected = { scope.chatScreenModel.executeCommand(it) },
                    onModeSelectedForMention = { scope.chatScreenModel.selectMode(it.id) },
                    onHistoryUp = { scope.chatScreenModel.navigateHistoryUp() },
                    onHistoryDown = { scope.chatScreenModel.navigateHistoryDown() }
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!scope.state.isConnected) {
                com.mocca.app.ui.components.modern.ConnectionStatusBanner(
                    status = when {
                        scope.state.isConnecting -> com.mocca.app.ui.components.modern.ConnectionBannerStatus.Connecting(
                            attempt = scope.state.connectionAttempt,
                            maxAttempts = scope.state.maxConnectionAttempts
                        )

                        scope.state.isWaitingForNetwork -> com.mocca.app.ui.components.modern.ConnectionBannerStatus.WaitingForNetwork
                        scope.state.connectionError != null -> com.mocca.app.ui.components.modern.ConnectionBannerStatus.Error(scope.state.connectionError)
                        else -> com.mocca.app.ui.components.modern.ConnectionBannerStatus.Disconnected()
                    },
                    onRetryClick = scope.onRetryConnection,
                    onSetupClick = scope.onOpenSetup
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                com.mocca.app.ui.components.modern.QuoteRotator(
                    versionText = scope.state.appVersion,
                    serverText = if (scope.state.isConnected) "Local server" else null,
                    isLoading = scope.state.isLoadingSession || scope.state.isCreatingSession,
                    loadingText = if (scope.state.isCreatingSession) "Creating session..." else "Loading session..."
                )
            }
        }
    }
}

@Composable
private fun MainSessionTabs(
    sessions: List<com.mocca.app.domain.model.Session>,
    selectedSessionId: String?,
    onSessionSelected: (String) -> Unit
) {
    if (sessions.isEmpty()) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = sessions,
            key = { session -> session.id },
            contentType = { "session-tab" }
        ) { session ->
            val selected = session.id == selectedSessionId
            Surface(
                shape = AppShapes.pill,
                color = if (selected) AppColors.primary.copy(alpha = 0.16f) else AppColors.surfaceContainer,
                tonalElevation = if (selected) 2.dp else 0.dp,
                modifier = Modifier.clickable { onSessionSelected(session.id) }
            ) {
                androidx.compose.material3.Text(
                    text = session.title?.takeIf { it.isNotBlank() } ?: "Untitled",
                    style = AppTypography.labelMedium,
                    color = if (selected) AppColors.primary else AppColors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                )
            }
        }
    }
}
