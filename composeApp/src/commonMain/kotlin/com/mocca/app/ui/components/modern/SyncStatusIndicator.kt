package com.mocca.app.ui.components.modern

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.GlobalSyncState
import com.mocca.app.domain.model.SyncState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.datetime.Clock

/**
 * Sync status indicator showing data freshness state.
 *
 * This component displays the current synchronization state between the app
 * and the OpenCode server, providing users with confidence about data freshness.
 *
 * States:
 * - NotSynced: Never synced (grey)
 * - Syncing: Currently fetching data (animated green)
 * - Fresh: All data is fresh (green)
 * - Partial: Some data fresh, some stale (yellow)
 * - Failed: Critical failure (red)
 */
// ═══════════════════════════════════════════════════════════════════════════════
// COMPACT INDICATOR
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Compact inline sync status indicator for headers and top bars.
 */
@Composable
fun SyncStatusIndicator(
    globalSyncState: GlobalSyncState,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val (icon, color, label) = when (globalSyncState) {
        is GlobalSyncState.NotSynced -> Triple(
            Icons.Default.CloudOff,
            AppColors.grey,
            "NOT SYNCED"
        )
        is GlobalSyncState.Syncing -> Triple(
            Icons.Default.Refresh,
            AppColors.accentGreen,
            if (globalSyncState.currentRepo != null) {
                "SYNCING ${globalSyncState.currentRepo.uppercase()}..."
            } else {
                "SYNCING..."
            }
        )
        is GlobalSyncState.Fresh -> {
            val ageSeconds = (Clock.System.now().toEpochMilliseconds() - globalSyncState.lastSyncMs) / 1000
            val freshnessLabel = when {
                ageSeconds < 5 -> "FRESH"
                ageSeconds < 60 -> "${ageSeconds}s AGO"
                else -> "${ageSeconds / 60}m AGO"
            }
            Triple(Icons.Default.CheckCircle, AppColors.statusOnline, freshnessLabel)
        }
        is GlobalSyncState.Partial -> Triple(
            Icons.Default.Warning,
            AppColors.statusWaiting,
            "${globalSyncState.freshRepos.size}/${globalSyncState.freshRepos.size + globalSyncState.staleRepos.size + globalSyncState.failedRepos.size} SYNCED"
        )
        is GlobalSyncState.Failed -> Triple(
            Icons.Default.Error,
            AppColors.statusOffline,
            "SYNC FAILED"
        )
    }

    // Animate rotation for syncing state
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val isSyncing = globalSyncState is GlobalSyncState.Syncing

    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.clip(AppShapes.pill) else Modifier)
            .then(if (onClick != null) {
                Modifier.background(AppColors.surfaceVariant.copy(alpha = 0.5f), AppShapes.pill)
            } else Modifier)
            .padding(horizontal = if (onClick != null) AppSpacing.sm else 0.dp, vertical = if (onClick != null) AppSpacing.xs else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(14.dp)
                .then(if (isSyncing) Modifier.rotate(rotation) else Modifier)
        )
        if (showLabel) {
            Text(
                text = label,
                style = AppTypography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DETAILED STATUS CARD
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Detailed sync status card for dashboard.
 * Shows per-repository sync state with timestamps.
 */
@Composable
fun SyncStatusCard(
    globalSyncState: GlobalSyncState,
    repoSyncStates: Map<String, SyncState>,
    modifier: Modifier = Modifier,
    onRefreshClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.surfaceContainer, AppShapes.card)
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYNC STATUS",
                style = AppTypography.labelSmall,
                color = AppColors.textSecondary,
                fontWeight = FontWeight.Bold
            )
            SyncStatusIndicator(
                globalSyncState = globalSyncState,
                showLabel = true
            )
        }

        // Progress bar for syncing state
        if (globalSyncState is GlobalSyncState.Syncing) {
            LinearProgressIndicator(
                progress = { globalSyncState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(AppShapes.pill),
                color = AppColors.accentGreen,
                trackColor = AppColors.surfaceVariant
            )
        }

        // Repository status chips
        if (repoSyncStates.isNotEmpty()) {
            Wrap(
                modifier = Modifier.fillMaxWidth(),
                horizontalSpacing = AppSpacing.xs,
                verticalSpacing = AppSpacing.xs
            ) {
                repoSyncStates.forEach { (repoName, state) ->
                    RepoSyncChip(repoName = repoName, syncState = state)
                }
            }
        }

        // Refresh button
        if (onRefreshClick != null && globalSyncState !is GlobalSyncState.Syncing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                MoccaTextButton(
                    text = "REFRESH",
                    onClick = onRefreshClick,
                    textColor = AppColors.accentGreen
                )
            }
        }
    }
}

/**
 * Individual repository sync status chip.
 */
@Composable
private fun RepoSyncChip(
    repoName: String,
    syncState: SyncState,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (syncState) {
        is SyncState.Idle -> AppColors.grey to Icons.Default.HourglassEmpty
        is SyncState.Fetching -> AppColors.accentGreen to Icons.Default.Refresh
        is SyncState.Fresh -> AppColors.statusOnline to Icons.Default.CheckCircle
        is SyncState.Failed -> AppColors.statusOffline to Icons.Default.Error
    }

    Row(
        modifier = modifier
            .background(AppColors.surfaceVariant, AppShapes.pill)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = repoName.uppercase(),
            style = AppTypography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Simple wrap layout for chips.
 */
@Composable
private fun Wrap(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = AppSpacing.sm,
    verticalSpacing: Dp = AppSpacing.sm,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MINIMAL STATUS DOT
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Minimal sync status dot for very compact displays.
 */
@Composable
fun SyncStatusDot(
    globalSyncState: GlobalSyncState,
    modifier: Modifier = Modifier
) {
    val color = when (globalSyncState) {
        is GlobalSyncState.NotSynced -> AppColors.grey
        is GlobalSyncState.Syncing -> AppColors.accentGreen
        is GlobalSyncState.Fresh -> AppColors.statusOnline
        is GlobalSyncState.Partial -> AppColors.statusWaiting
        is GlobalSyncState.Failed -> AppColors.statusOffline
    }

    // Pulsing animation for syncing state
    val infiniteTransition = rememberInfiniteTransition(label = "sync_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val isSyncing = globalSyncState is GlobalSyncState.Syncing

    Box(
        modifier = modifier
            .size(8.dp)
            .background(
                color = if (isSyncing) color.copy(alpha = alpha) else color,
                shape = AppShapes.circle
            )
    )
}
