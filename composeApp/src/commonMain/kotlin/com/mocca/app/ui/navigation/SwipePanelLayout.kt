package com.mocca.app.ui.navigation

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import kotlin.math.abs
import kotlin.math.roundToInt

enum class PanelState {
    CENTER,
    LEFT_OPEN,
    RIGHT_OPEN
}

/**
 * Holds drag progress state outside the composition scope.
 * Updates do NOT trigger recomposition of SwipePanelLayout or its parent.
 * Observers read [dragProgress] directly from this holder.
 */
class PanelProgressHolder {
    var dragProgress by mutableFloatStateOf(0.5f)
        private set
    internal fun updateProgress(progress: Float) { dragProgress = progress }
}

/**
 * Custom swipe panel layout with three panels: left, center, right.
 * Optimized for responsive navigation with balanced swipe distance.
 *
 * Performance optimizations:
 * - Uses onSizeChanged instead of BoxWithConstraints to avoid recomposition on layout changes
 * - PanelProgressHolder holds drag progress outside composition scope
 * - clipToBounds prevents offscreen panels from rendering outside bounds
 *
 * Best Practices Applied:
 * - Positional threshold (33%) — balanced sensitivity: not too eager, not too sluggish
 * - Velocity threshold (100dp) — moderate flick speed triggers navigation
 * - Spring physics snap animation — natural, bouncy panel settle
 * - Velocity-based navigation — fling gestures work naturally
 * - Direct progress holder for zero-callback indicator sync
 */

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
    progressHolder: PanelProgressHolder
) {
    val density = LocalDensity.current

    // Track container size via onSizeChanged — only fires on actual size changes
    var maxWidth by remember { mutableFloatStateOf(0f) }
    var effectivePanelWidthPx by remember { mutableFloatStateOf(0f) }

    // Initialize state with spring physics for natural, bouncy panel snapping
    val state = remember {
        AnchoredDraggableState(
            initialValue = panelState
        )
    }

    // Create fling behavior with animation specs and thresholds (Separated from state in Compose 1.7+)
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { distance -> distance * positionalThresholdFraction },
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
            .clipToBounds()
            .onSizeChanged { size ->
                val w = size.width.toFloat()
                maxWidth = w
                effectivePanelWidthPx = if (useFullWidthPanels) w
                    else with(density) { panelWidth.toPx() }

                // Update anchors whenever size changes
                if (effectivePanelWidthPx > 0f) {
                    val anchors = DraggableAnchors {
                        PanelState.LEFT_OPEN at effectivePanelWidthPx
                        PanelState.CENTER at 0f
                        PanelState.RIGHT_OPEN at -effectivePanelWidthPx
                    }
                    state.updateAnchors(anchors)
                }
            }
    ) {
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

        // Real-time drag progress reporting directly to holder (no parent callback)
        // Progress: 0.0 (RIGHT_OPEN) -> 0.5 (CENTER) -> 1.0 (LEFT_OPEN)
        LaunchedEffect(state) {
            var lastReportedProgress = Float.NaN
            snapshotFlow { state.offset }
                .collect { offset ->
                    val panelWidth = effectivePanelWidthPx
                    if (!offset.isNaN() && panelWidth > 0f) {
                        val progress = ((offset + panelWidth) / (2 * panelWidth))
                            .coerceIn(0f, 1f)
                        if (lastReportedProgress.isNaN() || abs(progress - lastReportedProgress) >= 0.02f) {
                            lastReportedProgress = progress
                            progressHolder.updateProgress(progress)
                        }
                    }
                }
        }

        // Skip rendering panels until we have valid dimensions
        if (effectivePanelWidthPx > 0f) {
            // Apply anchoredDraggable to the draggable container
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
