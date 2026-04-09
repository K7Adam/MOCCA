package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Data class representing a diagnostic step result
 */
data class DiagnosticStep(
    val name: String,
    val status: DiagnosticStatus,
    val message: String,
    val helpText: String? = null
)

enum class DiagnosticStatus {
    PASS, FAIL, WARNING, CHECKING
}

/**
 * Connection troubleshooting card showing diagnostic steps and actionable fixes.
 */
@Composable
fun ConnectionTroubleshootingCard(
    connectionStatus: String,
    diagnosticSteps: List<DiagnosticStep>,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.surfaceVariant, AppShapes.card)
            .border(AppSpacing.borderThin, AppColors.outline, AppShapes.card)
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        // Header
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
                text = "Connection Troubleshooting",
                style = AppTypography.labelMedium,
                color = AppColors.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        // Status summary
        Text(
            text = "Status: $connectionStatus",
            style = AppTypography.bodySmall,
            color = AppColors.onSurfaceVariant
        )

        // Diagnostic steps
        Column(
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            diagnosticSteps.forEach { step ->
                DiagnosticStepRow(step)
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            MoccaOutlinedButton(
                text = "Check Settings",
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f),
                height = AppSpacing.buttonHeightCompact
            )

            MoccaButton(
                text = "Retry Connection",
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                height = AppSpacing.buttonHeightCompact,
                icon = Icons.Default.Refresh
            )
        }
    }
}

@Composable
private fun DiagnosticStepRow(step: DiagnosticStep) {
    val (icon, color) = when (step.status) {
        DiagnosticStatus.PASS -> Icons.Default.CheckCircle to AppColors.statusOnline
        DiagnosticStatus.FAIL -> Icons.Default.Error to AppColors.statusOffline
        DiagnosticStatus.WARNING -> Icons.Default.Info to AppColors.statusWaiting
        DiagnosticStatus.CHECKING -> Icons.Default.Refresh to AppColors.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = step.name,
                style = AppTypography.bodySmall,
                color = AppColors.onSurface,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = step.message,
                style = AppTypography.bodySmall,
                color = color
            )

            step.helpText?.let { help ->
                Text(
                    text = help,
                    style = AppTypography.labelSmall,
                    color = AppColors.outline
                )
            }
        }
    }
}

/**
 * Simplified connection help card for inline display in error states.
 */
@Composable
fun ConnectionHelpInline(
    errorMessage: String,
    onTroubleshoot: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.errorDim, AppShapes.small)
            .border(AppSpacing.borderThin, AppColors.error, AppShapes.small)
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = AppColors.error
            )
            Text(
                text = "Connection Problem",
                style = AppTypography.labelMedium,
                color = AppColors.error,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = errorMessage,
            style = AppTypography.bodySmall,
            color = AppColors.onSurfaceDim
        )

        Text(
            text = "Common causes:\n" +
                    "• OpenCode server is not running on your computer\n" +
                    "• Device and computer are on different networks\n" +
                    "• Firewall blocking port 4242",
            style = AppTypography.bodySmall,
            color = AppColors.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            MoccaTextButton(
                text = "TROUBLESHOOT",
                onClick = onTroubleshoot
            )

            Spacer(modifier = Modifier.weight(1f))

            MoccaTextButton(
                text = "RETRY",
                onClick = onRetry,
                textColor = AppColors.primary
            )
        }
    }
}
