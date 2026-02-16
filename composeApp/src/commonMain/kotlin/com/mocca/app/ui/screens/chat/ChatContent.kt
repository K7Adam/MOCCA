package com.mocca.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.ui.components.ErrorScreen
import com.mocca.app.ui.components.GodHeader
import com.mocca.app.ui.components.PermissionRequestDialog
import com.mocca.app.ui.components.QuestionDialog
import com.mocca.app.ui.components.chat.TodoListPanel
import com.mocca.app.ui.components.terminal.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.util.FilePickerHelper
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(screenModel: ChatScreenModel) {
    val state by screenModel.state.collectAsState()
    val inputText by screenModel.inputText.collectAsState()
    val streamingText by screenModel.streamingText.collectAsState()
    val messages by screenModel.aggregatedMessages.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    
    val commands = state.commands
    var showShareDialog by remember { mutableStateOf(false) }
    
    val filePickerLauncher = rememberFilePickerLauncher(
        type = FilePickerHelper.createFileType(),
        mode = FileKitMode.Multiple()
    ) { files ->
        files?.forEach { file ->
            coroutineScope.launch {
                try {
                    val attached = FilePickerHelper.toAttachedFile(file)
                    screenModel.addAttachment(attached)
                } catch (e: Exception) {
                    io.github.aakira.napier.Napier.e("Failed to attach file", e)
                }
            }
        }
    }
    
    LaunchedEffect(streamingText) {
        if (streamingText.isNotEmpty()) {
            // Throttled haptic for streaming
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Pagination Trigger (Reverse layout: Top is end of list)
    val isAtTop by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 5
        }
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    var hasNewMessagesWhileScrolledUp by remember { mutableStateOf(false) }

    // Reset new message indicator when at bottom
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex <= 2) {
            hasNewMessagesWhileScrolledUp = false
        }
    }

    // Set indicator when new messages arrive while scrolled up
    LaunchedEffect(messages.size) {
        if (showScrollToBottom) {
            hasNewMessagesWhileScrolledUp = true
        }
    }

    LaunchedEffect(isAtTop) {
        if (isAtTop) screenModel.loadMoreMessages()
    }
    
    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            // Only auto-scroll if user is already at the bottom
            if (listState.firstVisibleItemIndex <= 1) {
                 listState.animateScrollToItem(0)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        state.session?.let { session ->
            GodHeader(
                title = session.title ?: "SESSION_${session.id.take(8)}",
                subtitle = "mobile-agent-v2",
                subtitleIcon = {
                    Icon(
                        imageVector = Icons.Default.Rocket,
                        contentDescription = null,
                        tint = AppColors.white.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { screenModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (state.isLoading) AppColors.accentGreen else AppColors.white
                        )
                    }
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = AppColors.white)
                    }
                    IconButton(onClick = { screenModel.toggleTodoPanel() }) {
                        Icon(
                            imageVector = if (state.showTodoPanel) Icons.Default.Close else Icons.AutoMirrored.Filled.List,
                            contentDescription = "Todos",
                            tint = if (state.showTodoPanel) AppColors.accentGreen else AppColors.white
                        )
                    }
                }
            )
        }
        
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
                            TerminalInput(
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
                        TerminalCompactButton(
                            text = "COPY LINK",
                            onClick = { 
                                clipboardManager.setText(AnnotatedString(shareUrl))
                                showShareDialog = false
                            }
                        )
                    } else {
                        TerminalCompactButton(
                            text = "SHARE PUBLICLY",
                            onClick = { 
                                screenModel.shareSession()
                            }
                        )
                    }
                },
                dismissButton = {
                    if (isShared) {
                        TerminalTextButton(
                            text = "UNSHARE",
                            onClick = { 
                                screenModel.unshareSession()
                                showShareDialog = false
                            },
                            textColor = AppColors.alertRed
                        )
                    } else {
                        TerminalTextButton(
                            text = "CANCEL",
                            onClick = { showShareDialog = false }
                        )
                    }
                }
            )
        }
        
        TodoListPanel(
            todos = state.todos,
            isVisible = state.showTodoPanel
        )

        // Main content area: messages + input as overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
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
            
            if (state.isLoading && state.messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppSpacing.screenPaddingHorizontal, vertical = AppSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    repeat(3) { MessageSkeleton() }
                }
            } else if (state.error != null && state.messages.isEmpty()) {
                ErrorScreen(message = state.error ?: "UNKNOWN_ERROR") {
                    screenModel.retry()
                }
            } else if (state.messages.isEmpty()) {
                EmptySessionState()
            } else {
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { screenModel.refreshData() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AppSpacing.screenPaddingHorizontal),
                        contentPadding = PaddingValues(
                            top = AppSpacing.lg,
                            bottom = 140.dp // Space for input overlay
                        ),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        // Extra spacer to push content above the input field
                        item { Spacer(modifier = Modifier.height(160.dp)) }

                        if (state.isSending && streamingText.isEmpty() && !state.isThinking) {
                            item(contentType = "processing") { TerminalProcessingIndicator() }
                        }
                        
                        if (state.isThinking) {
                            item(contentType = "thinking") {
                                TerminalThinkingIndicator(
                                    thinkingContent = state.thinkingContent,
                                    elapsedMs = state.thinkingElapsedMs
                                )
                            }
                        }
                        
                        if (streamingText.isNotEmpty()) {
                            item(contentType = "streaming") { TerminalStreamingMessage(text = streamingText) }
                        }
                        
                        val displayMessages = messages.filter { msg ->
                            msg.role == MessageRole.USER || msg.parts.isNotEmpty()
                        }
                        
                        items(
                            items = displayMessages.asReversed(),
                            key = { it.id },
                            contentType = { "message" }
                        ) { message ->
                            val index = displayMessages.indexOf(message)
                            val nextMessage = if (index < displayMessages.size - 1) displayMessages[index + 1] else null
                            val prevMessage = if (index > 0) displayMessages[index - 1] else null
                            
                            // Grouping logic: Same sender, within 5 minutes
                            val isFirstInGroup = prevMessage == null || 
                                prevMessage.role != message.role || 
                                (message.createdAt - prevMessage.createdAt) > 300_000 // 5 mins
                            
                            // Date header logic
                            val showDateHeader = if (nextMessage == null) {
                                com.mocca.app.util.TimeFormatter.formatDate(message.createdAt)
                            } else {
                                val nextDate = com.mocca.app.util.TimeFormatter.formatDate(nextMessage.createdAt)
                                val currentDate = com.mocca.app.util.TimeFormatter.formatDate(message.createdAt)
                                if (nextDate != currentDate) currentDate else null
                            }

                            TerminalMessage(
                                message = message,
                                isFirstInGroup = isFirstInGroup,
                                dateHeader = showDateHeader,
                                onFork = { screenModel.forkSession(message) },
                                onRevert = { screenModel.revertSession(message) }
                            )
                        }
                    }
                }
            }
            
            state.pendingPermission?.let { permission ->
                PermissionRequestDialog(
                    permission = permission,
                    onApprove = { screenModel.approvePermission() },
                    onDeny = { screenModel.denyPermission() }
                )
            }
            
            state.pendingQuestion?.let { question ->
                QuestionDialog(
                    request = question,
                    onAnswer = { answers -> screenModel.answerQuestion(answers) },
                    onReject = { screenModel.rejectQuestion() }
                )
            }

            // Scroll to bottom FAB
            ScrollToBottomButton(
                isVisible = showScrollToBottom,
                hasNewMessages = hasNewMessagesWhileScrolledUp,
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = AppSpacing.lg, bottom = 140.dp) // Adjusted to be above RichChatInput
            )
            
            // Chat input pinned to bottom, overlaying messages
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding() // Handles keyboard padding
                    .windowInsetsPadding(WindowInsets.navigationBars) // Ensures input is above nav bar when keyboard closed
            ) {
                RichChatInput(
                    value = inputText,
                    onValueChange = { screenModel.updateInputText(it) },
                    onSendClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        screenModel.sendMessage() 
                    },
                    enabled = state.connectionStatus is ConnectionStatus.Connected && state.isSessionIdle,
                    modelName = state.modelName,
                    agentName = state.agentName,
                    providerResponse = state.providerInfo,
                    selectedProviderId = state.selectedProviderId,
                    selectedModelId = state.selectedModelId,
                    onModelSelected = { providerId, modelId -> screenModel.selectModel(providerId, modelId) },
                    recentModels = state.recentModels,
                    modes = state.modes,
                    selectedModeId = state.selectedModeId,
                    onModeSelected = { screenModel.selectMode(it) },
                    attachedFiles = state.attachedFiles,
                    onRemoveAttachment = { screenModel.removeAttachment(it) },
                    onAttachClick = { filePickerLauncher.launch() },
                    commands = commands,
                    onCommandSelected = { cmd -> coroutineScope.launch { cmd.action() } },
                    onModeSelectedForMention = { mode -> screenModel.selectMode(mode.id) }
                )
            }
        }
    }
}

@Composable
private fun ChatHeader(
    title: String,
    showTodos: Boolean,
    onTodoClick: () -> Unit,
    onShareClick: () -> Unit,
    onSummarizeClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isRefreshing: Boolean
) {
    // Modern header with transparent/blurred background
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPaddingHorizontal, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title.uppercase(),
            style = AppTypography.labelMedium,
            color = AppColors.white,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            // Add Refresh Button
            TerminalIconButton(
                icon = Icons.Default.Refresh,
                onClick = onRefreshClick,
                iconColor = if (isRefreshing) AppColors.accentGreen else AppColors.textSecondary,
                size = 36.dp,
                contentDescription = "Refresh"
            )

            TerminalIconButton(
                icon = Icons.Default.Description,
                onClick = onSummarizeClick,
                iconColor = AppColors.textSecondary,
                size = 36.dp,
                contentDescription = "Summarize"
            )
            
            TerminalIconButton(
                icon = Icons.Default.Share,
                onClick = onShareClick,
                iconColor = AppColors.textSecondary,
                size = 36.dp
            )
            
            TerminalIconButton(
                icon = if (showTodos) Icons.Default.Close else Icons.AutoMirrored.Filled.List,
                onClick = onTodoClick,
                iconColor = if (showTodos) AppColors.accentGreen else AppColors.textSecondary,
                size = 36.dp
            )
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
                TerminalTextButton(
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
                TerminalIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    iconColor = AppColors.white
                )
            }
        }
    }
}

@Composable
fun MarkdownText(
    markdown: String,
    style: TextStyle,
    color: Color
) {
    Markdown(
        content = markdown,
        colors = markdownColor(
            text = color,
            codeText = AppColors.accentGreen,
            codeBackground = AppColors.surfaceVariant,
            inlineCodeText = AppColors.accentGreen,
            inlineCodeBackground = AppColors.surfaceVariant,
            linkText = AppColors.primary
        ),
        typography = markdownTypography(
            text = style,
            code = AppTypography.bodySmall.copy(color = AppColors.accentGreen),
            h1 = AppTypography.headlineMedium.copy(color = color),
            h2 = AppTypography.headlineSmall.copy(color = color),
            h3 = AppTypography.titleLarge.copy(color = color),
            h4 = AppTypography.titleMedium.copy(color = color),
            h5 = AppTypography.titleSmall.copy(color = color),
            h6 = AppTypography.labelLarge.copy(color = color)
        )
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
            TerminalBootSequence()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "START A CONVERSATION",
                style = AppTypography.labelMedium,
                color = AppColors.white.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Type below to begin. Use the model selector in the input bar to choose your AI provider.",
                style = AppTypography.bodySmall,
                color = AppColors.white.copy(alpha = 0.3f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun TerminalBootSequence() {
    val lines = listOf(
        "MOCCA_OS [Version 2.4.0]",
        "(c) 2026 OH_MY_OPENCODE. All rights reserved.",
        "",
        "> LOADING_CORES...",
        "> UPLINK_READY",
        "> STANDBY_MODE_ACTIVE"
    )
    
    Column(horizontalAlignment = Alignment.Start) {
        lines.forEachIndexed { index, line ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index * 200L)
                visible = true
            }
            if (visible) {
                Text(
                    text = line,
                    style = AppTypography.codeSmall,
                    color = if (line.startsWith(">")) AppColors.accentGreen else AppColors.textSecondary,
                    fontFamily = AppTypography.monoFamily
                )
            }
        }
    }
}


