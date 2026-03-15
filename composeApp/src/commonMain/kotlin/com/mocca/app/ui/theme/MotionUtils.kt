package com.mocca.app.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

/**
 * Custom modifier for M3 Expressive scale interaction.
 * Provides a consistent "squish" effect across the app.
 */
@Composable
fun Modifier.moccaClickable(
    onClick: (() -> Unit)?,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.96f,
    rippleColor: Color = AppColors.accentGreen.copy(alpha = 0.1f),
    enabled: Boolean = true
): Modifier {
    if (onClick == null) return this
    
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "moccaClickableScale"
    )
    
    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(color = rippleColor),
            enabled = enabled,
            onClick = onClick
        )
}
