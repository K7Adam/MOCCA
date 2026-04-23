package com.mocca.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.mocca.app.ui.theme.AppShapes
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
import com.mocca.app.bridge.client.DirectBridgeNetwork
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaInput
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import kotlinx.collections.immutable.ImmutableList

/**
 * Connect step — unified screen for server discovery (background) + server list + manual entry.
 */

@Composable
internal fun OnboardingConnectStep(
    discoveredServers: ImmutableList<DiscoveredServer>,
    isDiscovering: Boolean,
    showManualEntry: Boolean,
    error: String?,
    selectedServer: DiscoveredServer?,
    onServerSelected: (DiscoveredServer) -> Unit,
    onManualConnect: (host: String, port: Int, username: String, password: String, useHttps: Boolean) -> Unit,
    bridgePairingPayload: String,
    bridgePairingNetwork: DirectBridgeNetwork?,
    onBridgePairingPayloadChange: (String) -> Unit,
    onBridgePairingConnect: () -> Unit,
    onBridgePairingPayloadScanned: (String) -> Unit,
    onBridgePairingError: (String) -> Unit,
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
            animation = tween(2000, easing = LinearEasing),
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
            text = "FIND YOUR SERVER",
            style = AppTypography.headlineSmall,
            color = AppColors.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = AppSpacing.lg)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = AppShapes.card,
                color = AppColors.surfaceContainerHigh,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = AppColors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                Text(
                                    text = "MOCCA CLI",
                                    style = AppTypography.labelLarge,
                                    color = AppColors.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .background(AppColors.accent.copy(alpha = 0.16f), AppShapes.pill)
                                        .padding(horizontal = AppSpacing.sm, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Recommended",
                                        style = AppTypography.labelSmall,
                                        color = AppColors.accent,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Text(
                                text = "Run npx mocca-cli, scan the QR code, and MOCCA will start OpenCode for you.",
                                style = AppTypography.labelSmall,
                                color = AppColors.outline
                            )
                        }
                    }

                    MoccaInput(
                        value = bridgePairingPayload,
                        onValueChange = onBridgePairingPayloadChange,
                        label = "Pairing link",
                        placeholder = "mocca://bridge/connect?...",
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(
                        visible = bridgePairingNetwork == DirectBridgeNetwork.TAILSCALE,
                        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) +
                            expandVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                        exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) +
                            shrinkVertically(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec())
                    ) {
                        BridgeNetworkHint()
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth < 360.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                BridgeQrScanButton(
                                    enabled = true,
                                    onPayloadScanned = onBridgePairingPayloadScanned,
                                    onError = onBridgePairingError,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MoccaButton(
                                    text = "Connect",
                                    icon = if (bridgePairingPayload.isBlank()) Icons.Default.Link else Icons.Default.PlayArrow,
                                    onClick = onBridgePairingConnect,
                                    enabled = bridgePairingPayload.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                            ) {
                                BridgeQrScanButton(
                                    enabled = true,
                                    onPayloadScanned = onBridgePairingPayloadScanned,
                                    onError = onBridgePairingError,
                                    modifier = Modifier.weight(1f)
                                )
                                MoccaButton(
                                    text = "Connect",
                                    icon = if (bridgePairingPayload.isBlank()) Icons.Default.Link else Icons.Default.PlayArrow,
                                    onClick = onBridgePairingConnect,
                                    enabled = bridgePairingPayload.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "This enables local OpenCode config, providers, agents, commands, MCP and fast session APIs through the CLI bridge.",
                        style = AppTypography.labelSmall,
                        color = AppColors.onSurfaceVariant
                    )
                }
            }

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
                        tint = if (isDiscovering) AppColors.accent else AppColors.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .then(if (isDiscovering) Modifier.alpha(scanAlpha) else Modifier)
                    )
                    Text(
                        text = if (isDiscovering) "Scanning for OpenCode servers..." else "Discovered Servers",
                        style = AppTypography.labelLarge,
                        color = if (isDiscovering) AppColors.accent else AppColors.onSurfaceVariant,
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
                        text = "Scan Again",
                        onClick = onRefreshDiscovery,
                        height = AppSpacing.buttonHeightSmall,
                        icon = Icons.Default.Refresh
                    )
                }
            }

            Text(
                text = if (discoveredServers.isNotEmpty()) {
                    "Tap a server to connect. MOCCA will import its providers and models automatically."
                } else {
                    "Scanning saved configs and your local network for OpenCode servers."
                },
                style = AppTypography.labelSmall,
                color = AppColors.outline
            )

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
                        .background(AppColors.bgRaised, AppShapes.medium)
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
                            tint = AppColors.outline,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No OpenCode servers found",
                            style = AppTypography.bodyMedium,
                            color = AppColors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Use MOCCA CLI for the simplest setup, or enter an existing OpenCode server manually below.",
                            style = AppTypography.bodySmall,
                            color = AppColors.outline,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Quick start: npx mocca-cli",
                            style = AppTypography.labelSmall,
                            color = AppColors.accent,
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

            if (!showManualEntry) {
                MoccaOutlinedButton(
                    text = "Enter address manually",
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
                        color = AppColors.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Enter your OpenCode server address if discovery didn't find it.",
                        style = AppTypography.labelSmall,
                        color = AppColors.outline
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth < 360.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                            ) {
                                MoccaInput(
                                    value = host,
                                    onValueChange = { host = it },
                                    label = "Host / IP",
                                    placeholder = "192.168.1.100",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MoccaInput(
                                    value = port,
                                    onValueChange = { port = it },
                                    label = "Port",
                                    placeholder = "4242",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
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
                        }
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
                            color = AppColors.onSurfaceVariant
                        )
                        Switch(
                            checked = useHttps,
                            onCheckedChange = { useHttps = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = AppColors.primary,
                                checkedThumbColor = AppColors.onSurface,
                                uncheckedTrackColor = AppColors.surfaceContainerHigh,
                                uncheckedThumbColor = AppColors.outline
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
                        color = AppColors.outline,
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

@Composable
private fun BridgeNetworkHint() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.accent.copy(alpha = 0.12f), AppShapes.medium)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = AppColors.accent,
            modifier = Modifier.size(18.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Tailscale bridge detected",
                style = AppTypography.labelMedium,
                color = AppColors.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "MOCCA will connect through your tailnet instead of local Wi-Fi discovery.",
                style = AppTypography.labelSmall,
                color = AppColors.onSurfaceVariant
            )
        }
    }
}
