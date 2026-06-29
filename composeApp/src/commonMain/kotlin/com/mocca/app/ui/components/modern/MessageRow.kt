package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
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
import com.mocca.app.util.ChatExporter
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
    onFork: (() -> Unit)? = null,
    onRevert: (() -> Unit)? = null,
    showTimestamps: Boolean = true,
    showTokenCounts: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onDeletePart: (String) -> Unit = {},
    onEditPart: ((MessagePart) -> Unit)? = null,
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
                },
                onRevert = onRevert
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
    onLongClick: () -> Unit,
    onRevert: (() -> Unit)? = null
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
                RenderPartGroup(group = group, onFileClick = onFileClick, onRevert = onRevert)
                Spacer(modifier = Modifier.height(AppSpacing.xs))
            }

            if (showTokenCounts && message.tokens != null) {
                TokenCountFooter(tokens = message.tokens, cost = message.cost)
            }
        }
    }
}

@Composable
private fun RenderPartGroup(
    group: MessagePartGroup,
    onFileClick: ((String) -> Unit)?,
    onRevert: (() -> Unit)? = null
) {
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
            // V2 part types — minimal rendering for now
            is MessagePart.Snapshot -> SnapshotBadge(part, onRevert = onRevert)
            is MessagePart.Patch -> PatchBadge(part)
            is MessagePart.AgentDelegate -> AgentDelegateBadge(part)
            is MessagePart.Retry -> RetryBadge(part)
            is MessagePart.Compaction -> CompactionBadge(part)
        }

        is MessagePartGroup.ToolGroup -> ContextToolGroup(tools = group.tools)
    }
}

@Composable
private fun TokenCountFooter(tokens: TokenUsage, cost: Double? = null) {
    if (!tokens.hasVisibleDetails && cost == null) return

    Spacer(modifier = Modifier.height(AppSpacing.xs))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tokens.input > 0) {
            TokenChip("IN", FormatUtils.formatCompactNumber(tokens.input), AppColors.outline)
        }
        if (tokens.output > 0) {
            TokenChip("OUT", FormatUtils.formatCompactNumber(tokens.output), AppColors.primary.copy(alpha = 0.7f))
        }
        if (tokens.reasoning > 0) {
            TokenChip("THINK", FormatUtils.formatCompactNumber(tokens.reasoning), AppColors.primary.copy(alpha = 0.5f))
        }
        tokens.cache?.let { cache ->
            if (cache.read > 0 || cache.write > 0) {
                val cacheText = buildString {
                    if (cache.read > 0) append("${FormatUtils.formatCompactNumber(cache.read)}R")
                    if (cache.read > 0 && cache.write > 0) append("/")
                    if (cache.write > 0) append("${FormatUtils.formatCompactNumber(cache.write)}W")
                }
                TokenChip("CACHE", cacheText, AppColors.accentGreen.copy(alpha = 0.7f))
            }
        }
        cost?.let { c ->
            if (c > 0.0) {
                TokenChip("", FormatUtils.formatCost(c), AppColors.warning)
            }
        }
    }
}

@Composable
private fun TokenChip(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .background(AppColors.bgRaised, AppShapes.badge)
            .padding(horizontal = AppSpacing.xs, vertical = 1.dp)
    ) {
        Text(
            text = if (label.isNotEmpty()) "$label: $value" else value,
            color = color,
            style = AppTypography.labelExtraSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun Message.buildCopyText(): String {
    return parts.mapNotNull { part ->
        when (part) {
            is MessagePart.Text -> part.text.trim().takeIf { it.isNotEmpty() }
            is MessagePart.Reasoning -> part.content.trim().takeIf { it.isNotEmpty() }
            is MessagePart.Thinking -> part.content.trim().takeIf { it.isNotEmpty() }
            is MessagePart.ToolInvocation -> buildString {
                append("Tool: ${part.name}")
                val input = part.input.trim()
                if (input.isNotEmpty()) {
                    append("\n")
                    append(input)
                }
            }.trim().takeIf { it.isNotEmpty() }
            is MessagePart.ToolResult -> part.result.trim().takeIf { it.isNotEmpty() }?.let { "Result:\n$it" }
            is MessagePart.File -> part.filename?.trim()?.takeIf { it.isNotEmpty() }?.let { "File: $it" }
            is MessagePart.SubTask -> null
            // V2 part types — minimal export text
            is MessagePart.Snapshot -> "Snapshot: ${part.messageId}"
            is MessagePart.Patch -> "Patch: ${part.path} (+${part.additions}/-${part.deletions})"
            is MessagePart.AgentDelegate -> "Agent: ${part.agentName} [${part.status}]"
            is MessagePart.Retry -> "Retry #${part.attempt}${part.reason?.let { " - $it" } ?: ""}"
            is MessagePart.Compaction -> "Context compacted (${part.tokensBefore}→${part.tokensAfter} tokens)"
        }
    }.joinToString("\n\n")
}

private fun Message.buildExportText(): String {
    return ChatExporter.exportSessionToMarkdown(
        sessionTitle = "Message Export",
        messages = listOf(this)
    ).trim()
}

@Composable
private fun MessageContextMenu(
    expanded: Boolean,
    message: Message,
    onDismiss: () -> Unit,
    onFork: (() -> Unit)?,
    onRevert: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onEditPart: ((MessagePart) -> Unit)?
) {
    if (!expanded) return

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val copyText = remember(message.parts) { message.buildCopyText() }
    val exportText = remember(message.id, message.role, message.createdAt, message.parts) { message.buildExportText() }
    val editablePart = remember(message.parts) { message.parts.filterIsInstance<MessagePart.Text>().firstOrNull() }
    val editAction = onEditPart
    val forkAction = onFork
    val revertAction = onRevert
    val deleteAction = onDelete
    val canCopy = copyText.isNotBlank()
    val canEdit = editablePart != null && editAction != null
    val canFork = forkAction != null
    val canRevert = revertAction != null
    val canDelete = deleteAction != null
    val canExport = exportText.isNotBlank()

    if (!canCopy && !canEdit && !canFork && !canRevert && !canDelete && !canExport) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = AppColors.bgRaised,
            shape = AppShapes.pill,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canCopy) {
                    MoccaIconButton(
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(copyText))
                            onDismiss()
                        },
                        iconColor = AppColors.onSurfaceVariant,
                        contentDescription = "Copy message",
                        size = 32.dp
                    )
                }

                if (canEdit) {
                    MoccaIconButton(
                        icon = Icons.Default.Edit,
                        onClick = {
                            editAction(editablePart)
                            onDismiss()
                        },
                        iconColor = AppColors.onSurfaceVariant,
                        contentDescription = "Edit message",
                        size = 32.dp
                    )
                }

                if (canFork) {
                    MoccaIconButton(
                        icon = Icons.Default.Refresh,
                        onClick = {
                            forkAction()
                            onDismiss()
                        },
                        iconColor = AppColors.onSurfaceVariant,
                        contentDescription = "Fork from message",
                        size = 32.dp
                    )
                }

                if (canRevert) {
                    MoccaIconButton(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        onClick = {
                            revertAction()
                            onDismiss()
                        },
                        iconColor = AppColors.onSurfaceVariant,
                        contentDescription = "Revert to message",
                        size = 32.dp
                    )
                }

                if (canExport) {
                    MoccaIconButton(
                        icon = Icons.Default.Share,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(exportText))
                            onDismiss()
                        },
                        iconColor = AppColors.onSurfaceVariant,
                        contentDescription = "Export message as Markdown",
                        size = 32.dp
                    )
                }

                if (canDelete) {
                    MoccaIconButton(
                        icon = Icons.Default.Delete,
                        onClick = {
                            deleteAction()
                            onDismiss()
                        },
                        iconColor = AppColors.error,
                        contentDescription = "Delete message",
                        size = 32.dp
                    )
                }
            }
        }
    }
}

// ==== V2 Part Type Badges ====

@Composable
private fun SnapshotBadge(
    part: com.mocca.app.domain.model.MessagePart.Snapshot,
    onRevert: (() -> Unit)? = null
) {
    V2PartBadge(
        icon = Icons.Default.CameraAlt,
        label = "Snapshot",
        detail = part.messageId.take(8),
        onClick = onRevert
    )
}

@Composable
private fun PatchBadge(part: com.mocca.app.domain.model.MessagePart.Patch) {
    V2PartBadge(
        icon = Icons.Default.Difference,
        label = "Patch",
        detail = "${part.path} (+${part.additions}/-${part.deletions})"
    )
}

@Composable
private fun AgentDelegateBadge(part: com.mocca.app.domain.model.MessagePart.AgentDelegate) {
    V2PartBadge(
        icon = Icons.Default.SmartToy,
        label = "Agent: ${part.agentName}",
        detail = part.status
    )
}

@Composable
private fun RetryBadge(part: com.mocca.app.domain.model.MessagePart.Retry) {
    V2PartBadge(
        icon = Icons.Default.Refresh,
        label = "Retry #${part.attempt}",
        detail = part.reason ?: ""
    )
}

@Composable
private fun CompactionBadge(part: com.mocca.app.domain.model.MessagePart.Compaction) {
    V2PartBadge(
        icon = Icons.Default.Compress,
        label = "Context Compacted",
        detail = "${part.tokensBefore}→${part.tokensAfter} tokens"
    )
}

@Composable
private fun V2PartBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (onClick != null) {
                    Modifier.clip(AppShapes.small).clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = AppColors.onSurfaceVariant
        )
        Text(
            text = label,
            style = AppTypography.labelSmall,
            color = AppColors.onSurfaceVariant
        )
        if (detail.isNotEmpty()) {
            Text(
                text = detail,
                style = AppTypography.labelSmall,
                color = AppColors.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
