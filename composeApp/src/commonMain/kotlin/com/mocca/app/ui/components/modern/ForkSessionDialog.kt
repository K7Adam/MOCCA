package com.mocca.app.ui.components.modern

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ForkSessionDialog(
    messages: List<Message>,
    onFork: (Message) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(messages, query) {
        if (query.isBlank()) messages
        else messages.filter { msg ->
            msg.parts.filterIsInstance<MessagePart.Text>().any { it.text.contains(query, ignoreCase = true) }
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surfaceElevated, AppShapes.dialog)
                .border(AppSpacing.borderThin, AppColors.border, AppShapes.dialog)
                .padding(AppSpacing.lg)
        ) {
            Text(
                text = "FORK FROM",
                style = AppTypography.labelLarge,
                color = AppColors.accentGreen,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "Select the message to fork from",
                style = AppTypography.labelSmall,
                color = AppColors.textSecondary
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
            MoccaInput(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search messages...",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered) { message ->
                    ForkMessageRow(
                        message = message,
                        onClick = { onFork(message) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                MoccaOutlinedButton(
                    text = "CANCEL",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ForkMessageRow(
    message: Message,
    onClick: () -> Unit
) {
    val isUser = message.role == MessageRole.USER
    val roleLabel = if (isUser) "USER" else "AGENT"
    val roleColor = if (isUser) AppColors.textSecondary else AppColors.accentGreen
    val textSnippet = remember(message.parts) {
        message.parts.filterIsInstance<MessagePart.Text>().firstOrNull()?.text?.take(120)?.trim()
            ?: message.parts.filterIsInstance<MessagePart.ToolInvocation>().firstOrNull()?.let { "[${it.name}]" }
            ?: "…"
    }
    val timeLabel = remember(message.createdAt) {
        if (message.createdAt > 0L) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.createdAt))
        } else ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surfaceVariant, AppShapes.small)
            .border(AppSpacing.borderThin, AppColors.border.copy(alpha = 0.4f), AppShapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = roleLabel,
                    style = AppTypography.labelExtraSmall,
                    color = roleColor,
                    fontWeight = FontWeight.Bold
                )
                if (timeLabel.isNotEmpty()) {
                    Text(
                        text = timeLabel,
                        style = AppTypography.labelExtraSmall,
                        color = AppColors.textTertiary
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = textSnippet,
                style = AppTypography.bodySmall,
                color = AppColors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
