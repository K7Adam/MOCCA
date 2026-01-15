package com.mocca.app.ui.screens.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ripple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Session
import com.mocca.app.domain.model.SessionStatus
import com.mocca.app.ui.components.terminal.ContextInfoContainer
import com.mocca.app.ui.components.terminal.StatusSquare
import com.mocca.app.ui.components.terminal.TerminalHeader
import com.mocca.app.ui.components.terminal.TerminalTextButton
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

/**
 * Left swipe panel: Context info + Session history.
 * Matches mockup: mockups_screens/context_&_history_sidebar/screen.png
 */
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
    onSessionClick: (Session) -> Unit = {},
    onNewSessionClick: () -> Unit = {},
    onClearHistoryClick: () -> Unit = {}
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
        
        Spacer(modifier = Modifier.height(TerminalSpacing.xl))
        
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
            onSessionClick = onSessionClick,
            onNewSessionClick = onNewSessionClick,
            onClearHistoryClick = onClearHistoryClick,
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
        // Terminal/monitor icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(TerminalSpacing.borderThin, TerminalColors.borderLight, RectangleShape),
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
                style = TerminalTypography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "v2.4.0-STABLE",
                color = TerminalColors.grey,
                style = TerminalTypography.bodySmall
            )
        }
    }
}

/**
 * Conversation history list with new session button.
 */
@Composable
private fun ConversationHistorySection(
    sessions: List<Session>,
    currentSessionId: String?,
    isCreatingSession: Boolean,
    loadingSessionId: String?,
    newlyCreatedSessionId: String?,
    onSessionClick: (Session) -> Unit,
    onNewSessionClick: () -> Unit,
    onClearHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TerminalHeader(
                text = "CONVERSATION_HISTORY",
                showBrackets = true
            )
            TerminalTextButton(
                text = "CLEAR",
                onClick = onClearHistoryClick
            )
        }
        
        Spacer(modifier = Modifier.height(TerminalSpacing.md))
        
        // New session button with loading state
        NewSessionButton(
            onClick = onNewSessionClick,
            isLoading = isCreatingSession
        )
        
        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
        
        // Session list with animations
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(
                items = sessions,
                key = { it.id }
            ) { session ->
                // Animate new session appearing
                val isNewSession = session.id == newlyCreatedSessionId
                val isLoading = session.id == loadingSessionId
                
                AnimatedVisibility(
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
                    Column {
                        SessionListItem(
                            session = session,
                            isActive = session.id == currentSessionId,
                            isLoading = isLoading,
                            isNew = isNewSession,
                            onClick = { onSessionClick(session) }
                        )
                        HorizontalDivider(
                            thickness = TerminalSpacing.borderThin,
                            color = TerminalColors.border
                        )
                    }
                }
            }
        }
    }
}

/**
 * New session button at top of list with loading state.
 */
@Composable
private fun NewSessionButton(
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    val borderColor by animateColorAsState(
        targetValue = if (isLoading) TerminalColors.statusWaiting else TerminalColors.borderLight,
        animationSpec = tween(150),
        label = "borderColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(TerminalSpacing.borderThin, borderColor, RectangleShape)
            .clickable(
                enabled = !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = TerminalColors.white.copy(alpha = 0.1f)),
                onClick = onClick
            )
            .padding(TerminalSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(TerminalSpacing.borderThin, if (isLoading) TerminalColors.statusWaiting else TerminalColors.white, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = TerminalColors.statusWaiting
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New session",
                    tint = TerminalColors.white,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = if (isLoading) "CREATING..." else "INIT_NEW_SESSION",
            color = if (isLoading) TerminalColors.statusWaiting else TerminalColors.white,
            style = TerminalTypography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Individual session list item with loading and animation states.
 */
@Composable
private fun SessionListItem(
    session: Session,
    isActive: Boolean,
    isLoading: Boolean = false,
    isNew: Boolean = false,
    onClick: () -> Unit
) {
    val activeIndicatorWidth = TerminalSpacing.activeIndicatorWidth
    
    // Animate background for new/loading state
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isNew -> TerminalColors.statusOnline.copy(alpha = 0.1f)
            isLoading -> TerminalColors.statusWaiting.copy(alpha = 0.1f)
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "bgColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .then(
                if (isActive) {
                    Modifier.drawBehind {
                        drawRect(
                            color = TerminalColors.activeIndicator,
                            topLeft = Offset(0f, 0f),
                            size = Size(activeIndicatorWidth.toPx(), size.height)
                        )
                    }
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = TerminalColors.white.copy(alpha = 0.1f)),
                onClick = onClick
            )
            .padding(
                start = if (isActive) TerminalSpacing.lg else TerminalSpacing.md,
                end = TerminalSpacing.md,
                top = TerminalSpacing.md,
                bottom = TerminalSpacing.md
            ),
        verticalAlignment = Alignment.Top
    ) {
        // Status indicator - show loading spinner when loading
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(10.dp)
                    .padding(top = 4.dp),
                strokeWidth = 1.5.dp,
                color = TerminalColors.statusWaiting
            )
        } else {
            StatusSquare(
                color = when {
                    isActive -> TerminalColors.statusOnline
                    session.status == SessionStatus.RUNNING -> TerminalColors.statusWaiting
                    else -> TerminalColors.grey
                },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(TerminalSpacing.sm))
        
        Column(modifier = Modifier.weight(1f)) {
            // Session ID and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "#${formatSessionId(session.id)}",
                    color = if (isActive) TerminalColors.white else TerminalColors.greyLight,
                    style = TerminalTypography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        isLoading -> "LOADING..."
                        isNew -> "NEW"
                        isActive -> "ACTIVE"
                        else -> formatTimeAgo(session.updatedAt)
                    },
                    color = when {
                        isLoading -> TerminalColors.statusWaiting
                        isNew -> TerminalColors.statusOnline
                        isActive -> TerminalColors.statusOnline
                        else -> TerminalColors.grey
                    },
                    style = TerminalTypography.labelSmall
                )
            }
            
            Spacer(modifier = Modifier.height(TerminalSpacing.xxs))
            
            // Session title
            Text(
                text = session.title ?: "Untitled Session",
                color = if (isActive) TerminalColors.white else TerminalColors.greyLight,
                style = TerminalTypography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
