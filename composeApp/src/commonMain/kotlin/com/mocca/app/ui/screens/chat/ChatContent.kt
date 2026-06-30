package com.mocca.app.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.ui.components.ErrorScreen
import com.mocca.app.ui.components.QuestionDialog
import com.mocca.app.ui.components.chat.PermissionBanner
import com.mocca.app.ui.components.chat.TodoListPanel
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.components.modern.message.*
import com.mocca.app.ui.theme.*
import com.mocca.app.util.TimeFormatter
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import com.mocca.app.ui.screens.files.FilesScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope
import com.mocca.app.domain.model.AgentActivity

enum class HeroMomentType { NONE, CONNECTED, COMPLETED }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatContent(
    screenModel: ChatScreenModel,
    onScrollDirectionChange: (ScrollDirection) -> Unit = {},
    onScrollToBottomStateChange: (showButton: Boolean, hasNewMessages: Boolean) -> Unit = { _, _ -> },
    scrollToBottomTrigger: Long = 0L,
    targetMessageId: String? = null,
    onTargetMessageHandled: () -> Unit = {}
) {
    val state by screenModel.state.collectAsState()
    val streamingText by screenModel.streamingText.collectAsState()
    val aggregatedMessages by screenModel.aggregatedMessages.collectAsState()
    val performance = LocalAppPerformance.current

    val listState = rememberLazyListState(
        cacheWindow = LazyLayoutCacheWindow(
            ahead = performance.lazyListCacheAhead,
            behind = performance.lazyListCacheBehind
        )
    )
    val haptic = LocalHapticFeedback.current

    val navigator = LocalNavigator.current
    val onFileClick: (String) -> Unit = remember(navigator) {
        { _ -> navigator?.push(FilesScreen()) }
    }
    
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    // Track scroll direction for dock auto-hide
    var previousScrollIndex by remember { mutableStateOf(0) }
    var previousScrollOffset by remember { mutableStateOf(0) }
    
    LaunchedEffect(listState) {
        snapshotFlow { 
            Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) 
        }
        .collect { pair ->
            val currentIndex = pair.first
            val currentOffset = pair.second
            // Determine scroll direction based on position changes
            val direction = when {
                currentIndex > previousScrollIndex -> ScrollDirection.UP
                currentIndex < previousScrollIndex -> ScrollDirection.DOWN
                currentOffset > previousScrollOffset + 10 -> ScrollDirection.UP
                currentOffset < previousScrollOffset - 10 -> ScrollDirection.DOWN
                else -> ScrollDirection.IDLE
            }
            
            if (direction != ScrollDirection.IDLE) {
                onScrollDirectionChange(direction)
            }
            
            previousScrollIndex = currentIndex
            previousScrollOffset = currentOffset
        }
    }
    
    LaunchedEffect(streamingText.isNotEmpty()) {
        if (streamingText.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Auto-scroll state management
    val autoScrollState = rememberAutoScrollState(listState = listState)

    var currentHeroMoment by remember { mutableStateOf(HeroMomentType.NONE) }

    // Hero Moment: Connection Success
    val isConnected = state.connectionStatus is com.mocca.app.domain.model.ConnectionStatus.Connected
    var lastConnectionState by remember { mutableStateOf(isConnected) }
    
    LaunchedEffect(isConnected) {
        if (isConnected && !lastConnectionState) {
            currentHeroMoment = HeroMomentType.CONNECTED
            kotlinx.coroutines.delay(3000)
            currentHeroMoment = HeroMomentType.NONE
        }
        lastConnectionState = isConnected
    }

    // Hero Moment: Completion State
    var wasNotIdle by remember { mutableStateOf(false) }
    LaunchedEffect(state.isSessionIdle) {
        if (!state.isSessionIdle) {
            wasNotIdle = true
        } else if (wasNotIdle && state.isSessionIdle) {
            wasNotIdle = false
            currentHeroMoment = HeroMomentType.COMPLETED
            kotlinx.coroutines.delay(3000)
            currentHeroMoment = HeroMomentType.NONE
        }
    }

    // Apply auto-scroll effect - respects user scroll position
    AutoScrollEffect(
        listState = listState,
        messageCount = aggregatedMessages.size,
        streamingText = streamingText,
        autoScrollState = autoScrollState,
        enabled = !state.isLoading
    )

    val isAtTop by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 5
        }
    }
    // Show scroll-to-bottom button when user has scrolled up (read listState directly, not autoScrollState)
    val showScrollToBottom by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
            listState.firstVisibleItemScrollOffset > 50
        }
    }

    // Track new messages while scrolled up (for badge indicator)
    var hasNewMessagesWhileScrolledUp by remember { mutableStateOf(false) }

    // Consolidate scroll-position tracking into a single snapshotFlow to avoid
    // back-writing from multiple LaunchedEffects that read layout state independently.
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val isAtBottom = index == 0 && offset <= 50
            if (isAtBottom) {
                hasNewMessagesWhileScrolledUp = false
            }
        }
    }

    LaunchedEffect(aggregatedMessages.size) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50) {
            hasNewMessagesWhileScrolledUp = true
        }
    }

    // Report scroll-to-bottom state to parent (MainScreen)
    LaunchedEffect(showScrollToBottom, hasNewMessagesWhileScrolledUp) {
        onScrollToBottomStateChange(showScrollToBottom, hasNewMessagesWhileScrolledUp)
    }

    LaunchedEffect(isAtTop, aggregatedMessages.size) {
        if (isAtTop && aggregatedMessages.size >= 80) {
            screenModel.loadMoreMessages()
        }
    }

    // Allow parent (MainScreen) to trigger scroll-to-bottom
    LaunchedEffect(scrollToBottomTrigger) {
        if (scrollToBottomTrigger > 0L) {
            listState.animateScrollToBottom()
            hasNewMessagesWhileScrolledUp = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        
        // Main content area with sticky overlays
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            androidx.compose.animation.AnimatedVisibility(
                visible = currentHeroMoment != HeroMomentType.NONE,
                enter = fadeIn(MaterialTheme.motionScheme.fastSpatialSpec()) + 
                        scaleIn(initialScale = 0.5f, animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.slowSpatialSpec()) + 
                       scaleOut(targetScale = 1.5f, animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()),
                modifier = Modifier.align(Alignment.Center).zIndex(100f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Background Kinetic Shape
                    val shape = if (currentHeroMoment == HeroMomentType.CONNECTED) AppShapes.flower else AppShapes.gem
                    val color = if (currentHeroMoment == HeroMomentType.CONNECTED) AppColors.statusOnline else AppColors.accent
                    
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(color.copy(alpha = 0.15f), shape)
                            .border(2.dp, color.copy(alpha = 0.3f), shape)
                    )
                    
                    // Reveal Text with Emphasized Typography
                    Text(
                        text = if (currentHeroMoment == HeroMomentType.CONNECTED) "CONNECTED" else "COMPLETED",
                        style = AppTypography.displayLargeEmphasized,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            ChatOverlayHost(screenModel = screenModel)

            // In-app agent error banner (session.error / agent.status error)
            val agentError = state.agentError
            if (agentError != null) {
                ChatErrorBanner(
                    error = agentError,
                    onDismiss = screenModel::dismissAgentError,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(50f),
                )
            }

            ChatMessagePane(
                screenModel = screenModel,
                listState = listState,
                aggregatedMessages = aggregatedMessages,
                streamingText = streamingText,
                isLoading = state.isLoading,
                error = state.error,
                isSending = state.isSending,
                isThinking = state.isThinking,
                thinkingContent = state.thinkingContent,
                thinkingElapsedMs = state.thinkingElapsedMs,
                agentActivity = state.agentActivity,
                pendingPermissionCount = state.pendingPermissions.size,
                pendingQuestionCount = state.pendingQuestions.size,
                showTimestamps = state.showTimestamps,
                showTokenCounts = state.showTokenCounts,
                onFileClick = onFileClick,
                targetMessageId = targetMessageId,
                onTargetMessageHandled = onTargetMessageHandled
            )

            QuestionDialogHost(screenModel = screenModel)
        }

ShareSessionDialogHost(screenModel = screenModel)
        ExportChatDialogHost(screenModel = screenModel)
        EditMessagePartDialogHost(screenModel = screenModel)
        ForkSessionDialogHost(screenModel = screenModel)
    }
}

@Composable
private fun BoxScope.ChatOverlayHost(
    screenModel: ChatScreenModel
) {
    val state by screenModel.state.collectAsState()

    TodoListPanel(
        todos = state.todos,
        isVisible = state.showTodoPanel,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .zIndex(10f)
    )

    state.pendingPermission?.let { permission ->
        PermissionBanner(
            permission = permission,
            onApprove = { screenModel.approvePermission() },
            onApproveAlways = { screenModel.approvePermissionAlways() },
            onDeny = { screenModel.denyPermission() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (state.showTodoPanel && state.todos.isNotEmpty()) 56.dp else 0.dp)
                .zIndex(11f)
        )
    }

    if (state.session?.isReverted == true) {
        RevertedSessionBanner(
            onResume = { screenModel.unrevertSession() }
        )
    }

    if (state.sessionDisposed) {
        SessionDisposedBanner(
            reason = state.disposalReason ?: "Server session ended",
            onDismiss = { screenModel.dismissDisposal() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(12f)
        )
    }

    state.error?.let { error ->
        TerminalErrorOverlay(
            error = error,
            onDismiss = { screenModel.clearError() }
        )
    }
}

@Composable
private fun ChatMessagePane(
    screenModel: ChatScreenModel,
    listState: LazyListState,
    aggregatedMessages: ImmutableList<Message>,
    streamingText: String,
    isLoading: Boolean,
    error: String?,
    isSending: Boolean,
    isThinking: Boolean,
    thinkingContent: String,
    thinkingElapsedMs: Long,
    agentActivity: AgentActivity?,
    pendingPermissionCount: Int,
    pendingQuestionCount: Int,
    showTimestamps: Boolean,
    showTokenCounts: Boolean,
    onFileClick: (String) -> Unit,
    targetMessageId: String?,
    onTargetMessageHandled: () -> Unit
) {
    if (isLoading && aggregatedMessages.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.screenPaddingHorizontal, vertical = AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            repeat(3) { MessageSkeleton() }
        }
    } else if (error != null && aggregatedMessages.isEmpty()) {
        ErrorScreen(message = error) {
            screenModel.retry()
        }
    } else if (aggregatedMessages.isEmpty()) {
        EmptySessionState()
    } else {
        val displayMessages by remember(aggregatedMessages) {
            derivedStateOf {
                aggregatedMessages.filter { msg ->
                    msg.role == MessageRole.USER || msg.parts.isNotEmpty()
                }
            }
        }
        val reversedMessages = remember(displayMessages) { displayMessages.asReversed() }
        val activeMessage = remember(displayMessages, agentActivity) {
            agentActivity?.messageId?.let { id -> displayMessages.firstOrNull { it.id == id } }
        }
        val activeHasTextPart = activeMessage?.parts?.any { part ->
            part is MessagePart.Text && part.text.isNotBlank()
        } == true
        val activeHasReasoningPart = activeMessage?.parts?.any { part ->
            when (part) {
                is MessagePart.Reasoning -> part.content.isNotBlank()
                is MessagePart.Thinking -> part.content.isNotBlank()
                else -> false
            }
        } == true
        val showDetachedThinking = isThinking && !activeHasReasoningPart
        val showDetachedStreaming = streamingText.isNotEmpty() && !activeHasTextPart

        LaunchedEffect(targetMessageId, reversedMessages, isSending, showDetachedThinking, showDetachedStreaming) {
            val messageId = targetMessageId ?: return@LaunchedEffect
            val targetIndex = reversedMessages.indexOfFirst { it.id == messageId }
            if (targetIndex >= 0) {
                val prefixItemCount = 1 +
                    (if (agentActivity != null || pendingPermissionCount > 0 || pendingQuestionCount > 0) 1 else 0) +
                    (if (isSending && !showDetachedStreaming && !showDetachedThinking) 1 else 0) +
                    (if (showDetachedThinking) 1 else 0) +
                    (if (showDetachedStreaming) 1 else 0)

                listState.animateScrollToItem(prefixItemCount + targetIndex)
                onTargetMessageHandled()
            }
        }

        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.screenPaddingHorizontal),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = AppSpacing.bottomBarExpandedMinHeight + 56.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            item(
                key = "bottom_spacer",
                contentType = "spacer"
            ) { Spacer(modifier = Modifier.height(32.dp)) }

            if (agentActivity != null || pendingPermissionCount > 0 || pendingQuestionCount > 0) {
                item(
                    key = "agent_activity_strip",
                    contentType = "agent_activity"
                ) {
                    AgentActivityStrip(
                        activity = agentActivity,
                        permissionCount = pendingPermissionCount,
                        questionCount = pendingQuestionCount
                    )
                }
            }

            if (isSending && !showDetachedStreaming && !showDetachedThinking) {
                item(
                    key = "processing_indicator",
                    contentType = "processing"
                ) { ModernProcessingIndicator() }
            }

            if (showDetachedThinking) {
                item(
                    key = "thinking_indicator",
                    contentType = "thinking"
                ) {
                    ModernThinkingIndicator(
                        thinkingContent = thinkingContent,
                        elapsedMs = thinkingElapsedMs
                    )
                }
            }

            if (showDetachedStreaming) {
                item(
                    key = "streaming_message",
                    contentType = "streaming"
                ) { ModernStreamingMessage(text = streamingText) }
            }

            itemsIndexed(
                items = reversedMessages,
                key = { _, it -> it.id },
                contentType = { _, _ -> "message" }
            ) { index, message ->
                val nextMessage = reversedMessages.getOrNull(index + 1)
                val prevMessage = reversedMessages.getOrNull(index - 1)

                val isFirstInGroup = prevMessage == null ||
                    prevMessage.role != message.role ||
                    (message.createdAt - prevMessage.createdAt) > 300_000

                val showDateHeader = if (nextMessage == null) {
                    TimeFormatter.formatDate(message.createdAt)
                } else {
                    val nextDate = TimeFormatter.formatDate(nextMessage.createdAt)
                    val currentDate = TimeFormatter.formatDate(message.createdAt)
                    if (nextDate != currentDate) currentDate else null
                }

                MessageRow(
                    message = message,
                    modifier = Modifier.animateItem(
                        fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                        placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                    ),
                    isFirstInGroup = isFirstInGroup,
                    dateHeader = showDateHeader,
                    onFork = { screenModel.openForkDialog() },
                    onRevert = { screenModel.revertSession(message) },
                    showTimestamps = showTimestamps,
                    showTokenCounts = showTokenCounts,
                    onDelete = { screenModel.deleteMessage(message) },
                    onDeletePart = { partId -> screenModel.deleteMessagePart(message, partId) },
                    onEditPart = { part -> screenModel.showEditPart(message, part) },
                    onFileClick = onFileClick
                )
            }
        }
    }
}

@Composable
private fun AgentActivityStrip(
    activity: AgentActivity?,
    permissionCount: Int,
    questionCount: Int,
    modifier: Modifier = Modifier
) {
    val stage = activity?.stage ?: AgentActivity.STAGE_IDLE
    val status = when (stage) {
        AgentActivity.STAGE_QUEUED -> "Queued"
        AgentActivity.STAGE_REASONING -> "Reasoning"
        AgentActivity.STAGE_TOOL -> activity?.title ?: "Tool running"
        AgentActivity.STAGE_WRITING -> "Writing"
        AgentActivity.STAGE_ERROR -> "Attention needed"
        AgentActivity.STAGE_IDLE -> "Idle"
        else -> stage.replaceFirstChar { it.uppercase() }
    }
    val color = when (stage) {
        AgentActivity.STAGE_ERROR -> AppColors.statusError
        AgentActivity.STAGE_TOOL -> AppColors.statusProcessing
        AgentActivity.STAGE_REASONING -> AppColors.statusThinking
        AgentActivity.STAGE_WRITING -> AppColors.accentGreen
        else -> AppColors.statusInfo
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.small,
        color = AppColors.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, AppShapes.statusDot)
            )
            Text(
                text = status,
                style = AppTypography.labelSmall,
                color = AppColors.onSurface
            )
            if (permissionCount > 0) {
                Text(
                    text = "$permissionCount permission",
                    style = AppTypography.labelSmall,
                    color = AppColors.warning
                )
            }
            if (questionCount > 0) {
                Text(
                    text = "$questionCount question",
                    style = AppTypography.labelSmall,
                    color = AppColors.accentBright
                )
            }
        }
    }
}

@Composable
private fun QuestionDialogHost(
    screenModel: ChatScreenModel
) {
    val state by screenModel.state.collectAsState()

    state.pendingQuestion?.let { question ->
        QuestionDialog(
            request = question,
            onAnswer = { answers -> screenModel.answerQuestion(answers) },
            onReject = { screenModel.rejectQuestion() }
        )
    }
}

@Composable
private fun ShareSessionDialogHost(
    screenModel: ChatScreenModel
) {
    val showShareDialog by screenModel.showShareDialog.collectAsState()

    if (!showShareDialog) return

    val state by screenModel.state.collectAsState()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    val isShared = state.session?.shareID != null
    val shareUrl = if (isShared) "https://opencode.dev/s/${state.session?.shareID}" else ""

    AlertDialog(
        onDismissRequest = { screenModel.dismissShareDialog() },
        containerColor = AppColors.surfaceContainerHigh,
        shape = AppShapes.dialog,
        title = { Text(if (isShared) "SESSION SHARED" else "SHARE SESSION", style = AppTypography.labelMedium, color = AppColors.onSurface) },
        text = {
            Column {
                if (isShared) {
                    Text("This session is publicly accessible.", color = AppColors.onSurfaceVariant, style = AppTypography.bodySmall)
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    MoccaInput(
                        value = shareUrl,
                        onValueChange = {},
                        showPrompt = false,
                        enabled = false
                    )
                } else {
                    Text("Make this session publicly accessible?", color = AppColors.onSurface, style = AppTypography.bodyMedium)
                }
            }
        },
        confirmButton = {
            if (isShared) {
                MoccaCompactButton(
                    text = "COPY LINK",
                    onClick = {
                        clipboard.setText(AnnotatedString(shareUrl))
                        screenModel.dismissShareDialog()
                    }
                )
            } else {
                MoccaCompactButton(
                    text = "SHARE PUBLICLY",
                    onClick = {
                        screenModel.shareSession()
                    }
                )
            }
        },
        dismissButton = {
            if (isShared) {
                MoccaTextButton(
                    text = "UNSHARE",
                    onClick = {
                        screenModel.unshareSession()
                        screenModel.dismissShareDialog()
                    },
                    textColor = AppColors.error
                )
            } else {
                MoccaTextButton(
                    text = "CANCEL",
                    onClick = { screenModel.dismissShareDialog() }
                )
            }
        }
    )
}

@Composable
private fun EditMessagePartDialogHost(
    screenModel: ChatScreenModel
) {
    val editingPart by screenModel.editingPart.collectAsState()

    editingPart?.let { (_, part) ->
        EditMessagePartDialog(
            part = part,
            onConfirm = { content -> screenModel.commitEditPart(content) },
            onDismiss = { screenModel.dismissEditPart() }
        )
    }
}

@Composable
private fun ForkSessionDialogHost(
    screenModel: ChatScreenModel
) {
    val showForkDialog by screenModel.showForkDialog.collectAsState()

    if (!showForkDialog) return

    val aggregatedMessages by screenModel.aggregatedMessages.collectAsState()

    ForkSessionDialog(
        messages = aggregatedMessages,
        onFork = { message -> screenModel.forkSession(message) },
        onDismiss = { screenModel.dismissForkDialog() }
    )
}

@Composable
private fun ExportChatDialogHost(
    screenModel: ChatScreenModel
) {
    val showExportDialog by screenModel.showExportDialog.collectAsState()

    if (!showExportDialog) return

    val state by screenModel.state.collectAsState()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = { screenModel.dismissExportDialog() },
        containerColor = AppColors.surfaceContainerHigh,
        shape = AppShapes.dialog,
        title = { Text("EXPORT CHAT", style = AppTypography.labelMedium, color = AppColors.onSurface) },
        text = {
            Text(
                "Copy chat to clipboard as Markdown?",
                color = AppColors.onSurface,
                style = AppTypography.bodyMedium
            )
        },
        confirmButton = {
            MoccaCompactButton(
                text = "COPY",
                onClick = {
                    val markdown = screenModel.exportChatToMarkdown()
                    clipboard.setText(AnnotatedString(markdown))
                    screenModel.dismissExportDialog()
                }
            )
        },
        dismissButton = {
            MoccaTextButton(
                text = "CANCEL",
                onClick = { screenModel.dismissExportDialog() }
            )
        }
    )
}
