package com.mocca.app.ui.components.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.ui.components.RichToolCard
import com.mocca.app.ui.screens.chat.MarkdownText
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.domain.model.ToolState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Build

import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import com.mocca.app.domain.model.SessionStatus
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// ---------------------------------------------------------------------------
// Tool grouping helpers
// ---------------------------------------------------------------------------

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
 * Modern Glassmorphic message bubble.
 * Compact refactor: Rounded corners, reduced padding, distinct user/agent styles.
 *
 * @param message The message to display
 * @param modifier Modifier for the bubble
 * @param isFirstInGroup Whether this is the first message in a group
 * @param dateHeader Optional date header to display above the message
 * @param onFork Callback for forking the session from this message
 * @param onRevert Callback for reverting the session to this message
 * @param showTimestamps Whether to show timestamps in the message header
 * @param showTokenCounts Whether to show token counts (for assistant messages)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
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

        // Message Header
        if (isFirstInGroup) {
            val icon = if (isUser) Icons.Default.Person else Icons.Default.SmartToy
            val label = if (isUser) "USER" else "AGENT"
            val color = if (isUser) AppColors.textSecondary else AppColors.accentGreen
            val timeText = remember(message.createdAt) { formatTime(message.createdAt) }

            Row(
                modifier = Modifier.padding(bottom = AppSpacing.xxs, start = if (isUser) 0.dp else AppSpacing.sm, end = if (isUser) AppSpacing.sm else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                if (!isUser) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                }
                
                Text(
                    text = label,
                    color = color,
                    style = AppTypography.labelExtraSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Show timestamp if enabled
                if (showTimestamps) {
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    
                    Text(
                        text = timeText,
                        color = AppColors.textTertiary,
                        style = AppTypography.labelExtraSmall
                    )
                }
                
                if (isUser) {
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        
        // Message Content Box
        val shape = if (isUser) {
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 2.dp,
                bottomEnd = 16.dp,
                bottomStart = 16.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 2.dp,
                topEnd = 16.dp,
                bottomEnd = 16.dp,
                bottomStart = 16.dp
            )
        }

        val bubbleColor = remember(isUser) {
            // Use LiquidGlass tints for consistent glass effect with good contrast
            if (isUser) LiquidGlassDefaults.tintSecondary else LiquidGlassDefaults.tintPrimary
        }
        val borderColor = remember(isUser) {
            // Use LiquidGlass borders for consistent edge styling
            if (isUser) LiquidGlassDefaults.borderPrimary.copy(alpha = 0.4f) else LiquidGlassDefaults.borderPrimary.copy(alpha = 0.3f)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 1f)
                .clip(shape)
                .background(bubbleColor, shape)
                .border(
                    width = AppSpacing.borderThin,
                    color = borderColor,
                    shape = shape
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showContextMenu = true
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.md)
            ) {
                val partGroups = remember(message.parts) { groupParts(message.parts) }
                partGroups.forEach { group ->
                    when (group) {
                        is PartGroup.Single -> when (val part = group.part) {
                            is MessagePart.Text -> {
                                MarkdownText(
                                    markdown = part.text,
                                    style = AppTypography.bodyMedium,
                                    color = AppColors.white,
                                    onFileClick = onFileClick
                                )
                            }
                            is MessagePart.Reasoning -> {
                                ModernReasoningBlock(part)
                            }
                            is MessagePart.ToolInvocation -> {
                                RichToolCard(part)
                            }
                            is MessagePart.ToolResult -> {
                                ModernToolResultBlock(part)
                            }
                            is MessagePart.File -> {
                                ModernFileBlock(part)
                            }
                            is MessagePart.SubTask -> {
                                ModernSubTaskBlock(part)
                            }
                            is MessagePart.Thinking -> {
                                ModernThinkingBlock(part)
                            }
                        }
                        is PartGroup.ToolGroup -> {
                            ContextToolGroup(tools = group.tools)
                        }
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                }
                
                // Token Count Display (for assistant messages with token info)
                if (showTokenCounts && !isUser && message.tokens != null) {
                    val tokens = message.tokens
                    if (tokens.input > 0 || tokens.output > 0) {
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        HorizontalDivider(
                            color = AppColors.border.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
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
                }
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier.background(AppColors.surfaceElevated, AppShapes.medium)
        ) {
            DropdownMenuItem(
                text = { Text("FORK FROM HERE", style = AppTypography.labelSmall, color = AppColors.textSecondary) },
                onClick = { onFork(); showContextMenu = false }
            )
            DropdownMenuItem(
                text = { Text("REVERT TO HERE", style = AppTypography.labelSmall, color = AppColors.textSecondary) },
                onClick = { onRevert(); showContextMenu = false }
            )
            // EDIT PART — only shown for messages with editable (Text) parts
            val textParts = message.parts.filterIsInstance<MessagePart.Text>()
            if (textParts.isNotEmpty()) {
                HorizontalDivider(color = AppColors.border.copy(alpha = 0.3f))
                textParts.forEachIndexed { index, textPart ->
                    val label = if (textParts.size > 1) "EDIT PART ${index + 1}" else "EDIT PART"
                    DropdownMenuItem(
                        text = { Text(label, style = AppTypography.labelSmall, color = AppColors.textSecondary) },
                        onClick = { onEditPart(textPart); showContextMenu = false }
                    )
                }
            }
            HorizontalDivider(color = AppColors.border.copy(alpha = 0.3f))
            DropdownMenuItem(
                text = { Text("DELETE MESSAGE", style = AppTypography.labelSmall, color = AppColors.error) },
                onClick = { onDelete(); showContextMenu = false }
            )
        }
    }

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

@Composable
fun ModernSubTaskBlock(part: MessagePart.SubTask) {
    var expanded by remember { mutableStateOf(part.status == SessionStatus.RUNNING) }
    
    val statusColor = when (part.status) {
        SessionStatus.RUNNING -> AppColors.accentGreen
        SessionStatus.COMPLETED -> AppColors.grey
        SessionStatus.ERROR -> AppColors.error
        SessionStatus.IDLE -> AppColors.textTertiary
    }
    
    val statusIcon = when (part.status) {
        SessionStatus.RUNNING -> Icons.Default.PlayArrow
        SessionStatus.COMPLETED -> Icons.Default.CheckCircle
        SessionStatus.ERROR -> Icons.Default.Error
        SessionStatus.IDLE -> Icons.Default.Terminal
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .border(1.dp, statusColor.copy(alpha = 0.5f), AppShapes.medium)
            .background(AppColors.surface, AppShapes.medium)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(AppSpacing.sm)
                .background(statusColor.copy(alpha = 0.1f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = part.title.uppercase(),
                    style = AppTypography.labelExtraSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                if (part.status == SessionStatus.RUNNING && part.streamingText.isNotEmpty()) {
                    Text(
                        text = "EXECUTING...",
                        style = AppTypography.labelExtraSmall,
                        color = AppColors.textTertiary
                    )
                }
            }
            
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
        }

        // Expanded Content
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
            ) {
                HorizontalDivider(color = statusColor.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                
                if (part.messages.isEmpty() && part.streamingText.isEmpty()) {
                    Text(
                        text = "INITIALIZING...",
                        style = AppTypography.bodySmall,
                        color = AppColors.textSecondary,
                        modifier = Modifier.padding(bottom = AppSpacing.sm)
                    )
                } else {
                    // Render sub-messages
                    val displayMessages = part.messages.filter { msg ->
                        msg.role == MessageRole.USER || msg.parts.isNotEmpty()
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        displayMessages.forEach { msg ->
                            ModernSubMessage(msg)
                        }
                        
                        if (part.streamingText.isNotEmpty()) {
                            MarkdownText(
                                markdown = part.streamingText + "█",
                                style = AppTypography.bodySmall,
                                color = AppColors.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                }
            }
        }
    }
}

@Composable
fun ModernSubMessage(message: Message) {
    val isUser = message.role == MessageRole.USER
    val color = if (isUser) AppColors.textSecondary else AppColors.white
    
    Row(modifier = Modifier.fillMaxWidth()) {
        if (!isUser) {
            Text(
                text = "•", 
                color = AppColors.accentGreen, 
                style = AppTypography.bodySmall,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        
        Column {
            message.parts.forEach { part ->
                when (part) {
                    is MessagePart.Text -> {
                        MarkdownText(
                            markdown = part.text,
                            style = AppTypography.bodySmall,
                            color = color
                        )
                    }
                    is MessagePart.ToolInvocation -> {
                        Text(
                            text = "{ ${part.name} }",
                            style = AppTypography.labelExtraSmall,
                            color = AppColors.textTertiary
                        )
                    }
                    is MessagePart.ToolResult -> {
                         Text(
                            text = "{ Result }",
                            style = AppTypography.labelExtraSmall,
                            color = AppColors.textTertiary
                        )
                    }
                    is MessagePart.SubTask -> {
                        Text(
                            text = "{ ${part.title} }",
                             style = AppTypography.labelExtraSmall,
                            color = AppColors.accentGreen
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}


@Composable
fun ModernReasoningBlock(part: MessagePart.Reasoning) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.medium)
            .background(AppColors.background.copy(alpha = 0.3f), AppShapes.medium)
            .padding(AppSpacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = AppColors.statusThinking,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "THOUGHTS [${part.timeMs}ms]",
                color = AppColors.textSecondary,
                style = AppTypography.labelExtraSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppColors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AppSpacing.sm),
                    thickness = AppSpacing.borderThin,
                    color = AppColors.border.copy(alpha = 0.5f)
                )
                Text(
                    text = part.content,
                    color = AppColors.textTertiary,
                    style = AppTypography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ModernThinkingBlock(part: MessagePart.Thinking) {
    var expanded by remember { mutableStateOf(false) }
    val durationText = part.durationMs?.let { ms ->
        when {
            ms < 1000 -> "${ms}ms"
            ms < 60000 -> "${ms / 1000}.${(ms % 1000) / 100}s"
            else -> "${ms / 60000}m ${(ms % 60000) / 1000}s"
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .border(AppSpacing.borderThin, AppColors.statusThinking.copy(alpha = 0.3f), AppShapes.medium)
            .background(AppColors.background.copy(alpha = 0.3f), AppShapes.medium)
            .padding(AppSpacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = AppColors.statusThinking,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = if (durationText != null) "THINKING [$durationText]" else "THINKING",
                color = AppColors.statusThinking,
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppColors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AppSpacing.sm),
                    thickness = AppSpacing.borderThin,
                    color = AppColors.statusThinking.copy(alpha = 0.2f)
                )
                Text(
                    text = part.content,
                    color = AppColors.textTertiary,
                    style = AppTypography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ModernToolResultBlock(part: MessagePart.ToolResult) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.surface.copy(alpha = 0.5f), AppShapes.medium)
            .padding(AppSpacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOOL OUTPUT",
                color = AppColors.textSecondary,
                style = AppTypography.labelExtraSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppColors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = part.result,
                    color = AppColors.whiteDim,
                    style = AppTypography.bodySmall
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ContextToolGroup — collapsible card grouping consecutive tool calls
// ---------------------------------------------------------------------------

@Composable
fun ContextToolGroup(tools: List<Pair<MessagePart.ToolInvocation, MessagePart.ToolResult?>>) {
    if (tools.isEmpty()) return

    val hasRunning = tools.any { it.first.state == ToolState.RUNNING }
    val errorCount = tools.count { it.first.state == ToolState.ERROR }
    val completedCount = tools.count { it.first.state == ToolState.COMPLETED }

    // Auto-expand when any tool is running; collapse when everything is done and count >= 2
    var expanded by remember(hasRunning) { mutableStateOf(hasRunning || tools.size == 1) }

    val accentColor = when {
        hasRunning -> AppColors.statusWaiting
        errorCount > 0 -> AppColors.error
        else -> AppColors.textTertiary
    }
    val borderColor = accentColor.copy(alpha = 0.35f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .border(AppSpacing.borderThin, borderColor, AppShapes.medium)
            .background(LiquidGlassDefaults.tintPrimary.copy(alpha = 0.6f), AppShapes.medium)
    ) {
        // Header row — always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "${tools.size} TOOLS",
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.weight(1f)
            )
            // Status badges
            if (hasRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = AppColors.statusWaiting
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
            }
            if (completedCount > 0) {
                Text(
                    text = "\u2713 $completedCount",
                    style = AppTypography.labelExtraSmall,
                    color = AppColors.accentGreen
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
            }
            if (errorCount > 0) {
                Text(
                    text = "\u2717 $errorCount",
                    style = AppTypography.labelExtraSmall,
                    color = AppColors.error
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppColors.textTertiary,
                modifier = Modifier.size(14.dp)
            )
        }

        // Expanded tool cards
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(
                    start = AppSpacing.sm,
                    end = AppSpacing.sm,
                    bottom = AppSpacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                tools.forEach { (invocation, _) ->
                    RichToolCard(invocation)
                }
            }
        }
    }
}

@Composable
fun ModernFileBlock(part: MessagePart.File) {
    val isImage = part.mediaType.startsWith("image/")
    var showPreview by remember { mutableStateOf(false) }

    if (isImage && part.url != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .border(AppSpacing.borderThin, AppColors.border, AppShapes.medium)
                .clickable { showPreview = true }
                .padding(AppSpacing.xxs)
        ) {
            coil3.compose.AsyncImage(
                model = part.url,
                contentDescription = part.filename,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(AppShapes.medium),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            if (part.filename != null) {
                Text(
                    text = part.filename,
                    style = AppTypography.labelExtraSmall,
                    color = AppColors.textTertiary,
                    modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                )
            }
        }
        if (showPreview) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showPreview = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.background.copy(alpha = 0.95f))
                        .clickable { showPreview = false },
                    contentAlignment = Alignment.Center
                ) {
                    coil3.compose.AsyncImage(
                        model = part.url,
                        contentDescription = part.filename,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clip(AppShapes.card),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    Text(
                        text = "TAP TO CLOSE",
                        style = AppTypography.labelExtraSmall,
                        color = AppColors.textTertiary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = AppSpacing.xl)
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .border(AppSpacing.borderThin, AppColors.border, AppShapes.medium)
                .padding(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = AppColors.textSecondary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = part.filename ?: "ATTACHMENT",
                color = AppColors.textSecondary,
                style = AppTypography.bodySmall
            )
        }
    }
}

/**
 * Streaming message bubble (for active generation).
 */
@Composable
fun ModernStreamingMessage(
    text: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.sm),
        horizontalAlignment = Alignment.Start
    ) {
        // Header
        Row(
            modifier = Modifier.padding(bottom = AppSpacing.xxs, start = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = AppColors.accentGreen,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Text(
                text = "AGENT STREAMING...",
                color = AppColors.accentGreen,
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Content Box
        val shape = RoundedCornerShape(
            topStart = 2.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AppColors.surfaceContainer.copy(alpha = 0.9f), shape)
                .border(
                    width = AppSpacing.borderThin,
                    color = AppColors.accentGreen.copy(alpha = 0.3f),
                    shape = shape
                )
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.md)
            ) {
                Row {
                    MarkdownText(
                        markdown = text,
                        style = AppTypography.bodyMedium,
                        color = AppColors.white
                    )
                    Text(
                        text = "█",
                        color = AppColors.accentGreen.copy(alpha = cursorAlpha),
                        style = AppTypography.bodyMedium,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSeparator(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AppColors.border.copy(alpha = 0.5f))
        )
        Text(
            text = date.uppercase(),
            style = AppTypography.labelExtraSmall,
            color = AppColors.textTertiary,
            modifier = Modifier.padding(horizontal = AppSpacing.md),
            letterSpacing = 2.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AppColors.border.copy(alpha = 0.5f))
        )
    }
}

private fun formatTime(timestamp: Long): String {
    return com.mocca.app.util.TimeFormatter.formatTime(timestamp)
}
