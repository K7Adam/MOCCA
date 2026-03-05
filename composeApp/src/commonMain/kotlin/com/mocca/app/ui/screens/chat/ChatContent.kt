package com.mocca.app.ui.screens.chat

import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch
import com.mocca.app.ui.screens.files.FilesScreen
import cafe.adriel.voyager.navigator.LocalNavigator

@OptIn(ExperimentalMaterial3Api::class)
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
    
    val listState = rememberLazyListState()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    
    var showShareDialog by remember { mutableStateOf(false) }

    val navigator = LocalNavigator.current
    val onFileClick: (String) -> Unit = remember(navigator) {
        { _ -> navigator?.push(FilesScreen()) }
    }
    
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
                onDismissRequest = { showShareDialog = false },
                containerColor = AppColors.surfaceContainerHigh,
                shape = AppShapes.dialog,
                title = { Text(if (isShared) "SESSION SHARED" else "SHARE SESSION", style = AppTypography.labelMedium, color = AppColors.white) },
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
                            Text("Make this session publicly accessible?", color = AppColors.white, style = AppTypography.bodyMedium)
                        }
                    }
                },
                confirmButton = {
                    if (isShared) {
                        MoccaCompactButton(
                            text = "COPY LINK",
                            onClick = { 
                                clipboard.setText(AnnotatedString(shareUrl))
                                showShareDialog = false
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
                                showShareDialog = false
                            },
                            textColor = AppColors.alertRed
                        )
                    } else {
                        MoccaTextButton(
                            text = "CANCEL",
                            onClick = { showShareDialog = false }
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
                        top = AppSpacing.bottomBarClearance,
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
                            com.mocca.app.util.TimeFormatter.formatDate(message.createdAt)
                        } else {
                            val nextDate = com.mocca.app.util.TimeFormatter.formatDate(nextMessage.createdAt)
                            val currentDate = com.mocca.app.util.TimeFormatter.formatDate(message.createdAt)
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
