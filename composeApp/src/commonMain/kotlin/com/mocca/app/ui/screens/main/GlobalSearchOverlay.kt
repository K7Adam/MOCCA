package com.mocca.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.MessageRole
import com.mocca.app.domain.model.FileSearchResult
import com.mocca.app.domain.model.FileContentSearchResult
import com.mocca.app.domain.model.SearchMode
import com.mocca.app.domain.model.Session
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.LocalAppPerformance
import com.mocca.app.ui.theme.LocalCodeFontFamily
import com.mocca.app.ui.theme.moccaClickable

internal data class GlobalSearchMessageMatch(
    val messageId: String,
    val sessionId: String,
    val preview: String,
    val role: MessageRole
)

internal fun Session.matchesGlobalSearch(query: String): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return false

    return title.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
        slug.orEmpty().contains(normalizedQuery, ignoreCase = true)
}

internal fun Message.toGlobalSearchMatch(query: String): GlobalSearchMessageMatch? {
    val preview = buildMessageSearchPreview(query) ?: return null
    return GlobalSearchMessageMatch(
        messageId = id,
        sessionId = sessionId,
        preview = preview,
        role = role
    )
}

private fun Message.buildMessageSearchPreview(query: String): String? {
    val searchableText = parts.mapNotNull { part ->
        when (part) {
            is MessagePart.Text -> part.text.trim().takeIf { it.isNotEmpty() }
            is MessagePart.Reasoning -> part.content.trim().takeIf { it.isNotEmpty() }
            is MessagePart.Thinking -> part.content.trim().takeIf { it.isNotEmpty() }
            is MessagePart.ToolInvocation -> part.title?.trim()?.takeIf { it.isNotEmpty() }
                ?: part.name.trim().takeIf { it.isNotEmpty() }
            is MessagePart.ToolResult -> part.result.trim().takeIf { it.isNotEmpty() }
            is MessagePart.File -> part.filename?.trim()?.takeIf { it.isNotEmpty() }
            is MessagePart.SubTask -> null
            // V2 part types — not searchable
            is MessagePart.Snapshot -> null
            is MessagePart.Patch -> part.path.trim().takeIf { it.isNotEmpty() }
            is MessagePart.AgentDelegate -> part.agentName.trim().takeIf { it.isNotEmpty() }
            is MessagePart.Retry -> part.reason?.trim()?.takeIf { it.isNotEmpty() }
            is MessagePart.Compaction -> null
        }
    }.joinToString(separator = " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    if (searchableText.isEmpty() || !searchableText.contains(query, ignoreCase = true)) {
        return null
    }

    val matchIndex = searchableText.indexOf(query, ignoreCase = true).coerceAtLeast(0)
    val startIndex = (matchIndex - 48).coerceAtLeast(0)
    val endIndex = (matchIndex + query.length + 72).coerceAtMost(searchableText.length)
    val prefix = if (startIndex > 0) "…" else ""
    val suffix = if (endIndex < searchableText.length) "…" else ""

    return prefix + searchableText.substring(startIndex, endIndex).trim() + suffix
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GlobalSearchOverlay(
    query: String,
    sessionResults: List<Session>,
    messageResults: List<GlobalSearchMessageMatch>,
    fileSearchMode: SearchMode,
    fileResults: List<FileSearchResult>,
    fileContentResults: List<FileContentSearchResult>,
    isFileSearchLoading: Boolean,
    fileSearchError: String?,
    currentSessionTitle: String?,
    onQueryChange: (String) -> Unit,
    onFileSearchModeChange: (SearchMode) -> Unit,
    onDismiss: () -> Unit,
    onSessionClick: (Session) -> Unit,
    onMessageClick: (GlobalSearchMessageMatch) -> Unit,
    onFileClick: (String) -> Unit
) {
    val performance = LocalAppPerformance.current
    val listState = rememberLazyListState(
        cacheWindow = LazyLayoutCacheWindow(
            ahead = performance.lazyListCacheAhead,
            behind = performance.lazyListCacheBehind
        )
    )

    val focusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        top = AppSpacing.topBarHeight + AppSpacing.sm,
                        start = AppSpacing.screenPaddingHorizontal,
                        end = AppSpacing.screenPaddingHorizontal,
                        bottom = AppSpacing.screenPaddingBottom
                    )
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clip(AppShapes.dialog)
                    .background(AppColors.bgOverlay)
                    .border(
                        width = AppSpacing.borderThin,
                        color = AppColors.outline.copy(alpha = 0.35f),
                        shape = AppShapes.dialog
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(AppSpacing.iconSizeMedium)
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = AppTypography.bodyMedium.copy(color = AppColors.onSurface),
                        cursorBrush = SolidColor(AppColors.primary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search sessions, current chat, and files…",
                                        style = AppTypography.bodyMedium,
                                        color = AppColors.textPlaceholder
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .size(AppSpacing.iconButtonSize)
                            .clip(AppShapes.circle)
                            .moccaClickable(onClick = onDismiss, pressedScale = 0.96f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close search",
                            tint = AppColors.onSurfaceVariant,
                            modifier = Modifier.size(AppSpacing.iconSizeMedium)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = AppSpacing.borderThin,
                    color = AppColors.outline.copy(alpha = 0.3f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    FileSearchModeChip(
                        label = "GLOB",
                        selected = fileSearchMode == SearchMode.FILE_PATTERN,
                        onClick = { onFileSearchModeChange(SearchMode.FILE_PATTERN) }
                    )
                    FileSearchModeChip(
                        label = "GREP",
                        selected = fileSearchMode == SearchMode.TEXT_CONTENT,
                        onClick = { onFileSearchModeChange(SearchMode.TEXT_CONTENT) }
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = AppSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    if (query.isBlank()) {
                        item {
                            SearchGuidanceCard(
                                currentSessionTitle = currentSessionTitle,
                                modifier = Modifier.padding(
                                    horizontal = AppSpacing.sm,
                                    vertical = AppSpacing.md
                                )
                            )
                        }
                    } else {
                        if (sessionResults.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "SESSIONS",
                                    subtitle = "Local session titles",
                                    count = sessionResults.size
                                )
                            }
                            items(sessionResults, key = { it.id }) { session ->
                                SearchResultRow(
                                    icon = Icons.Default.Topic,
                                    accentColor = AppColors.primary,
                                    title = session.title?.takeIf { it.isNotBlank() } ?: "Untitled",
                                    subtitle = session.slug?.takeIf { it.isNotBlank() } ?: session.directory,
                                    meta = session.status.name,
                                    onClick = { onSessionClick(session) }
                                )
                            }
                        }

                        if (messageResults.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "MESSAGES",
                                    subtitle = currentSessionTitle?.let { "Current chat: $it" } ?: "Current chat only",
                                    count = messageResults.size
                                )
                            }
                            items(messageResults, key = { it.messageId }) { result ->
                                SearchResultRow(
                                    icon = Icons.Default.ChatBubbleOutline,
                                    accentColor = AppColors.warning,
                                    title = result.role.name,
                                    subtitle = result.preview,
                                    meta = null,
                                    onClick = { onMessageClick(result) }
                                )
                            }
                        }

                        item {
                            SearchSectionHeader(
                                title = "FILES",
                                subtitle = if (fileSearchMode == SearchMode.FILE_PATTERN) {
                                    "Glob-style file path search via /find/file"
                                } else {
                                    "Grep-style file content search via /find"
                                },
                                count = if (fileSearchMode == SearchMode.FILE_PATTERN) fileResults.size else fileContentResults.size
                            )
                        }

                        if (isFileSearchLoading) {
                            item {
                                SearchStatusRow(
                                    icon = Icons.Default.Schedule,
                                    text = if (fileSearchMode == SearchMode.FILE_PATTERN) {
                                        "Searching file paths…"
                                    } else {
                                        "Searching file contents…"
                                    }
                                ) {
                                    LoadingIndicator(
                                        color = AppColors.primary,
                                        modifier = Modifier.size(AppSpacing.iconSizeMedium)
                                    )
                                }
                            }
                        }

                        if (fileSearchError != null) {
                            item {
                                SearchStatusRow(
                                    icon = Icons.Default.FolderOpen,
                                    text = fileSearchError
                                )
                            }
                        }

                        if (fileSearchMode == SearchMode.FILE_PATTERN) {
                            items(fileResults, key = { it.path }) { result ->
                                SearchResultRow(
                                    icon = Icons.Default.FolderOpen,
                                    accentColor = AppColors.accent,
                                    title = result.name?.ifBlank { result.path.substringAfterLast('/') }
                                        ?: result.path.substringAfterLast('/').ifBlank { result.path },
                                    subtitle = result.path,
                                    meta = "Path",
                                    onClick = { onFileClick(result.path) }
                                )
                            }
                        } else {
                            items(fileContentResults, key = { "${it.path}:${it.line}:${it.content}" }) { result ->
                                ContentSearchResultRow(
                                    result = result,
                                    onClick = { onFileClick(result.path) }
                                )
                            }
                        }

                        if (
                            sessionResults.isEmpty() &&
                            messageResults.isEmpty() &&
                            fileResults.isEmpty() &&
                            fileContentResults.isEmpty() &&
                            !isFileSearchLoading &&
                            fileSearchError == null
                        ) {
                            item {
                                SearchEmptyState(
                                    query = query,
                                    currentSessionTitle = currentSessionTitle
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = AppSpacing.borderThin,
                    color = AppColors.outline.copy(alpha = 0.2f)
                )
                Text(
                    text = "Sessions use local state. Message matches come from the current chat cache. File search uses the existing server /find and /find/file endpoints.",
                    style = AppTypography.labelSmall,
                    color = AppColors.fgMuted,
                    modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun FileSearchModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(AppShapes.pill)
            .background(if (selected) AppColors.surfaceContainerHigh else AppColors.surfaceContainer)
            .moccaClickable(onClick = onClick, pressedScale = 0.98f)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppTypography.labelSmall,
            color = if (selected) AppColors.onSurface else AppColors.fgMuted,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SearchGuidanceCard(
    currentSessionTitle: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.extraLarge)
            .background(AppColors.bgRaised)
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Text(
            text = "GLOBAL SEARCH",
            style = AppTypography.labelLarge,
            color = AppColors.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Search session titles, preview matches from ${currentSessionTitle ?: "the current chat"}, and switch between glob path search or grep content search backed by the existing server endpoints.",
            style = AppTypography.bodySmall,
            color = AppColors.fgMuted
        )
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    subtitle: String,
    count: Int
) {
    Column(
        modifier = Modifier.padding(
            start = AppSpacing.sm,
            end = AppSpacing.sm,
            top = AppSpacing.md,
            bottom = AppSpacing.xxs
        )
    ) {
        Text(
            text = "$title · $count",
            style = AppTypography.labelSmall,
            color = AppColors.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = AppTypography.labelSmall,
            color = AppColors.fgSubtle
        )
    }
}

@Composable
private fun SearchResultRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String?,
    meta: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.surfaceContainer)
            .moccaClickable(onClick = onClick, pressedScale = 0.98f)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(AppSpacing.iconButtonSize)
                .clip(AppShapes.medium)
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(AppSpacing.iconSizeMedium)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.bodyMedium,
                color = AppColors.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = AppTypography.bodySmall,
                    color = AppColors.fgMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!meta.isNullOrBlank()) {
            Text(
                text = meta,
                style = AppTypography.labelSmall,
                color = AppColors.fgSubtle,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchStatusRow(
    icon: ImageVector,
    text: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.fgMuted,
            modifier = Modifier.size(AppSpacing.iconSizeMedium)
        )
        Text(
            text = text,
            style = AppTypography.bodySmall,
            color = AppColors.fgMuted,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun ContentSearchResultRow(
    result: FileContentSearchResult,
    onClick: () -> Unit
) {
    val codeFont = LocalCodeFontFamily.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.surfaceContainer)
            .moccaClickable(onClick = onClick, pressedScale = 0.98f)
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(AppSpacing.iconButtonSize)
                    .clip(AppShapes.medium)
                    .background(AppColors.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = AppColors.accent,
                    modifier = Modifier.size(AppSpacing.iconSizeMedium)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.path.substringAfterLast('/').ifBlank { result.path },
                    style = AppTypography.bodyMedium,
                    color = AppColors.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.path,
                    style = AppTypography.bodySmall,
                    color = AppColors.fgMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Line ${result.line}",
                style = AppTypography.labelSmall,
                color = AppColors.fgSubtle,
                maxLines = 1
            )
        }

        SearchContextPreview(
            result = result,
            codeStyle = TextStyle(
                fontFamily = codeFont,
                fontSize = 12.sp,
                color = AppColors.onSurface
            )
        )
    }
}

@Composable
private fun SearchContextPreview(
    result: FileContentSearchResult,
    codeStyle: TextStyle
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.bgOverlay)
            .padding(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
    ) {
        result.contextBefore.forEach { line ->
            SearchContextLine(
                lineNumber = line.lineNumber,
                content = line.content,
                codeStyle = codeStyle,
                color = AppColors.fgMuted
            )
        }

        SearchContextLine(
            lineNumber = result.line,
            content = result.content,
            codeStyle = codeStyle,
            color = AppColors.onSurface,
            highlight = true
        )

        result.contextAfter.forEach { line ->
            SearchContextLine(
                lineNumber = line.lineNumber,
                content = line.content,
                codeStyle = codeStyle,
                color = AppColors.fgMuted
            )
        }
    }
}

@Composable
private fun SearchContextLine(
    lineNumber: Int,
    content: String,
    codeStyle: TextStyle,
    color: Color,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.small)
            .background(if (highlight) AppColors.surfaceContainerHigh else Color.Transparent)
            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Text(
            text = lineNumber.toString(),
            style = codeStyle,
            color = AppColors.fgSubtle
        )
        Text(
            text = content.ifEmpty { " " },
            style = codeStyle,
            color = color,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SearchEmptyState(
    query: String,
    currentSessionTitle: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Text(
            text = "No results for \"$query\"",
            style = AppTypography.bodyMedium,
            color = AppColors.onSurface,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Sessions search local titles, message matches come from ${currentSessionTitle ?: "the current chat"}, and files use the server-backed glob and grep search endpoints.",
            style = AppTypography.bodySmall,
            color = AppColors.fgMuted
        )
    }
}
