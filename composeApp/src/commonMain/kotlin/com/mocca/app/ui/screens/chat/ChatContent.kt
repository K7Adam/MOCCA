package com.mocca.app.ui.screens.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.ui.components.ErrorScreen
import com.mocca.app.ui.components.PermissionRequestDialog
import com.mocca.app.ui.components.QuestionDialog
import com.mocca.app.ui.components.chat.PermissionBanner
import com.mocca.app.ui.components.chat.TodoListPanel
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun MarkdownText(
    markdown: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val mdColor = markdownColor(
        text = color,
        codeText = AppColors.accentGreen,
        codeBackground = AppColors.backgroundVariant,
        inlineCodeText = AppColors.accentGreen,
        inlineCodeBackground = AppColors.backgroundVariant,
        linkText = AppColors.accentGreen
    )
    
    val mdTypography = markdownTypography(
        text = style,
        code = style.copy(fontSize = 12.sp, color = AppColors.accentGreen, fontFamily = FontFamily.Monospace),
        h1 = AppTypography.headlineMedium.copy(color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        h2 = AppTypography.headlineSmall.copy(color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        h3 = AppTypography.titleLarge.copy(color = color, fontWeight = FontWeight.Bold),
        h4 = AppTypography.titleMedium.copy(color = color, fontWeight = FontWeight.Bold),
        h5 = AppTypography.titleSmall.copy(color = color, fontWeight = FontWeight.Bold),
        h6 = AppTypography.labelLarge.copy(color = color, fontWeight = FontWeight.Bold)
    )

    Markdown(
        content = markdown,
        colors = mdColor,
        typography = mdTypography,
        modifier = modifier
    )
}

@Composable
private fun EmptySessionState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            ModernBootSequence()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "MOCCA_AI_v2",
                style = AppTypography.headlineMedium,
                color = AppColors.white,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SYSTEM READY // SELECT_MODEL",
                style = AppTypography.labelExtraSmall,
                color = AppColors.textTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ModernBootSequence() {
    val lines = listOf(
        "MOCCA_OS_BOOT",
        "NETWORK_UPLINK_SECURED",
        "RESOURCES_MAXIMIZED"
    )
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        lines.forEachIndexed { index, line ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index * 120L)
                visible = true
            }
            if (visible) {
                Text(
                    text = line,
                    style = AppTypography.labelExtraSmall,
                    color = if (index == lines.size - 1) AppColors.accentGreen else AppColors.textTertiary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Scroll direction state for dock auto-hide functionality.
 */
enum class ScrollDirection {
    UP,      // Scrolling up (reading older messages) - hide dock
    DOWN,    // Scrolling down (reading newer messages) - show dock
    IDLE     // Not scrolling - show dock
}

/**
 * State holder for auto-scroll behavior.
 * Tracks whether user is at bottom and manages auto-scroll decisions.
 */
data class AutoScrollState(
    val isAtBottom: Boolean = true,
    val shouldAutoScroll: Boolean = true,
    val userHasScrolledUp: Boolean = false
)

/**
 * Threshold (in items) to determine if user is "at bottom" in reverse layout.
 * With reverseLayout=true, index 0 is the bottom (newest messages).
 */
private const val BOTTOM_THRESHOLD = 2

/**
 * Creates and manages auto-scroll state for a LazyColumn with reverseLayout.
 * 
 * With reverseLayout = true:
 * - Index 0 is the BOTTOM (newest messages)
 * - Higher indices are at the TOP (older messages)
 * 
 * @param listState The LazyListState to track
 * @param enabled Whether auto-scroll is enabled
 * @return AutoScrollState indicating current scroll position state
 */
@Composable
fun rememberAutoScrollState(
    listState: LazyListState,
    enabled: Boolean = true
): AutoScrollState {
    val isAtBottom by remember(listState) {
        derivedStateOf {
            if (!enabled) true
            else listState.firstVisibleItemIndex <= BOTTOM_THRESHOLD
        }
    }
    
    val userHasScrolledUp by remember(listState) {
        derivedStateOf {
            if (!enabled) false
            else {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50
            }
        }
    }
    
    val shouldAutoScroll by remember(listState) {
        derivedStateOf {
            if (!enabled) false
            else isAtBottom
        }
    }
    
    return remember(isAtBottom, shouldAutoScroll, userHasScrolledUp) {
        AutoScrollState(
            isAtBottom = isAtBottom,
            shouldAutoScroll = shouldAutoScroll,
            userHasScrolledUp = userHasScrolledUp
        )
    }
}

/**
 * Auto-scroll effect that smoothly scrolls to bottom when new content arrives.
 * Respects user scroll position - won't auto-scroll if user is reading history.
 * 
 * @param listState The LazyListState to control
 * @param messageCount Current message count (triggers scroll when changed)
 * @param streamingText Current streaming text (triggers scroll when non-empty)
 * @param autoScrollState Current auto-scroll state
 * @param enabled Whether auto-scroll is enabled
 */
@Composable
fun AutoScrollEffect(
    listState: LazyListState,
    messageCount: Int,
    streamingText: String,
    autoScrollState: AutoScrollState,
    enabled: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    
    var lastMessageCount by remember { mutableStateOf(messageCount) }
    var wasStreaming by remember { mutableStateOf(false) }
    
    LaunchedEffect(messageCount, streamingText, enabled) {
        if (!enabled) return@LaunchedEffect
        
        val messagesChanged = messageCount != lastMessageCount
        val startedStreaming = streamingText.isNotEmpty() && !wasStreaming
        val isStreaming = streamingText.isNotEmpty()
        
        val shouldScroll = autoScrollState.shouldAutoScroll && (
            messagesChanged || 
            startedStreaming ||
            (isStreaming && autoScrollState.isAtBottom)
        )
        
        if (shouldScroll) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = 0,
                    scrollOffset = 0
                )
            }
        }
        
        lastMessageCount = messageCount
        wasStreaming = streamingText.isNotEmpty()
    }
}

/**
 * Extension function to smoothly scroll to the bottom of a reverse-layout LazyColumn.
 */
suspend fun LazyListState.animateScrollToBottom() {
    animateScrollToItem(index = 0, scrollOffset = 0)
}

/**
 * Extension function to instantly scroll to the bottom.
 */
suspend fun LazyListState.scrollToBottom() {
    scrollToItem(index = 0, scrollOffset = 0)
}

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
    
    val sessionTitle = remember(state.session) {
        state.session?.let { it.title?.uppercase() ?: "SESSION_${it.id.take(8)}" } ?: "NEW_SESSION"
    }
    
    val listState = rememberLazyListState()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    
    var showShareDialog by remember { mutableStateOf(false) }
    
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

    // Show scroll-to-bottom button when user has scrolled up
    // IMPORTANT: Read listState directly — it's a Compose State holder.
    // Do NOT read autoScrollState here (plain data class = stale closure).
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
                containerColor = AppColors.surfaceElevated,
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
                        top = 80.dp,
                        bottom = 160.dp // Space for unified floating bottom bar
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

                        MessageBubble(
                            message = message,
                            isFirstInGroup = isFirstInGroup,
                            dateHeader = showDateHeader,
                            onFork = { screenModel.forkSession(message) },
                            onRevert = { screenModel.revertSession(message) },
                            showTimestamps = state.showTimestamps,
                            showTokenCounts = state.showTokenCounts
                        )
                    }
                }
            }
            
            // Permission banner is now handled by sticky PermissionBanner above
            
            state.pendingQuestion?.let { question ->
                QuestionDialog(
                    request = question,
                    onAnswer = { answers -> screenModel.answerQuestion(answers) },
                    onReject = { screenModel.rejectQuestion() }
                )
            }

            // ScrollToBottomButton removed — now hosted in MainScreen for liquid glass support
            
            // RichChatInput removed - now handled by UnifiedFloatingBottomBar in MainScreen
        }
    }
}

@Composable
private fun RevertedSessionBanner(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .padding(horizontal = AppSpacing.screenPaddingHorizontal, vertical = AppSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(AppColors.surfaceVariant)
                .padding(AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History, 
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Text(
                        text = "VIEWING OLDER VERSION",
                        style = AppTypography.labelSmall,
                        color = AppColors.textSecondary
                    )
                }
                MoccaTextButton(
                    text = "RESUME LATEST",
                    onClick = onResume,
                    textColor = AppColors.accentGreen
                )
            }
        }
    }
}

@Composable
private fun TerminalErrorOverlay(
    error: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(2f)
            .padding(horizontal = AppSpacing.screenPaddingHorizontal, vertical = AppSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(AppColors.error.copy(alpha = 0.9f))
                .padding(AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ERROR: $error",
                    color = AppColors.white,
                    style = AppTypography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = AppColors.white)
                }
            }
        }
    }
}
