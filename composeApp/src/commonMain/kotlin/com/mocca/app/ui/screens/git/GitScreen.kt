package com.mocca.app.ui.screens.git

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.*
import com.mocca.app.ui.components.terminal.*
import com.mocca.app.ui.theme.*
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Git Control Center - Modern "God Mode" UI for Git operations.
 * Ported from high-fidelity HTML/Tailwind mocks.
 */
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
        
        Scaffold(
            topBar = {
                GodHeader(
                    title = selectedTab.title,
                    onBackClick = { navigator.pop() },
                    subtitle = "mobile-agent-v2",
                    subtitleIcon = {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = AppColors.white.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* More actions */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = AppColors.white)
                        }
                    }
                )
            },
            containerColor = AppColors.background,
            floatingActionButton = {
                GitFloatingBar(
                    onFetch = { screenModel.fetch() },
                    onPull = { screenModel.pull() },
                    onPush = { screenModel.push() }
                )
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tabs Navigation
                    GitTabsNavigation(selectedTab, onTabSelected = { screenModel.selectTab(it) })
                    
                    // Main Content
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                                GitTab.STATUS -> GitStatusSummary(uiState, screenModel, onNavigateToDiff = { path, staged ->
                                    navigator.push(GitDiffScreen(path, staged))
                                })
                                GitTab.BRANCHES -> BranchesTab(uiState, screenModel)
                                GitTab.LOG -> LogTab(uiState, screenModel)
                                GitTab.REMOTES -> RemotesTab(uiState, screenModel)
                                GitTab.TAGS -> TagsTab(uiState, screenModel)
                            }
                        }
                    }
                }

                // Dialogs & Overlays
                GitOverlays(uiState, screenModel)
            }
        }
    }
}

@Composable
private fun GitTabsNavigation(selectedTab: GitTab, onTabSelected: (GitTab) -> Unit) {
    Surface(
        color = AppColors.background,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, AppColors.white.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 20.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GitTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Button(
                    onClick = { onTabSelected(tab) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AppColors.primary else AppColors.white.copy(alpha = 0.05f),
                        contentColor = if (isSelected) AppColors.white else AppColors.white.copy(alpha = 0.6f)
                    ),
                    shape = AppShapes.pill,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(text = tab.title, style = AppTypography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun GitFloatingBar(
    onFetch: () -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(bottom = 16.dp),
        color = Color(0xFF1C1C1E),
        shape = AppShapes.pill,
        border = BorderStroke(1.dp, AppColors.white.copy(alpha = 0.1f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onFetch, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text("Refetch", color = AppColors.white, style = AppTypography.labelMedium, fontWeight = FontWeight.Bold)
            }
            
            Box(modifier = Modifier.size(1.dp, 16.dp).background(AppColors.white.copy(alpha = 0.1f)))
            
            TextButton(onClick = onPull, modifier = Modifier.padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pull", color = AppColors.white, style = AppTypography.labelMedium, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.size(6.dp).background(AppColors.primary, AppShapes.circle))
                }
            }

            Box(modifier = Modifier.size(1.dp, 16.dp).background(AppColors.white.copy(alpha = 0.1f)))

            TextButton(onClick = onPush, modifier = Modifier.padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Push", color = AppColors.primary, style = AppTypography.labelMedium, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AppColors.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun GitStatusSummary(
    uiState: GitUiState,
    screenModel: GitScreenModel,
    onNavigateToDiff: (String, Boolean) -> Unit
) {
    val status = uiState.status
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Status",
                style = AppTypography.displaySmall,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.white
            )
        }

        // Staged Section
        if (status != null && status.staged.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STAGED",
                        style = AppTypography.labelSmall,
                        color = AppColors.white.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                    GodBadge(
                        text = "${status.staged.size} FILES",
                        containerColor = AppColors.primary.copy(alpha = 0.2f),
                        contentColor = AppColors.primary
                    )
                }
            }
            items(status.staged) { change ->
                GodListItem(
                    title = change.path.substringAfterLast('/'),
                    subtitle = "Modified • ${change.status}",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { onNavigateToDiff(change.path, true) }
                )
            }
        }

        // Unstaged Section
        if (status != null && status.unstaged.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UNSTAGED",
                        style = AppTypography.labelSmall,
                        color = AppColors.white.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                    GodBadge(
                        text = "${status.unstaged.size} FILE",
                        containerColor = AppColors.white.copy(alpha = 0.1f),
                        contentColor = AppColors.white.copy(alpha = 0.6f)
                    )
                }
            }
            items(status.unstaged) { change ->
                GodListItem(
                    title = change.path.substringAfterLast('/'),
                    subtitle = "Modified • ${change.status}",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            tint = AppColors.alertRed,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { onNavigateToDiff(change.path, false) }
                )
            }
        }
        
        if (uiState.stagedCount > 0) {
            item { 
                Spacer(modifier = Modifier.height(16.dp))
                GodButton(
                    text = "COMMIT ${uiState.stagedCount} FILES", 
                    onClick = { screenModel.showCommitDialog() }, 
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
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
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val localBranches = branches.filter { !it.remote }
        if (localBranches.isNotEmpty()) {
            item {
                Text(
                    text = "LOCAL BRANCHES",
                    style = AppTypography.labelSmall,
                    color = AppColors.white.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
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
}

@Composable
private fun LogTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val commits = uiState.log?.commits ?: emptyList()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(items = commits, key = { _, commit -> commit.hash }) { index, commit ->
            LogTimelineItem(
                commit = commit,
                isLast = index == commits.size - 1,
                hasMore = uiState.log?.hasMore == true
            )
        }
        
        if (uiState.log?.hasMore == true) {
            item {
                Box(modifier = Modifier.padding(start = 20.dp, top = 16.dp)) {
                    GodButton(
                        text = "LOAD MORE",
                        onClick = { screenModel.loadLog(skip = commits.size) },
                        containerColor = AppColors.white.copy(alpha = 0.05f),
                        contentColor = AppColors.white
                    )
                }
            }
        }
    }
}

@Composable
private fun LogTimelineItem(
    commit: GitCommit,
    isLast: Boolean,
    hasMore: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Column
        Column(
            modifier = Modifier.width(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppColors.surfaceElevated, AppShapes.circle)
                    .border(BorderStroke(1.dp, AppColors.white.copy(alpha = 0.1f)), AppShapes.circle),
                contentAlignment = Alignment.Center
            ) {
                if (commit.author.contains("Bot", ignoreCase = true)) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = AppColors.white.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    // Placeholder for avatar
                    Text(
                        text = commit.author.take(1).uppercase(),
                        style = AppTypography.labelMedium,
                        color = AppColors.white
                    )
                }
            }
            
            // Connecting line
            if (!isLast || hasMore) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp)
                        .background(AppColors.white.copy(alpha = 0.1f))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Content Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = commit.message,
                        style = AppTypography.titleSmall,
                        color = AppColors.white,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (commit.author.contains("Bot", ignoreCase = true)) {
                        GodBadge(
                            text = "BOT",
                            containerColor = AppColors.primary.copy(alpha = 0.2f),
                            contentColor = AppColors.primary
                        )
                    }
                }
                Text(
                    text = commit.shortHash,
                    style = AppTypography.codeSmall,
                    color = AppColors.white.copy(alpha = 0.3f),
                    modifier = Modifier
                        .background(AppColors.white.copy(alpha = 0.05f), AppShapes.badge)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = commit.message,
                style = AppTypography.bodySmall,
                color = AppColors.white.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = commit.author,
                    style = AppTypography.labelSmall,
                    color = AppColors.white.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Medium
                )
                Box(modifier = Modifier.size(4.dp).background(AppColors.white.copy(alpha = 0.2f), AppShapes.circle))
                Text(
                    text = "3 min ago", 
                    style = AppTypography.labelSmall,
                    color = AppColors.white.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun RemotesTab(uiState: GitUiState, screenModel: GitScreenModel) {
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
                    contentColor = AppColors.white,
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
private fun TagsTab(uiState: GitUiState, screenModel: GitScreenModel) {
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
                    contentColor = AppColors.white,
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

@Composable
private fun GitOverlays(uiState: GitUiState, screenModel: GitScreenModel) {
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
                .fillMaxSize()
                .padding(bottom = 100.dp),
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
                        text = message.uppercase(),
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
private fun BranchActionDialog(branch: String, onDismiss: () -> Unit, onCheckout: () -> Unit, onMerge: () -> Unit, onRebase: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceElevated,
        title = { Text("BRANCH ACTIONS", style = AppTypography.labelLarge, color = AppColors.white) },
        text = {
            Column {
                Text(text = branch, color = AppColors.statusOnline, style = AppTypography.bodyMedium, modifier = Modifier.padding(vertical = AppSpacing.md))
                Spacer(modifier = Modifier.height(AppSpacing.md))
                GodButton(text = "CHECKOUT", onClick = onCheckout, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                GodButton(text = "MERGE INTO CURRENT", onClick = onMerge, containerColor = AppColors.white.copy(alpha = 0.05f), contentColor = AppColors.white, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                GodButton(text = "REBASE CURRENT ONTO", onClick = onRebase, containerColor = AppColors.white.copy(alpha = 0.05f), contentColor = AppColors.white, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {},
        dismissButton = {
            TerminalTextButton(text = "CANCEL", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        },
        shape = AppShapes.dialog
    )
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
            GodButton(text = "COMMIT", onClick = onCommit, enabled = message.isNotBlank())
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
            GodButton(text = "STASH", onClick = onCreate)
        },
        dismissButton = {
            TerminalTextButton(text = "CANCEL", onClick = onDismiss)
        },
        shape = AppShapes.dialog
    )
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
            GodButton(text = "ADD", onClick = { onAdd(name, url) }, enabled = name.isNotBlank() && url.isNotBlank())
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
            GodButton(text = "CREATE", onClick = { onAdd(name, msg) }, enabled = name.isNotBlank())
        },
        dismissButton = {
            TerminalTextButton(text = "CANCEL", onClick = onDismiss)
        },
        shape = AppShapes.dialog
    )
}