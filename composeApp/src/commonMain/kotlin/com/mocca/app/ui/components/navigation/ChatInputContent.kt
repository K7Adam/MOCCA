package com.mocca.app.ui.components.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mocca.app.domain.model.AttachedFile
import com.mocca.app.domain.model.Command
import com.mocca.app.domain.model.Mode
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.domain.model.mergeCommands
import com.mocca.app.ui.components.modern.CommandPaletteOverlay
import com.mocca.app.ui.components.modern.ModelSelectorDialog
import com.mocca.app.ui.components.modern.VariantSelectorDialog
import com.mocca.app.ui.components.voice.RequestVoicePermissionEffect
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing

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
    onExportClick: () -> Unit,
    onMicClick: () -> Unit,
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
    // Shell mode
    shellMode: Boolean = false,
    onShellModeToggle: () -> Unit = {},
    // Plan mode
    planMode: Boolean = false,
    onPlanModeToggle: () -> Unit = {},
    // Prompt history navigation
    onHistoryUp: () -> Unit = {},
    onHistoryDown: () -> Unit = {},
    isVoiceListening: Boolean = false,
    isVoiceAvailable: Boolean = false,
    voicePermissionRequestToken: Int = 0,
    onVoicePermissionResult: (Boolean) -> Unit = {},
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
            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        ChatInputStatusBar(
            modelName = modelName,
            agentName = agentName,
            providerResponse = providerResponse,
            onModelSelectorClick = { showModelSelector = true },
            variants = variants,
            selectedVariantId = selectedVariantId,
            onVariantSelectorClick = { showVariantSelector = true },
            modes = modes,
            selectedModeId = selectedModeId,
            onModeSelected = onModeSelected
        )

        ChatInputTextFieldArea(
            inputText = inputText,
            onValueChange = handleValueChange,
            inputEnabled = inputEnabled,
            placeholder = placeholder,
            onInputFocusChanged = { isInputFocused = it },
            showTextSuggestions = showTextSuggestions,
            onDismissSuggestions = { showTextSuggestions = false },
            isCommandSuggestion = isCommandSuggestion,
            suggestionQuery = suggestionQuery,
            filteredCommands = filteredCommands,
            filteredModes = filteredModes,
            selectedModeId = selectedModeId,
            onCommandSelected = onCommandSelected,
            onModeSelectedForMention = onModeSelectedForMention,
            onInputTextChange = onInputTextChange
        )

        if (attachedFiles.isNotEmpty()) {
            AttachmentPreviewStrip(
                files = attachedFiles,
                onRemove = onRemoveAttachment
            )
        }

        HorizontalDivider(
            thickness = AppSpacing.borderThin,
            color = AppColors.outline.copy(alpha = 0.3f)
        )

        ChatInputActionToolbar(
            inputText = inputText,
            inputEnabled = inputEnabled,
            isSessionIdle = isSessionIdle,
            onSendClick = onSendClick,
            onAbortClick = onAbortClick,
            onAttachClick = onAttachClick,
            onMicClick = onMicClick,
            onExportClick = onExportClick,
            isListening = isVoiceListening,
            isVoiceAvailable = isVoiceAvailable,
            modes = modes,
            selectedModeId = selectedModeId,
            onModeSelectedForMention = onModeSelectedForMention,
            showCommandPalette = showCommandPalette,
            onCommandPaletteToggle = { showCommandPalette = !showCommandPalette },
            shellMode = shellMode,
            onShellModeToggle = onShellModeToggle,
            planMode = planMode,
            onPlanModeToggle = onPlanModeToggle,
            onHistoryUp = onHistoryUp,
            onHistoryDown = onHistoryDown
        )

        // Subtle divider before nav row
        HorizontalDivider(
            thickness = AppSpacing.borderThin,
            color = AppColors.outline.copy(alpha = 0.3f)
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

    // Full-screen command palette overlay (triggered by / button)
    if (showCommandPalette) {
        CommandPaletteOverlay(
            commands = mergedCommands,
            modes = modes,
            onCommandSelected = { cmd ->
                onCommandSelected(cmd)
                showCommandPalette = false
            },
            onModeSelected = { mode ->
                onModeSelectedForMention(mode)
                showCommandPalette = false
            },
            onDismiss = { showCommandPalette = false }
        )
    }

    RequestVoicePermissionEffect(
        requestToken = voicePermissionRequestToken,
        onResult = onVoicePermissionResult
    )
}
