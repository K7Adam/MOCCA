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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoccaIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { navigator.pop() },
                    contentDescription = "Back",
                    iconColor = AppColors.onSurface
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
                    .border(AppSpacing.borderThin, AppColors.outlineVariant, AppShapes.medium)
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
