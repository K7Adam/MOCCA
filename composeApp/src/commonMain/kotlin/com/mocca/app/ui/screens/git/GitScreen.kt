package com.mocca.app.ui.screens.git

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
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
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TerminalColors.background)
                    .padding(TerminalSpacing.lg)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TerminalIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = { navigator.pop() },
                            iconColor = TerminalColors.white
                        )
                        Spacer(modifier = Modifier.width(TerminalSpacing.md))
                        Column {
                            TerminalHeader(text = "GIT_CONTROL", showBrackets = true)
                            Text(
                                text = "BRANCH: ${uiState.currentBranch}",
                                color = TerminalColors.statusOnline,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
                        TerminalIconButton(icon = Icons.Default.Sync, onClick = { screenModel.fetch() }, contentDescription = "FETCH")
                        TerminalIconButton(icon = Icons.Default.Download, onClick = { screenModel.pull() }, contentDescription = "PULL")
                        TerminalIconButton(icon = Icons.Default.Upload, onClick = { screenModel.push() }, contentDescription = "PUSH")
                    }
                }
                
                Spacer(modifier = Modifier.height(TerminalSpacing.md))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
                ) {
                    GitTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .background(if (isSelected) TerminalColors.white else Color.Transparent, RectangleShape)
                                .border(TerminalSpacing.borderThin, TerminalColors.white, RectangleShape)
                                .clickable { screenModel.selectTab(tab) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.title.uppercase(),
                                color = if (isSelected) TerminalColors.background else TerminalColors.white,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
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

            if (uiState.showServerNotRunningDialog) {
                com.mocca.app.ui.components.GitServerNotRunningDialog(
                    onDismiss = { screenModel.hideServerNotRunningDialog() },
                    onStartServer = { screenModel.requestStartGitServer() },
                    showAdbHelp = uiState.showAdbReverseHelp
                )
            }

            if (uiState.isStartingServer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TerminalColors.background.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.lg)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(color = TerminalColors.white, strokeWidth = 2.dp)
                        Text(text = "STARTING GIT SERVER...", color = TerminalColors.white, style = MaterialTheme.typography.titleMedium)
                        uiState.serverStartProgress?.let { progress ->
                            Text(text = progress.uppercase(), color = TerminalColors.greyLight, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            toastMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(TerminalSpacing.md)
                        .background(if (toastIsWarning) TerminalColors.statusWaiting else TerminalColors.statusOnline)
                        .padding(TerminalSpacing.md)
                ) {
                    Text(text = message.uppercase(), color = TerminalColors.background, style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth())
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
        contentPadding = PaddingValues(TerminalSpacing.md),
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
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
            item { SectionHeader(title = "STAGED_CHANGES [${status.staged.size}]", action = { TerminalTextButton(text = "UNSTAGE_ALL", onClick = { screenModel.unstageAll() }) }) }
            itemsIndexed(items = status.staged, key = { index, change -> "staged-$index-${change.path}" }) { _, change ->
                FileChangeItem(change = change, staged = true, onUnstage = { screenModel.unstageFile(change.path) }, onClick = { onNavigateToDiff(change.path, true) })
            }
        }
        if (status != null && status.unstaged.isNotEmpty()) {
            item { SectionHeader(title = "CHANGES [${status.unstaged.size}]", action = { TerminalTextButton(text = "STAGE_ALL", onClick = { screenModel.stageAll() }) }) }
            itemsIndexed(status.unstaged, key = { index, change -> "unstaged-$index-${change.path}" }) { _, change ->
                FileChangeItem(change = change, staged = false, onStage = { screenModel.stageFile(change.path) }, onDiscard = { screenModel.discardFile(change.path) }, onClick = { onNavigateToDiff(change.path, false) })
            }
        }
        if (status != null && status.untracked.isNotEmpty()) {
            item { SectionHeader(title = "UNTRACKED_FILES [${status.untracked.size}]") }
            items(status.untracked, key = { "untracked-$it" }) { path ->
                UntrackedFileItem(path = path, onStage = { screenModel.stageFile(path) })
            }
        }
        if (uiState.stagedCount > 0) {
            item { TerminalButton(text = "COMMIT ${uiState.stagedCount} FILES", onClick = { screenModel.showCommitDialog() }, icon = Icons.Default.Check) }
        }
        if (status?.clean == true) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TerminalColors.statusOnline, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("WORKING_TREE_CLEAN", style = MaterialTheme.typography.headlineSmall, color = TerminalColors.statusOnline)
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
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(TerminalSpacing.md), verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
        val localBranches = branches.filter { !it.remote }
        if (localBranches.isNotEmpty()) {
            item { SectionHeader("LOCAL_BRANCHES") }
            items(items = localBranches, key = { "local-${it.name}" }) { branch ->
                BranchItem(branch = branch, isCurrent = branch.name == currentBranch, onClick = { selectedBranch = branch.name })
            }
        }
        val remoteBranches = branches.filter { it.remote }
        if (remoteBranches.isNotEmpty()) {
            item { Spacer(Modifier.height(TerminalSpacing.lg)); SectionHeader("REMOTE_BRANCHES") }
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
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(TerminalSpacing.md), verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
        item { SectionHeader(title = "REMOTES [${remotes.size}]", action = { TerminalTextButton("ADD", onClick = { screenModel.showAddRemoteDialog() }) }) }
        items(items = remotes, key = { it.name }) { remote ->
            Row(modifier = Modifier.fillMaxWidth().border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape).padding(TerminalSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = remote.name.uppercase(), color = TerminalColors.white, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(text = remote.url, style = MaterialTheme.typography.bodySmall, color = TerminalColors.grey)
                }
                TerminalIconButton(icon = Icons.Default.Delete, onClick = { screenModel.removeRemote(remote.name) }, iconColor = TerminalColors.error)
            }
        }
    }
}

@Composable
private fun TagsTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val tags = uiState.tags
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(TerminalSpacing.md), verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
        item { SectionHeader(title = "TAGS [${tags.size}]", action = { TerminalTextButton("CREATE", onClick = { screenModel.showCreateTagDialog() }) }) }
        items(tags) { tag ->
            Row(modifier = Modifier.fillMaxWidth().border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape).padding(TerminalSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = TerminalColors.statusOnline, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(TerminalSpacing.sm))
                Text(text = tag, color = TerminalColors.white, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TerminalIconButton(icon = Icons.Default.Delete, onClick = { screenModel.deleteTag(tag) }, iconColor = TerminalColors.error)
            }
        }
    }
}

@Composable
private fun StatusHeader(status: GitStatusResponse?) {
    Column(modifier = Modifier.fillMaxWidth().border(TerminalSpacing.borderThin, TerminalColors.borderLight, RectangleShape).padding(TerminalSpacing.md)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "BRANCH: ${status?.branch ?: "UNKNOWN"}", color = TerminalColors.white, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (status?.upstream != null) { Text(text = "UPSTREAM: ${status.upstream}", color = TerminalColors.grey, style = MaterialTheme.typography.bodySmall) }
        }
        if (status?.ahead ?: 0 > 0 || status?.behind ?: 0 > 0) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (status?.ahead ?: 0 > 0) { Text("${status?.ahead} AHEAD", color = TerminalColors.statusOnline, style = MaterialTheme.typography.labelSmall) }
                if (status?.behind ?: 0 > 0) { Text("${status?.behind} BEHIND", color = TerminalColors.error, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: @Composable (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "// $title", color = TerminalColors.greyLight, style = MaterialTheme.typography.labelMedium)
        action?.invoke()
    }
}

@Composable
private fun FileChangeItem(change: GitFileChange, staged: Boolean, onStage: (() -> Unit)? = null, onUnstage: (() -> Unit)? = null, onDiscard: (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    val statusColor = when (change.status) { GitFileStatus.ADDED -> TerminalColors.statusOnline; GitFileStatus.MODIFIED -> TerminalColors.statusWaiting; GitFileStatus.DELETED -> TerminalColors.error; GitFileStatus.RENAMED -> TerminalColors.statusOffline; else -> TerminalColors.white }
    val statusChar = when (change.status) { GitFileStatus.ADDED -> "A"; GitFileStatus.MODIFIED -> "M"; GitFileStatus.DELETED -> "D"; GitFileStatus.RENAMED -> "R"; else -> "?" }
    Row(modifier = Modifier.fillMaxWidth().border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape).clickable { onClick?.invoke() }.padding(TerminalSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).background(statusColor.copy(alpha = 0.2f), RectangleShape), contentAlignment = Alignment.Center) { Text(text = statusChar, color = statusColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.width(TerminalSpacing.sm))
        Text(text = change.path, color = TerminalColors.white, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (staged) { onUnstage?.let { TerminalIconButton(icon = Icons.Default.Remove, onClick = it, contentDescription = "Unstage", iconColor = TerminalColors.greyLight) } }
        else { onStage?.let { TerminalIconButton(icon = Icons.Default.Add, onClick = it, contentDescription = "Stage", iconColor = TerminalColors.statusOnline) }; onDiscard?.let { TerminalIconButton(icon = Icons.AutoMirrored.Filled.Undo, onClick = it, contentDescription = "Discard", iconColor = TerminalColors.error) } }
    }
}

@Composable
private fun UntrackedFileItem(path: String, onStage: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape).padding(TerminalSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).background(TerminalColors.greyDark, RectangleShape), contentAlignment = Alignment.Center) { Text(text = "?", color = TerminalColors.grey, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.width(TerminalSpacing.sm))
        Text(text = path, color = TerminalColors.white, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        TerminalIconButton(icon = Icons.Default.Add, onClick = onStage, contentDescription = "Stage", iconColor = TerminalColors.statusOnline)
    }
}

@Composable
private fun BranchItem(branch: GitBranch, isCurrent: Boolean, onClick: () -> Unit) {
    val bgColor = if (isCurrent) TerminalColors.statusOnline.copy(alpha = 0.1f) else Color.Transparent
    val borderColor = if (isCurrent) TerminalColors.statusOnline else TerminalColors.border
    Row(modifier = Modifier.fillMaxWidth().background(bgColor, RectangleShape).border(TerminalSpacing.borderThin, borderColor, RectangleShape).clickable(onClick = onClick).padding(TerminalSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        if (isCurrent) { Icon(Icons.Default.Check, contentDescription = "Current", tint = TerminalColors.statusOnline, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(TerminalSpacing.sm)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = branch.name, color = if (isCurrent) TerminalColors.statusOnline else TerminalColors.white, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
            if (branch.upstream != null) { Text(text = "TRACKS: ${branch.upstream}", style = MaterialTheme.typography.labelSmall, color = TerminalColors.grey) }
        }
        if (branch.ahead > 0 || branch.behind > 0) { Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) { if (branch.ahead > 0) { Text("+${branch.ahead}", color = TerminalColors.statusOnline, style = MaterialTheme.typography.labelSmall) }; if (branch.behind > 0) { Text("-${branch.behind}", color = TerminalColors.error, style = MaterialTheme.typography.labelSmall) } } }
    }
}

@Composable
private fun CommitItem(commit: GitCommit, dateFormat: SimpleDateFormat) {
    Column(modifier = Modifier.fillMaxWidth().border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape).padding(TerminalSpacing.md)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = commit.shortHash, style = MaterialTheme.typography.bodySmall, color = TerminalColors.statusWaiting)
            Text(text = dateFormat.format(Date(commit.date)), style = MaterialTheme.typography.labelSmall, color = TerminalColors.grey)
        }
        Spacer(Modifier.height(TerminalSpacing.sm))
        Text(text = commit.message, color = TerminalColors.white, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(TerminalSpacing.sm))
        Text(text = "AUTHOR: ${commit.author}", style = MaterialTheme.typography.labelSmall, color = TerminalColors.greyLight)
    }
}

@Composable
private fun TerminalCommitDialog(message: String, onMessageChange: (String) -> Unit, onCommit: () -> Unit, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(TerminalColors.background.copy(alpha = 0.9f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth(0.9f).background(TerminalColors.background, RectangleShape).border(TerminalSpacing.borderStandard, TerminalColors.white, RectangleShape).padding(TerminalSpacing.lg)) {
            TerminalHeader(text = "COMMIT_CHANGES", showBrackets = true)
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            TerminalInput(value = message, onValueChange = onMessageChange, label = "COMMIT_MESSAGE", placeholder = "feat: implemented terminal ui", singleLine = false, modifier = Modifier.height(120.dp))
            Spacer(modifier = Modifier.height(TerminalSpacing.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)) {
                TerminalOutlinedButton(text = "CANCEL", onClick = onDismiss, modifier = Modifier.weight(1f))
                TerminalButton(text = "COMMIT", onClick = onCommit, enabled = message.isNotBlank(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CreateStashDialog(message: String, onMessageChange: (String) -> Unit, onCreate: () -> Unit, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(TerminalColors.background.copy(alpha = 0.9f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth(0.9f).background(TerminalColors.background, RectangleShape).border(TerminalSpacing.borderStandard, TerminalColors.white, RectangleShape).padding(TerminalSpacing.lg)) {
            TerminalHeader(text = "CREATE_STASH", showBrackets = true)
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            TerminalInput(value = message, onValueChange = onMessageChange, label = "STASH_MESSAGE", placeholder = "WIP: saving changes...", singleLine = false, modifier = Modifier.height(120.dp))
            Spacer(modifier = Modifier.height(TerminalSpacing.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)) {
                TerminalOutlinedButton(text = "CANCEL", onClick = onDismiss, modifier = Modifier.weight(1f))
                TerminalButton(text = "STASH", onClick = onCreate, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StashesSection(stashes: List<GitStash>, onCreateStash: () -> Unit, onPop: (Int) -> Unit, onApply: (Int) -> Unit, onDrop: (Int) -> Unit) {
    Column {
        SectionHeader(title = "STASHES [${stashes.size}]", action = { TerminalTextButton(text = "CREATE", onClick = onCreateStash) })
        stashes.forEach { stash ->
            Row(modifier = Modifier.fillMaxWidth().border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape).padding(TerminalSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "stash@{${stash.index}}", color = TerminalColors.statusWaiting, style = MaterialTheme.typography.labelSmall)
                    Text(text = stash.message, color = TerminalColors.white, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row {
                    TerminalIconButton(icon = Icons.Default.Upload, onClick = { onPop(stash.index) }, contentDescription = "Pop", iconColor = TerminalColors.statusOnline)
                    TerminalIconButton(icon = Icons.Default.PlayArrow, onClick = { onApply(stash.index) }, contentDescription = "Apply", iconColor = TerminalColors.white)
                    TerminalIconButton(icon = Icons.Default.Delete, onClick = { onDrop(stash.index) }, contentDescription = "Drop", iconColor = TerminalColors.error)
                }
            }
        }
    }
}

@Composable
private fun AddRemoteDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize().background(TerminalColors.background.copy(alpha = 0.9f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth(0.9f).background(TerminalColors.background, RectangleShape).border(TerminalSpacing.borderStandard, TerminalColors.white, RectangleShape).padding(TerminalSpacing.lg)) {
            TerminalHeader(text = "ADD_REMOTE", showBrackets = true)
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            TerminalInput(value = name, onValueChange = { name = it }, label = "REMOTE_NAME", placeholder = "origin")
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            TerminalInput(value = url, onValueChange = { url = it }, label = "REMOTE_URL", placeholder = "https://github.com/...")
            Spacer(modifier = Modifier.height(TerminalSpacing.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)) {
                TerminalOutlinedButton(text = "CANCEL", onClick = onDismiss, modifier = Modifier.weight(1f))
                TerminalButton(text = "ADD", onClick = { onAdd(name, url) }, enabled = name.isNotBlank() && url.isNotBlank(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CreateTagDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize().background(TerminalColors.background.copy(alpha = 0.9f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth(0.9f).background(TerminalColors.background, RectangleShape).border(TerminalSpacing.borderStandard, TerminalColors.white, RectangleShape).padding(TerminalSpacing.lg)) {
            TerminalHeader(text = "CREATE_TAG", showBrackets = true)
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            TerminalInput(value = name, onValueChange = { name = it }, label = "TAG_NAME", placeholder = "v1.0.0")
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            TerminalInput(value = msg, onValueChange = { msg = it }, label = "TAG_MESSAGE", placeholder = "Release version 1.0.0")
            Spacer(modifier = Modifier.height(TerminalSpacing.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)) {
                TerminalOutlinedButton(text = "CANCEL", onClick = onDismiss, modifier = Modifier.weight(1f))
                TerminalButton(text = "CREATE", onClick = { onAdd(name, msg) }, enabled = name.isNotBlank(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LogTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val commits = uiState.log?.commits ?: emptyList()
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(TerminalSpacing.md), verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
        items(items = commits, key = { it.hash }) { commit -> CommitItem(commit, dateFormat) }
        if (uiState.log?.hasMore == true) {
            item { TerminalButton(text = "LOAD MORE", onClick = { screenModel.loadLog(skip = commits.size) }, modifier = Modifier.fillMaxWidth() ) }
        }
    }
}

@Composable
private fun BranchActionDialog(branch: String, onDismiss: () -> Unit, onCheckout: () -> Unit, onMerge: () -> Unit, onRebase: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(TerminalColors.background.copy(alpha = 0.9f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth(0.8f).background(TerminalColors.background, RectangleShape).border(TerminalSpacing.borderStandard, TerminalColors.white, RectangleShape).padding(TerminalSpacing.lg)) {
            TerminalHeader(text = "BRANCH_ACTIONS", showBrackets = true)
            Text(text = branch, color = TerminalColors.statusOnline, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = TerminalSpacing.md))
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            TerminalButton(text = "CHECKOUT", onClick = onCheckout, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
            TerminalButton(text = "MERGE INTO CURRENT", onClick = onMerge, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
            TerminalButton(text = "REBASE CURRENT ONTO", onClick = onRebase, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            TerminalOutlinedButton(text = "CANCEL", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
