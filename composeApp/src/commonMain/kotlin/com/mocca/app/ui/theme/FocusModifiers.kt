package com.mocca.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val animatedColor by animateColorAsState(
        targetValue = if (isFocused) color else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "focusBorderColor"
    )

    return if (animatedColor.alpha > 0f) {
        this.border(width, animatedColor, shape)
    } else {
        this
    }
}

/**
 * Applies a [contentDescription] to the component for screen reader accessibility.
 *
 * Use this on interactive controls that lack visible text labels (icon-only buttons,
 * icon-based toggles, close buttons, back buttons, etc.). Do not use on decorative
 * elements or elements that already have text content.
 *
 * @param description The accessibility description read aloud by screen readers.
 */
fun Modifier.accessibleLabel(description: String): Modifier = this.semantics {
    contentDescription = description
}

/**
 * Marks a composable as decorative so that screen readers skip it.
 *
 * Uses [mergeDescendants] to fold this node into its parent, effectively
 * hiding it from TalkBack. Use on purely decorative icons or images that
 * convey no semantic meaning and are redundant with adjacent text.
 */
fun Modifier.decorativeElement(): Modifier = this.semantics(mergeDescendants = true) { }
