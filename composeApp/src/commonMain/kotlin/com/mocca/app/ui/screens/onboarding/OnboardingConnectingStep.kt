package com.mocca.app.ui.screens.onboarding

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Connecting step — staged progress visualization with auto-navigation on success.
 */

@Composable
internal fun OnboardingConnectingStep(
    connectionStage: ConnectionStage,
    connectionProgress: String,
    error: String?,
    isSuccess: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val successScale by animateFloatAsState(
        targetValue = if (connectionStage == ConnectionStage.CONNECTED) 1f else 0.5f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "successScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.screenPaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.2f))

        if (connectionStage == ConnectionStage.CONNECTED) {

            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Connected",
                tint = AppColors.success,
                modifier = Modifier
                    .size(80.dp)
                    .scale(successScale)
            )

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            Text(
                text = "Connected!",
                style = AppTypography.headlineMedium,
                color = AppColors.success,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Text(
                text = "Launching MOCCA...",
                style = AppTypography.bodyMedium,
                color = AppColors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

        } else if (connectionStage == ConnectionStage.FAILED) {

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Failed",
                tint = AppColors.error,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            Text(
                text = "Connection Failed",
                style = AppTypography.headlineMedium,
                color = AppColors.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Text(
                    text = error,
                    style = AppTypography.bodyMedium,
                    color = AppColors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            MoccaButton(
                text = "Try Again",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            MoccaOutlinedButton(
                text = "Go Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(0.6f)
            )

        } else {

            LoadingIndicator(
                modifier = Modifier.size(48.dp),
                color = AppColors.accent
            )

            Spacer(modifier = Modifier.height(AppSpacing.xxl))

            Text(
                text = "Connecting...",
                style = AppTypography.headlineMedium,
                color = AppColors.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppSpacing.xxxl))

            // Staged progress items
            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
            ) {
                StageItem(
                    label = "Saving configuration...",
                    isComplete = connectionStage.ordinal > ConnectionStage.SAVING_CONFIG.ordinal,
                    isActive = connectionStage == ConnectionStage.SAVING_CONFIG,
                    index = 0
                )
                StageItem(
                    label = "Resolving server...",
                    isComplete = connectionStage.ordinal > ConnectionStage.RESOLVING_SERVER.ordinal,
                    isActive = connectionStage == ConnectionStage.RESOLVING_SERVER,
                    index = 1
                )
                StageItem(
                    label = "Authenticating...",
                    isComplete = connectionStage.ordinal > ConnectionStage.AUTHENTICATING.ordinal,
                    isActive = connectionStage == ConnectionStage.AUTHENTICATING,
                    index = 2
                )
                StageItem(
                    label = "Testing API connection...",
                    isComplete = connectionStage.ordinal > ConnectionStage.TESTING_API.ordinal,
                    isActive = connectionStage == ConnectionStage.TESTING_API,
                    index = 3
                )
                StageItem(
                    label = "Importing server config...",
                    isComplete = connectionStage.ordinal > ConnectionStage.IMPORTING_CONFIG.ordinal,
                    isActive = connectionStage == ConnectionStage.IMPORTING_CONFIG,
                    index = 4
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xxl))

            MoccaOutlinedButton(
                text = "Cancel",
                onClick = onBack
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Individual stage item in the connection progress.
 */

@Composable
private fun StageItem(
    label: String,
    isComplete: Boolean,
    isActive: Boolean,
    index: Int
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) +
                slideInVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    initialOffsetY = { it / 4 }
                )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            when {
                isComplete -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = AppColors.success,
                        modifier = Modifier.size(20.dp)
                    )
                }
                isActive -> {
                    LoadingIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AppColors.accent
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.size(20.dp))
                }
            }

            Text(
                text = label,
                style = AppTypography.bodyMedium,
                color = when {
                    isComplete -> AppColors.success
                    isActive -> AppColors.onSurface
                    else -> AppColors.outline
                },
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.alpha(
                    when {
                        isComplete -> 0.7f
                        isActive -> 1f
                        else -> 0.4f
                    }
                )
            )
        }
    }
}

