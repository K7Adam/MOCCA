package com.mocca.app.ui.components.modern

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import io.github.fletchmckee.liquid.LiquidState

/**
 * A pure liquid glass floating button to scroll to the bottom of the chat.
 * No blur, no color - just transparent glass with edge highlights.
 * Includes a new message indicator dot.
 *
 * @param isVisible Whether the button should be visible
 * @param hasNewMessages Whether there are new messages
 * @param onClick Callback when button is clicked
 * @param liquidState Optional LiquidState (unused - kept for API compatibility)
 * @param modifier Modifier for styling
 */
@Composable
fun ScrollToBottomButton(
    isVisible: Boolean,
    hasNewMessages: Boolean,
    onClick: () -> Unit,
    liquidState: LiquidState? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .pureLiquidGlassButton(shape = AppShapes.circle)
                .clickable { onClick() },
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
