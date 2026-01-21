package com.mocca.app.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalShapes
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography
import androidx.compose.material3.MaterialTheme

/**
 * Context info components for the left panel.
 * Modern design: Rounded cards, clean typography.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// CONTEXT INFO GRID (2x2 layout)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Context info grid showing MCP status, model, latency, port.
 */
@Composable
fun ContextInfoGrid(
    mcpStatus: String,
    isMcpOnline: Boolean,
    model: String,
    latency: String,
    port: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(TerminalShapes.card)
            .background(TerminalColors.surfaceContainer, TerminalShapes.card)
            .border(TerminalSpacing.borderThin, TerminalColors.border, TerminalShapes.card)
            .padding(TerminalSpacing.cardPadding)
    ) {
        // Row 1: MCP_STATUS and MODEL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // MCP_STATUS
            Column {
                Text(
                    text = "MCP STATUS",
                    color = TerminalColors.textTertiary,
                    style = TerminalTypography.labelSmall
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                ) {
                    if (!isMcpOnline) {
                        StatusDot(
                            color = TerminalColors.statusOffline
                        )
                    }
                    Text(
                        text = mcpStatus.uppercase(),
                        color = if (isMcpOnline) TerminalColors.white else TerminalColors.statusOffline,
                        style = TerminalTypography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // MODEL
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "MODEL",
                    color = TerminalColors.textTertiary,
                    style = TerminalTypography.labelSmall
                )
                Text(
                    text = model.uppercase(),
                    color = TerminalColors.white,
                    style = TerminalTypography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(TerminalSpacing.md))
        
        // Row 2: LATENCY and PORT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LATENCY
            Column {
                Text(
                    text = "LATENCY",
                    color = TerminalColors.textTertiary,
                    style = TerminalTypography.labelSmall
                )
                Text(
                    text = latency.uppercase(),
                    color = TerminalColors.white,
                    style = TerminalTypography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // PORT
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "PORT",
                    color = TerminalColors.textTertiary,
                    style = TerminalTypography.labelSmall
                )
                Text(
                    text = port.uppercase(),
                    color = TerminalColors.white,
                    style = TerminalTypography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONTEXT WINDOW BAR (usage progress bar)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Context window usage bar.
 * Modern pill-shaped progress bar.
 */
@Composable
fun ContextWindowBar(
    usedTokens: Int,
    maxTokens: Int,
    modifier: Modifier = Modifier,
    barHeight: Dp = 8.dp,
    showLabels: Boolean = true
) {
    val usagePercent = if (maxTokens > 0) (usedTokens.toFloat() / maxTokens) else 0f
    val usagePercentInt = (usagePercent * 100).toInt()
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Labels row
        if (showLabels) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CTX WINDOW",
                    color = TerminalColors.textTertiary,
                    style = TerminalTypography.labelSmall
                )
                Text(
                    text = "${formatTokenCount(usedTokens)}/${formatTokenCount(maxTokens)}",
                    color = TerminalColors.white,
                    style = TerminalTypography.labelSmall
                )
            }
            
            Spacer(modifier = Modifier.height(TerminalSpacing.xs))
        }
        
        // Progress bar (pill shaped)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(TerminalShapes.pill)
                .background(TerminalColors.surfaceVariant, TerminalShapes.pill)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(usagePercent)
                    .fillMaxHeight() // Missing import, fix later or use Modifier.fillMaxHeight()
                    .clip(TerminalShapes.pill)
                    .background(TerminalColors.accentGreen, TerminalShapes.pill)
            )
        }
        
        // Usage percent label
        if (showLabels) {
            Spacer(modifier = Modifier.height(TerminalSpacing.xxs))
            Text(
                text = "$usagePercentInt% USED",
                color = TerminalColors.textTertiary,
                style = TerminalTypography.labelSmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/**
 * Format token count with k suffix for thousands.
 */
private fun formatTokenCount(count: Int): String {
    return when {
        count >= 1000 -> "${count / 1000}k"
        else -> count.toString()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONTEXT INFO CONTAINER (combines grid + usage bar)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Full context info container as shown in left panel mockup.
 */
@Composable
fun ContextInfoContainer(
    mcpStatus: String = "ONLINE",
    isMcpOnline: Boolean = true,
    model: String = "CLAUDE-3.5",
    latency: String = "45ms",
    port: String = ":8080",
    usedTokens: Int = 4096,
    maxTokens: Int = 32000,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONTEXT INFO",
                color = TerminalColors.textSecondary,
                style = TerminalTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            if (!isMcpOnline) {
                StatusDot(
                    color = TerminalColors.statusOffline
                )
            }
        }
        
        Spacer(modifier = Modifier.height(TerminalSpacing.md))
        
        // Info grid
        ContextInfoGrid(
            mcpStatus = mcpStatus,
            isMcpOnline = isMcpOnline,
            model = model,
            latency = latency,
            port = port
        )
        
        Spacer(modifier = Modifier.height(TerminalSpacing.md))
        
        // Context window bar
        ContextWindowBar(
            usedTokens = usedTokens,
            maxTokens = maxTokens
        )
    }
}
