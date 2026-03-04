package com.mocca.app.ui.components.modern.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

// ---------------------------------------------------------------------------
// Reasoning & Thinking part renderers
// ---------------------------------------------------------------------------

@Composable
fun ModernReasoningBlock(part: MessagePart.Reasoning) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.medium)
            .background(AppColors.background.copy(alpha = 0.3f), AppShapes.medium)
            .padding(AppSpacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = AppColors.statusThinking,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "THOUGHTS [${part.timeMs}ms]",
                color = AppColors.textSecondary,
                style = AppTypography.labelExtraSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppColors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AppSpacing.sm),
                    thickness = AppSpacing.borderThin,
                    color = AppColors.border.copy(alpha = 0.5f)
                )
                Text(
                    text = part.content,
                    color = AppColors.textTertiary,
                    style = AppTypography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ModernThinkingBlock(part: MessagePart.Thinking) {
    var expanded by remember { mutableStateOf(false) }
    val durationText = part.durationMs?.let { ms ->
        when {
            ms < 1000 -> "${ms}ms"
            ms < 60000 -> "${ms / 1000}.${(ms % 1000) / 100}s"
            else -> "${ms / 60000}m ${(ms % 60000) / 1000}s"
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .border(AppSpacing.borderThin, AppColors.statusThinking.copy(alpha = 0.3f), AppShapes.medium)
            .background(AppColors.background.copy(alpha = 0.3f), AppShapes.medium)
            .padding(AppSpacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = AppColors.statusThinking,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = if (durationText != null) "THINKING [$durationText]" else "THINKING",
                color = AppColors.statusThinking,
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppColors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AppSpacing.sm),
                    thickness = AppSpacing.borderThin,
                    color = AppColors.statusThinking.copy(alpha = 0.2f)
                )
                Text(
                    text = part.content,
                    color = AppColors.textTertiary,
                    style = AppTypography.bodySmall
                )
            }
        }
    }
}
