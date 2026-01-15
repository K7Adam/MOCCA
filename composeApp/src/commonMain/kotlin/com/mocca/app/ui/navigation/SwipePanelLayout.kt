package com.mocca.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import kotlin.math.roundToInt

enum class PanelState {
    CENTER,
    LEFT_OPEN,
    RIGHT_OPEN
}

/**
 * Custom swipe panel layout with three panels: left, center, right.
 * Optimized for quick, responsive navigation with minimal swipe distance.
 * 
 * Best Practices Applied:
 * - Low positionalThreshold (20%) - Only need to swipe 20% of screen width
 * - Low velocityThreshold (50dp) - Quick flicks trigger navigation immediately
 * - Fast snap animation (200ms) - Immediate feedback on release
 * - Velocity-based navigation - Fling gestures work naturally
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipePanelLayout(
    leftPanel: @Composable () -> Unit,
    centerPanel: @Composable () -> Unit,
    rightPanel: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    useFullWidthPanels: Boolean = true,
    panelWidth: Dp = 300.dp,
    positionalThresholdFraction: Float = 0.2f, // 20% - very easy to trigger
    velocityThresholdDp: Dp = 50.dp, // Low velocity = responsive flicks
    animationDurationMs: Int = 200, // Fast snap animation
    panelState: PanelState = PanelState.CENTER,
    onPanelStateChange: (PanelState) -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalColors.background)
    ) {
        val density = LocalDensity.current
        val maxWidth = constraints.maxWidth.toFloat()
        // Use full screen width for panels, or custom width
        val effectivePanelWidthPx = if (useFullWidthPanels) maxWidth else with(density) { panelWidth.toPx() }
        
        // Calculate velocity threshold in pixels
        val velocityThresholdPx = with(density) { velocityThresholdDp.toPx() }
        
        // Initialize state with optimized thresholds for quick, responsive swiping
        // Using deprecated constructor because new API doesn't expose threshold configuration
        val state = remember(effectivePanelWidthPx) {
            AnchoredDraggableState(
                initialValue = panelState,
                // Low positional threshold = easy to trigger with short swipes
                positionalThreshold = { distance -> distance * positionalThresholdFraction },
                // Low velocity threshold = quick flicks work even without reaching positional threshold
                velocityThreshold = { velocityThresholdPx },
                // Fast, responsive snap animation
                snapAnimationSpec = tween(durationMillis = animationDurationMs),
                // Smooth decay for natural feeling
                decayAnimationSpec = androidx.compose.animation.core.exponentialDecay(),
                confirmValueChange = { true }
            )
        }
        
        // Update anchors when panel width changes
        val anchors = DraggableAnchors {
            PanelState.LEFT_OPEN at effectivePanelWidthPx
            PanelState.CENTER at 0f
            PanelState.RIGHT_OPEN at -effectivePanelWidthPx
        }
        state.updateAnchors(anchors)
        
        // Sync external state change -> internal state
        LaunchedEffect(panelState) {
            if (state.currentValue != panelState) {
                state.animateTo(panelState)
            }
        }
        
        // Sync internal state change -> external listener
        LaunchedEffect(state) {
            snapshotFlow { state.targetValue }
                .collect { newState ->
                    if (newState != panelState) {
                        onPanelStateChange(newState)
                    }
                }
        }

        // Apply anchoredDraggable to the root container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal
                )
        ) {
            // Get current offset (safely handle NaN during init)
            val currentOffset = if (state.offset.isNaN()) 0f else state.offset
            
            // Left panel (positioned at -width + offset)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset((currentOffset - effectivePanelWidthPx).roundToInt(), 0) }
            ) {
                leftPanel()
            }
            
            // Center panel (moves with offset)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(currentOffset.roundToInt(), 0) }
            ) {
                centerPanel()
            }
            
            // Right panel (positioned at width + offset)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset((maxWidth + currentOffset).roundToInt(), 0) }
            ) {
                rightPanel()
            }
        }
    }
}

/**
 * Simplified version with state management built-in.
 */
@Composable
fun rememberPanelState(initialState: PanelState = PanelState.CENTER): PanelStateHolder {
    return remember { PanelStateHolder(initialState) }
}

class PanelStateHolder(initialState: PanelState) {
    var state by mutableStateOf(initialState)
    
    fun openLeft() { state = PanelState.LEFT_OPEN }
    fun openRight() { state = PanelState.RIGHT_OPEN }
    fun closePanel() { state = PanelState.CENTER }
    fun toggle(side: PanelSide) {
        state = when {
            side == PanelSide.LEFT && state != PanelState.LEFT_OPEN -> PanelState.LEFT_OPEN
            side == PanelSide.RIGHT && state != PanelState.RIGHT_OPEN -> PanelState.RIGHT_OPEN
            else -> PanelState.CENTER
        }
    }
}

enum class PanelSide {
    LEFT, RIGHT
}
