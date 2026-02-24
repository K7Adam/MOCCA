package com.mocca.app.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.AttachedFile
import com.mocca.app.domain.model.Command
import com.mocca.app.domain.model.Mode
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.domain.model.mergeCommands
import com.mocca.app.ui.components.modern.ModelSelectorDialog
import com.mocca.app.ui.components.modern.VariantSelectorDialog
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Chat input content component - the content that appears ABOVE the persistent nav row.
 * 
 * This component contains:
 * - Status bar with model/agent/variant selectors
 * - Text input field
 * - Action toolbar with attachment and send buttons
 * 
 * The navigation row is handled separately by PersistentNavRow to ensure
 * consistent sizing and positioning across all modes.
 *
 * @param inputText Current input text
 * @param onInputTextChange Callback when input text changes
 * @param onSendClick Callback when send button is clicked
 * @param inputEnabled Whether input is enabled
 * @param placeholder Placeholder text for input field
 * @param modelName Current model name
 * @param agentName Current agent name
 * @param providerResponse Available providers and models
 * @param selectedProviderId Currently selected provider ID
 * @param selectedModelId Currently selected model ID
 * @param onModelSelected Callback when model is selected
 * @param variants Available variants
 * @param selectedVariantId Currently selected variant ID
 * @param onVariantSelected Callback when variant is selected
 * @param modes Available modes/agents
 * @param selectedModeId Currently selected mode ID
 * @param onModeSelected Callback when mode is selected
 * @param attachedFiles Currently attached files
 * @param onRemoveAttachment Callback to remove an attachment
 * @param onAttachClick Callback to open file picker
 * @param commands Available terminal commands
 * @param onCommandSelected Callback when command is selected
 * @param onModeSelectedForMention Callback when mode is selected via @mention
 * @param modifier Modifier for styling
 * @param alpha Alpha value for animation
 */
@Composable
fun ChatInputContent(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    inputEnabled: Boolean,
    placeholder: String,
    // Agent state
    isSessionIdle: Boolean = true,
    onAbortClick: () -> Unit = {},
    // Model/Agent selection
    modelName: String,
    agentName: String,
    providerResponse: ProviderResponse?,
    selectedProviderId: String,
    selectedModelId: String,
    onModelSelected: (String, String) -> Unit,
    // Variants
    variants: List<String>,
    selectedVariantId: String?,
    onVariantSelected: (String) -> Unit,
    // Modes
    modes: List<Mode>,
    selectedModeId: String?,
    onModeSelected: (String?) -> Unit,
    // Attachments
    attachedFiles: List<AttachedFile>,
    onRemoveAttachment: (AttachedFile) -> Unit,
    onAttachClick: () -> Unit,
    // Commands
    commands: List<Command>,
    onCommandSelected: (Command) -> Unit,
    onModeSelectedForMention: (Mode) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    var showModelSelector by remember { mutableStateOf(false) }
    var showVariantSelector by remember { mutableStateOf(false) }
    var isInputFocused by remember { mutableStateOf(false) }
    
    // Text-triggered suggestions state
    var showTextSuggestions by remember { mutableStateOf(false) }
    var suggestionQuery by remember { mutableStateOf("") }
    var isCommandSuggestion by remember { mutableStateOf(true) } // true = commands, false = modes
    
    // Manual palette triggers (for / and @ buttons)
    var showCommandPalette by remember { mutableStateOf(false) }
    var showAgentPalette by remember { mutableStateOf(false) }

    // Merge API commands with built-in commands
    val mergedCommands = remember(commands) { mergeCommands(commands) }
    
    // Calculate filtered suggestions based on current query
    val filteredCommands = remember(mergedCommands, suggestionQuery) {
        val query = suggestionQuery.lowercase()
        if (query.isEmpty()) mergedCommands
        else mergedCommands.filter { it.name.lowercase().contains(query) }
    }
    
    val filteredModes = remember(modes, suggestionQuery) {
        val query = suggestionQuery.lowercase()
        if (query.isEmpty()) modes
        else modes.filter { it.name.lowercase().contains(query) || it.id.lowercase().contains(query) }
    }

    // Handle input changes for suggestions
    val handleValueChange = { newValue: String ->
        onInputTextChange(newValue)
        if (newValue.startsWith("/")) {
            isCommandSuggestion = true
            suggestionQuery = newValue.drop(1)
            showTextSuggestions = true
        } else {
            val lastAt = newValue.lastIndexOf('@')
            if (lastAt != -1 && (lastAt == 0 || newValue[lastAt - 1].isWhitespace())) {
                isCommandSuggestion = false
                suggestionQuery = newValue.substring(lastAt + 1)
                showTextSuggestions = true
            } else {
                showTextSuggestions = false
            }
        }
        if (newValue.isEmpty()) showTextSuggestions = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ═══════════════ STATUS BAR (Premium Pill Chips) ═══════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NavConstants.StatusBarHeight)
                .padding(horizontal = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            // Model selector - styled as pill chip
            Box(
                modifier = Modifier
                    .height(NavConstants.StatusBarChipHeight)
                    .background(
                        color = AppColors.surface.copy(alpha = 0.5f),
                        shape = AppShapes.pill
                    )
                    .border(
                        width = 0.5.dp,
                        color = AppColors.border.copy(alpha = 0.3f),
                        shape = AppShapes.pill
                    )
                    .clickable(enabled = providerResponse != null) { showModelSelector = true }
                    .padding(horizontal = AppSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(NavConstants.StatusBarIconSize),
                        tint = AppColors.textSecondary
                    )
                    Text(
                        text = modelName.take(10).uppercase(),
                        color = if (providerResponse != null) AppColors.textSecondary else AppColors.textTertiary,
                        style = AppTypography.labelSmall,
                        maxLines = 1
                    )
                }
            }

            // Variant selector (if available) - pill chip
            if (variants.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .height(NavConstants.StatusBarChipHeight)
                        .background(
                            color = AppColors.surface.copy(alpha = 0.5f),
                            shape = AppShapes.pill
                        )
                        .border(
                            width = 0.5.dp,
                            color = AppColors.border.copy(alpha = 0.3f),
                            shape = AppShapes.pill
                        )
                        .clickable { showVariantSelector = true }
                        .padding(horizontal = AppSpacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(NavConstants.StatusBarIconSize),
                            tint = AppColors.textTertiary
                        )
                        Text(
                            text = (selectedVariantId ?: "DEF").take(5).uppercase(),
                            color = AppColors.textSecondary,
                            style = AppTypography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }

            // Spacer to push agent selector to the right
            Spacer(modifier = Modifier.weight(1f))

            // Agent selector - pill chip with dropdown
            Box {
                var showAgentMenu by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .height(NavConstants.StatusBarChipHeight)
                        .background(
                            color = AppColors.surface.copy(alpha = 0.5f),
                            shape = AppShapes.pill
                        )
                        .border(
                            width = 0.5.dp,
                            color = AppColors.border.copy(alpha = 0.3f),
                            shape = AppShapes.pill
                        )
                        .clickable { showAgentMenu = true }
                        .padding(horizontal = AppSpacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(NavConstants.StatusBarIconSize),
                            tint = AppColors.textTertiary
                        )
                        Text(
                            text = agentName.take(8).uppercase(),
                            color = AppColors.textTertiary,
                            style = AppTypography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
                
                // Themed dropdown menu
                DropdownMenu(
                    expanded = showAgentMenu,
                    onDismissRequest = { showAgentMenu = false },
                    properties = PopupProperties(focusable = false),
                    modifier = Modifier
                        .background(AppColors.surfaceElevated, AppShapes.medium)
                        .border(AppSpacing.borderThin, AppColors.border.copy(alpha = 0.5f), AppShapes.medium)
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
        }

        // ═══════════════ INPUT AREA ═══════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = NavConstants.InputFieldMinHeight, max = NavConstants.InputFieldMaxHeight)
                .padding(horizontal = AppSpacing.inputPaddingHorizontal, vertical = AppSpacing.sm)
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            isInputFocused = interactionSource.collectIsFocusedAsState().value

            BasicTextField(
                value = inputText,
                onValueChange = handleValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = inputEnabled,
                textStyle = AppTypography.bodyMedium.copy(color = AppColors.white),
                cursorBrush = SolidColor(AppColors.accentGreen),
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
                onDismissRequest = { showTextSuggestions = false },
                properties = PopupProperties(focusable = false),
                modifier = Modifier
                    .background(AppColors.surfaceElevated, AppShapes.medium)
                    .border(AppSpacing.borderThin, AppColors.border.copy(alpha = 0.5f), AppShapes.medium)
            ) {
                if (isCommandSuggestion) {
                    // Command suggestions (triggered by "/")
                    if (filteredCommands.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "No commands match \"$suggestionQuery\"",
                                    style = AppTypography.labelSmall,
                                    color = AppColors.textTertiary
                                )
                            },
                            onClick = { showTextSuggestions = false }
                        )
                    } else {
                        filteredCommands.forEach { cmd ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            "/${cmd.name}",
                                            style = AppTypography.labelSmall,
                                            color = AppColors.accentGreen
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
                                    showTextSuggestions = false
                                }
                            )
                        }
                    }
                } else {
                    // Mode/Agent suggestions (triggered by "@")
                    if (filteredModes.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "No agents match \"$suggestionQuery\"",
                                    style = AppTypography.labelSmall,
                                    color = AppColors.textTertiary
                                )
                            },
                            onClick = { showTextSuggestions = false }
                        )
                    } else {
                        filteredModes.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            mode.name.uppercase(),
                                            style = AppTypography.labelSmall,
                                            color = if (mode.id == selectedModeId) AppColors.accentGreen else AppColors.textSecondary
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
                                    showTextSuggestions = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // ═══════════════ ACTION TOOLBAR (Cleaner, Grouped) ═══════════════
        HorizontalDivider(
            thickness = AppSpacing.borderThin,
            color = AppColors.border.copy(alpha = 0.3f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NavConstants.ActionToolbarHeight)
                .padding(horizontal = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT GROUP: Quick actions (@ and /)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
            ) {
                // @ mention button - opens agent palette via DropdownMenu
                Box {
                    // Anchor for dropdown
                    Box(
                        modifier = Modifier
                            .size(NavConstants.ActionButtonSize)
                            .background(
                                color = if (showAgentPalette) AppColors.accentGreen.copy(alpha = 0.2f) else AppColors.surface.copy(alpha = 0.3f),
                                shape = AppShapes.pill
                            )
                            .then(
                                if (showAgentPalette) Modifier.border(AppSpacing.borderThin, AppColors.accentGreen, AppShapes.pill) else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showAgentPalette = !showAgentPalette }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "@",
                            color = if (showAgentPalette) AppColors.accentGreen else AppColors.textSecondary,
                            style = AppTypography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Agent dropdown menu (stable positioning)
                    DropdownMenu(
                        expanded = showAgentPalette,
                        onDismissRequest = { showAgentPalette = false },
                        properties = PopupProperties(focusable = false),
                        modifier = Modifier
                            .background(AppColors.surfaceElevated, AppShapes.medium)
                            .border(AppSpacing.borderThin, AppColors.border.copy(alpha = 0.5f), AppShapes.medium)
                    ) {
                        if (modes.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "No agents available",
                                        style = AppTypography.labelSmall,
                                        color = AppColors.textTertiary
                                    )
                                },
                                onClick = { showAgentPalette = false }
                            )
                        } else {
                            modes.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                mode.name.uppercase(),
                                                style = AppTypography.labelSmall,
                                                color = if (mode.id == selectedModeId) AppColors.accentGreen else AppColors.textSecondary
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
                                        showAgentPalette = false
                                    }
                                )
                            }
                        }
                    }
                }

                // / command button - opens command palette via DropdownMenu
                Box {
                    // Anchor for dropdown
                    Box(
                        modifier = Modifier
                            .size(NavConstants.ActionButtonSize)
                            .background(
                                color = if (showCommandPalette) AppColors.accentGreen.copy(alpha = 0.2f) else AppColors.surface.copy(alpha = 0.3f),
                                shape = AppShapes.pill
                            )
                            .then(
                                if (showCommandPalette) Modifier.border(AppSpacing.borderThin, AppColors.accentGreen, AppShapes.pill) else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showCommandPalette = !showCommandPalette }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "/",
                            color = if (showCommandPalette) AppColors.accentGreen else AppColors.textSecondary,
                            style = AppTypography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Command dropdown menu (stable positioning)
                    DropdownMenu(
                        expanded = showCommandPalette,
                        onDismissRequest = { showCommandPalette = false },
                        properties = PopupProperties(focusable = false),
                        modifier = Modifier
                            .background(AppColors.surfaceElevated, AppShapes.medium)
                            .border(AppSpacing.borderThin, AppColors.border.copy(alpha = 0.5f), AppShapes.medium)
                    ) {
                        if (mergedCommands.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "No commands available",
                                        style = AppTypography.labelSmall,
                                        color = AppColors.textTertiary
                                    )
                                },
                                onClick = { showCommandPalette = false }
                            )
                        } else {
                            mergedCommands.forEach { cmd ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                "/${cmd.name}",
                                                style = AppTypography.labelSmall,
                                                color = AppColors.accentGreen
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
                                        showCommandPalette = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Subtle separator
            VerticalDivider(
                modifier = Modifier
                    .height(16.dp)
                    .padding(horizontal = AppSpacing.xs),
                thickness = AppSpacing.borderThin,
                color = AppColors.border.copy(alpha = 0.5f)
            )

            // CENTER: Attachment button
            Box(
                modifier = Modifier
                    .size(NavConstants.ActionButtonSize)
                    .background(
                        color = AppColors.surface.copy(alpha = 0.3f),
                        shape = AppShapes.pill
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAttachClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach file",
                    tint = AppColors.textSecondary,
                    modifier = Modifier.size(NavConstants.ActionIconSize)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // RIGHT: Send/Abort button (transforms based on agent state)
            if (isSessionIdle) {
                // SEND button - normal state
                val canSend = inputEnabled && inputText.isNotBlank()
                Box(
                    modifier = Modifier
                        .height(NavConstants.SendButtonHeight)
                        .then(
                            if (canSend) {
                                Modifier.background(AppColors.accentGreen, AppShapes.pill)
                            } else {
                                Modifier.background(AppColors.surface.copy(alpha = 0.5f), AppShapes.pill)
                            }
                        )
                        .clickable(
                            enabled = canSend,
                            onClick = onSendClick
                        )
                        .padding(horizontal = AppSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (canSend) AppColors.background else AppColors.grey,
                            modifier = Modifier.size(NavConstants.SendIconSize)
                        )
                        Text(
                            text = "SEND",
                            color = if (canSend) AppColors.background else AppColors.grey,
                            style = AppTypography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // ABORT button - agent running state
                // Pulsing animation to indicate active agent
                val infiniteTransition = rememberInfiniteTransition(label = "abortPulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.7f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                
                Box(
                    modifier = Modifier
                        .height(NavConstants.SendButtonHeight)
                        .background(AppColors.error.copy(alpha = pulseAlpha), AppShapes.pill)
                        .clickable(onClick = onAbortClick)
                        .padding(horizontal = AppSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = AppColors.white,
                            modifier = Modifier.size(NavConstants.SendIconSize)
                        )
                        Text(
                            text = "ABORT",
                            color = AppColors.white,
                            style = AppTypography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Subtle divider before nav row
        HorizontalDivider(
            thickness = AppSpacing.borderThin,
            color = AppColors.border.copy(alpha = 0.3f)
        )
    }

    // Model selector dialog
    if (showModelSelector && providerResponse != null) {
        ModelSelectorDialog(
            providerResponse = providerResponse,
            selectedProviderId = selectedProviderId,
            selectedModelId = selectedModelId,
            onModelSelected = onModelSelected,
            recentModels = emptyList(),
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
