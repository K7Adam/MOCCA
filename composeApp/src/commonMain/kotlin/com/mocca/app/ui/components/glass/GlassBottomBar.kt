package com.mocca.app.ui.components.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing

/**
 * Glass-styled bottom navigation bar.
 * 
 * A glass-material bottom bar optimized for navigation elements.
 * Features floating appearance with rounded corners and glass effect.
 * 
 * Usage:
 * ```kotlin
 * GlassBottomBar(
 *     modifier = Modifier.align(Alignment.BottomCenter)
 * ) {
 *     IconButton(onClick = {}) { Icon(...) }
 *     IconButton(onClick = {}) { Icon(...) }
 *     IconButton(onClick = {}) { Icon(...) }
 * }
 * ```
 * 
 * @param content Navigation content (typically IconButtons)
 * @param modifier Modifier for the bottom bar
 * @param tokens Theme-aware glass tokens
 * @param shape Shape of the glass surface
 * @param height Height of the bottom bar
 * @param contentPadding Horizontal padding for content
 * @param includeNavigationBars Whether to include navigation bars padding
 * @param reducedTransparency Accessibility mode
 */
@Composable
fun GlassBottomBar(
    content: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    shape: Shape = GlassDefaults.shapeFloating(),
    height: Dp = AppSpacing.bottomBarCompactHeight,
    contentPadding: Dp = AppSpacing.screenPaddingHorizontal,
    includeNavigationBars: Boolean = true,
    reducedTransparency: Boolean = false
) {
    GlassBottomBarLayout(
        modifier = modifier,
        tokens = tokens,
        shape = shape,
        height = height,
        contentPadding = contentPadding,
        includeNavigationBars = includeNavigationBars,
        reducedTransparency = reducedTransparency,
        content = content
    )
}

/**
 * Glass bottom bar with custom content (not limited to Row).
 * 
 * Use this variant when you need full control over the bottom bar content layout.
 * 
 * @param content Custom content for the bottom bar
 * @param modifier Modifier for the bottom bar
 * @param tokens Theme-aware glass tokens
 * @param shape Shape of the glass surface
 * @param height Height of the bottom bar
 * @param reducedTransparency Accessibility mode
 */
@Composable
fun GlassBottomBar(
    content: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    shape: Shape = GlassDefaults.shapeFloating(),
    height: Dp = AppSpacing.bottomBarCompactHeight,
    reducedTransparency: Boolean = false
) {
    GlassSurfaceFloating(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .navigationBarsPadding(),
        tokens = tokens,
        shape = shape,
        reducedTransparency = reducedTransparency,
        content = content
    )
}

/**
 * Internal layout for the standard GlassBottomBar.
 */
@Composable
private fun GlassBottomBarLayout(
    modifier: Modifier,
    tokens: GlassThemeTokens,
    shape: Shape,
    height: Dp,
    contentPadding: Dp,
    includeNavigationBars: Boolean,
    reducedTransparency: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    val containerModifier = modifier
        .fillMaxWidth()
        .height(height)
        .let { if (includeNavigationBars) it.navigationBarsPadding() else it }
    
    GlassSurfaceFloating(
        modifier = containerModifier,
        tokens = tokens,
        shape = shape,
        reducedTransparency = reducedTransparency
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Floating glass bottom bar with width constraints.
 * Centers the bar and constrains maximum width for larger screens.
 * 
 * @param content Navigation content
 * @param modifier Modifier for the bottom bar container
 * @param tokens Theme-aware glass tokens
 * @param minWidth Minimum width of the bar
 * @param maxWidth Maximum width of the bar
 * @param height Height of the bottom bar
 * @param reducedTransparency Accessibility mode
 */
@Composable
fun GlassBottomBarFloating(
    content: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    minWidth: Dp = 320.dp,
    maxWidth: Dp = 440.dp,
    height: Dp = AppSpacing.bottomBarCompactHeight,
    reducedTransparency: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassSurfaceFloating(
            modifier = Modifier
                .widthIn(min = minWidth, max = maxWidth)
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = AppSpacing.screenPaddingHorizontal),
            tokens = tokens,
            shape = AppShapes.rounded2xl,
            reducedTransparency = reducedTransparency
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * Compact glass bottom bar with minimal height.
 * Suitable for screens with limited vertical space.
 */
@Composable
fun GlassBottomBarCompact(
    content: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    reducedTransparency: Boolean = false
) {
    GlassBottomBar(
        content = content,
        modifier = modifier,
        tokens = tokens,
        shape = AppShapes.large,
        height = 48.dp,
        contentPadding = 12.dp,
        reducedTransparency = reducedTransparency
    )
}

/**
 * Glass bottom bar with expanded content area.
 * For bottom bars that need more vertical space (e.g., with input fields).
 * 
 * @param content Custom content for the expanded area
 * @param modifier Modifier for the bottom bar
 * @param tokens Theme-aware glass tokens
 * @param minHeight Minimum height
 * @param maxHeight Maximum height
 * @param reducedTransparency Accessibility mode
 */
@Composable
fun GlassBottomBarExpanded(
    content: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    minHeight: Dp = AppSpacing.bottomBarExpandedMinHeight,
    maxHeight: Dp = AppSpacing.bottomBarExpandedMaxHeight,
    reducedTransparency: Boolean = false
) {
    GlassSurfaceFloating(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tokens = tokens,
        shape = AppShapes.rounded2xl,
        reducedTransparency = reducedTransparency
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPaddingHorizontal),
            content = content
        )
    }
}

/**
 * Glass bottom bar with a liquid glass provider pattern.
 * Allows sharing state for content behind the glass to be sampled.
 * 
 * This is the recommended pattern for authentic liquid glass effect
 * where the glass samples the actual background content rather than
 * using a static screenshot.
 * 
 * Usage:
 * ```kotlin
 * val glassProvider = rememberLiquidGlassProvider()
 * 
 * Box {
 *     // Background content
 *     BackgroundContent(modifier = Modifier.glassSource(glassProvider))
 *     
 *     // Glass bottom bar
 *     GlassBottomBarWithProvider(
 *         glassProvider = glassProvider,
 *         content = { ... }
 *     )
 * }
 * ```
 */
@Composable
fun GlassBottomBarWithProvider(
    glassProvider: LiquidGlassProvider,
    content: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    shape: Shape = GlassDefaults.shapeFloating(),
    height: Dp = AppSpacing.bottomBarCompactHeight,
    reducedTransparency: Boolean = false
) {
    // TODO: Implement full liquid glass provider integration
    // For now, use the standard floating glass surface
    GlassBottomBar(
        content = content,
        modifier = modifier,
        tokens = tokens,
        shape = shape,
        height = height,
        reducedTransparency = reducedTransparency
    )
}

/**
 * Provider for liquid glass state.
 * Use this to share glass state between source content and glass surface.
 */
class LiquidGlassProvider {
    // TODO: Implement state sharing for true liquid glass effect
    // This will enable real-time sampling of background content
}

/**
 * Remembers a LiquidGlassProvider instance.
 */
@Composable
fun rememberLiquidGlassProvider(): LiquidGlassProvider {
    return androidx.compose.runtime.remember { LiquidGlassProvider() }
}

/**
 * Modifier to mark content as a source for liquid glass sampling.
 */
@Composable
fun Modifier.glassSource(provider: LiquidGlassProvider): Modifier {
    // TODO: Implement source marking for liquid glass sampling
    return this
}
