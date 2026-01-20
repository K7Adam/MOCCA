package com.mocca.app.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Todo
import com.mocca.app.domain.model.TodoStatus
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

@Composable
fun TodoListPanel(
    todos: List<Todo>,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalColors.surface)
                .border(
                    width = TerminalSpacing.borderThin,
                    color = TerminalColors.border,
                    shape = RectangleShape
                )
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalColors.surfaceVariant)
                    .padding(TerminalSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "// SESSION_PLAN [${todos.count { it.status == TodoStatus.COMPLETED }}/${todos.size}]",
                    style = TerminalTypography.labelSmall,
                    color = TerminalColors.grey
                )
            }
            
            if (todos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TerminalSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO_PLAN_DETECTED",
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.greyDark
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(TerminalSpacing.sm)
                ) {
                    todos.forEach { todo ->
                        TodoItem(todo)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoItem(todo: Todo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Status Icon
        val (icon, color) = when (todo.status) {
            TodoStatus.PENDING -> Icons.Default.Pending to TerminalColors.grey
            TodoStatus.IN_PROGRESS -> Icons.Default.PlayArrow to TerminalColors.statusOnline
            TodoStatus.COMPLETED -> Icons.Default.CheckCircle to TerminalColors.statusOnline
            TodoStatus.CANCELLED -> Icons.Default.Close to TerminalColors.greyDark
        }
        
        Icon(
            imageVector = icon,
            contentDescription = todo.status.name,
            tint = color,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        
        Spacer(modifier = Modifier.width(TerminalSpacing.sm))
        
        Text(
            text = todo.content,
            style = TerminalTypography.bodySmall,
            color = if (todo.status == TodoStatus.COMPLETED || todo.status == TodoStatus.CANCELLED) 
                TerminalColors.grey else TerminalColors.white
        )
    }
}
