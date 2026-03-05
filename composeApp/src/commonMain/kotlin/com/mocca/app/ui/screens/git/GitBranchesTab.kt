package com.mocca.app.ui.screens.git

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.components.*
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.*
import com.mocca.app.domain.model.*

@Composable
internal fun BranchesTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val branches = uiState.branches
    val currentBranch = uiState.currentBranch
    var selectedBranch by remember { mutableStateOf<String?>(null) }
    var showCreateBranch by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val localBranches = branches.filter { !it.remote }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOCAL BRANCHES",
                    style = AppTypography.labelSmall,
                    color = AppColors.white.copy(alpha = 0.4f)
                )
                GodButton(
                    text = "CREATE",
                    onClick = { showCreateBranch = true },
                    containerColor = AppColors.white.copy(alpha = 0.05f),
                    contentColor = AppColors.white,
                    modifier = Modifier.height(32.dp)
                )
            }
        }
        if (localBranches.isNotEmpty()) {
            items(items = localBranches, key = { "local-${it.name}" }) { branch ->
                GodListItem(
                    title = branch.name,
                    subtitle = if (branch.upstream != null) "Tracks: ${branch.upstream}" else "Local only",
                    icon = {
                        Icon(
                            imageVector = if (branch.name == currentBranch) Icons.Default.CheckCircle else Icons.Default.Source,
                            contentDescription = null,
                            tint = if (branch.name == currentBranch) AppColors.accentGreen else AppColors.white.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailing = {
                        if (branch.ahead > 0 || branch.behind > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (branch.ahead > 0) GodBadge(text = "+${branch.ahead}", contentColor = AppColors.accentGreen)
                                if (branch.behind > 0) GodBadge(text = "-${branch.behind}", contentColor = AppColors.alertRed)
                            }
                        }
                    },
                    onClick = { selectedBranch = branch.name }
                )
            }
        }

        val remoteBranches = branches.filter { it.remote }
        if (remoteBranches.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "REMOTE BRANCHES",
                    style = AppTypography.labelSmall,
                    color = AppColors.white.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(items = remoteBranches, key = { "remote-${it.name}" }) { branch ->
                GodListItem(
                    title = branch.name,
                    subtitle = "Remote branch",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = AppColors.white.copy(alpha = 0.2f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { selectedBranch = branch.name }
                )
            }
        }
    }
    
    selectedBranch?.let { branch ->
        BranchActionDialog(
            branch = branch,
            onDismiss = { selectedBranch = null },
            onCheckout = { screenModel.checkout(branch); selectedBranch = null },
            onMerge = { screenModel.merge(branch); selectedBranch = null },
            onRebase = { screenModel.rebase(branch); selectedBranch = null }
        )
    }
    
    if (showCreateBranch) {
        CreateBranchDialog(
            onDismiss = { showCreateBranch = false },
            onCreate = { name -> screenModel.checkout(name, create = true); showCreateBranch = false }
        )
    }
}
