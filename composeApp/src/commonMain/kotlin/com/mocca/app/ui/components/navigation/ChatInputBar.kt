package com.mocca.app.ui.components.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.AttachedFile
import com.mocca.app.domain.model.Command
import com.mocca.app.domain.model.Mode
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.*
import com.mocca.app.ui.theme.AppTypography
import androidx.compose.ui.graphics.Color

/**
 * Chat input bar reconstructed with Material 3 Expressive HorizontalFloatingToolbar.
 */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    var showCommandPalette by remember { mutableStateOf(false) }
    
    var expanded by remember { mutableStateOf(true) }
    
    val items = defaultBottomNavItems
    var travelDistancePx by remember { mutableFloatStateOf(0f) }
    var firstItemCenterPx by remember { mutableFloatStateOf(0f) }
    var lastItemCenterPx by remember { mutableFloatStateOf(0f) }

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
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalFloatingToolbar(
            expanded = expanded,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            shape = AppShapes.extraLarge,
            modifier = Modifier.fillMaxWidth(),
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.xs)
                ) {
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
                        thickness = 0.5.dp,
                        color = AppColors.outline.copy(alpha = 0.3f),
                        modifier = Modifier.padding(horizontal = AppSpacing.md)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = InputMinHeight, max = InputMaxHeight)
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        isInputFocused = interactionSource.collectIsFocusedAsState().value

                        BasicTextField(
                            value = inputText,
                            onValueChange = handleValueChange,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = inputEnabled,
                            textStyle = AppTypography.bodyMedium.copy(color = AppColors.onSurface),
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

                        if (showSuggestions && currentSuggestions.isNotEmpty()) {
                            SuggestionPopup(
                                suggestions = currentSuggestions,
                                onSuggestionSelected = onSuggestionSelected,
                                onDismiss = { showSuggestions = false }
                            )
                        }

                        if (showCommandPalette && commands.isNotEmpty()) {
                            SuggestionPopup(
                                suggestions = commands.map { cmd ->
                                    SuggestionItem(cmd.name, "/${cmd.name}", cmd.description, SuggestionType.COMMAND)
                                },
                                onSuggestionSelected = { item ->
                                    val cmd = commands.find { it.name == item.id }
                                    if (cmd != null) {
                                        onCommandSelected(cmd)
                                    }
                                    showCommandPalette = false
                                },
                                onDismiss = { showCommandPalette = false }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ActionToolbarHeight)
                            .padding(horizontal = AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MoccaIconButton(
                            icon = Icons.Default.AttachFile,
                            onClick = onAttachClick,
                            size = 32.dp,
                            iconColor = AppColors.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        
                        Box(
                            modifier = Modifier
                                .clickable(onClick = { showCommandPalette = !showCommandPalette })
                                .padding(horizontal = AppSpacing.xs)
                        ) {
                            Text(
                                text = "/",
                                color = if (showCommandPalette) AppColors.accentGreen else AppColors.onSurfaceVariant,
                                style = AppTypography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        val sendEnabled = inputEnabled && inputText.isNotBlank()
                        
                        SplitButtonLayout(
                            leadingButton = {
                                SplitButtonDefaults.LeadingButton(
                                    onClick = onSendClick,
                                    enabled = sendEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.primary,
                                        contentColor = AppColors.onPrimary
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                                        modifier = Modifier.padding(horizontal = AppSpacing.sm)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("SEND", style = AppTypography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            trailingButton = {
                                SplitButtonDefaults.TrailingButton(
                                    onClick = { /* Toggle more options */ },
                                    enabled = sendEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.primary,
                                        contentColor = AppColors.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(AppSpacing.xs))

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

    if (showVariantSelector) {
        VariantSelectorDialog(
            variants = variants,
            selectedVariantId = selectedVariantId,
            onVariantSelected = onVariantSelected,
            onDismiss = { showVariantSelector = false }
        )
    }
}

private val StatusBarHeight = 28.dp
private val InputMinHeight = 32.dp
private val InputMaxHeight = 80.dp
private val ActionToolbarHeight = 36.dp
private val NavIndicatorHeight = 32.dp
