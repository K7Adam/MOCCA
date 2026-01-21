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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
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
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing

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
                .background(TerminalColors.background)
                .padding(TerminalSpacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TerminalIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { navigator.pop() },
                    iconColor = TerminalColors.white
                )
                Spacer(modifier = Modifier.width(TerminalSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    TerminalHeader(text = "DIFF_VIEW: $path", showBrackets = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
                        if (staged) {
                            Text("STAGED", color = TerminalColors.statusOnline, style = MaterialTheme.typography.labelSmall)
                        } else {
                            Text("WORKING_TREE", color = TerminalColors.statusWaiting, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(TerminalSpacing.md))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(TerminalSpacing.borderThin, TerminalColors.borderLight, RectangleShape)
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
                .background(TerminalColors.surface)
                .padding(TerminalSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = file.path,
                color = TerminalColors.white,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                text = "+${file.additions} -${file.deletions}",
                color = TerminalColors.grey,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Hunks
        file.hunks.forEach { hunk ->
            // Hunk Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalColors.greyDark)
                    .padding(horizontal = TerminalSpacing.sm, vertical = 2.dp)
            ) {
                Text(
                    text = hunk.header,
                    color = TerminalColors.greyLight,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Lines
            hunk.lines.forEach { line ->
                val bgColor = when (line.type) {
                    DiffLineType.ADDITION -> Color(0xFF1E3A2F) // Dark Green
                    DiffLineType.DELETION -> Color(0xFF3A1E1E) // Dark Red
                    else -> Color.Transparent
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                ) {
                    // Line Numbers
                    Text(
                        text = "${line.oldLineNumber ?: ""} ",
                        color = TerminalColors.grey,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(32.dp).padding(start = 4.dp),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${line.newLineNumber ?: ""} ",
                        color = TerminalColors.grey,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(32.dp),
                        fontSize = 10.sp
                    )
                    
                    // Content
                    Text(
                        text = line.content,
                        color = TerminalColors.white,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 4.dp),
                        softWrap = false
                    )
                }
            }
        }
    }
}
