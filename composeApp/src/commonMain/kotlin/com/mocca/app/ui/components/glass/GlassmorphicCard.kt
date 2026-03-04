package com.mocca.app.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing

object GlassmorphicDefaults {
    val BackgroundColor: Color = Color(0x990D0E12)
    val BorderColor: Color = Color(0x407C5CFC)
    val HighlightColor: Color = Color(0x33A0A3B5)
    val ShadowColor: Color = Color(0x400D0E12)
    val BorderWidth: Dp = 1.dp
    val ContentPadding: Dp = AppSpacing.md
    val Shape: Shape = AppShapes.rounded2xl
    val BlurRadius: Dp = 0.dp
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassmorphicDefaults.Shape,
    backgroundColor: Color = GlassmorphicDefaults.BackgroundColor,
    borderColor: Color = GlassmorphicDefaults.BorderColor,
    borderWidth: Dp = GlassmorphicDefaults.BorderWidth,
    contentPadding: Dp = GlassmorphicDefaults.ContentPadding,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun GlassmorphicCardPremium(
    modifier: Modifier = Modifier,
    shape: Shape = GlassmorphicDefaults.Shape,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .glassyPremium()
            .padding(GlassmorphicDefaults.ContentPadding)
    ) {
        content()
    }
}

@Composable
fun Modifier.glassyMint(
    shape: Shape = AppShapes.rounded2xl,
    borderWidth: Dp = 1.dp,
    backgroundAlpha: Float = 0.6f,
    borderAlpha: Float = 0.25f
): Modifier = this
    .clip(shape)
    .glassyBackground(alpha = backgroundAlpha)
    .border(borderWidth, AppColors.accentGreen.copy(alpha = borderAlpha), shape)

@Composable
fun Modifier.glassyBackground(alpha: Float = 0.6f): Modifier = this.then(
    Modifier.background(AppColors.background.copy(alpha = alpha))
)

@Composable
fun Modifier.glassyWithBlur(
    shape: Shape = AppShapes.rounded2xl,
    blurRadius: Dp = 8.dp,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = Color(0x990D0E12),
    borderColor: Color = Color(0x407C5CFC)
): Modifier = this
    .clip(shape)
    .blur(blurRadius)
    .background(backgroundColor)
    .border(borderWidth, borderColor, shape)

@Composable
fun Modifier.glassyPremium(
    shape: Shape = AppShapes.rounded2xl,
    backgroundColor: Color = GlassmorphicDefaults.BackgroundColor,
    borderColor: Color = GlassmorphicDefaults.BorderColor,
    highlightColor: Color = GlassmorphicDefaults.HighlightColor,
    shadowColor: Color = GlassmorphicDefaults.ShadowColor,
    borderWidth: Dp = GlassmorphicDefaults.BorderWidth
): Modifier = this
    .clip(shape)
    .glassPremium(
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        highlightColor = highlightColor,
        shadowColor = shadowColor,
        borderWidth = borderWidth
    )

@Composable
fun Modifier.glassyAppBar(
    hasBottomBorder: Boolean = true
): Modifier = this.then(
    Modifier.glassyBackground(alpha = 0.82f).then(
        if (hasBottomBorder) {
            Modifier.border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        AppColors.accentGreen.copy(alpha = 0.15f)
                    )
                ),
                shape = AppShapes.none
            )
        } else {
            Modifier
        }
    )
)

@Composable
fun Modifier.glassyFab(
    shape: Shape = AppShapes.circle,
    backgroundAlpha: Float = 0.7f,
    borderAlpha: Float = 0.3f
): Modifier = this
    .clip(shape)
    .background(AppColors.background.copy(alpha = backgroundAlpha))
    .border(1.5.dp, AppColors.accentGreen.copy(alpha = borderAlpha), shape)
