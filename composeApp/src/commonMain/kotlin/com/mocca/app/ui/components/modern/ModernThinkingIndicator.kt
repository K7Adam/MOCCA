package com.mocca.app.ui.components.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Modern thinking indicator for AI reasoning.
 */
@Composable
fun ModernThinkingIndicator(
    thinkingContent: String = "",
    elapsedMs: Long = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs)
    ) {
        AnimatedVisibility(
            visible = thinkingContent.isEmpty(),
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = AppSpacing.xxs, start = AppSpacing.sm)
            ) {
                SnakeDotsLoader(
                    modifier = Modifier.size(40.dp),
                    dotColor = AppColors.statusThinking
                )
                
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                
                Text(
                    text = "THINKING...",
                    color = AppColors.statusThinking,
                    style = AppTypography.labelExtraSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                
                Text(
                    text = formatThinkingDuration(elapsedMs),
                    color = AppColors.outline,
                    style = AppTypography.labelExtraSmall
                )
            }
        }
        
        AnimatedVisibility(
            visible = thinkingContent.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            CompletedThinkingBlock(
                content = thinkingContent,
                elapsedMs = elapsedMs
            )
        }
    }
}

@Composable
private fun CompletedThinkingBlock(
    content: String,
    elapsedMs: Long,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.bgRaised, AppShapes.medium)
            .clickable { expanded = !expanded }
            .padding(AppSpacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Psychology,
                contentDescription = "Reasoning",
                tint = AppColors.statusThinking,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            
            Text(
                text = "Thought for ${formatThinkingDuration(elapsedMs)}",
                color = AppColors.statusThinking,
                style = AppTypography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = AppColors.statusThinking,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        rotationZ = if (expanded) 90f else 0f
                    }
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ExpandableThinkingPreview(content = content, expanded = expanded)
    }
}

@Composable
private fun ExpandableThinkingPreview(content: String, expanded: Boolean) {
    val lines = content.split("\n").filter { it.isNotBlank() }
    val displayContent = if (expanded) content else lines.firstOrNull() ?: ""

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = displayContent,
            color = AppColors.onSurfaceVariant,
            style = AppTypography.bodySmall,
            fontFamily = AppTypography.monoFamily
        )
        
        if (lines.size > 1) {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(
                text = if (expanded) "◓ COLLAPSE" else "◒ VIEW_${lines.size}_STEPS",
                color = AppColors.statusThinking.copy(alpha = 0.8f),
                style = AppTypography.labelSmall,
                fontWeight = FontWeight.Bold
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
