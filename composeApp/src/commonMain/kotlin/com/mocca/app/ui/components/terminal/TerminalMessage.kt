package com.mocca.app.ui.components.terminal

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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.ui.components.RichToolCard
import com.mocca.app.ui.screens.chat.MarkdownText
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import com.mocca.app.domain.model.SessionStatus

// ... (existing imports)

/**
 * Terminal-styled message bubble.
 * Modern refactor: Rounded corners, distinct user/agent styles.
 */
@Composable
fun TerminalMessage(
    message: Message,
    modifier: Modifier = Modifier,
    isFirstInGroup: Boolean = true,
    dateHeader: String? = null,
    onFork: () -> Unit = {},
    onRevert: () -> Unit = {}
) {
    val isUser = message.role == MessageRole.USER
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (isFirstInGroup) AppSpacing.sm else 2.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (dateHeader != null) {
            DateSeparator(dateHeader)
            Spacer(modifier = Modifier.height(AppSpacing.md))
        }

        // Message Header
        if (isFirstInGroup) {
            Row(
                modifier = Modifier.padding(bottom = AppSpacing.xs, start = if (isUser) 0.dp else AppSpacing.sm, end = if (isUser) AppSpacing.sm else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                val icon = if (isUser) Icons.Default.Person else Icons.Default.SmartToy
                val label = if (isUser) "USER" else "AGENT"
                val color = if (isUser) AppColors.textSecondary else AppColors.accentGreen
                
                if (!isUser) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                }
                
                Text(
                    text = label,
                    color = color,
                    style = AppTypography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                
                Text(
                    text = formatTime(message.createdAt),
                    color = AppColors.textTertiary,
                    style = AppTypography.labelSmall
                )
                
                if (isUser) {
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        
        // Message Content Box
        // Agent: Glass/Dark Container, rounded corners
        // User: Subtle/Transparent Container, rounded corners
        
        // Explicitly create shapes instead of copying from abstract Shape
        val shape = if (isUser) {
            RoundedCornerShape(
                topStart = 32.dp,
                topEnd = 4.dp,
                bottomEnd = 32.dp,
                bottomStart = 32.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 32.dp,
                bottomEnd = 32.dp,
                bottomStart = 32.dp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 1f)
                .clip(shape)
                .background(
                    if (isUser) AppColors.surfaceVariant else AppColors.surfaceContainer,
                    shape
                )
                .border(
                    width = AppSpacing.borderThin,
                    color = if (isUser) AppColors.border else AppColors.borderLight,
                    shape = shape
                )
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.cardPadding)
            ) {
                message.parts.forEach { part ->
                    when (part) {
                        is MessagePart.Text -> {
                            MarkdownText(
                                markdown = part.text,
                                style = AppTypography.bodyMedium,
                                color = AppColors.white
                            )
                        }
                        is MessagePart.Reasoning -> {
                            TerminalReasoningBlock(part)
                        }
                        is MessagePart.ToolInvocation -> {
                            RichToolCard(part)
                        }
                        is MessagePart.ToolResult -> {
                            TerminalToolResultBlock(part)
                        }
                        is MessagePart.File -> {
                            TerminalFileBlock(part)
                        }
                        is MessagePart.SubTask -> {
                            TerminalSubTaskBlock(part)
                        }
                        is MessagePart.Thinking -> {
                            // Thinking content is shown via TerminalThinkingIndicator during streaming
                        }
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                }
            }
        }
    }
}

@Composable
fun TerminalSubTaskBlock(part: MessagePart.SubTask) {
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
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SUBTASK: ${part.title.uppercase()}",
                    style = AppTypography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                if (part.status == SessionStatus.RUNNING && part.streamingText.isNotEmpty()) {
                    Text(
                        text = "Executing...",
                        style = AppTypography.labelSmall,
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
                HorizontalDivider(color = statusColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                
                if (part.messages.isEmpty() && part.streamingText.isEmpty()) {
                    Text(
                        text = "Initializing subtask environment...",
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
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        displayMessages.forEach { msg ->
                            TerminalSubMessage(msg)
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
fun TerminalSubMessage(message: Message) {
    val isUser = message.role == MessageRole.USER
    val color = if (isUser) AppColors.textSecondary else AppColors.white
    
    Row(modifier = Modifier.fillMaxWidth()) {
        if (!isUser) {
            Text(
                text = ">", 
                color = AppColors.accentGreen, 
                style = AppTypography.bodySmall,
                modifier = Modifier.padding(end = 8.dp)
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
                            text = "[Tool: ${part.name}]",
                            style = AppTypography.labelSmall,
                            color = AppColors.textTertiary
                        )
                    }
                    is MessagePart.ToolResult -> {
                         Text(
                            text = "[Result]",
                            style = AppTypography.labelSmall,
                            color = AppColors.textTertiary
                        )
                    }
                    // Recursion for nested subtasks is possible but let's limit it
                    is MessagePart.SubTask -> {
                        Text(
                            text = "[Nested Subtask: ${part.title}]",
                             style = AppTypography.labelSmall,
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
fun TerminalReasoningBlock(part: MessagePart.Reasoning) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.medium)
            .background(AppColors.background.copy(alpha = 0.5f), AppShapes.medium)
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
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "THOUGHT PROCESS [${part.timeMs}ms]",
                color = AppColors.textSecondary,
                style = AppTypography.labelSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppColors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AppSpacing.sm),
                    thickness = AppSpacing.borderThin,
                    color = AppColors.border
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
fun TerminalToolResultBlock(part: MessagePart.ToolResult) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.surface, AppShapes.medium)
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
                style = AppTypography.labelSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppColors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = part.result,
                    color = AppColors.whiteMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun TerminalFileBlock(part: MessagePart.File) {
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
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        Text(
            text = part.filename ?: "ATTACHMENT",
            color = AppColors.textSecondary,
            style = AppTypography.bodySmall
        )
    }
}

/**
 * Streaming message bubble (for active generation).
 */
@Composable
fun TerminalStreamingMessage(
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
            modifier = Modifier.padding(bottom = AppSpacing.xs, start = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = AppColors.accentGreen,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Text(
                text = "AGENT STREAMING...",
                color = AppColors.accentGreen,
                style = AppTypography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Content Box
        // Use consistent shape with Agent messages
        val shape = RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 32.dp,
            bottomEnd = 32.dp,
            bottomStart = 32.dp
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AppColors.surfaceContainer, shape)
                .border(
                    width = AppSpacing.borderThin,
                    color = AppColors.accentGreen.copy(alpha = 0.5f),
                    shape = shape
                )
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.cardPadding)
            ) {
                // Render text with a blinking block cursor
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
