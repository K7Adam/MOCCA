package com.mocca.app.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.screens.workspace.WorkspaceScreen
import com.mocca.app.ui.screens.settings.SettingsScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

class SessionsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<SessionsScreenModel>()
        val state by screenModel.state.collectAsState()
        
        LaunchedEffect(Unit) {
            screenModel.navigationEvent.collect { sessionId ->
                navigator.push(WorkspaceScreen(sessionId))
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Terminal Header Row
                Surface(
                    color = AppColors.surfaceContainer,
                    shape = AppShapes.none,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModernHeader(
                    text = "SESSIONS", 
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(AppSpacing.md))
                
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    // ═══════════════════════════════════════════════════════════════════════════════
                    // SESSION SEARCH (Priority 5.6) - Search toggle button
                    // ═══════════════════════════════════════════════════════════════════════════════
                    MoccaIconButton(
                        icon = Icons.Default.Search,
                        onClick = { screenModel.toggleSearch() },
                        contentDescription = "Search",
                        iconColor = if (state.isSearchVisible) AppColors.statusOnline else AppColors.onSurfaceVariant
                    )
                    MoccaIconButton(
                        icon = Icons.Default.Settings,
                        onClick = { navigator.push(SettingsScreen()) },
                        contentDescription = stringResource(Res.string.settings)
                    )
                    MoccaIconButton(
                        icon = Icons.Default.Refresh,
                        onClick = { screenModel.refresh() },
                        contentDescription = stringResource(Res.string.refresh)
                    )
                }
                    }
                }
            
            // ═══════════════════════════════════════════════════════════════════════════════
            // SESSION SEARCH (Priority 5.6) - Search bar
            // ═══════════════════════════════════════════════════════════════════════════════
            if (state.isSearchVisible) {
                ModernSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { screenModel.updateSearchQuery(it) },
                    onClear = { screenModel.clearSearch() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                )
            }
            
            HorizontalDivider(
                thickness = AppSpacing.borderThin,
                color = AppColors.outline
            )
            
            // Main Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val connectionState = state.connectionState) {
                    is ConnectionStatus.NotConfigured -> {
                        TerminalNotConnectedContent(
                            title = stringResource(Res.string.no_server_configured),
                            message = stringResource(Res.string.configure_server_hint),
                            onConfigureClick = { navigator.push(SettingsScreen()) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is ConnectionStatus.Connecting -> {
                        ModernConnectionProgressContent(
                            message = stringResource(Res.string.checking_connection),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is ConnectionStatus.WaitingForNetwork -> {
                        TerminalNotConnectedContent(
                            title = stringResource(Res.string.waiting_for_network),
                            message = stringResource(Res.string.network_unavailable_hint),
                            onConfigureClick = { navigator.push(SettingsScreen()) },
                            onRetryClick = { screenModel.retryConnection() },
                            modifier = Modifier.align(Alignment.Center),
                            icon = Icons.Default.WifiOff
                        )
                    }
                    is ConnectionStatus.Reconnecting -> {
                        ModernConnectionProgressContent(
                            message = stringResource(Res.string.reconnecting, connectionState.attempt, connectionState.maxAttempts),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is ConnectionStatus.Disconnected -> {
                        TerminalNotConnectedContent(
                            title = stringResource(Res.string.connection_failed_title),
                            message = connectionState.reason ?: stringResource(Res.string.connection_failed_message),
                            onConfigureClick = { navigator.push(SettingsScreen()) },
                            onRetryClick = { screenModel.retryConnection() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is ConnectionStatus.Error -> {
                        TerminalNotConnectedContent(
                            title = stringResource(Res.string.connection_failed_title),
                            message = connectionState.message,
                            onConfigureClick = { navigator.push(SettingsScreen()) },
                            onRetryClick = { screenModel.retryConnection() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is ConnectionStatus.Connected -> {
                        when {
                            state.isLoading && state.sessions.isEmpty() -> {
                                ModernProcessingIndicator()
                            }
                            state.sessions.isEmpty() -> {
                                ModernEmptySessionsContent(
                                    onCreateClick = { screenModel.createSession() },
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            state.filteredSessions.isEmpty() && state.searchQuery.isNotBlank() -> {
                                // No search results
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(AppSpacing.xxl),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = AppColors.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(AppSpacing.md))
                                    Text(
                                        text = "NO_RESULTS_FOR: \"${state.searchQuery}\"",
                                        style = AppTypography.bodyMedium,
                                        color = AppColors.onSurfaceVariantLight
                                    )
                                }
                            }
                            else -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    val filteredSessions = state.filteredSessions
                                    val childrenMap = state.childrenMap
                                    val selectedSessionId = state.selectedSessionId
                                    
                                    ModernSessionsList(
                                        sessions = filteredSessions,
                                        childrenMap = childrenMap,
                                        selectedSessionId = selectedSessionId,
                                        onSessionClick = { session ->
                                            screenModel.selectSession(session.id)
                                            navigator.push(WorkspaceScreen(session.id))
                                        },
                                        onDeleteClick = { session ->
                                            screenModel.deleteSession(session.id)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    // New Session Button at bottom
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(AppSpacing.md)
                                    ) {
                                        MoccaButton(
                                            text = "NEW SESSION",
                                            onClick = { screenModel.createSession() },
                                            icon = Icons.Default.Add
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (state.isLoading && state.sessions.isNotEmpty()) {
                            LoadingIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                color = AppColors.statusWaiting
                            )
                        }
                    }
                }
            }
        }
    }
}
