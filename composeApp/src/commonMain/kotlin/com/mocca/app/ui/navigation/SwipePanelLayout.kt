package com.mocca.app.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
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
import com.mocca.app.ui.theme.AppColors
import kotlin.math.roundToInt

enum class PanelState {
    CENTER,
    LEFT_OPEN,
    RIGHT_OPEN
}

/**
 * Custom swipe panel layout with three panels: left, center, right.
 * Optimized for responsive navigation with balanced swipe distance.
 *
 * Best Practices Applied:
 * - Positional threshold (33%) — balanced sensitivity: not too eager, not too sluggish
 * - Velocity threshold (100dp) — moderate flick speed triggers navigation
 * - Spring physics snap animation — natural, bouncy panel settle
 * - Velocity-based navigation — fling gestures work naturally
 * - Real-time progress reporting for external indicator sync
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipePanelLayout(
    leftPanel: @Composable () -> Unit,
    centerPanel: @Composable () -> Unit,
    rightPanel: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    useFullWidthPanels: Boolean = true,
    panelWidth: Dp = 300.dp,
    positionalThresholdFraction: Float = 0.33f, // 33% — balanced trigger distance
    velocityThresholdDp: Dp = 100.dp, // Moderate velocity = intentional flicks
    animationDurationMs: Int = 200, // Fallback for non-spring animations
    panelState: PanelState = PanelState.CENTER,
    onPanelStateChange: (PanelState) -> Unit = {},
    onDragProgressChange: (Float) -> Unit = {} // Real-time progress callback: 0.0 (right) -> 0.5 (center) -> 1.0 (left)
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background.copy(alpha = 0.82f))
    ) {
        val density = LocalDensity.current
        val maxWidth = constraints.maxWidth.toFloat()
        // Use full screen width for panels, or custom width
        val effectivePanelWidthPx = if (useFullWidthPanels) maxWidth else with(density) { panelWidth.toPx() }
        
        // Calculate velocity threshold in pixels
        val velocityThresholdPx = with(density) { velocityThresholdDp.toPx() }
        
        // Initialize state with spring physics for natural, bouncy panel snapping
        val state = remember(effectivePanelWidthPx) {
            AnchoredDraggableState(
                initialValue = panelState
            )
        }
        
        // Create fling behavior with animation specs and thresholds (Separated from state in Compose 1.7+)
        val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
            state = state,
            positionalThreshold = { distance -> distance * positionalThresholdFraction },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        
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

        // Real-time drag progress reporting
        // Progress: 0.0 (RIGHT_OPEN) -> 0.5 (CENTER) -> 1.0 (LEFT_OPEN)
        LaunchedEffect(state, effectivePanelWidthPx) {
            snapshotFlow { state.offset }
                .collect { offset ->
                    if (!offset.isNaN() && effectivePanelWidthPx > 0) {
                        // Calculate progress: (offset + width) / (2 * width)
                        // RIGHT_OPEN (-width) = 0.0, CENTER (0) = 0.5, LEFT_OPEN (+width) = 1.0
                        val progress = (offset + effectivePanelWidthPx) / (2 * effectivePanelWidthPx)
                        onDragProgressChange(progress.coerceIn(0f, 1f))
                    }
                }
        }

        // Apply anchoredDraggable to the root container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    flingBehavior = flingBehavior
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
