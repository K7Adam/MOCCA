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
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

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
                .background(AppColors.surface)
                .border(
                    width = AppSpacing.borderThin,
                    color = AppColors.border,
                    shape = RectangleShape
                )
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surfaceVariant)
                    .padding(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "// SESSION_PLAN [${todos.count { it.status == TodoStatus.COMPLETED }}/${todos.size}]",
                    style = AppTypography.labelSmall,
                    color = AppColors.grey
                )
            }
            
            if (todos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO_PLAN_DETECTED",
                        style = AppTypography.bodySmall,
                        color = AppColors.greyDark
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(AppSpacing.sm)
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
            TodoStatus.PENDING -> Icons.Default.Pending to AppColors.grey
            TodoStatus.IN_PROGRESS -> Icons.Default.PlayArrow to AppColors.accentGreen
            TodoStatus.COMPLETED -> Icons.Default.CheckCircle to AppColors.accentGreen
            TodoStatus.CANCELLED -> Icons.Default.Close to AppColors.greyDark
        }
        
        Icon(
            imageVector = icon,
            contentDescription = todo.status.name,
            tint = color,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        
        Text(
            text = todo.content,
            style = AppTypography.bodySmall,
            color = if (todo.status == TodoStatus.COMPLETED || todo.status == TodoStatus.CANCELLED) 
                AppColors.grey else AppColors.white
        )
    }
}
