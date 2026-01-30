package com.mocca.app.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.UpdateInfo
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    isDownloading: Boolean,
    progress: Float,
    error: String? = null,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    BasicAlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surfaceElevated, AppShapes.dialog)
                .border(AppSpacing.borderThin, AppColors.border, AppShapes.dialog)
                .padding(AppSpacing.lg)
        ) {
            // Header
            Text(
                text = "UPDATE AVAILABLE",
                color = AppColors.accentGreen,
                style = AppTypography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Text(
                text = "Version ${updateInfo.version}",
                color = AppColors.white,
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Release Notes
            if (updateInfo.releaseNotes.isNotBlank()) {
                Text(
                    text = updateInfo.releaseNotes,
                    color = AppColors.textSecondary,
                    style = AppTypography.bodyMedium
                )
                Spacer(modifier = Modifier.height(AppSpacing.md))
            }

            // Size
            Text(
                text = "Download size: ${updateInfo.size / 1024 / 1024} MB",
                color = AppColors.textTertiary,
                style = AppTypography.labelSmall
            )

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Error Message
            if (error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.alertRedDim, AppShapes.small)
                        .padding(AppSpacing.md)
                ) {
                    Text(
                        text = "DOWNLOAD FAILED",
                        color = AppColors.alertRed,
                        style = AppTypography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = error,
                        color = AppColors.alertRed,
                        style = AppTypography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.md))
            }

            when {
                isDownloading -> {
                    // Progress Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Downloading... ${(progress * 100).toInt()}%",
                            color = AppColors.accentGreen,
                            style = AppTypography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(AppColors.greyDark, AppShapes.pill)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(AppColors.accentGreen, AppShapes.pill)
                            )
                        }
                    }
                }
                error != null -> {
                    // Error state buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        TerminalOutlinedButton(
                            text = "Dismiss",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            height = AppSpacing.buttonHeightCompact
                        )
                        if (onRetry != null) {
                            TerminalButton(
                                text = "Retry",
                                onClick = onRetry,
                                modifier = Modifier.weight(1f),
                                height = AppSpacing.buttonHeightCompact
                            )
                        }
                    }
                }
                else -> {
                    // Normal buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        TerminalOutlinedButton(
                            text = "Later",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            height = AppSpacing.buttonHeightCompact
                        )
                        TerminalButton(
                            text = "Download & Install",
                            onClick = onUpdate,
                            modifier = Modifier.weight(1f),
                            height = AppSpacing.buttonHeightCompact,
                            showArrow = true
                        )
                    }
                }
            }
        }
    }
}
