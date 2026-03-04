package com.mocca.app.ui.screens.worktree

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.WorktreeInfo
import com.mocca.app.domain.model.WorktreeStatus
import com.mocca.app.ui.components.LoadingScreen
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaIconButton
import com.mocca.app.ui.components.modern.ModernHeader
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * W5-T1: Worktree management screen.
 *
 * Shows all Git worktrees for the current project with create / delete / reset actions.
 * Uses OpenCode's experimental /worktree endpoints.
 */
object WorktreeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<WorktreeScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .padding(AppSpacing.lg)
        ) {
            // ── Header ────────────────────────────────────────────────────────────
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
                    ModernHeader(text = "WORKTREES")
                    Text(
                        text = "Experimental — ${uiState.worktrees.size} worktree${if (uiState.worktrees.size != 1) "s" else ""}",
                        style = AppTypography.labelSmall,
                        color = AppColors.textTertiary
                    )
                }
                MoccaIconButton(
                    icon = Icons.Default.Refresh,
                    onClick = { screenModel.load() },
                    iconColor = AppColors.accent
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                MoccaIconButton(
                    icon = Icons.Default.Add,
                    onClick = { screenModel.showCreateDialog() },
                    iconColor = AppColors.accent
                )
            }

            // ── Error banner ──────────────────────────────────────────────────────
            uiState.error?.let { errorMsg ->
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.medium)
                        .background(AppColors.error.copy(alpha = 0.12f))
                        .border(0.5.dp, AppColors.error.copy(alpha = 0.4f), AppShapes.medium)
                        .padding(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMsg,
                        style = AppTypography.labelSmall,
                        color = AppColors.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { screenModel.clearError() }) {
                        Text("DISMISS", style = AppTypography.labelSmall, color = AppColors.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // ── Content ───────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(AppSpacing.borderThin, AppColors.borderLight, AppShapes.medium)
            ) {
                when {
                    uiState.isLoading && uiState.worktrees.isEmpty() -> LoadingScreen()
                    uiState.worktrees.isEmpty() -> WorktreeEmptyState(
                        onCreateClick = { screenModel.showCreateDialog() }
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AppSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        items(uiState.worktrees, key = { it.id }) { worktree ->
                            WorktreeCard(
                                worktree = worktree,
                                isDeleting = worktree.id in uiState.deletingIds,
                                isResetting = worktree.id in uiState.resettingIds,
                                onDelete = { screenModel.deleteWorktree(worktree.id) },
                                onReset = { screenModel.resetWorktree(worktree.id) }
                            )
                        }
                    }
                }
            }
        }

        // ── Create Dialog ──────────────────────────────────────────────────────────
        if (uiState.showCreateDialog) {
            WorktreeCreateDialog(
                isCreating = uiState.isCreating,
                error = uiState.createError,
                onCreate = { branch -> screenModel.createWorktree(branch) },
                onDismiss = { screenModel.dismissCreateDialog() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Worktree card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorktreeCard(
    worktree: WorktreeInfo,
    isDeleting: Boolean,
    isResetting: Boolean,
    onDelete: () -> Unit,
    onReset: () -> Unit
) {
    val statusColor = when (worktree.status) {
        WorktreeStatus.READY -> AppColors.statusOnline
        WorktreeStatus.CREATING -> AppColors.statusWaiting
        WorktreeStatus.FAILED -> AppColors.statusOffline
        WorktreeStatus.UNKNOWN -> AppColors.textTertiary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surface)
            .border(0.5.dp, AppColors.border, AppShapes.card)
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(AppShapes.circle)
                .background(statusColor)
        )

        Spacer(modifier = Modifier.width(AppSpacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            // Branch name
            Text(
                text = worktree.branch ?: "(no branch)",
                style = AppTypography.labelMedium,
                color = AppColors.white,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Path
            Text(
                text = worktree.path,
                style = AppTypography.monoLabel,
                color = AppColors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Status label + optional error
            Text(
                text = worktree.status.name + (worktree.error?.let { " — $it" } ?: ""),
                style = AppTypography.labelSmall,
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Session linkage
            worktree.sessionID?.let { sid ->
                Text(
                    text = "SESSION: $sid",
                    style = AppTypography.monoLabel,
                    color = AppColors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(AppSpacing.sm))

        // Reset button
        if (isResetting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AppColors.statusWaiting,
                strokeWidth = 2.dp
            )
        } else {
            MoccaIconButton(
                icon = Icons.Default.Refresh,
                onClick = onReset,
                iconColor = AppColors.statusWaiting
            )
        }

        Spacer(modifier = Modifier.width(AppSpacing.xs))

        // Delete button
        if (isDeleting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AppColors.statusOffline,
                strokeWidth = 2.dp
            )
        } else {
            MoccaIconButton(
                icon = Icons.Default.Delete,
                onClick = onDelete,
                iconColor = AppColors.statusOffline
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorktreeEmptyState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NO WORKTREES",
            style = AppTypography.labelLarge,
            color = AppColors.textTertiary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(
            text = "Create a worktree to work on multiple branches simultaneously.",
            style = AppTypography.labelSmall,
            color = AppColors.textTertiary
        )
        Spacer(modifier = Modifier.height(AppSpacing.xl))
        MoccaButton(
            text = "+ NEW WORKTREE",
            onClick = onCreateClick
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Create dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorktreeCreateDialog(
    isCreating: Boolean,
    error: String?,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var branchInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { if (!isCreating) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.dialog)
                .background(AppColors.surface)
                .border(0.5.dp, AppColors.border, AppShapes.dialog)
                .padding(AppSpacing.xl)
        ) {
            Text(
                text = "NEW WORKTREE",
                style = AppTypography.labelLarge,
                color = AppColors.accent,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text(
                text = "BRANCH NAME",
                style = AppTypography.labelSmall,
                color = AppColors.textTertiary
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            OutlinedTextField(
                value = branchInput,
                onValueChange = { branchInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("feature/my-branch", color = AppColors.textPlaceholder)
                },
                singleLine = true,
                enabled = !isCreating,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!isCreating) onCreate(branchInput)
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.accent,
                    unfocusedBorderColor = AppColors.border,
                    focusedTextColor = AppColors.white,
                    unfocusedTextColor = AppColors.white,
                    cursorColor = AppColors.accent,
                    focusedContainerColor = AppColors.background,
                    unfocusedContainerColor = AppColors.background
                ),
                textStyle = AppTypography.code
            )

            error?.let { err ->
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = err,
                    style = AppTypography.labelSmall,
                    color = AppColors.error
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm, Alignment.End)
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isCreating
                ) {
                    Text("CANCEL", style = AppTypography.labelSmall, color = AppColors.textTertiary)
                }

                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterVertically),
                        color = AppColors.accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    MoccaButton(
                        text = "CREATE",
                        onClick = { onCreate(branchInput) },
                        enabled = branchInput.isNotBlank()
                    )
                }
            }
        }
    }
}
