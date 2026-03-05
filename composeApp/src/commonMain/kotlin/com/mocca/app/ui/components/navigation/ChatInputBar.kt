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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.AttachedFile
import com.mocca.app.domain.model.Command
import com.mocca.app.domain.model.Mode
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.ui.components.modern.MoccaCompactButton
import com.mocca.app.ui.components.modern.MoccaIconButton
import com.mocca.app.ui.components.modern.ModelSelectorDialog
import com.mocca.app.ui.components.modern.SuggestionItem
import com.mocca.app.ui.components.modern.SuggestionPopup
import com.mocca.app.ui.components.modern.SuggestionType
import com.mocca.app.ui.components.modern.VariantSelectorDialog
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Chat input bar with integrated status bar, input field, action toolbar, and nav indicator.
 * Compact status bar, multi-line input, action toolbar, and nav indicator.
 *
 * @param inputText Current input text
 * @param onInputTextChange Callback when input text changes
 * @param onSendClick Callback when send button is clicked
 * @param inputEnabled Whether input is enabled
 * @param placeholder Placeholder text for input field
 * @param dragProgress Real-time drag progress for nav indicator
 * @param onItemClick Callback for nav item clicks
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
 * @param alpha Alpha value for crossfade animation
 */
@Composable
fun ChatInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    inputEnabled: Boolean,
    placeholder: String,
    dragProgress: Float,
    onItemClick: (PanelState) -> Unit,
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
    var showSuggestions by remember { mutableStateOf(false) }
    var suggestionQuery by remember { mutableStateOf("") }
    var suggestionType by remember { mutableStateOf<SuggestionType?>(null) }
    var isInputFocused by remember { mutableStateOf(false) }
    // Manual palette trigger (for / button)
    var showCommandPalette by remember { mutableStateOf(false) }

    val items = defaultBottomNavItems
    var travelDistancePx by remember { mutableFloatStateOf(0f) }
    var firstItemCenterPx by remember { mutableFloatStateOf(0f) }
    var lastItemCenterPx by remember { mutableFloatStateOf(0f) }

    // Calculate suggestions
    val currentSuggestions = remember(inputText, showSuggestions, suggestionType, suggestionQuery, modes, commands) {
        if (!showSuggestions || suggestionType == null) emptyList<SuggestionItem>()
        else {
            val query = suggestionQuery.lowercase()
            when (suggestionType) {
                SuggestionType.COMMAND -> {
                    commands.filter { it.name.lowercase().contains(query) }.map {
                        SuggestionItem(it.name, "/${it.name}", it.description, SuggestionType.COMMAND)
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
            .alpha(alpha)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ═══════════════ STATUS BAR ═══════════════
        ChatInputBarStatusRow(
            modelName = modelName,
            agentName = agentName,
            providerResponse = providerResponse,
            variants = variants,
            selectedVariantId = selectedVariantId,
            modes = modes,
            selectedModeId = selectedModeId,
            onModeSelected = onModeSelected,
            onModelSelectorClick = { showModelSelector = true },
            onVariantSelectorClick = { showVariantSelector = true },
            statusBarHeight = StatusBarHeight
        )

        HorizontalDivider(
            thickness = AppSpacing.borderThin,
            color = AppColors.border.copy(alpha = 0.5f)
        )

        // ═══════════════ INPUT AREA ═══════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = InputMinHeight, max = InputMaxHeight)
                .padding(horizontal = AppSpacing.inputPaddingHorizontal, vertical = AppSpacing.sm)
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            isInputFocused = interactionSource.collectIsFocusedAsState().value

            BasicTextField(
                value = inputText,
                onValueChange = handleValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = inputEnabled,
                textStyle = AppTypography.bodyMedium.copy(color = AppColors.textPrimary),
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

            // Suggestions popup (text-triggered)
            if (showSuggestions && currentSuggestions.isNotEmpty()) {
                SuggestionPopup(
                    suggestions = currentSuggestions,
                    onSuggestionSelected = onSuggestionSelected,
                    onDismiss = { showSuggestions = false }
                )
            }

            // Manual command palette (triggered by / button)
            if (showCommandPalette && commands.isNotEmpty()) {
                SuggestionPopup(
                    suggestions = commands.map { cmd ->
                        SuggestionItem(cmd.name, "/${cmd.name}", cmd.description, SuggestionType.COMMAND)
                    },
                    onSuggestionSelected = { item ->
                        val cmd = commands.find { it.name == item.id }
                        if (cmd != null) {
                            onCommandSelected(cmd)
                        } else {
                            // Fallback: insert into input
                            onInputTextChange("/${item.id} ")
                        }
                        showCommandPalette = false
                    },
                    onDismiss = { showCommandPalette = false }
                )
            }
        }

        // ═══════════════ ACTION TOOLBAR ═══════════════
        HorizontalDivider(
            thickness = AppSpacing.borderThin,
            color = AppColors.border.copy(alpha = 0.5f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ActionToolbarHeight)
                .padding(horizontal = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // / command button - opens command palette directly
            Box(
                modifier = Modifier
                    .size(ActionToolbarHeight)
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
                    )
                    .padding(horizontal = AppSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "/",
                    color = if (showCommandPalette) AppColors.accentGreen else AppColors.textSecondary,
                    style = AppTypography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(AppSpacing.xs))
            VerticalDivider(
                modifier = Modifier.height(14.dp),
                thickness = AppSpacing.borderThin,
                color = AppColors.border
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))

            // Attachment button
            MoccaIconButton(
                icon = Icons.Default.AttachFile,
                onClick = onAttachClick,
                size = 32.dp,
                iconColor = AppColors.textSecondary
            )

            Spacer(modifier = Modifier.weight(1f))

            // Send button
            MoccaCompactButton(
                text = "SEND",
                onClick = onSendClick,
                enabled = inputEnabled && inputText.isNotBlank(),
                icon = Icons.AutoMirrored.Filled.Send
            )
        }

        // ═══════════════ NAV INDICATOR WITH ICONS ═══════════════
        ChatInputBarNavIndicator(
            items = items,
            dragProgress = dragProgress,
            onItemClick = onItemClick,
            travelDistancePx = travelDistancePx,
            firstItemCenterPx = firstItemCenterPx,
            lastItemCenterPx = lastItemCenterPx,
            onTravelDistanceChanged = { travelDistancePx = it },
            onFirstItemCenterChanged = { firstItemCenterPx = it },
            onLastItemCenterChanged = { lastItemCenterPx = it },
            navIndicatorHeight = NavIndicatorHeight
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

// Height constants for ChatInputBar
private val StatusBarHeight = 28.dp
private val InputMinHeight = 32.dp
private val InputMaxHeight = 80.dp
private val ActionToolbarHeight = 36.dp
private val NavIndicatorHeight = 32.dp // Increased to fit icons + dots

