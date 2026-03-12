package com.mocca.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mocca.app.api.NetworkConfig
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaInput
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.collections.immutable.ImmutableList

/**
 * Connect step — unified screen for server discovery (background) + server list + manual entry.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun OnboardingConnectStep(
    discoveredServers: ImmutableList<DiscoveredServer>,
    isDiscovering: Boolean,
    showManualEntry: Boolean,
    error: String?,
    selectedServer: DiscoveredServer?,
    onServerSelected: (DiscoveredServer) -> Unit,
    onManualConnect: (host: String, port: Int, username: String, password: String, useHttps: Boolean) -> Unit,
    onRefreshDiscovery: () -> Unit,
    onToggleManualEntry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Manual entry fields
    var host by remember { mutableStateOf(NetworkConfig.DEFAULT_HOST_IP) }
    var port by remember { mutableStateOf(NetworkConfig.OPENCODE_SERVER_PORT.toString()) }
    var username by remember { mutableStateOf(NetworkConfig.DEFAULT_USERNAME) }
    var password by remember { mutableStateOf(NetworkConfig.DEFAULT_PASSWORD) }
    var useHttps by remember { mutableStateOf(false) }

    // Scanning animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.screenPaddingHorizontal)
            .imePadding()
    ) {
        // Header
        Text(
            text = "CONNECT TO SERVER",
            style = AppTypography.headlineSmall,
            color = AppColors.white,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = AppSpacing.lg)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // ── Discovery Section ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (isDiscovering) AppColors.accent else AppColors.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .then(if (isDiscovering) Modifier.alpha(scanAlpha) else Modifier)
                    )
                    Text(
                        text = if (isDiscovering) "Scanning network..." else "Available Servers",
                        style = AppTypography.labelLarge,
                        color = if (isDiscovering) AppColors.accent else AppColors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    if (isDiscovering) {
                        LoadingIndicator(
                            modifier = Modifier.size(14.dp),
                            color = AppColors.accent
                        )
                    }
                }

                if (!isDiscovering) {
                    MoccaOutlinedButton(
                        text = "Refresh",
                        onClick = onRefreshDiscovery,
                        height = AppSpacing.buttonHeightSmall,
                        icon = Icons.Default.Refresh
                    )
                }
            }

            // Server list
            if (discoveredServers.isNotEmpty()) {
                discoveredServers.forEach { server ->
                    ServerListItem(
                        server = server,
                        isSelected = selectedServer?.baseUrl == server.baseUrl,
                        onClick = { onServerSelected(server) }
                    )
                }
            } else if (!isDiscovering) {
                // No servers found
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.surfaceVariant, AppSpacing.let { 
                            androidx.compose.foundation.shape.RoundedCornerShape(AppSpacing.cornerRadiusMedium) 
                        })
                        .padding(AppSpacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AppColors.textTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No servers found on your network",
                            style = AppTypography.bodyMedium,
                            color = AppColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Make sure OpenCode is running, or enter server details manually",
                            style = AppTypography.bodySmall,
                            color = AppColors.textTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Error message
            if (error != null) {
                ErrorMessage(
                    message = error,
                    onRetry = onRefreshDiscovery
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // ── Manual Entry Section ────────────────────────────────────────────
            if (!showManualEntry) {
                MoccaOutlinedButton(
                    text = "Or enter server details manually",
                    onClick = onToggleManualEntry,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = showManualEntry,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    Text(
                        text = "MANUAL CONNECTION",
                        style = AppTypography.labelMedium,
                        color = AppColors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    // Host + Port row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        MoccaInput(
                            value = host,
                            onValueChange = { host = it },
                            label = "Host / IP",
                            placeholder = "192.168.1.100",
                            modifier = Modifier.weight(2f)
                        )
                        MoccaInput(
                            value = port,
                            onValueChange = { port = it },
                            label = "Port",
                            placeholder = "4242",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    MoccaInput(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        placeholder = "opencode"
                    )

                    MoccaInput(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Enter server password"
                    )

                    // HTTPS toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Use HTTPS",
                            style = AppTypography.bodyMedium,
                            color = AppColors.textSecondary
                        )
                        Switch(
                            checked = useHttps,
                            onCheckedChange = { useHttps = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = AppColors.accentGreen,
                                checkedThumbColor = AppColors.white,
                                uncheckedTrackColor = AppColors.surfaceContainerHigh,
                                uncheckedThumbColor = AppColors.textTertiary
                            )
                        )
                    }

                    // URL preview
                    val protocol = if (useHttps) "https" else "http"
                    val previewHost = host.ifBlank { "..." }
                    val previewPort = port.ifBlank { "4242" }
                    Text(
                        text = "$protocol://$previewHost:$previewPort",
                        style = AppTypography.bodySmall,
                        color = AppColors.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Connect button
                    MoccaButton(
                        text = "Connect",
                        onClick = {
                            onManualConnect(
                                host,
                                port.toIntOrNull() ?: NetworkConfig.OPENCODE_SERVER_PORT,
                                username,
                                password,
                                useHttps
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = host.isNotBlank()
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))
        }

        // Bottom back button
        MoccaOutlinedButton(
            text = "Back",
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.lg)
        )
    }
}
