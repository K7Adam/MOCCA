package com.mocca.app.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.window.PopupProperties
import com.mocca.app.domain.model.Command
import com.mocca.app.domain.model.Mode
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * The text input field area for ChatInputContent, including the BasicTextField
 * and text-triggered suggestion dropdowns (for "/" commands and "@" agent mentions).
 */
@Composable
internal fun ChatInputTextFieldArea(
    inputText: String,
    onValueChange: (String) -> Unit,
    inputEnabled: Boolean,
    placeholder: String,
    onInputFocusChanged: (Boolean) -> Unit,
    // Text-triggered suggestions
    showTextSuggestions: Boolean,
    onDismissSuggestions: () -> Unit,
    isCommandSuggestion: Boolean,
    suggestionQuery: String,
    filteredCommands: List<Command>,
    filteredModes: List<Mode>,
    selectedModeId: String?,
    onCommandSelected: (Command) -> Unit,
    onModeSelectedForMention: (Mode) -> Unit,
    onInputTextChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NavConstants.InputFieldMinHeight, max = NavConstants.InputFieldMaxHeight)
            .padding(horizontal = AppSpacing.inputPaddingHorizontal, vertical = AppSpacing.sm)
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        onInputFocusChanged(interactionSource.collectIsFocusedAsState().value)

        BasicTextField(
            value = inputText,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = inputEnabled,
            textStyle = AppTypography.bodyMedium.copy(color = AppColors.white),
            cursorBrush = SolidColor(AppColors.accent),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box {
                    if (inputText.isEmpty()) {
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

        // Text-triggered suggestions dropdown (typing "/" or "@" in input)
        // Uses DropdownMenu for stable positioning instead of SuggestionPopup
        // PopupProperties(focusable = false) keeps keyboard open while dropdown is shown
        DropdownMenu(
            expanded = showTextSuggestions,
            onDismissRequest = onDismissSuggestions,
            properties = PopupProperties(focusable = false),
            modifier = Modifier
                .background(AppColors.surfaceContainerHigh, AppShapes.medium)
                .border(AppSpacing.borderThin, AppColors.border.copy(alpha = 0.5f), AppShapes.medium)
        ) {
            if (isCommandSuggestion) {
                CommandSuggestionItems(
                    filteredCommands = filteredCommands,
                    suggestionQuery = suggestionQuery,
                    onCommandSelected = onCommandSelected,
                    onDismiss = onDismissSuggestions
                )
            } else {
                ModeSuggestionItems(
                    filteredModes = filteredModes,
                    suggestionQuery = suggestionQuery,
                    selectedModeId = selectedModeId,
                    inputText = inputText,
                    onModeSelectedForMention = onModeSelectedForMention,
                    onInputTextChange = onInputTextChange,
                    onDismiss = onDismissSuggestions
                )
            }
        }
    }
}

@Composable
private fun CommandSuggestionItems(
    filteredCommands: List<Command>,
    suggestionQuery: String,
    onCommandSelected: (Command) -> Unit,
    onDismiss: () -> Unit
) {
    if (filteredCommands.isEmpty()) {
        DropdownMenuItem(
            text = {
                Text(
                    "No commands match \"$suggestionQuery\"",
                    style = AppTypography.labelSmall,
                    color = AppColors.textTertiary
                )
            },
            onClick = onDismiss
        )
    } else {
        filteredCommands.forEach { cmd ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            "/${cmd.name}",
                            style = AppTypography.labelSmall,
                            color = AppColors.accent
                        )
                        cmd.description?.let { desc ->
                            Text(
                                desc,
                                style = AppTypography.labelSmall,
                                color = AppColors.textTertiary,
                                maxLines = 1
                            )
                        }
                    }
                },
                onClick = {
                    onCommandSelected(cmd)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun ModeSuggestionItems(
    filteredModes: List<Mode>,
    suggestionQuery: String,
    selectedModeId: String?,
    inputText: String,
    onModeSelectedForMention: (Mode) -> Unit,
    onInputTextChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (filteredModes.isEmpty()) {
        DropdownMenuItem(
            text = {
                Text(
                    "No agents match \"$suggestionQuery\"",
                    style = AppTypography.labelSmall,
                    color = AppColors.textTertiary
                )
            },
            onClick = onDismiss
        )
    } else {
        filteredModes.forEach { mode ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            mode.name.uppercase(),
                            style = AppTypography.labelSmall,
                            color = if (mode.id == selectedModeId) AppColors.accent else AppColors.textSecondary
                        )
                        mode.description?.let { desc ->
                            Text(
                                desc,
                                style = AppTypography.labelSmall,
                                color = AppColors.textTertiary,
                                maxLines = 1
                            )
                        }
                    }
                },
                onClick = {
                    onModeSelectedForMention(mode)
                    // Remove the @ and any partial query from input
                    val lastAt = inputText.lastIndexOf('@')
                    if (lastAt != -1) {
                        onInputTextChange(inputText.substring(0, lastAt))
                    }
                    onDismiss()
                }
            )
        }
    }
}
