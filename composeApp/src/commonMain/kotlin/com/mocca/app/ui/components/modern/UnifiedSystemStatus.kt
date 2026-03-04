package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
 * Unified system status indicator showing both OpenCode Agent and Git Server health.
 * 
 * This abstracts the two-server architecture into a single user-friendly status.
 */
enum class UnifiedSystemStatus {
    FULLY_OPERATIONAL,  // Both servers connected and healthy
    AGENT_ONLY,         // Main agent works, git unavailable
    CONNECTING,         // Attempting to connect
    DEGRADED,          // Connected but slow/errors
    OFFLINE            // No connection
}

/**
 * Compact inline status indicator for top bars and headers.
 */
@Composable
fun UnifiedStatusIndicator(
    status: UnifiedSystemStatus,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when (status) {
        UnifiedSystemStatus.FULLY_OPERATIONAL -> 
            Triple(Icons.Default.CheckCircle, AppColors.statusOnline, "Ready")
        UnifiedSystemStatus.AGENT_ONLY -> 
            Triple(Icons.Default.Warning, AppColors.statusWaiting, "Git Unavailable")
        UnifiedSystemStatus.CONNECTING -> 
            Triple(Icons.Default.Refresh, AppColors.accent, "Connecting...")
        UnifiedSystemStatus.DEGRADED -> 
            Triple(Icons.Default.Warning, AppColors.statusWaiting, "Degraded")
        UnifiedSystemStatus.OFFLINE -> 
            Triple(Icons.Default.Error, AppColors.statusOffline, "Offline")
    }
    
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label.uppercase(),
            style = AppTypography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Detailed system status card for dashboard or settings.
 */
@Composable
fun SystemStatusCard(
    agentStatus: ServerComponentStatus,
    gitStatus: ServerComponentStatus,
    onRefresh: () -> Unit,
    onTroubleshoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unifiedStatus = calculateUnifiedStatus(agentStatus, gitStatus)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.surfaceVariant, AppShapes.card)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.card)
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        // Header with overall status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM STATUS",
                style = AppTypography.labelSmall,
                color = AppColors.textSecondary
            )
            
            UnifiedStatusIndicator(status = unifiedStatus)
        }
        
        // Individual component statuses
        ComponentStatusRow(
            name = "OpenCode Agent",
            status = agentStatus,
            icon = "🤖"
        )
        
        ComponentStatusRow(
            name = "Git Services",
            status = gitStatus,
            icon = "📦"
        )
        
        // Action buttons
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
                text = "REFRESH",
                onClick = onRefresh,
                textColor = AppColors.accent
            )
        }
    }
}

@Composable
private fun ComponentStatusRow(
    name: String,
    status: ServerComponentStatus,
    icon: String
) {
    val (indicatorColor, statusText) = when (status) {
        is ServerComponentStatus.Connected -> AppColors.statusOnline to "Connected"
        is ServerComponentStatus.Connecting -> AppColors.accent to "Connecting..."
        is ServerComponentStatus.Error -> AppColors.statusOffline to status.message
        is ServerComponentStatus.Unknown -> AppColors.grey to "Unknown"
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Text(
            text = icon,
            style = AppTypography.bodyMedium
        )
        
        Text(
            text = name,
            style = AppTypography.bodyMedium,
            color = AppColors.white,
            modifier = Modifier.weight(1f)
        )
        
        // Status dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(indicatorColor, AppShapes.circle)
        )
        
        Text(
            text = statusText,
            style = AppTypography.bodySmall,
            color = indicatorColor
        )
    }
}

/**
 * Sealed class representing component status
 */
sealed class ServerComponentStatus {
    data object Unknown : ServerComponentStatus()
    data object Connecting : ServerComponentStatus()
    data class Connected(val latencyMs: Long? = null) : ServerComponentStatus()
    data class Error(val message: String) : ServerComponentStatus()
}

/**
 * Calculate unified status from individual component statuses
 */
fun calculateUnifiedStatus(
    agentStatus: ServerComponentStatus,
    gitStatus: ServerComponentStatus
): UnifiedSystemStatus {
    return when {
        agentStatus is ServerComponentStatus.Connecting || 
        gitStatus is ServerComponentStatus.Connecting -> 
            UnifiedSystemStatus.CONNECTING
        
        agentStatus is ServerComponentStatus.Connected && 
        gitStatus is ServerComponentStatus.Connected -> 
            UnifiedSystemStatus.FULLY_OPERATIONAL
        
        agentStatus is ServerComponentStatus.Connected && 
        gitStatus is ServerComponentStatus.Error -> 
            UnifiedSystemStatus.AGENT_ONLY
        
        agentStatus is ServerComponentStatus.Error -> 
            UnifiedSystemStatus.OFFLINE
        
        agentStatus is ServerComponentStatus.Connected -> 
            UnifiedSystemStatus.DEGRADED
        
        else -> UnifiedSystemStatus.OFFLINE
    }
}
