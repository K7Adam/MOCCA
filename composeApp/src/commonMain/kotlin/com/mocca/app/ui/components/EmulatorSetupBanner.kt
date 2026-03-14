package com.mocca.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
 * Banner shown when running on Android emulator to warn about ADB reverse requirement.
 * Dismissible but important for Git functionality to work.
 */
@Composable
fun EmulatorSetupBanner(
    onDismiss: () -> Unit,
    onShowHelp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.statusWaiting.copy(alpha = 0.15f), AppShapes.medium)
            .border(AppSpacing.borderThin, AppColors.statusWaiting, AppShapes.medium)
            .clickable { onShowHelp() }
            .padding(AppSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AppColors.statusWaiting,
                modifier = Modifier.size(24.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Android Emulator Detected",
                    color = AppColors.textPrimary,
                    style = AppTypography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                
                Text(
                    text = "Git features require ADB port forwarding. Tap for setup instructions.",
                    color = AppColors.textSecondary,
                    style = AppTypography.bodySmall
                )
            }
            
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = AppColors.textSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onDismiss() }
            )
        }
    }
}
