package com.mocca.app.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.Session
import com.mocca.app.domain.model.SessionStatus
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.screens.workspace.WorkspaceScreen
import com.mocca.app.ui.screens.settings.SettingsScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.Text

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
        ) {
            // Terminal Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                        iconColor = if (state.isSearchVisible) AppColors.statusOnline else AppColors.grey
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
                color = AppColors.border
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
                                        tint = AppColors.grey
                                    )
                                    Spacer(modifier = Modifier.height(AppSpacing.md))
                                    Text(
                                        text = "NO_RESULTS_FOR: \"${state.searchQuery}\"",
                                        style = AppTypography.bodyMedium,
                                        color = AppColors.greyLight
                                    )
                                }
                            }
                            else -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    ModernSessionsList(
                                        sessions = state.filteredSessions,
                                        childrenMap = state.childrenMap,
                                        selectedSessionId = state.selectedSessionId,
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
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                color = AppColors.statusWaiting,
                                trackColor = AppColors.border
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernConnectionProgressContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = AppColors.statusWaiting,
            strokeWidth = 1.5.dp,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(AppSpacing.md))
        Text(
            text = message.uppercase(),
            style = AppTypography.labelSmall,
            color = AppColors.greyLight
        )
    }
}

@Composable
private fun ModernEmptySessionsContent(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.no_sessions).uppercase(),
            style = AppTypography.headlineSmall,
            color = AppColors.white
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = stringResource(Res.string.no_sessions_hint),
            style = AppTypography.bodySmall,
            color = AppColors.greyLight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        MoccaButton(
            text = "NEW SESSION",
            onClick = onCreateClick,
            icon = Icons.Default.Add,
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}

@Composable
private fun ModernSessionsList(
    sessions: List<Session>,
    childrenMap: Map<String, List<Session>>,
    selectedSessionId: String?,
    onSessionClick: (Session) -> Unit,
    onDeleteClick: (Session) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        items(
            items = sessions,
            key = { it.id },
            contentType = { "session" }
        ) { session ->
            MoccaSessionCard(
                session = session,
                childSessions = childrenMap[session.id] ?: emptyList(),
                isSelected = session.id == selectedSessionId,
                onClick = { onSessionClick(session) },
                onDeleteClick = { onDeleteClick(session) }
            )
        }
    }
}

@Composable
private fun MoccaSessionCard(
    session: Session,
    childSessions: List<Session>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val borderColor = if (isSelected) AppColors.statusOnline else AppColors.border
    val bgColor = if (isSelected) AppColors.statusOnline.copy(alpha = 0.05f) else Color.Transparent
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(bgColor, AppShapes.medium)
            .border(AppSpacing.borderThin, borderColor, AppShapes.medium)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active indicator dot
            if (isSelected) {
                StatusDot(color = AppColors.statusOnline, size = 6.dp)
                Spacer(modifier = Modifier.width(AppSpacing.sm))
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = (session.title ?: stringResource(Res.string.untitled_session)).uppercase(),
                    style = AppTypography.labelMedium,
                    color = if (isSelected) AppColors.statusOnline else AppColors.white,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    ModernStatusChip(status = session.status)
                    Text(
                        text = formatTime(session.updatedAt),
                        style = AppTypography.labelExtraSmall,
                        color = AppColors.grey
                    )
                    session.summary?.let { summary ->
                        if (summary.files > 0) {
                            Text(
                                text = "• ${summary.files}F",
                                style = AppTypography.labelExtraSmall,
                                color = AppColors.greyLight
                            )
                        }
                    }
                }
            }
            
            MoccaIconButton(
                icon = Icons.Default.Delete,
                onClick = onDeleteClick,
                iconColor = AppColors.error.copy(alpha = 0.6f),
                size = 32.dp,
                contentDescription = stringResource(Res.string.delete_session)
            )
        }
    }
}

@Composable
private fun ModernStatusChip(status: SessionStatus) {
    val (color, textRes) = when (status) {
        SessionStatus.IDLE -> AppColors.grey to Res.string.session_idle
        SessionStatus.RUNNING -> AppColors.statusOnline to Res.string.session_running
        SessionStatus.COMPLETED -> AppColors.success to Res.string.session_completed
        SessionStatus.ERROR -> AppColors.error to Res.string.session_error
    }
    
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), AppShapes.small)
            .padding(horizontal = AppSpacing.xs, vertical = 1.dp)
    ) {
        Text(
            text = stringResource(textRes).uppercase(),
            style = AppTypography.labelExtraSmall,
            color = color
        )
    }
}


private fun formatTime(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        ""
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SESSION SEARCH (Priority 5.6) - Search bar component
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ModernSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Box(
        modifier = modifier
            .background(AppColors.surface.copy(alpha = 0.8f), AppShapes.medium)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.medium)
            .padding(AppSpacing.xs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AppColors.grey
            )
            
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = AppTypography.labelSmall.copy(
                    color = AppColors.white
                ),
                cursorBrush = SolidColor(AppColors.accentGreen),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* Already filtering live */ }),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "SEARCH...",
                                style = AppTypography.labelSmall,
                                color = AppColors.grey
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            if (query.isNotEmpty()) {
                MoccaIconButton(
                    icon = Icons.Default.Clear,
                    onClick = onClear,
                    iconColor = AppColors.grey,
                    size = 28.dp,
                    contentDescription = "Clear"
                )
            }
        }
    }
}

@Composable
private fun TerminalNotConnectedContent(
    title: String,
    message: String,
    onConfigureClick: () -> Unit,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.CloudOff
) {
    Column(
        modifier = modifier.padding(AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = AppColors.grey
        )
        Spacer(modifier = Modifier.height(AppSpacing.md))
        Text(
            text = title.uppercase(),
            style = AppTypography.headlineSmall,
            color = AppColors.white
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = message,
            style = AppTypography.bodySmall,
            color = AppColors.greyLight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            MoccaButton(
                text = "CONFIGURE",
                onClick = onConfigureClick,
                icon = Icons.Default.Settings,
                modifier = Modifier.weight(1f)
            )
            if (onRetryClick != null) {
                MoccaOutlinedButton(
                    text = "RETRY",
                    onClick = onRetryClick,
                    icon = Icons.Default.Refresh,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
