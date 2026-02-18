package com.mocca.app.ui.components.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.AttachedFile
import com.mocca.app.domain.model.Mode
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.ui.components.glass.GlassDefaults
import com.mocca.app.ui.components.glass.GlassShaderParams
import com.mocca.app.ui.components.glass.GlassThemeTokens
import com.mocca.app.ui.components.glass.glassFloating
import com.mocca.app.ui.components.modern.LiquidGlassDefaults
import com.mocca.app.ui.components.modern.glassyPremium
import com.mocca.app.ui.components.modern.liquidGlassFloating
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.util.TerminalCommand
import io.github.fletchmckee.liquid.LiquidState

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
 * LiquidGlassContainer
 * ├── MorphingContentAboveNav (cross-fades based on mode)
 * │   ├── [Nav Mode: Spacer]
 * │   └── [Chat Mode: ChatInputContent]
 * └── PersistentNavRow (ALWAYS SAME SIZE AND POSITION)
 * ```
 *
 * Features:
 * - Fluid spring-based animations for mode transitions
 * - Single glassy container for cohesive premium look
 * - Nav buttons NEVER change size or position
 * - Integrated navigation indicator visible in both modes
 * - TRUE Liquid Glass effect with lens refraction, chromatic aberration
 *
 * @param mode Current mode (Navigation or ChatInput)
 * @param dragProgress Real-time drag progress from SwipePanelLayout (0.0 = right, 0.5 = center, 1.0 = left)
 * @param onItemClick Callback when a navigation item is clicked
 * @param liquidState Optional LiquidState for TRUE liquid glass effect. Pass null to use fallback glassyPremium.
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
    onItemClick: (PanelState) -> Unit,
    // TRUE Liquid Glass integration (legacy - prefer glassTokens parameter)
    // Note: liquidState is deprecated. Use glassTokens parameter instead.
    liquidState: LiquidState? = null,
    // New Glass system tokens
    glassTokens: GlassThemeTokens = GlassDefaults.tokens(),
    // Chat input parameters
    inputText: String = "",
    onInputTextChange: (String) -> Unit = {},
    onSendClick: () -> Unit = {},
    inputEnabled: Boolean = true,
    placeholder: String = "Type a message...",
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
    commands: List<TerminalCommand> = emptyList(),
    onCommandSelected: (TerminalCommand) -> Unit = {},
    onModeSelectedForMention: (Mode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Animated height based on mode
    val targetHeight = when (mode) {
        is BottomBarMode.Navigation -> NavConstants.NavigationModeHeight
        is BottomBarMode.ChatInput -> NavConstants.ChatInputModeMinHeight
    }
    
    // Premium smooth height animation with low bouncy spring
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bottomBarHeight"
    )
    
    // Determine if we should show labels (only in Navigation mode)
    val showLabels = mode is BottomBarMode.Navigation
    
    Box(
        modifier = modifier
            .widthIn(min = 320.dp, max = 440.dp)
            .padding(horizontal = AppSpacing.screenPaddingHorizontal)
            .navigationBarsPadding()
    ) {
        // TRUE Liquid Glass container with animated height - authentic iOS 26 style
        // Uses lens refraction, chromatic aberration, and saturation boost
        // Priority: 1) Legacy liquid library 2) New first-principles Glass system
        @Suppress("DEPRECATION")
        val containerModifier = if (liquidState != null) {
            // Legacy path - use liquid library for backward compatibility
            Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .liquidGlassFloating(
                    liquidState = liquidState,
                    shape = AppShapes.rounded2xl,
                    tint = LiquidGlassDefaults.tintSemiDark
                )
        } else {
            // NEW: First-principles Glass system
            Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .glassFloating(
                    shape = AppShapes.rounded2xl,
                    tokens = glassTokens,
                    reducedTransparency = false
                )
        }
        
        Column(
            modifier = containerModifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ═══════════════ MORPHING CONTENT ABOVE NAV ═══════════════
            // Premium slide + fade transition for smooth morphing effect
            // The nav row below NEVER changes size or position
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    // ChatInput slides up and fades in (entering)
                    // Navigation slides up slightly and fades out (exiting)
                    (slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetY = { it / 3 }  // Slide from bottom third
                    ) + fadeIn(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )).togetherWith(
                        slideOutVertically(
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            targetOffsetY = { -it / 4 }  // Slide up and out
                        ) + fadeOut(
                            animationSpec = tween(150, easing = FastOutSlowInEasing)
                        )
                    ).using(
                        // Clip during transition to prevent overflow
                        SizeTransform(clip = true)
                    )
                },
                label = "modeContentTransition"
            ) { targetMode ->
                when (targetMode) {
                    is BottomBarMode.Navigation -> {
                        // In navigation mode, content above nav is just padding
                        // The nav row will show icons + labels
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        )
                    }
                    is BottomBarMode.ChatInput -> {
                        // In chat input mode, show the input content above nav
                        // The nav row will show only icons (no labels) but SAME POSITION
                        ChatInputContent(
                            inputText = inputText,
                            onInputTextChange = onInputTextChange,
                            onSendClick = onSendClick,
                            inputEnabled = inputEnabled,
                            placeholder = placeholder,
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        )
                    }
                }
            }
            
            // ═══════════════ PERSISTENT NAV ROW ═══════════════
            // This nav row is ALWAYS visible with SAME SIZE and SAME POSITION
            // Icons are always 22dp, touch targets always 48dp
            // Only the labels show/hide based on mode
            PersistentNavRow(
                dragProgress = dragProgress,
                onItemClick = onItemClick,
                showLabels = showLabels,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
            )
        }
    }
}
