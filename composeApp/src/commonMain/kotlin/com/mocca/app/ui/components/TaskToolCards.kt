package com.mocca.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.ToolState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.serialization.json.*

@Composable
fun TodoToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    BaseToolCard(
        toolName = "todo",
        state = part.state,
        title = part.title ?: "Task List",
        icon = Icons.Default.Checklist,
        iconTint = AppColors.accent,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            part.output?.takeIf { it.isNotBlank() }?.let { output ->
                TodoListView(output)
            }
            
            part.error?.takeIf { it.isNotBlank() }?.let { error ->
                ErrorBlock(error)
            }
        }
    }
}

@Composable
fun WebFetchToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    val url = extractInputField(part.input, "url")
    val format = extractInputField(part.input, "format")
    
    BaseToolCard(
        toolName = "webfetch",
        state = part.state,
        title = url?.take(50) ?: part.title,
        icon = Icons.Default.Language,
        iconTint = AppColors.statusWaiting,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            // URL
            if (!url.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.white
                    )
                    Text(
                        text = url,
                        style = AppTypography.bodySmall,
                        color = AppColors.white,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Format
            if (!format.isNullOrBlank()) {
                Text(
                    text = "Format: $format",
                    style = AppTypography.labelSmall,
                    color = AppColors.grey
                )
            }
            
            // Content preview
            part.output?.takeIf { it.isNotBlank() }?.let { output ->
                CodeBlock(
                    code = output.take(2000),
                    label = "Content",
                    language = if (format == "html") "html" else "markdown",
                    maxLines = 20
                )
            }
            
            part.error?.takeIf { it.isNotBlank() }?.let { error ->
                ErrorBlock(error)
            }
            
            ShowDuration(part.richState)
        }
    }
}

@Composable
fun TaskToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    val description = extractInputField(part.input, "description")
    val subagentType = extractInputField(part.input, "subagent_type")
    
    BaseToolCard(
        toolName = "task",
        state = part.state,
        title = description ?: subagentType ?: part.title,
        icon = Icons.Default.AccountTree,
        iconTint = AppColors.accent,
        modifier = modifier,
        headerExtra = {
            if (!subagentType.isNullOrBlank()) {
                Surface(
                    shape = AppShapes.medium,
                    color = AppColors.surfaceVariant
                ) {
                    Text(
                        text = subagentType,
                        style = AppTypography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = AppColors.white
                    )
                }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            // Agent response preview
            part.output?.takeIf { it.isNotBlank() }?.let { output ->
                CodeBlock(
                    code = output.take(2000),
                    label = "Agent Response",
                    language = "markdown",
                    maxLines = 20
                )
            }
            
            part.error?.takeIf { it.isNotBlank() }?.let { error ->
                ErrorBlock(error)
            }
            
            ShowDuration(part.richState)
        }
    }
}

@Composable
fun GenericToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    BaseToolCard(
        toolName = part.name,
        state = part.state,
        title = part.title,
        icon = Icons.Default.Build,
        iconTint = when (part.state) {
            ToolState.PENDING -> AppColors.grey
            ToolState.RUNNING -> AppColors.statusWaiting
            ToolState.COMPLETED -> AppColors.accent
            ToolState.ERROR -> AppColors.error
        },
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            // Input
            if (part.input.isNotBlank()) {
                CodeBlock(
                    code = part.input.take(1000),
                    label = "Input",
                    language = "json"
                )
            }
            
            // Output
            part.output?.takeIf { it.isNotBlank() }?.let { output ->
                CodeBlock(
                    code = output.take(2000),
                    label = "Output",
                    language = "text",
                    maxLines = 20
                )
            }
            
            part.error?.takeIf { it.isNotBlank() }?.let { error ->
                ErrorBlock(error)
            }
            
            ShowDuration(part.richState)
        }
    }
}

// ===== Todo helpers =====

@Composable
private fun TodoListView(output: String) {
    // Try to parse as JSON todo list
    val todos = remember(output) {
        try {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<List<TodoItem>>(output)
        } catch (e: Exception) {
            null
        }
    }

    if (todos != null) {
        val inProgress = todos.filter { it.status == "in_progress" }
        val pending = todos.filter { it.status == "pending" }
        val completed = todos.filter { it.status == "completed" }
        
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            // Summary
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                Text(
                    text = "Total: ${todos.size}",
                    style = AppTypography.labelSmall,
                    color = AppColors.grey
                )
                if (inProgress.isNotEmpty()) {
                    Text(
                        text = "In Progress: ${inProgress.size}",
                        style = AppTypography.labelSmall,
                        color = AppColors.statusWaiting
                    )
                }
                if (completed.isNotEmpty()) {
                    Text(
                        text = "Done: ${completed.size}",
                        style = AppTypography.labelSmall,
                        color = AppColors.accent
                    )
                }
            }
            
            // In progress items
            inProgress.forEach { todo ->
                TodoItemRow(todo, isInProgress = true)
            }
            
            // Pending items
            pending.forEach { todo ->
                TodoItemRow(todo, isInProgress = false)
            }
            
            // Completed items (collapsed by default)
            if (completed.isNotEmpty()) {
                var showCompleted by remember { mutableStateOf(false) }
                TextButton(onClick = { showCompleted = !showCompleted }) {
                    Text(
                        text = if (showCompleted) "Hide completed (${completed.size})" else "Show completed (${completed.size})",
                        style = AppTypography.labelSmall,
                        color = AppColors.grey
                    )
                }
                if (showCompleted) {
                    completed.forEach { todo ->
                        TodoItemRow(todo, isCompleted = true)
                    }
                }
            }
        }
    } else {
        // Fall back to plain text
        Text(
            text = output.take(500),
            style = AppTypography.bodySmall,
            color = AppColors.white
        )
    }
}

@kotlinx.serialization.Serializable
private data class TodoItem(
    val id: String? = null,
    val content: String,
    val status: String,
    val priority: String? = null
)

@Composable
private fun TodoItemRow(
    todo: TodoItem,
    isInProgress: Boolean = false,
    isCompleted: Boolean = false
) {
    Row(
        modifier = Modifier.padding(vertical = AppSpacing.xxs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Status indicator
        when {
            isInProgress -> {
                val infiniteTransition = rememberInfiniteTransition()
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(y = 4.dp)
                        .background(
                            AppColors.statusWaiting.copy(alpha = alpha),
                            AppShapes.medium
                        )
                )
            }
            isCompleted -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.accent
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(y = 4.dp)
                        .border(AppSpacing.borderThin, AppColors.border, AppShapes.medium)
                )
            }
        }
        
        Text(
            text = todo.content,
            style = AppTypography.bodySmall,
            color = if (isCompleted) 
                AppColors.grey 
            else 
                AppColors.white
        )
        
        // Priority badge
        if (todo.priority == "high") {
            Surface(
                shape = AppShapes.medium,
                color = AppColors.error.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "HIGH",
                    style = AppTypography.labelSmall,
                    modifier = Modifier.padding(horizontal = AppSpacing.xs, vertical = 1.dp),
                    color = AppColors.error
                )
            }
        }
    }
}
