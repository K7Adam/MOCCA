package com.mocca.app.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.mocca.app.domain.model.Mode
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Action toolbar for ChatInputContent — quick actions (@ / ! P ↑ ↓), attachment, send/abort.
 */
@Composable
internal fun ChatInputActionToolbar(
    inputText: String,
    inputEnabled: Boolean,
    isSessionIdle: Boolean,
    onSendClick: () -> Unit,
    onAbortClick: () -> Unit,
    onAttachClick: () -> Unit,
    // @ mention palette
    modes: List<Mode>,
    selectedModeId: String?,
    onModeSelectedForMention: (Mode) -> Unit,
    // / command palette
    showCommandPalette: Boolean,
    onCommandPaletteToggle: () -> Unit,
    // Shell mode
    shellMode: Boolean,
    onShellModeToggle: () -> Unit,
    // Plan mode
    planMode: Boolean,
    onPlanModeToggle: () -> Unit,
    // History
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit
) {
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
            AgentMentionButton(
                modes = modes,
                selectedModeId = selectedModeId,
                onModeSelectedForMention = onModeSelectedForMention
            )

            // / command button
            ToggleActionButton(
                text = "/",
                isActive = showCommandPalette,
                onClick = onCommandPaletteToggle
            )
        }

        // ! shell mode button
        ToggleActionButton(
            text = "!",
            isActive = shellMode,
            onClick = onShellModeToggle
        )

        // P plan mode button
        ToggleActionButton(
            text = "P",
            isActive = planMode,
            onClick = onPlanModeToggle
        )

        // History up button
        ToggleActionButton(
            text = "\u2191",
            isActive = false,
            onClick = onHistoryUp
        )

        // History down button
        ToggleActionButton(
            text = "\u2193",
            isActive = false,
            onClick = onHistoryDown
        )

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
            SendButton(
                canSend = inputEnabled && inputText.isNotBlank(),
                onClick = onSendClick
            )
        } else {
            AbortButton(onClick = onAbortClick)
        }
    }
}

@Composable
private fun ToggleActionButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(NavConstants.ActionButtonSize)
            .background(
                color = if (isActive) AppColors.accentGreen.copy(alpha = 0.2f) else AppColors.surface.copy(alpha = 0.3f),
                shape = AppShapes.pill
            )
            .then(
                if (isActive) Modifier.border(AppSpacing.borderThin, AppColors.accentGreen, AppShapes.pill) else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) AppColors.accentGreen else AppColors.textSecondary,
            style = AppTypography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AgentMentionButton(
    modes: List<Mode>,
    selectedModeId: String?,
    onModeSelectedForMention: (Mode) -> Unit
) {
    var showAgentPalette by remember { mutableStateOf(false) }

    Box {
        // Anchor for dropdown
        ToggleActionButton(
            text = "@",
            isActive = showAgentPalette,
            onClick = { showAgentPalette = !showAgentPalette }
        )

        // Agent dropdown menu (stable positioning)
        DropdownMenu(
            expanded = showAgentPalette,
            onDismissRequest = { showAgentPalette = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier
                .background(AppColors.surfaceContainerHigh, AppShapes.medium)
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
}

@Composable
private fun SendButton(
    canSend: Boolean,
    onClick: () -> Unit
) {
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
                onClick = onClick
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
                tint = if (canSend) AppColors.background else AppColors.textSecondary,
                modifier = Modifier.size(NavConstants.SendIconSize)
            )
            Text(
                text = "SEND",
                color = if (canSend) AppColors.background else AppColors.textSecondary,
                style = AppTypography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AbortButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(NavConstants.SendButtonHeight)
            .background(AppColors.error, AppShapes.pill)
            .clickable(onClick = onClick)
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
                tint = AppColors.textPrimary,
                modifier = Modifier.size(NavConstants.SendIconSize)
            )
            Text(
                text = "ABORT",
                color = AppColors.textPrimary,
                style = AppTypography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
