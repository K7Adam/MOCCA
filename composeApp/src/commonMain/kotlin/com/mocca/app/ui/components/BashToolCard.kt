package com.mocca.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
fun BashToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    val command = extractInputField(part.input, "command") ?: part.input.take(100)
    val workdir = extractInputField(part.input, "workdir")
    val description = extractInputField(part.input, "description") ?: part.title
    
    BaseToolCard(
        toolName = "bash",
        state = part.state,
        title = description ?: command.take(50),
        icon = Icons.Default.Terminal,
        iconTint = AppColors.statusWaiting,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            // Command
            if (command.isNotBlank()) {
                CodeBlock(
                    code = command,
                    label = "Command",
                    language = "bash"
                )
            }
            
            // Working directory
            if (!workdir.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.grey
                    )
                    Text(
                        text = workdir,
                        style = AppTypography.labelSmall,
                        color = AppColors.grey
                    )
                }
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
            
            // Error
            part.error?.takeIf { it.isNotBlank() }?.let { error ->
                ErrorBlock(error)
            }
            
            // Duration
            ShowDuration(part.richState)
        }
    }
}
