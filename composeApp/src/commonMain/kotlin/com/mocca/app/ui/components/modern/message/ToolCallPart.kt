package com.mocca.app.ui.components.modern.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
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
import com.mocca.app.domain.model.ToolState
import com.mocca.app.ui.components.RichToolCard
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

// ---------------------------------------------------------------------------
// Tool result & grouped tool calls
// ---------------------------------------------------------------------------

@Composable
fun ModernToolResultBlock(part: MessagePart.ToolResult) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.surface.copy(alpha = 0.5f), AppShapes.medium)
            .padding(AppSpacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOOL OUTPUT",
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
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = part.result,
                    color = AppColors.whiteDim,
                    style = AppTypography.bodySmall
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ContextToolGroup — collapsible card grouping consecutive tool calls
// ---------------------------------------------------------------------------

@Composable
fun ContextToolGroup(tools: List<Pair<MessagePart.ToolInvocation, MessagePart.ToolResult?>>) {
    if (tools.isEmpty()) return

    val hasRunning = tools.any { it.first.state == ToolState.RUNNING }
    val errorCount = tools.count { it.first.state == ToolState.ERROR }
    val completedCount = tools.count { it.first.state == ToolState.COMPLETED }

    // Auto-expand when any tool is running; collapse when everything is done and count >= 2
    var expanded by remember(hasRunning) { mutableStateOf(hasRunning || tools.size == 1) }

    val accentColor = when {
        hasRunning -> AppColors.statusWaiting
        errorCount > 0 -> AppColors.error
        else -> AppColors.textTertiary
    }
    val borderColor = accentColor.copy(alpha = 0.35f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .border(AppSpacing.borderThin, borderColor, AppShapes.medium)
            .background(AppColors.surfaceContainerHigh.copy(alpha = 0.6f), AppShapes.medium)
    ) {
        // Header row — always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "${tools.size} TOOLS",
                style = AppTypography.labelExtraSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.weight(1f)
            )
            // Status badges
            if (hasRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = AppColors.statusWaiting
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
            }
            if (completedCount > 0) {
                Text(
                    text = "\u2713 $completedCount",
                    style = AppTypography.labelExtraSmall,
                    color = AppColors.accentGreen
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
            }
            if (errorCount > 0) {
                Text(
                    text = "\u2717 $errorCount",
                    style = AppTypography.labelExtraSmall,
                    color = AppColors.error
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppColors.textTertiary,
                modifier = Modifier.size(14.dp)
            )
        }

        // Expanded tool cards
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(
                    start = AppSpacing.sm,
                    end = AppSpacing.sm,
                    bottom = AppSpacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                tools.forEach { (invocation, _) ->
                    RichToolCard(invocation)
                }
            }
        }
    }
}
