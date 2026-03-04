package com.mocca.app.ui.screens.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.GodHeader
import com.mocca.app.ui.theme.*

/**
 * Terminal screen with multi-tab support.
 * W3-T1: Multiple terminal tabs via tab bar
 * W3-T2: Terminal lifecycle — resize on layout change, close (delete) terminal
 */
class TerminalScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<TerminalScreenModel>()
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                GodHeader(
                    title = "TERMINAL",
                    onBackClick = { navigator.pop() },
                    subtitle = state.activeTab?.let { "${state.cols}×${state.rows}" },
                    actions = {
                        // New tab button
                        IconButton(
                            onClick = { screenModel.createTab() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (state.isCreatingTab) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = AppColors.accent,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "New terminal",
                                    tint = AppColors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
            },
            containerColor = AppColors.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ── TAB BAR ──────────────────────────────────────────────────
                if (state.tabs.isNotEmpty()) {
                    TerminalTabBar(
                        tabs = state.tabs,
                        activeTabId = state.activeTabId,
                        onTabSelected = { screenModel.selectTab(it) },
                        onTabClosed = { screenModel.closeTab(it) }
                    )
                }

                // ── CONTENT AREA ─────────────────────────────────────────────
                when {
                    state.isLoadingTabs -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = AppColors.accent,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "LOADING TERMINALS...",
                                    style = AppTypography.labelSmall,
                                    color = AppColors.textSecondary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                    state.tabs.isEmpty() -> {
                        TerminalEmptyState(
                            isCreating = state.isCreatingTab,
                            onCreateClick = { screenModel.createTab() }
                        )
                    }
                    state.activeTab != null -> {
                        val currentTab = state.activeTab!!
                        TerminalContent(
                            tab = currentTab,
                            currentCols = state.cols,
                            currentRows = state.rows,
                            onInput = { input -> screenModel.sendInput(currentTab.terminal.id, input) },
                            onResize = { cols, rows -> screenModel.notifyResize(cols, rows) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "SELECT A TERMINAL TAB",
                                style = AppTypography.labelMedium,
                                color = AppColors.textTertiary,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
