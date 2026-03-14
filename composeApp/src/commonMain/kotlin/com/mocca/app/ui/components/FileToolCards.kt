package com.mocca.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
fun ReadToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    val filePath = extractInputField(part.input, "filePath") ?: extractInputField(part.input, "file_path")
    val limit = extractInputField(part.input, "limit")
    val offset = extractInputField(part.input, "offset")
    
    BaseToolCard(
        toolName = "read",
        state = part.state,
        title = filePath?.substringAfterLast('/') ?: part.title,
        icon = Icons.Default.Visibility,
        iconTint = AppColors.accentGreen,
        modifier = modifier,
        headerExtra = {
            // Show line range if specified
            if (limit != null || offset != null) {
                Surface(
                    shape = AppShapes.medium,
                    color = AppColors.surfaceVariant
                ) {
                    Text(
                        text = buildString {
                            if (offset != null) append("from $offset")
                            if (limit != null) {
                                if (offset != null) append(", ")
                                append("$limit lines")
                            }
                        },
                        style = AppTypography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = AppColors.textSecondary
                    )
                }
            }
        }
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
                        tint = AppColors.textSecondary
                    )
                    Text(
                        text = filePath,
                        style = AppTypography.bodySmall,
                        color = AppColors.textSecondary
                    )
                }
            }
            
            // File content preview
            part.output?.takeIf { it.isNotBlank() }?.let { output ->
                val cleanedOutput = cleanFileOutput(output)
                CodeBlock(
                    code = cleanedOutput.take(3000),
                    label = "Content",
                    language = detectLanguageFromPath(filePath ?: ""),
                    maxLines = 30
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
fun GlobToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    val pattern = extractInputField(part.input, "pattern")
    val path = extractInputField(part.input, "path")
    
    BaseToolCard(
        toolName = "glob",
        state = part.state,
        title = pattern ?: part.title,
        icon = Icons.Default.Search,
        iconTint = AppColors.statusWaiting,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            // Pattern
            if (!pattern.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Text(
                        text = "Pattern:",
                        style = AppTypography.labelSmall,
                        color = AppColors.textSecondary
                    )
                    Text(
                        text = pattern,
                        style = AppTypography.bodySmall,
                        color = AppColors.textPrimary
                    )
                }
            }
            
            // Base path
            if (!path.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Text(
                        text = "In:",
                        style = AppTypography.labelSmall,
                        color = AppColors.textSecondary
                    )
                    Text(
                        text = path,
                        style = AppTypography.bodySmall,
                        color = AppColors.textSecondary
                    )
                }
            }
            
            // Results
            part.output?.takeIf { it.isNotBlank() }?.let { output ->
                FileListView(output)
            }
            
            part.error?.takeIf { it.isNotBlank() }?.let { error ->
                ErrorBlock(error)
            }
            
            ShowDuration(part.richState)
        }
    }
}

@Composable
fun GrepToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    val pattern = extractInputField(part.input, "pattern") ?: extractInputField(part.input, "query")
    val include = extractInputField(part.input, "include")
    val path = extractInputField(part.input, "path")
    
    BaseToolCard(
        toolName = "grep",
        state = part.state,
        title = pattern ?: part.title,
        icon = Icons.Default.FindInPage,
        iconTint = AppColors.textPrimary,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            // Search pattern
            if (!pattern.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Text(
                        text = "Search:",
                        style = AppTypography.labelSmall,
                        color = AppColors.textSecondary
                    )
                    Text(
                        text = pattern,
                        style = AppTypography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.textPrimary
                    )
                }
            }
            
            // Include filter
            if (!include.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Text(
                        text = "Files:",
                        style = AppTypography.labelSmall,
                        color = AppColors.textSecondary
                    )
                    Text(
                        text = include,
                        style = AppTypography.bodySmall,
                        color = AppColors.textSecondary
                    )
                }
            }
            
            // Search results
            part.output?.takeIf { it.isNotBlank() }?.let { output ->
                GrepResultsView(output)
            }
            
            part.error?.takeIf { it.isNotBlank() }?.let { error ->
                ErrorBlock(error)
            }
            
            ShowDuration(part.richState)
        }
    }
}

@Composable
fun ListToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    val path = extractInputField(part.input, "path") ?: "."
    
    BaseToolCard(
        toolName = "list",
        state = part.state,
        title = path,
        icon = Icons.Default.Folder,
        iconTint = AppColors.statusWaiting,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            part.output?.takeIf { it.isNotBlank() }?.let { output ->
                FileListView(output)
            }
            
            part.error?.takeIf { it.isNotBlank() }?.let { error ->
                ErrorBlock(error)
            }
            
            ShowDuration(part.richState)
        }
    }
}

@Composable
fun WriteToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    val filePath = extractInputField(part.input, "filePath") ?: extractInputField(part.input, "file_path")
    val content = extractInputField(part.input, "content")
    
    BaseToolCard(
        toolName = "write",
        state = part.state,
        title = filePath?.substringAfterLast('/') ?: part.title,
        icon = @Suppress("DEPRECATION") Icons.Default.NoteAdd,
        iconTint = AppColors.textPrimary,
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
                        tint = AppColors.textSecondary
                    )
                    Text(
                        text = filePath,
                        style = AppTypography.bodySmall,
                        color = AppColors.textSecondary
                    )
                }
            }
            
            // Content preview
            if (!content.isNullOrBlank()) {
                CodeBlock(
                    code = content.take(2000),
                    label = "Content",
                    language = detectLanguageFromPath(filePath ?: ""),
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
