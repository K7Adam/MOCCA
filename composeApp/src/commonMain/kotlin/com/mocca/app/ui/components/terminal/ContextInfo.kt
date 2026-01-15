package com.mocca.app.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import androidx.compose.material3.MaterialTheme

/**
 * Context info components for the left panel.
 * Displays MCP status, model info, latency, port in a 2x2 grid.
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
            .border(TerminalSpacing.borderThin, TerminalColors.borderLight, RectangleShape)
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
                    text = "MCP_STATUS",
                    color = TerminalColors.grey,
                    style = MaterialTheme.typography.labelSmall
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
                ) {
                    StatusSquare(
                        color = if (isMcpOnline) TerminalColors.statusOnline else TerminalColors.statusOffline
                    )
                    Text(
                        text = mcpStatus.uppercase(),
                        color = TerminalColors.white,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // MODEL
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "MODEL",
                    color = TerminalColors.grey,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = model.uppercase(),
                    color = TerminalColors.white,
                    style = MaterialTheme.typography.bodyMedium,
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
                    color = TerminalColors.grey,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = latency.uppercase(),
                    color = TerminalColors.white,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // PORT
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "PORT",
                    color = TerminalColors.grey,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = port.uppercase(),
                    color = TerminalColors.white,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONTEXT WINDOW BAR (usage progress bar with hatched fill)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Context window usage bar with hatched/striped fill.
 */
@Composable
fun ContextWindowBar(
    usedTokens: Int,
    maxTokens: Int,
    modifier: Modifier = Modifier,
    barHeight: Dp = 12.dp,
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
                    text = "CTX_WINDOW",
                    color = TerminalColors.grey,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "${formatTokenCount(usedTokens)}/${formatTokenCount(maxTokens)}",
                    color = TerminalColors.white,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            Spacer(modifier = Modifier.height(TerminalSpacing.xs))
        }
        
        // Progress bar with hatched fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .border(TerminalSpacing.borderThin, TerminalColors.borderLight, RectangleShape)
                .drawBehind {
                    // Draw hatched fill for used portion
                    val usedWidth = size.width * usagePercent
                    
                    // Draw diagonal lines (hatched pattern)
                    val lineSpacing = 6.dp.toPx()
                    val lineWidth = 2.dp.toPx()
                    
                    for (x in 0..(usedWidth.toInt() + size.height.toInt()) step lineSpacing.toInt()) {
                        val startX = x.toFloat().coerceAtMost(usedWidth)
                        val endX = (x.toFloat() - size.height).coerceIn(0f, usedWidth)
                        
                        if (startX >= 0 && endX <= usedWidth) {
                            drawLine(
                                color = TerminalColors.white,
                                start = Offset(endX, size.height),
                                end = Offset(startX.coerceAtMost(usedWidth), 0f),
                                strokeWidth = lineWidth
                            )
                        }
                    }
                }
        )
        
        // Usage percent label
        if (showLabels) {
            Spacer(modifier = Modifier.height(TerminalSpacing.xxs))
            Text(
                text = "$usagePercentInt% USED",
                color = TerminalColors.grey,
                style = MaterialTheme.typography.labelSmall,
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
            TerminalHeader(
                text = "CONTEXT_INFO",
                showBrackets = true
            )
            StatusDot(
                color = if (isMcpOnline) TerminalColors.statusOnline else TerminalColors.statusOffline
            )
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
        
        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
        
        // Divider inside container
        HorizontalDivider(
            thickness = TerminalSpacing.borderThin,
            color = TerminalColors.border,
            modifier = Modifier.padding(horizontal = TerminalSpacing.cardPadding)
        )
        
        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
        
        // Context window bar
        Box(
            modifier = Modifier.padding(horizontal = TerminalSpacing.cardPadding)
        ) {
            ContextWindowBar(
                usedTokens = usedTokens,
                maxTokens = maxTokens
            )
        }
    }
}
