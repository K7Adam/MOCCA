package com.mocca.app.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import androidx.compose.material3.MaterialTheme

/**
 * Terminal-styled buttons with blocky appearance.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// PRIMARY BUTTON (Light grey background, black text)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Primary terminal button - light grey background, black text.
 * Used for main actions like "[ CONNECT ]".
 */
@Composable
fun TerminalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = TerminalColors.buttonBackground,
    textColor: Color = TerminalColors.buttonText,
    disabledBackgroundColor: Color = TerminalColors.greyDark,
    disabledTextColor: Color = TerminalColors.grey,
    height: Dp = TerminalSpacing.buttonHeight,
    showBrackets: Boolean = true,
    icon: ImageVector? = null
) {
    val bgColor = if (enabled) backgroundColor else disabledBackgroundColor
    val txtColor = if (enabled) textColor else disabledTextColor
    val displayText = if (showBrackets) "[ $text ]" else text
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(bgColor, RectangleShape)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.Black.copy(alpha = 0.2f)),
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
                Spacer(modifier = Modifier.width(TerminalSpacing.sm))
            }
            Text(
                text = displayText.uppercase(),
                color = txtColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// OUTLINED BUTTON (Transparent background, white border)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Outlined terminal button - transparent background, white border.
 * Used for secondary actions.
 */
@Composable
fun TerminalOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = TerminalColors.borderLight,
    textColor: Color = TerminalColors.white,
    disabledBorderColor: Color = TerminalColors.greyDark,
    disabledTextColor: Color = TerminalColors.grey,
    borderWidth: Dp = TerminalSpacing.borderThin,
    height: Dp = TerminalSpacing.buttonHeight,
    showBrackets: Boolean = true,
    icon: ImageVector? = null
) {
    val brdColor = if (enabled) borderColor else disabledBorderColor
    val txtColor = if (enabled) textColor else disabledTextColor
    val displayText = if (showBrackets) "[ $text ]" else text
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.Transparent, RectangleShape)
            .border(borderWidth, brdColor, RectangleShape)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
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
                Spacer(modifier = Modifier.width(TerminalSpacing.sm))
            }
            Text(
                text = displayText.uppercase(),
                color = txtColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPACT BUTTON (smaller, for toolbars)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Compact terminal button - smaller height, for action toolbars.
 */
@Composable
fun TerminalCompactButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = TerminalColors.white,
    textColor: Color = TerminalColors.background,
    height: Dp = 36.dp,
    paddingHorizontal: Dp = TerminalSpacing.lg,
    icon: ImageVector? = null
) {
    val bgColor = if (enabled) backgroundColor else TerminalColors.greyDark
    val txtColor = if (enabled) textColor else TerminalColors.grey
    
    Box(
        modifier = modifier
            .height(height)
            .background(bgColor, RectangleShape)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
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
                    contentDescription = null,
                    tint = txtColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(TerminalSpacing.xs))
            }
            Text(
                text = text.uppercase(),
                color = txtColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ICON BUTTON (square, for toolbars)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Square icon button - for toolbar actions.
 */
@Composable
fun TerminalIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    backgroundColor: Color = Color.Transparent,
    iconColor: Color = TerminalColors.white,
    borderColor: Color? = null,
    size: Dp = TerminalSpacing.iconButtonSize
) {
    val tintColor = if (enabled) iconColor else TerminalColors.grey
    
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, RectangleShape)
            .then(
                if (borderColor != null) {
                    Modifier.border(TerminalSpacing.borderThin, borderColor, RectangleShape)
                } else {
                    Modifier
                }
            )
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
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
// TEXT BUTTON (underlined link style)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Text button - for inline actions like "CLEAR", "RETRY", "CONFIG".
 * Includes minimum touch target size of 48dp for accessibility.
 */
@Composable
fun TerminalTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = TerminalColors.greyLight
) {
    val color = if (enabled) textColor else TerminalColors.greyDark
    
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.1f)),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = TerminalSpacing.md, vertical = TerminalSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
