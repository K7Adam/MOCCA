package com.mocca.app.ui.screens.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ripple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Session
import com.mocca.app.domain.model.SessionGroup
import com.mocca.app.domain.model.SessionStatus
import com.mocca.app.ui.components.terminal.ContextInfoContainer
import com.mocca.app.ui.components.terminal.GroupedSessionCard
import com.mocca.app.ui.components.terminal.RunningSessionIndicator
import com.mocca.app.ui.components.terminal.StatusSquare
import com.mocca.app.ui.components.terminal.TerminalHeader
import com.mocca.app.ui.components.terminal.MoccaSessionCard
import com.mocca.app.ui.components.terminal.MoccaTextButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Left swipe panel: Context info + Session history.
 * Matches mockup: mockups_screens/context_&_history_sidebar/screen.png
 * Refactored for modern UI/UX with session grouping.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextHistoryPanel(
    sessions: List<Session>,
    sessionGroups: List<SessionGroup> = emptyList(),
    runningSessionIds: Set<String> = emptySet(),
    currentSessionId: String?,
    mcpStatus: String,
    model: String,
    latency: String,
    port: String,
    usedTokens: Int,
    maxTokens: Int,
    agentName: String = "--",
    appVersion: String = "",
    modifier: Modifier = Modifier,
    isCreatingSession: Boolean = false,
    loadingSessionId: String? = null,
    newlyCreatedSessionId: String? = null,
    isRefreshing: Boolean = false,
    onSessionClick: (Session) -> Unit = {},
    onNewSessionClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onGroupExpandToggle: (String) -> Unit = {}
) {
    val isMcpOnline = mcpStatus.equals("ONLINE", ignoreCase = true)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
            .padding(AppSpacing.lg)
    ) {
        // Agent header
        AgentHeader(agentName = agentName, appVersion = appVersion)
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        // Context info section
        ContextInfoContainer(
            mcpStatus = mcpStatus,
            isMcpOnline = isMcpOnline,
            model = model,
            latency = latency,
            port = port,
            usedTokens = usedTokens,
            maxTokens = maxTokens
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.xl))
        
        // Conversation history section (use grouped if available)
        ConversationHistorySection(
            sessions = sessions,
            sessionGroups = sessionGroups,
            runningSessionIds = runningSessionIds,
            currentSessionId = currentSessionId,
            isCreatingSession = isCreatingSession,
            loadingSessionId = loadingSessionId,
            newlyCreatedSessionId = newlyCreatedSessionId,
            isRefreshing = isRefreshing,
            onSessionClick = onSessionClick,
            onNewSessionClick = onNewSessionClick,
            onRefresh = onRefresh,
            onGroupExpandToggle = onGroupExpandToggle,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Agent header with icon and version.
 */
@Composable
private fun AgentHeader(
    agentName: String = "--",
    appVersion: String = ""
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        // Terminal/monitor icon (rounded)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(AppShapes.medium)
                .background(AppColors.surfaceContainer, AppShapes.medium)
                .border(AppSpacing.borderThin, AppColors.border, AppShapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = null,
                tint = AppColors.white,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column {
            Text(
                text = agentName.uppercase(),
                color = AppColors.white,
                style = AppTypography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = appVersion.ifEmpty { "--" },
                color = AppColors.textTertiary,
                style = AppTypography.bodySmall
            )
        }
    }
}

/**
 * Conversation history list with new session button.
 * Supports both flat session list and grouped sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationHistorySection(
    sessions: List<Session>,
    sessionGroups: List<SessionGroup>,
    runningSessionIds: Set<String>,
    currentSessionId: String?,
    isCreatingSession: Boolean,
    loadingSessionId: String?,
    newlyCreatedSessionId: String?,
    isRefreshing: Boolean,
    onSessionClick: (Session) -> Unit,
    onNewSessionClick: () -> Unit,
    onRefresh: () -> Unit,
    onGroupExpandToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use grouped sessions if available, otherwise fall back to flat list
    val useGroupedView = sessionGroups.isNotEmpty()
    
    Column(modifier = modifier) {
        // Section header with running count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONVERSATION HISTORY",
                color = AppColors.textSecondary,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Show running session count if any
            if (runningSessionIds.isNotEmpty()) {
                RunningSessionIndicator(
                    isRunning = true,
                    statusLabel = "${runningSessionIds.size} ACTIVE"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        // Session list with NEW SESSION button overlaid at top
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Session list with pull-to-refresh
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
            if (useGroupedView) {
                // Grouped session view
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    items(
                        items = sessionGroups,
                        key = { it.parent.id }
                    ) { group ->
                        val isActive = group.parent.id == currentSessionId ||
                            group.children.any { it.id == currentSessionId }
                        val isRunning = runningSessionIds.contains(group.parent.id)
                        
                        GroupedSessionCard(
                            group = group,
                            isActive = isActive,
                            isRunning = isRunning,
                            onSessionClick = onSessionClick,
                            onExpandToggle = { onGroupExpandToggle(group.parent.id) }
                        )
                    }
                }
            } else {
                // Flat session view (fallback)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    items(
                        items = sessions,
                        key = { it.id }
                    ) { session ->
                        val isNewSession = session.id == newlyCreatedSessionId
                        val isLoading = session.id == loadingSessionId
                        val isActive = session.id == currentSessionId
                        val isRunning = runningSessionIds.contains(session.id)
                        
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = if (isNewSession) {
                                expandVertically(
                                    animationSpec = tween(200),
                                    expandFrom = Alignment.Top
                                ) + fadeIn(animationSpec = tween(200))
                            } else {
                                fadeIn(animationSpec = tween(0))
                            }
                        ) {
                            MoccaSessionCard(
                                isActive = isActive,
                                modifier = Modifier.clickable { onSessionClick(session) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "#${formatSessionId(session.id)}",
                                                color = if (isActive) AppColors.white else AppColors.textSecondary,
                                                style = AppTypography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (isRunning) {
                                                    RunningSessionIndicator(
                                                        isRunning = true,
                                                        statusLabel = "LIVE"
                                                    )
                                                }
                                                
                                                if (isLoading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(12.dp),
                                                        strokeWidth = 2.dp,
                                                        color = AppColors.statusWaiting
                                                    )
                                                } else {
                                                    Text(
                                                        text = formatTimeAgo(session.updatedAt),
                                                        color = AppColors.textTertiary,
                                                        style = AppTypography.labelSmall
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            text = session.title ?: "Untitled Session",
                                            color = if (isActive) AppColors.white else AppColors.textTertiary,
                                            style = AppTypography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            
            // Floating NEW SESSION button with gradient fade at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AppColors.background,
                                AppColors.background,
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 160f
                        )
                    )
                    .padding(bottom = AppSpacing.sm)
            ) {
                NewSessionButton(
                    onClick = onNewSessionClick,
                    isLoading = isCreatingSession
                )
            }
        }
    }
}

/**
 * New session button at top of list with loading state.
 * Modern pill/rounded card style.
 */
@Composable
private fun NewSessionButton(
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    val borderColor by animateColorAsState(
        targetValue = if (isLoading) AppColors.statusWaiting else AppColors.border,
        animationSpec = tween(150),
        label = "borderColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .border(AppSpacing.borderThin, borderColor, AppShapes.card)
            .background(AppColors.surfaceContainer, AppShapes.card)
            .clickable(
                enabled = !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = AppColors.white.copy(alpha = 0.1f)),
                onClick = onClick
            )
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = AppColors.statusWaiting
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "CREATING...",
                color = AppColors.statusWaiting,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New session",
                tint = AppColors.accentGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "NEW SESSION",
                color = AppColors.white,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Format session ID for display (first 6 chars uppercase).
 */
private fun formatSessionId(id: String): String {
    return "SESS-${id.take(3).uppercase()}"
}

/**
 * Format timestamp as relative time.
 */
private fun formatTimeAgo(timestamp: Long): String {
    return com.mocca.app.util.TimeFormatter.formatTimeAgo(timestamp)
}