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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Session
import com.mocca.app.domain.model.SessionStatus
import com.mocca.app.ui.components.terminal.ContextInfoContainer
import com.mocca.app.ui.components.terminal.StatusSquare
import com.mocca.app.ui.components.terminal.TerminalHeader
import com.mocca.app.ui.components.terminal.TerminalSessionCard
import com.mocca.app.ui.components.terminal.TerminalTextButton
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalShapes
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

/**
 * Left swipe panel: Context info + Session history.
 * Matches mockup: mockups_screens/context_&_history_sidebar/screen.png
 * Refactored for modern UI/UX.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextHistoryPanel(
    sessions: List<Session>,
    currentSessionId: String?,
    mcpStatus: String,
    model: String,
    latency: String,
    port: String,
    usedTokens: Int,
    maxTokens: Int,
    modifier: Modifier = Modifier,
    isCreatingSession: Boolean = false,      // Loading state for INIT_NEW_SESSION button
    loadingSessionId: String? = null,        // ID of session being loaded
    newlyCreatedSessionId: String? = null,   // ID of newly created session (for animation)
    isRefreshing: Boolean = false,           // Pull-to-refresh state
    onSessionClick: (Session) -> Unit = {},
    onNewSessionClick: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val isMcpOnline = mcpStatus.equals("ONLINE", ignoreCase = true)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalColors.background)
            .padding(TerminalSpacing.lg)
    ) {
        // Agent header
        AgentHeader()
        
        Spacer(modifier = Modifier.height(TerminalSpacing.lg))
        
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
        
        Spacer(modifier = Modifier.height(TerminalSpacing.xl))
        
        // Conversation history section
        ConversationHistorySection(
            sessions = sessions,
            currentSessionId = currentSessionId,
            isCreatingSession = isCreatingSession,
            loadingSessionId = loadingSessionId,
            newlyCreatedSessionId = newlyCreatedSessionId,
            isRefreshing = isRefreshing,
            onSessionClick = onSessionClick,
            onNewSessionClick = onNewSessionClick,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Agent header with icon and version.
 */
@Composable
private fun AgentHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
    ) {
        // Terminal/monitor icon (rounded)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(TerminalShapes.medium)
                .background(TerminalColors.surfaceContainer, TerminalShapes.medium)
                .border(TerminalSpacing.borderThin, TerminalColors.border, TerminalShapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = null,
                tint = TerminalColors.white,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column {
            Text(
                text = "AGENT_01",
                color = TerminalColors.white,
                style = TerminalTypography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "v2.4.0-STABLE",
                color = TerminalColors.textTertiary,
                style = TerminalTypography.bodySmall
            )
        }
    }
}

/**
 * Conversation history list with new session button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationHistorySection(
    sessions: List<Session>,
    currentSessionId: String?,
    isCreatingSession: Boolean,
    loadingSessionId: String?,
    newlyCreatedSessionId: String?,
    isRefreshing: Boolean,
    onSessionClick: (Session) -> Unit,
    onNewSessionClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONVERSATION HISTORY",
                color = TerminalColors.textSecondary,
                style = TerminalTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
        
        // New session button with loading state
        NewSessionButton(
            onClick = onNewSessionClick,
            isLoading = isCreatingSession
        )
        
        Spacer(modifier = Modifier.height(TerminalSpacing.md))
        
        // Session list with animations and pull-to-refresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
            ) {
                items(
                    items = sessions,
                    key = { it.id }
                ) { session ->
                    // Animate new session appearing
                    val isNewSession = session.id == newlyCreatedSessionId
                    val isLoading = session.id == loadingSessionId
                    val isActive = session.id == currentSessionId
                    
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
                        TerminalSessionCard(
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
                                            color = if (isActive) TerminalColors.white else TerminalColors.textSecondary,
                                            style = TerminalTypography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 2.dp,
                                                color = TerminalColors.statusWaiting
                                            )
                                        } else {
                                            Text(
                                                text = formatTimeAgo(session.updatedAt),
                                                color = TerminalColors.textTertiary,
                                                style = TerminalTypography.labelSmall
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = session.title ?: "Untitled Session",
                                        color = if (isActive) TerminalColors.white else TerminalColors.textTertiary,
                                        style = TerminalTypography.bodySmall,
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
        targetValue = if (isLoading) TerminalColors.statusWaiting else TerminalColors.border,
        animationSpec = tween(150),
        label = "borderColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TerminalShapes.card)
            .border(TerminalSpacing.borderThin, borderColor, TerminalShapes.card)
            .background(TerminalColors.surfaceContainer, TerminalShapes.card)
            .clickable(
                enabled = !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = TerminalColors.white.copy(alpha = 0.1f)),
                onClick = onClick
            )
            .padding(TerminalSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = TerminalColors.statusWaiting
            )
            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
            Text(
                text = "CREATING...",
                color = TerminalColors.statusWaiting,
                style = TerminalTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New session",
                tint = TerminalColors.accentGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
            Text(
                text = "NEW SESSION",
                color = TerminalColors.white,
                style = TerminalTypography.labelMedium,
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