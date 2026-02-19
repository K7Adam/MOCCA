@file:Suppress("DEPRECATION")

package com.mocca.app.ui.components.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppSpacing

/**
 * Glass-styled top app bar.
 * 
 * A drop-in replacement for TopAppBar that uses the Liquid Glass material.
 * Features full-width glass effect with optimized parameters for header bars.
 * 
 * Usage:
 * ```kotlin
 * GlassAppBar(
 *     title = { Text("Screen Title") },
 *     navigationIcon = { IconButton(onClick = {}) { Icon(...) } },
 *     actions = {
 *         IconButton(onClick = {}) { Icon(...) }
 *     }
 * )
 * ```
 * 
 * @param title Title content (typically Text)
 * @param modifier Modifier for the app bar
 * @param navigationIcon Navigation icon content (typically IconButton)
 * @param actions Action buttons content (typically IconButtons)
 * @param tokens Theme-aware glass tokens
 * @param height Height of the app bar
 * @param contentPadding Horizontal padding for content
 * @param reducedTransparency Accessibility mode - use solid background
 */
@Composable
fun GlassAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    height: Dp = AppSpacing.topBarHeight,
    contentPadding: Dp = AppSpacing.screenPaddingHorizontal,
    reducedTransparency: Boolean = false
) {
    GlassAppBarLayout(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .statusBarsPadding(),
        tokens = tokens,
        reducedTransparency = reducedTransparency,
        contentPadding = contentPadding,
        navigationIcon = navigationIcon,
        title = title,
        actions = actions
    )
}

/**
 * Glass-styled top app bar with custom content.
 * 
 * Use this variant when you need full control over the app bar content layout.
 * 
 * @param content Custom content for the app bar
 * @param modifier Modifier for the app bar
 * @param tokens Theme-aware glass tokens
 * @param height Height of the app bar
 * @param reducedTransparency Accessibility mode
 */
@Composable
fun GlassAppBar(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    height: Dp = AppSpacing.topBarHeight,
    reducedTransparency: Boolean = false
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .statusBarsPadding(),
        tokens = tokens,
        shape = GlassDefaults.shapeAppBar(),
        shaderParams = GlassShaderParams.AppBar,
        reducedTransparency = reducedTransparency
    ) {
        content()
    }
}

/**
 * Internal layout for the standard GlassAppBar.
 */
@Composable
private fun GlassAppBarLayout(
    modifier: Modifier,
    tokens: GlassThemeTokens,
    reducedTransparency: Boolean,
    contentPadding: Dp,
    navigationIcon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        tokens = tokens,
        shape = GlassDefaults.shapeAppBar(),
        shaderParams = GlassShaderParams.AppBar,
        reducedTransparency = reducedTransparency
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                navigationIcon()
            }
            
            // Title - centered or start-aligned based on content
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                title()
            }
            
            // Actions
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
            }
        }
    }
}

/**
 * Compact glass app bar with minimal height.
 * Suitable for screens with limited vertical space.
 */
@Composable
fun GlassAppBarCompact(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    reducedTransparency: Boolean = false
) {
    GlassAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        tokens = tokens,
        height = 48.dp,
        contentPadding = 12.dp,
        reducedTransparency = reducedTransparency
    )
}

/**
 * Glass app bar with floating appearance.
 * Uses rounded corners and floating glass effect.
 */
@Composable
fun GlassAppBarFloating(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    shape: Shape = GlassDefaults.shapeFloating(),
    reducedTransparency: Boolean = false
) {
    GlassSurfaceFloating(
        modifier = modifier
            .fillMaxWidth()
            .height(AppSpacing.topBarHeight),
        tokens = tokens,
        shape = shape,
        reducedTransparency = reducedTransparency
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPaddingHorizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                navigationIcon()
            }
            
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                title()
            }
            
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
            }
        }
    }
}
