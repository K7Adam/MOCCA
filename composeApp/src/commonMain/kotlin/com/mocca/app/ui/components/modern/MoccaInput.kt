package com.mocca.app.ui.components.modern

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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Modern MOCCA input components with pill-shaped design.
 * Based on UI overhaul designs - 32dp rounded corners, clean aesthetic.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// BASIC TERMINAL INPUT (Pill-shaped, 32dp radius)
// ═══════════════════════════════════════════════════════════════════════════════

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
    borderColor: Color = AppColors.border,
    borderWidth: Dp = AppSpacing.borderThin,
    shape: Shape = AppShapes.input,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier) {
        // Label (uppercase, subtle)
        if (label != null) {
            Text(
                text = label.uppercase(),
                color = AppColors.textSecondary,
                style = AppTypography.labelMedium
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
        }
        
        // Input field with rounded corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.inputHeight)
                .clip(shape)
                .background(backgroundColor, shape)
                .border(borderWidth, borderColor, shape)
                .padding(horizontal = AppSpacing.inputPaddingHorizontal)
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showPrompt) {
                    Text(
                        text = "> ",
                        color = AppColors.accentGreen,
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
                        color = AppColors.white
                    ),
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(AppColors.accentGreen),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
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
                color = AppColors.textTertiary,
                style = AppTypography.labelSmall
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// RICH CHAT INPUT (Modern card-based input with status bar)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Rich chat input field with status bar and action toolbar.
 * Main input field for chat screen with model selection, mode toggle, and attachments.
 * 
 * Modern design: Rounded card container, clean status bar, pill-shaped send button.
 */
@Composable
fun RichChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    modelName: String = "--",
    agentName: String = "--",
    placeholder: String = "Type a message...",
    enabled: Boolean = true,
    // Model selection
    providerResponse: com.mocca.app.domain.model.ProviderResponse? = null,
    selectedProviderId: String = "",
    selectedModelId: String = "",
    onModelSelected: (providerId: String, modelId: String) -> Unit = { _, _ -> },
    recentModels: List<com.mocca.app.domain.model.RecentModel> = emptyList(),
    // Variant selection
    variants: List<String> = emptyList(),
    selectedVariantId: String? = null,
    onVariantSelected: (String) -> Unit = {},
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
    var showVariantSelector by remember { mutableStateOf(false) }
    
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
            .glassy(AppShapes.card)
    ) {
        // Status bar (MODEL + MODE) - clickable to open selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = AppSpacing.inputPaddingHorizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Model selector (clickable)
            Row(
                modifier = Modifier
                    .clickable(enabled = providerResponse != null) { showModelSelector = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.textTertiary
                )
                Text(
                    text = modelName.uppercase(),
                    color = if (providerResponse != null) AppColors.textSecondary else AppColors.textTertiary,
                    style = AppTypography.labelSmall
                )
            }
            
            // Variant selector (clickable if variants available)
            if (variants.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clickable { showVariantSelector = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.textTertiary
                    )
                    Text(
                        text = (selectedVariantId ?: "DEFAULT").uppercase(),
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }
            }
            
            // Agent selector (clickable)
            Row(
                modifier = Modifier.clickable { /* Toggle Agent Logic */ },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.textTertiary
                )
                Text(
                    text = agentName.uppercase(),
                    color = AppColors.textTertiary,
                    style = AppTypography.labelSmall
                )
            }
        }
        
        // Divider
        HorizontalDivider(
            thickness = AppSpacing.borderThin,
            color = AppColors.border
        )
        
        // Attached files display (if any)
        if (attachedFiles.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.inputPaddingHorizontal, vertical = AppSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                attachedFiles.forEach { file ->
                    AttachmentChip(
                        file = file,
                        onRemove = { onRemoveAttachment(file) }
                    )
                }
            }
            
            HorizontalDivider(
                thickness = AppSpacing.borderThin,
                color = AppColors.border
            )
        }
        
        // Input area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = 240.dp)
                .padding(AppSpacing.inputPaddingHorizontal)
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
                textStyle = AppTypography.bodyMedium.copy(
                    color = AppColors.white
                ),
                cursorBrush = SolidColor(AppColors.accentGreen),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                keyboardActions = KeyboardActions.Default,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier) {
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
            thickness = AppSpacing.borderThin,
            color = AppColors.border
        )
        
        // Action toolbar with mode buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.actionToolbarHeight)
                .padding(horizontal = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // @ mention button - inserts @
            MoccaIconButton(
                icon = Icons.Default.Add,
                onClick = { handleValueChange(if (value.isEmpty()) "@" else "$value @") },
                size = 36.dp,
                iconColor = AppColors.textSecondary
            )
            
            // / command button - inserts /
            MoccaTextButton(
                text = "/",
                onClick = { handleValueChange("/") },
                textColor = AppColors.textSecondary
            )
            
            // Divider
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            VerticalDivider(
                modifier = Modifier.height(16.dp),
                thickness = AppSpacing.borderThin,
                color = AppColors.border
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            
            // Mode selector buttons (Dropdown-like trigger)
            Box {
                var showAgentMenu by remember { mutableStateOf(false) }
                
                MoccaCompactButton(
                    text = agentName.uppercase(),
                    onClick = { showAgentMenu = true },
                    icon = Icons.Default.Person,
                    backgroundColor = AppColors.surfaceVariant,
                    textColor = AppColors.textSecondary
                )

                DropdownMenu(
                    expanded = showAgentMenu,
                    onDismissRequest = { showAgentMenu = false },
                    modifier = Modifier.background(AppColors.surfaceElevated)
                ) {
                    modes.forEach { mode ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    mode.name.uppercase(), 
                                    style = AppTypography.labelSmall,
                                    color = if (mode.id == selectedModeId) AppColors.accentGreen else AppColors.textSecondary
                                ) 
                            },
                            onClick = {
                                onModeSelected(mode.id)
                                showAgentMenu = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Attachment button
            MoccaIconButton(
                icon = Icons.Default.AttachFile,
                onClick = onAttachClick,
                size = 36.dp,
                iconColor = AppColors.textSecondary
            )
            
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            
            // Send button (pill-shaped)
            MoccaCompactButton(
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
    
    // Variant selector dialog
    if (showVariantSelector) {
        VariantSelectorDialog(
            variants = variants,
            selectedVariantId = selectedVariantId,
            onVariantSelected = onVariantSelected,
            onDismiss = { showVariantSelector = false }
        )
    }
}

/**
 * Chip showing an attached file with remove button.
 * Modern pill-shaped design.
 */
@Composable
private fun AttachmentChip(
    file: com.mocca.app.domain.model.AttachedFile,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(AppShapes.pill)
            .background(AppColors.surfaceVariant, AppShapes.pill)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.pill)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.AttachFile,
            contentDescription = null,
            tint = AppColors.textSecondary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = file.name.take(20),
            style = AppTypography.labelSmall,
            color = AppColors.textSecondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "(${file.displaySize})",
            style = AppTypography.labelSmall,
            color = AppColors.textTertiary
        )
        Spacer(modifier = Modifier.width(AppSpacing.xs))
        Icon(
            Icons.Default.Close,
            contentDescription = "Remove",
            tint = AppColors.textTertiary,
            modifier = Modifier
                .size(14.dp)
                .clickable { onRemove() }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMMAND LINE INPUT (Modern pill-shaped variant)
// ═══════════════════════════════════════════════════════════════════════════════

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
    // ═══════════════════════════════════════════════════════════════════════════════
    // COMMAND HISTORY (Priority 5.3) - History navigation callbacks
    // ═══════════════════════════════════════════════════════════════════════════════
    onHistoryUp: (() -> Unit)? = null,
    onHistoryDown: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.surfaceContainer, shape)
            .border(AppSpacing.borderThin, AppColors.border, shape)
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
                color = AppColors.white
            ),
            cursorBrush = SolidColor(AppColors.accentGreen),
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
        
        // ═══════════════════════════════════════════════════════════════════════════════
        // COMMAND HISTORY (Priority 5.3) - History navigation buttons
        // ═══════════════════════════════════════════════════════════════════════════════
        if (onHistoryUp != null || onHistoryDown != null) {
            Column(
                modifier = Modifier.padding(horizontal = AppSpacing.xs)
            ) {
                if (onHistoryUp != null) {
                    MoccaIconButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        onClick = onHistoryUp,
                        enabled = enabled,
                        iconColor = AppColors.textSecondary,
                        size = 24.dp,
                        contentDescription = "Previous command"
                    )
                }
                if (onHistoryDown != null) {
                    MoccaIconButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        onClick = onHistoryDown,
                        enabled = enabled,
                        iconColor = AppColors.textSecondary,
                        size = 24.dp,
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
            size = 48.dp,
            backgroundColor = if (value.isNotBlank()) AppColors.buttonBackground else AppColors.greyDark,
            iconColor = if (value.isNotBlank()) AppColors.buttonText else AppColors.textTertiary
        )
    }
}
