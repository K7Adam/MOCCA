package com.mocca.app.ui.components.modern.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.domain.model.SessionStatus
import com.mocca.app.ui.screens.chat.MarkdownText
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

// ---------------------------------------------------------------------------
// SubTask block + SubMessage renderer
// ---------------------------------------------------------------------------

@Composable
fun ModernSubTaskBlock(part: MessagePart.SubTask) {
    var expanded by remember { mutableStateOf(part.status == SessionStatus.RUNNING) }

    val statusColor = when (part.status) {
        SessionStatus.RUNNING -> AppColors.accentGreen
        SessionStatus.COMPLETED -> AppColors.textSecondary
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
    val color = if (isUser) AppColors.textSecondary else AppColors.textPrimary

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
