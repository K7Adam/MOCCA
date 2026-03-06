package com.mocca.app.ui.components.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.AttachedFile
import com.mocca.app.domain.model.Command
import com.mocca.app.domain.model.Mode
import com.mocca.app.domain.model.ProviderResponse


import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import androidx.compose.material3.Surface

/**
 * Sealed class representing the current mode of the unified bottom bar.
 */
sealed class BottomBarMode {
    /** Compact navigation-only mode for non-chat screens */
    object Navigation : BottomBarMode()
    
    /** Expanded chat input mode with integrated navigation */
    object ChatInput : BottomBarMode()
}

/**
 * Premium unified floating bottom bar that morphs between navigation and chat input modes.
 * 
 * ARCHITECTURE:
 * The nav buttons are ALWAYS the same size and position - this is critical for UX.
 * Only the content ABOVE the nav buttons changes between modes.
 * 
 * Structure:
 * ```
 * LiquidContainer
 * ├── AnimatedVisibility (ChatInputContent - controlled by isChatInputVisible)
 * └── PersistentNavRow (ALWAYS VISIBLE - NEVER HIDDEN)
 * ```
 *
 * Clean M3 Surface with surfaceContainer color.
 *
 * Features:
 * - Fluid spring-based animations for mode transitions
 * - Single surface container for cohesive premium look
 * - Nav buttons NEVER change size or position
 * - Nav row is ALWAYS visible across all screens
 * - Integrated navigation indicator visible in both modes
 * - Dynamic spring-based animations with smooth transitions
 * - Luminance adaptation for dynamic brightness/contrast (when using backdrop)
 * - Auto-hide only affects chat input field, not navigation
 *
 * @param mode Current mode (Navigation or ChatInput)
 * @param dragProgress Real-time drag progress from SwipePanelLayout (0.0 = right, 0.5 = center, 1.0 = left)
 * @param isChatInputVisible Whether the chat input is visible (auto-hide support) - ONLY affects chat input, nav row stays visible
 * @param onItemClick Callback when a navigation item is clicked
 * 
 * @param modifier Modifier for styling
 * @param inputText Current input text (ChatInput mode only)
 * @param onInputTextChange Callback when input text changes
 * @param onSendClick Callback when send button is clicked
 * @param inputEnabled Whether input is enabled
 * @param placeholder Placeholder text for input field
 * @param modelName Current model name for display
 * @param agentName Current agent name for display
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
 */
@Composable
fun UnifiedFloatingBottomBar(
    mode: BottomBarMode,
    dragProgress: Float,
    isChatInputVisible: Boolean = true,
    onItemClick: (PanelState) -> Unit,
    // Chat input parameters

    // Chat input parameters
    inputText: String = "",
    onInputTextChange: (String) -> Unit = {},
    onSendClick: () -> Unit = {},
    inputEnabled: Boolean = true,
    placeholder: String = "Type a message...",
    // Agent state
    isSessionIdle: Boolean = true,
    onAbortClick: () -> Unit = {},
    // Model/Agent selection
    modelName: String = "--",
    agentName: String = "--",
    providerResponse: ProviderResponse? = null,
    selectedProviderId: String = "",
    selectedModelId: String = "",
    onModelSelected: (String, String) -> Unit = { _, _ -> },
    // Variants
    variants: List<String> = emptyList(),
    selectedVariantId: String? = null,
    onVariantSelected: (String) -> Unit = {},
    // Modes
    modes: List<Mode> = emptyList(),
    selectedModeId: String? = null,
    onModeSelected: (String?) -> Unit = {},
    // Attachments
    attachedFiles: List<AttachedFile> = emptyList(),
    onRemoveAttachment: (AttachedFile) -> Unit = {},
    onAttachClick: () -> Unit = {},
    // Commands
    commands: List<Command> = emptyList(),
    onCommandSelected: (Command) -> Unit = {},
    onModeSelectedForMention: (Mode) -> Unit = {},
    // Shell mode
    shellMode: Boolean = false,
    onShellModeToggle: () -> Unit = {},
    // Plan mode
    planMode: Boolean = false,
    onPlanModeToggle: () -> Unit = {},
    // Prompt history navigation
    onHistoryUp: () -> Unit = {},
    onHistoryDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Determine if chat input should be shown:
    // 1. Must be in ChatInput mode
    // 2. AND isChatInputVisible must be true (auto-hide check)
    val showChatInput = mode is BottomBarMode.ChatInput && isChatInputVisible
    
    // Calculate target height based on what's visible
    // Nav row is ALWAYS visible, chat input can be hidden
    val targetHeight = if (showChatInput) {
        NavConstants.ChatInputModeMinHeight
    } else {
        NavConstants.NavigationModeHeight
    }
    
    // Premium smooth height animation with low bouncy spring
    // Premium smooth height animation with bouncy spring
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bottomBarHeight"
    )
    
    // Determine if we should show labels - ALWAYS show labels for better UX
    // Labels help users understand navigation options in all modes
    val showLabels = true
    

    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPaddingHorizontalCompact)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Surface container for the bottom bar
        @Suppress("DEPRECATION")
        val containerModifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            // INVISIBLE GESTURE SHIELD:
            // Intercept slight horizontal/vertical finger rolls (wiggles)
            // so they are NOT stolen by the underlying SwipePanelLayout's anchoredDraggable.
            .draggable(
                state = rememberDraggableState { },
                orientation = androidx.compose.foundation.gestures.Orientation.Horizontal
            )
            .draggable(
                state = rememberDraggableState { },
                orientation = androidx.compose.foundation.gestures.Orientation.Vertical
            )
        
        Surface(
            modifier = containerModifier,
            color = AppColors.surfaceContainer,
            shape = AppShapes.rounded2xl
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
                // ═══════════════ CHAT INPUT CONTENT (Can auto-hide) ═══════════════
                // AnimatedVisibility allows the chat input to hide while nav row stays visible
                // This is the KEY fix: nav row is ALWAYS rendered below this
                AnimatedVisibility(
                    visible = showChatInput,
                    modifier = Modifier.weight(1f),
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    ),
                    exit = shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeOut(
                        animationSpec = tween(150, easing = FastOutSlowInEasing)
                    )
                ) {
                    ChatInputContent(
                        inputText = inputText,
                        onInputTextChange = onInputTextChange,
                        onSendClick = onSendClick,
                        inputEnabled = inputEnabled,
                        placeholder = placeholder,
                        isSessionIdle = isSessionIdle,
                        onAbortClick = onAbortClick,
                        modelName = modelName,
                        agentName = agentName,
                        providerResponse = providerResponse,
                        selectedProviderId = selectedProviderId,
                        selectedModelId = selectedModelId,
                        onModelSelected = onModelSelected,
                        variants = variants,
                        selectedVariantId = selectedVariantId,
                        onVariantSelected = onVariantSelected,
                        modes = modes,
                        selectedModeId = selectedModeId,
                        onModeSelected = onModeSelected,
                        attachedFiles = attachedFiles,
                        onRemoveAttachment = onRemoveAttachment,
                        onAttachClick = onAttachClick,
                        commands = commands,
                        onCommandSelected = onCommandSelected,
                        onModeSelectedForMention = onModeSelectedForMention,
                        shellMode = shellMode,
                        onShellModeToggle = onShellModeToggle,
                        planMode = planMode,
                        onPlanModeToggle = onPlanModeToggle,
                        onHistoryUp = onHistoryUp,
                        onHistoryDown = onHistoryDown,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // ═══════════════ PERSISTENT NAV ROW (ALWAYS VISIBLE!) ═══════════════
                // This nav row is ALWAYS visible with SAME SIZE and SAME POSITION
                // It NEVER hides - only the chat input above it can hide
                // Icons are always 22dp, touch targets always 48dp
                // Only the labels show/hide based on mode
                PersistentNavRow(
                    dragProgress = dragProgress,
                    onItemClick = onItemClick,
                    showLabels = showLabels,
                    isAgentRunning = !isSessionIdle, // Show indicator when agent is running
                    modifier = Modifier.fillMaxWidth() // Removed internal horizontal/vertical padding to extend touch targets to the pill's edge
                )
            }
        }
    }
}
