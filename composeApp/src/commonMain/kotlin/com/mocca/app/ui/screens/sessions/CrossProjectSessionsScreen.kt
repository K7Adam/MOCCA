package com.mocca.app.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.data.repository.CrossProjectSessionsRepository
import com.mocca.app.domain.model.CrossProjectSession
import com.mocca.app.domain.model.Resource
import com.mocca.app.ui.components.LoadingScreen
import com.mocca.app.ui.components.modern.MoccaIconButton
import com.mocca.app.ui.components.modern.ModernHeader
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class CrossProjectSessionsUiState(
    val isLoading: Boolean = false,
    val sessions: List<CrossProjectSession> = emptyList(),
    val error: String? = null,
    val groupedByProject: Map<String, List<CrossProjectSession>> = emptyMap()
)

// ─────────────────────────────────────────────────────────────────────────────
// ScreenModel
// ─────────────────────────────────────────────────────────────────────────────

class CrossProjectSessionsScreenModel(
    private val repository: CrossProjectSessionsRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(CrossProjectSessionsUiState())
    val uiState: StateFlow<CrossProjectSessionsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getCrossProjectSessions().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    is Resource.Success -> {
                        val sessions = resource.data.sortedByDescending { it.createdAt ?: 0L }
                        val grouped = sessions.groupBy { it.projectID }
                        _uiState.value = CrossProjectSessionsUiState(
                            isLoading = false,
                            sessions = sessions,
                            groupedByProject = grouped
                        )
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = resource.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * W5-T2: Cross-project sessions browser.
 *
 * Displays all sessions from GET /experimental/session grouped by project.
 * Allows navigating into a session from any project.
 */
object CrossProjectSessionsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<CrossProjectSessionsScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .padding(AppSpacing.lg)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoccaIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { navigator.pop() },
                    iconColor = AppColors.textPrimary
                )
                Spacer(modifier = Modifier.width(AppSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    ModernHeader(text = "ALL SESSIONS")
                    Text(
                        text = "Experimental — ${uiState.sessions.size} session${if (uiState.sessions.size != 1) "s" else ""} across ${uiState.groupedByProject.size} project${if (uiState.groupedByProject.size != 1) "s" else ""}",
                        style = AppTypography.labelSmall,
                        color = AppColors.textTertiary
                    )
                }
                MoccaIconButton(
                    icon = Icons.Default.Refresh,
                    onClick = { screenModel.load() },
                    iconColor = AppColors.accentGreen
                )
            }

            // ── Error Banner ──────────────────────────────────────────────────
            uiState.error?.let { errorMsg ->
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.medium)
                        .background(AppColors.error.copy(alpha = 0.12f))
                        .border(0.5.dp, AppColors.error.copy(alpha = 0.4f), AppShapes.medium)
                        .padding(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMsg,
                        style = AppTypography.labelSmall,
                        color = AppColors.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { screenModel.clearError() }) {
                        Text("DISMISS", style = AppTypography.labelSmall, color = AppColors.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // ── Content ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(AppSpacing.borderThin, AppColors.borderLight, AppShapes.medium)
            ) {
                when {
                    uiState.isLoading && uiState.sessions.isEmpty() -> LoadingScreen()
                    uiState.sessions.isEmpty() -> CrossProjectSessionsEmptyState()
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AppSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        uiState.groupedByProject.forEach { (projectId, projectSessions) ->
                            // Project group header
                            item(key = "header_$projectId") {
                                ProjectGroupHeader(
                                    projectId = projectId,
                                    projectPath = projectSessions.firstOrNull()?.projectPath,
                                    count = projectSessions.size
                                )
                            }
                            items(projectSessions, key = { it.id }) { session ->
                                CrossProjectSessionRow(
                                    session = session,
                                    onOpen = {
                                        // Navigate to chat screen with session id
                                        navigator.push(
                                            com.mocca.app.ui.screens.chat.ChatScreen(
                                                sessionId = session.id
                                            )
                                        )
                                    }
                                )
                            }
                            item(key = "divider_$projectId") {
                                Spacer(modifier = Modifier.height(AppSpacing.sm))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Project Group Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectGroupHeader(
    projectId: String,
    projectPath: String?,
    count: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.md, bottom = AppSpacing.xs, start = AppSpacing.xs, end = AppSpacing.xs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(AppShapes.small)
                    .background(AppColors.accentGreen)
            )
            Text(
                text = projectPath?.substringAfterLast('/')?.substringAfterLast('\\')?.uppercase()
                    ?: projectId.take(12).uppercase(),
                style = AppTypography.labelMedium,
                color = AppColors.accentGreen,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "($count)",
                style = AppTypography.labelSmall,
                color = AppColors.textTertiary
            )
        }
        projectPath?.let { path ->
            Spacer(modifier = Modifier.height(AppSpacing.xxs))
            Text(
                text = path,
                style = AppTypography.monoLabel,
                color = AppColors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = AppSpacing.lg + AppSpacing.xs)
            )
        }
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = AppColors.border
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Session Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CrossProjectSessionRow(
    session: CrossProjectSession,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .clickable(onClick = onOpen)
            .background(AppColors.surface)
            .border(0.5.dp, AppColors.border, AppShapes.card)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Title or session id
            Text(
                text = session.title?.ifBlank { null } ?: "Session ${session.id.take(8)}",
                style = AppTypography.labelMedium,
                color = AppColors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(AppSpacing.xxs))
            // Session ID in mono
            Text(
                text = session.id,
                style = AppTypography.monoLabel,
                color = AppColors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(AppSpacing.sm))

        // Open icon
        MoccaIconButton(
            icon = Icons.AutoMirrored.Filled.OpenInNew,
            onClick = onOpen,
            iconColor = AppColors.accentGreen
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CrossProjectSessionsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NO SESSIONS",
            style = AppTypography.labelLarge,
            color = AppColors.textTertiary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(
            text = "No sessions found across any projects.",
            style = AppTypography.labelSmall,
            color = AppColors.textTertiary
        )
    }
}
