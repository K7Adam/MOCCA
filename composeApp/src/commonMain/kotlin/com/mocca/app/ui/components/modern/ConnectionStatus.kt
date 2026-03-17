package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import androidx.compose.material3.MaterialTheme

/**
 * Connection status banners and warning components.
 * Modern design: Rounded, friendly alerts.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// CONNECTION STATUS BANNER
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Full-width connection status banner.
 * Shows at top of screen when disconnected, reconnecting, etc.
 */
@Composable
fun ConnectionStatusBanner(
    status: ConnectionBannerStatus,
    modifier: Modifier = Modifier,
    onRetryClick: (() -> Unit)? = null,
    onSetupClick: (() -> Unit)? = null
) {
    val (icon, message, backgroundColor, textColor) = when (status) {
        is ConnectionBannerStatus.Disconnected -> 
            Quadruple(
                Icons.Default.WifiOff,
                status.error ?: "NO CONNECTION",
                AppColors.surfaceContainer,
                AppColors.statusOffline
            )
        is ConnectionBannerStatus.Connecting ->
            Quadruple(
                Icons.Default.Warning,
                "CONNECTING... (${status.attempt}/${status.maxAttempts})",
                AppColors.surfaceContainer,
                AppColors.statusWaiting
            )
        is ConnectionBannerStatus.WaitingForNetwork ->
            Quadruple(
                Icons.Default.WifiOff,
                "WAITING FOR NETWORK",
                AppColors.surfaceContainer,
                AppColors.statusWaiting
            )
        is ConnectionBannerStatus.Reconnecting ->
            Quadruple(
                Icons.Default.Warning,
                "RECONNECTING... (${status.attempt}/${status.maxAttempts})",
                AppColors.surfaceContainer,
                AppColors.statusWaiting
            )
        is ConnectionBannerStatus.Error ->
            Quadruple(
                Icons.Default.Warning,
                status.message,
                AppColors.surfaceContainer,
                AppColors.error
            )
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(backgroundColor, AppShapes.medium)
            .padding(
                horizontal = AppSpacing.lg,
                vertical = AppSpacing.md
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor
                )
                Text(
                    text = message.uppercase(),
                    color = textColor,
                    style = AppTypography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.padding(start = AppSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                
                if (status is ConnectionBannerStatus.Error && onSetupClick != null) {
                    MoccaTextButton(
                        text = "SETUP SERVER",
                        onClick = onSetupClick,
                        textColor = AppColors.onSurface
                    )
                } else if (onRetryClick != null) {
                    MoccaTextButton(
                        text = "RETRY",
                        onClick = onRetryClick,
                        textColor = AppColors.onSurface
                    )
                }
            }
        }
    }
}

sealed class ConnectionBannerStatus {
    data class Disconnected(val error: String? = null) : ConnectionBannerStatus()
    data class Connecting(val attempt: Int, val maxAttempts: Int) : ConnectionBannerStatus()
    data object WaitingForNetwork : ConnectionBannerStatus()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ConnectionBannerStatus()
    data class Error(val message: String) : ConnectionBannerStatus()
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

// ═══════════════════════════════════════════════════════════════════════════════
// INLINE STATUS INDICATOR
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Compact inline status indicator for top bars.
 */
@Composable
fun InlineConnectionStatus(
    isConnected: Boolean,
    serverName: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        if (!isConnected) {
            StatusDot(color = AppColors.statusOffline)
            Text(
                text = "NO SIGNAL",
                color = AppColors.statusOffline,
                style = AppTypography.labelSmall
            )
        } else {
            // Minimalist: show connected text without dot
            Text(
                text = (serverName?.uppercase() ?: "CONNECTED"),
                color = AppColors.onSurfaceVariant,
                style = AppTypography.labelSmall
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SSE STATUS INDICATOR
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * SSE (Server-Sent Events) connection status indicator.
 * Shows a small indicator when SSE is connected/disconnected.
 * Used for real-time synchronization status.
 */
@Composable
fun SseStatusIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
    ) {
        StatusDot(
            color = if (isConnected) AppColors.accentGreen else AppColors.statusOffline
        )
        Text(
            text = if (isConnected) "SSE" else "POLL",
            color = if (isConnected) AppColors.accentGreen else AppColors.statusOffline,
            style = AppTypography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Combined connection + SSE status indicator for dashboard.
 */
@Composable
fun CombinedConnectionIndicator(
    isHttpConnected: Boolean,
    isSseConnected: Boolean,
    serverName: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // HTTP connection status
        if (isHttpConnected) {
            StatusDot(color = AppColors.accentGreen)
            Text(
                text = serverName?.uppercase() ?: "ONLINE",
                color = AppColors.onSurfaceVariant,
                style = AppTypography.labelSmall
            )
        } else {
            StatusDot(color = AppColors.statusOffline)
            Text(
                text = "OFFLINE",
                color = AppColors.statusOffline,
                style = AppTypography.labelSmall
            )
        }
        
        // SSE status (separate indicator)
        if (isHttpConnected) {
            Text(
                text = "•",
                color = AppColors.outline,
                style = AppTypography.labelSmall
            )
            SseStatusIndicator(isConnected = isSseConnected)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// NOT IMPLEMENTED BANNER
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * "Not yet implemented" placeholder banner.
 */
@Composable
fun NotImplementedBanner(
    featureName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.surfaceVariant, AppShapes.medium)
            .padding(AppSpacing.lg)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "NOT IMPLEMENTED",
                color = AppColors.statusWaiting,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "${featureName.uppercase()} feature is under development",
                color = AppColors.outline,
                style = AppTypography.bodySmall
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Empty state placeholder for lists.
 */
@Composable
fun TerminalEmptyState(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.outline
            )
        }
        
        Text(
            text = title.uppercase(),
            color = AppColors.onSurface,
            style = AppTypography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = AppColors.onSurfaceVariant,
                style = AppTypography.bodySmall
            )
        }
        
        if (action != null) {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            action()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LOADING STATE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Terminal-styled loading indicator.
 */
@Composable
fun TerminalLoadingState(
    message: String = "LOADING...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        // Simple text-based loading indicator
        TypewriterText(
            text = message.uppercase(),
            color = AppColors.onSurface,
            style = AppTypography.headlineSmall,
            typingDelayMs = 100L,
            showCursor = true
        )
    }
}
