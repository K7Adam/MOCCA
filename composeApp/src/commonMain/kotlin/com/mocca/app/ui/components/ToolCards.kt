package com.mocca.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.RichToolState
import com.mocca.app.domain.model.ToolState
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalTypography
import com.mocca.app.ui.theme.TerminalSpacing
import kotlinx.serialization.json.*

/**
 * Rich tool card component that renders tools with appropriate UI based on tool type.
 * Matches OpenChamber's tool rendering patterns.
 * Migrated to TerminalTheme styling.
 */
@Composable
fun RichToolCard(
    part: MessagePart.ToolInvocation,
    modifier: Modifier = Modifier
) {
    val toolName = part.name.lowercase()
    
    when {
        toolName == "bash" || toolName == "shell" -> BashToolCard(part, modifier)
        toolName == "edit" || toolName == "multiedit" -> EditToolCard(part, modifier)
        toolName == "read" -> ReadToolCard(part, modifier)
        toolName == "glob" -> GlobToolCard(part, modifier)
        toolName == "grep" || toolName == "search" -> GrepToolCard(part, modifier)
        toolName == "list" || toolName == "ls" -> ListToolCard(part, modifier)
        toolName == "write" -> WriteToolCard(part, modifier)
        toolName == "todowrite" || toolName == "todo" -> TodoToolCard(part, modifier)
        toolName == "webfetch" || toolName == "web_search" -> WebFetchToolCard(part, modifier)
        toolName == "task" -> TaskToolCard(part, modifier)
        else -> GenericToolCard(part, modifier)
    }
}

/**
 * Base tool card layout with consistent terminal styling.
 */
@Composable
private fun BaseToolCard(
    toolName: String,
    state: ToolState,
    title: String?,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    headerExtra: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val backgroundColor = when (state) {
        ToolState.PENDING -> TerminalColors.surfaceVariant.copy(alpha = 0.3f)
        ToolState.RUNNING -> TerminalColors.statusWaiting.copy(alpha = 0.1f)
        ToolState.COMPLETED -> TerminalColors.statusOnline.copy(alpha = 0.1f)
        ToolState.ERROR -> TerminalColors.error.copy(alpha = 0.2f)
    }
    
    val borderColor = when (state) {
        ToolState.PENDING -> TerminalColors.border.copy(alpha = 0.3f)
        ToolState.RUNNING -> TerminalColors.statusWaiting.copy(alpha = 0.5f)
        ToolState.COMPLETED -> TerminalColors.statusOnline.copy(alpha = 0.3f)
        ToolState.ERROR -> TerminalColors.error.copy(alpha = 0.5f)
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TerminalSpacing.xs)
            .border(TerminalSpacing.borderThin, borderColor, RectangleShape)
            .clip(RectangleShape)
            .clickable { expanded = !expanded }
            .animateContentSize(),
        shape = RectangleShape,
        color = backgroundColor
    ) {
        Column(modifier = Modifier.padding(TerminalSpacing.md)) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
            ) {
                // Tool icon with state indicator
                Box {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = iconTint
                    )
                    if (state == ToolState.RUNNING) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .background(
                                    TerminalColors.statusWaiting.copy(alpha = alpha),
                                    RectangleShape
                                )
                        )
                    }
                }
                
                // Tool name
                Text(
                    text = toolName,
                    style = TerminalTypography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = iconTint
                )
                
                // Title or summary
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.grey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Extra header content
                headerExtra()
                
                // State indicator
                ToolStateIndicator(state)
                
                // Expand/collapse icon
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(18.dp),
                    tint = TerminalColors.grey
                )
            }
            
            // Expandable content
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = TerminalSpacing.md)) {
                    HorizontalDivider(
                        color = TerminalColors.border.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = TerminalSpacing.md)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun ToolStateIndicator(state: ToolState) {
    when (state) {
        ToolState.PENDING -> {
            Text(
                text = "Pending",
                style = TerminalTypography.labelSmall,
                color = TerminalColors.grey
            )
        }
        ToolState.RUNNING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = TerminalColors.statusWaiting
                )
                Text(
                    text = "Running",
                    style = TerminalTypography.labelSmall,
                    color = TerminalColors.statusWaiting
                )
            }
        }
        ToolState.COMPLETED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TerminalColors.statusOnline
                )
            }
        }
        ToolState.ERROR -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TerminalColors.error
                )
                Text(
                    text = "Error",
                    style = TerminalTypography.labelSmall,
                    color = TerminalColors.error
                )
            }
        }
    }
}

// ===== Tool-specific cards =====

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
        iconTint = TerminalColors.statusWaiting,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
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
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TerminalColors.grey
                    )
                    Text(
                        text = workdir,
                        style = TerminalTypography.labelSmall,
                        color = TerminalColors.grey
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
        iconTint = TerminalColors.white,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
            // File path
            if (!filePath.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TerminalColors.grey
                    )
                    Text(
                        text = filePath,
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.grey,
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
        iconTint = TerminalColors.statusOnline,
        modifier = modifier,
        headerExtra = {
            // Show line range if specified
            if (limit != null || offset != null) {
                Surface(
                    shape = RectangleShape,
                    color = TerminalColors.surfaceVariant
                ) {
                    Text(
                        text = buildString {
                            if (offset != null) append("from $offset")
                            if (limit != null) {
                                if (offset != null) append(", ")
                                append("$limit lines")
                            }
                        },
                        style = TerminalTypography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = TerminalColors.grey
                    )
                }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
            // File path
            if (!filePath.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TerminalColors.grey
                    )
                    Text(
                        text = filePath,
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.grey
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
        iconTint = TerminalColors.statusWaiting,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
            // Pattern
            if (!pattern.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                ) {
                    Text(
                        text = "Pattern:",
                        style = TerminalTypography.labelSmall,
                        color = TerminalColors.grey
                    )
                    Text(
                        text = pattern,
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.white
                    )
                }
            }
            
            // Base path
            if (!path.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                ) {
                    Text(
                        text = "In:",
                        style = TerminalTypography.labelSmall,
                        color = TerminalColors.grey
                    )
                    Text(
                        text = path,
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.grey
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
        iconTint = TerminalColors.white,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
            // Search pattern
            if (!pattern.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                ) {
                    Text(
                        text = "Search:",
                        style = TerminalTypography.labelSmall,
                        color = TerminalColors.grey
                    )
                    Text(
                        text = pattern,
                        style = TerminalTypography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TerminalColors.white
                    )
                }
            }
            
            // Include filter
            if (!include.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                ) {
                    Text(
                        text = "Files:",
                        style = TerminalTypography.labelSmall,
                        color = TerminalColors.grey
                    )
                    Text(
                        text = include,
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.grey
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
        iconTint = TerminalColors.statusWaiting,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
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
        iconTint = TerminalColors.white,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
            // File path
            if (!filePath.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TerminalColors.grey
                    )
                    Text(
                        text = filePath,
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.grey
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

@Composable
fun TodoToolCard(part: MessagePart.ToolInvocation, modifier: Modifier = Modifier) {
    BaseToolCard(
        toolName = "todo",
        state = part.state,
        title = part.title ?: "Task List",
        icon = Icons.Default.Checklist,
        iconTint = TerminalColors.statusOnline,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
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
        iconTint = TerminalColors.statusWaiting,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
            // URL
            if (!url.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TerminalColors.white
                    )
                    Text(
                        text = url,
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.white,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Format
            if (!format.isNullOrBlank()) {
                Text(
                    text = "Format: $format",
                    style = TerminalTypography.labelSmall,
                    color = TerminalColors.grey
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
        iconTint = TerminalColors.statusOnline,
        modifier = modifier,
        headerExtra = {
            if (!subagentType.isNullOrBlank()) {
                Surface(
                    shape = RectangleShape,
                    color = TerminalColors.surfaceVariant
                ) {
                    Text(
                        text = subagentType,
                        style = TerminalTypography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = TerminalColors.white
                    )
                }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
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
            ToolState.PENDING -> TerminalColors.grey
            ToolState.RUNNING -> TerminalColors.statusWaiting
            ToolState.COMPLETED -> TerminalColors.statusOnline
            ToolState.ERROR -> TerminalColors.error
        },
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
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

// ===== Helper composables =====

@Composable
private fun CodeBlock(
    code: String,
    label: String? = null,
    language: String = "text",
    maxLines: Int = 50
) {
    Column {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = TerminalTypography.labelSmall,
                color = TerminalColors.grey,
                modifier = Modifier.padding(bottom = TerminalSpacing.xs)
            )
        }
        
        Surface(
            shape = RectangleShape,
            color = TerminalColors.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            val scrollState = rememberScrollState()
            Text(
                text = code.lines().take(maxLines).joinToString("\n"),
                style = TerminalTypography.code,
                color = TerminalColors.white,
                modifier = Modifier
                    .padding(TerminalSpacing.md)
                    .horizontalScroll(scrollState)
            )
        }
    }
}

@Composable
private fun ErrorBlock(error: String) {
    Surface(
        shape = RectangleShape,
        color = TerminalColors.error.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(TerminalSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = TerminalColors.error
            )
            Text(
                text = error,
                style = TerminalTypography.bodySmall,
                color = TerminalColors.error
            )
        }
    }
}

@Composable
private fun ShowDuration(richState: RichToolState) {
    val duration = when (richState) {
        is RichToolState.Completed -> richState.durationMs
        is RichToolState.Error -> richState.durationMs
        else -> null
    }
    
    if (duration != null && duration > 0) {
        Text(
            text = formatDuration(duration),
            style = TerminalTypography.labelSmall,
            color = TerminalColors.grey.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun DiffView(oldText: String, newText: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
    ) {
        // Removed lines
        if (oldText.isNotBlank()) {
            Surface(
                shape = RectangleShape,
                color = TerminalColors.diffDeletion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(TerminalSpacing.sm)) {
                    Text(
                        text = "-",
                        style = TerminalTypography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TerminalColors.diffDeletionText,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        text = oldText.lines().take(20).joinToString("\n"),
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.diffDeletionText.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        // Added lines
        if (newText.isNotBlank()) {
            Surface(
                shape = RectangleShape,
                color = TerminalColors.diffAddition,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(TerminalSpacing.sm)) {
                    Text(
                        text = "+",
                        style = TerminalTypography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TerminalColors.diffAdditionText,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        text = newText.lines().take(20).joinToString("\n"),
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.diffAdditionText.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FileListView(output: String) {
    val files = output.trim().lines().filter { it.isNotBlank() }.take(50)
    val fileCount = output.trim().lines().count { it.isNotBlank() }
    
    Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)) {
        Text(
            text = "Found $fileCount file${if (fileCount != 1) "s" else ""}",
            style = TerminalTypography.labelSmall,
            color = TerminalColors.grey
        )
        
        Surface(
            shape = RectangleShape,
            color = TerminalColors.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(TerminalSpacing.sm)
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                files.forEach { file ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs),
                        modifier = Modifier.padding(vertical = TerminalSpacing.xxs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    TerminalColors.white.copy(alpha = 0.6f),
                                    RectangleShape
                                )
                        )
                        Text(
                            text = file.trim(),
                            style = TerminalTypography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TerminalColors.white
                        )
                    }
                }
                if (fileCount > 50) {
                    Text(
                        text = "... and ${fileCount - 50} more",
                        style = TerminalTypography.labelSmall,
                        color = TerminalColors.grey
                    )
                }
            }
        }
    }
}

@Composable
private fun GrepResultsView(output: String) {
    val lines = output.trim().lines().filter { it.isNotBlank() }.take(30)
    val totalMatches = output.trim().lines().count { it.isNotBlank() }
    
    Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)) {
        Text(
            text = "Found $totalMatches match${if (totalMatches != 1) "es" else ""}",
            style = TerminalTypography.labelSmall,
            color = TerminalColors.grey
        )
        
        Surface(
            shape = RectangleShape,
            color = TerminalColors.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(TerminalSpacing.sm)
                    .heightIn(max = 250.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                lines.forEach { line ->
                    // Parse grep format: file:line:content
                    val parts = line.split(":", limit = 3)
                    Row(
                        modifier = Modifier.padding(vertical = TerminalSpacing.xxs),
                        horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                    ) {
                        if (parts.size >= 2) {
                            Text(
                                text = parts[0].substringAfterLast('/'),
                                style = TerminalTypography.labelSmall,
                                color = TerminalColors.white
                            )
                            if (parts.size >= 3) {
                                Text(
                                    text = ":${parts[1]}",
                                    style = TerminalTypography.labelSmall,
                                    color = TerminalColors.grey
                                )
                            }
                        }
                    }
                    Text(
                        text = if (parts.size >= 3) parts[2] else line,
                        style = TerminalTypography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = TerminalSpacing.sm),
                        color = TerminalColors.whiteDim
                    )
                }
            }
        }
    }
}

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
        
        Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
            // Summary
            Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)) {
                Text(
                    text = "Total: ${todos.size}",
                    style = TerminalTypography.labelSmall,
                    color = TerminalColors.grey
                )
                if (inProgress.isNotEmpty()) {
                    Text(
                        text = "In Progress: ${inProgress.size}",
                        style = TerminalTypography.labelSmall,
                        color = TerminalColors.statusWaiting
                    )
                }
                if (completed.isNotEmpty()) {
                    Text(
                        text = "Done: ${completed.size}",
                        style = TerminalTypography.labelSmall,
                        color = TerminalColors.statusOnline
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
                        style = TerminalTypography.labelSmall,
                        color = TerminalColors.grey
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
            style = TerminalTypography.bodySmall,
            color = TerminalColors.white
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
        modifier = Modifier.padding(vertical = TerminalSpacing.xxs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
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
                            TerminalColors.statusWaiting.copy(alpha = alpha),
                            RectangleShape
                        )
                )
            }
            isCompleted -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TerminalColors.statusOnline
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(y = 4.dp)
                        .border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape)
                )
            }
        }
        
        Text(
            text = todo.content,
            style = TerminalTypography.bodySmall,
            color = if (isCompleted) 
                TerminalColors.grey 
            else 
                TerminalColors.white
        )
        
        // Priority badge
        if (todo.priority == "high") {
            Surface(
                shape = RectangleShape,
                color = TerminalColors.error.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "HIGH",
                    style = TerminalTypography.labelSmall,
                    modifier = Modifier.padding(horizontal = TerminalSpacing.xs, vertical = 1.dp),
                    color = TerminalColors.error
                )
            }
        }
    }
}

// ===== Utility functions =====

private fun extractInputField(input: String, field: String): String? {
    return try {
        val json = Json.parseToJsonElement(input)
        if (json is JsonObject) {
            json[field]?.let { element ->
                when (element) {
                    is JsonPrimitive -> element.contentOrNull
                    else -> element.toString()
                }
            }
        } else null
    } catch (e: Exception) {
        // Try regex fallback for non-JSON input
        val regex = Regex(""""$field"\s*:\s*"([^"]+)"""")
        regex.find(input)?.groupValues?.getOrNull(1)
    }
}

private fun cleanFileOutput(output: String): String {
    return output
        .replace(Regex("""^<file>\s*\n?"""), "")
        .replace(Regex("""\n?</file>\s*$"""), "")
        .replace(Regex("""^\s*\d{5}\|\s?""", RegexOption.MULTILINE), "")
        .trim()
}

private fun detectLanguageFromPath(path: String): String {
    val extension = path.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "kt", "kts" -> "kotlin"
        "java" -> "java"
        "js", "mjs" -> "javascript"
        "ts", "mts" -> "typescript"
        "tsx" -> "tsx"
        "jsx" -> "jsx"
        "py" -> "python"
        "rb" -> "ruby"
        "go" -> "go"
        "rs" -> "rust"
        "swift" -> "swift"
        "c", "h" -> "c"
        "cpp", "cc", "cxx", "hpp" -> "cpp"
        "cs" -> "csharp"
        "json" -> "json"
        "xml" -> "xml"
        "html", "htm" -> "html"
        "css" -> "css"
        "scss", "sass" -> "scss"
        "yaml", "yml" -> "yaml"
        "toml" -> "toml"
        "md", "markdown" -> "markdown"
        "sh", "bash", "zsh" -> "bash"
        "sql" -> "sql"
        "gradle" -> "groovy"
        else -> "text"
    }
}

private fun formatDuration(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> "${ms / 1000.0}s".take(5)
        else -> "${ms / 60000}m ${(ms % 60000) / 1000}s"
    }
}
