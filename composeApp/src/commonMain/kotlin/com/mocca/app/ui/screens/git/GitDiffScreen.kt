package com.mocca.app.ui.screens.git

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mocca.app.ui.theme.AppShapes
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
import com.mocca.app.ui.components.terminal.TerminalHeader
import com.mocca.app.ui.components.terminal.TerminalIconButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

data class GitDiffScreen(val path: String, val staged: Boolean = false) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<GitDiffScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        // Load diff on entry
        androidx.compose.runtime.LaunchedEffect(path) {
            screenModel.loadDiff(path, staged)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .padding(AppSpacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TerminalIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { navigator.pop() },
                    iconColor = AppColors.white
                )
                Spacer(modifier = Modifier.width(AppSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    TerminalHeader(text = "DIFF_VIEW: $path", showBrackets = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        if (staged) {
                            Text("STAGED", color = AppColors.statusOnline, style = AppTypography.labelSmall)
                        } else {
                            Text("WORKING_TREE", color = AppColors.statusWaiting, style = AppTypography.labelSmall)
                        }
                    }
                }
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
                    uiState.diff != null -> DiffContent(uiState.diff!!)
                }
            }
        }
    }
}

@Composable
private fun DiffContent(diff: GitDiff) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(diff.files) { file ->
            FileDiffItem(file)
        }
    }
}

@Composable
private fun FileDiffItem(file: GitDiffFile) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // File Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
                .padding(AppSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Text(
                text = file.path,
                color = AppColors.white,
                style = AppTypography.labelMedium
            )
            Text(
                text = "+${file.additions} -${file.deletions}",
                color = AppColors.grey,
                style = AppTypography.labelSmall
            )
        }

        // Hunks
        file.hunks.forEach { hunk ->
            // Hunk Header
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

            // Lines
            hunk.lines.forEach { line ->
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
                    // Line Numbers
                    Text(
                        text = "${line.oldLineNumber ?: ""} ",
                        color = AppColors.grey,
                        style = AppTypography.monoLabel,
                        modifier = Modifier.width(32.dp).padding(start = 4.dp)
                    )
                    Text(
                        text = "${line.newLineNumber ?: ""} ",
                        color = AppColors.grey,
                        style = AppTypography.monoLabel,
                        modifier = Modifier.width(32.dp)
                    )

                    // Content
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
        }
    }
}
