package com.mocca.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.ui.components.ErrorScreen
import com.mocca.app.ui.components.LoadingScreen
import com.mocca.app.ui.components.PermissionRequestDialog
import com.mocca.app.ui.components.QuestionDialog
import com.mocca.app.ui.components.terminal.RichChatInput
import com.mocca.app.ui.components.terminal.TerminalIconButton
import com.mocca.app.ui.components.terminal.TerminalMessage
import com.mocca.app.ui.components.terminal.TerminalStreamingMessage
import com.mocca.app.ui.components.terminal.TerminalTextButton
import com.mocca.app.ui.components.terminal.TerminalProcessingIndicator
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mocca.app.util.FilePickerHelper
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.FileKitMode
import kotlinx.coroutines.launch
import com.mocca.app.ui.components.terminal.SuggestionType

@Composable
fun ChatContent(screenModel: ChatScreenModel) {
    val state by screenModel.state.collectAsState()
    val inputText by screenModel.inputText.collectAsState() // PERFORMANCE FIX: Separate input state
    val streamingText by screenModel.streamingText.collectAsState() // PERFORMANCE FIX: Separate streaming text
    val messages by screenModel.aggregatedMessages.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Command definitions
    // Command definitions
    // Command definitions
    // Only use commands fetched from API
    val commands = state.commands
    
    // File picker launcher using FileKit
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
                    // Log error but don't crash
                    io.github.aakira.napier.Napier.e("Failed to attach file", e)
                }
            }
        }
    }
    
    // Auto-scroll (reverseLayout: 0 is bottom)
    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            // With reverseLayout, 0 is the bottom. 
            // We only animate if not already at 0, or just ensure visibility.
            if (listState.firstVisibleItemIndex > 1) {
                 // If far away, maybe just show badge? For now force scroll.
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
        // Chat Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Reverted session banner
            if (state.session?.isReverted == true) {
                RevertedSessionBanner(
                    onResume = { screenModel.unrevertSession() }
                )
            }
            
            // Error overlay
            state.error?.let { error ->
                TerminalErrorOverlay(
                    error = error,
                    onDismiss = { screenModel.clearError() }
                )
            }
            
            if (state.isLoading && state.messages.isEmpty()) {
                LoadingScreen()
            } else if (state.error != null && state.messages.isEmpty()) {
                ErrorScreen(message = state.error ?: "UNKNOWN_ERROR") {
                    screenModel.retry()
                }
            } else {
                // Messages list (Reversed for Chat)
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = TerminalSpacing.lg),
                    contentPadding = PaddingValues(
                        top = TerminalSpacing.lg,
                        bottom = TerminalSpacing.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
                ) {
                    // Bottom items first in reverseLayout
                    
                    // Processing indicator
                    if (state.isSending && streamingText.isEmpty()) {
                        item(contentType = "processing") {
                            TerminalProcessingIndicator()
                        }
                    }
                    
                    // Streaming text
                    if (streamingText.isNotEmpty()) {
                        item(contentType = "streaming") {
                            TerminalStreamingMessage(text = streamingText)
                        }
                    }
                    
                    // Messages (Newest first at bottom)
                    // Filter out empty assistant messages to prevent duplicate blocks during streaming
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
                } // LazyColumn ends here
            }
            
            // Dialogs
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
        
        // Chat Input
        RichChatInput(
            value = inputText,
            onValueChange = { screenModel.updateInputText(it) },
            onSendClick = { screenModel.sendMessage() },
            enabled = state.connectionStatus is ConnectionStatus.Connected && state.isSessionIdle,
            modelName = state.modelName,
            agentName = state.agentName,
            // Model selection
            providerResponse = state.providerInfo,
            selectedProviderId = state.selectedProviderId,
            selectedModelId = state.selectedModelId,
            onModelSelected = { providerId, modelId -> 
                screenModel.selectModel(providerId, modelId) 
            },
            recentModels = state.recentModels,
            // Mode selection
            // Mode selection
            modes = state.modes,
            selectedModeId = state.selectedModeId,
            onModeSelected = { screenModel.selectMode(it) },
            // Attachments
            attachedFiles = state.attachedFiles,
            onRemoveAttachment = { screenModel.removeAttachment(it) },
            onAttachClick = { filePickerLauncher.launch() },
            // Command/Mention
            commands = commands,
            onCommandSelected = { cmd -> 
                coroutineScope.launch { cmd.action() } 
            },
            onModeSelectedForMention = { mode -> 
                screenModel.selectMode(mode.id) 
            }
        )
    }
}

@Composable
private fun RevertedSessionBanner(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
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
                    tint = TerminalColors.grey,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(TerminalSpacing.sm))
                Text(
                    text = "VIEWING_OLDER_VERSION // NEW_MESSAGES_WILL_FORK",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalColors.grey
                )
            }
            TerminalTextButton(
                text = "RESUME_LATEST",
                onClick = onResume,
                textColor = TerminalColors.statusOnline
            )
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
            .background(TerminalColors.error.copy(alpha = 0.9f))
            .padding(TerminalSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ERROR: $error".uppercase(),
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

@Composable
fun MarkdownText(
    markdown: String,
    style: TextStyle,
    color: Color
) {
    // Use multiplatform-markdown-renderer for proper markdown rendering
    Markdown(
        content = markdown,
        colors = markdownColor(
            text = color,
            codeText = TerminalColors.statusOnline,
            codeBackground = TerminalColors.surface,
            inlineCodeText = TerminalColors.statusOnline,
            inlineCodeBackground = TerminalColors.surface,
            linkText = TerminalColors.statusOnline
        ),
        typography = markdownTypography(
            text = style,
            code = MaterialTheme.typography.bodySmall.copy(color = TerminalColors.statusOnline),
            h1 = MaterialTheme.typography.headlineMedium.copy(color = color),
            h2 = MaterialTheme.typography.headlineSmall.copy(color = color),
            h3 = MaterialTheme.typography.titleLarge.copy(color = color),
            h4 = MaterialTheme.typography.titleMedium.copy(color = color),
            h5 = MaterialTheme.typography.titleSmall.copy(color = color),
            h6 = MaterialTheme.typography.labelLarge.copy(color = color)
        )
    )
}
