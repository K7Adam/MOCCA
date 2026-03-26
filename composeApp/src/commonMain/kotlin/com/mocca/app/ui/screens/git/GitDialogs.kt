package com.mocca.app.ui.screens.git

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.components.*
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.*
import com.mocca.app.domain.model.*

@Composable
internal fun GitOverlays(uiState: GitUiState, screenModel: GitScreenModel) {
    val toastMessage = uiState.warningMessage ?: uiState.operationResult
    val toastIsWarning = uiState.warningMessage != null

    // Dialogs
    if (uiState.showCommitDialog) {
        TerminalCommitDialog(
            message = uiState.commitMessage,
            onMessageChange = { screenModel.updateCommitMessage(it) },
            onCommit = { screenModel.commit(uiState.commitMessage) },
            onDismiss = { screenModel.hideCommitDialog() }
        )
    }

    if (uiState.showStashDialog) {
        CreateStashDialog(
            message = uiState.stashMessage,
            onMessageChange = { screenModel.updateStashMessage(it) },
            onCreate = { screenModel.createStash(uiState.stashMessage.ifBlank { null }) },
            onDismiss = { screenModel.hideStashDialog() }
        )
    }

    if (uiState.showAddRemoteDialog) {
        AddRemoteDialog(
            onAdd = { name, url -> screenModel.addRemote(name, url); screenModel.hideAddRemoteDialog() },
            onDismiss = { screenModel.hideAddRemoteDialog() }
        )
    }

    if (uiState.showCreateTagDialog) {
        CreateTagDialog(
            onAdd = { name, msg -> screenModel.createTag(name, msg.ifBlank { null }); screenModel.hideCreateTagDialog() },
            onDismiss = { screenModel.hideCreateTagDialog() }
        )
    }

    toastMessage?.let { message ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = AppSpacing.bottomBarExpandedMinHeight),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = if (toastIsWarning) AppColors.error else AppColors.accentGreen,
                shape = AppShapes.pill,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (toastIsWarning) Icons.Default.Warning else Icons.Default.Check,
                        contentDescription = null,
                        tint = AppColors.background,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = message,
                        color = AppColors.background,
                        style = AppTypography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
internal fun BranchActionDialog(branch: String, onDismiss: () -> Unit, onCheckout: () -> Unit, onMerge: () -> Unit, onRebase: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceContainerHigh,
        title = { Text("Branch actions", style = AppTypography.labelLarge, color = AppColors.onSurface) },
        text = {
            Column {
                Text(text = branch, color = AppColors.statusOnline, style = AppTypography.bodyMedium, modifier = Modifier.padding(vertical = AppSpacing.md))
                Spacer(modifier = Modifier.height(AppSpacing.md))
                GodButton(text = "Checkout", onClick = onCheckout, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                GodButton(text = "Merge into current", onClick = onMerge, containerColor = AppColors.white.copy(alpha = 0.05f), contentColor = AppColors.onSurface, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                GodButton(text = "Rebase current onto", onClick = onRebase, containerColor = AppColors.white.copy(alpha = 0.05f), contentColor = AppColors.onSurface, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {},
        dismissButton = {
            MoccaTextButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        },
        shape = AppShapes.dialog
    )
}

@Composable
internal fun TerminalCommitDialog(message: String, onMessageChange: (String) -> Unit, onCommit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceContainerHigh,
        title = { Text("Commit changes", style = AppTypography.labelLarge, color = AppColors.onSurface) },
        text = {
            MoccaInput(
                value = message,
                onValueChange = onMessageChange,
                placeholder = "feat: implemented terminal ui",
                singleLine = false
            )
        },
        confirmButton = {
            GodButton(text = "Commit", onClick = onCommit, enabled = message.isNotBlank())
        },
        dismissButton = {
            MoccaTextButton(text = "Cancel", onClick = onDismiss)
        },
        shape = AppShapes.dialog
    )
}

@Composable
internal fun CreateStashDialog(message: String, onMessageChange: (String) -> Unit, onCreate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceContainerHigh,
        title = { Text("Create stash", style = AppTypography.labelLarge, color = AppColors.onSurface) },
        text = {
            MoccaInput(
                value = message,
                onValueChange = onMessageChange,
                placeholder = "WIP: saving changes...",
                singleLine = false
            )
        },
        confirmButton = {
            GodButton(text = "Stash", onClick = onCreate)
        },
        dismissButton = {
            MoccaTextButton(text = "Cancel", onClick = onDismiss)
        },
        shape = AppShapes.dialog
    )
}

@Composable
internal fun AddRemoteDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceContainerHigh,
        title = { Text("Add remote", style = AppTypography.labelLarge, color = AppColors.onSurface) },
        text = {
            Column {
                MoccaInput(value = name, onValueChange = { name = it }, label = "Remote name", placeholder = "origin")
                Spacer(modifier = Modifier.height(AppSpacing.md))
                MoccaInput(value = url, onValueChange = { url = it }, label = "Remote URL", placeholder = "https://github.com/...")
            }
        },
        confirmButton = {
            GodButton(text = "Add", onClick = { onAdd(name, url) }, enabled = name.isNotBlank() && url.isNotBlank())
        },
        dismissButton = {
            MoccaTextButton(text = "Cancel", onClick = onDismiss)
        },
        shape = AppShapes.dialog
    )
}

@Composable
internal fun CreateTagDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceContainerHigh,
        title = { Text("Create tag", style = AppTypography.labelLarge, color = AppColors.onSurface) },
        text = {
            Column {
                MoccaInput(value = name, onValueChange = { name = it }, label = "Tag name", placeholder = "v1.0.0")
                Spacer(modifier = Modifier.height(AppSpacing.md))
                MoccaInput(value = msg, onValueChange = { msg = it }, label = "Tag message", placeholder = "Release version 1.0.0")
            }
        },
        confirmButton = {
            GodButton(text = "Create", onClick = { onAdd(name, msg) }, enabled = name.isNotBlank())
        },
        dismissButton = {
            MoccaTextButton(text = "Cancel", onClick = onDismiss)
        },
        shape = AppShapes.dialog
    )
}

@Composable
internal fun CreateBranchDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceContainerHigh,
        title = { Text("Create branch", style = AppTypography.labelLarge, color = AppColors.onSurface) },
        text = {
            MoccaInput(
                value = name,
                onValueChange = { name = it },
                label = "Branch name",
                placeholder = "feature/new-feature"
            )
        },
        confirmButton = {
            GodButton(text = "Create & checkout", onClick = { onCreate(name) }, enabled = name.isNotBlank())
        },
        dismissButton = {
            MoccaTextButton(text = "Cancel", onClick = onDismiss)
        },
        shape = AppShapes.dialog
    )
}
