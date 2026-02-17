package com.mocca.app.ui.components.terminal

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * A compact, glassmorphic floating button to scroll to the bottom of the chat.
 * Includes a new message indicator dot.
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
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(elevation = 12.dp, shape = AppShapes.circle, ambientColor = AppColors.accentGreen.copy(alpha = 0.5f))
                .clip(AppShapes.circle)
                .background(AppColors.glassBackground)
                .border(1.dp, AppColors.glassBorder, AppShapes.circle)
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
