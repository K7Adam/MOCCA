package com.mocca.app.ui.components.terminal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing

/**
 * Skeleton loading placeholder for messages.
 * Terminal aesthetic: rectangular shimmer blocks, no rounded corners.
 */
@Composable
fun MessageSkeleton(
    modifier: Modifier = Modifier,
    lineCount: Int = 3
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    
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
                alpha = alpha
            )
            SkeletonBlock(
                width = 80.dp,
                height = 12.dp,
                alpha = alpha * 0.7f
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
                alpha = alpha
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
    val infiniteTransition = rememberInfiniteTransition(label = "compact_skeleton_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "compact_skeleton_alpha"
    )
    
    Row(
        modifier = modifier.padding(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        SkeletonBlock(width = 48.dp, height = 14.dp, alpha = alpha)
        SkeletonBlock(width = 120.dp, height = 12.dp, alpha = alpha * 0.8f)
    }
}

/**
 * Tool card skeleton for loading tool states.
 */
@Composable
fun ToolCardSkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tool_skeleton_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tool_skeleton_alpha"
    )
    
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
            SkeletonBlock(width = 18.dp, height = 18.dp, alpha = alpha)
            // Tool name
            SkeletonBlock(width = 60.dp, height = 14.dp, alpha = alpha)
            Spacer(modifier = Modifier.weight(1f))
            // Status
            SkeletonBlock(width = 50.dp, height = 12.dp, alpha = alpha * 0.7f)
        }
        
        // Content placeholder
        SkeletonBlock(
            modifier = Modifier.fillMaxWidth(),
            height = 40.dp,
            alpha = alpha * 0.5f
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
    val infiniteTransition = rememberInfiniteTransition(label = "session_skeleton_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "session_skeleton_alpha"
    )
    
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
            alpha = alpha
        )
        // Subtitle line
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            SkeletonBlock(width = 80.dp, height = 12.dp, alpha = alpha * 0.7f)
            SkeletonBlock(width = 60.dp, height = 12.dp, alpha = alpha * 0.6f)
        }
    }
}

/**
 * Base skeleton block component.
 */
@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp,
    alpha: Float
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .alpha(alpha)
            .background(
                color = AppColors.grey,
                shape = AppShapes.small
            )
    )
}
