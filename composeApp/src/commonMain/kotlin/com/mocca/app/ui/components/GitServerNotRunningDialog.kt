package com.mocca.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Alert dialog for Git server not running.
 * Shows when GitApiClient detects server unavailable and offers options.
 * 
 * Enhanced UX: User-friendly messaging, auto-start capability, and clear troubleshooting.
 */
@Composable
fun GitServerNotRunningDialog(
    onDismiss: () -> Unit,
    onStartServer: () -> Unit,
    showAdbHelp: Boolean = false,
    isAttemptingStart: Boolean = false,
    attemptCount: Int = 0,
    maxAttempts: Int = 3
) {
    val navigator = LocalNavigator.current

    AlertDialog(
        onDismissRequest = { if (!isAttemptingStart) onDismiss() },
        confirmButton = {
            when {
                isAttemptingStart -> {
                    // Show progress indicator instead of button during attempt
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AppColors.accentGreen,
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Starting... (${attemptCount}/$maxAttempts)",
                            color = AppColors.accentGreen,
                            style = AppTypography.labelMedium
                        )
                    }
                }
                showAdbHelp -> {
                    // If ADB help is shown, main action is just "OK" (dismiss)
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = AppColors.accentGreen)
                    }
                }
                else -> {
                    TextButton(onClick = {
                        onStartServer()
                    }) {
                        Text("Enable Git", color = AppColors.accentGreen)
                    }
                }
            }
        },
        dismissButton = {
            if (!isAttemptingStart && !showAdbHelp) {
                TextButton(onClick = onDismiss) {
                    Text("Not Now", color = AppColors.grey)
                }
            }
        },
        title = {
            Text(
                when {
                    isAttemptingStart -> "Starting Git Services..."
                    showAdbHelp -> "Android Emulator Setup"
                    else -> "Git Features Unavailable"
                },
                fontWeight = FontWeight.Bold,
                color = AppColors.white
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                when {
                    isAttemptingStart -> {
                        // Progress indication
                        Text(
                            "We're automatically starting Git services on your OpenCode server. " +
                            "This may take a few moments...",
                            color = AppColors.whiteDim
                        )
                        
                        if (attemptCount > 1) {
                            Text(
                                "Attempt $attemptCount of $maxAttempts",
                                color = AppColors.textTertiary,
                                style = AppTypography.bodySmall
                            )
                        }
                    }
                    showAdbHelp -> {
                        // Emulator-specific help
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AppColors.accentGreen,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        Text(
                            "Android Emulator Detected",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.accentGreen
                        )
                        
                        Text(
                            "To use Git features on the emulator, you need to set up port forwarding. " +
                            "Run this command on your computer:",
                            color = AppColors.whiteDim
                        )
                        
                        // Copy-able command box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.surfaceVariant, AppShapes.small)
                                .padding(AppSpacing.md)
                        ) {
                            Text(
                                "adb reverse tcp:4097 tcp:4097",
                                color = AppColors.white,
                                style = AppTypography.codeSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Text(
                            "After running the command, tap OK and try your Git operation again.",
                            color = AppColors.textSecondary,
                            style = AppTypography.bodySmall
                        )
                    }
                    else -> {
                        // Standard message - user-friendly, no technical jargon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AppColors.statusWaiting
                            )
                            Text(
                                "Git version control is temporarily unavailable",
                                fontWeight = FontWeight.Medium,
                                color = AppColors.white
                            )
                        }
                        
                        Text(
                            "Git features (commit, push, pull, etc.) require additional services to be running. " +
                            "We can automatically enable them for you.",
                            color = AppColors.whiteDim
                        )
                        
                        // What will happen
                        Column(
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                        ) {
                            Text(
                                "✓ Start Git services on your OpenCode server",
                                color = AppColors.textSecondary,
                                style = AppTypography.bodySmall
                            )
                            Text(
                                "✓ Enable all Git operations in the app",
                                color = AppColors.textSecondary,
                                style = AppTypography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        tonalElevation = 6.dp,
        icon = {
            when {
                isAttemptingStart -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = AppColors.accentGreen,
                        strokeWidth = 2.dp
                    )
                }
                showAdbHelp -> Icon(
                    imageVector = Icons.Default.Info, 
                    contentDescription = null, 
                    tint = AppColors.accentGreen
                )
                else -> Icon(
                    imageVector = Icons.Filled.Warning, 
                    contentDescription = null, 
                    tint = AppColors.warning
                )
            }
        },
        containerColor = AppColors.surface,
        titleContentColor = AppColors.white,
        textContentColor = AppColors.whiteDim
    )
}
