package com.mocca.app.ui.components.modern

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.LocalAppPerformance
import com.mocca.app.ui.theme.innerShadow
/**
 * Modern MOCCA input components with pill-shaped design.
 * Based on UI overhaul designs - 32dp rounded corners, clean aesthetic.
 */

// BASIC TERMINAL INPUT (Pill-shaped, 32dp radius)


/**
 * Basic input field with rounded corners.
 * Modern design: 32dp rounded corners, dark background, subtle border.
 */
@Composable
fun MoccaInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    showPrompt: Boolean = false, // Modern design doesn't use terminal prompts
    backgroundColor: Color = AppColors.surfaceContainer,
    borderColor: Color = Color.Transparent, // Transparent - use background contrast; focus uses animated color
    borderWidth: Dp = 0.dp,
    shape: Shape = AppShapes.input,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val useDecorativeShadows = LocalAppPerformance.current.useHeavyNavigationMotion
    
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isFocused) AppColors.primary else borderColor,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else borderWidth,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                color = AppColors.onSurfaceVariant,
                style = AppTypography.labelMedium
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.inputHeight)
                .clip(shape)
                .background(backgroundColor, shape)
                .innerShadow(
                    enabled = useDecorativeShadows,
                    shape = shape,
                    color = Color.Black.copy(alpha = 0.5f),
                    blur = 8.dp
                )
                .border(animatedBorderWidth, animatedBorderColor, shape)
                .padding(horizontal = AppSpacing.inputPaddingHorizontal)
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showPrompt) {
                    Text(
                        text = "> ",
                        color = AppColors.primary,
                        style = AppTypography.bodyMedium
                    )
                }
                
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = AppTypography.bodyMedium.copy(
                        color = AppColors.onSurface
                    ),
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(AppColors.primary),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    interactionSource = interactionSource,
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = AppColors.textPlaceholder,
                                    style = AppTypography.bodyMedium
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
        
        // Hint (optional helper text)
        if (hint != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = hint,
                color = AppColors.outline,
                style = AppTypography.labelSmall
            )
        }
    }
}

internal object ChatInputDefaults {
    val MinHeight: Dp = 44.dp
    val MaxHeight: Dp = MinHeight + 20.dp.times(4)
}

// COMMAND LINE INPUT (Modern pill-shaped variant)


/**
 * Simple command line input for terminal screen.
 * Modern design: Rounded corners, accent cursor.
 * 
 * Supports command history navigation via up/down arrow callbacks.
 */
@Composable
fun CommandLineInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Enter command...",
    shape: Shape = AppShapes.input,

    // Command history navigation callbacks

    onHistoryUp: (() -> Unit)? = null,
    onHistoryDown: (() -> Unit)? = null
) {
    val useDecorativeShadows = LocalAppPerformance.current.useHeavyNavigationMotion

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.surfaceContainer, shape)
            .innerShadow(
                enabled = useDecorativeShadows,
                shape = shape,
                color = Color.Black.copy(alpha = 0.5f),
                blur = 8.dp
            )
            .border(AppSpacing.borderThin, AppColors.outline, shape)
            .padding(horizontal = AppSpacing.inputPaddingHorizontal, vertical = AppSpacing.inputPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            singleLine = true,
            textStyle = AppTypography.bodyMedium.copy(
                color = AppColors.onSurface
            ),
            cursorBrush = SolidColor(AppColors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = AppColors.textPlaceholder,
                            style = AppTypography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Command history navigation buttons

        if (onHistoryUp != null || onHistoryDown != null) {
            Column(
                modifier = Modifier.padding(horizontal = AppSpacing.xs)
            ) {
                if (onHistoryUp != null) {
                    MoccaIconButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        onClick = onHistoryUp,
                        enabled = enabled,
                        iconColor = AppColors.onSurfaceVariant,
                        size = AppSpacing.iconButtonSizeCompact,
                        contentDescription = "Previous command"
                    )
                }
                if (onHistoryDown != null) {
                    MoccaIconButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        onClick = onHistoryDown,
                        enabled = enabled,
                        iconColor = AppColors.onSurfaceVariant,
                        size = AppSpacing.iconButtonSizeCompact,
                        contentDescription = "Next command"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        
        // Send button (circular FAB style)
        MoccaFab(
            icon = Icons.AutoMirrored.Filled.Send,
            onClick = onSubmit,
            contentDescription = "Send command",
            size = AppSpacing.fabSize,
            backgroundColor = if (value.isNotBlank()) AppColors.primary else AppColors.onSurfaceVariantDark,
            iconColor = if (value.isNotBlank()) AppColors.background else AppColors.outline
        )
    }
}

