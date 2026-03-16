package com.mocca.app.ui.components.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.ui.theme.MoccaTheme

/**
 * A modern chat bubble with Material 3 Expressive motion and theme integration.
 */
@Composable
fun ModernChatBubble(
    text: String,
    role: MessageRole,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val isUser = role == MessageRole.USER
    val containerColor = if (isUser) {
        MoccaTheme.colors.primaryContainer
    } else {
        MoccaTheme.colors.surfaceContainer
    }
    
    val contentColor = if (isUser) {
        MoccaTheme.colors.onPrimaryContainer
    } else {
        MoccaTheme.colors.onSurface
    }

    val shape = if (isUser) {
        MoccaTheme.shapes.large.copy(
            bottomEnd = androidx.compose.foundation.shape.CornerSize(4.dp)
        )
    } else {
        MoccaTheme.shapes.large.copy(
            bottomStart = androidx.compose.foundation.shape.CornerSize(4.dp)
        )
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = shape,
        modifier = modifier
            .widthIn(max = 340.dp)
            .padding(vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            content()
        }
    }
}

/**
 * A complete message item row with header and bubble.
 */
@Composable
fun ModernMessageItem(
    message: Message,
    modifier: Modifier = Modifier,
    isFirstInGroup: Boolean = true,
    isStreaming: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val isUser = message.role == MessageRole.USER
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (isFirstInGroup) 8.dp else 2.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isFirstInGroup) {
            Text(
                text = if (isUser) "YOU" else "AGENT",
                style = MoccaTheme.typography.labelSmall,
                color = MoccaTheme.colors.textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        ModernChatBubble(
            text = "", // Text is handled in content block or passed separately
            role = message.role,
            isStreaming = isStreaming
        ) {
            // Simplified for now, in a real scenario we'd map message parts here
            Text(
                text = message.parts.filterIsInstance<com.mocca.app.domain.model.MessagePart.Text>()
                    .firstOrNull()?.text ?: "",
                style = MoccaTheme.typography.bodyMedium
            )
        }
    }
}
