package com.mocca.app.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalTypography
import com.mikepenz.markdown.m3.Markdown
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.domain.model.ToolState
import org.jetbrains.compose.resources.stringResource
import com.mocca.app.ui.components.RichToolCard
import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    onFork: ((Message) -> Unit)? = null,
    onRevert: ((Message) -> Unit)? = null
) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    
    // Terminal style: User bubbles use surfaceVariant, Assistant is plain text/markdown
    val containerColor = if (isUser) TerminalColors.surfaceVariant else Color.Transparent
    val contentColor = if (isUser) TerminalColors.white else TerminalColors.white
    
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        // Name label for assistant/system
        if (!isUser) {
            Text(
                text = message.role.name.lowercase().replaceFirstChar { it.uppercase() },
                style = TerminalTypography.labelSmall,
                color = TerminalColors.grey,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = RectangleShape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .widthIn(max = 600.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
        ) {
            Box {
                Column(modifier = Modifier.padding(if (isUser) 12.dp else 0.dp)) {
                    message.parts.forEach { part ->
                        when (part) {
                            is MessagePart.Text -> {
                                if (isUser) {
                                    Text(part.text)
                                } else {
                                    Markdown(content = part.text)
                                }
                            }
                            is MessagePart.Reasoning -> {
                                ReasoningBlock(part)
                            }
                            is MessagePart.ToolInvocation -> {
                                RichToolCard(part)
                            }
                            is MessagePart.ToolResult -> {
                                ToolResultBlock(part)
                            }
                            is MessagePart.File -> {
                                FileAttachmentBlock(part)
                            }
                            is MessagePart.SubTask -> {
                                // Subtasks are handled separately or embedded
                            }
                            is MessagePart.Thinking -> {
                                // Thinking content shown via indicator during streaming
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onFork != null) {
                        DropdownMenuItem(
                            text = { Text("Fork Session") },
                            leadingIcon = { Icon(@Suppress("DEPRECATION") Icons.Default.CallSplit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onFork(message)
                            }
                        )
                    }
                    if (onRevert != null) {
                        DropdownMenuItem(
                            text = { Text("Revert to Here") },
                            leadingIcon = { Icon(@Suppress("DEPRECATION") Icons.Default.Undo, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRevert(message)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReasoningBlock(part: MessagePart.Reasoning) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, TerminalColors.border, RectangleShape)
            .clickable { expanded = !expanded },
        shape = RectangleShape,
        color = TerminalColors.surface
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info, // Lightbulb icon ideal here
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TerminalColors.statusOnline
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Thinking Process (${part.timeMs}ms)",
                    style = TerminalTypography.labelSmall,
                    color = TerminalColors.statusOnline
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TerminalColors.grey
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = TerminalColors.border)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = part.content,
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.grey
                    )
                }
            }
        }
    }
}


// ToolInvocationBlock removed, replaced by RichToolCard


@Composable
fun ToolResultBlock(part: MessagePart.ToolResult) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        color = TerminalColors.surfaceVariant.copy(alpha = 0.5f),
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tool Output",
                    style = TerminalTypography.labelSmall,
                    color = TerminalColors.grey
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TerminalColors.grey
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = part.result,
                    style = TerminalTypography.bodySmall,
                    color = TerminalColors.white
                )
            }
        }
    }
}

@Composable
fun FileAttachmentBlock(part: MessagePart.File) {
    AssistChip(
        onClick = { /* Open file */ },
        label = { Text(part.filename ?: "Attachment") },
        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
    )
}
