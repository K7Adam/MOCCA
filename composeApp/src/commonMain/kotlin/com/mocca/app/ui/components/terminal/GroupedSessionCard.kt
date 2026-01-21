package com.mocca.app.ui.components.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mocca.app.domain.model.Session
import com.mocca.app.domain.model.SessionGroup
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalShapes
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore

// ═══════════════════════════════════════════════════════════════════════════════
// RUNNING SESSION INDICATOR
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Pulsing indicator for running/active sessions.
 * Shows a mint green dot with "LIVE" or "PROCESSING" label.
 */
@Composable
fun RunningSessionIndicator(
    isRunning: Boolean,
    statusLabel: String = "LIVE",
    modifier: Modifier = Modifier
) {
    if (!isRunning) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Pulsing dot with outer glow effect
        Box(contentAlignment = Alignment.Center) {
            // Glow ring
            Box(
                modifier = Modifier
                    .size((8 * pulseScale).dp)
                    .alpha(pulseAlpha * 0.5f)
                    .background(TerminalColors.accentGreen, CircleShape)
            )
            // Core dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(TerminalColors.accentGreen, CircleShape)
            )
        }
        
        // Status label
        Text(
            text = statusLabel,
            style = TerminalTypography.labelSmall,
            color = TerminalColors.accentGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STACKED CHILD PREVIEW
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Visual stack of child session cards behind parent.
 * Shows 1-3 stacked cards with decreasing offset.
 */
@Composable
private fun StackedChildPreview(
    childCount: Int,
    hasRunningChild: Boolean,
    modifier: Modifier = Modifier
) {
    if (childCount == 0) return
    
    val visibleStacks = minOf(childCount, 3)
    
    Box(modifier = modifier) {
        // Render stacks from back to front
        for (i in (visibleStacks - 1) downTo 0) {
            val offsetX = ((visibleStacks - 1 - i) * 4).dp
            val offsetY = ((visibleStacks - 1 - i) * 3).dp
            val alpha = 0.3f + (i * 0.2f)
            
            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .zIndex(i.toFloat())
                    .size(width = 40.dp, height = 24.dp)
                    .alpha(alpha)
                    .clip(TerminalShapes.small)
                    .background(
                        if (hasRunningChild && i == visibleStacks - 1) {
                            TerminalColors.accentGreen.copy(alpha = 0.2f)
                        } else {
                            TerminalColors.surfaceContainer
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (hasRunningChild && i == visibleStacks - 1) {
                            TerminalColors.accentGreen.copy(alpha = 0.5f)
                        } else {
                            TerminalColors.border
                        },
                        shape = TerminalShapes.small
                    )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// GROUPED SESSION CARD
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Card component for displaying a session group with parent-child hierarchy.
 * 
 * Features:
 * - Stacked card visualization for child sessions
 * - Pulsing indicator for running sessions
 * - Expand/collapse for viewing children
 * - Active indicator bar
 */
@Composable
fun GroupedSessionCard(
    group: SessionGroup,
    isActive: Boolean,
    isRunning: Boolean,
    onSessionClick: (Session) -> Unit,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasChildren = group.children.isNotEmpty()
    val hasRunningChild = group.runningSessions.any { it.id != group.parent.id }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Parent card with stacked preview
        Box(modifier = Modifier.fillMaxWidth()) {
            // Stacked child preview (behind parent)
            if (hasChildren && !group.isExpanded) {
                StackedChildPreview(
                    childCount = group.children.size,
                    hasRunningChild = hasRunningChild,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 8.dp)
                )
            }
            
            // Parent session card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TerminalShapes.sessionCard)
                    .background(TerminalColors.surfaceContainer, TerminalShapes.sessionCard)
                    .then(
                        if (isActive) {
                            Modifier.drawBehind {
                                drawRect(
                                    color = TerminalColors.accentGreen,
                                    topLeft = Offset(0f, 0f),
                                    size = androidx.compose.ui.geometry.Size(
                                        TerminalSpacing.activeIndicatorWidth.toPx(),
                                        size.height
                                    )
                                )
                            }
                        } else Modifier
                    )
                    .then(
                        if (isRunning) {
                            Modifier.border(
                                width = 1.dp,
                                color = TerminalColors.accentGreen.copy(alpha = 0.5f),
                                shape = TerminalShapes.sessionCard
                            )
                        } else {
                            Modifier.border(
                                width = TerminalSpacing.borderThin,
                                color = TerminalColors.border,
                                shape = TerminalShapes.sessionCard
                            )
                        }
                    )
                    .clickable { onSessionClick(group.parent) }
                    .padding(TerminalSpacing.cardPadding)
            ) {
                // Header row: Title + Running indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.parent.title ?: "Untitled Session",
                        style = TerminalTypography.titleSmall,
                        color = TerminalColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (isRunning) {
                        Spacer(modifier = Modifier.width(8.dp))
                        RunningSessionIndicator(
                            isRunning = true,
                            statusLabel = "LIVE"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Footer: Child count + Expand toggle
                if (hasChildren) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExpandToggle() }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${group.children.size} sub-session${if (group.children.size > 1) "s" else ""}",
                                style = TerminalTypography.labelSmall,
                                color = TerminalColors.textTertiary
                            )
                            
                            if (hasRunningChild) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(TerminalColors.accentGreen, CircleShape)
                                )
                            }
                        }
                        
                        Icon(
                            imageVector = if (group.isExpanded) Icons.Filled.ExpandLess
                                else Icons.Filled.ExpandMore,
                            contentDescription = if (group.isExpanded) "Collapse" else "Expand",
                            tint = TerminalColors.textTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        // Expanded children list
        AnimatedVisibility(
            visible = group.isExpanded && hasChildren,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                group.children.forEach { child ->
                    val isChildRunning = group.runningSessions.any { it.id == child.id }
                    
                    ChildSessionCard(
                        session = child,
                        isRunning = isChildRunning,
                        onClick = { onSessionClick(child) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CHILD SESSION CARD
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Compact card for child/sub sessions.
 */
@Composable
private fun ChildSessionCard(
    session: Session,
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TerminalShapes.medium)
            .background(
                if (isRunning) TerminalColors.accentGreen.copy(alpha = 0.1f)
                else TerminalColors.surfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (isRunning) TerminalColors.accentGreen.copy(alpha = 0.3f)
                else TerminalColors.border,
                shape = TerminalShapes.medium
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title (with sub-agent prefix stripped)
        val displayTitle = session.title?.let { title ->
            when {
                title.startsWith("Background:") -> title.removePrefix("Background:").trim()
                title.startsWith("look_at:") -> title.removePrefix("look_at:").trim()
                else -> title
            }
        } ?: "Sub-session"
        
        Text(
            text = displayTitle,
            style = TerminalTypography.bodySmall,
            color = TerminalColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        if (isRunning) {
            Spacer(modifier = Modifier.width(8.dp))
            RunningSessionIndicator(
                isRunning = true,
                statusLabel = "ACTIVE"
            )
        }
    }
}
