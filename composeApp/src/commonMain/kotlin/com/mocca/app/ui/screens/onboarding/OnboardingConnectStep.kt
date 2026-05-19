package com.mocca.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.bridge.client.DirectBridgeNetwork
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaInput
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.TestTags
import androidx.compose.ui.platform.testTag

/**
 * Connect step — MOCCA CLI bridge pairing only.
 */

@Composable
internal fun OnboardingConnectStep(
    error: String?,
    bridgePairingPayload: String,
    bridgePairingNetwork: DirectBridgeNetwork?,
    onBridgePairingPayloadChange: (String) -> Unit,
    onBridgePairingConnect: () -> Unit,
    onBridgePairingPayloadScanned: (String) -> Unit,
    onBridgePairingError: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.screenPaddingHorizontal)
            .imePadding()
            .testTag(TestTags.Onboarding.connectStep)
    ) {
        Text(
            text = "CONNECT TO MOCCA CLI",
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
                            }
                            Text(
                                text = "Run npx mocca-cli on your computer, then scan the QR code or paste the pairing link.",
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
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.Onboarding.pairingLinkInput)
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
                                    modifier = Modifier.fillMaxWidth().testTag(TestTags.Onboarding.qrScanButton)
                                )
                                MoccaButton(
                                    text = "Connect",
                                    icon = if (bridgePairingPayload.isBlank()) Icons.Default.Link else Icons.Default.PlayArrow,
                                    onClick = onBridgePairingConnect,
                                    enabled = bridgePairingPayload.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().testTag(TestTags.Onboarding.connectButton)
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
                                    modifier = Modifier.weight(1f).testTag(TestTags.Onboarding.qrScanButton)
                                )
                                MoccaButton(
                                    text = "Connect",
                                    icon = if (bridgePairingPayload.isBlank()) Icons.Default.Link else Icons.Default.PlayArrow,
                                    onClick = onBridgePairingConnect,
                                    enabled = bridgePairingPayload.isNotBlank(),
                                    modifier = Modifier.weight(1f).testTag(TestTags.Onboarding.connectButton)
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

            // Error message
            if (error != null) {
                ErrorMessage(
                    message = error,
                    onRetry = onBridgePairingConnect
                )
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
                .testTag(TestTags.Onboarding.backButton)
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
