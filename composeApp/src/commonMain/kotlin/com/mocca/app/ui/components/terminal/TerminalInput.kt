package com.mocca.app.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

/**
 * Terminal-styled input components.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// BASIC TERMINAL INPUT
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Basic terminal input field with white border.
 * Used for server address, auth token inputs on onboarding.
 */
@Composable
fun TerminalInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    showPrompt: Boolean = true,
    borderColor: Color = TerminalColors.borderLight,
    borderWidth: Dp = TerminalSpacing.borderStandard,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier) {
        // Label
        if (label != null) {
            Text(
                text = "// ${label.uppercase()}",
                color = TerminalColors.white,
                style = TerminalTypography.labelMedium
            )
            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
        }
        
        // Input field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TerminalSpacing.inputHeight)
                .background(TerminalColors.background, RectangleShape)
                .border(borderWidth, borderColor, RectangleShape)
                .padding(horizontal = TerminalSpacing.inputPadding)
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showPrompt) {
                    Text(
                        text = "> ",
                        color = TerminalColors.white,
                        style = TerminalTypography.bodyMedium
                    )
                }
                
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = TerminalTypography.bodyMedium.copy(
                        color = TerminalColors.white
                    ),
                    cursorBrush = SolidColor(TerminalColors.white),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder.uppercase(),
                                    color = TerminalColors.greyDark,
                                    style = TerminalTypography.bodyMedium
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
        
        // Hint
        if (hint != null) {
            Spacer(modifier = Modifier.height(TerminalSpacing.xs))
            Text(
                text = "* $hint",
                color = TerminalColors.grey,
                style = TerminalTypography.labelSmall
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// RICH CHAT INPUT (with @mentions, /commands, attachments)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Rich chat input field with status bar and action toolbar.
 * Main input field for chat screen as per mockups.
 */
@Composable
fun RichChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    modelName: String = "CLAUDE OPUS 4.5",
    agentName: String = "SISYPHUS",
    placeholder: String = "Input instruction...",
    enabled: Boolean = true,
    isPlanMode: Boolean = false,
    onPlanModeToggle: () -> Unit = {},
    onMentionClick: () -> Unit = {},
    onCommandClick: () -> Unit = {},
    onAttachClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalColors.background, RectangleShape)
            .border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape)
    ) {
        // Status bar (MODEL + AGENT)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = TerminalSpacing.inputPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "MODEL: $modelName".uppercase(),
                color = TerminalColors.grey,
                style = TerminalTypography.labelSmall
            )
            Text(
                text = "AGENT: $agentName".uppercase(),
                color = TerminalColors.grey,
                style = TerminalTypography.labelSmall
            )
        }
        
        // Divider
        HorizontalDivider(
            thickness = TerminalSpacing.borderThin,
            color = TerminalColors.border
        )
        
        // Input area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(TerminalSpacing.inputPadding)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                textStyle = TerminalTypography.bodyMedium.copy(
                    color = TerminalColors.white
                ),
                cursorBrush = SolidColor(TerminalColors.white),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                decorationBox = { innerTextField ->
                    Row {
                        Text(
                            text = "> ",
                            color = TerminalColors.white,
                            style = TerminalTypography.bodyMedium
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = TerminalColors.greyDark,
                                    style = TerminalTypography.bodyMedium
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
        }
        
        // Divider
        HorizontalDivider(
            thickness = TerminalSpacing.borderThin,
            color = TerminalColors.border
        )
        
        // Action toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TerminalSpacing.actionToolbarHeight)
                .padding(horizontal = TerminalSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // @ mention button
            TerminalIconButton(
                icon = Icons.Default.Add,
                onClick = onMentionClick,
                size = 36.dp,
                iconColor = TerminalColors.greyLight
            )
            
            // / command button
            TerminalTextButton(
                text = "/",
                onClick = onCommandClick,
                textColor = TerminalColors.greyLight
            )
            
            // Divider
            Spacer(modifier = Modifier.width(TerminalSpacing.xs))
            VerticalDivider(
                modifier = Modifier.height(16.dp),
                thickness = TerminalSpacing.borderThin,
                color = TerminalColors.border
            )
            Spacer(modifier = Modifier.width(TerminalSpacing.xs))
            
            // Plan mode toggle
            TerminalTextButton(
                text = if (isPlanMode) "* PLAN" else "PLAN",
                onClick = onPlanModeToggle,
                textColor = if (isPlanMode) TerminalColors.white else TerminalColors.greyLight
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Attachment button
            TerminalIconButton(
                icon = Icons.Default.AttachFile,
                onClick = onAttachClick,
                size = 36.dp,
                iconColor = TerminalColors.greyLight
            )
            
            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
            
            // Send button
            TerminalCompactButton(
                text = "SEND",
                onClick = onSendClick,
                enabled = enabled && value.isNotBlank(),
                icon = Icons.AutoMirrored.Filled.Send
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMMAND LINE INPUT (simple > prompt)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Simple command line input for terminal screen.
 */
@Composable
fun CommandLineInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Enter command..."
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalColors.surface, RectangleShape)
            .border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape)
            .padding(TerminalSpacing.inputPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "> ",
            color = TerminalColors.statusOnline,
            style = TerminalTypography.bodyMedium
        )
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            singleLine = true,
            textStyle = TerminalTypography.bodyMedium.copy(
                color = TerminalColors.white
            ),
            cursorBrush = SolidColor(TerminalColors.statusOnline),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = TerminalColors.greyDark,
                            style = TerminalTypography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        Spacer(modifier = Modifier.width(TerminalSpacing.sm))
        
        // Send button (white square with arrow)
        TerminalIconButton(
            icon = Icons.AutoMirrored.Filled.Send,
            onClick = onSubmit,
            enabled = enabled && value.isNotBlank(),
            backgroundColor = if (value.isNotBlank()) TerminalColors.white else TerminalColors.greyDark,
            iconColor = TerminalColors.background,
            size = 40.dp
        )
    }
}
