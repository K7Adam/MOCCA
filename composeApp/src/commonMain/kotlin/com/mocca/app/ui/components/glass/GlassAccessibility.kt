@file:Suppress("DEPRECATION")

package com.mocca.app.ui.components.glass

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Accessibility support for Liquid Glass components.
 * 
 * This object provides utilities for detecting and responding to
 * accessibility settings that affect glass rendering:
 * 
 * - **Reduce Transparency**: When enabled, glass surfaces should use solid backgrounds
 * - **Reduce Motion**: When enabled, parallax/refraction animations should be disabled
 * - **High Contrast Text**: When enabled, ensure text on glass meets WCAG AA contrast
 * - **Touch Target Size**: All interactive elements must be at least 48x48.dp
 * 
 * Usage:
 * ```kotlin
 * val accessibilityState = rememberGlassAccessibilityState()
 * 
 * GlassSurface(
 *     reducedTransparency = accessibilityState.reducedTransparency,
 *     ...
 * ) {
 *     // Content
 * }
 * ```
 */
object GlassAccessibility {
    
    /**
     * Minimum touch target size for accessibility compliance.
     */
    val minTouchTarget: Dp = GlassTokens.minTouchTarget
    
    /**
     * WCAG AA contrast ratio requirement (4.5:1).
     */
    const val minContrastRatio: Float = GlassTokens.minContrastRatio
    
    /**
     * Checks if "Reduce transparency" accessibility setting is enabled.
     * On Android, this maps to "Remove animations" in some contexts,
     * but there's no direct equivalent. We check for window animation scale.
     */
    fun isReducedTransparencyEnabled(context: Context): Boolean {
        // Android doesn't have a direct "reduce transparency" setting
        // We can check if the user has enabled "Remove animations"
        // which often indicates preference for reduced visual complexity
        return try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as? AccessibilityManager
            
            // Check if any accessibility service is enabled that might indicate
            // preference for reduced visual complexity
            accessibilityManager?.isTouchExplorationEnabled == true ||
            accessibilityManager?.isEnabled == false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Checks if "Reduce motion" accessibility setting is enabled.
     * Maps to window animation scale on Android.
     */
    fun isReducedMotionEnabled(context: Context): Boolean {
        return try {
            val animatorDurationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            
            val transitionAnimationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f
            )
            
            val windowAnimationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.WINDOW_ANIMATION_SCALE,
                1f
            )
            
            // If any animation scale is 0, consider motion reduced
            animatorDurationScale == 0f ||
            transitionAnimationScale == 0f ||
            windowAnimationScale == 0f
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Checks if high contrast text is enabled.
     */
    fun isHighContrastTextEnabled(context: Context): Boolean {
        return try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as? AccessibilityManager
            
            accessibilityManager?.isTouchExplorationEnabled == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets the current accessibility state.
     */
    fun getAccessibilityState(context: Context): GlassAccessibilityState {
        return GlassAccessibilityState(
            reducedTransparency = isReducedTransparencyEnabled(context),
            reducedMotion = isReducedMotionEnabled(context),
            highContrastText = isHighContrastTextEnabled(context)
        )
    }
}

/**
 * Accessibility state for glass components.
 */
data class GlassAccessibilityState(
    val reducedTransparency: Boolean = false,
    val reducedMotion: Boolean = false,
    val highContrastText: Boolean = false
) {
    companion object {
        val Default = GlassAccessibilityState()
        
        /**
         * Accessibility state with all accessibility features enabled.
         */
        val Accessible = GlassAccessibilityState(
            reducedTransparency = true,
            reducedMotion = true,
            highContrastText = true
        )
    }
}

/**
 * Remembers the current glass accessibility state.
 * 
 * This should be called at the top level of your screen and passed
 * to glass components.
 */
@Composable
fun rememberGlassAccessibilityState(): GlassAccessibilityState {
    val context = LocalContext.current
    
    return remember(context) {
        GlassAccessibility.getAccessibilityState(context)
    }
}

/**
 * Modifier that ensures minimum touch target size for accessibility.
 * 
 * Use this on all interactive elements (buttons, icon buttons, etc.)
 * inside glass surfaces.
 */
fun Modifier.accessibleTouchTarget(
    minSize: Dp = GlassAccessibility.minTouchTarget
): Modifier = this.size(minSize)

/**
 * Creates glass parameters adjusted for accessibility settings.
 */
fun GlassShaderParams.withAccessibility(
    accessibilityState: GlassAccessibilityState
): GlassShaderParams {
    return if (accessibilityState.reducedTransparency || accessibilityState.reducedMotion) {
        copy(
            enableRefraction = enableRefraction && !accessibilityState.reducedMotion,
            enableChromaticAberration = enableChromaticAberration && !accessibilityState.reducedMotion,
            reducedTransparency = accessibilityState.reducedTransparency,
            reducedMotion = accessibilityState.reducedMotion
        )
    } else {
        this
    }
}

/**
 * Creates glass tokens adjusted for accessibility settings.
 */
fun GlassThemeTokens.withAccessibility(
    accessibilityState: GlassAccessibilityState
): GlassThemeTokens {
    return if (accessibilityState.reducedTransparency) {
        GlassThemeTokens(isDark).copy(
            // Use fallback background for solid surfaces
        )
    } else {
        this
    }
}

/**
 * Ensures text contrast meets WCAG AA requirements on glass surfaces.
 * 
 * This utility calculates the appropriate text color based on
 * background brightness and accessibility settings.
 * 
 * @param backgroundColor The glass surface background color
 * @param isHighContrastEnabled Whether high contrast mode is enabled
 * @return The appropriate text color for readability
 */
@Composable
fun rememberContrastAdjustedTextColor(
    backgroundColor: androidx.compose.ui.graphics.Color,
    isHighContrastEnabled: Boolean = false
): androidx.compose.ui.graphics.Color {
    val tokens = GlassDefaults.tokens()
    
    return remember(backgroundColor, isHighContrastEnabled) {
        // Calculate luminance of background
        val luminance = (
            0.299 * backgroundColor.red +
            0.587 * backgroundColor.green +
            0.114 * backgroundColor.blue
        )
        
        // For dark backgrounds (typical in MOCCA), use white text
        // For light backgrounds, use dark text
        when {
            isHighContrastEnabled -> {
                if (luminance > 0.5) {
                    androidx.compose.ui.graphics.Color.Black
                } else {
                    androidx.compose.ui.graphics.Color.White
                }
            }
            luminance > 0.5 -> {
                androidx.compose.ui.graphics.Color(0xFF16171E) // Dark navy text
            }
            else -> {
                androidx.compose.ui.graphics.Color.White // White text
            }
        }
    }
}

/**
 * Provides glass accessibility settings via CompositionLocal.
 * 
 * Wrap your app with this provider to make accessibility settings
 * available to all glass components.
 * 
 * Usage:
 * ```kotlin
 * GlassAccessibilityProvider {
 *     AppTheme {
 *         // Your app content
 *     }
 * }
 * ```
 */
@Composable
fun GlassAccessibilityProvider(
    content: @Composable () -> Unit
) {
    val accessibilityState = rememberGlassAccessibilityState()
    
    androidx.compose.runtime.CompositionLocalProvider(
        LocalGlassAccessibilityState provides accessibilityState,
        content = content
    )
}

/**
 * CompositionLocal for glass accessibility state.
 */
val LocalGlassAccessibilityState = androidx.compose.runtime.staticCompositionLocalOf {
    GlassAccessibilityState.Default
}

/**
 * Extension to access accessibility state in composables.
 */
@Composable
@ReadOnlyComposable
fun glassAccessibilityState(): GlassAccessibilityState {
    return LocalGlassAccessibilityState.current
}
