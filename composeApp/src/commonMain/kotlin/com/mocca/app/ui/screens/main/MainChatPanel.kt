package com.mocca.app.ui.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.mocca.app.domain.model.VoiceInputState
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.components.navigation.ChatInputContent
import com.mocca.app.ui.screens.chat.ChatScreenModel
import com.mocca.app.ui.screens.chat.ChatContent
import com.mocca.app.ui.screens.chat.ScrollDirection
import com.mocca.app.ui.screens.onboarding.ProgressiveOnboardingScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
fun MainChatPanel(
    isActive: Boolean,
    mainScreenModel: MainScreenModel,
    chatScreenModel: ChatScreenModel,
    navigator: Navigator,
    onAttachClick: () -> Unit,
    targetMessageId: String?,
    onTargetMessageHandled: () -> Unit,
    onSearchClick: () -> Unit
) {
    val state by mainScreenModel.state.collectAsState()
    val chatState by chatScreenModel.state.collectAsState()
    val inputText by chatScreenModel.inputText.collectAsState()
    val voicePermissionRequestToken by chatScreenModel.voicePermissionRequestToken.collectAsState()
    val voiceInputState by chatScreenModel.voiceInputState.collectAsState()

    var scrollDirection by remember { mutableStateOf(ScrollDirection.IDLE) }
    var showScrollToBottom by remember { mutableStateOf(false) }
    var hasNewMessagesWhileScrolledUp by remember { mutableStateOf(false) }
    var scrollToBottomTrigger by remember { mutableStateOf(0L) }

    LaunchedEffect(isActive) {
        if (!isActive) {
            scrollDirection = ScrollDirection.IDLE
            showScrollToBottom = false
            hasNewMessagesWhileScrolledUp = false
        }
    }

    if (state.currentSessionId != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            val currentSession = state.sessions.find { it.id == state.currentSessionId }
            MainSessionTabs(
                sessions = state.sessionGroups.map { it.parent },
                selectedSessionId = currentSession?.effectiveParentID ?: currentSession?.id,
                onSessionSelected = { mainScreenModel.selectSession(it) }
            )
            ModernTopBar(
                title = currentSession?.title ?: "Chat",
                sessionId = state.currentSessionId,
                actions = {
                    MoccaIconButton(
                        icon = Icons.Default.Search,
                        onClick = onSearchClick,
                        contentDescription = "Open global search",
                        iconColor = AppColors.onSurface,
                        borderColor = AppColors.outline.copy(alpha = 0.35f)
                    )
                    MoccaIconButton(
                        icon = Icons.Default.Share,
                        onClick = { chatScreenModel.openShareDialog() },
                        contentDescription = if (chatState.session?.shareID != null) {
                            "Shared session options"
                        } else {
                            "Share session"
                        },
                        iconColor = if (chatState.session?.shareID != null) {
                            AppColors.primary
                        } else {
                            AppColors.onSurface
                        },
                        borderColor = if (chatState.session?.shareID != null) {
                            AppColors.primary.copy(alpha = 0.45f)
                        } else {
                            AppColors.outline.copy(alpha = 0.35f)
                        }
                    )
                }
            )

            if (!state.isConnected) {
                val connectionError = state.connectionError
                ConnectionStatusBanner(
                    status = when {
                        state.isConnecting -> ConnectionBannerStatus.Connecting(
                            attempt = state.connectionAttempt,
                            maxAttempts = state.maxConnectionAttempts
                        )
                        state.isWaitingForNetwork -> ConnectionBannerStatus.WaitingForNetwork
                        connectionError != null -> ConnectionBannerStatus.Error(connectionError)
                        else -> ConnectionBannerStatus.Disconnected()
                    },
                    onRetryClick = { mainScreenModel.retryConnection() },
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

            Box(modifier = Modifier.weight(1f)) {
                ChatContent(
                    screenModel = chatScreenModel,
                    onScrollDirectionChange = { scrollDirection = it },
                    onScrollToBottomStateChange = { show, hasNew ->
                        showScrollToBottom = show
                        hasNewMessagesWhileScrolledUp = hasNew
                    },
                    scrollToBottomTrigger = scrollToBottomTrigger,
                    targetMessageId = targetMessageId,
                    onTargetMessageHandled = onTargetMessageHandled
                )

                ScrollToBottomButton(
                    isVisible = showScrollToBottom,
                    hasNewMessages = hasNewMessagesWhileScrolledUp,
                    onClick = {
                        scrollToBottomTrigger += 1L
                        showScrollToBottom = false
                        hasNewMessagesWhileScrolledUp = false
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = AppSpacing.lg)
                )
            }

            // Chat input sits at the bottom of the Chat page only.
            // imePadding() here ensures the input lifts above the keyboard.
            Surface(
                modifier = Modifier.fillMaxWidth().imePadding(),
                color = Color.Transparent
            ) {
                ChatInputContent(
                    voicePermissionRequestToken = voicePermissionRequestToken,
                    onVoicePermissionResult = { chatScreenModel.onVoicePermissionResult(it) },
                    inputText = inputText,
                    onInputTextChange = { chatScreenModel.updateInputText(it) },
                    onSendClick = { chatScreenModel.sendMessage() },
                    onMicClick = { chatScreenModel.toggleVoiceInput() },
                    inputEnabled = chatState.connectionStatus is com.mocca.app.domain.model.ConnectionStatus.Connected &&
                        chatState.isSessionIdle &&
                        chatState.aiConfigState.isReady,
                    placeholder = "Type a message...",
                    isSessionIdle = chatState.isSessionIdle,
                    isVoiceListening = voiceInputState is VoiceInputState.Listening,
                    isVoiceAvailable = voiceInputState !is VoiceInputState.NotAvailable,
                    onAbortClick = { chatScreenModel.abortSession() },
                    modelName = chatState.modelName,
                    agentName = chatState.agentName,
                    modelPickerState = chatState.modelPickerState,
                    onModelSelected = { providerId, modelId ->
                        chatScreenModel.selectModel(providerId, modelId)
                    },
                    variants = chatState.variantPickerState.variants,
                    selectedVariantId = chatState.selectedVariantId,
                    onVariantSelected = { chatScreenModel.selectVariant(it) },
                    modes = chatState.modes,
                    selectedModeId = chatState.selectedModeId,
                    onModeSelected = { chatScreenModel.selectMode(it) },
                    attachedFiles = chatState.attachedFiles,
                    onRemoveAttachment = { chatScreenModel.removeAttachment(it) },
                    onAttachClick = onAttachClick,
                    commands = chatState.commands,
                    onCommandSelected = { chatScreenModel.executeCommand(it) },
                    onModeSelectedForMention = { chatScreenModel.selectMode(it.id) },
                    onHistoryUp = { chatScreenModel.navigateHistoryUp() },
                    onHistoryDown = { chatScreenModel.navigateHistoryDown() }
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!state.isConnected) {
                val connectionError = state.connectionError
                ConnectionStatusBanner(
                    status = when {
                        state.isConnecting -> ConnectionBannerStatus.Connecting(
                            attempt = state.connectionAttempt,
                            maxAttempts = state.maxConnectionAttempts
                        )
                        state.isWaitingForNetwork -> ConnectionBannerStatus.WaitingForNetwork
                        connectionError != null -> ConnectionBannerStatus.Error(connectionError)
                        else -> ConnectionBannerStatus.Disconnected()
                    },
                    onRetryClick = { mainScreenModel.retryConnection() },
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
                Text(
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
