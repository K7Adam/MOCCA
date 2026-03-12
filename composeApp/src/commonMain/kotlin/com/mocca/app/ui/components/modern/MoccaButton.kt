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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import com.mocca.app.ui.theme.AppAnimations

object MoccaButtonDefaults {
    val Height: Dp = 48.dp
    val HeightCompact: Dp = 36.dp
    val HeightSmall: Dp = 32.dp
    val IconSize: Dp = 18.dp
    val IconSizeSmall: Dp = 14.dp
    val PaddingHorizontal: Dp = 12.dp
    val SpacingIcon: Dp = 6.dp
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoccaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = AppColors.accentGreen,
    textColor: Color = AppColors.background,
    disabledBackgroundColor: Color = AppColors.greyDark,
    disabledTextColor: Color = AppColors.grey,
    height: Dp = MoccaButtonDefaults.Height,
    showBrackets: Boolean = false,
    icon: ImageVector? = null,
    showArrow: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Use M3 Expressive Motion Scheme for scale animation
    val scaleSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
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
                        indication = ripple(color = Color.Black.copy(alpha = 0.15f)),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
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

/**
 * Material 3 Expressive Split Button for Mocca.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            SplitButtonDefaults.FilledLeadingButton(
                onClick = onPrimaryClick,
                enabled = enabled,
                colors = SplitButtonDefaults.filledLeadingButtonColors(
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
            SplitButtonDefaults.FilledTrailingButton(
                checked = checked,
                onCheckedChange = { onCheckedChange?.invoke(it) ?: onTrailingClick() },
                enabled = enabled,
                colors = SplitButtonDefaults.filledTrailingButtonColors(
                    containerColor = backgroundColor,
                    contentColor = textColor
                )
            ) {
                val rotation by animateFloatAsState(if (checked) 180f else 0f)
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
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
    textColor: Color = AppColors.white,
    disabledBorderColor: Color = AppColors.greyDark,
    disabledTextColor: Color = AppColors.grey,
    borderWidth: Dp = AppSpacing.borderThin,
    height: Dp = MoccaButtonDefaults.Height,
    showBrackets: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = if (isPressed) AppAnimations.SpringBouncy else AppAnimations.SpringSmooth,
        label = "buttonScale"
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
                        indication = ripple(color = AppColors.accentGreen.copy(alpha = 0.15f)),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
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
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = if (isPressed) AppAnimations.SpringBouncy else AppAnimations.SpringSmooth,
        label = "buttonScale"
    )
    
    val bgColor = if (enabled) backgroundColor else AppColors.greyDark
    val txtColor = if (enabled) textColor else AppColors.grey
    
    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .background(bgColor, AppShapes.pill)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = Color.Black.copy(alpha = 0.15f)),
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
                    contentDescription = null,
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
