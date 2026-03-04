package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

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
    modelName: String = "--",
    agentName: String = "--",
    placeholder: String = "Type a message...",
    enabled: Boolean = true,
    providerResponse: com.mocca.app.domain.model.ProviderResponse? = null,
    selectedProviderId: String = "",
    selectedModelId: String = "",
    onModelSelected: (providerId: String, modelId: String) -> Unit = { _, _ -> },
    recentModels: List<com.mocca.app.domain.model.RecentModel> = emptyList(),
    variants: List<String> = emptyList(),
    selectedVariantId: String? = null,
    onVariantSelected: (String) -> Unit = {},
    modes: List<com.mocca.app.domain.model.Mode> = emptyList(),
    selectedModeId: String? = null,
    onModeSelected: (String?) -> Unit = {},
    attachedFiles: List<com.mocca.app.domain.model.AttachedFile> = emptyList(),
    onRemoveAttachment: (com.mocca.app.domain.model.AttachedFile) -> Unit = {},
    onAttachClick: () -> Unit = {},
    commands: List<com.mocca.app.util.TerminalCommand> = emptyList(),
    onCommandSelected: (com.mocca.app.util.TerminalCommand) -> Unit = {},
    onModeSelectedForMention: (com.mocca.app.domain.model.Mode) -> Unit = {}
) {
    var showModelSelector by remember { mutableStateOf(false) }
    var showVariantSelector by remember { mutableStateOf(false) }

    var showSuggestions by remember { mutableStateOf(false) }
    var suggestionQuery by remember { mutableStateOf("") }
    var suggestionType by remember { mutableStateOf<SuggestionType?>(null) }

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

    val handleValueChange = { newValue: String ->
        onValueChange(newValue)
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
        if (newValue.isEmpty()) {
            showSuggestions = false
        }
    }

    val onSuggestionSelected = { item: SuggestionItem ->
        when (item.type) {
            SuggestionType.COMMAND -> {
                onValueChange("/${item.id}")
                showSuggestions = false
            }
            SuggestionType.MODE -> {
                val mode = modes.find { it.id == item.id }
                if (mode != null) {
                    onModeSelectedForMention(mode)
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
            .background(AppColors.surfaceContainer, AppShapes.rounded2xl)
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        // Status bar (MODEL + MODE)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = AppSpacing.inputPaddingHorizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clickable(enabled = providerResponse != null) { showModelSelector = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Icon(Icons.Default.SmartToy, null, Modifier.size(12.dp), AppColors.textTertiary)
                Text(modelName.uppercase(), color = if (providerResponse != null) AppColors.textSecondary else AppColors.textTertiary, style = AppTypography.labelSmall)
            }
            if (variants.isNotEmpty()) {
                Row(
                    modifier = Modifier.clickable { showVariantSelector = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Icon(Icons.Default.Tune, null, Modifier.size(12.dp), AppColors.textTertiary)
                    Text((selectedVariantId ?: "DEFAULT").uppercase(), color = AppColors.textSecondary, style = AppTypography.labelSmall)
                }
            }
            Row(
                modifier = Modifier.clickable { /* Toggle Agent Logic */ },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Icon(Icons.Default.Person, null, Modifier.size(12.dp), AppColors.textTertiary)
                Text(agentName.uppercase(), color = AppColors.textTertiary, style = AppTypography.labelSmall)
            }
        }

        HorizontalDivider(thickness = AppSpacing.borderThin, color = AppColors.border)

        if (attachedFiles.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.inputPaddingHorizontal, vertical = AppSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                attachedFiles.forEach { file ->
                    AttachmentChip(file = file, onRemove = { onRemoveAttachment(file) })
                }
            }
            HorizontalDivider(thickness = AppSpacing.borderThin, color = AppColors.border)
        }

        val inputScrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ChatInputDefaults.MinHeight, max = ChatInputDefaults.MaxHeight)
                .padding(AppSpacing.inputPaddingHorizontal)
        ) {
            BasicTextField(
                value = value,
                onValueChange = handleValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(inputScrollState)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            event.key == Key.Enter &&
                            event.isCtrlPressed) {
                            onSendClick()
                            true
                        } else false
                    },
                enabled = enabled,
                textStyle = AppTypography.bodyMedium.copy(color = AppColors.white),
                cursorBrush = SolidColor(AppColors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                keyboardActions = KeyboardActions.Default,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier) {
                        if (value.isEmpty()) {
                            Text(placeholder, color = AppColors.textPlaceholder, style = AppTypography.bodyMedium)
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
        }

        HorizontalDivider(thickness = AppSpacing.borderThin, color = AppColors.border)

        // Action toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.actionToolbarHeight)
                .padding(horizontal = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoccaIconButton(
                icon = Icons.Default.Add,
                onClick = { handleValueChange(if (value.isEmpty()) "@" else "$value @") },
                size = AppSpacing.iconButtonSizeCompact,
                iconColor = AppColors.textSecondary
            )
            MoccaTextButton(text = "/", onClick = { handleValueChange("/") }, textColor = AppColors.textSecondary)
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            VerticalDivider(modifier = Modifier.height(16.dp), thickness = AppSpacing.borderThin, color = AppColors.border)
            Spacer(modifier = Modifier.width(AppSpacing.xs))
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
                                    color = if (mode.id == selectedModeId) AppColors.accent else AppColors.textSecondary
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
            MoccaIconButton(
                icon = Icons.Default.AttachFile,
                onClick = onAttachClick,
                size = AppSpacing.iconButtonSizeCompact,
                iconColor = AppColors.textSecondary
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            MoccaCompactButton(
                text = "SEND",
                onClick = onSendClick,
                enabled = enabled && value.isNotBlank(),
                icon = Icons.AutoMirrored.Filled.Send,
                backgroundColor = AppColors.accent,
                textColor = AppColors.background
            )
        }
    }

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

    if (showVariantSelector) {
        VariantSelectorDialog(
            variants = variants,
            selectedVariantId = selectedVariantId,
            onVariantSelected = onVariantSelected,
            onDismiss = { showVariantSelector = false }
        )
    }
}

@Composable
internal fun AttachmentChip(
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
        Icon(Icons.Default.AttachFile, null, tint = AppColors.textSecondary, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(file.name.take(20), style = AppTypography.labelSmall, color = AppColors.textSecondary)
        Spacer(modifier = Modifier.width(4.dp))
        Text("(${file.displaySize})", style = AppTypography.labelSmall, color = AppColors.textTertiary)
        Spacer(modifier = Modifier.width(AppSpacing.xs))
        Icon(Icons.Default.Close, "Remove", tint = AppColors.textTertiary, modifier = Modifier.size(14.dp).clickable { onRemove() })
    }
}
