package com.mocca.app.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
import com.mocca.app.ui.screens.files.FilesScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope

enum class HeroMomentType { NONE, CONNECTED, COMPLETED }

@Composable
fun ChatContent(
    screenModel: ChatScreenModel,
    onScrollDirectionChange: (ScrollDirection) -> Unit = {},
    onScrollToBottomStateChange: (showButton: Boolean, hasNewMessages: Boolean) -> Unit = { _, _ -> },
    scrollToBottomTrigger: Long = 0L
) {
    val state by screenModel.state.collectAsState()
    val inputText by screenModel.inputText.collectAsState()
    val streamingText by screenModel.streamingText.collectAsState()
    val aggregatedMessages by screenModel.aggregatedMessages.collectAsState()
    val editingPart by screenModel.editingPart.collectAsState()
    val showForkDialog by screenModel.showForkDialog.collectAsState()
    val showShareDialog by screenModel.showShareDialog.collectAsState()

    val listState = rememberLazyListState()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
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
    
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        
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
    
    LaunchedEffect(streamingText) {
        if (streamingText.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Auto-scroll state management
    val autoScrollState = rememberAutoScrollState(listState = listState)
    
    // ─── Hero Moments ─────────────────────────────────────────────────────────
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

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 50) {
            hasNewMessagesWhileScrolledUp = false
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

    LaunchedEffect(isAtTop) {
        if (isAtTop) screenModel.loadMoreMessages()
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
        
        if (showShareDialog) {
            val isShared = state.session?.shareID != null
            val shareUrl = if (isShared) "https://opencode.dev/s/${state.session?.shareID}" else ""
            
            AlertDialog(
                onDismissRequest = { screenModel.dismissShareDialog() },
                containerColor = AppColors.surfaceContainerHigh,
                shape = AppShapes.dialog,
                title = { Text(if (isShared) "SESSION SHARED" else "SHARE SESSION", style = AppTypography.labelMedium, color = AppColors.textPrimary) },
                text = {
                    Column {
                        if (isShared) {
                            Text("This session is publicly accessible.", color = AppColors.textSecondary, style = AppTypography.bodySmall)
                            Spacer(modifier = Modifier.height(AppSpacing.md))
                            MoccaInput(
                                value = shareUrl,
                                onValueChange = {},
                                showPrompt = false,
                                enabled = false 
                            )
                        } else {
                            Text("Make this session publicly accessible?", color = AppColors.textPrimary, style = AppTypography.bodyMedium)
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
                            textColor = AppColors.alertRed
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
        
        // Main content area with sticky overlays
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // ─── Expressive Chained Hero Moment ───────────────────────────────────
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
                    val color = if (currentHeroMoment == HeroMomentType.CONNECTED) AppColors.accentGreen else AppColors.accent
                    
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

            // Sticky Todo Panel at top (zIndex ensures it stays above messages)
            TodoListPanel(
                todos = state.todos,
                isVisible = state.showTodoPanel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
            )
            
            // Sticky Permission Banner below todo panel
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
            
            if (state.isLoading && aggregatedMessages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppSpacing.screenPaddingHorizontal, vertical = AppSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    repeat(3) { MessageSkeleton() }
                }
            } else if (state.error != null && aggregatedMessages.isEmpty()) {
                ErrorScreen(message = state.error ?: "UNKNOWN_ERROR") {
                    screenModel.retry()
                }
            } else if (aggregatedMessages.isEmpty()) {
                EmptySessionState()
            } else {
                // SSE-driven content - no manual refresh needed
                val displayMessages by remember(aggregatedMessages) {
                    derivedStateOf {
                        aggregatedMessages.filter { msg ->
                            msg.role == MessageRole.USER || msg.parts.isNotEmpty()
                        }
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
                        // Add extra clearance to ensure the last message clears the compact floating bottom bar (170dp min height)
                        bottom = AppSpacing.bottomBarExpandedMinHeight + 56.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    item(
                        key = "bottom_spacer",
                        contentType = "spacer"
                    ) { Spacer(modifier = Modifier.height(32.dp)) }

                    if (state.isSending && streamingText.isEmpty() && !state.isThinking) {
                        item(
                            key = "processing_indicator",
                            contentType = "processing"
                        ) { ModernProcessingIndicator() }
                    }
                    
                    if (state.isThinking) {
                        item(
                            key = "thinking_indicator",
                            contentType = "thinking"
                        ) {
                            ModernThinkingIndicator(
                                thinkingContent = state.thinkingContent,
                                elapsedMs = state.thinkingElapsedMs
                            )
                        }
                    }
                    
                    if (streamingText.isNotEmpty()) {
                        item(
                            key = "streaming_message",
                            contentType = "streaming"
                        ) { ModernStreamingMessage(text = streamingText) }
                    }
                    
                    items(
                        items = displayMessages.asReversed(),
                        key = { it.id },
                        contentType = { "message" }
                    ) { message ->
                        val index = displayMessages.indexOf(message)
                        val nextMessage = if (index < displayMessages.size - 1) displayMessages[index + 1] else null
                        val prevMessage = if (index > 0) displayMessages[index - 1] else null
                        
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
                            isFirstInGroup = isFirstInGroup,
                            dateHeader = showDateHeader,
                            onFork = { screenModel.openForkDialog() },
                            onRevert = { screenModel.revertSession(message) },
                            showTimestamps = state.showTimestamps,
                            showTokenCounts = state.showTokenCounts,
                            onDelete = { screenModel.deleteMessage(message) },
                            onDeletePart = { partId -> screenModel.deleteMessagePart(message, partId) },
                            onEditPart = { part -> screenModel.showEditPart(message, part) },
                            onFileClick = onFileClick
                        )
                    }
                }
            }
            
            // Permission banner is now handled by sticky PermissionBanner above
            
            editingPart?.let { (_, part) ->
                EditMessagePartDialog(
                    part = part,
                    onConfirm = { content -> screenModel.commitEditPart(content) },
                    onDismiss = { screenModel.dismissEditPart() }
                )
            }
            if (showForkDialog) {
                ForkSessionDialog(
                    messages = aggregatedMessages,
                    onFork = { message -> screenModel.forkSession(message) },
                    onDismiss = { screenModel.dismissForkDialog() }
                )
            }
            state.pendingQuestion?.let { question ->
                QuestionDialog(
                    request = question,
                    onAnswer = { answers -> screenModel.answerQuestion(answers) },
                    onReject = { screenModel.rejectQuestion() }
                )
            }
        }
    }
}
