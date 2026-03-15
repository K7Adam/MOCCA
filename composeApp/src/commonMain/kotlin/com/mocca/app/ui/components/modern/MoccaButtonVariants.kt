package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.*

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
            .moccaClickable(onClick = onClick, pressedScale = 0.94f, rippleColor = AppColors.accentGreen.copy(alpha = 0.15f))
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
    val tintColor = if (enabled) iconColor else AppColors.textSecondary
    
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, AppShapes.circle)
            .then(
                if (borderColor != null) {
                    Modifier.border(AppSpacing.borderThin, borderColor, AppShapes.circle)
                } else {
                    Modifier
                }
            )
            .moccaClickable(
                onClick = onClick, 
                pressedScale = 0.9f, 
                rippleColor = AppColors.accentGreen.copy(alpha = 0.15f),
                enabled = enabled
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
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, AppShapes.circle)
            .moccaClickable(onClick = onClick, pressedScale = 0.95f, rippleColor = Color.Black.copy(alpha = 0.15f)),
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
    val color = if (enabled) textColor else AppColors.textSecondaryDark
    
    Box(
        modifier = modifier
            .height(MoccaButtonDefaults.HeightCompact)
            .moccaClickable(
                onClick = onClick, 
                pressedScale = 0.96f, 
                rippleColor = AppColors.accentGreen.copy(alpha = 0.1f),
                enabled = enabled
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
