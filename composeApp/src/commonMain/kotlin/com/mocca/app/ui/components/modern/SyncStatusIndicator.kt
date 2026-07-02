package com.mocca.app.ui.components.modern

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.GlobalSyncState
import com.mocca.app.domain.model.SyncState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.moccaClickable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import kotlin.time.Clock

/**
 * Sync status indicator for showing data freshness.
 */
@Composable
fun SyncStatusIndicator(
    globalSyncState: GlobalSyncState,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    // Only show visual state for errors or manual sync
    val (icon, color, label) = when (globalSyncState) {
        is GlobalSyncState.NotSynced -> Triple(
            Icons.Default.CloudOff,
            AppColors.onSurfaceVariant,
            null // Don't show label for NotSynced - not intrusive
        )
        is GlobalSyncState.Syncing -> Triple(
            Icons.Default.Refresh,
            AppColors.primary,
            if (globalSyncState.progress > 0) "SYNCING..." else null
        )
        is GlobalSyncState.Fresh -> Triple(
            Icons.Default.CheckCircle,
            AppColors.statusOnline,
            null // Don't show label for Fresh - not intrusive
        )
        is GlobalSyncState.Partial -> Triple(
            Icons.Default.Warning,
            AppColors.statusWaiting,
            "PARTIAL" // Show warning for partial
        )
        is GlobalSyncState.Failed -> Triple(
            Icons.Default.Error,
            AppColors.statusOffline,
            "SYNC ERROR" // Always show errors
        )
    }

    val isSyncing = globalSyncState is GlobalSyncState.Syncing
    val isError = globalSyncState is GlobalSyncState.Failed

    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.clip(AppShapes.pill) else Modifier)
            .then(if (onClick != null) {
                Modifier.background(
                    if (isError) AppColors.error.copy(alpha = 0.2f)
                    else AppColors.surfaceVariant.copy(alpha = 0.5f),
                    AppShapes.pill
                )
            } else Modifier)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = if (onClick != null) AppSpacing.sm else 0.dp, vertical = if (onClick != null) AppSpacing.xs else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        if (isSyncing) {
            LoadingIndicator(
                modifier = Modifier.size(14.dp),
                color = color,
                polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
        // Only show label when there's meaningful content
        if (showLabel && label != null) {
            Text(
                text = label,
                style = AppTypography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// MINIMAL STATUS CARD - No progress bar, no flicker


/**
 * Minimal sync status card for dashboard.
 * NO progress bar - prevents flicker during background sync.
 * Only shows status when there's something to report.
 */
@Composable
fun SyncStatusCard(
    globalSyncState: GlobalSyncState,
    repoSyncStates: Map<String, SyncState>,
    modifier: Modifier = Modifier,
    onRefreshClick: (() -> Unit)? = null
) {
    // Only show the card if there's something meaningful to display
    // (errors, or user-triggered sync)
    val hasError = globalSyncState is GlobalSyncState.Failed
    val isSyncing = globalSyncState is GlobalSyncState.Syncing
    val isPartial = globalSyncState is GlobalSyncState.Partial
    
    // Don't render the whole card if everything is fine and quiet
    if (!hasError && !isSyncing && !isPartial && repoSyncStates.isEmpty()) {
        // Just show a minimal refresh button
        if (onRefreshClick != null) {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .moccaClickable(onClick = onRefreshClick, pressedScale = 0.92f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = AppColors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        return
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (hasError) AppColors.error.copy(alpha = 0.1f)
                else AppColors.surfaceContainer,
                AppShapes.card
            )
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasError) "SYNC ERROR" else "SYNC STATUS",
                style = AppTypography.labelSmall,
                color = if (hasError) AppColors.error else AppColors.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            SyncStatusIndicator(
                globalSyncState = globalSyncState,
                showLabel = true
            )
        }

        // Show failed repos if any
        if (globalSyncState is GlobalSyncState.Failed) {
            globalSyncState.errors.forEach { (repo, error) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = AppColors.error,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${repo.uppercase()}: $error",
                        style = AppTypography.labelSmall,
                        color = AppColors.error
                    )
                }
            }
        }

        // Refresh button (always available, but not during sync)
        if (onRefreshClick != null && !isSyncing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                MoccaTextButton(
                    text = "REFRESH",
                    onClick = onRefreshClick,
                    textColor = if (hasError) AppColors.error else AppColors.primary
                )
            }
        }
    }
}

// MINIMAL STATUS DOT - For connection indicators


/**
 * Minimal sync status dot for connection indicators.
 * Static dot - no pulsing animation to avoid distraction.
 */
@Composable
fun SyncStatusDot(
    globalSyncState: GlobalSyncState,
    modifier: Modifier = Modifier
) {
    val color = when (globalSyncState) {
        is GlobalSyncState.NotSynced -> AppColors.onSurfaceVariant
        is GlobalSyncState.Syncing -> AppColors.primary
        is GlobalSyncState.Fresh -> AppColors.statusOnline
        is GlobalSyncState.Partial -> AppColors.statusWaiting
        is GlobalSyncState.Failed -> AppColors.statusOffline
    }

    // Static dot - no animation to prevent visual noise
    Box(
        modifier = modifier
            .size(8.dp)
            .background(color = color, shape = AppShapes.circle)
    )
}
