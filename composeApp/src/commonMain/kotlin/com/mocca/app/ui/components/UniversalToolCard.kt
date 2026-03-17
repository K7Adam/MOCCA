package com.mocca.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.ToolState
import com.mocca.app.domain.model.RichToolState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Universal Tool Card that handles all Mocca tool types.
 * Replaces specialized cards (Bash, Edit, File, Task).
 */
@Composable
fun UniversalToolCard(
    invocation: MessagePart.ToolInvocation,
    result: MessagePart.ToolResult?,
    modifier: Modifier = Modifier
) {
    val state = invocation.state
    val richState = invocation.richState
    
    val startTimeMs = when (richState) {
        is RichToolState.Running -> richState.startTime
        is RichToolState.Completed -> richState.startTime
        is RichToolState.Error -> richState.startTime
        else -> null
    }

    val (icon, iconTint) = getToolVisuals(invocation.name)
    val title = invocation.title ?: getToolTitle(invocation)

    BaseToolCard(
        toolName = invocation.name.uppercase(),
        state = state,
        title = title,
        icon = icon,
        iconTint = iconTint,
        startTimeMs = startTimeMs,
        modifier = modifier
    ) {
        // Tool Content Router
        when (invocation.name) {
            "bash", "shell", "terminal" -> BashToolContent(invocation, result)
            "edit", "write_file", "replace", "write", "todowrite" -> EditToolContent(invocation, result)
            "ls", "list_directory", "read_file", "read", "glob", "grep_search", "grep", "search" -> FileToolContent(invocation, result)
            "task", "ticket", "prd" -> TaskToolContent(invocation, result)
            else -> DefaultToolContent(invocation, result)
        }
    }
}

@Composable
private fun BashToolContent(invocation: MessagePart.ToolInvocation, result: MessagePart.ToolResult?) {
    val command = extractInputField(invocation.input, "command")
    if (command != null) {
        CodeBlock(code = command, language = "bash", label = "COMMAND")
    }
    
    val output = invocation.output ?: result?.result
    if (!output.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        CodeBlock(
            code = cleanFileOutput(output),
            language = "text",
            label = if (invocation.state == ToolState.ERROR) "ERROR" else "OUTPUT"
        )
    }
}

@Composable
private fun EditToolContent(invocation: MessagePart.ToolInvocation, result: MessagePart.ToolResult?) {
    val path = extractInputField(invocation.input, "file_path") ?: extractInputField(invocation.input, "path")
    if (path != null) {
        FileHeader(path = path)
    }
    
    val instruction = extractInputField(invocation.input, "instruction")
    if (instruction != null) {
        Text(
            text = instruction,
            style = AppTypography.bodySmall,
            color = AppColors.onSurfaceVariant,
            modifier = Modifier.padding(vertical = AppSpacing.xs)
        )
    }
}

@Composable
private fun FileToolContent(invocation: MessagePart.ToolInvocation, result: MessagePart.ToolResult?) {
    val path = extractInputField(invocation.input, "path") ?: extractInputField(invocation.input, "dir_path")
    if (path != null) {
        FileHeader(path = path)
    }
    
    val output = invocation.output ?: result?.result
    if (!output.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        val language = path?.let { detectLanguageFromPath(it) } ?: "text"
        CodeBlock(
            code = cleanFileOutput(output),
            language = language,
            label = "CONTENT"
        )
    }
}

@Composable
private fun TaskToolContent(invocation: MessagePart.ToolInvocation, result: MessagePart.ToolResult?) {
    val objective = extractInputField(invocation.input, "objective") ?: extractInputField(invocation.input, "task")
    if (objective != null) {
        Text(
            text = objective,
            style = AppTypography.bodyMedium,
            color = AppColors.onSurface
        )
    }
}

@Composable
private fun DefaultToolContent(invocation: MessagePart.ToolInvocation, result: MessagePart.ToolResult?) {
    CodeBlock(code = invocation.input, language = "json", label = "INPUT")
    val output = invocation.output ?: result?.result
    if (!output.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        CodeBlock(code = output, language = "text", label = "RESULT")
    }
}

@Composable
private fun getToolVisuals(toolName: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    return when (toolName) {
        "bash", "shell", "terminal" -> Icons.Default.Terminal to AppColors.syntaxFunction
        "edit", "write_file", "replace", "write", "todowrite" -> Icons.Default.Edit to AppColors.syntaxString
        "ls", "list_directory", "read_file", "read", "glob", "grep_search", "grep", "search" -> Icons.Default.Folder to AppColors.accent
        "task", "ticket", "prd" -> Icons.AutoMirrored.Filled.Notes to AppColors.statusWaiting
        else -> Icons.Default.Build to AppColors.onSurfaceVariant
    }
}

private fun getToolTitle(invocation: MessagePart.ToolInvocation): String? {
    return extractInputField(invocation.input, "command")
        ?: extractInputField(invocation.input, "path")
        ?: extractInputField(invocation.input, "file_path")
        ?: extractInputField(invocation.input, "dir_path")
        ?: extractInputField(invocation.input, "objective")
}
