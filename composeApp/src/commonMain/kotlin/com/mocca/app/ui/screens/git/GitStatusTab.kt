package com.mocca.app.ui.screens.git

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.components.*
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.*
import com.mocca.app.domain.model.*

@Composable
internal fun GitStatusSummary(
    uiState: GitUiState,
    screenModel: GitScreenModel,
    onNavigateToDiff: (String, Boolean) -> Unit
) {
    val status = uiState.status
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.cardPaddingLarge, vertical = AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {

        item {
            Surface(
                color = AppColors.surfaceContainerHigh,
                shape = AppShapes.medium,
                border = BorderStroke(AppSpacing.borderThin, AppColors.white.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(AppSpacing.cardPaddingLarge),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            Icon(
                                Icons.Default.Source,
                                contentDescription = "Current branch",
                                tint = AppColors.primary,
                                modifier = Modifier.size(AppSpacing.iconSizeMedium)
                            )
                            Text(
                                text = status?.branch ?: "unknown",
                                style = AppTypography.titleMedium,
                                color = AppColors.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if ((status?.ahead ?: 0) > 0) {
                                GodBadge(
                                    text = "↑${status?.ahead}",
                                    containerColor = AppColors.primary.copy(alpha = 0.2f),
                                    contentColor = AppColors.primary
                                )
                            }
                            if ((status?.behind ?: 0) > 0) {
                                GodBadge(
                                    text = "↓${status?.behind}",
                                    containerColor = AppColors.error.copy(alpha = 0.2f),
                                    contentColor = AppColors.error
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
                        Text(
                            text = "${uiState.stagedCount} staged",
                            style = AppTypography.labelSmall,
                            color = AppColors.primary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${uiState.unstagedCount} modified",
                            style = AppTypography.labelSmall,
                            color = AppColors.statusWaiting.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${uiState.untrackedCount} untracked",
                            style = AppTypography.labelSmall,
                            color = AppColors.white.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        if (status != null && status.staged.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Text("Staged", style = AppTypography.labelSmall, color = AppColors.primary.copy(alpha = 0.6f))
                        GodBadge(
                            text = "${status.staged.size}",
                            containerColor = AppColors.primary.copy(alpha = 0.15f),
                            contentColor = AppColors.primary
                        )
                    }
                    TextButton(onClick = { screenModel.unstageAll() }) {
                        Text("Unstage all", style = AppTypography.labelSmall, color = AppColors.primary)
                    }
                }
            }
            items(status.staged) { change ->
                GodListItem(
                    title = change.path.substringAfterLast('/'),
                    subtitle = change.path.substringBeforeLast('/', "").ifBlank { "/" } + " • ${change.status.name}",
                    icon = {
                        Icon(
                            imageVector = statusIcon(change.status),
                            contentDescription = gitFileStatusDescription(change.status),
                            tint = gitAccentColor(change.status),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailing = {
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(AppSpacing.iconButtonSize)
                                .moccaClickable(onClick = { screenModel.unstageFile(change.path) }, pressedScale = 0.92f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Unstage ${change.path.substringAfterLast('/')}",
                                tint = AppColors.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(AppSpacing.iconSizeMedium)
                            )
                        }
                    },
                    onClick = { onNavigateToDiff(change.path, true) }
                )
            }
        }

        if (status != null && status.unstaged.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Text(
                            "Modified",
                            style = AppTypography.labelSmall,
                            color = AppColors.statusWaiting.copy(alpha = 0.6f)
                        )
                        GodBadge(
                            text = "${status.unstaged.size}",
                            containerColor = AppColors.statusWaiting.copy(alpha = 0.15f),
                            contentColor = AppColors.statusWaiting
                        )
                    }
                    TextButton(onClick = { screenModel.stageAll() }) {
                        Text("Stage all", style = AppTypography.labelSmall, color = AppColors.primary)
                    }
                }
            }
            items(status.unstaged) { change ->
                GodListItem(
                    title = change.path.substringAfterLast('/'),
                    subtitle = change.path.substringBeforeLast('/', "").ifBlank { "/" } + " • ${change.status.name}",
                    icon = {
                        Icon(
                            imageVector = statusIcon(change.status),
                            contentDescription = gitFileStatusDescription(change.status),
                            tint = AppColors.statusWaiting,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(AppSpacing.iconButtonSize)
                                    .moccaClickable(onClick = { screenModel.stageFile(change.path) }, pressedScale = 0.92f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Stage ${change.path.substringAfterLast('/')}",
                                    tint = AppColors.primary.copy(alpha = 0.85f),
                                    modifier = Modifier.size(AppSpacing.iconSizeMedium)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(AppSpacing.iconButtonSize)
                                    .moccaClickable(onClick = { screenModel.discardFile(change.path) }, pressedScale = 0.92f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Discard changes to ${change.path.substringAfterLast('/')}",
                                    tint = AppColors.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(AppSpacing.iconSizeMedium)
                                )
                            }
                        }
                    },
                    onClick = { onNavigateToDiff(change.path, false) }
                )
            }
        }

        if (status != null && status.untracked.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Text("Untracked", style = AppTypography.labelSmall, color = AppColors.white.copy(alpha = 0.4f))
                        GodBadge(
                            text = "${status.untracked.size}",
                            containerColor = AppColors.white.copy(alpha = 0.08f),
                            contentColor = AppColors.white.copy(alpha = 0.5f)
                        )
                    }
                    TextButton(onClick = { screenModel.stageAll() }) {
                        Text("Stage all", style = AppTypography.labelSmall, color = AppColors.primary)
                    }
                }
            }
            items(status.untracked) { path ->
                GodListItem(
                    title = path.substringAfterLast('/'),
                    subtitle = path.substringBeforeLast('/', "").ifBlank { "/" } + " • New",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FiberNew,
                            contentDescription = "New file",
                            tint = AppColors.white.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailing = {
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(AppSpacing.iconButtonSize)
                                .moccaClickable(onClick = { screenModel.stageFile(path) }, pressedScale = 0.92f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Stage ${path.substringAfterLast('/')}",
                                tint = AppColors.primary.copy(alpha = 0.85f),
                                modifier = Modifier.size(AppSpacing.iconSizeMedium)
                            )
                        }
                    }
                )
            }
        }

        if (uiState.stagedCount > 0) {
            item {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                GodButton(
                    text = "Commit ${uiState.stagedCount} files",
                    onClick = { screenModel.showCommitDialog() },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (uiState.stashes.isNotEmpty() || uiState.hasChanges) {
            item {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Text("Stashes", style = AppTypography.labelSmall, color = AppColors.white.copy(alpha = 0.4f))
                        if (uiState.stashes.isNotEmpty()) {
                            GodBadge(
                                text = "${uiState.stashes.size}",
                                containerColor = AppColors.white.copy(alpha = 0.08f),
                                contentColor = AppColors.white.copy(alpha = 0.5f)
                            )
                        }
                    }
                    if (uiState.hasChanges) {
                        TextButton(onClick = { screenModel.showStashDialog() }) {
                            Text("Stash changes", style = AppTypography.labelSmall, color = AppColors.primary)
                        }
                    }
                }
            }
            items(uiState.stashes.toList()) { stash ->
                GodListItem(
                    title = stash.message.ifBlank { "stash@{${stash.index}}" },
                    subtitle = "stash@{${stash.index}}" + (stash.branch?.let { " on $it" } ?: ""),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Stash entry",
                            tint = AppColors.white.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(AppSpacing.iconButtonSize)
                                    .moccaClickable(onClick = { screenModel.popStash(stash.index) }, pressedScale = 0.92f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Unarchive,
                                    contentDescription = "Apply ${stash.message.ifBlank { "stash ${stash.index}" }}",
                                    tint = AppColors.primary.copy(alpha = 0.85f),
                                    modifier = Modifier.size(AppSpacing.iconSizeMedium)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(AppSpacing.iconButtonSize)
                                    .moccaClickable(onClick = { screenModel.dropStash(stash.index) }, pressedScale = 0.92f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete ${stash.message.ifBlank { "stash ${stash.index}" }}",
                                    tint = AppColors.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(AppSpacing.iconSizeMedium)
                                )
                            }
                        }
                    }
                )
            }
        }

        if (status?.clean == true && uiState.stashes.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(AppSpacing.xxl), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppColors.statusOnline,
                            modifier = Modifier.size(AppSpacing.xxxl)
                        )
                        Spacer(Modifier.height(AppSpacing.sm))
                        Text("Working tree clean", style = AppTypography.headlineSmall, color = AppColors.statusOnline)
                    }
                }
            }
        }

        // Bottom padding for floating bar clearance
        item { Spacer(modifier = Modifier.height(AppSpacing.bottomBarClearance)) }
    }
}

/**
 * Maps GitFileStatus to an appropriate Material icon.
 */
internal fun statusIcon(status: GitFileStatus): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        GitFileStatus.ADDED -> Icons.Default.Add
        GitFileStatus.MODIFIED -> Icons.Default.Edit
        GitFileStatus.DELETED -> Icons.Default.Delete
        GitFileStatus.RENAMED -> Icons.Default.DriveFileRenameOutline
        GitFileStatus.COPIED -> Icons.Default.ContentCopy
        GitFileStatus.UNMERGED -> Icons.AutoMirrored.Filled.CallMerge
        GitFileStatus.UNKNOWN -> Icons.AutoMirrored.Filled.HelpOutline
    }
}

private fun gitFileStatusDescription(status: GitFileStatus): String {
    return when (status) {
        GitFileStatus.ADDED -> "Added file"
        GitFileStatus.MODIFIED -> "Modified file"
        GitFileStatus.DELETED -> "Deleted file"
        GitFileStatus.RENAMED -> "Renamed file"
        GitFileStatus.COPIED -> "Copied file"
        GitFileStatus.UNMERGED -> "Unmerged file"
        GitFileStatus.UNKNOWN -> "Unknown file status"
    }
}

@Composable
private fun gitAccentColor(status: GitFileStatus) = when (status) {
    GitFileStatus.ADDED -> AppColors.diffAddition
    GitFileStatus.MODIFIED -> AppColors.statusWaiting
    GitFileStatus.DELETED -> AppColors.error
    GitFileStatus.RENAMED,
    GitFileStatus.COPIED,
    GitFileStatus.UNMERGED,
    GitFileStatus.UNKNOWN -> AppColors.primary
}
