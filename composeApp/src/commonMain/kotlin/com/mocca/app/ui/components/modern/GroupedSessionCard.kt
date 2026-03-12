package com.mocca.app.ui.components.modern

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope

/**
 * Card component for displaying a session group with parent-child hierarchy.
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
    
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Parent card
        Box(modifier = Modifier.fillMaxWidth()) {
            if (hasChildren && !group.isExpanded) {
                StackedChildPreview(
                    childCount = group.children.size,
                    hasRunningChild = hasRunningChild,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 8.dp)
                )
            }
            
            val parentModifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.sessionCard)
                .background(AppColors.surfaceContainerHigh, AppShapes.sessionCard)
            
            val borderModifier = if (isRunning) {
                parentModifier.border(
                    width = 1.dp,
                    color = AppColors.accentGreen,
                    shape = AppShapes.sessionCard
                )
            } else {
                parentModifier.border(
                    width = AppSpacing.borderThin,
                    color = AppColors.border.copy(alpha = 0.3f),
                    shape = AppShapes.sessionCard
                )
            }
            
            Column(
                modifier = borderModifier
                    .then(
                        if (isActive) {
                            Modifier.drawBehind {
                                drawRect(
                                    color = AppColors.accentGreen,
                                    topLeft = Offset(0f, 0f),
                                    size = androidx.compose.ui.geometry.Size(
                                        AppSpacing.activeIndicatorWidth.toPx(),
                                        size.height
                                    )
                                )
                            }
                        } else Modifier
                    )
                    .clickable { onSessionClick(group.parent) }
                    .padding(AppSpacing.cardPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val titleText = group.parent.title ?: "Untitled Session"
                    
                    val titleModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedBounds(
                                rememberSharedContentState(key = "session_title_${group.parent.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    } else {
                        Modifier
                    }

                    Text(
                        text = titleText,
                        style = AppTypography.titleSmall,
                        color = AppColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).then(titleModifier)
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
                                style = AppTypography.labelSmall,
                                color = AppColors.textTertiary
                            )
                            
                            if (hasRunningChild) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(AppColors.accentGreen, CircleShape)
                                )
                            }
                        }
                        
                        Icon(
                            imageVector = if (group.isExpanded) Icons.Filled.ExpandLess
                                else Icons.Filled.ExpandMore,
                            contentDescription = if (group.isExpanded) "Collapse" else "Expand",
                            tint = AppColors.textTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
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

/**
 * Pulsing indicator for running/active sessions.
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
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size((8 * pulseScale).dp)
                    .alpha(pulseAlpha * 0.5f)
                    .background(AppColors.accentGreen, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(AppColors.accentGreen, CircleShape)
            )
        }
        
        Text(
            text = statusLabel,
            style = AppTypography.labelSmall,
            color = AppColors.accentGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Visual stack of child session cards behind parent.
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
                    .clip(AppShapes.small)
                    .background(
                        if (hasRunningChild && i == visibleStacks - 1) {
                            AppColors.accentGreen
                        } else {
                            AppColors.surfaceContainerHigh
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (hasRunningChild && i == visibleStacks - 1) {
                            AppColors.accentGreen
                        } else {
                            AppColors.border.copy(alpha = 0.3f)
                        },
                        shape = AppShapes.small
                    )
            )
        }
    }
}

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
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(
                if (isRunning) AppColors.accentGreen.copy(alpha = 0.1f)
                else AppColors.surfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (isRunning) AppColors.accentGreen.copy(alpha = 0.3f)
                else AppColors.border,
                shape = AppShapes.medium
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayTitle = session.title?.let { title ->
            when {
                title.startsWith("Background:") -> title.removePrefix("Background:").trim()
                title.startsWith("look_at:") -> title.removePrefix("look_at:").trim()
                else -> title
            }
        } ?: "Sub-session"
        
        val titleModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    rememberSharedContentState(key = "session_title_${session.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        } else {
            Modifier
        }

        Text(
            text = displayTitle,
            style = AppTypography.bodySmall,
            color = AppColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).then(titleModifier)
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
