package com.mocca.app.ui.screens.onboarding

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.moccaClickable

@Composable
internal fun SetupChecklist() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surfaceContainer, AppShapes.card)
            .border(AppSpacing.borderThin, AppColors.outlineVariant, AppShapes.card)
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppColors.accent.copy(alpha = 0.14f), AppShapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lan,
                    contentDescription = null,
                    tint = AppColors.accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Text(
                    text = "GET STARTED",
                    style = AppTypography.labelSmall,
                    color = AppColors.accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bridge-first setup for reliable agent chat",
                    style = AppTypography.bodySmall,
                    color = AppColors.onSurfaceVariant
                )
            }
        }

        ChecklistItem(
            icon = Icons.Default.Terminal,
            eyebrow = "1 minute",
            text = "Start the MOCCA CLI bridge",
            subtext = "Run mocca-cli on your workstation so the app can use OpenCode's live event stream."
        )
        
        ChecklistItem(
            icon = Icons.Default.Wifi,
            eyebrow = "Pair once",
            text = "Scan the QR code or paste the link",
            subtext = "Use the QR code or pairing link from mocca-cli to connect. Saved bridge targets reconnect later."
        )
        
        ChecklistItem(
            icon = Icons.Default.AutoAwesome,
            eyebrow = "Live state",
            text = "Chat with the agent",
            subtext = "MOCCA shows reasoning, tool runs, permissions, questions, usage, and session progress as they happen."
        )
    }
}

@Composable
internal fun ChecklistItem(
    icon: ImageVector,
    eyebrow: String,
    text: String,
    subtext: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(AppColors.primary.copy(alpha = 0.12f), AppShapes.small)
                .border(AppSpacing.borderThin, AppColors.primary.copy(alpha = 0.45f), AppShapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Text(
                text = eyebrow.uppercase(),
                style = AppTypography.labelSmall,
                color = AppColors.accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = text,
                style = AppTypography.bodyMedium,
                color = AppColors.onSurface,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = subtext,
                style = AppTypography.bodySmall,
                color = AppColors.outline
            )
        }
    }
}

@Composable
internal fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.error.copy(alpha = 0.1f), AppShapes.card)
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Error",
            tint = AppColors.error,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        
        Text(
            text = message,
            style = AppTypography.bodySmall,
            color = AppColors.error,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        
        Text(
            text = "Retry",
            style = AppTypography.labelSmall,
            color = AppColors.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.moccaClickable(onClick = onRetry, pressedScale = 0.98f)
        )
    }
}
