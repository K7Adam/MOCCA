package com.mocca.app.ui.components.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.AttachedFile
import com.mocca.app.domain.model.Mode
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.ui.components.modern.MoccaCompactButton
import com.mocca.app.ui.components.modern.MoccaIconButton
import com.mocca.app.ui.components.modern.MoccaTextButton
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
import com.mocca.app.util.TerminalCommand
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Chat input bar with integrated status bar, input field, action toolbar, and nav indicator.
 * 
 * Features:
 * - Compact status bar with model/agent/variant selectors
 * - Multi-line input field with placeholder
 * - Action toolbar with quick actions and send button
 * - Integrated nav indicator at bottom
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
            .alpha(alpha)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ═══════════════ STATUS BAR ═══════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(StatusBarHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Model selector
            Row(
                modifier = Modifier.clickable(enabled = providerResponse != null) { showModelSelector = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = AppColors.textTertiary
                )
                val displayModelName = modelName.substringAfterLast("/").let {
                    if (it.length > 14) "…" + it.takeLast(13) else it
                }
                Text(
                    text = displayModelName.uppercase(),
                    color = if (providerResponse != null) AppColors.textSecondary else AppColors.textTertiary,
                    style = AppTypography.labelSmall
                )
            }

            // Variant selector (if available)
            if (variants.isNotEmpty()) {
                Row(
                    modifier = Modifier.clickable { showVariantSelector = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = AppColors.textTertiary
                    )
                    Text(
                        text = (selectedVariantId ?: "DEF").take(6).uppercase(),
                        color = AppColors.textSecondary,
                        style = AppTypography.labelSmall
                    )
                }
            }

            // Agent selector
            var showAgentSelector by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.clickable { showAgentSelector = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = AppColors.textTertiary
                )
                Text(
                    text = agentName.take(10).uppercase(),
                    color = AppColors.textTertiary,
                    style = AppTypography.labelSmall
                )
            }
            
            if (showAgentSelector) {
                AgentSelectorBottomSheet(
                    modes = modes,
                    selectedModeId = selectedModeId,
                    onModeSelected = onModeSelected,
                    onDismiss = { showAgentSelector = false }
                )
            }
        }

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
                        SuggestionItem(cmd.trigger, cmd.trigger, cmd.description, SuggestionType.COMMAND)
                    },
                    onSuggestionSelected = { item ->
                        val cmd = commands.find { it.trigger == item.id }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NavIndicatorHeight),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val distanceFromProgress = abs(dragProgress - item.targetProgress)
                    val isSelected = distanceFromProgress < 0.25f

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 32.dp)
                            .onGloballyPositioned { coords ->
                                val center = coords.size.width / 2f
                                if (index == 0) firstItemCenterPx = coords.localToRoot(
                                    androidx.compose.ui.geometry.Offset(center, 0f)
                                ).x
                                if (index == items.size - 1) lastItemCenterPx = coords.localToRoot(
                                    androidx.compose.ui.geometry.Offset(center, 0f)
                                ).x
                                if (firstItemCenterPx != 0f && lastItemCenterPx != 0f) {
                                    travelDistancePx = lastItemCenterPx - firstItemCenterPx
                                }
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onItemClick(item.panelState) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Nav icon
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) AppColors.accentGreen else AppColors.textTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        // Indicator dot
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 4.dp else 3.dp)
                                .background(
                                    color = if (isSelected) AppColors.accentGreen else AppColors.textTertiary.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(50)
                                )
                        )
                    }
                }
            }

            // Sliding indicator line
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .offset {
                        val xOffsetPx = (travelDistancePx / 2f) * (1.0f - 2.0f * dragProgress)
                        IntOffset(xOffsetPx.roundToInt(), 0)
                    }
                    .background(
                        color = AppColors.accentGreen.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(1.dp)
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSelectorBottomSheet(
    modes: List<Mode>,
    selectedModeId: String?,
    onModeSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.background,
        contentColor = AppColors.white,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.border) },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "// SELECT AGENT",
                    style = AppTypography.titleMedium,
                    color = AppColors.white,
                    fontWeight = FontWeight.Bold
                )
                MoccaIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    iconColor = AppColors.grey
                )
            }
            
            HorizontalDivider(thickness = AppSpacing.borderThin, color = AppColors.border)
            
            // Agent list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(AppSpacing.sm)
            ) {
                item {
                    Text(
                        text = "// AVAILABLE",
                        style = AppTypography.labelSmall,
                        color = AppColors.accentGreen,
                        modifier = Modifier.padding(start = AppSpacing.sm, top = AppSpacing.sm, bottom = AppSpacing.xs)
                    )
                }
                
                items(modes) { mode ->
                    val isSelected = mode.id == selectedModeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onModeSelected(mode.id)
                                onDismiss()
                            }
                            .background(if (isSelected) AppColors.accentGreen.copy(alpha = 0.2f) else AppColors.background)
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "> ${mode.name.uppercase()}",
                            style = AppTypography.bodySmall,
                            color = if (isSelected) AppColors.accentGreen else AppColors.white
                        )
                        
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AppColors.accentGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
