package com.mocca.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mocca.app.ui.components.modern.MoccaTextButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
internal fun RevertedSessionBanner(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .padding(horizontal = AppSpacing.screenPaddingHorizontal, vertical = AppSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(AppColors.surfaceVariant)
                .padding(AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History, 
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Text(
                        text = "VIEWING OLDER VERSION",
                        style = AppTypography.labelSmall,
                        color = AppColors.textSecondary
                    )
                }
                MoccaTextButton(
                    text = "RESUME LATEST",
                    onClick = onResume,
                    textColor = AppColors.accent
                )
            }
        }
    }
}

@Composable
internal fun TerminalErrorOverlay(
    error: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(2f)
            .padding(horizontal = AppSpacing.screenPaddingHorizontal, vertical = AppSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(AppColors.error.copy(alpha = 0.9f))
                .padding(AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ERROR: $error",
                    color = AppColors.white,
                    style = AppTypography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = AppColors.white)
                }
            }
        }
    }
}

@Composable
internal fun SessionDisposedBanner(
    reason: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.warning.copy(alpha = 0.15f))
            .border(AppSpacing.borderThin, AppColors.warning.copy(alpha = 0.4f))
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = AppColors.warning,
                modifier = Modifier.size(14.dp)
            )
            Column {
                Text(
                    text = "SESSION DISPOSED",
                    style = AppTypography.labelSmall,
                    color = AppColors.warning,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = reason,
                    style = AppTypography.labelExtraSmall,
                    color = AppColors.textSecondary
                )
            }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = AppColors.textTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
