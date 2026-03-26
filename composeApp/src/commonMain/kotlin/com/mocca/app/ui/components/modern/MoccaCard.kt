package com.mocca.app.ui.components.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.focusBorder
import com.mocca.app.ui.theme.innerShadow

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.shrinkVertically
import com.mocca.app.ui.theme.*

@Composable
fun MoccaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = AppColors.surfaceContainer,
    borderColor: Color = AppColors.outline,
    borderWidth: Dp = AppSpacing.borderThin,
    contentPadding: Dp = AppSpacing.cardPadding,
    shape: Shape = AppShapes.card,
    isLoading: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .innerShadow(shape = shape, color = Color.Black.copy(alpha = 0.25f), blur = 12.dp)
            .border(borderWidth, borderColor, shape)
            .focusBorder(interactionSource, shape)
            .moccaClickable(onClick = onClick, pressedScale = 0.98f, interactionSource = interactionSource)
            .then(if (isLoading) Modifier.shimmer() else Modifier)
            .animateContentSize(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
            .padding(contentPadding)
    ) {
        AnimatedVisibility(
            visible = !isLoading,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + expandVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) + shrinkVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun MoccaCardElevated(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = AppColors.surfaceContainer,
    borderColor: Color = AppColors.outlineVariant,
    borderWidth: Dp = AppSpacing.borderThin,
    contentPadding: Dp = AppSpacing.cardPadding,
    shape: Shape = AppShapes.card,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .innerShadow(shape = shape, color = Color.Black.copy(alpha = 0.4f), blur = 16.dp)
            .border(borderWidth, borderColor, shape)
            .focusBorder(interactionSource, shape)
            .moccaClickable(onClick = onClick, pressedScale = 0.98f, rippleColor = AppColors.primary.copy(alpha = 0.15f), interactionSource = interactionSource)
            .animateContentSize(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun StatusMonitorCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = AppColors.surfaceContainer,
    borderColor: Color = AppColors.outline,
    borderWidth: Dp = AppSpacing.borderThin,
    contentPadding: Dp = AppSpacing.cardPaddingLarge,
    shape: Shape = AppShapes.extraLarge,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .innerShadow(shape = shape, color = Color.Black.copy(alpha = 0.15f), blur = 20.dp)
            .border(borderWidth, borderColor, shape)
            .focusBorder(interactionSource, shape)
            .moccaClickable(onClick = onClick, pressedScale = 0.98f, interactionSource = interactionSource)
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun MoccaSectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leftBorderColor: Color = AppColors.primary,
    leftBorderWidth: Dp = AppSpacing.borderStandard,
    backgroundColor: Color = AppColors.surfaceVariant,
    contentPadding: Dp = AppSpacing.lg,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(AppShapes.medium)
            .background(backgroundColor, AppShapes.medium)
            .drawBehind {
                drawRect(
                    color = leftBorderColor,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(leftBorderWidth.toPx(), size.height)
                )
            }
            .focusBorder(interactionSource, AppShapes.medium)
            .moccaClickable(onClick = onClick, pressedScale = 0.99f, rippleColor = AppColors.primary.copy(alpha = 0.05f), interactionSource = interactionSource)
            .animateContentSize(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
            .padding(start = contentPadding + leftBorderWidth, end = contentPadding, 
                     top = contentPadding, bottom = contentPadding),
        content = content
    )
}

@Composable
fun MoccaSessionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isActive: Boolean = false,
    backgroundColor: Color = AppColors.surfaceContainerHigh,
    borderColor: Color = AppColors.surfaceContainerHighest.copy(alpha = 0.3f),
    activeIndicatorColor: Color = AppColors.primary,
    activeIndicatorWidth: Dp = AppSpacing.activeIndicatorWidth,
    contentPadding: Dp = AppSpacing.cardPadding,
    shape: Shape = if (isActive) AppShapes.slanted else AppShapes.sessionCard,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isActive) AppColors.surfaceContainerHighest else backgroundColor,
                shape
            )
            .border(AppSpacing.borderThin, if (isActive) activeIndicatorColor else borderColor, shape)
            .focusBorder(interactionSource, shape)
            .moccaClickable(onClick = onClick, pressedScale = 0.97f, interactionSource = interactionSource)
            .animateContentSize(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun ModuleToolCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = AppColors.moduleBackground,
    borderColor: Color = AppColors.outline,
    contentPadding: Dp = AppSpacing.modulePadding,
    shape: Shape = AppShapes.moduleCard,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(AppSpacing.borderThin, borderColor, shape)
            .focusBorder(interactionSource, shape)
            .moccaClickable(onClick = onClick, pressedScale = 0.96f, interactionSource = interactionSource)
            .animateContentSize(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = AppColors.surfaceContainerHigh,
    borderColor: Color = AppColors.outline,
    contentPadding: Dp = AppSpacing.cardPadding,
    shape: Shape = AppShapes.card,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(AppSpacing.borderThin, borderColor, shape)
            .focusBorder(interactionSource, shape)
            .moccaClickable(onClick = onClick, pressedScale = 0.98f, interactionSource = interactionSource)
            .animateContentSize(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
            .padding(contentPadding),
        content = content
    )
}