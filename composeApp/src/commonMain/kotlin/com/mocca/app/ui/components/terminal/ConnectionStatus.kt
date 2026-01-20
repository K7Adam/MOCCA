package com.mocca.app.ui.components.terminal

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
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalShapes
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography
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
    onRetryClick: (() -> Unit)? = null
) {
    val (icon, message, backgroundColor, textColor) = when (status) {
        is ConnectionBannerStatus.Disconnected -> 
            Quadruple(
                Icons.Default.WifiOff,
                status.error ?: "NO CONNECTION",
                TerminalColors.surfaceContainer,
                TerminalColors.statusOffline
            )
        is ConnectionBannerStatus.Connecting ->
            Quadruple(
                Icons.Default.Warning,
                "CONNECTING... (${status.attempt}/${status.maxAttempts})",
                TerminalColors.surfaceContainer,
                TerminalColors.statusWaiting
            )
        is ConnectionBannerStatus.WaitingForNetwork ->
            Quadruple(
                Icons.Default.WifiOff,
                "WAITING FOR NETWORK",
                TerminalColors.surfaceContainer,
                TerminalColors.statusWaiting
            )
        is ConnectionBannerStatus.Reconnecting ->
            Quadruple(
                Icons.Default.Warning,
                "RECONNECTING... (${status.attempt}/${status.maxAttempts})",
                TerminalColors.surfaceContainer,
                TerminalColors.statusWaiting
            )
        is ConnectionBannerStatus.Error ->
            Quadruple(
                Icons.Default.Warning,
                status.message,
                TerminalColors.surfaceContainer,
                TerminalColors.error
            )
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(TerminalShapes.medium)
            .background(backgroundColor, TerminalShapes.medium)
            .padding(
                horizontal = TerminalSpacing.lg,
                vertical = TerminalSpacing.md
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor
                )
                Text(
                    text = message.uppercase(),
                    color = textColor,
                    style = TerminalTypography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
            ) {
                if (onRetryClick != null) {
                    TerminalTextButton(
                        text = "RETRY",
                        onClick = onRetryClick,
                        textColor = TerminalColors.white
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
        horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
    ) {
        StatusDot(
            color = if (isConnected) TerminalColors.statusOnline else TerminalColors.statusOffline
        )
        Text(
            text = if (isConnected) "NO SIGNAL" else (serverName?.uppercase() ?: "CONNECTED"),
            color = TerminalColors.textSecondary,
            style = TerminalTypography.labelSmall
        )
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
            .clip(TerminalShapes.medium)
            .background(TerminalColors.surfaceVariant, TerminalShapes.medium)
            .padding(TerminalSpacing.lg)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "NOT IMPLEMENTED",
                color = TerminalColors.statusWaiting,
                style = TerminalTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(TerminalSpacing.xs))
            Text(
                text = "${featureName.uppercase()} feature is under development",
                color = TerminalColors.textTertiary,
                style = TerminalTypography.bodySmall
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
            .padding(TerminalSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TerminalColors.textTertiary
            )
        }
        
        Text(
            text = title.uppercase(),
            color = TerminalColors.white,
            style = TerminalTypography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = TerminalColors.textSecondary,
                style = TerminalTypography.bodySmall
            )
        }
        
        if (action != null) {
            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
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
            .padding(TerminalSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
    ) {
        // Simple text-based loading indicator
        TypewriterText(
            text = message.uppercase(),
            color = TerminalColors.white,
            style = TerminalTypography.headlineSmall,
            typingDelayMs = 100L,
            showCursor = true
        )
    }
}
