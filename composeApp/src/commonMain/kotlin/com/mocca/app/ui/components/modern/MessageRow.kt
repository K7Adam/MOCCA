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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.mocca.app.domain.model.MessagePartGroup
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.domain.model.TokenUsage
import com.mocca.app.domain.model.groupForUi
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
import com.mocca.app.util.FormatUtils

private fun formatTime(timestamp: Long): String {
    return com.mocca.app.util.TimeFormatter.formatTime(timestamp)
}

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
        if (dateHeader != null) {
            DateSeparator(dateHeader)
            Spacer(modifier = Modifier.height(AppSpacing.md))
        }

        if (isFirstInGroup) {
            MessageHeader(isUser = isUser, createdAt = message.createdAt, showTimestamps = showTimestamps)
        }

        if (isUser) {
            UserMessageContent(
                message = message,
                onFileClick = onFileClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showContextMenu = true
                }
            )
        } else {
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
    val color = if (isUser) AppColors.onSurfaceVariant else AppColors.primary
    val iconDescription = if (isUser) "User sender" else "Agent sender"
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
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
        }

        Text(text = label, color = color, style = AppTypography.labelExtraSmall, fontWeight = FontWeight.Bold)

        if (showTimestamps) {
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(text = timeText, color = AppColors.outline, style = AppTypography.labelExtraSmall)
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
private fun UserMessageContent(
    message: Message,
    onFileClick: ((String) -> Unit)?,
    onLongClick: () -> Unit
) {
    val shape = AppShapes.large

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(shape)
            .background(AppColors.surfaceContainerHigh, shape)
            .border(AppSpacing.borderThin, AppColors.outlineVariant.copy(alpha = 0.4f), shape)
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            val partGroups = remember(message.parts) { message.parts.groupForUi() }
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
    val shape = AppShapes.large

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.surfaceContainerLow.copy(alpha = 0.5f), shape)
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
    ) {
        Column {
            val partGroups = remember(message.parts) { message.parts.groupForUi() }
            partGroups.forEach { group ->
                RenderPartGroup(group = group, onFileClick = onFileClick)
                Spacer(modifier = Modifier.height(AppSpacing.xs))
            }

            if (showTokenCounts && message.tokens != null) {
                TokenCountFooter(tokens = message.tokens)
            }
        }
    }
}

@Composable
private fun RenderPartGroup(group: MessagePartGroup, onFileClick: ((String) -> Unit)?) {
    when (group) {
        is MessagePartGroup.Single -> when (val part = group.part) {
            is MessagePart.Text -> MarkdownText(
                markdown = part.text,
                style = AppTypography.bodyMedium,
                color = AppColors.onSurface,
                onFileClick = onFileClick
            )

            is MessagePart.Reasoning -> ModernReasoningBlock(part)
            is MessagePart.ToolInvocation -> RichToolCard(
                part,
                null
            ) // In ContextToolGroup, they are paired. Standalone calls get null result.
            is MessagePart.ToolResult -> ModernToolResultBlock(part)
            is MessagePart.File -> ModernFileBlock(part)
            is MessagePart.SubTask -> ModernSubTaskBlock(part)
            is MessagePart.Thinking -> ModernThinkingBlock(part)
        }

        is MessagePartGroup.ToolGroup -> ContextToolGroup(tools = group.tools)
    }
}

@Composable
private fun TokenCountFooter(tokens: TokenUsage) {
    if (tokens.input <= 0 && tokens.output <= 0) return

    Spacer(modifier = Modifier.height(AppSpacing.xs))
    HorizontalDivider(color = AppColors.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
    Spacer(modifier = Modifier.height(AppSpacing.xs))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tokens.input > 0) {
            Text(
                text = "IN: ${FormatUtils.formatCompactNumber(tokens.input)}",
                color = AppColors.outline,
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
        }
        if (tokens.output > 0) {
            Text(
                text = "OUT: ${FormatUtils.formatCompactNumber(tokens.output)}",
                color = AppColors.primary.copy(alpha = 0.7f),
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Medium
            )
        }
        if (tokens.reasoning > 0) {
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "REASON: ${FormatUtils.formatCompactNumber(tokens.reasoning)}",
                color = AppColors.primary.copy(alpha = 0.5f),
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
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
    if (!expanded) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.HorizontalFloatingToolbar(
            expanded = expanded,
            modifier = Modifier.clip(AppShapes.pill),
            colors = androidx.compose.material3.FloatingToolbarDefaults.standardFloatingToolbarColors(),
            shape = AppShapes.pill,
            content = {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoccaIconButton(
                        icon = Icons.Default.ContentCopy,
                        onClick = { /* Copy action */ onDismiss() },
                        iconColor = AppColors.onSurfaceVariant,
                        contentDescription = "Copy message",
                        size = 32.dp
                    )

                    MoccaIconButton(
                        icon = Icons.Default.Refresh,
                        onClick = { onFork(); onDismiss() },
                        iconColor = AppColors.onSurfaceVariant,
                        contentDescription = "Fork from message",
                        size = 32.dp
                    )

                    val textParts = message.parts.filterIsInstance<MessagePart.Text>()
                    if (textParts.isNotEmpty()) {
                        MoccaIconButton(
                            icon = Icons.Default.Edit,
                            onClick = { onEditPart(textParts.first()); onDismiss() },
                            iconColor = AppColors.onSurfaceVariant,
                            contentDescription = "Edit message",
                            size = 32.dp
                        )
                    }

                    MoccaIconButton(
                        icon = Icons.Default.Delete,
                        onClick = { onDelete(); onDismiss() },
                        iconColor = AppColors.error,
                        contentDescription = "Delete message",
                        size = 32.dp
                    )
                }
            }
        )
    }
}
