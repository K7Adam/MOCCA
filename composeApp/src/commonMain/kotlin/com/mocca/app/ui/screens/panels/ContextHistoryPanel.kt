package com.mocca.app.ui.screens.panels

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.*
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi

import androidx.compose.animation.animateContentSize

/**
 * Left swipe panel: Context info + Session history.
 * Matches mockup: mockups_screens/context_&_history_sidebar/screen.png
 * Refactored for modern UI/UX with session grouping.
 */

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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AgentHeader(
    agentName: String = "--",
    appVersion: String = ""
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    val headerModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "nav_item_LEFT"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        Modifier
    }

    Row(
        modifier = headerModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(AppShapes.medium)
                .background(AppColors.surfaceContainer, AppShapes.medium)
                .border(AppSpacing.borderThin, AppColors.outline, AppShapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = null,
                tint = AppColors.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column {
            Text(
                text = agentName,
                color = AppColors.onSurface,
                style = AppTypography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = appVersion.ifEmpty { "--" },
                color = AppColors.outline,
                style = AppTypography.bodySmall
            )
        }
    }
}

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
    val useGroupedView = sessionGroups.isNotEmpty()
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Conversation history",
                color = AppColors.onSurfaceVariant,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            if (runningSessionIds.isNotEmpty()) {
                RunningSessionIndicator(
                    isRunning = true,
                    statusLabel = "${runningSessionIds.size} active"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
            if (useGroupedView) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = AppSpacing.topBarHeight, bottom = AppSpacing.bottomBarClearance),
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
                            modifier = Modifier.animateItem(
                                fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                            ),
                            onSessionClick = onSessionClick,
                            onExpandToggle = { onGroupExpandToggle(group.parent.id) }
                        )
                    }
                }
            } else {
                // Flat session view (fallback)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = AppSpacing.topBarHeight, bottom = AppSpacing.bottomBarClearance),
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
                        
                        MoccaSessionCard(
                            modifier = Modifier.animateItem(
                                fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                            ),
                            isActive = isActive,
                            onClick = { onSessionClick(session) }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "#${formatSessionId(session.id)}",
                                        color = if (isActive) AppColors.onSurface else AppColors.onSurfaceVariant,
                                        style = AppTypography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isRunning) {
                                            RunningSessionIndicator(isRunning = true, statusLabel = "LIVE")
                                        }
                                        if (isLoading) {
                                            LoadingIndicator(
                                                modifier = Modifier.size(12.dp),
                                                color = AppColors.statusWaiting
                                            )
                                        } else {
                                            Text(
                                                text = formatTimeAgo(session.updatedAt),
                                                color = AppColors.outline,
                                                style = AppTypography.labelSmall
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = session.title ?: "Untitled Session",
                                    color = if (isActive) AppColors.onSurface else AppColors.outline,
                                    style = AppTypography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
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
}


@Composable
private fun NewSessionButton(
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    val borderColor by animateColorAsState(
        targetValue = if (isLoading) AppColors.statusWaiting else AppColors.outline,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "borderColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .border(AppSpacing.borderThin, borderColor, AppShapes.card)
            .background(AppColors.surfaceContainer, AppShapes.card)
            .moccaClickable(
                onClick = onClick, 
                pressedScale = 0.96f, 
                rippleColor = AppColors.onSurface.copy(alpha = 0.1f),
                enabled = !isLoading
            )
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            LoadingIndicator(
                modifier = Modifier.size(16.dp),
                color = AppColors.statusWaiting
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "Creating...",
                color = AppColors.statusWaiting,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New session",
                tint = AppColors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "New session",
                color = AppColors.onSurface,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
private fun formatSessionId(id: String): String = "SESS-${id.take(3).uppercase()}"

private fun formatTimeAgo(timestamp: Long): String = com.mocca.app.util.TimeFormatter.formatTimeAgo(timestamp)
