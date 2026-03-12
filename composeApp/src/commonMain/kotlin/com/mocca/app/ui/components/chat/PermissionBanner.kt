package com.mocca.app.ui.components.chat

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.*
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.PermissionRequest
import com.mocca.app.ui.components.modern.MoccaCompactButton
import com.mocca.app.ui.components.modern.MoccaTextButton
import com.mocca.app.ui.theme.*
import kotlinx.coroutines.delay

private const val AUTO_APPROVE_TTL_SECONDS = 30

/**
 * Surface styling with amber/warning accent
 * - Prominent ALLOW / ALWAYS / DENY actions
 * - Auto-approves after [AUTO_APPROVE_TTL_SECONDS] seconds with animated countdown ring
 *
 * @param permission      The permission request to display
 * @param onApprove       Callback when ALLOW button is clicked (one-time approval)
 * @param onApproveAlways Callback when ALWAYS button is clicked (persistent approval)
 * @param onDeny          Callback when DENY button is clicked
 * @param modifier        Modifier for styling
 */

@Composable
fun PermissionBanner(
    permission: PermissionRequest,
    onApprove: () -> Unit,
    onApproveAlways: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var secondsRemaining by remember(permission.id) { mutableIntStateOf(AUTO_APPROVE_TTL_SECONDS) }
    var timerActive by remember(permission.id) { mutableStateOf(true) }

    // Amber/warning colors for permission highlight
    val amberAccent = AppColors.warning
    val amberBg = AppColors.warning.copy(alpha = 0.2f)

    // Countdown: auto-approve after TTL expires
    LaunchedEffect(permission.id) {
        while (secondsRemaining > 0 && timerActive) {
            delay(1000L)
            secondsRemaining--
        }
        if (timerActive && secondsRemaining == 0) {
            onApprove()
        }
    }

    // Animated progress for the countdown ring (1.0 → 0.0)
    val countdownProgress by animateFloatAsState(
        targetValue = if (timerActive) secondsRemaining.toFloat() / AUTO_APPROVE_TTL_SECONDS else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "countdown_progress"
    )

    AnimatedVisibility(
        visible = true,
        enter = expandVertically(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
        ),
        exit = shrinkVertically(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
        ),
        modifier = modifier
    ) {
        Surface(
            color = AppColors.surfaceContainer,
            shape = AppShapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPaddingHorizontal, vertical = AppSpacing.xs)
                .border(
                    width = 1.5.dp,
                    color = amberAccent.copy(alpha = 0.6f),
                    shape = AppShapes.medium
                )
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
                        // Warning indicator
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

                        // Permission type label
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

                    // Right: countdown ring + expand/collapse icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        // Countdown ring (only while timer is running)
                        if (timerActive) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(28.dp)
                            ) {
                                LoadingIndicator(
                                    modifier = Modifier.fillMaxSize(),
                                    color = amberAccent
                                )
                                Text(
                                    text = secondsRemaining.toString(),
                                    style = AppTypography.labelExtraSmall,
                                    color = amberAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = amberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // ═══════════════ EXPANDED CONTENT ═══════════════
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                    ),
                    exit = shrinkVertically(
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
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

                        // Action buttons: [ALLOW] [ALWAYS] ... [DENY]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ALLOW once (primary, stops timer)
                            MoccaCompactButton(
                                text = "ALLOW",
                                onClick = {
                                    timerActive = false
                                    onApprove()
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // ALWAYS allow (secondary accent, stops timer)
                            MoccaCompactButton(
                                text = "ALWAYS",
                                onClick = {
                                    timerActive = false
                                    onApproveAlways()
                                },
                                modifier = Modifier.weight(1f),
                                backgroundColor = amberAccent,
                                textColor = AppColors.background
                            )

                            // DENY (text button in error color, stops timer)
                            MoccaTextButton(
                                text = "DENY",
                                onClick = {
                                    timerActive = false
                                    onDeny()
                                },
                                textColor = AppColors.error
                            )
                        }
                    }
                }
            }
        }
    }
}
