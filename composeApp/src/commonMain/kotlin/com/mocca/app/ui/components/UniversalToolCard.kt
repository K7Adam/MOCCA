package com.mocca.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.ToolState
import com.mocca.app.domain.model.RichToolState
import com.mocca.app.ui.components.chat.InlineDiffViewer
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.util.DiffParser

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
    
    // Compact command preview pill for shell tools
    if (command != null) {
        CompactCommandPill(command = command)
    }
    
    val output = invocation.output ?: result?.result
    if (!output.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        // Check if output looks like diff, render InlineDiffViewer instead of plain text
        if (DiffParser.looksLikeDiff(output)) {
            InlineDiffViewer(diffText = cleanFileOutput(output))
        } else {
            CodeBlock(
                code = cleanFileOutput(output),
                language = "text",
                label = if (invocation.state == ToolState.ERROR) "ERROR" else "OUTPUT"
            )
        }
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
        // Check if output looks like diff
        if (DiffParser.looksLikeDiff(output)) {
            InlineDiffViewer(diffText = cleanFileOutput(output))
        } else {
            val language = path?.let { detectLanguageFromPath(it) } ?: "text"
            CodeBlock(
                code = cleanFileOutput(output),
                language = language,
                label = "CONTENT"
            )
        }
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

/**
 * Compact command preview pill for shell/bash tool invocations.
 * Displays a single-line terminal icon + truncated command in a pill chip.
 */
@Composable
private fun CompactCommandPill(command: String) {
    val displayCommand = if (command.length > 60) {
        command.take(57) + "..."
    } else {
        command
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.pill)
            .background(AppColors.bgRaised)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = "Terminal",
            tint = AppColors.syntaxFunction,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(AppSpacing.xs))
        Text(
            text = displayCommand,
            style = AppTypography.codeSmall,
            color = AppColors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
