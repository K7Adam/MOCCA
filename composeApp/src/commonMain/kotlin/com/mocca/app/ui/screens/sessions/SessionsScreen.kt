package com.mocca.app.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.data.repository.AppConnectionState
import com.mocca.app.domain.model.Session
import com.mocca.app.domain.model.SessionStatus
import com.mocca.app.ui.components.terminal.TerminalButton
import com.mocca.app.ui.components.terminal.TerminalHeader
import com.mocca.app.ui.components.terminal.TerminalIconButton
import com.mocca.app.ui.components.terminal.TerminalOutlinedButton
import com.mocca.app.ui.components.terminal.TerminalProcessingIndicator
import com.mocca.app.ui.components.terminal.TerminalSessionCard
import com.mocca.app.ui.screens.workspace.WorkspaceScreen
import com.mocca.app.ui.screens.settings.SettingsScreen
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import androidx.compose.material3.MaterialTheme
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
                .background(TerminalColors.background)
        ) {
            // Terminal Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TerminalSpacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TerminalHeader(text = "SESSIONS", showBrackets = true)
                
                Row(horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
                    TerminalIconButton(
                        icon = Icons.Default.Settings,
                        onClick = { navigator.push(SettingsScreen()) },
                        contentDescription = stringResource(Res.string.settings)
                    )
                    TerminalIconButton(
                        icon = Icons.Default.Refresh,
                        onClick = { screenModel.refresh() },
                        contentDescription = stringResource(Res.string.refresh)
                    )
                }
            }
            
            HorizontalDivider(
                thickness = TerminalSpacing.borderThin,
                color = TerminalColors.border
            )
            
            // Main Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val connectionState = state.connectionState) {
                    is AppConnectionState.NotConfigured -> {
                        TerminalNotConnectedContent(
                            title = stringResource(Res.string.no_server_configured),
                            message = stringResource(Res.string.configure_server_hint),
                            onConfigureClick = { navigator.push(SettingsScreen()) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is AppConnectionState.Checking -> {
                        TerminalConnectionProgressContent(
                            message = stringResource(Res.string.checking_connection),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is AppConnectionState.WaitingForNetwork -> {
                        TerminalNotConnectedContent(
                            title = stringResource(Res.string.waiting_for_network),
                            message = stringResource(Res.string.network_unavailable_hint),
                            onConfigureClick = { navigator.push(SettingsScreen()) },
                            onRetryClick = { screenModel.retryConnection() },
                            modifier = Modifier.align(Alignment.Center),
                            icon = Icons.Default.WifiOff
                        )
                    }
                    is AppConnectionState.Connecting -> {
                        TerminalConnectionProgressContent(
                            message = stringResource(Res.string.connecting_attempt, connectionState.attempt, connectionState.maxAttempts),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is AppConnectionState.Reconnecting -> {
                        TerminalConnectionProgressContent(
                            message = stringResource(Res.string.reconnecting, connectionState.attempt, connectionState.maxAttempts),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is AppConnectionState.Disconnected -> {
                        TerminalNotConnectedContent(
                            title = stringResource(Res.string.connection_failed_title),
                            message = connectionState.error ?: stringResource(Res.string.connection_failed_message),
                            onConfigureClick = { navigator.push(SettingsScreen()) },
                            onRetryClick = { screenModel.retryConnection() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is AppConnectionState.Connected -> {
                        when {
                            state.isLoading && state.sessions.isEmpty() -> {
                                TerminalProcessingIndicator()
                            }
                            state.sessions.isEmpty() -> {
                                TerminalEmptySessionsContent(
                                    onCreateClick = { screenModel.createSession() },
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            else -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    TerminalSessionsList(
                                        sessions = state.sessions,
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
                                            .padding(TerminalSpacing.lg)
                                    ) {
                                        TerminalButton(
                                            text = "NEW_SESSION",
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
                                color = TerminalColors.statusWaiting,
                                trackColor = TerminalColors.border
                            )
                        }
                    }
                }
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
        modifier = modifier.padding(TerminalSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TerminalColors.grey
        )
        Spacer(modifier = Modifier.height(TerminalSpacing.lg))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            color = TerminalColors.white
        )
        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TerminalColors.greyLight
        )
        Spacer(modifier = Modifier.height(TerminalSpacing.xl))
        Row(
            horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
        ) {
            TerminalButton(
                text = "CONFIGURE",
                onClick = onConfigureClick,
                icon = Icons.Default.Settings,
                modifier = Modifier.weight(1f)
            )
            if (onRetryClick != null) {
                TerminalOutlinedButton(
                    text = "RETRY",
                    onClick = onRetryClick,
                    icon = Icons.Default.Refresh,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TerminalConnectionProgressContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = TerminalColors.statusWaiting,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(TerminalSpacing.lg))
        Text(
            text = message.uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = TerminalColors.greyLight
        )
    }
}

@Composable
private fun TerminalEmptySessionsContent(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(TerminalSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.no_sessions).uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            color = TerminalColors.white
        )
        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
        Text(
            text = stringResource(Res.string.no_sessions_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = TerminalColors.greyLight
        )
        Spacer(modifier = Modifier.height(TerminalSpacing.xl))
        TerminalButton(
            text = "NEW_SESSION",
            onClick = onCreateClick,
            icon = Icons.Default.Add,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
    }
}

@Composable
private fun TerminalSessionsList(
    sessions: List<Session>,
    childrenMap: Map<String, List<Session>>,
    selectedSessionId: String?,
    onSessionClick: (Session) -> Unit,
    onDeleteClick: (Session) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(TerminalSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
    ) {
        items(
            items = sessions,
            key = { it.id },
            contentType = { "session" }
        ) { session ->
            TerminalSessionCard(
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
private fun TerminalSessionCard(
    session: Session,
    childSessions: List<Session>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val borderColor = if (isSelected) TerminalColors.statusOnline else TerminalColors.border
    val bgColor = if (isSelected) TerminalColors.statusOnline.copy(alpha = 0.1f) else Color.Transparent
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RectangleShape)
            .border(TerminalSpacing.borderThin, borderColor, RectangleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TerminalSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active indicator bar
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(TerminalSpacing.activeIndicatorWidth)
                        .height(48.dp)
                        .background(TerminalColors.statusOnline, RectangleShape)
                )
                Spacer(modifier = Modifier.width(TerminalSpacing.md))
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = (session.title ?: stringResource(Res.string.untitled_session)).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) TerminalColors.statusOnline else TerminalColors.white,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(TerminalSpacing.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
                ) {
                    TerminalStatusChip(status = session.status)
                    Text(
                        text = formatTime(session.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalColors.grey
                    )
                    session.summary?.let { summary ->
                        if (summary.files > 0) {
                            Text(
                                text = stringResource(Res.string.files_count_suffix, summary.files),
                                style = MaterialTheme.typography.labelSmall,
                                color = TerminalColors.greyLight
                            )
                        }
                    }
                    if (childSessions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .border(TerminalSpacing.borderThin, TerminalColors.greyLight, RectangleShape)
                                .padding(horizontal = TerminalSpacing.xs, vertical = TerminalSpacing.xxs)
                        ) {
                            Text(
                                text = stringResource(Res.string.tasks_count_suffix, childSessions.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = TerminalColors.greyLight
                            )
                        }
                    }
                }
            }
            
            TerminalIconButton(
                icon = Icons.Default.Delete,
                onClick = onDeleteClick,
                iconColor = TerminalColors.error,
                contentDescription = stringResource(Res.string.delete_session)
            )
        }
    }
}

@Composable
private fun TerminalStatusChip(status: SessionStatus) {
    val (color, textRes) = when (status) {
        SessionStatus.IDLE -> TerminalColors.grey to Res.string.session_idle
        SessionStatus.RUNNING -> TerminalColors.statusOnline to Res.string.session_running
        SessionStatus.COMPLETED -> TerminalColors.success to Res.string.session_completed
        SessionStatus.ERROR -> TerminalColors.error to Res.string.session_error
    }
    
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RectangleShape)
            .border(TerminalSpacing.borderThin, color, RectangleShape)
            .padding(horizontal = TerminalSpacing.sm, vertical = TerminalSpacing.xxs)
    ) {
        Text(
            text = stringResource(textRes).uppercase(),
            style = MaterialTheme.typography.labelSmall,
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
