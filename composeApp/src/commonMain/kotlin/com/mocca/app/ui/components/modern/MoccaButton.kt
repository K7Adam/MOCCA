package com.mocca.app.ui.components.modern

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

object MoccaButtonDefaults {
    val Height: Dp = 48.dp
    val HeightCompact: Dp = 36.dp
    val HeightSmall: Dp = 32.dp
    val IconSize: Dp = 20.dp
    val IconSizeSmall: Dp = 16.dp
    val PaddingHorizontal: Dp = 16.dp
    val SpacingIcon: Dp = 8.dp
}

@Composable
fun MoccaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = AppColors.accentGreen,
    textColor: Color = AppColors.background,
    disabledBackgroundColor: Color = AppColors.onSurfaceVariantDark,
    disabledTextColor: Color = AppColors.onSurfaceVariant,
    height: Dp = MoccaButtonDefaults.Height,
    showBrackets: Boolean = false,
    icon: ImageVector? = null,
    showArrow: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // State-of-the-art M3 Expressive scale motion
    val scaleSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = scaleSpec,
        label = "buttonScale"
    )
    
    val bgColor = if (enabled) backgroundColor else disabledBackgroundColor
    val txtColor = if (enabled) textColor else disabledTextColor
    val displayText = if (showBrackets) "[ $text ]" else text
    
    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .background(bgColor, AppShapes.pill)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = Color.Black.copy(alpha = 0.2f)),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = MoccaButtonDefaults.PaddingHorizontal),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$text icon",
                    tint = txtColor,
                    modifier = Modifier.size(MoccaButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(MoccaButtonDefaults.SpacingIcon))
            }
            Text(
                text = displayText.uppercase(),
                color = txtColor,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            if (showArrow) {
                Spacer(modifier = Modifier.width(MoccaButtonDefaults.SpacingIcon))
                Text(
                    text = "→",
                    color = txtColor,
                    style = AppTypography.labelMedium
                )
            }
        }
    }
}

@Composable
fun MoccaSplitButton(
    text: String,
    onPrimaryClick: () -> Unit,
    onTrailingClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    icon: ImageVector? = null,
    backgroundColor: Color = AppColors.accentGreen,
    textColor: Color = AppColors.background
) {
    SplitButtonLayout(
        modifier = modifier,
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = onPrimaryClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = backgroundColor,
                    contentColor = textColor
                )
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize)
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                }
                Text(
                    text = text.uppercase(),
                    style = AppTypography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                checked = checked,
                onCheckedChange = { onCheckedChange?.invoke(it) ?: onTrailingClick() },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = backgroundColor,
                    contentColor = textColor
                )
            ) {
                val rotationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                val rotation by animateFloatAsState(
                    targetValue = if (checked) 180f else 0f,
                    animationSpec = rotationSpec,
                    label = "splitBtnRotation"
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Options",
                    modifier = Modifier
                        .size(SplitButtonDefaults.TrailingIconSize)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
    )
}

@Composable
fun MoccaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = AppColors.accentGreen,
    textColor: Color = AppColors.onSurface,
    disabledBorderColor: Color = AppColors.onSurfaceVariantDark,
    disabledTextColor: Color = AppColors.onSurfaceVariant,
    borderWidth: Dp = AppSpacing.borderThin,
    height: Dp = MoccaButtonDefaults.Height,
    showBrackets: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = scaleSpec,
        label = "buttonScaleOutlined"
    )
    
    val brdColor = if (enabled) borderColor else disabledBorderColor
    val txtColor = if (enabled) textColor else disabledTextColor
    val displayText = if (showBrackets) "[ $text ]" else text
    
    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .background(Color.Transparent, AppShapes.pill)
            .border(borderWidth, brdColor, AppShapes.pill)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = AppColors.accentGreen.copy(alpha = 0.2f)),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = MoccaButtonDefaults.PaddingHorizontal),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$text icon",
                    tint = txtColor,
                    modifier = Modifier.size(MoccaButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(MoccaButtonDefaults.SpacingIcon))
            }
            Text(
                text = displayText.uppercase(),
                color = txtColor,
                style = AppTypography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MoccaCompactButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = AppColors.accentGreen,
    textColor: Color = AppColors.background,
    height: Dp = MoccaButtonDefaults.HeightCompact,
    paddingHorizontal: Dp = MoccaButtonDefaults.PaddingHorizontal,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = scaleSpec,
        label = "buttonScaleCompact"
    )
    
    val bgColor = if (enabled) backgroundColor else AppColors.onSurfaceVariantDark
    val txtColor = if (enabled) textColor else AppColors.onSurfaceVariant
    
    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .background(bgColor, AppShapes.pill)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = Color.Black.copy(alpha = 0.2f)),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = paddingHorizontal),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$text icon",
                    tint = txtColor,
                    modifier = Modifier.size(MoccaButtonDefaults.IconSizeSmall)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text.uppercase(),
                color = txtColor,
                style = AppTypography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}