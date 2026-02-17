package com.mocca.app.ui.components.terminal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Modern MOCCA button components with pill-shaped design.
 * Based on UI overhaul designs - fully rounded corners, subtle glow effects.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// PRIMARY BUTTON (Pill shape, light background, dark text)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Primary pill button - off-white background, black text, arrow icon.
 * Used for main CTAs like "CONNECT →"
 */
@Composable
fun MoccaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = AppColors.buttonBackground,
    textColor: Color = AppColors.buttonText,
    disabledBackgroundColor: Color = AppColors.greyDark,
    disabledTextColor: Color = AppColors.grey,
    height: Dp = AppSpacing.buttonHeight,
    showBrackets: Boolean = false, // Changed default - modern design doesn't use brackets
    icon: ImageVector? = null,
    showArrow: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "buttonScale"
    )
    
    val bgColor = if (enabled) backgroundColor else disabledBackgroundColor
    val txtColor = if (enabled) textColor else disabledTextColor
    val displayText = if (showBrackets) "[ $text ]" else text
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .background(bgColor, AppShapes.pill)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = Color.Black.copy(alpha = 0.1f)),
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
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
            }
            Text(
                text = displayText.uppercase(),
                color = txtColor,
                style = AppTypography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            if (showArrow) {
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Text(
                    text = "→",
                    color = txtColor,
                    style = AppTypography.labelLarge
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// OUTLINED BUTTON (Pill shape, transparent background, border)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Outlined pill button - transparent background, subtle border.
 * Used for secondary actions.
 */
@Composable
fun MoccaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = AppColors.borderLight,
    textColor: Color = AppColors.white,
    disabledBorderColor: Color = AppColors.greyDark,
    disabledTextColor: Color = AppColors.grey,
    borderWidth: Dp = AppSpacing.borderThin,
    height: Dp = AppSpacing.buttonHeight,
    showBrackets: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "buttonScale"
    )
    
    val brdColor = if (enabled) borderColor else disabledBorderColor
    val txtColor = if (enabled) textColor else disabledTextColor
    val displayText = if (showBrackets) "[ $text ]" else text
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .background(Color.Transparent, AppShapes.pill)
            .border(borderWidth, brdColor, AppShapes.pill)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = Color.White.copy(alpha = 0.1f)),
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
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
            }
            Text(
                text = displayText.uppercase(),
                color = txtColor,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPACT BUTTON (Smaller pill for toolbars)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Compact pill button - smaller height, for action toolbars.
 */
@Composable
fun MoccaCompactButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = AppColors.white,
    textColor: Color = AppColors.background,
    height: Dp = 40.dp,
    paddingHorizontal: Dp = AppSpacing.pillPaddingHorizontal,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
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
                        indication = ripple(color = Color.Black.copy(alpha = 0.1f)),
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
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
            }
            Text(
                text = text.uppercase(),
                color = txtColor,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB PILL BUTTON (For tab selectors)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Tab pill button - for horizontal tab selectors.
 * Active state: filled background. Inactive: transparent with border.
 */
@Composable
fun TabPillButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeBackgroundColor: Color = AppColors.primary,
    activeTextColor: Color = AppColors.white,
    inactiveBackgroundColor: Color = Color.Transparent,
    inactiveBorderColor: Color = AppColors.borderLight,
    inactiveTextColor: Color = AppColors.textSecondary
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .height(40.dp)
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
                indication = ripple(color = Color.White.copy(alpha = 0.1f)),
                onClick = onClick
            )
            .padding(horizontal = AppSpacing.pillPaddingHorizontal),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) activeTextColor else inactiveTextColor,
            style = AppTypography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ICON BUTTON (Circular for FABs and toolbars)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Circular icon button - for FABs and toolbar actions.
 */
@Composable
fun MoccaIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    backgroundColor: Color = Color.Transparent,
    iconColor: Color = AppColors.white,
    borderColor: Color? = null,
    size: Dp = AppSpacing.iconButtonSize
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(100),
        label = "iconButtonScale"
    )
    
    val tintColor = if (enabled) iconColor else AppColors.grey
    
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
                            color = Color.White.copy(alpha = 0.1f)
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
            modifier = Modifier.size(24.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FAB (Floating Action Button)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Floating action button - circular, prominent.
 */
@Composable
fun MoccaFab(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    backgroundColor: Color = AppColors.buttonBackground,
    iconColor: Color = AppColors.buttonText,
    size: Dp = AppSpacing.fabSize
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
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
                    color = Color.Black.copy(alpha = 0.1f)
                ),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TEXT BUTTON (Link style)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Text button - for inline actions, link style.
 */
@Composable
fun MoccaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = AppColors.greyLight
) {
    val interactionSource = remember { MutableInteractionSource() }
    val color = if (enabled) textColor else AppColors.greyDark
    
    Box(
        modifier = modifier
            .height(48.dp)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.1f)),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            style = AppTypography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
