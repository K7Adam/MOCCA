package com.mocca.app.ui.components.modern.message

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.screens.chat.MarkdownText
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import com.mocca.app.ui.theme.AppTypography

// ---------------------------------------------------------------------------
// Streaming message (active generation) & Date separator
// ---------------------------------------------------------------------------

/**
 * Streaming message display with animated cursor for active AI generation.
 * Full-width layout — no bubble/background, consistent with the new MessageRow design.
 */
@Composable
fun ModernStreamingMessage(
    text: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.sm),
        horizontalAlignment = Alignment.Start
    ) {
        // Header
        Row(
            modifier = Modifier.padding(bottom = AppSpacing.xxs, start = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = AppColors.accentGreen,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Text(
                text = "AGENT STREAMING...",
                color = AppColors.accentGreen,
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Full-width content (no bubble for AI)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg)
        ) {
            Row {
                MarkdownText(
                    markdown = text,
                    style = AppTypography.bodyMedium,
                    color = AppColors.onSurface
                )
                Text(
                    text = "█",
                    color = AppColors.accentGreen.copy(alpha = cursorAlpha),
                    style = AppTypography.bodyMedium,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Date separator between messages
// ---------------------------------------------------------------------------

@Composable
fun DateSeparator(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AppColors.outline.copy(alpha = 0.5f))
        )
        Text(
            text = date.uppercase(),
            style = AppTypography.labelExtraSmall,
            color = AppColors.outline,
            modifier = Modifier.padding(horizontal = AppSpacing.md)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AppColors.outline.copy(alpha = 0.5f))
        )
    }
}
