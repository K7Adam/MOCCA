package com.mocca.app.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.domain.model.Todo
import com.mocca.app.domain.model.TodoStatus
import androidx.compose.material3.Surface
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppTypography

/**
 * Surface with surfaceContainer background.
 * - Shows progress count when collapsed
 * - Smooth spring animations for expand/collapse
 *
 * @param todos List of todos to display
 * @param isVisible Whether the panel is visible
 * @param modifier Modifier for styling
 */
@Composable
fun TodoListPanel(
    todos: List<Todo>,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val completedCount = todos.count { it.status == TodoStatus.COMPLETED }
    val inProgressCount = todos.count { it.status == TodoStatus.IN_PROGRESS }
    val totalCount = todos.size
    
    // Animated corner radius for smooth expansion
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 16.dp else 12.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cornerRadius"
    )
    
    AnimatedVisibility(
        visible = isVisible && todos.isNotEmpty(),
        enter = expandVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ),
        exit = shrinkVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ),
        modifier = modifier
    ) {
        Surface(
            color = AppColors.surfaceContainer,
            shape = AppShapes.medium
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // ═══════════════ HEADER (Always visible, clickable to expand/collapse) ═══════════════
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isExpanded = !isExpanded }
                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Progress indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        // Completion progress ring
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(AppColors.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Progress text
                            Text(
                                text = "$completedCount",
                                style = AppTypography.labelExtraSmall,
                                color = AppColors.accentGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Status text
                        Text(
                            text = when {
                                inProgressCount > 0 -> "// ACTIVE [$inProgressCount]"
                                completedCount == totalCount && totalCount > 0 -> "// COMPLETE"
                                else -> "// PLAN [$totalCount]"
                            },
                            style = AppTypography.labelExtraSmall,
                            color = when {
                                inProgressCount > 0 -> AppColors.accentGreen
                                completedCount == totalCount && totalCount > 0 -> AppColors.accentGreen
                                else -> AppColors.textTertiary
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Right: Expand/collapse icon
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = AppColors.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // ═══════════════ EXPANDED CONTENT (Todo items) ═══════════════
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ),
                    exit = shrinkVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.sm)
                            .padding(bottom = AppSpacing.sm)
                    ) {
                        // Subtle divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(AppColors.border.copy(alpha = 0.3f))
                        )
                        
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        
                        // Todo items
                        todos.forEach { todo ->
                            TodoItem(todo)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoItem(todo: Todo) {
    // Animated color for status
    val statusColor by animateColorAsState(
        targetValue = when (todo.status) {
            TodoStatus.PENDING -> AppColors.grey
            TodoStatus.IN_PROGRESS -> AppColors.accentGreen
            TodoStatus.COMPLETED -> AppColors.accentGreen
            TodoStatus.CANCELLED -> AppColors.greyDark
        },
        label = "statusColor"
    )
    
    // Animated alpha for completed items
    val textAlpha by animateColorAsState(
        targetValue = if (todo.status == TodoStatus.COMPLETED || todo.status == TodoStatus.CANCELLED) {
            AppColors.grey.copy(alpha = 0.6f)
        } else {
            AppColors.white
        },
        label = "textAlpha"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Status Icon
        val icon = when (todo.status) {
            TodoStatus.PENDING -> Icons.Default.RadioButtonUnchecked
            TodoStatus.IN_PROGRESS -> Icons.Default.PlayArrow
            TodoStatus.COMPLETED -> Icons.Default.CheckCircle
            TodoStatus.CANCELLED -> Icons.Default.Close
        }
        
        Icon(
            imageVector = icon,
            contentDescription = todo.status.name,
            tint = statusColor,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 1.dp)
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.xs))
        
        Text(
            text = todo.content,
            style = AppTypography.labelSmall,
            color = textAlpha,
            maxLines = 2
        )
    }
}
