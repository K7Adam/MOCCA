package com.mocca.app.ui.components.terminal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Terminal-style thinking indicator for Claude/o1 extended reasoning.
 * Shows a pulsing "brain activity" animation with optional thinking content preview.
 * Modern design: Rounded, subtle.
 */
@Composable
fun TerminalThinkingIndicator(
    thinkingContent: String = "",
    elapsedMs: Long = 0,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = AppSpacing.xs, start = AppSpacing.sm)
        ) {
            // High-fidelity activity indicator
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(1.dp, AppColors.statusThinking.copy(alpha = 0.5f), CircleShape)
                    .drawWithContent {
                        drawContent()
                        clipRect(bottom = size.height / 2f) {
                            drawCircle(
                                color = AppColors.statusThinking,
                                radius = size.width / 4f,
                                center = center,
                                alpha = pulseAlpha
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AppColors.statusThinking, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            
            Text(
                text = "REASONING...",
                color = AppColors.statusThinking,
                style = AppTypography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            
            // Animated duration
            Text(
                text = formatThinkingDuration(elapsedMs),
                color = AppColors.textTertiary,
                style = AppTypography.monoLabel
            )
        }
        
        // Optimized thinking content preview
        if (thinkingContent.isNotEmpty()) {
            ExpandableThinkingPreview(content = thinkingContent)
        }
    }
}

@Composable
private fun ExpandableThinkingPreview(content: String) {
    var expanded by remember { mutableStateOf(false) }
    
    val lines = content.split("\n").filter { it.isNotBlank() }
    val displayContent = if (expanded) content else lines.firstOrNull() ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = AppSpacing.xl) // Indent from icon
            .clip(AppShapes.medium)
            .background(AppColors.surfaceVariant.copy(alpha = 0.3f), AppShapes.medium)
            .border(
                width = AppSpacing.borderThin,
                color = AppColors.statusThinking.copy(alpha = 0.2f),
                shape = AppShapes.medium
            )
            .clickable { expanded = !expanded }
            .padding(AppSpacing.md)
    ) {
        AnimatedContent(
            targetState = displayContent,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "thinkingContent"
        ) { text ->
            Text(
                text = text,
                color = AppColors.textSecondary,
                style = AppTypography.bodySmall,
                fontFamily = AppTypography.monoFamily
            )
        }
        
        if (lines.size > 1) {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(
                text = if (expanded) "◓ COLLAPSE" else "◒ VIEW_${lines.size}_STEPS",
                color = AppColors.statusThinking.copy(alpha = 0.8f),
                style = AppTypography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

private fun formatThinkingDuration(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> "${ms / 1000}.${(ms % 1000) / 100}s"
        else -> "${ms / 60000}m ${(ms % 60000) / 1000}s"
    }
}
