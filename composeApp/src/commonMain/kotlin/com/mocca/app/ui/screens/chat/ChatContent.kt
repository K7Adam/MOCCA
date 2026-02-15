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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.ui.components.ErrorScreen
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
import kotlinx.serialization.json.JsonObject

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
    
    val commands = state.commands
    var showShareDialog by remember { mutableStateOf(false) }
    var showInitDialog by remember { mutableStateOf(false) }
    
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
    
    // Pagination Trigger (Reverse layout: Top is end of list)
    val isAtTop by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 5
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
                EmptySessionState(
                    onInit = { showInitDialog = true }
                )
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppSpacing.screenPaddingHorizontal),
                    contentPadding = PaddingValues(
                        top = AppSpacing.lg,
                        bottom = AppSpacing.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
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
                        TerminalMessage(
                            message = message,
                            onFork = { screenModel.forkSession(message) },
                            onRevert = { screenModel.revertSession(message) }
                        )
                    }
                }
            }
            
            if (showInitDialog && state.providerInfo != null) {
                InitSessionDialog(
                    providerInfo = state.providerInfo!!,
                    onInit = { providerId, modelId -> 
                        screenModel.initSession(providerId, modelId)
                        showInitDialog = false
                    },
                    onDismiss = { showInitDialog = false }
                )
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
        }
        
        // Input area with padding
        Box(
            modifier = Modifier.padding(
                start = AppSpacing.screenPaddingHorizontal,
                end = AppSpacing.screenPaddingHorizontal,
                bottom = AppSpacing.screenPaddingBottom,
                top = AppSpacing.md
            )
        ) {
            RichChatInput(
                value = inputText,
                onValueChange = { screenModel.updateInputText(it) },
                onSendClick = { screenModel.sendMessage() },
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
private fun EmptySessionState(onInit: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(AppColors.surfaceElevated, AppShapes.circle)
                    .border(1.dp, AppColors.white.copy(alpha = 0.05f), AppShapes.circle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = AppColors.accentGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "READY TO INITIALIZE",
                style = AppTypography.headlineSmall,
                color = AppColors.white,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Select a model to begin the project analysis and generation session.",
                style = AppTypography.bodyMedium,
                color = AppColors.white.copy(alpha = 0.4f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            GodButton(
                text = "INITIALIZE",
                onClick = onInit,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InitSessionDialog(
    providerInfo: com.mocca.app.domain.model.ProviderResponse,
    onInit: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultProvider = providerInfo.all.firstOrNull()?.id ?: ""
    val defaultModel = (providerInfo.all.firstOrNull()?.models as? JsonObject)?.keys?.firstOrNull() ?: ""
    
    var selectedProvider by remember { mutableStateOf(defaultProvider) }
    var selectedModel by remember { mutableStateOf(defaultModel) }
    
    val provider = providerInfo.all.find { it.id == selectedProvider }
    val currentModelIds = (provider?.models as? JsonObject)?.keys?.toList() ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111111),
        shape = RoundedCornerShape(32.dp),
        title = { 
            Text(
                text = "INITIALIZE", 
                style = AppTypography.titleLarge, 
                color = AppColors.white,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column {
                Text(
                    text = "PROVIDER",
                    style = AppTypography.labelSmall,
                    color = AppColors.white.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                providerInfo.all.forEach { providerItem ->
                    GodListItem(
                        title = providerItem.name,
                        subtitle = providerItem.id,
                        icon = {
                            Icon(
                                imageVector = if (selectedProvider == providerItem.id) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (selectedProvider == providerItem.id) AppColors.accentGreen else AppColors.white.copy(alpha = 0.2f)
                            )
                        },
                        onClick = { 
                            selectedProvider = providerItem.id 
                            selectedModel = (providerItem.models as? JsonObject)?.keys?.firstOrNull() ?: ""
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "MODEL",
                    style = AppTypography.labelSmall,
                    color = AppColors.white.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.height(200.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(currentModelIds) { modelId ->
                            GodListItem(
                                title = modelId,
                                subtitle = "AI Model",
                                icon = {
                                    Icon(
                                        imageVector = if (selectedModel == modelId) Icons.Default.CheckCircle else Icons.Default.Circle,
                                        contentDescription = null,
                                        tint = if (selectedModel == modelId) AppColors.accentGreen else AppColors.white.copy(alpha = 0.1f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = { selectedModel = modelId }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            GodButton(
                text = "START SESSION",
                onClick = { onInit(selectedProvider, selectedModel) },
                enabled = selectedProvider.isNotEmpty() && selectedModel.isNotEmpty()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = AppColors.white.copy(alpha = 0.4f), style = AppTypography.labelMedium)
            }
        }
    )
}
