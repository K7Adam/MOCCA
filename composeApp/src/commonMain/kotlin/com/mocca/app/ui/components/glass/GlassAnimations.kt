package com.mocca.app.ui.components.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.mocca.app.ui.theme.AppAnimations

/**
 * Spring animation parameters for glass components.
 * Uses 2025-2026 best practices with bouncy springs for natural feel.
 */
object GlassAnimations {
    val PressScale = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val ReleaseScale = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val HoverScale = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 200f
    )
}

/**
 * Applies a spring scale animation to a glass component on press.
 * Creates a tactile "press" feel typical of modern glass UIs.
 *
 * Usage:
 * ```
 * Box(
 *     modifier = Modifier
 *         .glassSurface()
 *         .glassSpringPress()
 * ) { ... }
 * ```
 */
@Composable
fun Modifier.glassSpringPress(): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = if (isPressed) GlassAnimations.PressScale else GlassAnimations.ReleaseScale,
        label = "glassPressScale"
    )
    
    return this
        .interactionSource(interactionSource)
        .then(
            if (scale != 1f) Modifier.scale(scale) else Modifier
        )
}

/**
 * Applies hover/focus scale animation to a glass component.
 */
@Composable
fun Modifier.glassSpringHover(enabled: Boolean = true): Modifier {
    if (!enabled) return this
    
    // Note: hover is Android-only, would need platform-specific implementation
    return this
}
