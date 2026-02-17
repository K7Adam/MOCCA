package com.mocca.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.ui.components.ErrorScreen
import com.mocca.app.ui.components.PermissionRequestDialog
import com.mocca.app.ui.components.QuestionDialog
import com.mocca.app.ui.components.chat.TodoListPanel
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.*
import com.mocca.app.util.FilePickerHelper
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(screenModel: ChatScreenModel) {
    val state by screenModel.state.collectAsState()
    val inputText by screenModel.inputText.collectAsState()
    val streamingText by screenModel.streamingText.collectAsState()
    val aggregatedMessages by screenModel.aggregatedMessages.collectAsState()
    
    val sessionTitle by remember(state.session) {
        derivedStateOf {
            state.session?.let { it.title?.uppercase() ?: "SESSION_${it.id.take(8)}" } ?: "NEW_SESSION"
        }
    }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
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
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val isAtTop by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 5
        }
    }

    val showScrollToBottom by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    var hasNewMessagesWhileScrolledUp by remember { mutableStateOf(false) }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex <= 2) {
            hasNewMessagesWhileScrolledUp = false
        }
    }

    LaunchedEffect(aggregatedMessages.size) {
        if (showScrollToBottom) {
            hasNewMessagesWhileScrolledUp = true
        }
    }

    LaunchedEffect(isAtTop) {
        if (isAtTop) screenModel.loadMoreMessages()
    }
    
    LaunchedEffect(aggregatedMessages.size, streamingText) {
        if (aggregatedMessages.isNotEmpty() || streamingText.isNotEmpty()) {
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
        
        TodoListPanel(
            todos = state.todos,
            isVisible = state.showTodoPanel
        )

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
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { screenModel.refreshData() },
                    modifier = Modifier.fillMaxSize()
                ) {
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
                        item { Spacer(modifier = Modifier.height(32.dp)) }

                        if (state.isSending && streamingText.isEmpty() && !state.isThinking) {
                            item(contentType = "processing") { ModernProcessingIndicator() }
                        }
                        
                        if (state.isThinking) {
                            item(contentType = "thinking") {
                                ModernThinkingIndicator(
                                    thinkingContent = state.thinkingContent,
                                    elapsedMs = state.thinkingElapsedMs
                                )
                            }
                        }
                        
                        if (streamingText.isNotEmpty()) {
                            item(contentType = "streaming") { ModernStreamingMessage(text = streamingText) }
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

            ScrollToBottomButton(
                isVisible = showScrollToBottom,
                hasNewMessages = hasNewMessagesWhileScrolledUp,
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = AppSpacing.md, bottom = 160.dp) // Space for unified bottom bar
                    .zIndex(10f)
            )
            
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
