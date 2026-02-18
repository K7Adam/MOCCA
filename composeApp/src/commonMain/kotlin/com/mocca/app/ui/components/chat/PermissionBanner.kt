package com.mocca.app.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.PermissionRequest
import com.mocca.app.ui.components.glass.glassFloating
import com.mocca.app.ui.components.glass.GlassDefaults
import com.mocca.app.ui.components.modern.MoccaCompactButton
import com.mocca.app.ui.components.modern.MoccaTextButton
import com.mocca.app.ui.theme.*

/**
 * Sticky, glass-morphic permission banner with amber highlight.
 * 
 * Features:
 * - Positioned at top of chat content (sticky)
 * - Glass-morphic styling with amber/warning accent
 * - Collapsible to show just the permission type when not focused
 * - Prominent APPROVE/DENY actions
 * 
 * @param permission The permission request to display
 * @param onApprove Callback when approve button is clicked
 * @param onDeny Callback when deny button is clicked
 * @param modifier Modifier for styling
 */
@Composable
fun PermissionBanner(
    permission: PermissionRequest,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    // Amber/warning colors for permission highlight
    val amberAccent = Color(0xFFFFB300)  // Amber
    val amberBg = Color(0x33FFB300)      // Amber with transparency
    
    AnimatedVisibility(
        visible = true,
        enter = expandVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ),
        exit = shrinkVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ),
        modifier = modifier
    ) {
        // Glass-morphic container with amber border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPaddingHorizontal, vertical = AppSpacing.xs)
                .glassFloating(
                    shape = AppShapes.medium,
                    tokens = GlassDefaults.tokens(),
                    reducedTransparency = true
                )
                .border(
                    width = 1.5.dp,
                    color = amberAccent.copy(alpha = 0.6f),
                    shape = AppShapes.medium
                )
                .clip(AppShapes.medium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(amberBg.copy(alpha = 0.15f))
            ) {
                // ═══════════════ HEADER (Always visible) ═══════════════
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isExpanded = !isExpanded }
                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Warning icon + permission type
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Pulsing warning indicator
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(amberAccent.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = amberAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        
                        // Permission type
                        Text(
                            text = permission.permission.uppercase(),
                            style = AppTypography.labelSmall,
                            color = AppColors.white,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Right: Expand/collapse icon
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = amberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // ═══════════════ EXPANDED CONTENT ═══════════════
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ),
                    exit = shrinkVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.sm)
                            .padding(bottom = AppSpacing.sm)
                    ) {
                        // Subtle divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(amberAccent.copy(alpha = 0.2f))
                        )
                        
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        
                        // Pattern preview (if available)
                        if (permission.patterns.isNotEmpty()) {
                            Text(
                                text = "// TARGETS",
                                style = AppTypography.labelExtraSmall,
                                color = AppColors.textTertiary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            Text(
                                text = permission.patterns.take(3).joinToString("\n"),
                                style = AppTypography.labelSmall,
                                color = AppColors.textSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            if (permission.patterns.size > 3) {
                                Text(
                                    text = "+ ${permission.patterns.size - 3} more",
                                    style = AppTypography.labelExtraSmall,
                                    color = AppColors.textTertiary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                        }
                        
                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // APPROVE button (primary)
                            MoccaCompactButton(
                                text = "ALLOW",
                                onClick = onApprove,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // DENY button (secondary)
                            MoccaTextButton(
                                text = "DENY",
                                onClick = onDeny,
                                textColor = AppColors.error
                            )
                        }
                    }
                }
            }
        }
    }
}
