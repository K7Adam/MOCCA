package com.mocca.app.ui.screens.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.data.repository.SkillsRepository
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.SkillInfo
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
data class SkillsUiState(
    val isLoading: Boolean = false,
    val skills: List<SkillInfo> = emptyList(),
    val error: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// ScreenModel
// ─────────────────────────────────────────────────────────────────────────────

class SkillsScreenModel(
    private val repository: SkillsRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(SkillsUiState())
    val uiState: StateFlow<SkillsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getSkills().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    is Resource.Success -> _uiState.value = SkillsUiState(
                        isLoading = false,
                        skills = resource.data.sortedBy { it.name }
                    )
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
 * W5-T3: Skills browser screen.
 *
 * Displays all skills registered on the OpenCode server via GET /skill.
 * Skills are agent macros/prompts defined in .opencode/skills/.
 */
object SkillsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<SkillsScreenModel>()
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
                    ModernHeader(text = "SKILLS")
                    Text(
                        text = "${uiState.skills.size} skill${if (uiState.skills.size != 1) "s" else ""} available",
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
                    uiState.isLoading && uiState.skills.isEmpty() -> LoadingScreen()
                    uiState.skills.isEmpty() -> SkillsEmptyState()
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AppSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        items(uiState.skills, key = { it.name }) { skill ->
                            SkillCard(skill = skill)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Skill Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SkillCard(skill: SkillInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surface)
            .border(0.5.dp, AppColors.border, AppShapes.card)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
    ) {
        // Name row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = skill.name,
                style = AppTypography.labelMedium,
                color = AppColors.accentGreen,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Description
        skill.description?.let { desc ->
            Spacer(modifier = Modifier.height(AppSpacing.xxs))
            Text(
                text = desc,
                style = AppTypography.bodySmall,
                color = AppColors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Tags
        if (skill.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                items(skill.tags) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(AppShapes.pill)
                            .background(AppColors.accentGreen.copy(alpha = 0.12f))
                            .border(0.5.dp, AppColors.accentGreen.copy(alpha = 0.3f), AppShapes.pill)
                            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                    ) {
                        Text(
                            text = tag,
                            style = AppTypography.labelSmall,
                            color = AppColors.accentGreen
                        )
                    }
                }
            }
        }

        // System prompt preview divider
        skill.system?.let { sys ->
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            HorizontalDivider(thickness = 0.5.dp, color = AppColors.border)
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = sys,
                style = AppTypography.monoLabel,
                color = AppColors.textTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SkillsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NO SKILLS",
            style = AppTypography.labelLarge,
            color = AppColors.textTertiary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(
            text = "No skills found. Add skills to .opencode/skills/ on the server.",
            style = AppTypography.labelSmall,
            color = AppColors.textTertiary
        )
    }
}
