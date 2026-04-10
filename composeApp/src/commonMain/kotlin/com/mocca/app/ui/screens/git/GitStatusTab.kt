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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Surface(
                color = AppColors.surfaceContainerHigh,
                shape = AppShapes.medium,
                border = BorderStroke(0.5.dp, AppColors.white.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Source,
                                contentDescription = null,
                                tint = AppColors.primary,
                                modifier = Modifier.size(20.dp)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            contentDescription = null,
                            tint = gitAccentColor(change.status),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailing = {
                        IconButton(
                            onClick = { screenModel.unstageFile(change.path) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Unstage ${change.path.substringAfterLast('/')}",
                                tint = AppColors.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            contentDescription = null,
                            tint = AppColors.statusWaiting,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { screenModel.stageFile(change.path) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Stage ${change.path.substringAfterLast('/')}",
                                    tint = AppColors.primary.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { screenModel.discardFile(change.path) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Discard changes to ${change.path.substringAfterLast('/')}",
                                    tint = AppColors.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            contentDescription = null,
                            tint = AppColors.white.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailing = {
                        IconButton(
                            onClick = { screenModel.stageFile(path) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Stage ${path.substringAfterLast('/')}",
                                tint = AppColors.primary.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }
        }

        if (uiState.stagedCount > 0) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            contentDescription = null,
                            tint = AppColors.white.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { screenModel.popStash(stash.index) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    Icons.Default.Unarchive,
                                    contentDescription = "Apply ${stash.message.ifBlank { "stash ${stash.index}" }}",
                                    tint = AppColors.primary.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { screenModel.dropStash(stash.index) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete ${stash.message.ifBlank { "stash ${stash.index}" }}",
                                    tint = AppColors.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
            }
        }

        if (status?.clean == true && uiState.stashes.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppColors.statusOnline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
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

private fun gitAccentColor(status: GitFileStatus) = when (status) {
    GitFileStatus.ADDED -> AppColors.diffAddition
    GitFileStatus.MODIFIED -> AppColors.statusWaiting
    GitFileStatus.DELETED -> AppColors.error
    GitFileStatus.RENAMED,
    GitFileStatus.COPIED,
    GitFileStatus.UNMERGED,
    GitFileStatus.UNKNOWN -> AppColors.primary
}
