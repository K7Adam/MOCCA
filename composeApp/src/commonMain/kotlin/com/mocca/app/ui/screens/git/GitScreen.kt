package com.mocca.app.ui.screens.git

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.ErrorScreen
import com.mocca.app.ui.components.LoadingScreen
import com.mocca.app.ui.components.terminal.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GitScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<GitScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val selectedTab by screenModel.selectedTab.collectAsState()
        
        val toastMessage = uiState.warningMessage ?: uiState.operationResult
        val toastIsWarning = uiState.warningMessage != null
        
        LaunchedEffect(uiState.operationResult) {
            if (uiState.operationResult != null) {
                delay(3000)
                screenModel.clearOperationResult()
            }
        }
        
        LaunchedEffect(uiState.warningMessage) {
            if (uiState.warningMessage != null) {
                delay(4000)
                screenModel.clearWarning()
            }
        }
        
        Box(modifier = Modifier.fillMaxSize().background(AppColors.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.lg)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TerminalIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = { navigator.pop() },
                            iconColor = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.md))
                        Column {
                            Text(
                                text = "GIT CONTROL",
                                style = AppTypography.labelLarge,
                                color = AppColors.white,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "BRANCH: ${uiState.currentBranch}",
                                color = AppColors.statusOnline,
                                style = AppTypography.labelSmall
                            )
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        TerminalIconButton(icon = Icons.Default.Sync, onClick = { screenModel.fetch() }, contentDescription = "FETCH", iconColor = AppColors.textSecondary)
                        TerminalIconButton(icon = Icons.Default.Download, onClick = { screenModel.pull() }, contentDescription = "PULL", iconColor = AppColors.textSecondary)
                        TerminalIconButton(icon = Icons.Default.Upload, onClick = { screenModel.push() }, contentDescription = "PUSH", iconColor = AppColors.textSecondary)
                    }
                }
                
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                
                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    GitTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        TabPillButton(
                            text = tab.title.uppercase(),
                            isSelected = isSelected,
                            onClick = { screenModel.selectTab(tab) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                
                // Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(AppShapes.card)
                        .background(AppColors.surfaceContainer, AppShapes.card)
                        .border(AppSpacing.borderThin, AppColors.border, AppShapes.card)
                ) {
                    when {
                        uiState.isLoading -> LoadingScreen()
                        uiState.error != null -> ErrorScreen(
                            message = uiState.error!!,
                            onRetry = {
                                screenModel.clearError()
                                screenModel.selectTab(selectedTab)
                            }
                        )
                        else -> when (selectedTab) {
                            GitTab.STATUS -> StatusTab(
                                uiState,
                                screenModel,
                                onNavigateToDiff = { path, staged ->
                                    navigator.push(GitDiffScreen(path, staged))
                                }
                            )
                            GitTab.BRANCHES -> BranchesTab(uiState, screenModel)
                            GitTab.LOG -> LogTab(uiState, screenModel)
                            GitTab.REMOTES -> RemotesTab(uiState, screenModel)
                            GitTab.TAGS -> TagsTab(uiState, screenModel)
                        }
                    }

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
                }
            }

            // Overlays
            if (uiState.showServerNotRunningDialog) {
                com.mocca.app.ui.components.GitServerNotRunningDialog(
                    onDismiss = { screenModel.hideServerNotRunningDialog() },
                    onStartServer = { screenModel.requestStartGitServer() },
                    showAdbHelp = uiState.showAdbReverseHelp,
                    isAttemptingStart = uiState.isStartingServer,
                    attemptCount = uiState.serverStartAttempt,
                    maxAttempts = uiState.maxServerStartAttempts
                )
            }

            if (uiState.isStartingServer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.background.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
                    ) {
                        CircularProgressIndicator(color = AppColors.accentGreen, strokeWidth = 2.dp)
                        Text(text = "STARTING GIT SERVER...", color = AppColors.white, style = AppTypography.titleMedium)
                        uiState.serverStartProgress?.let { progress ->
                            Text(text = progress.uppercase(), color = AppColors.textSecondary, style = AppTypography.bodySmall)
                        }
                    }
                }
            }

            toastMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(AppSpacing.md)
                        .clip(AppShapes.pill)
                        .background(if (toastIsWarning) AppColors.statusWaiting else AppColors.statusOnline)
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
                ) {
                    Text(
                        text = message.uppercase(), 
                        color = AppColors.background, 
                        style = AppTypography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusTab(
    uiState: GitUiState, 
    screenModel: GitScreenModel,
    onNavigateToDiff: (String, Boolean) -> Unit
) {
    val status = uiState.status
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        item { StatusHeader(status) }
        item {
            StashesSection(
                stashes = uiState.stashes,
                onCreateStash = { screenModel.showStashDialog() },
                onPop = { screenModel.popStash(it) },
                onApply = { screenModel.applyStash(it) },
                onDrop = { screenModel.dropStash(it) }
            )
        }
        if (status != null && status.staged.isNotEmpty()) {
            item { SectionHeader(title = "STAGED CHANGES [${status.staged.size}]", action = { TerminalTextButton(text = "UNSTAGE ALL", onClick = { screenModel.unstageAll() }) }) }
            itemsIndexed(items = status.staged, key = { index, change -> "staged-$index-${change.path}" }) { _, change ->
                FileChangeItem(change = change, staged = true, onUnstage = { screenModel.unstageFile(change.path) }, onClick = { onNavigateToDiff(change.path, true) })
            }
        }
        if (status != null && status.unstaged.isNotEmpty()) {
            item { SectionHeader(title = "CHANGES [${status.unstaged.size}]", action = { TerminalTextButton(text = "STAGE ALL", onClick = { screenModel.stageAll() }) }) }
            itemsIndexed(status.unstaged, key = { index, change -> "unstaged-$index-${change.path}" }) { _, change ->
                FileChangeItem(change = change, staged = false, onStage = { screenModel.stageFile(change.path) }, onDiscard = { screenModel.discardFile(change.path) }, onClick = { onNavigateToDiff(change.path, false) })
            }
        }
        if (status != null && status.untracked.isNotEmpty()) {
            item { SectionHeader(title = "UNTRACKED FILES [${status.untracked.size}]") }
            items(status.untracked, key = { "untracked-$it" }) { path ->
                UntrackedFileItem(path = path, onStage = { screenModel.stageFile(path) })
            }
        }
        if (uiState.stagedCount > 0) {
            item { 
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                TerminalButton(
                    text = "COMMIT ${uiState.stagedCount} FILES", 
                    onClick = { screenModel.showCommitDialog() }, 
                    icon = Icons.Default.Check,
                    modifier = Modifier.fillMaxWidth()
                ) 
            }
        }
        if (status?.clean == true) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppColors.statusOnline, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("WORKING TREE CLEAN", style = AppTypography.headlineSmall, color = AppColors.statusOnline)
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchesTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val branches = uiState.branches
    val currentBranch = uiState.currentBranch
    var selectedBranch by remember { mutableStateOf<String?>(null) }
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        val localBranches = branches.filter { !it.remote }
        if (localBranches.isNotEmpty()) {
            item { SectionHeader("LOCAL BRANCHES") }
            items(items = localBranches, key = { "local-${it.name}" }) { branch ->
                BranchItem(branch = branch, isCurrent = branch.name == currentBranch, onClick = { selectedBranch = branch.name })
            }
        }
        val remoteBranches = branches.filter { it.remote }
        if (remoteBranches.isNotEmpty()) {
            item { Spacer(Modifier.height(AppSpacing.lg)); SectionHeader("REMOTE BRANCHES") }
            items(items = remoteBranches, key = { "remote-${it.name}" }) { branch ->
                BranchItem(branch = branch, isCurrent = false, onClick = { selectedBranch = branch.name })
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
}

@Composable
private fun RemotesTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val remotes = uiState.remotes
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        item { SectionHeader(title = "REMOTES [${remotes.size}]", action = { TerminalTextButton("ADD", onClick = { screenModel.showAddRemoteDialog() }) }) }
        items(items = remotes, key = { it.name }) { remote ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.card)
                    .background(AppColors.surfaceVariant, AppShapes.card)
                    .border(AppSpacing.borderThin, AppColors.border, AppShapes.card)
                    .padding(AppSpacing.md), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = remote.name.uppercase(), color = AppColors.white, style = AppTypography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(text = remote.url, style = AppTypography.bodySmall, color = AppColors.textTertiary)
                }
                TerminalIconButton(icon = Icons.Default.Delete, onClick = { screenModel.removeRemote(remote.name) }, iconColor = AppColors.alertRed)
            }
        }
    }
}

@Composable
private fun TagsTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val tags = uiState.tags
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        item { SectionHeader(title = "TAGS [${tags.size}]", action = { TerminalTextButton("CREATE", onClick = { screenModel.showCreateTagDialog() }) }) }
        items(tags) { tag ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.card)
                    .background(AppColors.surfaceVariant, AppShapes.card)
                    .border(AppSpacing.borderThin, AppColors.border, AppShapes.card)
                    .padding(AppSpacing.sm), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = AppColors.accentGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(AppSpacing.sm))
                Text(text = tag, color = AppColors.white, style = AppTypography.bodySmall, modifier = Modifier.weight(1f))
                TerminalIconButton(icon = Icons.Default.Delete, onClick = { screenModel.deleteTag(tag) }, iconColor = AppColors.alertRed)
            }
        }
    }
}

@Composable
private fun StatusHeader(status: GitStatusResponse?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceVariant, AppShapes.card)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.card)
            .padding(AppSpacing.md)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "BRANCH: ${status?.branch ?: "UNKNOWN"}", color = AppColors.white, style = AppTypography.bodyMedium, fontWeight = FontWeight.Bold)
            if (status?.upstream != null) { Text(text = "UPSTREAM: ${status.upstream}", color = AppColors.textTertiary, style = AppTypography.bodySmall) }
        }
        if (status?.ahead ?: 0 > 0 || status?.behind ?: 0 > 0) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (status?.ahead ?: 0 > 0) { Text("${status?.ahead} AHEAD", color = AppColors.statusOnline, style = AppTypography.labelSmall) }
                if (status?.behind ?: 0 > 0) { Text("${status?.behind} BEHIND", color = AppColors.error, style = AppTypography.labelSmall) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: @Composable (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, color = AppColors.textSecondary, style = AppTypography.labelMedium)
        action?.invoke()
    }
}

@Composable
private fun FileChangeItem(change: GitFileChange, staged: Boolean, onStage: (() -> Unit)? = null, onUnstage: (() -> Unit)? = null, onDiscard: (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    val statusColor = when (change.status) { GitFileStatus.ADDED -> AppColors.statusOnline; GitFileStatus.MODIFIED -> AppColors.statusWaiting; GitFileStatus.DELETED -> AppColors.error; GitFileStatus.RENAMED -> AppColors.statusOffline; else -> AppColors.white }
    val statusChar = when (change.status) { GitFileStatus.ADDED -> "A"; GitFileStatus.MODIFIED -> "M"; GitFileStatus.DELETED -> "D"; GitFileStatus.RENAMED -> "R"; else -> "?" }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceVariant, AppShapes.card)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.card)
            .clickable { onClick?.invoke() }
            .padding(AppSpacing.sm), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp).clip(AppShapes.extraSmall).background(statusColor.copy(alpha = 0.2f), AppShapes.extraSmall), contentAlignment = Alignment.Center) { Text(text = statusChar, color = statusColor, fontWeight = FontWeight.Bold, style = AppTypography.bodyMedium) }
        Spacer(Modifier.width(AppSpacing.sm))
        Text(text = change.path, color = AppColors.white, style = AppTypography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (staged) { onUnstage?.let { TerminalIconButton(icon = Icons.Default.Remove, onClick = it, contentDescription = "Unstage", iconColor = AppColors.textSecondary) } }
        else { onStage?.let { TerminalIconButton(icon = Icons.Default.Add, onClick = it, contentDescription = "Stage", iconColor = AppColors.statusOnline) }; onDiscard?.let { TerminalIconButton(icon = Icons.AutoMirrored.Filled.Undo, onClick = it, contentDescription = "Discard", iconColor = AppColors.alertRed) } }
    }
}

@Composable
private fun UntrackedFileItem(path: String, onStage: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceVariant, AppShapes.card)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.card)
            .padding(AppSpacing.sm), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp).clip(AppShapes.extraSmall).background(AppColors.surfaceContainer, AppShapes.extraSmall), contentAlignment = Alignment.Center) { Text(text = "?", color = AppColors.textTertiary, fontWeight = FontWeight.Bold, style = AppTypography.bodyMedium) }
        Spacer(Modifier.width(AppSpacing.sm))
        Text(text = path, color = AppColors.white, style = AppTypography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        TerminalIconButton(icon = Icons.Default.Add, onClick = onStage, contentDescription = "Stage", iconColor = AppColors.statusOnline)
    }
}

@Composable
private fun BranchItem(branch: GitBranch, isCurrent: Boolean, onClick: () -> Unit) {
    val bgColor = if (isCurrent) AppColors.statusOnline.copy(alpha = 0.1f) else AppColors.surfaceVariant
    val borderColor = if (isCurrent) AppColors.statusOnline else AppColors.border
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(bgColor, AppShapes.card)
            .border(AppSpacing.borderThin, borderColor, AppShapes.card)
            .clickable(onClick = onClick)
            .padding(AppSpacing.md), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrent) { Icon(Icons.Default.Check, contentDescription = "Current", tint = AppColors.statusOnline, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(AppSpacing.sm)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = branch.name, color = if (isCurrent) AppColors.statusOnline else AppColors.white, style = AppTypography.bodyMedium, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
            if (branch.upstream != null) { Text(text = "TRACKS: ${branch.upstream}", style = AppTypography.labelSmall, color = AppColors.textTertiary) }
        }
        if (branch.ahead > 0 || branch.behind > 0) { Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) { if (branch.ahead > 0) { Text("+${branch.ahead}", color = AppColors.statusOnline, style = AppTypography.labelSmall) }; if (branch.behind > 0) { Text("-${branch.behind}", color = AppColors.error, style = AppTypography.labelSmall) } } }
    }
}

@Composable
private fun CommitItem(commit: GitCommit, dateFormat: SimpleDateFormat) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceVariant, AppShapes.card)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.card)
            .padding(AppSpacing.md)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = commit.shortHash, style = AppTypography.bodySmall, color = AppColors.accentGreen)
            Text(text = dateFormat.format(Date(commit.date)), style = AppTypography.labelSmall, color = AppColors.textTertiary)
        }
        Spacer(Modifier.height(AppSpacing.sm))
        Text(text = commit.message, color = AppColors.white, style = AppTypography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(AppSpacing.sm))
        Text(text = "AUTHOR: ${commit.author}", style = AppTypography.labelSmall, color = AppColors.textSecondary)
    }
}

@Composable
private fun TerminalCommitDialog(message: String, onMessageChange: (String) -> Unit, onCommit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceElevated,
        title = { Text("COMMIT CHANGES", style = AppTypography.labelLarge, color = AppColors.white) },
        text = {
            TerminalInput(
                value = message,
                onValueChange = onMessageChange,
                placeholder = "feat: implemented terminal ui",
                singleLine = false
            )
        },
        confirmButton = {
            TerminalButton(text = "COMMIT", onClick = onCommit, enabled = message.isNotBlank())
        },
        dismissButton = {
            TerminalTextButton(text = "CANCEL", onClick = onDismiss)
        },
        shape = AppShapes.dialog
    )
}

@Composable
private fun CreateStashDialog(message: String, onMessageChange: (String) -> Unit, onCreate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceElevated,
        title = { Text("CREATE STASH", style = AppTypography.labelLarge, color = AppColors.white) },
        text = {
            TerminalInput(
                value = message,
                onValueChange = onMessageChange,
                placeholder = "WIP: saving changes...",
                singleLine = false
            )
        },
        confirmButton = {
            TerminalButton(text = "STASH", onClick = onCreate)
        },
        dismissButton = {
            TerminalTextButton(text = "CANCEL", onClick = onDismiss)
        },
        shape = AppShapes.dialog
    )
}

@Composable
private fun StashesSection(stashes: List<GitStash>, onCreateStash: () -> Unit, onPop: (Int) -> Unit, onApply: (Int) -> Unit, onDrop: (Int) -> Unit) {
    Column {
        SectionHeader(title = "STASHES [${stashes.size}]", action = { TerminalTextButton(text = "CREATE", onClick = onCreateStash) })
        stashes.forEach { stash ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.card)
                    .background(AppColors.surfaceVariant, AppShapes.card)
                    .border(AppSpacing.borderThin, AppColors.border, AppShapes.card)
                    .padding(AppSpacing.sm), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "stash@{${stash.index}}", color = AppColors.statusWaiting, style = AppTypography.labelSmall)
                    Text(text = stash.message, color = AppColors.white, style = AppTypography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row {
                    TerminalIconButton(icon = Icons.Default.Upload, onClick = { onPop(stash.index) }, contentDescription = "Pop", iconColor = AppColors.statusOnline)
                    TerminalIconButton(icon = Icons.Default.PlayArrow, onClick = { onApply(stash.index) }, contentDescription = "Apply", iconColor = AppColors.white)
                    TerminalIconButton(icon = Icons.Default.Delete, onClick = { onDrop(stash.index) }, contentDescription = "Drop", iconColor = AppColors.error)
                }
            }
        }
    }
}

@Composable
private fun AddRemoteDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceElevated,
        title = { Text("ADD REMOTE", style = AppTypography.labelLarge, color = AppColors.white) },
        text = {
            Column {
                TerminalInput(value = name, onValueChange = { name = it }, label = "REMOTE NAME", placeholder = "origin")
                Spacer(modifier = Modifier.height(AppSpacing.md))
                TerminalInput(value = url, onValueChange = { url = it }, label = "REMOTE URL", placeholder = "https://github.com/...")
            }
        },
        confirmButton = {
            TerminalButton(text = "ADD", onClick = { onAdd(name, url) }, enabled = name.isNotBlank() && url.isNotBlank())
        },
        dismissButton = {
            TerminalTextButton(text = "CANCEL", onClick = onDismiss)
        },
        shape = AppShapes.dialog
    )
}

@Composable
private fun CreateTagDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceElevated,
        title = { Text("CREATE TAG", style = AppTypography.labelLarge, color = AppColors.white) },
        text = {
            Column {
                TerminalInput(value = name, onValueChange = { name = it }, label = "TAG NAME", placeholder = "v1.0.0")
                Spacer(modifier = Modifier.height(AppSpacing.md))
                TerminalInput(value = msg, onValueChange = { msg = it }, label = "TAG MESSAGE", placeholder = "Release version 1.0.0")
            }
        },
        confirmButton = {
            TerminalButton(text = "CREATE", onClick = { onAdd(name, msg) }, enabled = name.isNotBlank())
        },
        dismissButton = {
            TerminalTextButton(text = "CANCEL", onClick = onDismiss)
        },
        shape = AppShapes.dialog
    )
}

@Composable
private fun LogTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val commits = uiState.log?.commits ?: emptyList()
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        items(items = commits, key = { it.hash }) { commit -> CommitItem(commit, dateFormat) }
        if (uiState.log?.hasMore == true) {
            item { TerminalButton(text = "LOAD MORE", onClick = { screenModel.loadLog(skip = commits.size) }, modifier = Modifier.fillMaxWidth() ) }
        }
    }
}

@Composable
private fun BranchActionDialog(branch: String, onDismiss: () -> Unit, onCheckout: () -> Unit, onMerge: () -> Unit, onRebase: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceElevated,
        title = { Text("BRANCH ACTIONS", style = AppTypography.labelLarge, color = AppColors.white) },
        text = {
            Column {
                Text(text = branch, color = AppColors.statusOnline, style = AppTypography.bodyMedium, modifier = Modifier.padding(vertical = AppSpacing.md))
                Spacer(modifier = Modifier.height(AppSpacing.md))
                TerminalButton(text = "CHECKOUT", onClick = onCheckout, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                TerminalButton(text = "MERGE INTO CURRENT", onClick = onMerge, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                TerminalButton(text = "REBASE CURRENT ONTO", onClick = onRebase, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {},
        dismissButton = {
            TerminalTextButton(text = "CANCEL", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        },
        shape = AppShapes.dialog
    )
}
