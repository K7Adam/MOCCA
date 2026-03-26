package com.mocca.app.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Applies a focus border to a component when focused.
 *
 * This modifier provides a consistent focus indicator across MOCCA components,
 * using the design system's colors and shapes.
 *
 * @param interactionSource The interaction source to observe for focus state.
 * @param shape The shape of the border. Defaults to [AppShapes.pill].
 * @param color The border color when focused. Defaults to [AppColors.onSurface].
 * @param width The border width when focused. Defaults to 2.dp.
 */
@Composable
fun Modifier.focusBorder(
    interactionSource: InteractionSource,
    shape: Shape = AppShapes.pill,
    color: Color = AppColors.onSurface,
    width: androidx.compose.ui.unit.Dp = 2.dp
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()

    return if (isFocused) {
        this.border(width, color, shape)
    } else {
        this
    }
}
