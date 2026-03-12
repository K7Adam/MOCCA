package com.mocca.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.RichToolState
import com.mocca.app.domain.model.ToolState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.serialization.json.*

/**
 * Base tool card layout with consistent terminal styling.
 * Now includes live timing display for running tools.
 */
@Composable
internal fun BaseToolCard(
    toolName: String,
    state: ToolState,
    title: String?,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    startTimeMs: Long? = null, // For live timing during running state
    headerExtra: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val backgroundColor = when (state) {
        ToolState.PENDING -> AppColors.surfaceContainerHigh.copy(alpha = 0.8f)
        ToolState.RUNNING -> AppColors.statusWaiting.copy(alpha = 0.15f)
        ToolState.COMPLETED -> AppColors.success.copy(alpha = 0.15f)
        ToolState.ERROR -> AppColors.error.copy(alpha = 0.2f)
    }
    
    val borderColor = when (state) {
        ToolState.PENDING -> AppColors.surfaceContainerHigh.copy(alpha = 0.3f)
        ToolState.RUNNING -> AppColors.statusWaiting.copy(alpha = 0.5f)
        ToolState.COMPLETED -> AppColors.success.copy(alpha = 0.4f)
        ToolState.ERROR -> AppColors.error.copy(alpha = 0.5f)
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs)
            .border(AppSpacing.borderThin, borderColor, AppShapes.medium)
            .clip(AppShapes.medium)
            .clickable { expanded = !expanded }
            .animateContentSize(),
        shape = AppShapes.medium,
        color = backgroundColor
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
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
                                    AppColors.statusWaiting.copy(alpha = alpha),
                                    AppShapes.circle
                                )
                        )
                    }
                }
                
                // Tool name
                Text(
                    text = toolName,
                    style = AppTypography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = iconTint
                )
                
                // Title or summary
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = AppTypography.bodySmall,
                        color = AppColors.grey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Extra header content
                headerExtra()
                
                // State indicator with live timing
                ToolStateIndicator(state, startTimeMs)
                
                // Expand/collapse icon
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.grey
                )
            }
            
            // Expandable content
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = AppSpacing.md)) {
                    HorizontalDivider(
                        color = AppColors.border.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = AppSpacing.md)
                    )
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ToolStateIndicator(state: ToolState, startTimeMs: Long? = null) {
    when (state) {
        ToolState.PENDING -> {
            Text(
                text = "Pending",
                style = AppTypography.labelSmall,
                color = AppColors.grey
            )
        }
        ToolState.RUNNING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                LoadingIndicator(
                    modifier = Modifier.size(12.dp),
                    color = AppColors.statusWaiting
                )
                
                // Live elapsed time display
                if (startTimeMs != null) {
                    var elapsedMs by remember { mutableStateOf(System.currentTimeMillis() - startTimeMs) }
                    
                    LaunchedEffect(startTimeMs) {
                        while (true) {
                            elapsedMs = System.currentTimeMillis() - startTimeMs
                            kotlinx.coroutines.delay(100) // Update every 100ms
                        }
                    }
                    
                    Text(
                        text = formatDuration(elapsedMs),
                        style = AppTypography.labelSmall,
                        color = AppColors.statusWaiting
                    )
                } else {
                    Text(
                        text = "Running",
                        style = AppTypography.labelSmall,
                        color = AppColors.statusWaiting
                    )
                }
            }
        }
        ToolState.COMPLETED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.accentGreen
                )
            }
        }
        ToolState.ERROR -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.error
                )
                Text(
                    text = "Error",
                    style = AppTypography.labelSmall,
                    color = AppColors.error
                )
            }
        }
    }
}

@Composable
internal fun ShowDuration(richState: RichToolState) {
    val duration = when (richState) {
        is RichToolState.Completed -> richState.durationMs
        is RichToolState.Error -> richState.durationMs
        else -> null
    }
    
    if (duration != null && duration > 0) {
        Text(
            text = formatDuration(duration),
            style = AppTypography.labelSmall,
            color = AppColors.grey.copy(alpha = 0.7f)
        )
    }
}

// ===== Utility functions =====

internal fun extractInputField(input: String, field: String): String? {
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

internal fun cleanFileOutput(output: String): String {
    return output
        .replace(Regex("""^<file>\s*\n?"""), "")
        .replace(Regex("""\n?</file>\s*$"""), "")
        .replace(Regex("""^\s*\d{5}\|\s?""", RegexOption.MULTILINE), "")
        .trim()
}

internal fun detectLanguageFromPath(path: String): String {
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

internal fun formatDuration(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> "${ms / 1000.0}s".take(5)
        else -> "${ms / 60000}m ${(ms % 60000) / 1000}s"
    }
}
