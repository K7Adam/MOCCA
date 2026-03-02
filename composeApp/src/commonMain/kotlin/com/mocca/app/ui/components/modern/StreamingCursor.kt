package com.mocca.app.ui.components.modern

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppTypography

@Composable
fun StreamingCursor(
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AppColors.accentGreen,
    style: TextStyle = AppTypography.bodyLarge
) {
    if (!isStreaming) return

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Text(
        text = "|",
        color = color,
        style = style,
        modifier = modifier.alpha(alpha)
    )
}
