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
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalShapes
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography
import com.mocca.app.util.FilePickerHelper
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

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
    
    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            if (listState.firstVisibleItemIndex > 1) {
                 listState.animateScrollToItem(0)
            } else {
                 listState.animateScrollToItem(0)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalColors.background)
    ) {
        state.session?.let { session ->
            ChatHeader(
                title = session.title ?: "SESSION_${session.id.take(8)}",
                showTodos = state.showTodoPanel,
                onTodoClick = { screenModel.toggleTodoPanel() },
                onShareClick = { showShareDialog = true },
                onSummarizeClick = { screenModel.summarizeSession() }
            )
        }
        
        if (showShareDialog) {
            val isShared = state.session?.shareID != null
            val shareUrl = if (isShared) "https://opencode.dev/s/${state.session?.shareID}" else ""
            
            AlertDialog(
                onDismissRequest = { showShareDialog = false },
                containerColor = TerminalColors.surfaceElevated,
                shape = TerminalShapes.dialog,
                title = { Text(if (isShared) "SESSION SHARED" else "SHARE SESSION", style = TerminalTypography.labelMedium, color = TerminalColors.white) },
                text = {
                    Column {
                        if (isShared) {
                            Text("This session is publicly accessible.", color = TerminalColors.textSecondary, style = TerminalTypography.bodySmall)
                            Spacer(modifier = Modifier.height(TerminalSpacing.md))
                            TerminalInput(
                                value = shareUrl,
                                onValueChange = {},
                                showPrompt = false,
                                enabled = false 
                            )
                        } else {
                            Text("Make this session publicly accessible?", color = TerminalColors.white, style = TerminalTypography.bodyMedium)
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
                            textColor = TerminalColors.alertRed
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
                        .padding(horizontal = TerminalSpacing.screenPaddingHorizontal, vertical = TerminalSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
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
                        .padding(horizontal = TerminalSpacing.screenPaddingHorizontal),
                    contentPadding = PaddingValues(
                        top = TerminalSpacing.lg,
                        bottom = TerminalSpacing.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
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
                start = TerminalSpacing.screenPaddingHorizontal,
                end = TerminalSpacing.screenPaddingHorizontal,
                bottom = TerminalSpacing.screenPaddingBottom,
                top = TerminalSpacing.md
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
    onSummarizeClick: () -> Unit
) {
    // Modern header with transparent/blurred background
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TerminalSpacing.screenPaddingHorizontal, vertical = TerminalSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title.uppercase(),
            style = TerminalTypography.labelMedium,
            color = TerminalColors.white,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
            TerminalIconButton(
                icon = Icons.Default.Description,
                onClick = onSummarizeClick,
                iconColor = TerminalColors.textSecondary,
                size = 36.dp,
                contentDescription = "Summarize"
            )
            
            TerminalIconButton(
                icon = Icons.Default.Share,
                onClick = onShareClick,
                iconColor = TerminalColors.textSecondary,
                size = 36.dp
            )
            
            TerminalIconButton(
                icon = if (showTodos) Icons.Default.Close else Icons.AutoMirrored.Filled.List,
                onClick = onTodoClick,
                iconColor = if (showTodos) TerminalColors.accentGreen else TerminalColors.textSecondary,
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
            .padding(horizontal = TerminalSpacing.screenPaddingHorizontal, vertical = TerminalSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TerminalShapes.medium)
                .background(TerminalColors.surfaceVariant)
                .padding(TerminalSpacing.sm)
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
                        tint = TerminalColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(TerminalSpacing.sm))
                    Text(
                        text = "VIEWING OLDER VERSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalColors.textSecondary
                    )
                }
                TerminalTextButton(
                    text = "RESUME LATEST",
                    onClick = onResume,
                    textColor = TerminalColors.accentGreen
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
            .padding(horizontal = TerminalSpacing.screenPaddingHorizontal, vertical = TerminalSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TerminalShapes.medium)
                .background(TerminalColors.error.copy(alpha = 0.9f))
                .padding(TerminalSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ERROR: $error",
                    color = TerminalColors.white,
                    style = TerminalTypography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TerminalIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    iconColor = TerminalColors.white
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
            codeText = TerminalColors.accentGreen,
            codeBackground = TerminalColors.surfaceVariant,
            inlineCodeText = TerminalColors.accentGreen,
            inlineCodeBackground = TerminalColors.surfaceVariant,
            linkText = TerminalColors.primary
        ),
        typography = markdownTypography(
            text = style,
            code = MaterialTheme.typography.bodySmall.copy(color = TerminalColors.accentGreen),
            h1 = MaterialTheme.typography.headlineMedium.copy(color = color),
            h2 = MaterialTheme.typography.headlineSmall.copy(color = color),
            h3 = MaterialTheme.typography.titleLarge.copy(color = color),
            h4 = MaterialTheme.typography.titleMedium.copy(color = color),
            h5 = MaterialTheme.typography.titleSmall.copy(color = color),
            h6 = MaterialTheme.typography.labelLarge.copy(color = color)
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
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Rocket,
                contentDescription = null,
                tint = TerminalColors.textTertiary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            Text(
                text = "SESSION READY",
                style = TerminalTypography.headlineSmall,
                color = TerminalColors.textTertiary
            )
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            TerminalButton(
                text = "INITIALIZE PROJECT",
                onClick = onInit
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
        containerColor = TerminalColors.surfaceElevated,
        shape = TerminalShapes.dialog,
        title = { 
            Text(
                text = "INITIALIZE PROJECT", 
                style = TerminalTypography.labelMedium, 
                color = TerminalColors.white
            ) 
        },
        text = {
            Column {
                Text(
                    text = "Select an AI model to analyze the project:",
                    style = TerminalTypography.bodySmall,
                    color = TerminalColors.textSecondary
                )
                Spacer(modifier = Modifier.height(TerminalSpacing.md))
                
                Text("PROVIDER", color = TerminalColors.accentGreen, style = TerminalTypography.labelSmall)
                Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                providerInfo.all.forEach { providerItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedProvider = providerItem.id 
                                selectedModel = (providerItem.models as? JsonObject)?.keys?.firstOrNull() ?: ""
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Use a custom radio-like indicator
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(TerminalShapes.circle)
                                .border(1.dp, if (selectedProvider == providerItem.id) TerminalColors.accentGreen else TerminalColors.grey, TerminalShapes.circle)
                                .background(if (selectedProvider == providerItem.id) TerminalColors.accentGreen else Color.Transparent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = providerItem.name,
                            color = if (selectedProvider == providerItem.id) TerminalColors.white else TerminalColors.textSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(TerminalSpacing.md))
                
                Text("MODEL", color = TerminalColors.accentGreen, style = TerminalTypography.labelSmall)
                Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(currentModelIds) { modelId ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedModel = modelId }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(TerminalShapes.circle)
                                    .border(1.dp, if (selectedModel == modelId) TerminalColors.accentGreen else TerminalColors.grey, TerminalShapes.circle)
                                    .background(if (selectedModel == modelId) TerminalColors.accentGreen else Color.Transparent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = modelId,
                                color = if (selectedModel == modelId) TerminalColors.white else TerminalColors.textSecondary,
                                style = TerminalTypography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TerminalCompactButton(
                text = "START",
                onClick = { onInit(selectedProvider, selectedModel) },
                enabled = selectedProvider.isNotEmpty() && selectedModel.isNotEmpty()
            )
        },
        dismissButton = {
            TerminalTextButton(
                text = "CANCEL",
                onClick = onDismiss
            )
        }
    )
}