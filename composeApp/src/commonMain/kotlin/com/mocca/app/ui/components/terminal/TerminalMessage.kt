package com.mocca.app.ui.components.terminal

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.ui.components.RichToolCard
import com.mocca.app.ui.screens.chat.MarkdownText
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalShapes
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

/**
 * Terminal-styled message bubble.
 * Modern refactor: Rounded corners, distinct user/agent styles.
 */
@Composable
fun TerminalMessage(
    message: Message,
    modifier: Modifier = Modifier,
    onFork: () -> Unit = {},
    onRevert: () -> Unit = {}
) {
    val isUser = message.role == MessageRole.USER
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TerminalSpacing.sm),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Message Header
        Row(
            modifier = Modifier.padding(bottom = TerminalSpacing.xs, start = if (isUser) 0.dp else TerminalSpacing.sm, end = if (isUser) TerminalSpacing.sm else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            val icon = if (isUser) Icons.Default.Person else Icons.Default.SmartToy
            val label = if (isUser) "USER" else "AGENT"
            val color = if (isUser) TerminalColors.textSecondary else TerminalColors.accentGreen
            
            if (!isUser) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(TerminalSpacing.xs))
            }
            
            Text(
                text = label,
                color = color,
                style = TerminalTypography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
            
            Text(
                text = formatTime(message.createdAt),
                color = TerminalColors.textTertiary,
                style = TerminalTypography.labelSmall
            )
            
            if (isUser) {
                Spacer(modifier = Modifier.width(TerminalSpacing.xs))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
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
                    if (isUser) TerminalColors.surfaceVariant else TerminalColors.surfaceContainer,
                    shape
                )
                .border(
                    width = TerminalSpacing.borderThin,
                    color = if (isUser) TerminalColors.border else TerminalColors.borderLight,
                    shape = shape
                )
        ) {
            Column(
                modifier = Modifier.padding(TerminalSpacing.cardPadding)
            ) {
                message.parts.forEach { part ->
                    when (part) {
                        is MessagePart.Text -> {
                            MarkdownText(
                                markdown = part.text,
                                style = TerminalTypography.bodyMedium,
                                color = TerminalColors.white
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
                            // Subtasks handled separately or embedded
                        }
                        is MessagePart.Thinking -> {
                            // Thinking content is shown via TerminalThinkingIndicator during streaming
                        }
                    }
                    Spacer(modifier = Modifier.height(TerminalSpacing.sm))
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
            .clip(TerminalShapes.medium)
            .border(TerminalSpacing.borderThin, TerminalColors.border, TerminalShapes.medium)
            .background(TerminalColors.background.copy(alpha = 0.5f), TerminalShapes.medium)
            .padding(TerminalSpacing.sm)
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
                tint = TerminalColors.statusThinking,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
            Text(
                text = "THOUGHT PROCESS [${part.timeMs}ms]",
                color = TerminalColors.textSecondary,
                style = TerminalTypography.labelSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TerminalColors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = TerminalSpacing.sm),
                    thickness = TerminalSpacing.borderThin,
                    color = TerminalColors.border
                )
                Text(
                    text = part.content,
                    color = TerminalColors.textTertiary,
                    style = TerminalTypography.bodySmall
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
            .clip(TerminalShapes.medium)
            .background(TerminalColors.surface, TerminalShapes.medium)
            .padding(TerminalSpacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOOL OUTPUT",
                color = TerminalColors.textSecondary,
                style = TerminalTypography.labelSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TerminalColors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(modifier = Modifier.height(TerminalSpacing.xs))
                Text(
                    text = part.result,
                    color = TerminalColors.whiteMuted,
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
            .clip(TerminalShapes.medium)
            .border(TerminalSpacing.borderThin, TerminalColors.border, TerminalShapes.medium)
            .padding(TerminalSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = TerminalColors.textSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(TerminalSpacing.sm))
        Text(
            text = part.filename ?: "ATTACHMENT",
            color = TerminalColors.textSecondary,
            style = TerminalTypography.bodySmall
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TerminalSpacing.sm),
        horizontalAlignment = Alignment.Start
    ) {
        // Header
        Row(
            modifier = Modifier.padding(bottom = TerminalSpacing.xs, start = TerminalSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = TerminalColors.accentGreen,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(TerminalSpacing.xs))
            Text(
                text = "AGENT STREAMING...",
                color = TerminalColors.accentGreen,
                style = TerminalTypography.labelSmall,
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
                .background(TerminalColors.surfaceContainer, shape)
                .border(
                    width = TerminalSpacing.borderThin,
                    color = TerminalColors.accentGreen.copy(alpha = 0.5f),
                    shape = shape
                )
        ) {
            Column(
                modifier = Modifier.padding(TerminalSpacing.cardPadding)
            ) {
                MarkdownText(
                    markdown = text + "█", // Cursor effect
                    style = TerminalTypography.bodyMedium,
                    color = TerminalColors.white
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return com.mocca.app.util.TimeFormatter.formatTime(timestamp)
}