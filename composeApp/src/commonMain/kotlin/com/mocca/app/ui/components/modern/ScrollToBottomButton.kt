@file:Suppress("DEPRECATION")

package com.mocca.app.ui.components.modern

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
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.components.glass.LiquidBackdrop
import com.mocca.app.ui.components.glass.liquidGlassFab
import com.mocca.app.ui.components.glass.glassButton

/**
 * A liquid-glass-styled floating action button to scroll to the bottom of the chat.
 *
 * When backdrop/graphicsLayer/luminance are provided (placed OUTSIDE the backdrop source
 * hierarchy, e.g. in MainScreen), it uses the real Kyant0 liquidGlassFab() for true
 * liquid glass with lens refraction, blur, and luminance adaptation — matching the
 * unified bottom bar exactly.
 *
 * Falls back to gradient-glass [glassButton] when no backdrop is available.
 *
 * @param isVisible Whether the button should be visible (driven by scroll state)
 * @param hasNewMessages Whether there are unread messages below the viewport
 * @param onClick Callback invoked when the user taps the button
 * @param backdrop LiquidBackdrop for real liquid glass (optional)
 * @param graphicsLayer GraphicsLayer for luminance sampling (optional)
 * @param luminance Current luminance 0f-1f (optional)
 * @param modifier Modifier for positioning / z-index
 */
@Composable
fun ScrollToBottomButton(
    isVisible: Boolean,
    hasNewMessages: Boolean,
    onClick: () -> Unit,
    backdrop: LiquidBackdrop? = null,
    graphicsLayer: GraphicsLayer? = null,
    luminance: Float = 0f,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        // Use real liquid glass when backdrop is available, else fallback
        val glassModifier = if (backdrop != null && graphicsLayer != null) {
            Modifier
                .size(48.dp)
                .liquidGlassFab(
                    backdrop = backdrop,
                    layer = graphicsLayer,
                    luminance = luminance
                )
        } else {
            Modifier
                .size(48.dp)
                .glassButton(shape = AppShapes.circle)
        }

        Box(
            modifier = glassModifier
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
