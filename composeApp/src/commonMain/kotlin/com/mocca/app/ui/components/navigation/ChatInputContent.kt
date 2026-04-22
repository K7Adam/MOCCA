package com.mocca.app.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.PopupProperties
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
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.moccaClickable

/**
 * Chat composer content above the persistent navigation row.
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
    isSessionIdle: Boolean = true,
    onAbortClick: () -> Unit = {},
    modelName: String,
    agentName: String,
    providerResponse: ProviderResponse?,
    selectedProviderId: String,
    selectedModelId: String,
    onModelSelected: (String, String) -> Unit,
    variants: List<String>,
    selectedVariantId: String?,
    onVariantSelected: (String) -> Unit,
    modes: List<Mode>,
    selectedModeId: String?,
    onModeSelected: (String?) -> Unit,
    attachedFiles: List<AttachedFile>,
    onRemoveAttachment: (AttachedFile) -> Unit,
    onAttachClick: () -> Unit,
    commands: List<Command>,
    onCommandSelected: (Command) -> Unit,
    onModeSelectedForMention: (Mode) -> Unit,
    shellMode: Boolean = false,
    onShellModeToggle: () -> Unit = {},
    planMode: Boolean = false,
    onPlanModeToggle: () -> Unit = {},
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
    var showTextSuggestions by remember { mutableStateOf(false) }
    var suggestionQuery by remember { mutableStateOf("") }
    var isCommandSuggestion by remember { mutableStateOf(true) }
    var showCommandPalette by remember { mutableStateOf(false) }

    val mergedCommands = remember(commands) { mergeCommands(commands) }
    val filteredCommands = remember(mergedCommands, suggestionQuery) {
        val query = suggestionQuery.lowercase()
        if (query.isEmpty()) mergedCommands else mergedCommands.filter { it.name.lowercase().contains(query) }
    }
    val filteredModes = remember(modes, suggestionQuery) {
        val query = suggestionQuery.lowercase()
        if (query.isEmpty()) modes else modes.filter {
            it.name.lowercase().contains(query) || it.id.lowercase().contains(query)
        }
    }
    val hasSendPayload = inputText.isNotBlank() || attachedFiles.isNotEmpty()
    val canSend = inputEnabled && isSessionIdle && hasSendPayload

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
        if (newValue.isEmpty()) {
            showTextSuggestions = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
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

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        ComposerSurface(
            focused = isInputFocused,
            canSend = canSend,
            isVoiceListening = isVoiceListening,
            inputEnabled = inputEnabled
        ) {
            if (attachedFiles.isNotEmpty()) {
                AttachmentPreviewStrip(files = attachedFiles, onRemove = onRemoveAttachment)
                Spacer(modifier = Modifier.height(AppSpacing.xs))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                ComposerIconButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Attach file",
                    onClick = onAttachClick,
                    enabled = inputEnabled
                )

                ChatInputTextFieldArea(
                    modifier = Modifier.weight(1f),
                    inputText = inputText,
                    onValueChange = handleValueChange,
                    onSendRequest = { if (canSend) onSendClick() },
                    canSend = canSend,
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

                ComposerIconButton(
                    icon = Icons.Default.Mic,
                    contentDescription = if (isVoiceListening) "Stop voice input" else "Start voice input",
                    onClick = onMicClick,
                    enabled = inputEnabled && isVoiceAvailable,
                    active = isVoiceListening
                )

                if (isSessionIdle) {
                    ComposerSendButton(canSend = canSend, onClick = onSendClick)
                } else {
                    ComposerAbortButton(onClick = onAbortClick)
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            ComposerToolRow(
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
                onHistoryDown = onHistoryDown,
                onExportClick = onExportClick,
                inputEnabled = inputEnabled
            )
        }
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

    if (showCommandPalette) {
        CommandPaletteOverlay(
            commands = mergedCommands,
            modes = modes,
            onCommandSelected = { command ->
                onCommandSelected(command)
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

@Composable
private fun ComposerSurface(
    focused: Boolean,
    canSend: Boolean,
    isVoiceListening: Boolean,
    inputEnabled: Boolean,
    content: @Composable () -> Unit
) {
    val targetBorderColor = when {
        !inputEnabled -> AppColors.outline.copy(alpha = 0.16f)
        isVoiceListening -> AppColors.primary.copy(alpha = 0.75f)
        canSend -> AppColors.primary.copy(alpha = 0.48f)
        focused -> AppColors.outline.copy(alpha = 0.42f)
        else -> AppColors.outline.copy(alpha = 0.22f)
    }
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "composerBorderColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.extraLarge)
            .background(AppColors.bgRaised, AppShapes.extraLarge)
            .border(AppSpacing.borderThin, borderColor, AppShapes.extraLarge)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.sm)
    ) {
        content()
    }
}

@Composable
private fun ComposerToolRow(
    modes: List<Mode>,
    selectedModeId: String?,
    onModeSelectedForMention: (Mode) -> Unit,
    showCommandPalette: Boolean,
    onCommandPaletteToggle: () -> Unit,
    shellMode: Boolean,
    onShellModeToggle: () -> Unit,
    planMode: Boolean,
    onPlanModeToggle: () -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    onExportClick: () -> Unit,
    inputEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        AgentToolButton(
            modes = modes,
            selectedModeId = selectedModeId,
            onModeSelectedForMention = onModeSelectedForMention,
            enabled = inputEnabled
        )
        ComposerTextButton(
            text = "/",
            active = showCommandPalette,
            contentDescription = "Open command palette",
            onClick = onCommandPaletteToggle,
            enabled = inputEnabled
        )
        ComposerTextButton(
            text = "!",
            active = shellMode,
            contentDescription = "Toggle shell mode",
            onClick = onShellModeToggle,
            enabled = inputEnabled
        )
        ComposerTextButton(
            text = "P",
            active = planMode,
            contentDescription = "Toggle plan mode",
            onClick = onPlanModeToggle,
            enabled = inputEnabled
        )
        ComposerIconButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "Previous prompt",
            onClick = onHistoryUp,
            enabled = inputEnabled,
            size = NavConstants.CompactActionButtonSize,
            iconSize = AppSpacing.iconSizeSmall
        )
        ComposerIconButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "Next prompt",
            onClick = onHistoryDown,
            enabled = inputEnabled,
            size = NavConstants.CompactActionButtonSize,
            iconSize = AppSpacing.iconSizeSmall
        )
        ComposerIconButton(
            icon = Icons.Default.ContentCopy,
            contentDescription = "Export chat to Markdown",
            onClick = onExportClick,
            enabled = true,
            size = NavConstants.CompactActionButtonSize,
            iconSize = AppSpacing.iconSizeSmall
        )
    }
}

@Composable
private fun AgentToolButton(
    modes: List<Mode>,
    selectedModeId: String?,
    onModeSelectedForMention: (Mode) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        ComposerTextButton(
            text = "@",
            active = expanded,
            contentDescription = "Open agent mention menu",
            onClick = { expanded = !expanded },
            enabled = enabled
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier
                .background(AppColors.surfaceContainerHigh, AppShapes.medium)
                .border(AppSpacing.borderThin, AppColors.outline.copy(alpha = 0.5f), AppShapes.medium)
        ) {
            if (modes.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "No agents available",
                            style = AppTypography.labelSmall,
                            color = AppColors.outline
                        )
                    },
                    onClick = { expanded = false }
                )
            } else {
                modes.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    mode.name.uppercase(),
                                    style = AppTypography.labelSmall,
                                    color = if (mode.id == selectedModeId) AppColors.primary else AppColors.onSurfaceVariant
                                )
                                mode.description?.let { description ->
                                    Text(
                                        description,
                                        style = AppTypography.labelSmall,
                                        color = AppColors.outline,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        onClick = {
                            onModeSelectedForMention(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    active: Boolean = false,
    size: Dp = NavConstants.ActionButtonSize,
    iconSize: Dp = NavConstants.ActionIconSize
) {
    val containerColor = when {
        active -> AppColors.primary.copy(alpha = 0.2f)
        enabled -> AppColors.surfaceContainerHigh.copy(alpha = 0.84f)
        else -> AppColors.surfaceContainer.copy(alpha = 0.46f)
    }
    val iconColor = when {
        active -> AppColors.primary
        enabled -> AppColors.onSurfaceVariant
        else -> AppColors.fgSubtle
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(AppShapes.pill)
            .background(containerColor, AppShapes.pill)
            .then(if (active) Modifier.border(AppSpacing.borderThin, AppColors.primary, AppShapes.pill) else Modifier)
            .moccaClickable(onClick = onClick, enabled = enabled, pressedScale = 0.94f)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun ComposerTextButton(
    text: String,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val containerColor = when {
        active -> AppColors.primary.copy(alpha = 0.2f)
        enabled -> AppColors.surfaceContainerHigh.copy(alpha = 0.84f)
        else -> AppColors.surfaceContainer.copy(alpha = 0.46f)
    }
    val textColor = when {
        active -> AppColors.primary
        enabled -> AppColors.onSurfaceVariant
        else -> AppColors.fgSubtle
    }

    Box(
        modifier = Modifier
            .size(NavConstants.CompactActionButtonSize)
            .clip(AppShapes.pill)
            .background(containerColor, AppShapes.pill)
            .then(if (active) Modifier.border(AppSpacing.borderThin, AppColors.primary, AppShapes.pill) else Modifier)
            .moccaClickable(onClick = onClick, enabled = enabled, pressedScale = 0.94f)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                this.selected = active
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = AppTypography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun ComposerSendButton(
    canSend: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (canSend) AppColors.primary else AppColors.surfaceContainerHigh.copy(alpha = 0.58f),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "composerSendContainerColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (canSend) AppColors.onPrimary else AppColors.fgSubtle,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "composerSendIconColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (canSend) 1f else 0.92f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "composerSendScale"
    )

    Box(
        modifier = Modifier
            .size(NavConstants.SendButtonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AppShapes.pill)
            .background(containerColor, AppShapes.pill)
            .moccaClickable(onClick = onClick, enabled = canSend, pressedScale = 0.95f)
            .semantics {
                role = Role.Button
                contentDescription = "Send message"
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(NavConstants.SendIconSize)
        )
    }
}

@Composable
private fun ComposerAbortButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(NavConstants.SendButtonHeight)
            .clip(AppShapes.pill)
            .background(AppColors.error, AppShapes.pill)
            .moccaClickable(onClick = onClick, pressedScale = 0.95f)
            .semantics {
                role = Role.Button
                contentDescription = "Abort current response"
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = AppColors.onSurface,
            modifier = Modifier.size(NavConstants.SendIconSize)
        )
    }
}
