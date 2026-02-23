package com.mocca.app.ui.components.modern

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing

/**
 * Shimmer configuration for customizing the loading animation.
 *
 * @param durationMs Animation duration in milliseconds (default: 1200ms for smooth effect)
 * @param easing Animation easing curve (default: FastOutSlowInEasing for natural motion)
 * @param baseColor Base color of the shimmer (dark for loading placeholders)
 * @param highlightColor Highlight color that sweeps across (lighter for contrast)
 * @param direction Shimmer direction (default: LeftToRight)
 */
data class ShimmerTheme(
    val durationMs: Int = 1200,
    val easing: androidx.compose.animation.core.Easing = FastOutSlowInEasing,
    val baseColor: Color = AppColors.shimmerBase,
    val highlightColor: Color = AppColors.shimmerHighlight,
    val direction: ShimmerDirection = ShimmerDirection.LeftToRight
)

/**
 * Shimmer animation direction.
 */
enum class ShimmerDirection {
    LeftToRight,
    RightToLeft,
    TopToBottom,
    BottomToTop
}

/**
 * Applies a shimmer loading effect to any composable.
 *
 * Usage:
 * ```
 * Box(
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .height(100.dp)
 *         .shimmer(isLoading)
 * ) {
 *     // Content shown when not loading
 * }
 * ```
 *
 * Note: Apply .clip(shape) before .shimmer() if you need rounded corners.
 *
 * @param enabled Whether shimmer is active (typically your loading state)
 * @param theme Shimmer configuration (colors, duration, direction)
 */
@Composable
fun Modifier.shimmer(
    enabled: Boolean = true,
    theme: ShimmerTheme = ShimmerTheme()
): Modifier {
    if (!enabled) return this

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")

    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = theme.durationMs,
                easing = theme.easing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )

    // Calculate shimmer brush based on progress
    val brush = rememberShimmerBrush(
        progress = shimmerProgress,
        theme = theme
    )

    return this.then(
        Modifier.background(brush = brush)
    )
}

/**
 * Creates a shimmer brush based on the current animation progress.
 */
@Composable
private fun rememberShimmerBrush(
    progress: Float,
    theme: ShimmerTheme
): Brush {
    // Calculate shimmer sweep position based on direction
    val shimmerWidth = 400f // Fixed shimmer highlight width in pixels

    // Calculate gradient start/end based on direction and progress
    val animatedStart = -shimmerWidth + (shimmerWidth * 4 * progress) // Move from left to right

    // Use color stops format: Pair(position, color)
    val colorStops = listOf(
        0.0f to theme.baseColor,
        0.3f to theme.highlightColor,
        0.5f to theme.highlightColor,
        0.8f to theme.baseColor
    )

    return Brush.linearGradient(
        *colorStops.toTypedArray(),
        start = Offset(animatedStart, 0f),
        end = Offset(animatedStart + shimmerWidth, 0f)
    )
}

/**
 * A shimmer loading placeholder that mimics the shape of content to be loaded.
 *
 * @param modifier Modifier for the shimmer box
 * @param isLoading Whether to show shimmer or content
 * @param shape Shape of the shimmer overlay (apply .clip(shape) before shimmer for clipping)
 * @param theme Shimmer configuration
 * @param content Content to show when not loading
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = AppShapes.small,
    theme: ShimmerTheme = ShimmerTheme(),
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
    ) {
        if (isLoading) {
            // Show shimmer placeholder
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = AppColors.shimmerBase,
                        shape = shape
                    )
                    .shimmer(theme = theme)
            )
        } else {
            content()
        }
    }
}

/**
 * A row of shimmer placeholders for list loading states.
 *
 * @param count Number of placeholder items
 * @param modifier Modifier for the container
 * @param itemHeight Height of each placeholder item
 * @param spacing Spacing between items
 */
@Composable
fun ShimmerListPlaceholder(
    count: Int = 5,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 64.dp,
    spacing: Dp = AppSpacing.sm
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.padding(AppSpacing.md),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing)
    ) {
        repeat(count) { index ->
            // Vary width to create realistic list item appearance
            val widthFraction = when (index % 3) {
                0 -> 0.9f
                1 -> 0.75f
                else -> 0.6f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(itemHeight)
                    .shimmer()
            )
        }
    }
}

/**
 * Shimmer placeholder for card-style content.
 */
@Composable
fun ShimmerCardPlaceholder(
    modifier: Modifier = Modifier,
    hasImage: Boolean = true,
    hasTitle: Boolean = true,
    hasSubtitle: Boolean = true
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.md)
    ) {
        // Image placeholder
        if (hasImage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .shimmer()
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(AppSpacing.sm))
        }

        // Title placeholder
        if (hasTitle) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .shimmer()
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(AppSpacing.xs))
        }

        // Subtitle placeholder
        if (hasSubtitle) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .shimmer()
            )
        }
    }
}
