package com.mocca.app.ui.screens.mcp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.McpConnectionStatus
import com.mocca.app.domain.model.McpServerInfo
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
internal fun McpServerCard(
    server: McpServerInfo,
    isOperationInProgress: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val statusColor = when {
        server.status.isConnected -> AppColors.statusOnline
        server.status.needsAuth -> AppColors.statusWaiting
        server.status.hasFailed -> AppColors.error
        else -> AppColors.grey
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceVariant.copy(alpha = 0.5f), AppShapes.card)
            .border(AppSpacing.borderThin, statusColor.copy(alpha = 0.3f), AppShapes.card)
            .clickable(onClick = onClick)
            .padding(AppSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Code-like syntax for the server name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "{ ",
                    color = AppColors.grey,
                    style = AppTypography.code
                )
                Text(
                    text = "\"${server.name}\"",
                    color = AppColors.syntaxString,
                    style = AppTypography.code,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = " }",
                    color = AppColors.grey,
                    style = AppTypography.code
                )
            }
            
            // Toggle or loading
            if (isOperationInProgress) {
                CircularProgressIndicator(
                    color = AppColors.statusWaiting,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                ModernToggle(
                    checked = server.isConnected,
                    onCheckedChange = onToggle
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        // Metadata Row (JSON-style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg)
        ) {
            McpMetaField(label = "type", value = server.displayType, color = AppColors.syntaxKeyword)
            if (server.toolCount > 0) {
                McpMetaField(label = "tools", value = server.toolCount.toString(), color = AppColors.syntaxFunction)
            }
            McpMetaField(label = "status", value = getStatusText(server.status.status), color = statusColor)
        }
        
        // Error message if any
        server.status.error?.let { error ->
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.error.copy(alpha = 0.05f))
                    .padding(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "!! error: ",
                    color = AppColors.error,
                    style = AppTypography.codeSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "\"$error\"",
                    color = AppColors.error.copy(alpha = 0.8f),
                    style = AppTypography.codeSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun McpMetaField(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            color = AppColors.grey,
            style = AppTypography.codeSmall
        )
        Text(
            text = value,
            color = color,
            style = AppTypography.codeSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun McpErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.error.copy(alpha = 0.1f))
            .border(AppSpacing.borderThin, AppColors.error, AppShapes.medium)
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = AppColors.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        Text(
            text = message,
            color = AppColors.error,
            style = AppTypography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = AppColors.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Get human-readable status text.
 */
internal fun getStatusText(status: McpConnectionStatus): String {
    return when (status) {
        McpConnectionStatus.CONNECTED -> "CONNECTED"
        McpConnectionStatus.DISCONNECTED -> "OFFLINE"
        McpConnectionStatus.CONNECTING -> "CONNECTING"
        McpConnectionStatus.DISCONNECTING -> "DISCONNECTING"
        McpConnectionStatus.FAILED -> "FAILED"
        McpConnectionStatus.NEEDS_AUTH -> "AUTH_REQUIRED"
        McpConnectionStatus.NEEDS_CLIENT_REGISTRATION -> "REGISTRATION_REQUIRED"
        McpConnectionStatus.DISABLED -> "DISABLED"
    }
}
