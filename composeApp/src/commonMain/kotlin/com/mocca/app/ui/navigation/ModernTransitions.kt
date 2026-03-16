package com.mocca.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.mocca.app.ui.theme.MoccaTheme

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
        val exitSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
        
        return fadeIn(animationSpec = enterSpec) + 
               scaleIn(initialScale = 0.92f, animationSpec = enterSpec) togetherWith
               fadeOut(animationSpec = exitSpec) + 
               scaleOut(targetScale = 1.08f, animationSpec = exitSpec)
    }
}
