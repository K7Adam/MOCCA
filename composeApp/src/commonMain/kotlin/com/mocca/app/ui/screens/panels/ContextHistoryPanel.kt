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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSessionClick: (Session) -> Unit = {},
    onNewSessionClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onGroupExpandToggle: (String) -> Unit = {}
) {
    val isMcpOnline = mcpStatus.equals("ONLINE", ignoreCase = true)

    // Filter sessions based on search query
    val filteredSessions = if (searchQuery.isNotBlank()) {
        sessions.filter { session ->
            session.title?.contains(searchQuery, ignoreCase = true) == true ||
            session.id.contains(searchQuery, ignoreCase = true)
        }
    } else {
        sessions
    }

    val filteredGroups = if (searchQuery.isNotBlank() && sessionGroups.isNotEmpty()) {
        sessionGroups.map { group ->
            val filteredParent = group.parent.title?.contains(searchQuery, ignoreCase = true) == true ||
                group.parent.id.contains(searchQuery, ignoreCase = true)
            val filteredChildren = group.children.filter { child ->
                child.title?.contains(searchQuery, ignoreCase = true) == true ||
                child.id.contains(searchQuery, ignoreCase = true)
            }
            if (filteredParent || filteredChildren.isNotEmpty()) {
                group.copy(
                    children = filteredChildren,
                    parent = if (filteredParent) group.parent else group.parent.copy(
                        title = "Untitled Session"
                    )
                )
            } else null
        }.filterNotNull()
    } else {
        sessionGroups
    }

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
            sessions = filteredSessions,
            sessionGroups = filteredGroups,
            runningSessionIds = runningSessionIds,
            currentSessionId = currentSessionId,
            isCreatingSession = isCreatingSession,
            loadingSessionId = loadingSessionId,
            newlyCreatedSessionId = newlyCreatedSessionId,
            isRefreshing = isRefreshing,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
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
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
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

        // Search bar
        SessionSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
            if (useGroupedView) {
                val groupedByDate = groupSessionGroupsByDate(sessionGroups)
                val dateGroups = groupedByDate.keys.toList()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = AppSpacing.topBarHeight, bottom = AppSpacing.bottomBarClearance),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    dateGroups.forEach { dateGroup ->
                        item(key = "header_${dateGroup.name}") {
                            DateGroupHeader(dateLabel = dateGroup.label)
                        }

                        val groupsInDate = groupedByDate[dateGroup] ?: emptyList()
                        items(
                            items = groupsInDate,
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
            }
} else {
                // Flat session view with date grouping (fallback)
                val groupedByDate = groupSessionsByDate(sessions)
                val dateGroups = groupedByDate.keys.toList()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = AppSpacing.topBarHeight, bottom = AppSpacing.bottomBarClearance),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    dateGroups.forEach { dateGroup ->
                        // Date group header
                        item(key = "header_${dateGroup.name}") {
                            DateGroupHeader(dateLabel = dateGroup.label)
                        }

                        // Sessions in this date group
                        val sessionsInGroup = groupedByDate[dateGroup] ?: emptyList()
                        items(
                            items = sessionsInGroup,
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
        targetValue = if (isLoading) AppColors.statusWaiting else AppColors.outline.copy(alpha = 0.3f),
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "borderColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            // Borderless style: use background contrast instead of explicit borders
            .background(AppColors.bgRaised, AppShapes.medium)
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

/**
 * Search bar for filtering sessions in the history panel.
 * Borderless design: uses background contrast instead of explicit borders.
 */
@Composable
private fun SessionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppShapes.medium)
            // Borderless: background contrast for visual separation
            .background(AppColors.bgRaised, AppShapes.medium)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AppColors.fgSubtle
            )

            Spacer(modifier = Modifier.width(AppSpacing.sm))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = AppTypography.bodySmall.copy(
                    color = AppColors.onSurface
                ),
                cursorBrush = SolidColor(AppColors.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search sessions...",
                                style = AppTypography.bodySmall,
                                color = AppColors.fgSubtle
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    modifier = Modifier
                        .size(16.dp)
                        .moccaClickable(
                            onClick = { onQueryChange("") },
                            pressedScale = 0.9f
                        ),
                    tint = AppColors.fgSubtle
                )
            }
        }
    }
}
private fun formatSessionId(id: String): String = "SESS-${id.take(3).uppercase()}"

private fun formatTimeAgo(timestamp: Long): String = com.mocca.app.util.TimeFormatter.formatTimeAgo(timestamp)

/**
 * Date group header with clear date label (Today, Yesterday, or date).
 */
@Composable
private fun DateGroupHeader(
    dateLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            style = AppTypography.labelSmall,
            color = AppColors.fgMuted,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Groups sessions by date (Today, Yesterday, or older).
 */
private fun groupSessionsByDate(sessions: List<Session>): Map<DateGroup, List<Session>> {
    if (sessions.isEmpty()) return emptyMap()

    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    return sessions.groupBy { session ->
        val sessionTime = session.updatedAt
        val age = now - sessionTime
        when {
            age < oneDayMs -> DateGroup("Today", "today")
            age < 2 * oneDayMs -> DateGroup("Yesterday", "yesterday")
            else -> {
                val date = try {
                    java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
                        .format(java.util.Date(sessionTime))
                } catch (e: Exception) {
                    "Older"
                }
                DateGroup(date, date.lowercase())
            }
        }
    }
}

/**
 * Groups session groups by date (Today, Yesterday, or older) based on the parent session's updated time.
 */
private fun groupSessionGroupsByDate(groups: List<SessionGroup>): Map<DateGroup, List<SessionGroup>> {
    if (groups.isEmpty()) return emptyMap()

    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    return groups.groupBy { group ->
        val sessionTime = group.parent.updatedAt
        val age = now - sessionTime
        when {
            age < oneDayMs -> DateGroup("Today", "today")
            age < 2 * oneDayMs -> DateGroup("Yesterday", "yesterday")
            else -> {
                val date = try {
                    java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
                        .format(java.util.Date(sessionTime))
                } catch (e: Exception) {
                    "Older"
                }
                DateGroup(date, date.lowercase())
            }
        }
    }
}

private data class DateGroup(val label: String, val name: String)
