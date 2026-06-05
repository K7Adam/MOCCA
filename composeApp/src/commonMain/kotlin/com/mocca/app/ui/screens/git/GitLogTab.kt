package com.mocca.app.ui.screens.git

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.*
import com.mocca.app.domain.model.*
import com.mocca.app.ui.components.GodButton
import com.mocca.app.ui.components.GodBadge

@Composable
internal fun LogTab(uiState: GitUiState, screenModel: GitScreenModel) {
    val commits = uiState.log?.commits ?: emptyList()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.cardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(items = commits, key = { _, commit -> commit.hash }) { index, commit ->
            LogTimelineItem(
                commit = commit,
                isLast = index == commits.size - 1,
                hasMore = uiState.log?.hasMore == true
            )
        }
        
        if (uiState.log?.hasMore == true) {
            item {
                Box(modifier = Modifier.padding(start = AppSpacing.cardPaddingLarge, top = AppSpacing.lg)) {
                    GodButton(
                        text = "LOAD MORE",
                        onClick = { screenModel.loadLog(skip = commits.size) },
                        containerColor = AppColors.white.copy(alpha = 0.05f),
                        contentColor = AppColors.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun LogTimelineItem(
    commit: GitCommit,
    isLast: Boolean,
    hasMore: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Column
        Column(
            modifier = Modifier.width(AppSpacing.avatarSizeLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top circle
            Box(
                modifier = Modifier
                    .size(AppSpacing.avatarSizeLarge)
                    .background(AppColors.surfaceContainerHigh, AppShapes.circle)
                    .border(BorderStroke(AppSpacing.borderThin, AppColors.white.copy(alpha = 0.1f)), AppShapes.circle),
                contentAlignment = Alignment.Center
            ) {
                if (commit.author.contains("Bot", ignoreCase = true)) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = AppColors.white.copy(alpha = 0.4f),
                        modifier = Modifier.size(AppSpacing.iconSizeMedium)
                    )
                } else {
                    // Placeholder for avatar
                    Text(
                        text = commit.author.take(1).uppercase(),
                        style = AppTypography.labelMedium,
                        color = AppColors.onSurface
                    )
                }
            }
            
            // Connecting line
            if (!isLast || hasMore) {
                Box(
                    modifier = Modifier
                        .width(AppSpacing.borderThin)
                        .height(AppSpacing.bottomBarClearance)
                        .background(AppColors.white.copy(alpha = 0.1f))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(AppSpacing.lg))
        
        // Content Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text(
                        text = commit.message,
                        style = AppTypography.titleSmall,
                        color = AppColors.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (commit.author.contains("Bot", ignoreCase = true)) {
                        GodBadge(
                            text = "BOT",
                            containerColor = AppColors.primary.copy(alpha = 0.2f),
                            contentColor = AppColors.primary
                        )
                    }
                }
                Text(
                    text = commit.shortHash,
                    style = AppTypography.codeSmall,
                    color = AppColors.white.copy(alpha = 0.3f),
                    modifier = Modifier
                        .background(AppColors.white.copy(alpha = 0.05f), AppShapes.badge)
                        .padding(horizontal = AppSpacing.badgePaddingHorizontal, vertical = AppSpacing.badgePaddingVertical)
                )
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            
            Text(
                text = commit.message,
                style = AppTypography.bodySmall,
                color = AppColors.white.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Text(
                    text = commit.author,
                    style = AppTypography.labelSmall,
                    color = AppColors.white.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Medium
                )
                Box(modifier = Modifier.size(AppSpacing.statusDotSizeSmall).background(AppColors.white.copy(alpha = 0.2f), AppShapes.circle))
                Text(
                    text = formatRelativeTime(commit.date), 
                    style = AppTypography.labelSmall,
                    color = AppColors.white.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * Formats an epoch millis timestamp to a human-readable relative time string.
 */
internal fun formatRelativeTime(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - epochMillis
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30
    val years = days / 365
    
    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        weeks < 5 -> "${weeks}w ago"
        months < 12 -> "${months}mo ago"
        else -> "${years}y ago"
    }
}
