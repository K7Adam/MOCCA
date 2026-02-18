package com.mocca.app.ui.components.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Luminance detection and animation utilities for liquid glass effects.
 * 
 * Common expect declarations for platform-specific implementations.
 * Android implementation uses pixel sampling with ITU-R BT.709 coefficients.
 */

/**
 * Default sampling interval for luminance detection.
 */
val DefaultLuminanceSamplingInterval: Duration = 1.seconds

/**
 * Default animation spec for luminance transitions.
 */
val DefaultLuminanceAnimationSpec: AnimationSpec<Float> = tween(500)

/**
 * Detects and animates luminance from a graphics layer.
 * 
 * Continuously samples pixels from the provided graphics layer,
 * calculates the average luminance, and animates to the new value.
 * 
 * @param layer The graphics layer to sample
 * @param samplingInterval How often to sample the layer (default: 1 second)
 * @param animationSpec Animation spec for luminance transitions
 * @return Animatable luminance value (0f-1f, capped at 0.8f)
 */
@Composable
expect fun rememberLuminanceAnimation(
    layer: GraphicsLayer,
    samplingInterval: Duration = DefaultLuminanceSamplingInterval,
    animationSpec: AnimationSpec<Float> = DefaultLuminanceAnimationSpec
): Animatable<Float, *>

/**
 * Returns appropriate text/icon color based on luminance.
 * 
 * High luminance (> threshold) returns Black (for bright backgrounds).
 * Low luminance returns White (for dark backgrounds).
 * 
 * @param luminance The current luminance value (0f-1f)
 * @param threshold Luminance threshold for color switch (default: 0.6f)
 * @param animationSpec Animation spec for color transitions
 * @return Animated color (Black or White)
 */
@Composable
expect fun rememberLuminanceTextColor(
    luminance: Float,
    threshold: Float = 0.6f,
    animationSpec: AnimationSpec<Color> = tween(500)
): Color
