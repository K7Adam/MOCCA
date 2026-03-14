package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mocca.app.domain.model.Command
import com.mocca.app.domain.model.Mode
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Full-screen command palette overlay, equivalent to OpenCode Web Cmd+K modal.
 *
 * Shows a search field at the top and two sections:
 * - Slash commands (/) with name + optional description
 * - Agent modes (@) with name + optional description
 *
 * Selecting a command or mode dismisses the overlay and fires the corresponding callback.
 *
 * @param commands Available slash commands (already merged with built-ins)
 * @param modes    Available agent modes
 * @param onCommandSelected Called when the user picks a command
 * @param onModeSelected    Called when the user picks a mode
 * @param onDismiss         Called when the overlay is dismissed without a selection
 */
@Composable
fun CommandPaletteOverlay(
    commands: List<Command>,
    modes: List<Mode>,
    onCommandSelected: (Command) -> Unit,
    onModeSelected: (Mode) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Filtered lists
    val filteredCommands = remember(commands, query) {
        if (query.isBlank()) commands
        else commands.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.description?.contains(query, ignoreCase = true) == true
        }
    }
    val filteredModes = remember(modes, query) {
        if (query.isBlank()) modes
        else modes.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true) ||
                it.description?.contains(query, ignoreCase = true) == true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // Full-screen scrim with centred card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            // Card — stop click propagation so tapping inside doesn't dismiss
            Column(
                modifier = Modifier
                    .padding(top = 64.dp, start = AppSpacing.md, end = AppSpacing.md)
                    .fillMaxWidth()
                    .clip(AppShapes.dialog)
                    .background(AppColors.surfaceElevated)
                    .border(
                        width = AppSpacing.borderThin,
                        color = AppColors.border,
                        shape = AppShapes.dialog
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // consume click so the scrim dismissal is blocked
                    )
            ) {
                // ── Search field ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AppColors.accentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = AppTypography.bodyMedium.copy(color = AppColors.textPrimary),
                        cursorBrush = SolidColor(AppColors.accentGreen),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search commands and agents…",
                                        style = AppTypography.bodyMedium,
                                        color = AppColors.textPlaceholder
                                    )
                                }
                                inner()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = AppColors.textTertiary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { query = "" }
                                )
                        )
                    }
                }

                HorizontalDivider(
                    thickness = AppSpacing.borderThin,
                    color = AppColors.border.copy(alpha = 0.5f)
                )

                // ── Results list ─────────────────────────────────────────────
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                ) {
                    // Commands section
                    if (filteredCommands.isNotEmpty()) {
                        item {
                            PaletteSection(label = "COMMANDS")
                        }
                        itemsIndexed(filteredCommands) { _, cmd ->
                            CommandPaletteItem(
                                prefix = "/",
                                name = cmd.name,
                                description = cmd.description,
                                onClick = {
                                    onCommandSelected(cmd)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    // Modes / agents section
                    if (filteredModes.isNotEmpty()) {
                        item {
                            if (filteredCommands.isNotEmpty()) {
                                HorizontalDivider(
                                    thickness = AppSpacing.borderThin,
                                    color = AppColors.border.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(horizontal = AppSpacing.md)
                                )
                            }
                            PaletteSection(label = "AGENTS")
                        }
                        itemsIndexed(filteredModes) { _, mode ->
                            CommandPaletteItem(
                                prefix = "@",
                                name = mode.name,
                                description = mode.description,
                                onClick = {
                                    onModeSelected(mode)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    // Empty state
                    if (filteredCommands.isEmpty() && filteredModes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppSpacing.xl),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No results for \"$query\"",
                                    style = AppTypography.bodySmall,
                                    color = AppColors.textTertiary
                                )
                            }
                        }
                    }
                }

                // ── Footer hint ──────────────────────────────────────────────
                HorizontalDivider(
                    thickness = AppSpacing.borderThin,
                    color = AppColors.border.copy(alpha = 0.3f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    Text(
                        text = "↑↓ navigate",
                        style = AppTypography.labelSmall,
                        color = AppColors.textTertiary
                    )
                    Text(
                        text = "↵ select",
                        style = AppTypography.labelSmall,
                        color = AppColors.textTertiary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "esc dismiss",
                        style = AppTypography.labelSmall,
                        color = AppColors.textTertiary
                    )
                }
            }
        }
    }

    // Auto-focus the search field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaletteSection(label: String) {
    Text(
        text = label,
        style = AppTypography.labelSmall,
        color = AppColors.textTertiary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            start = AppSpacing.md,
            end = AppSpacing.md,
            top = AppSpacing.sm,
            bottom = AppSpacing.xxs
        )
    )
}

@Composable
private fun CommandPaletteItem(
    prefix: String,
    name: String,
    description: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Prefix badge
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = AppColors.accentGreen.copy(alpha = 0.15f),
                    shape = AppShapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = prefix,
                style = AppTypography.labelSmall,
                color = AppColors.accentGreen,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = AppTypography.bodySmall,
                color = AppColors.textPrimary,
                fontWeight = FontWeight.Medium
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = AppTypography.labelSmall,
                    color = AppColors.textTertiary,
                    maxLines = 1
                )
            }
        }
    }
}
