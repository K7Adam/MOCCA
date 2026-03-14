package com.mocca.app.ui.screens.git

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.components.*
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.*
import com.mocca.app.domain.model.*

@Composable
internal fun RemotesTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val remotes = uiState.remotes
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REMOTES",
                    style = AppTypography.labelSmall,
                    color = AppColors.white.copy(alpha = 0.4f)
                )
                GodButton(
                    text = "ADD",
                    onClick = { screenModel.showAddRemoteDialog() },
                    containerColor = AppColors.white.copy(alpha = 0.05f),
                    contentColor = AppColors.textPrimary,
                    modifier = Modifier.height(32.dp)
                )
            }
        }
        items(items = remotes, key = { it.name }) { remote ->
            GodListItem(
                title = remote.name.uppercase(),
                subtitle = remote.url,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = AppColors.white.copy(alpha = 0.4f)
                    )
                },
                trailing = {
                    IconButton(onClick = { screenModel.removeRemote(remote.name) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.alertRed.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}

@Composable
internal fun StashesTab(uiState: GitUiState, screenModel: GitScreenModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Stashes Not Fully Implemented", fontWeight = FontWeight.Bold, color = AppColors.textSecondary)
            Text("Counts: ${uiState.stashes.size}", color = AppColors.textSecondary.copy(alpha = 0.7f))
        }
    }
}

@Composable
internal fun TagsTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val tags = uiState.tags
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TAGS",
                    style = AppTypography.labelSmall,
                    color = AppColors.white.copy(alpha = 0.4f)
                )
                GodButton(
                    text = "CREATE",
                    onClick = { screenModel.showCreateTagDialog() },
                    containerColor = AppColors.white.copy(alpha = 0.05f),
                    contentColor = AppColors.textPrimary,
                    modifier = Modifier.height(32.dp)
                )
            }
        }
        items(tags) { tag ->
            GodListItem(
                title = tag,
                subtitle = "Release tag",
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        tint = AppColors.accentGreen.copy(alpha = 0.6f)
                    )
                },
                trailing = {
                    IconButton(onClick = { screenModel.deleteTag(tag) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.alertRed.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}
