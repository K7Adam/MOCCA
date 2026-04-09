@file:Suppress("DEPRECATION")

package com.mocca.app.ui.components.modern

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.moccaClickable

/**
 * Expressive floating pill that returns the user to the latest chat content.
 *
 * This is intentionally not a FAB. It behaves like contextual navigation chrome:
 * it appears only when the user is away from the latest messages, and it becomes
 * more prominent when unseen content arrives below the viewport.
 */
@Composable
fun ScrollToBottomButton(
    isVisible: Boolean,
    hasNewMessages: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val emphasisProgress by animateFloatAsState(
        targetValue = if (hasNewMessages) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "scrollToBottomEmphasis"
    )

    val containerColor = if (hasNewMessages) {
        AppColors.primaryContainer.copy(alpha = 0.94f)
    } else {
        AppColors.surfaceContainerHigh.copy(alpha = 0.96f)
    }

    val contentColor = if (hasNewMessages) {
        AppColors.onPrimaryContainer
    } else {
        AppColors.onSurface
    }

    val borderColor = if (hasNewMessages) {
        AppColors.primary.copy(alpha = 0.7f)
    } else {
        AppColors.outline.copy(alpha = 0.45f)
    }

    val labelEnterFadeSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val labelExitFadeSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            initialOffsetY = { it / 2 }
        ) + fadeIn(
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
        ) + scaleIn(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            initialScale = 0.92f
        ),
        exit = slideOutVertically(
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            targetOffsetY = { it / 3 }
        ) + fadeOut(
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
        ) + scaleOut(
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            targetScale = 0.94f
        ),
        modifier = modifier
    ) {
        Surface(
            shape = AppShapes.pill,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = if (hasNewMessages) 8.dp else 4.dp,
            shadowElevation = if (hasNewMessages) 6.dp else 2.dp,
            border = BorderStroke(AppSpacing.borderThin, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 56.dp)
                    .clip(AppShapes.pill)
                    .background(containerColor, AppShapes.pill)
                    .moccaClickable(
                        onClick = onClick,
                        interactionSource = interactionSource,
                        pressedScale = 0.96f,
                        rippleColor = AppColors.primary.copy(alpha = 0.16f)
                    )
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = "Jump to latest messages"
                        stateDescription = if (hasNewMessages) {
                            "New messages available"
                        } else {
                            "Not at latest message"
                        }
                    }
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = AppColors.primary.copy(alpha = 0.10f + (0.08f * emphasisProgress)),
                                shape = AppShapes.circle
                            )
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )

                    if (hasNewMessages) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .clip(AppShapes.circle)
                                .background(AppColors.primary)
                                .border(
                                    width = 2.dp,
                                    color = containerColor,
                                    shape = AppShapes.circle
                                )
                        )
                    }
                }

                AnimatedContent(
                    targetState = hasNewMessages,
                    transitionSpec = {
                        (
                                fadeIn(animationSpec = labelEnterFadeSpec) +
                                        slideInVertically(
                                            initialOffsetY = { it / 2 }
                                        )
                                ) togetherWith (
                                fadeOut(animationSpec = labelExitFadeSpec) +
                                        slideOutVertically(
                                            targetOffsetY = { -it / 3 }
                                        )
                                ) using SizeTransform(clip = false)
                    },
                    label = "scrollToBottomLabel"
                ) { showNewState ->
                    Text(
                        text = if (showNewState) "NEW MESSAGES" else "JUMP TO LATEST",
                        color = contentColor,
                        style = AppTypography.labelMedium
                    )
                }
            }
        }
    }
}
