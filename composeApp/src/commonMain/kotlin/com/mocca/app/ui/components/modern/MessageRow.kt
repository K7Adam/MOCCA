package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.domain.model.TokenUsage
import com.mocca.app.ui.components.RichToolCard
import com.mocca.app.ui.components.modern.message.ContextToolGroup
import com.mocca.app.ui.components.modern.message.DateSeparator
import com.mocca.app.ui.components.modern.message.ModernFileBlock
import com.mocca.app.ui.components.modern.message.ModernReasoningBlock
import com.mocca.app.ui.components.modern.message.ModernSubTaskBlock
import com.mocca.app.ui.components.modern.message.ModernThinkingBlock
import com.mocca.app.ui.components.modern.message.ModernToolResultBlock
import com.mocca.app.ui.screens.chat.MarkdownText
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

private sealed class PartGroup {
    data class Single(val part: MessagePart) : PartGroup()
    data class ToolGroup(val tools: List<Pair<MessagePart.ToolInvocation, MessagePart.ToolResult?>>) : PartGroup()
}

private fun groupParts(parts: List<MessagePart>): List<PartGroup> {
    val result = mutableListOf<PartGroup>()
    val toolBuffer = mutableListOf<Pair<MessagePart.ToolInvocation, MessagePart.ToolResult?>>()
    val partsList = parts.toList()
    var i = 0
    while (i < partsList.size) {
        val part = partsList[i]
        if (part is MessagePart.ToolInvocation) {
            val nextPart = partsList.getOrNull(i + 1)
            val toolResult = if (nextPart is MessagePart.ToolResult) { i++; nextPart } else null
            toolBuffer.add(Pair(part, toolResult))
        } else {
            if (toolBuffer.isNotEmpty()) {
                result.add(PartGroup.ToolGroup(toolBuffer.toList()))
                toolBuffer.clear()
            }
            result.add(PartGroup.Single(part))
        }
        i++
    }
    if (toolBuffer.isNotEmpty()) {
        result.add(PartGroup.ToolGroup(toolBuffer.toList()))
    }
    return result
}

/**
 * Format token count for display (e.g., 1.2K, 15K)
 */
private fun formatTokenCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${(count / 1_000_000f).let { if (it >= 10) it.toInt().toString() else "%.1f".format(it) }}M"
        count >= 1_000 -> "${(count / 1_000f).let { if (it >= 10) it.toInt().toString() else "%.1f".format(it) }}K"
        else -> count.toString()
    }
}

private fun formatTime(timestamp: Long): String {
    return com.mocca.app.util.TimeFormatter.formatTime(timestamp)
}

/**
 * Modern message row replacing the old bubble layout.
 *
 * - AI messages: full-width, no background/border (flat layout)
 * - User messages: subtle surfaceContainerHigh tint, 85% width, end-aligned
 * - Max nesting depth: 3-4 levels
 */

@Composable
fun MessageRow(
    message: Message,
    modifier: Modifier = Modifier,
    isFirstInGroup: Boolean = true,
    dateHeader: String? = null,
    onFork: () -> Unit = {},
    onRevert: () -> Unit = {},
    showTimestamps: Boolean = true,
    showTokenCounts: Boolean = true,
    onDelete: () -> Unit = {},
    onDeletePart: (String) -> Unit = {},
    onEditPart: (MessagePart) -> Unit = {},
    onFileClick: ((String) -> Unit)? = null
) {
    val isUser = message.role == MessageRole.USER
    val haptic = LocalHapticFeedback.current
    var showContextMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (isFirstInGroup) AppSpacing.sm else 1.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Date separator
        if (dateHeader != null) {
            DateSeparator(dateHeader)
            Spacer(modifier = Modifier.height(AppSpacing.md))
        }

        // Message header (role label + timestamp)
        if (isFirstInGroup) {
            MessageHeader(isUser = isUser, createdAt = message.createdAt, showTimestamps = showTimestamps)
        }

        // Message content
        if (isUser) {
            // User: tinted surface, 85% width, rounded
            UserMessageContent(
                message = message,
                onFileClick = onFileClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showContextMenu = true
                }
            )
        } else {
            // AI: full-width, no background
            AgentMessageContent(
                message = message,
                showTokenCounts = showTokenCounts,
                onFileClick = onFileClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showContextMenu = true
                }
            )
        }

        // Context menu
        MessageContextMenu(
            expanded = showContextMenu,
            message = message,
            onDismiss = { showContextMenu = false },
            onFork = onFork,
            onRevert = onRevert,
            onDelete = onDelete,
            onEditPart = onEditPart
        )
    }
}

@Composable
private fun MessageHeader(isUser: Boolean, createdAt: Long, showTimestamps: Boolean) {
    val icon = if (isUser) Icons.Default.Person else Icons.Default.SmartToy
    val label = if (isUser) "USER" else "AGENT"
    val color = if (isUser) AppColors.textSecondary else AppColors.accentGreen
    val timeText = remember(createdAt) { formatTime(createdAt) }

    Row(
        modifier = Modifier.padding(
            bottom = AppSpacing.xxs,
            start = if (isUser) 0.dp else AppSpacing.sm,
            end = if (isUser) AppSpacing.sm else 0.dp
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
            Spacer(modifier = Modifier.width(AppSpacing.xs))
        }

        Text(text = label, color = color, style = AppTypography.labelExtraSmall, fontWeight = FontWeight.Bold)

        if (showTimestamps) {
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(text = timeText, color = AppColors.textTertiary, style = AppTypography.labelExtraSmall)
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
        }
    }
}

@Composable
private fun UserMessageContent(
    message: Message,
    onFileClick: ((String) -> Unit)?,
    onLongClick: () -> Unit
) {
    // Content Grouping: Sharp top-right for User
    val shape = AppShapes.messageBubbleUser

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(shape)
            .background(AppColors.surfaceContainerHigh, shape)
            .border(AppSpacing.borderThin, AppColors.borderLight.copy(alpha = 0.4f), shape)
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            val partGroups = remember(message.parts) { groupParts(message.parts) }
            partGroups.forEach { group ->
                RenderPartGroup(group = group, onFileClick = onFileClick)
                Spacer(modifier = Modifier.height(AppSpacing.xs))
            }
        }
    }
}

@Composable
private fun AgentMessageContent(
    message: Message,
    showTokenCounts: Boolean,
    onFileClick: ((String) -> Unit)?,
    onLongClick: () -> Unit
) {
    // Content Grouping: Sharp top-left for Agent
    val shape = AppShapes.messageBubbleAgent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.surfaceContainerLow.copy(alpha = 0.5f), shape)
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
    ) {
        Column {
            val partGroups = remember(message.parts) { groupParts(message.parts) }
            partGroups.forEach { group ->
                RenderPartGroup(group = group, onFileClick = onFileClick)
                Spacer(modifier = Modifier.height(AppSpacing.xs))
            }

            // Token count footer
            if (showTokenCounts && message.tokens != null) {
                TokenCountFooter(tokens = message.tokens)
            }
        }
    }
}

@Composable
private fun RenderPartGroup(group: PartGroup, onFileClick: ((String) -> Unit)?) {
    when (group) {
        is PartGroup.Single -> when (val part = group.part) {
            is MessagePart.Text -> MarkdownText(
                markdown = part.text,
                style = AppTypography.bodyMedium,
                color = AppColors.textPrimary,
                onFileClick = onFileClick
            )
            is MessagePart.Reasoning -> ModernReasoningBlock(part)
            is MessagePart.ToolInvocation -> RichToolCard(part)
            is MessagePart.ToolResult -> ModernToolResultBlock(part)
            is MessagePart.File -> ModernFileBlock(part)
            is MessagePart.SubTask -> ModernSubTaskBlock(part)
            is MessagePart.Thinking -> ModernThinkingBlock(part)
        }
        is PartGroup.ToolGroup -> ContextToolGroup(tools = group.tools)
    }
}

@Composable
private fun TokenCountFooter(tokens: TokenUsage) {
    if (tokens.input <= 0 && tokens.output <= 0) return

    Spacer(modifier = Modifier.height(AppSpacing.xs))
    HorizontalDivider(color = AppColors.border.copy(alpha = 0.3f), thickness = 0.5.dp)
    Spacer(modifier = Modifier.height(AppSpacing.xs))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tokens.input > 0) {
            Text(
                text = "IN: ${formatTokenCount(tokens.input)}",
                color = AppColors.textTertiary,
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
        }
        if (tokens.output > 0) {
            Text(
                text = "OUT: ${formatTokenCount(tokens.output)}",
                color = AppColors.accentGreen.copy(alpha = 0.7f),
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Medium
            )
        }
        if (tokens.reasoning > 0) {
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "REASON: ${formatTokenCount(tokens.reasoning)}",
                color = AppColors.accentGreen.copy(alpha = 0.5f),
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MessageContextMenu(
    expanded: Boolean,
    message: Message,
    onDismiss: () -> Unit,
    onFork: () -> Unit,
    onRevert: () -> Unit,
    onDelete: () -> Unit,
    onEditPart: (MessagePart) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(AppColors.surfaceElevated, AppShapes.medium)
    ) {
        DropdownMenuItem(
            text = { Text("FORK FROM HERE", style = AppTypography.labelSmall, color = AppColors.textSecondary) },
            onClick = { onFork(); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("REVERT TO HERE", style = AppTypography.labelSmall, color = AppColors.textSecondary) },
            onClick = { onRevert(); onDismiss() }
        )
        // EDIT PART — only shown for messages with editable (Text) parts
        val textParts = message.parts.filterIsInstance<MessagePart.Text>()
        if (textParts.isNotEmpty()) {
            HorizontalDivider(color = AppColors.border.copy(alpha = 0.3f))
            textParts.forEachIndexed { index, textPart ->
                val label = if (textParts.size > 1) "EDIT PART ${index + 1}" else "EDIT PART"
                DropdownMenuItem(
                    text = { Text(label, style = AppTypography.labelSmall, color = AppColors.textSecondary) },
                    onClick = { onEditPart(textPart); onDismiss() }
                )
            }
        }
        HorizontalDivider(color = AppColors.border.copy(alpha = 0.3f))
        DropdownMenuItem(
            text = { Text("DELETE MESSAGE", style = AppTypography.labelSmall, color = AppColors.error) },
            onClick = { onDelete(); onDismiss() }
        )
    }
}
