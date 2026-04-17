package com.mocca.app.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.LocalCodeFontFamily
import com.mocca.app.util.DiffParser
import com.mocca.app.util.DiffParser.DiffAction
import com.mocca.app.util.DiffParser.DiffChunk
import com.mocca.app.util.DiffParser.DiffLineKind

/**
 * Inline diff viewer that parses unified diff text into expandable per-file cards.
 *
 * Each file section shows a clickable header with path, action badge, and +/- counts.
 * The body contains color-coded diff lines with left-side indicator bars.
 */
@Composable
fun InlineDiffViewer(
    diffText: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    val chunks = remember(diffText) { DiffParser.parseDiffChunks(diffText) }

    if (chunks.isEmpty()) {
        RawDiffFallback(diffText, modifier)
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        chunks.forEach { chunk ->
            DiffFileCard(
                chunk = chunk,
                initiallyExpanded = initiallyExpanded
            )
        }
    }
}

@Composable
private fun RawDiffFallback(diffText: String, modifier: Modifier) {
    val codeFont = LocalCodeFontFamily.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.bgRaised)
            .horizontalScroll(rememberScrollState())
            .padding(AppSpacing.md)
    ) {
        Text(
            text = diffText,
            style = TextStyle(fontFamily = codeFont, fontSize = 12.sp),
            color = AppColors.fgMuted
        )
    }
}

@Composable
private fun DiffFileCard(
    chunk: DiffChunk,
    initiallyExpanded: Boolean
) {
    var expanded by remember(chunk.id) { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.bgRaised)
    ) {
        DiffFileHeader(
            chunk = chunk,
            expanded = expanded,
            onClick = { expanded = !expanded }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            DiffFileBody(chunk = chunk)
        }
    }
}

@Composable
private fun DiffFileHeader(
    chunk: DiffChunk,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = AppColors.fgMuted,
            modifier = Modifier.width(16.dp).height(16.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = chunk.path,
            style = AppTypography.labelMedium,
            color = AppColors.diffFileHeader,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(AppSpacing.sm))

        ActionBadge(action = chunk.action)

        Spacer(modifier = Modifier.width(6.dp))

        if (chunk.additions > 0) {
            Text(
                text = "+${chunk.additions}",
                style = AppTypography.labelSmall,
                color = AppColors.diffAdditionLine
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        if (chunk.deletions > 0) {
            Text(
                text = "-${chunk.deletions}",
                style = AppTypography.labelSmall,
                color = AppColors.diffDeletionLine
            )
        }
    }
}

@Composable
private fun ActionBadge(action: DiffAction) {
    val (label, color) = when (action) {
        DiffAction.ADDED -> "Added" to AppColors.diffAdditionLine
        DiffAction.DELETED -> "Deleted" to AppColors.diffDeletionLine
        DiffAction.RENAMED -> "Renamed" to AppColors.primary
        DiffAction.EDITED -> "Edited" to AppColors.primary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = AppTypography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun DiffFileBody(chunk: DiffChunk) {
    val codeFont = LocalCodeFontFamily.current
    val lines = remember(chunk.diffCode) {
        chunk.diffCode.lines()
            .map { DiffParser.classifyDiffLine(it) to it }
            .filter { (kind, _) -> kind != DiffLineKind.META }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = AppSpacing.md, end = AppSpacing.md, bottom = AppSpacing.sm)
    ) {
        lines.forEach { (kind, line) ->
            DiffLine(
                line = line,
                kind = kind,
                codeFont = codeFont
            )
        }
    }
}

@Composable
private fun DiffLine(
    line: String,
    kind: DiffLineKind,
    codeFont: FontFamily
) {
    val textColor = when (kind) {
        DiffLineKind.ADDITION -> AppColors.diffAdditionLine
        DiffLineKind.DELETION -> AppColors.diffDeletionLine
        DiffLineKind.HUNK -> AppColors.diffHunkHeader
        DiffLineKind.NEUTRAL -> AppColors.fgMuted
        DiffLineKind.META -> AppColors.fgMuted
    }

    val barColor = when (kind) {
        DiffLineKind.ADDITION -> AppColors.diffAdditionLine
        DiffLineKind.DELETION -> AppColors.diffDeletionLine
        else -> null
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        if (barColor != null) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(barColor, RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = line,
            style = TextStyle(fontFamily = codeFont, fontSize = 11.sp, lineHeight = 16.sp),
            color = textColor
        )
    }
}
