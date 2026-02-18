package com.mocca.app.ui.components.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.AttachedFile
import com.mocca.app.domain.model.Mode
import com.mocca.app.domain.model.ProviderResponse
import com.mocca.app.ui.components.modern.LiquidGlassDefaults
import com.mocca.app.ui.components.modern.glassyPremium
import com.mocca.app.ui.components.modern.liquidGlass
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.util.TerminalCommand
import dev.chrisbanes.haze.HazeState

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
 * Features:
 * - Fluid spring-based animations for mode transitions
 * - Single glassy container for cohesive premium look
 * - Integrated navigation indicator visible in both modes
 * - Maximizes screen real estate with compact design
 * - Liquid Glass effect with true blur (when hazeState provided)
 *
 * @param mode Current mode (Navigation or ChatInput)
 * @param dragProgress Real-time drag progress from SwipePanelLayout (0.0 = right, 0.5 = center, 1.0 = left)
 * @param onItemClick Callback when a navigation item is clicked
 * @param hazeState Optional HazeState for liquid glass blur effect. Pass null to use fallback glassyPremium.
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
    // Liquid Glass integration
    hazeState: HazeState? = null,
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
        is BottomBarMode.Navigation -> NavigationModeHeight
        is BottomBarMode.ChatInput -> ChatInputModeMinHeight
    }
    
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "bottomBarHeight"
    )
    
    // Animated alpha for mode content transitions
    val navModeAlpha by animateFloatAsState(
        targetValue = if (mode is BottomBarMode.Navigation) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "navModeAlpha"
    )
    
    val chatModeAlpha by animateFloatAsState(
        targetValue = if (mode is BottomBarMode.ChatInput) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "chatModeAlpha"
    )
    
    Box(
        modifier = modifier
            .widthIn(min = 280.dp, max = 400.dp)
            .padding(horizontal = AppSpacing.screenPaddingHorizontal)
            .navigationBarsPadding()
    ) {
        // Liquid Glass container with animated height - authentic iOS 26 style
        // Uses true blur when hazeState is available, otherwise falls back to premium gradients
        val containerModifier = if (hazeState != null) {
            Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .liquidGlass(
                    hazeState = hazeState,
                    shape = AppShapes.rounded2xl,
                    style = LiquidGlassDefaults.primary(
                        blurRadius = 25.dp,
                        noiseFactor = LiquidGlassDefaults.noiseFactor
                    ),
                    borderWidth = 1.dp,
                    borderColor = LiquidGlassDefaults.borderPrimary,
                    specularColor = LiquidGlassDefaults.specularTop,
                    refractionColor = LiquidGlassDefaults.refractionAccent,
                    showSpecular = true,
                    showRefraction = true
                )
        } else {
            Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .glassyPremium(
                    shape = AppShapes.rounded2xl,
                    borderWidth = 1.dp,
                    backgroundColor = LiquidGlassDefaults.tintPrimary,
                    borderColor = LiquidGlassDefaults.borderPrimary,
                    specularColor = LiquidGlassDefaults.specularTop,
                    refractionColor = LiquidGlassDefaults.refractionAccent
                )
        }
        
        Box(
            modifier = containerModifier,
            contentAlignment = Alignment.Center
        ) {
            // Navigation Mode Content
            if (navModeAlpha > 0.01f) {
                CompactNavBar(
                    dragProgress = dragProgress,
                    onItemClick = onItemClick,
                    modifier = Modifier.fillMaxWidth(),
                    alpha = navModeAlpha
                )
            }
            
            // Chat Input Mode Content
            if (chatModeAlpha > 0.01f) {
                ChatInputBar(
                    inputText = inputText,
                    onInputTextChange = onInputTextChange,
                    onSendClick = onSendClick,
                    inputEnabled = inputEnabled,
                    placeholder = placeholder,
                    dragProgress = dragProgress,
                    onItemClick = onItemClick,
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
                    modifier = Modifier.fillMaxWidth(),
                    alpha = chatModeAlpha
                )
            }
        }
    }
}

// Height constants
private val NavigationModeHeight = 56.dp
private val ChatInputModeMinHeight = 140.dp
