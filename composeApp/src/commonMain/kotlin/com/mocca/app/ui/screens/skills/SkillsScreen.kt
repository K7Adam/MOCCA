package com.mocca.app.ui.screens.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.SkillInfo
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

class SkillsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<SkillsScreenModel>()
        val state by screenModel.state.collectAsState()
        var searchQuery by remember { mutableStateOf("") }
        var selectedSkill by remember { mutableStateOf<SkillInfo?>(null) }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.background)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(AppSpacing.lg)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoccaIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { navigator.pop() },
                        size = 40.dp,
                        contentDescription = "Back"
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Text(
                        text = "Agent Skills",
                        style = AppTypography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search skills...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.medium,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Skills count
                val filteredSkills = if (searchQuery.isBlank()) {
                    state.skills
                } else {
                    state.skills.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description?.contains(searchQuery, ignoreCase = true) == true
                    }
                }

                Text(
                    text = "${filteredSkills.size} skill${if (filteredSkills.size != 1) "s" else ""} available",
                    style = AppTypography.labelMedium,
                    color = AppColors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // Skills list
                if (filteredSkills.isEmpty() && !state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = AppColors.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.md))
                            Text(
                                text = if (searchQuery.isBlank()) "No skills discovered" else "No skills match \"$searchQuery\"",
                                style = AppTypography.bodyMedium,
                                color = AppColors.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                            Text(
                                text = "Skills are loaded from .opencode/skills/, .claude/skills/, .agents/skills/",
                                style = AppTypography.labelSmall,
                                color = AppColors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        items(filteredSkills) { skill ->
                            SkillCard(
                                skill = skill,
                                onClick = { selectedSkill = skill }
                            )
                        }
                    }
                }
            }

            // Skill detail dialog
            selectedSkill?.let { skill ->
                SkillDetailDialog(
                    skill = skill,
                    onDismiss = { selectedSkill = null }
                )
            }
        }
    }
}

@Composable
private fun SkillCard(
    skill: SkillInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.surfaceContainerLow)
            .border(
                AppSpacing.borderThin,
                AppColors.outlineVariant.copy(alpha = 0.3f),
                AppShapes.medium
            )
            .clickable(onClick = onClick)
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Extension,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = AppColors.primary
        )
        Spacer(modifier = Modifier.width(AppSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Text(
                    text = skill.name,
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.onSurface
                )
                if (skill.slash == true) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppColors.primaryContainer
                    ) {
                        Text(
                            text = "/",
                            style = AppTypography.labelSmall,
                            color = AppColors.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            if (skill.description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = skill.description,
                    style = AppTypography.bodySmall,
                    color = AppColors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SkillDetailDialog(
    skill: SkillInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = AppColors.primary
                )
                Text(
                    text = skill.name,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                if (skill.description != null) {
                    Text(
                        text = skill.description,
                        style = AppTypography.bodyMedium,
                        color = AppColors.onSurface
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                }
                Text(
                    text = "Location",
                    style = AppTypography.labelMedium,
                    color = AppColors.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = skill.location,
                    style = AppTypography.bodySmall,
                    color = AppColors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text(
                    text = "Content",
                    style = AppTypography.labelMedium,
                    color = AppColors.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = skill.content.take(500) + if (skill.content.length > 500) "..." else "",
                    style = AppTypography.bodySmall,
                    color = AppColors.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
