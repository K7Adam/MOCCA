package com.mocca.app.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

/**
 * Contextual screen transitions for MOCCA.
 */
object ModernTransitions {

    /**
     * A sharp, fast transition for panel changes.
     */
    @Composable
    fun panelTransition(): ContentTransform {
        val enterSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()

        return (
            fadeIn(animationSpec = enterSpec) +
                slideInVertically(
                    animationSpec = spatialSpec,
                    initialOffsetY = { it / 20 }
                )
            ) togetherWith (
            fadeOut(animationSpec = enterSpec) +
                slideOutVertically(
                    animationSpec = spatialSpec,
                    targetOffsetY = { -it / 20 }
                )
            )
    }

    /**
     * Subtle horizontal slide for panel content swaps (left/center/right).
     * Direction is context-dependent — caller should choose enter/exit direction.
     */
    @Composable
    fun panelSlideHorizontal(): ContentTransform {
        val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()

        return (
            fadeIn(animationSpec = effectsSpec) +
                slideInHorizontally(
                    animationSpec = spatialSpec,
                    initialOffsetX = { it / 10 }
                )
            ) togetherWith (
            fadeOut(animationSpec = effectsSpec) +
                slideOutHorizontally(
                    animationSpec = spatialSpec,
                    targetOffsetX = { -it / 10 }
                )
            )
    }

    /**
     * Lightweight root screen transition for global navigation.
     * Uses effect-only motion so LOW/MEDIUM performance tiers avoid full-screen
     * spatial transforms that can feel heavy during app-level navigation.
     */
    @Composable
    fun rootScreenFade(): ContentTransform {
        val enterSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        val exitSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

        return fadeIn(animationSpec = enterSpec) togetherWith
            fadeOut(animationSpec = exitSpec)
    }

    /**
     * A fluid, expressive fade + scale transition for screen changes.
     */
    @Composable
    fun expressiveFadeScale(): ContentTransform {
        val enterSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        val exitSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

        return (
            fadeIn(animationSpec = enterSpec) +
                scaleIn(initialScale = 0.97f, animationSpec = enterSpec)
            ) togetherWith (
            fadeOut(animationSpec = exitSpec) +
                scaleOut(targetScale = 0.97f, animationSpec = exitSpec)
            )
    }
}
