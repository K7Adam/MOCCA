package com.mocca.app.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.AgentErrorClassifier
import com.mocca.app.ui.components.modern.MoccaTextButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.moccaClickable

/**
 * Dismissible in-app error banner for agent/session errors.
 *
 * Shows a categorized, user-friendly error message with an optional actionable hint.
 * Uses Material 3 Expressive tokens (AppColors, AppShapes, AppTypography) and
 * animated enter/exit transitions.
 *
 * Placement: top of the chat message pane, above the message list.
 */
@Composable
fun ChatErrorBanner(
    error: AgentErrorClassifier.AgentError,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (error.severity) {
        AgentErrorClassifier.ErrorSeverity.CRITICAL -> AppColors.errorContainer
        AgentErrorClassifier.ErrorSeverity.WARNING -> AppColors.error.copy(alpha = 0.12f)
        AgentErrorClassifier.ErrorSeverity.INFO -> AppColors.surfaceContainerHigh
    }
    val accentColor = when (error.severity) {
        AgentErrorClassifier.ErrorSeverity.CRITICAL -> AppColors.error
        AgentErrorClassifier.ErrorSeverity.WARNING -> AppColors.statusWarning
        AgentErrorClassifier.ErrorSeverity.INFO -> AppColors.primary
    }
    val onContainerColor = when (error.severity) {
        AgentErrorClassifier.ErrorSeverity.CRITICAL -> AppColors.onErrorContainer
        AgentErrorClassifier.ErrorSeverity.WARNING -> AppColors.onSurface
        AgentErrorClassifier.ErrorSeverity.INFO -> AppColors.onSurface
    }

    AnimatedVisibility(
        visible = true,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacing.screenPaddingHorizontal,
                    vertical = AppSpacing.sm,
                )
                .clip(AppShapes.medium)
                .background(containerColor)
                .border(1.dp, accentColor.copy(alpha = 0.3f), AppShapes.medium)
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    // Severity icon
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when (error.severity) {
                                AgentErrorClassifier.ErrorSeverity.CRITICAL -> Icons.Filled.ErrorOutline
                                AgentErrorClassifier.ErrorSeverity.WARNING -> Icons.Filled.WarningAmber
                                AgentErrorClassifier.ErrorSeverity.INFO -> Icons.Filled.Info
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp),
                        )
                    }

                    // Title + message
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = error.title,
                            style = AppTypography.labelMedium,
                            color = accentColor,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = error.message,
                            style = AppTypography.bodySmall,
                            color = onContainerColor,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 3,
                        )
                        if (!error.hint.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Text(
                                text = error.hint,
                                style = AppTypography.labelSmall,
                                color = onContainerColor.copy(alpha = 0.7f),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                            )
                        }
                    }

                    // Dismiss button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .moccaClickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = onContainerColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                // Optional retry action
                if (onRetry != null) {
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MoccaTextButton(
                            text = "Retry",
                            onClick = onRetry,
                        )
                    }
                }
            }
        }
    }
}
