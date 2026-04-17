package com.mocca.app.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.LocalCodeFontFamily
import com.mocca.app.util.DiffParser

@Composable
fun FileChangeBlock(
    diffText: String,
    modifier: Modifier = Modifier
) {
    val chunks = remember(diffText) { DiffParser.parseDiffChunks(diffText) }
    if (chunks.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }

    val totalAdditions = chunks.sumOf { it.additions }
    val totalDeletions = chunks.sumOf { it.deletions }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Summary Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(AppColors.bgRaised)
                .clickable { isExpanded = !isExpanded }
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${chunks.size} files changed",
                color = AppColors.onSurface,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(AppSpacing.md))
            Text(
                text = "+$totalAdditions",
                color = AppColors.diffAdditionLine,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "-$totalDeletions",
                color = AppColors.diffDeletionLine,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = AppColors.onSurfaceVariant
            )
        }

        // Per-file cards
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                chunks.forEach { chunk ->
                    FileDiffCard(chunk = chunk)
                }
            }
        }
    }
}

@Composable
private fun FileDiffCard(chunk: DiffParser.DiffChunk) {
    var isExpanded by remember { mutableStateOf(false) }

    val actionColor = when (chunk.action) {
        DiffParser.DiffAction.EDITED -> AppColors.onSurfaceVariant
        DiffParser.DiffAction.ADDED -> AppColors.accentGreen
        DiffParser.DiffAction.DELETED -> AppColors.error
        DiffParser.DiffAction.RENAMED -> AppColors.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.bgOverlay)
    ) {
        // File Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chunk.action.name,
                color = actionColor,
                style = AppTypography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(60.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = chunk.path,
                color = AppColors.onSurface,
                style = AppTypography.labelMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "+${chunk.additions}",
                color = AppColors.diffAdditionLine,
                style = AppTypography.labelSmall
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Text(
                text = "-${chunk.deletions}",
                color = AppColors.diffDeletionLine,
                style = AppTypography.labelSmall
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = AppColors.onSurfaceVariant
            )
        }

        // Diff Content
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.bgBase)
                    .padding(vertical = AppSpacing.xs)
            ) {
                val lines = chunk.diffCode.lines()
                val scrollState = rememberScrollState()
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                ) {
                    lines.forEach { line ->
                        val kind = DiffParser.classifyDiffLine(line)
                        if (kind != DiffParser.DiffLineKind.META) {
                            DiffLineRow(line = line, kind = kind)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffLineRow(line: String, kind: DiffParser.DiffLineKind) {
    val textColor = when (kind) {
        DiffParser.DiffLineKind.ADDITION -> AppColors.diffAdditionLine
        DiffParser.DiffLineKind.DELETION -> AppColors.diffDeletionLine
        DiffParser.DiffLineKind.HUNK -> AppColors.diffHunkHeader
        else -> AppColors.onSurfaceVariant
    }

    val barColor = when (kind) {
        DiffParser.DiffLineKind.ADDITION -> AppColors.diffAdditionLine
        DiffParser.DiffLineKind.DELETION -> AppColors.diffDeletionLine
        else -> AppColors.bgBase
    }

    val bgColor = when (kind) {
        DiffParser.DiffLineKind.ADDITION -> AppColors.diffAdditionLine.copy(alpha = 0.1f)
        DiffParser.DiffLineKind.DELETION -> AppColors.diffDeletionLine.copy(alpha = 0.1f)
        else -> AppColors.bgBase
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(IntrinsicSize.Min)
                .background(barColor)
        )
        Text(
            text = line,
            color = textColor,
            fontFamily = LocalCodeFontFamily.current,
            style = AppTypography.bodySmall,
            modifier = Modifier.padding(start = AppSpacing.xs, end = AppSpacing.sm, top = 2.dp, bottom = 2.dp),
            softWrap = false
        )
    }
}