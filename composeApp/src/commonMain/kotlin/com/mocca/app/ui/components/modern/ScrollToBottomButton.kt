@file:Suppress("DEPRECATION")

package com.mocca.app.ui.components.modern

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
/**
 * A clean Material 3 Surface button to scroll to the bottom of the chat.
 *
 * @param isVisible Whether the button should be visible (driven by scroll state)
 * @param hasNewMessages Whether there are unread messages below the viewport
 * @param onClick Callback invoked when the user taps the button
 * @param modifier Modifier for positioning / z-index
 */
@Composable
fun ScrollToBottomButton(
    isVisible: Boolean,
    hasNewMessages: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            initialOffsetY = { it / 2 }
        ) + fadeIn(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
        ) + scaleIn(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            initialScale = 0.8f
        ),
        exit = slideOutVertically(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            targetOffsetY = { it / 2 }
        ) + fadeOut(
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
        ) + scaleOut(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            targetScale = 0.8f
        ),
        modifier = modifier
    ) {
        androidx.compose.material3.Surface(
            color = AppColors.surfaceContainer,
            shape = AppShapes.circle,
            modifier = Modifier.size(48.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Scroll to bottom",
                    tint = AppColors.white,
                    modifier = Modifier.size(20.dp)
                )

                if (hasNewMessages) {
                    // New message badge (mint green dot)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .clip(AppShapes.circle)
                            .background(AppColors.accentGreen)
                            .border(2.dp, AppColors.background, AppShapes.circle)
                    )
                }
            }
    }
    }
}