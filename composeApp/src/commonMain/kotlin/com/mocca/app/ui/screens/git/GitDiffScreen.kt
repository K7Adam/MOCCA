package com.mocca.app.ui.screens.git

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.DiffLineType
import com.mocca.app.domain.model.GitDiff
import com.mocca.app.domain.model.GitDiffFile
import com.mocca.app.ui.components.ErrorScreen
import com.mocca.app.ui.components.LoadingScreen
import com.mocca.app.ui.components.modern.ModernHeader
import com.mocca.app.ui.components.modern.MoccaIconButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Diff viewer screen.
 *
 * W4-T2: DiffChanges bar — per-file proportional addition/deletion bar.
 * W4-T3: Mode toggle   — unified (default) vs split (side-by-side) view.
 */
data class GitDiffScreen(val path: String, val staged: Boolean = false) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<GitDiffScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        // W4-T3: diff view mode state — unified = false, split = true
        var splitMode by remember { mutableStateOf(false) }

        LaunchedEffect(path) {
            screenModel.loadDiff(path, staged)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .padding(AppSpacing.lg)
        ) {
            // ── Header row ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoccaIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { navigator.pop() },
                    iconColor = AppColors.white
                )
                Spacer(modifier = Modifier.width(AppSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    ModernHeader(text = "DIFF: $path")
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        if (staged) {
                            Text("STAGED", color = AppColors.statusOnline, style = AppTypography.labelSmall)
                        } else {
                            Text("WORKING_TREE", color = AppColors.statusWaiting, style = AppTypography.labelSmall)
                        }
                    }
                }

                // W4-T3: mode toggle pill
                DiffModeToggle(
                    splitMode = splitMode,
                    onToggle = { splitMode = !splitMode }
                )
            }

            // W4-T2: total changes summary bar (shown when diff loaded)
            uiState.diff?.let { diff ->
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                DiffSummaryBar(diff = diff)
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(AppSpacing.borderThin, AppColors.borderLight, AppShapes.medium)
            ) {
                when {
                    uiState.isLoading -> LoadingScreen()
                    uiState.error != null -> ErrorScreen(
                        message = uiState.error!!,
                        onRetry = { screenModel.loadDiff(path, staged) }
                    )
                    uiState.diff != null -> DiffContent(diff = uiState.diff!!, splitMode = splitMode)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// W4-T3: Diff mode toggle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiffModeToggle(splitMode: Boolean, onToggle: () -> Unit) {
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
private fun DiffModeTab(label: String, active: Boolean, onClick: () -> Unit) {
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
private fun DiffSummaryBar(diff: GitDiff) {
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
private fun DiffContent(diff: GitDiff, splitMode: Boolean) {
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
private fun FileDiffItem(file: GitDiffFile, splitMode: Boolean) {
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
                    color = AppColors.white,
                    style = AppTypography.labelMedium,
                    modifier = Modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Text(
                    text = "+${file.additions} -${file.deletions}",
                    color = AppColors.grey,
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
                    .background(AppColors.greyDark)
                    .padding(horizontal = AppSpacing.sm, vertical = 2.dp)
            ) {
                Text(
                    text = hunk.header,
                    color = AppColors.greyLight,
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
private fun UnifiedDiffLine(line: com.mocca.app.domain.model.GitDiffLine) {
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
            color = AppColors.grey,
            style = AppTypography.monoLabel,
            modifier = Modifier.width(32.dp).padding(start = 4.dp)
        )
        Text(
            text = "${line.newLineNumber ?: ""}",
            color = AppColors.grey,
            style = AppTypography.monoLabel,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = line.content,
            color = when (line.type) {
                DiffLineType.ADDITION -> AppColors.diffAdditionText
                DiffLineType.DELETION -> AppColors.diffDeletionText
                else -> AppColors.white
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
private fun SplitDiffLines(lines: List<com.mocca.app.domain.model.GitDiffLine>) {
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
private fun SplitDiffCell(
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
            color = AppColors.grey,
            style = AppTypography.monoLabel,
            modifier = Modifier.width(28.dp).padding(start = 4.dp)
        )
        Text(
            text = line?.content ?: "",
            color = when (line?.type) {
                DiffLineType.ADDITION -> AppColors.diffAdditionText
                DiffLineType.DELETION -> AppColors.diffDeletionText
                else -> AppColors.white
            },
            style = AppTypography.codeSmall,
            modifier = Modifier.padding(start = 2.dp),
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}
