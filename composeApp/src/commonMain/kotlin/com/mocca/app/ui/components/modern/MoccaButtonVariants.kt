package com.mocca.app.ui.components.modern

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
fun TabPillButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeBackgroundColor: Color = AppColors.accentGreen,
    activeTextColor: Color = AppColors.background,
    inactiveBackgroundColor: Color = Color.Transparent,
    inactiveBorderColor: Color = AppColors.accentGreen.copy(alpha = 0.4f),
    inactiveTextColor: Color = AppColors.textSecondary
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .height(MoccaButtonDefaults.HeightCompact)
            .then(
                if (isSelected) {
                    Modifier.background(activeBackgroundColor, AppShapes.pill)
                } else {
                    Modifier
                        .background(inactiveBackgroundColor, AppShapes.pill)
                        .border(AppSpacing.borderThin, inactiveBorderColor, AppShapes.pill)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.accentGreen.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(horizontal = MoccaButtonDefaults.PaddingHorizontal),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = if (isSelected) activeTextColor else inactiveTextColor,
            style = AppTypography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun MoccaIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    backgroundColor: Color = Color.Transparent,
    iconColor: Color = AppColors.textPrimary,
    borderColor: Color? = null,
    size: Dp = AppSpacing.iconButtonSizeCompact
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "iconButtonScale"
    )
    
    val tintColor = if (enabled) iconColor else AppColors.textSecondary
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .background(backgroundColor, AppShapes.circle)
            .then(
                if (borderColor != null) {
                    Modifier.border(AppSpacing.borderThin, borderColor, AppShapes.circle)
                } else {
                    Modifier
                }
            )
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(
                            bounded = true,
                            color = AppColors.accentGreen.copy(alpha = 0.15f)
                        ),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MoccaFab(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    backgroundColor: Color = AppColors.accentGreen,
    iconColor: Color = AppColors.background,
    size: Dp = AppSpacing.fabSize
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "fabScale"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .background(backgroundColor, AppShapes.circle)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = Color.Black.copy(alpha = 0.15f)
                ),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun MoccaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = AppColors.textSecondaryLight
) {
    val interactionSource = remember { MutableInteractionSource() }
    val color = if (enabled) textColor else AppColors.textSecondaryDark
    
    Box(
        modifier = modifier
            .height(MoccaButtonDefaults.HeightCompact)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, color = AppColors.accentGreen.copy(alpha = 0.1f)),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = AppSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            style = AppTypography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
