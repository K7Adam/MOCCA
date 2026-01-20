package com.mocca.app.ui.components.terminal

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalShapes
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

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
    var pulseAlpha by remember { mutableFloatStateOf(0.3f) }
    
    // Pulsing animation for "brain activity"
    LaunchedEffect(Unit) {
        while (true) {
            animate(0.3f, 1f, animationSpec = tween(600)) { value, _ ->
                pulseAlpha = value
            }
            animate(1f, 0.3f, animationSpec = tween(600)) { value, _ ->
                pulseAlpha = value
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TerminalSpacing.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = TerminalSpacing.xs, start = TerminalSpacing.sm)
        ) {
            // Brain icon with pulse
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        TerminalColors.statusThinking.copy(alpha = pulseAlpha),
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(TerminalSpacing.xs))
            Text(
                text = "THINKING...",
                color = TerminalColors.statusThinking,
                style = TerminalTypography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
            // Elapsed time
            if (elapsedMs > 0) {
                Text(
                    text = formatThinkingDuration(elapsedMs),
                    color = TerminalColors.textTertiary,
                    style = TerminalTypography.labelSmall
                )
            }
        }
        
        // Optional: Show thinking content preview (collapsed by default)
        if (thinkingContent.isNotEmpty()) {
            ExpandableThinkingPreview(content = thinkingContent)
        }
    }
}

@Composable
private fun ExpandableThinkingPreview(content: String) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TerminalShapes.medium)
            .background(TerminalColors.background.copy(alpha = 0.5f), TerminalShapes.medium)
            .border(
                width = TerminalSpacing.borderThin,
                color = TerminalColors.statusThinking.copy(alpha = 0.3f),
                shape = TerminalShapes.medium
            )
            .clickable { expanded = !expanded }
            .padding(TerminalSpacing.sm)
    ) {
        Text(
            text = if (expanded) content else content.take(100) + if (content.length > 100) "..." else "",
            color = TerminalColors.textSecondary,
            style = TerminalTypography.bodySmall,
            maxLines = if (expanded) Int.MAX_VALUE else 2
        )
        
        if (content.length > 100) {
            Spacer(modifier = Modifier.height(TerminalSpacing.xs))
            Text(
                text = if (expanded) "COLLAPSE" else "EXPAND",
                color = TerminalColors.statusThinking.copy(alpha = 0.7f),
                style = TerminalTypography.labelSmall,
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
