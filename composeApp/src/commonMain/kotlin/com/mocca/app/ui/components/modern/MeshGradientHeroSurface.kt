package com.mocca.app.ui.components.modern

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors

/**
 * Decorative onboarding hero surface using layered animated gradients.
 *
 * This intentionally stays local to low-density onboarding content and is not
 * used on chat-heavy surfaces.
 */
@Composable
fun MeshGradientHeroSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "mesh-gradient")
    val phaseA by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phaseA"
    )
    val phaseB by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phaseB"
    )

    val gradient = Brush.radialGradient(
        colors = listOf(
            AppColors.accent.copy(alpha = 0.22f),
            AppColors.primary.copy(alpha = 0.16f),
            AppColors.surfaceContainerHigh.copy(alpha = 0.82f),
            AppColors.surface
        ),
        center = Offset(x = 320f * phaseA, y = 220f * phaseB),
        radius = 900f
    )

    val overlay = Brush.radialGradient(
        colors = listOf(
            AppColors.onSurface.copy(alpha = 0.0f),
            AppColors.surface.copy(alpha = 0.88f)
        ),
        center = Offset(x = 520f * phaseB, y = 340f * phaseA),
        radius = 980f
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .background(overlay)
    ) {
        Box(content = content)
    }
}
