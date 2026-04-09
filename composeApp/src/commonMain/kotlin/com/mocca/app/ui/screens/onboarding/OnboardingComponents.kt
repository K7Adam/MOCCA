package com.mocca.app.ui.screens.onboarding

import com.mocca.app.api.NetworkConfig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.domain.model.DiscoverySource
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaInput
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
internal fun SetupChecklist() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surfaceVariant, AppShapes.card)
            .border(AppSpacing.borderThin, AppColors.outline, AppShapes.card)
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Text(
            text = "SETUP CHECKLIST",
            style = AppTypography.labelSmall,
            color = AppColors.onSurfaceVariant
        )
        
        ChecklistItem(
            number = "1",
            text = "Install OpenCode on your computer",
            subtext = "Download from github.com/opencode-ai/opencode"
        )
        
        ChecklistItem(
            number = "2",
            text = "Start the OpenCode server",
            subtext = "opencode serve --port 4242"
        )
        
        ChecklistItem(
            number = "3",
            text = "Connect from this app",
            subtext = "Auto-discover or enter server details manually"
        )
    }
}

@Composable
internal fun ChecklistItem(
    number: String,
    text: String,
    subtext: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        // Number circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(AppColors.primary.copy(alpha = 0.2f), CircleShape)
                .border(AppSpacing.borderThin, AppColors.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = AppTypography.labelMedium,
                color = AppColors.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
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
internal fun ServerListItem(
    server: DiscoveredServer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val sourceIcon = when (server.source) {
        DiscoverySource.MDNS -> Icons.Default.Wifi
        DiscoverySource.SAVED -> Icons.Default.Check
        DiscoverySource.MANUAL -> Icons.Default.SettingsEthernet
        DiscoverySource.EMULATOR_AUTO -> Icons.Default.Refresh
    }
    
    val borderColor = if (isSelected) AppColors.primary else AppColors.outline
    val backgroundColor = if (isSelected) AppColors.primary.copy(alpha = 0.1f) else AppColors.surfaceVariant
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(backgroundColor, AppShapes.card)
            .border(AppSpacing.borderThin, borderColor, AppShapes.card)
            .clickable(onClick = onClick)
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = sourceIcon,
            contentDescription = null,
            tint = if (isSelected) AppColors.primary else AppColors.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.md))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                style = AppTypography.bodyMedium,
                color = if (isSelected) AppColors.primary else AppColors.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            Text(
                text = server.baseUrl,
                style = AppTypography.bodySmall,
                color = AppColors.onSurfaceVariant
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = AppColors.primary,
                modifier = Modifier.size(20.dp)
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
            .border(AppSpacing.borderThin, AppColors.error, AppShapes.card)
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
            modifier = Modifier.clickable(onClick = onRetry)
        )
    }
}

@Composable
internal fun CredentialDialog(
    serverName: String,
    onConfirm: (username: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(NetworkConfig.DEFAULT_USERNAME) }
    var password by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceContainerHigh,
        shape = AppShapes.dialog,
        title = {
            Text(
                text = "SERVER CREDENTIALS",
                color = AppColors.onSurface,
                style = AppTypography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter credentials for $serverName",
                    style = AppTypography.bodyMedium,
                    color = AppColors.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                
                MoccaInput(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    placeholder = NetworkConfig.DEFAULT_USERNAME
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.md))
                
                MoccaInput(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "Enter server password"
                )
            }
        },
        confirmButton = {
            MoccaButton(
                text = "Connect",
                onClick = {
                    onConfirm(
                        username.trim().ifBlank { NetworkConfig.DEFAULT_USERNAME },
                        password
                    )
                },
                height = AppSpacing.buttonHeightCompact
            )
        },
        dismissButton = {
            Text(
                text = "Cancel",
                style = AppTypography.bodyMedium,
                color = AppColors.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onDismiss)
            )
        }
    )
}
