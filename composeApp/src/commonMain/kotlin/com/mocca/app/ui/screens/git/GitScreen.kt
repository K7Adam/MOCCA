package com.mocca.app.ui.screens.git

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.*
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.*
import com.mocca.app.domain.model.*
import kotlinx.coroutines.delay

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
                    modifier = Modifier.background(AppColors.surfaceContainer, AppShapes.none),
                    subtitle = uiState.currentBranch,
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
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = AppColors.textPrimary)
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
            GitTabsNavigation(
                                selectedTab,
                                onTabSelected = { screenModel.selectTab(it) },
                                modifier = Modifier.background(AppColors.surfaceContainer, AppShapes.none)
                            )                    
                    // Main Content
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            uiState.isLoading -> LoadingScreen()
                            uiState.isNotGitRepo -> NotGitRepoScreen()
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
                                GitTab.STASHES -> StashesTab(uiState, screenModel)
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
private fun GitTabsNavigation(
    selectedTab: GitTab, 
    onTabSelected: (GitTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(0.5.dp, AppColors.white.copy(alpha = 0.05f))
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
                        contentColor = if (isSelected) AppColors.textPrimary else AppColors.white.copy(alpha = 0.6f)
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
        color = AppColors.surfaceContainerHigh,
        shape = AppShapes.pill,
        border = BorderStroke(1.dp, AppColors.white.copy(alpha = 0.1f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onFetch, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text("Refetch", color = AppColors.textPrimary, style = AppTypography.labelMedium, fontWeight = FontWeight.Bold)
            }
            
            Box(modifier = Modifier.size(1.dp, 16.dp).background(AppColors.white.copy(alpha = 0.1f)))
            
            TextButton(onClick = onPull, modifier = Modifier.padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pull", color = AppColors.textPrimary, style = AppTypography.labelMedium, fontWeight = FontWeight.Bold)
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
private fun NotGitRepoScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOff,
                contentDescription = null,
                tint = AppColors.white.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "NOT A GIT REPOSITORY",
                style = AppTypography.headlineSmall,
                color = AppColors.white.copy(alpha = 0.4f)
            )
            Text(
                text = "The server's working directory is not\nunder version control.",
                style = AppTypography.bodySmall,
                color = AppColors.white.copy(alpha = 0.25f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
