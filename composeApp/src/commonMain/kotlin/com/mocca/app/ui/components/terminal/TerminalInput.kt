package com.mocca.app.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
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
 * Main input field for chat screen with model selection, mode toggle, and attachments.
 */
@Composable
fun RichChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    modelName: String = "CLAUDE",
    agentName: String = "BUILD",
    placeholder: String = "Input instruction...",
    enabled: Boolean = true,
    // Model selection
    providerResponse: com.mocca.app.domain.model.ProviderResponse? = null,
    selectedProviderId: String = "",
    selectedModelId: String = "",
    onModelSelected: (providerId: String, modelId: String) -> Unit = { _, _ -> },
    recentModels: List<com.mocca.app.domain.model.RecentModel> = emptyList(),
    // Mode selection
    modes: List<com.mocca.app.domain.model.Mode> = emptyList(),
    selectedModeId: String? = null,
    onModeSelected: (String?) -> Unit = {},
    // Attachments
    attachedFiles: List<com.mocca.app.domain.model.AttachedFile> = emptyList(),
    onRemoveAttachment: (com.mocca.app.domain.model.AttachedFile) -> Unit = {},
    onAttachClick: () -> Unit = {},

    // Command/Mention callbacks
    commands: List<com.mocca.app.util.TerminalCommand> = emptyList(),
    onCommandSelected: (com.mocca.app.util.TerminalCommand) -> Unit = {},
    onModeSelectedForMention: (com.mocca.app.domain.model.Mode) -> Unit = {}
) {
    var showModelSelector by remember { mutableStateOf(false) }
    
    // Suggestion state
    var showSuggestions by remember { mutableStateOf(false) }
    var suggestionQuery by remember { mutableStateOf("") }
    var suggestionType by remember { mutableStateOf<SuggestionType?>(null) }
    
    // Calculate suggestions based on input
    val currentSuggestions = remember(value, showSuggestions, suggestionType, suggestionQuery, modes, commands) {
        if (!showSuggestions || suggestionType == null) emptyList<SuggestionItem>()
        else {
            val query = suggestionQuery.lowercase()
            when (suggestionType) {
                SuggestionType.COMMAND -> {
                    commands.filter { 
                        it.trigger.lowercase().contains(query) 
                    }.map { 
                        SuggestionItem(it.trigger, it.trigger, it.description, SuggestionType.COMMAND) 
                    }
                }
                SuggestionType.MODE -> {
                    modes.filter { 
                        it.name.lowercase().contains(query) || it.id.lowercase().contains(query)
                    }.map { 
                        SuggestionItem(it.id, it.name.uppercase(), it.description, SuggestionType.MODE) 
                    }
                }
                else -> emptyList<SuggestionItem>()
            }
        }
    }

    // Handle input changes to detect triggers
    val handleValueChange = { newValue: String ->
        onValueChange(newValue)
        
        // Simple regex to find trigger at end of string or single word
        // Slash command: starts with /
        if (newValue.startsWith("/")) {
            suggestionType = SuggestionType.COMMAND
            suggestionQuery = newValue.drop(1)
            showSuggestions = true
        } 
        // Mention: @ at word boundary
        else {
            val lastAt = newValue.lastIndexOf('@')
            if (lastAt != -1 && (lastAt == 0 || newValue[lastAt - 1].isWhitespace())) {
                suggestionType = SuggestionType.MODE
                suggestionQuery = newValue.substring(lastAt + 1)
                showSuggestions = true
            } else {
                showSuggestions = false
            }
        }
        
        if (newValue.isEmpty()) {
            showSuggestions = false
        }
    }
    
    // Handle suggestion selection
    val onSuggestionSelected = { item: SuggestionItem ->
        when (item.type) {
            SuggestionType.COMMAND -> {
                // Command selected: replace input with command trigger (or execute immediately?)
                // For now, let's just complete the command text
                onValueChange("/${item.id}")
                // Optionally execute immediately? usually user presses enter
                showSuggestions = false
            }
            SuggestionType.MODE -> {
                // Mode selected: set the mode and remove the @mention text
                val mode = modes.find { it.id == item.id }
                if (mode != null) {
                    onModeSelectedForMention(mode)
                    // Remove the @query part from input
                    val lastAt = value.lastIndexOf('@')
                    if (lastAt != -1) {
                         val prefix = value.substring(0, lastAt)
                         onValueChange(prefix)
                    }
                }
                showSuggestions = false
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalColors.background, RectangleShape)
            .border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape)
    ) {
        // Status bar (MODEL + MODE) - clickable to open selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = TerminalSpacing.inputPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Model selector (clickable)
            Row(
                modifier = Modifier
                    .clickable(enabled = providerResponse != null) { showModelSelector = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MODEL: $modelName".uppercase(),
                    color = if (providerResponse != null) TerminalColors.greyLight else TerminalColors.grey,
                    style = TerminalTypography.labelSmall
                )
                if (providerResponse != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "▼",
                        color = TerminalColors.grey,
                        style = TerminalTypography.labelSmall
                    )
                }
            }
            
            Text(
                text = "MODE: $agentName".uppercase(),
                color = TerminalColors.grey,
                style = TerminalTypography.labelSmall
            )
        }
        
        // Divider
        HorizontalDivider(
            thickness = TerminalSpacing.borderThin,
            color = TerminalColors.border
        )
        
        // Attached files display (if any)
        if (attachedFiles.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TerminalSpacing.inputPadding, vertical = TerminalSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
            ) {
                attachedFiles.forEach { file ->
                    AttachmentChip(
                        file = file,
                        onRemove = { onRemoveAttachment(file) }
                    )
                }
            }
            
            HorizontalDivider(
                thickness = TerminalSpacing.borderThin,
                color = TerminalColors.border
            )
        }
        
        // Input area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = 240.dp)
                .padding(TerminalSpacing.inputPadding)
        ) {
            BasicTextField(
                value = value,
                onValueChange = handleValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && 
                            event.key == Key.Enter && 
                            event.isCtrlPressed) {
                            onSendClick()
                            true
                        } else {
                            false
                        }
                    },
                enabled = enabled,
                textStyle = TerminalTypography.bodyMedium.copy(
                    color = TerminalColors.white
                ),
                cursorBrush = SolidColor(TerminalColors.white),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                keyboardActions = KeyboardActions.Default,
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

            
            // Suggestions popup anchored to input
            if (showSuggestions && currentSuggestions.isNotEmpty()) {
                SuggestionPopup(
                    suggestions = currentSuggestions,
                    onSuggestionSelected = onSuggestionSelected,
                    onDismiss = { showSuggestions = false }
                )
            }
        }
        
        // Divider
        HorizontalDivider(
            thickness = TerminalSpacing.borderThin,
            color = TerminalColors.border
        )
        
        // Action toolbar with mode buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TerminalSpacing.actionToolbarHeight)
                .padding(horizontal = TerminalSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // @ mention button - inserts @
            TerminalIconButton(
                icon = Icons.Default.Add,
                onClick = { handleValueChange(if (value.isEmpty()) "@" else "$value @") },
                size = 36.dp,
                iconColor = TerminalColors.greyLight
            )
            
            // / command button - inserts /
            TerminalTextButton(
                text = "/",
                onClick = { handleValueChange("/") },
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
            
            // Mode selector buttons
            if (modes.isNotEmpty()) {
                modes.take(3).forEach { mode ->
                    val isSelected = mode.id == selectedModeId
                    TerminalTextButton(
                        text = if (isSelected) "* ${mode.name.uppercase()}" else mode.name.uppercase(),
                        onClick = { onModeSelected(if (isSelected) null else mode.id) },
                        textColor = if (isSelected) TerminalColors.white else TerminalColors.greyLight
                    )
                    Spacer(modifier = Modifier.width(TerminalSpacing.xs))
                }
            } else {
                // Fallback static mode button
                TerminalTextButton(
                    text = "* ${agentName.uppercase()}",
                    onClick = { },
                    textColor = TerminalColors.white
                )
            }
            
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
    
    // Model selector dialog
    if (showModelSelector && providerResponse != null) {
        ModelSelectorDialog(
            providerResponse = providerResponse,
            selectedProviderId = selectedProviderId,
            selectedModelId = selectedModelId,
            onModelSelected = onModelSelected,
            recentModels = recentModels,
            onDismiss = { showModelSelector = false }
        )
    }
}

/**
 * Chip showing an attached file with remove button.
 */
@Composable
private fun AttachmentChip(
    file: com.mocca.app.domain.model.AttachedFile,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(TerminalColors.surface, RectangleShape)
            .border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape)
            .padding(horizontal = TerminalSpacing.sm, vertical = TerminalSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.AttachFile,
            contentDescription = null,
            tint = TerminalColors.grey,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = file.name.take(20),
            style = TerminalTypography.labelSmall,
            color = TerminalColors.greyLight
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "(${file.displaySize})",
            style = TerminalTypography.labelSmall,
            color = TerminalColors.grey
        )
        Spacer(modifier = Modifier.width(TerminalSpacing.xs))
        Icon(
            Icons.Default.Close,
            contentDescription = "Remove",
            tint = TerminalColors.grey,
            modifier = Modifier
                .size(14.dp)
                .clickable { onRemove() }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMMAND LINE INPUT (simple > prompt)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Simple command line input for terminal screen.
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
    // ═══════════════════════════════════════════════════════════════════════════════
    // COMMAND HISTORY (Priority 5.3) - History navigation callbacks
    // ═══════════════════════════════════════════════════════════════════════════════
    onHistoryUp: (() -> Unit)? = null,
    onHistoryDown: (() -> Unit)? = null
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
        
        // ═══════════════════════════════════════════════════════════════════════════════
        // COMMAND HISTORY (Priority 5.3) - History navigation buttons
        // ═══════════════════════════════════════════════════════════════════════════════
        if (onHistoryUp != null || onHistoryDown != null) {
            Column(
                modifier = Modifier.padding(horizontal = TerminalSpacing.xs)
            ) {
                if (onHistoryUp != null) {
                    TerminalIconButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        onClick = onHistoryUp,
                        enabled = enabled,
                        iconColor = TerminalColors.grey,
                        size = 24.dp,
                        contentDescription = "Previous command"
                    )
                }
                if (onHistoryDown != null) {
                    TerminalIconButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        onClick = onHistoryDown,
                        enabled = enabled,
                        iconColor = TerminalColors.grey,
                        size = 24.dp,
                        contentDescription = "Next command"
                    )
                }
            }
        }
        
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
