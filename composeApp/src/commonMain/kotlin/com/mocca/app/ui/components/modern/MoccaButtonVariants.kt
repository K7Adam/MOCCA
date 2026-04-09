package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
    activeBackgroundColor: Color = AppColors.primary,
    activeTextColor: Color = AppColors.background,
    inactiveBackgroundColor: Color = Color.Transparent,
    inactiveBorderColor: Color = AppColors.primary.copy(alpha = 0.4f),
    inactiveTextColor: Color = AppColors.onSurfaceVariant
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(
                if (isSelected) {
                    Modifier.background(activeBackgroundColor, AppShapes.pill)
                } else {
                    Modifier
                        .background(inactiveBackgroundColor, AppShapes.pill)
                        .border(AppSpacing.borderThin, inactiveBorderColor, AppShapes.pill)
                }
            )
            .semantics { role = Role.Tab }
            .moccaClickable(
                onClick = onClick,
                pressedScale = 0.94f,
                rippleColor = AppColors.primary.copy(alpha = 0.15f)
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
    iconColor: Color = AppColors.onSurface,
    borderColor: Color? = null,
    size: Dp = AppSpacing.iconButtonSizeCompact,
    interactionSource: InteractionSource = remember { MutableInteractionSource() }
) {
    val tintColor = if (enabled) iconColor else AppColors.onSurfaceVariant

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .size(size)
            .focusBorder(interactionSource, shape = AppShapes.circle)
            .background(backgroundColor, AppShapes.circle)
            .then(
                if (borderColor != null) {
                    Modifier.border(AppSpacing.borderThin, borderColor, AppShapes.circle)
                } else {
                    Modifier
                }
            )
            .then(
                if (contentDescription != null) {
                    androidx.compose.ui.Modifier.semantics {
                        this.contentDescription = contentDescription
                        this.role = Role.Button
                    }
                } else {
                    androidx.compose.ui.Modifier.semantics {
                        this.role = Role.Button
                    }
                }
            )
            .moccaClickable(
                onClick = onClick,
                pressedScale = 0.9f,
                rippleColor = AppColors.primary.copy(alpha = 0.15f),
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
    backgroundColor: Color = AppColors.primary,
    iconColor: Color = AppColors.background,
    size: Dp = AppSpacing.fabSize
) {
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, AppShapes.circle)
            .then(
                if (contentDescription != null) {
                    androidx.compose.ui.Modifier.semantics {
                        this.contentDescription = contentDescription
                        this.role = Role.Button
                    }
                } else {
                    androidx.compose.ui.Modifier.semantics {
                        this.role = Role.Button
                    }
                }
            )
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
    textColor: Color = AppColors.onSurfaceVariantLight
) {
    val color = if (enabled) textColor else AppColors.onSurfaceVariantDark

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { role = Role.Button }
            .moccaClickable(
                onClick = onClick,
                pressedScale = 0.96f,
                rippleColor = AppColors.primary.copy(alpha = 0.1f),
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
