package com.mocca.app.ui.screens.git

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.mocca.app.domain.model.GitBranch
import com.mocca.app.domain.model.GitCommit
import com.mocca.app.domain.model.GitFileChange
import com.mocca.app.domain.model.GitFileStatus
import com.mocca.app.domain.model.GitStatusResponse
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
        
        // Simple status message display
        val statusMessage = uiState.operationResult ?: uiState.error
        
        // Clear message after showing
        LaunchedEffect(uiState.operationResult) {
            if (uiState.operationResult != null) {
                kotlinx.coroutines.delay(3000)
                screenModel.clearOperationResult()
            }
        }
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TerminalColors.background)
                    .padding(TerminalSpacing.lg)
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
                
                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
                    TerminalIconButton(icon = Icons.Default.Sync, onClick = { screenModel.fetch() }, contentDescription = "FETCH")
                    TerminalIconButton(icon = Icons.Default.Download, onClick = { screenModel.pull() }, contentDescription = "PULL")
                    TerminalIconButton(icon = Icons.Default.Upload, onClick = { screenModel.push() }, contentDescription = "PUSH")
                }
            }
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            // Tabs
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
                            .background(
                                if (isSelected) TerminalColors.white else Color.Transparent, 
                                RectangleShape
                            )
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
            
            // Content
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
                        GitTab.STATUS -> StatusTab(uiState, screenModel)
                        GitTab.BRANCHES -> BranchesTab(uiState, screenModel)
                        GitTab.LOG -> LogTab(uiState, screenModel)
                        GitTab.REMOTES -> RemotesTab(uiState)
                    }
                }
                
                // Overlay Commit Dialog
                if (uiState.showCommitDialog) {
                    TerminalCommitDialog(
                        message = uiState.commitMessage,
                        onMessageChange = { screenModel.updateCommitMessage(it) },
                        onCommit = { screenModel.commit(uiState.commitMessage) },
                        onDismiss = { screenModel.hideCommitDialog() }
                    )
                }
            }
            } // End Column
            
            // Status toast overlay at bottom (inside outer Box for BoxScope.align)
            statusMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(TerminalSpacing.md)
                        .background(
                            if (uiState.error != null) TerminalColors.error 
                            else TerminalColors.statusOnline
                        )
                        .padding(TerminalSpacing.md)
                ) {
                    Text(
                        text = message.uppercase(),
                        color = TerminalColors.background,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } // End outer Box
    }
}

@Composable
private fun StatusTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val status = uiState.status
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TerminalSpacing.md),
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
    ) {
        // Status Header Info
        item(contentType = "status-header") {
            StatusHeader(status)
        }
        
        // Staged changes
        if (status != null && status.staged.isNotEmpty()) {
            item(key = "staged-header", contentType = "section-header") {
                SectionHeader(
                    title = "STAGED_CHANGES [${status.staged.size}]",
                    action = { 
                        TerminalTextButton(text = "UNSTAGE_ALL", onClick = { screenModel.unstageAll() })
                    }
                )
            }
            items(
                items = status.staged,
                key = { "staged-${it.path}" },
                contentType = { "file-change" }
            ) { change ->
                FileChangeItem(
                    change = change,
                    staged = true,
                    onUnstage = { screenModel.unstageFile(change.path) }
                )
            }
        }
        
        // Unstaged changes
        if (status != null && status.unstaged.isNotEmpty()) {
            item(key = "unstaged-header") {
                SectionHeader(
                    title = "CHANGES [${status.unstaged.size}]",
                    action = {
                        TerminalTextButton(text = "STAGE_ALL", onClick = { screenModel.stageAll() })
                    }
                )
            }
            items(status.unstaged, key = { "unstaged-${it.path}" }) { change ->
                FileChangeItem(
                    change = change,
                    staged = false,
                    onStage = { screenModel.stageFile(change.path) },
                    onDiscard = { screenModel.discardFile(change.path) }
                )
            }
        }
        
        // Untracked files
        if (status != null && status.untracked.isNotEmpty()) {
            item(key = "untracked-header") {
                SectionHeader(title = "UNTRACKED_FILES [${status.untracked.size}]")
            }
            items(status.untracked, key = { "untracked-$it" }) { path ->
                UntrackedFileItem(
                    path = path,
                    onStage = { screenModel.stageFile(path) }
                )
            }
        }
        
        // Commit button
        if (uiState.stagedCount > 0) {
            item {
                TerminalButton(
                    text = "COMMIT ${uiState.stagedCount} FILES",
                    onClick = { screenModel.showCommitDialog() },
                    icon = Icons.Default.Check
                )
            }
        }
        
        // Clean state message
        if (status?.clean == true) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TerminalColors.statusOnline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "WORKING_TREE_CLEAN",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TerminalColors.statusOnline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHeader(status: GitStatusResponse?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(TerminalSpacing.borderThin, TerminalColors.borderLight, RectangleShape)
            .padding(TerminalSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "BRANCH: ${status?.branch ?: "UNKNOWN"}",
                color = TerminalColors.white,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (status?.upstream != null) {
                Text(
                    text = "UPSTREAM: ${status.upstream}",
                    color = TerminalColors.grey,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        if (status?.ahead ?: 0 > 0 || status?.behind ?: 0 > 0) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (status?.ahead ?: 0 > 0) {
                    Text(
                        "${status?.ahead} AHEAD",
                        color = TerminalColors.statusOnline,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (status?.behind ?: 0 > 0) {
                    Text(
                        "${status?.behind} BEHIND",
                        color = TerminalColors.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "// $title",
            color = TerminalColors.greyLight,
            style = MaterialTheme.typography.labelMedium
        )
        action?.invoke()
    }
}

@Composable
private fun FileChangeItem(
    change: GitFileChange,
    staged: Boolean,
    onStage: (() -> Unit)? = null,
    onUnstage: (() -> Unit)? = null,
    onDiscard: (() -> Unit)? = null
) {
    val statusColor = when (change.status) {
        GitFileStatus.ADDED -> TerminalColors.statusOnline
        GitFileStatus.MODIFIED -> TerminalColors.statusWaiting
        GitFileStatus.DELETED -> TerminalColors.error
        GitFileStatus.RENAMED -> TerminalColors.statusOffline
        else -> TerminalColors.white
    }
    
    val statusChar = when (change.status) {
        GitFileStatus.ADDED -> "A"
        GitFileStatus.MODIFIED -> "M"
        GitFileStatus.DELETED -> "D"
        GitFileStatus.RENAMED -> "R"
        else -> "?"
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape)
            .padding(TerminalSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(statusColor.copy(alpha = 0.2f), RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statusChar,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(Modifier.width(TerminalSpacing.sm))
        
        Text(
            text = change.path,
            color = TerminalColors.white,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Actions
        if (staged) {
            onUnstage?.let {
                TerminalIconButton(
                    icon = Icons.Default.Remove, 
                    onClick = it, 
                    contentDescription = "Unstage",
                    iconColor = TerminalColors.greyLight
                )
            }
        } else {
            onStage?.let {
                TerminalIconButton(
                    icon = Icons.Default.Add, 
                    onClick = it, 
                    contentDescription = "Stage",
                    iconColor = TerminalColors.statusOnline
                )
            }
            onDiscard?.let {
                TerminalIconButton(
                    icon = Icons.AutoMirrored.Filled.Undo, 
                    onClick = it, 
                    contentDescription = "Discard",
                    iconColor = TerminalColors.error
                )
            }
        }
    }
}

@Composable
private fun UntrackedFileItem(path: String, onStage: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape)
            .padding(TerminalSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(TerminalColors.greyDark, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "?",
                color = TerminalColors.grey,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(Modifier.width(TerminalSpacing.sm))
        
        Text(
            text = path,
            color = TerminalColors.white,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        TerminalIconButton(
            icon = Icons.Default.Add, 
            onClick = onStage, 
            contentDescription = "Stage",
            iconColor = TerminalColors.statusOnline
        )
    }
}

@Composable
private fun BranchesTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val branches = uiState.branches
    val currentBranch = uiState.currentBranch
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TerminalSpacing.md),
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
    ) {
        // Local branches
        val localBranches = branches.filter { !it.remote }
        if (localBranches.isNotEmpty()) {
            item(key = "local-header", contentType = "section-header") { SectionHeader("LOCAL_BRANCHES") }
            items(
                items = localBranches,
                key = { "local-${it.name}" },
                contentType = { "branch" }
            ) { branch ->
                BranchItem(
                    branch = branch,
                    isCurrent = branch.name == currentBranch,
                    onCheckout = { screenModel.checkout(branch.name) }
                )
            }
        }
        
        // Remote branches
        val remoteBranches = branches.filter { it.remote }
        if (remoteBranches.isNotEmpty()) {
            item(key = "remote-header", contentType = "section-header") { 
                Spacer(Modifier.height(TerminalSpacing.lg))
                SectionHeader("REMOTE_BRANCHES") 
            }
            items(
                items = remoteBranches,
                key = { "remote-${it.name}" },
                contentType = { "branch" }
            ) { branch ->
                BranchItem(
                    branch = branch,
                    isCurrent = false,
                    onCheckout = { screenModel.checkout(branch.name) }
                )
            }
        }
    }
}

@Composable
private fun BranchItem(
    branch: GitBranch,
    isCurrent: Boolean,
    onCheckout: () -> Unit
) {
    val bgColor = if (isCurrent) TerminalColors.statusOnline.copy(alpha = 0.1f) else Color.Transparent
    val borderColor = if (isCurrent) TerminalColors.statusOnline else TerminalColors.border
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RectangleShape)
            .border(TerminalSpacing.borderThin, borderColor, RectangleShape)
            .clickable { if (!isCurrent) onCheckout() }
            .padding(TerminalSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrent) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Current",
                tint = TerminalColors.statusOnline,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(TerminalSpacing.sm))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = branch.name,
                color = if (isCurrent) TerminalColors.statusOnline else TerminalColors.white,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            )
            if (branch.upstream != null) {
                Text(
                    text = "TRACKS: ${branch.upstream}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalColors.grey
                )
            }
        }
        
        if (branch.ahead > 0 || branch.behind > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
                if (branch.ahead > 0) {
                    Text("+${branch.ahead}", color = TerminalColors.statusOnline, style = MaterialTheme.typography.labelSmall)
                }
                if (branch.behind > 0) {
                    Text("-${branch.behind}", color = TerminalColors.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun LogTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val commits = uiState.log?.commits ?: emptyList()
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TerminalSpacing.md),
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
    ) {
        items(
            items = commits,
            key = { it.hash },
            contentType = { "commit" }
        ) { commit ->
            CommitItem(commit, dateFormat)
        }
        
        if (uiState.log?.hasMore == true) {
            item(key = "load-more", contentType = "load-more") {
                TerminalButton(
                    text = "LOAD MORE",
                    onClick = { 
                        screenModel.loadLog(
                            skip = commits.size
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CommitItem(commit: GitCommit, dateFormat: SimpleDateFormat) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape)
            .padding(TerminalSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = commit.shortHash,
                style = MaterialTheme.typography.bodySmall,
                color = TerminalColors.statusWaiting
            )
            Text(
                text = dateFormat.format(Date(commit.date)),
                style = MaterialTheme.typography.labelSmall,
                color = TerminalColors.grey
            )
        }
        
        Spacer(Modifier.height(TerminalSpacing.sm))
        
        Text(
            text = commit.message,
            color = TerminalColors.white,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(Modifier.height(TerminalSpacing.sm))
        
        Text(
            text = "AUTHOR: ${commit.author}",
            style = MaterialTheme.typography.labelSmall,
            color = TerminalColors.greyLight
        )
    }
}

@Composable
private fun RemotesTab(uiState: GitUiState) {
    val remotes = uiState.remotes
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TerminalSpacing.md),
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
    ) {
        items(
            items = remotes,
            key = { it.name },
            contentType = { "remote" }
        ) { remote ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape)
                    .padding(TerminalSpacing.md)
            ) {
                Text(
                    text = remote.name.uppercase(),
                    color = TerminalColors.white,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(TerminalSpacing.xs))
                Text(
                    text = remote.url,
                style = MaterialTheme.typography.bodySmall,
                    color = TerminalColors.grey
                )
            }
        }
        
        if (remotes.isEmpty()) {
            item(contentType = "empty-state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "NO_REMOTES_CONFIGURED",
                        color = TerminalColors.grey,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalCommitDialog(
    message: String,
    onMessageChange: (String) -> Unit,
    onCommit: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalColors.background.copy(alpha = 0.9f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(TerminalColors.background, RectangleShape)
                .border(TerminalSpacing.borderStandard, TerminalColors.white, RectangleShape)
                .padding(TerminalSpacing.lg)
        ) {
            TerminalHeader(text = "COMMIT_CHANGES", showBrackets = true)
            
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            
            TerminalInput(
                value = message,
                onValueChange = onMessageChange,
                label = "COMMIT_MESSAGE",
                placeholder = "feat: implemented terminal ui",
                singleLine = false,
                modifier = Modifier.height(120.dp)
            )
            
            Spacer(modifier = Modifier.height(TerminalSpacing.xl))
            
            Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)) {
                TerminalOutlinedButton(
                    text = "CANCEL",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                TerminalButton(
                    text = "COMMIT",
                    onClick = onCommit,
                    enabled = message.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
