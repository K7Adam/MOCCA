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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mocca.app.domain.model.Mode
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.ui.components.modern.ModelSelectorDialog
import com.mocca.app.ui.components.modern.SuggestionItem
import com.mocca.app.ui.components.modern.SuggestionPopup
import com.mocca.app.ui.components.modern.SuggestionType
import com.mocca.app.ui.components.modern.VariantSelectorDialog
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.util.TerminalCommand

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
    commands: List<TerminalCommand>,
    onCommandSelected: (TerminalCommand) -> Unit,
    onModeSelectedForMention: (Mode) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    var showModelSelector by remember { mutableStateOf(false) }
    var showVariantSelector by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var suggestionQuery by remember { mutableStateOf("") }
    var suggestionType by remember { mutableStateOf<SuggestionType?>(null) }
    var isInputFocused by remember { mutableStateOf(false) }

    // Calculate suggestions
    val currentSuggestions = remember(inputText, showSuggestions, suggestionType, suggestionQuery, modes, commands) {
        if (!showSuggestions || suggestionType == null) emptyList<SuggestionItem>()
        else {
            val query = suggestionQuery.lowercase()
            when (suggestionType) {
                SuggestionType.COMMAND -> {
                    commands.filter { it.trigger.lowercase().contains(query) }.map {
                        SuggestionItem(it.trigger, it.trigger, it.description, SuggestionType.COMMAND)
                    }
                }
                SuggestionType.MODE -> {
                    modes.filter { it.name.lowercase().contains(query) || it.id.lowercase().contains(query) }.map {
                        SuggestionItem(it.id, it.name.uppercase(), it.description, SuggestionType.MODE)
                    }
                }
                else -> emptyList()
            }
        }
    }

    // Handle input changes for suggestions
    val handleValueChange = { newValue: String ->
        onInputTextChange(newValue)
        if (newValue.startsWith("/")) {
            suggestionType = SuggestionType.COMMAND
            suggestionQuery = newValue.drop(1)
            showSuggestions = true
        } else {
            val lastAt = newValue.lastIndexOf('@')
            if (lastAt != -1 && (lastAt == 0 || newValue[lastAt - 1].isWhitespace())) {
                suggestionType = SuggestionType.MODE
                suggestionQuery = newValue.substring(lastAt + 1)
                showSuggestions = true
            } else {
                showSuggestions = false
            }
        }
        if (newValue.isEmpty()) showSuggestions = false
    }

    val onSuggestionSelected = { item: SuggestionItem ->
        when (item.type) {
            SuggestionType.COMMAND -> {
                onInputTextChange("/${item.id}")
                showSuggestions = false
            }
            SuggestionType.MODE -> {
                val mode = modes.find { it.id == item.id }
                if (mode != null) {
                    onModeSelectedForMention(mode)
                    val lastAt = inputText.lastIndexOf('@')
                    if (lastAt != -1) {
                        onInputTextChange(inputText.substring(0, lastAt))
                    }
                }
                showSuggestions = false
            }
        }
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

            // Suggestions popup
            if (showSuggestions && currentSuggestions.isNotEmpty()) {
                SuggestionPopup(
                    suggestions = currentSuggestions,
                    onSuggestionSelected = onSuggestionSelected,
                    onDismiss = { showSuggestions = false }
                )
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
                // @ mention button - styled as subtle chip
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
                            onClick = { handleValueChange(if (inputText.isEmpty()) "@" else "$inputText @") }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "@",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // / command button - styled as subtle chip
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
                            onClick = { handleValueChange("/") }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "/",
                        color = AppColors.textSecondary,
                        style = AppTypography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
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
