package com.mocca.app.ui.screens.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                    title = "Terminal",
                    onBackClick = { navigator.pop() },
                    subtitle = state.activeTab?.let { "${state.cols}×${state.rows}" },
                    actions = {
                        // New tab button
                        Box(
                            modifier = Modifier
                                .size(AppSpacing.iconButtonSize)
                                .moccaClickable(
                                    onClick = { screenModel.createTab() },
                                    enabled = !state.isCreatingTab,
                                    pressedScale = 0.92f
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isCreatingTab) {
                                LoadingIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = AppColors.primary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "New terminal",
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(AppSpacing.iconSizeMedium)
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

                if (state.tabs.isNotEmpty()) {
                    TerminalTabBar(
                        tabs = state.tabs,
                        activeTabId = state.activeTabId,
                        onTabSelected = { screenModel.selectTab(it) },
                        onTabClosed = { screenModel.closeTab(it) }
                    )
                }

                when {
                    state.isLoadingTabs -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LoadingIndicator(
                                    color = AppColors.primary,
                                    modifier = Modifier.size(AppSpacing.xxl)
                                )
                                Spacer(Modifier.height(AppSpacing.md))
                                Text(
                                    "Loading terminals...",
                                    style = AppTypography.labelSmall,
                                    color = AppColors.onSurfaceVariant
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
                                "Select a terminal tab",
                                style = AppTypography.labelMedium,
                                color = AppColors.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
