package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.UpdateInfo
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    isDownloading: Boolean,
    progress: Float,
    error: String? = null,
    logs: List<String> = emptyList(),
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    
    BasicAlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp) // Constrain max height to prevent overflow
                .background(AppColors.surfaceElevated, AppShapes.dialog)
                .border(AppSpacing.borderThin, AppColors.border, AppShapes.dialog)
                .padding(AppSpacing.lg)
        ) {
            // Header (always visible, not scrollable)
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

            // Scrollable content area for release notes
            Column(
                modifier = Modifier
                    .weight(1f, fill = false) // Take available space but don't force expansion
                    .verticalScroll(rememberScrollState())
            ) {
                // Release Notes
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(
                        text = updateInfo.releaseNotes,
                        color = AppColors.textSecondary,
                        style = AppTypography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                }

                // Log Console (Visible during download or error)
                if (isDownloading || error != null || logs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(AppColors.background, AppShapes.small)
                            .border(AppSpacing.borderThin, AppColors.border, AppShapes.small)
                    ) {
                        // Log Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.surfaceVariant)
                                .padding(horizontal = AppSpacing.sm, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LOGS",
                                style = AppTypography.labelSmall,
                                color = AppColors.grey
                            )
                            
                            IconButton(
                                onClick = {
                                    // Build text to copy including logs and error
                                    val textToCopy = buildString {
                                        logs.forEach { log ->
                                            appendLine("> $log")
                                        }
                                        if (error != null) {
                                            appendLine("> ERROR: $error")
                                        }
                                    }
                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Logs",
                                    tint = AppColors.accentGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        // Log Content
                        val logScrollState = rememberScrollState()
                        // Auto-scroll to bottom when logs change
                        androidx.compose.runtime.LaunchedEffect(logs.size, error) {
                            logScrollState.animateScrollTo(logScrollState.maxValue)
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(AppSpacing.sm)
                                .verticalScroll(logScrollState)
                        ) {
                            logs.forEach { log ->
                                Text(
                                    text = "> $log",
                                    style = AppTypography.codeSmall.copy(fontSize = 10.sp),
                                    color = if (log.startsWith("ERROR") || log.startsWith("CRITICAL")) AppColors.alertRed else AppColors.textSecondary,
                                    fontFamily = AppTypography.monoFamily
                                )
                            }
                            
                            // Error Message - Displayed inside logs so it gets copied
                            if (error != null) {
                                Spacer(modifier = Modifier.height(AppSpacing.sm))
                                Text(
                                    text = "> ERROR: $error",
                                    style = AppTypography.codeSmall.copy(fontSize = 10.sp),
                                    color = AppColors.alertRed,
                                    fontFamily = AppTypography.monoFamily
                                )
                            }
                        }
                    }
                }

                // Size
                Text(
                    text = "Download size: ${updateInfo.size / 1024 / 1024} MB",
                    color = AppColors.textTertiary,
                    style = AppTypography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Buttons (always visible at bottom, not scrollable)
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
                        MoccaOutlinedButton(
                            text = "Dismiss",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            height = AppSpacing.buttonHeightCompact
                        )
                        if (onRetry != null) {
                            MoccaButton(
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
                        MoccaOutlinedButton(
                            text = "Later",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            height = AppSpacing.buttonHeightCompact
                        )
                        MoccaButton(
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
