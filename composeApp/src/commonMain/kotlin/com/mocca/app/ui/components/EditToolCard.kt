package com.mocca.app.ui.components

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
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
fun EditToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    val filePath = extractInputField(part.input, "filePath") ?: extractInputField(part.input, "file_path")
    val oldString = extractInputField(part.input, "oldString")
    val newString = extractInputField(part.input, "newString")
    
    BaseToolCard(
        toolName = "edit",
        state = part.state,
        title = filePath?.substringAfterLast('/') ?: part.title,
        icon = Icons.Default.Edit,
        iconTint = AppColors.white,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            // File path
            if (!filePath.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.grey
                    )
                    Text(
                        text = filePath,
                        style = AppTypography.bodySmall,
                        color = AppColors.grey,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Diff view
            if (!oldString.isNullOrBlank() || !newString.isNullOrBlank()) {
                DiffView(
                    oldText = oldString ?: "",
                    newText = newString ?: ""
                )
            }
            
            // Error
            part.error?.takeIf { it.isNotBlank() }?.let { error ->
                ErrorBlock(error)
            }
            
            ShowDuration(part.richState)
        }
    }
}
