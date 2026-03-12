package com.mocca.app.ui.components.modern

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.mocca.app.data.GlobalActivityManager
import com.mocca.app.ui.theme.AppColors
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import org.koin.compose.koinInject

/**
 * A subtle pulsing indicator shown when any background activity is running.
 * Should be placed in a corner of the main UI (e.g., top-right of status bar).
 * 
 * Terminal aesthetic: Small, unobtrusive, monochrome pulsing dot.
 */
@Composable
fun GlobalActivityIndicator(
    modifier: Modifier = Modifier,
    activityManager: GlobalActivityManager = koinInject()
) {
    val isActive by activityManager.isActive.collectAsState()
    
    if (!isActive) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "activity_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "activity_alpha"
    )
    
    Box(
        modifier = modifier
            .padding(8.dp)
            .size(8.dp)
            .alpha(alpha)
            .background(
                color = AppColors.accentGreen,
                shape = CircleShape
            )
    )
}

/**
 * Compact activity indicator for inline use (e.g., in headers or status bars).
 * Shows a smaller dot with faster pulse.
 */
@Composable
fun CompactActivityIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isActive) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "compact_activity_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "compact_activity_alpha"
    )
    
    Box(
        modifier = modifier
            .size(6.dp)
            .alpha(alpha)
            .background(
                color = AppColors.accentGreen,
                shape = CircleShape
            )
    )
}
