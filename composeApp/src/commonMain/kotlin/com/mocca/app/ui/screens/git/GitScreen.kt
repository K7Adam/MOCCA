package com.mocca.app.ui.screens.git

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.unit.IntSize
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
                            modifier = Modifier.size(AppSpacing.iconSizeSmall)
                        )
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .size(AppSpacing.iconButtonSize)
                                .moccaClickable(onClick = { /* More actions */ }, pressedScale = 0.92f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = AppColors.onSurface)
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
                .padding(vertical = AppSpacing.lg, horizontal = AppSpacing.cardPaddingLarge)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            GitTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Button(
                    onClick = { onTabSelected(tab) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AppColors.primary else AppColors.white.copy(alpha = 0.05f),
                        contentColor = if (isSelected) AppColors.onSurface else AppColors.white.copy(alpha = 0.6f)
                    ),
                    shape = AppShapes.pill,
                    contentPadding = PaddingValues(horizontal = AppSpacing.cardPaddingLarge, vertical = AppSpacing.sm)
                ) {
                    Text(text = tab.title, style = AppTypography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GitFloatingBar(
    onFetch: () -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit
) {
    Box(modifier = Modifier.padding(bottom = AppSpacing.lg)) {
        Row(
            modifier = Modifier.animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Button(
                onClick = onFetch,
                shape = AppShapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.surfaceContainerHigh,
                    contentColor = AppColors.onSurface
                ),
                border = BorderStroke(AppSpacing.borderThin, AppColors.white.copy(alpha = 0.1f))
            ) {
                Text("Refetch", style = AppTypography.labelMedium, fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = onPull,
                shape = AppShapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.surfaceContainerHigh,
                    contentColor = AppColors.onSurface
                ),
                border = BorderStroke(AppSpacing.borderThin, AppColors.white.copy(alpha = 0.1f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text("Pull", style = AppTypography.labelMedium, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.size(AppSpacing.statusDotSizeLarge).background(AppColors.primary, AppShapes.circle))
                }
            }

            Button(
                onClick = onPush,
                shape = AppShapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.surfaceContainerHigh,
                    contentColor = AppColors.primary
                ),
                border = BorderStroke(AppSpacing.borderThin, AppColors.white.copy(alpha = 0.1f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text("Push", style = AppTypography.labelMedium, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AppColors.primary, modifier = Modifier.size(AppSpacing.iconSizeSmall))
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
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOff,
                contentDescription = null,
                tint = AppColors.white.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Not a git repository",
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
