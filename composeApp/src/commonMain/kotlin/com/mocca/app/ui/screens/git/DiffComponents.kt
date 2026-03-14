package com.mocca.app.ui.screens.git

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.DiffLineType
import com.mocca.app.domain.model.GitDiff
import com.mocca.app.domain.model.GitDiffFile
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

// ─────────────────────────────────────────────────────────────────────────────
// W4-T3: Diff mode toggle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun DiffModeToggle(splitMode: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(AppShapes.pill)
            .background(AppColors.surface, AppShapes.pill)
            .border(0.5.dp, AppColors.border, AppShapes.pill),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DiffModeTab(label = "UNIFIED", active = !splitMode) {
            if (splitMode) onToggle()
        }
        DiffModeTab(label = "SPLIT", active = splitMode) {
            if (!splitMode) onToggle()
        }
    }
}

@Composable
internal fun DiffModeTab(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(AppShapes.pill)
            .background(
                if (active) AppColors.accentGreen.copy(alpha = 0.2f) else AppColors.background,
                AppShapes.pill
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppTypography.labelSmall,
            color = if (active) AppColors.accentGreen else AppColors.textTertiary,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// W4-T2: Summary bar across all files
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun DiffSummaryBar(diff: GitDiff) {
    val totalAdditions = diff.additions
    val totalDeletions = diff.deletions
    val total = totalAdditions + totalDeletions

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Text(
            text = "+$totalAdditions",
            style = AppTypography.labelSmall,
            color = AppColors.diffAdditionText,
            fontWeight = FontWeight.Bold
        )
        // proportional bar
        if (total > 0) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(AppShapes.pill)
                    .background(AppColors.diffDeletion.copy(alpha = 0.6f), AppShapes.pill)
            ) {
                val addFraction = (totalAdditions.toFloat() / total).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(addFraction)
                        .clip(AppShapes.pill)
                        .background(AppColors.diffAdditionText.copy(alpha = 0.8f), AppShapes.pill)
                )
            }
        }
        Text(
            text = "-$totalDeletions",
            style = AppTypography.labelSmall,
            color = AppColors.diffDeletionText,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${diff.files.size} file${if (diff.files.size != 1) "s" else ""}",
            style = AppTypography.labelSmall,
            color = AppColors.textTertiary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Diff content router (unified vs split)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun DiffContent(diff: GitDiff, splitMode: Boolean) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(diff.files) { file ->
            FileDiffItem(file = file, splitMode = splitMode)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// File-level diff item with W4-T2 changes bar in header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun FileDiffItem(file: GitDiffFile, splitMode: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // ── File header ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
                .padding(AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = file.path,
                    color = AppColors.textPrimary,
                    style = AppTypography.labelMedium,
                    modifier = Modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Text(
                    text = "+${file.additions} -${file.deletions}",
                    color = AppColors.textSecondary,
                    style = AppTypography.labelSmall
                )
            }

            // W4-T2: per-file proportional changes bar
            val total = file.additions + file.deletions
            if (total > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                DiffFileChangesBar(additions = file.additions, deletions = file.deletions)
            }
        }

        // ── Hunks ────────────────────────────────────────────────────────
        file.hunks.forEach { hunk ->
            // Hunk header row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.textSecondaryDark)
                    .padding(horizontal = AppSpacing.sm, vertical = 2.dp)
            ) {
                Text(
                    text = hunk.header,
                    color = AppColors.textSecondaryLight,
                    style = AppTypography.monoLabel
                )
            }

            // Diff lines — unified or split
            if (splitMode) {
                SplitDiffLines(hunk.lines)
            } else {
                hunk.lines.forEach { line ->
                    UnifiedDiffLine(line)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// W4-T2: Per-file changes bar composable (reusable)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DiffFileChangesBar(additions: Int, deletions: Int, modifier: Modifier = Modifier) {
    val total = additions + deletions
    if (total == 0) return

    val addFraction = (additions.toFloat() / total).coerceIn(0f, 1f)
    val maxSquares = 5
    val greenSquares = (addFraction * maxSquares).toInt().coerceAtLeast(if (additions > 0) 1 else 0)
    val redSquares = (maxSquares - greenSquares).coerceAtLeast(if (deletions > 0) 1 else 0)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(greenSquares.coerceAtMost(maxSquares)) {
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 8.dp)
                    .clip(AppShapes.extraSmall)
                    .background(AppColors.diffAdditionText, AppShapes.extraSmall)
            )
        }
        repeat((maxSquares - greenSquares).coerceAtLeast(0)) {
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 8.dp)
                    .clip(AppShapes.extraSmall)
                    .background(AppColors.diffDeletionText, AppShapes.extraSmall)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Unified diff line
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun UnifiedDiffLine(line: com.mocca.app.domain.model.GitDiffLine) {
    val bgColor = when (line.type) {
        DiffLineType.ADDITION -> AppColors.diffAddition
        DiffLineType.DELETION -> AppColors.diffDeletion
        else -> AppColors.background
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
    ) {
        Text(
            text = "${line.oldLineNumber ?: ""}",
            color = AppColors.textSecondary,
            style = AppTypography.monoLabel,
            modifier = Modifier.width(32.dp).padding(start = 4.dp)
        )
        Text(
            text = "${line.newLineNumber ?: ""}",
            color = AppColors.textSecondary,
            style = AppTypography.monoLabel,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = line.content,
            color = when (line.type) {
                DiffLineType.ADDITION -> AppColors.diffAdditionText
                DiffLineType.DELETION -> AppColors.diffDeletionText
                else -> AppColors.textPrimary
            },
            style = AppTypography.codeSmall,
            modifier = Modifier.padding(start = 4.dp),
            softWrap = false
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// W4-T3: Split diff lines (side-by-side)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun SplitDiffLines(lines: List<com.mocca.app.domain.model.GitDiffLine>) {
    // Pair up deletions with additions for side-by-side display
    val deletions = lines.filter { it.type == DiffLineType.DELETION }
    val additions = lines.filter { it.type == DiffLineType.ADDITION }
    val context = lines.filter { it.type == DiffLineType.CONTEXT }

    // Build a merged list for side-by-side rendering
    // Strategy: interleave context lines with paired deletion/addition pairs
    var delIdx = 0
    var addIdx = 0
    val pairs = mutableListOf<Pair<com.mocca.app.domain.model.GitDiffLine?, com.mocca.app.domain.model.GitDiffLine?>>()

    lines.forEach { line ->
        when (line.type) {
            DiffLineType.CONTEXT -> pairs.add(Pair(line, line))
            DiffLineType.DELETION -> {
                val matchingAdd = additions.getOrNull(addIdx)
                if (matchingAdd != null) {
                    pairs.add(Pair(line, matchingAdd))
                    addIdx++
                } else {
                    pairs.add(Pair(line, null))
                }
            }
            DiffLineType.ADDITION -> {
                // Only add if not already consumed by a deletion pairing
                val alreadyPaired = pairs.count { it.second == line } > 0
                if (!alreadyPaired) {
                    pairs.add(Pair(null, line))
                }
            }
            else -> pairs.add(Pair(line, null))
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        pairs.forEach { (left, right) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // Left side (old / deletion)
                SplitDiffCell(
                    line = left,
                    modifier = Modifier.weight(1f),
                    isLeft = true
                )
                // 1dp separator
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(IntrinsicSize.Min)
                        .background(AppColors.border)
                )
                // Right side (new / addition)
                SplitDiffCell(
                    line = right,
                    modifier = Modifier.weight(1f),
                    isLeft = false
                )
            }
        }
    }
}

@Composable
internal fun SplitDiffCell(
    line: com.mocca.app.domain.model.GitDiffLine?,
    modifier: Modifier = Modifier,
    isLeft: Boolean
) {
    val bgColor = when {
        line == null -> AppColors.surface.copy(alpha = 0.3f)
        line.type == DiffLineType.DELETION -> AppColors.diffDeletion
        line.type == DiffLineType.ADDITION -> AppColors.diffAddition
        else -> AppColors.background
    }

    Row(
        modifier = modifier.background(bgColor)
    ) {
        val lineNum = if (isLeft) line?.oldLineNumber else line?.newLineNumber
        Text(
            text = "${lineNum ?: ""}",
            color = AppColors.textSecondary,
            style = AppTypography.monoLabel,
            modifier = Modifier.width(28.dp).padding(start = 4.dp)
        )
        Text(
            text = line?.content ?: "",
            color = when (line?.type) {
                DiffLineType.ADDITION -> AppColors.diffAdditionText
                DiffLineType.DELETION -> AppColors.diffDeletionText
                else -> AppColors.textPrimary
            },
            style = AppTypography.codeSmall,
            modifier = Modifier.padding(start = 2.dp),
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}
