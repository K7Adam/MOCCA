package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing

/**
 * Skeleton loading placeholder for messages.
 * Terminal aesthetic with true shimmer effect.
 *
 * @param modifier Modifier for the skeleton
 * @param lineCount Number of content lines to show
 */
@Composable
fun MessageSkeleton(
    modifier: Modifier = Modifier,
    lineCount: Int = 3
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Role badge skeleton
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            SkeletonBlock(
                width = 60.dp,
                height = 16.dp,
                showShimmer = true
            )
            SkeletonBlock(
                width = 80.dp,
                height = 12.dp,
                showShimmer = true
            )
        }

        // Content lines skeleton
        repeat(lineCount) { index ->
            val widthFraction = when (index) {
                0 -> 0.95f
                lineCount - 1 -> 0.6f
                else -> 0.85f
            }
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth(widthFraction),
                height = 14.dp,
                showShimmer = true
            )
        }
    }
}

/**
 * Compact message skeleton for single-line loading states.
 */
@Composable
fun CompactMessageSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        SkeletonBlock(width = 48.dp, height = 14.dp, showShimmer = true)
        SkeletonBlock(width = 120.dp, height = 12.dp, showShimmer = true)
    }
}

/**
 * Tool card skeleton for loading tool states.
 */
@Composable
fun ToolCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.surfaceVariant.copy(alpha = 0.2f), AppShapes.small)
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Header row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            // Icon placeholder
            SkeletonBlock(width = 18.dp, height = 18.dp, showShimmer = true)
            // Tool name
            SkeletonBlock(width = 60.dp, height = 14.dp, showShimmer = true)
            Spacer(modifier = Modifier.weight(1f))
            // Status
            SkeletonBlock(width = 50.dp, height = 12.dp, showShimmer = true)
        }

        // Content placeholder
        SkeletonBlock(
            modifier = Modifier.fillMaxWidth(),
            height = 40.dp,
            showShimmer = true
        )
    }
}

/**
 * Session list item skeleton.
 */
@Composable
fun SessionItemSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        // Title line
        SkeletonBlock(
            modifier = Modifier.fillMaxWidth(0.7f),
            height = 16.dp,
            showShimmer = true
        )
        // Subtitle line
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            SkeletonBlock(width = 80.dp, height = 12.dp, showShimmer = true)
            SkeletonBlock(width = 60.dp, height = 12.dp, showShimmer = true)
        }
    }
}

/**
 * Base skeleton block component with shimmer effect.
 *
 * @param modifier Modifier for the block
 * @param width Fixed width (optional if modifier provides width)
 * @param height Height of the block
 * @param showShimmer Whether to show shimmer animation
 */
@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp,
    showShimmer: Boolean = true
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .clip(AppShapes.small)
            .background(AppColors.shimmerBase)
            .shimmer(enabled = showShimmer)
    )
}
