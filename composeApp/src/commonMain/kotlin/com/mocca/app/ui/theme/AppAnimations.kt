package com.mocca.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object AppAnimations {
    const val DurationFast = 150
    const val DurationMedium = 300
    const val DurationSlow = 500
    
    val SpringDefault: SpringSpec<Float> = spring()
    
    val SpringSmooth: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val SpringBouncy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val SpringResponsive: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 200f
    )
    
    val SpringSnappy: SpringSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 380f
    )
    
    @Composable
    fun springDamping(dampingRatio: Float = Spring.DampingRatioMediumBouncy): SpringSpec<Float> {
        return spring(
            dampingRatio = dampingRatio,
            stiffness = Spring.StiffnessLow
        )
    }
}

object AppDurations {
    const val Instant = 0
    const val Fast = 100
    const val Quick = 150
    const val Medium = 300
    const val Slow = 500
    const val Slower = 800
}

object AppEasing {
    val FastOutSlowIn = tween<Float>(durationMillis = 150, easing = FastOutSlowInEasing)
    val Smooth = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
}

@Composable
fun rememberFastSpring() = AppAnimations.SpringResponsive

@Composable
fun rememberBouncySpring() = AppAnimations.SpringBouncy

@Composable
fun rememberSmoothSpring() = AppAnimations.SpringSmooth

@Composable
fun rememberSnappySpring() = AppAnimations.SpringSnappy
