package com.mocca.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.domain.model.DiscoverySource
import com.mocca.app.domain.model.QrConnectionPayload
import com.mocca.app.ui.components.terminal.TerminalIconButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import io.github.aakira.napier.Napier

/**
 * Screen for QR code scanning during onboarding.
 * 
 * This screen wraps the platform-specific QrCodeScanner component and handles
 * the scanned result to configure the server connection.
 */
class QrScannerScreen(
    private val onServerDiscovered: (DiscoveredServer) -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppSpacing.screenPaddingHorizontal,
                        vertical = AppSpacing.md
                    )
            ) {
                TerminalIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { navigator.pop() },
                    iconColor = AppColors.textSecondary
                )

                Text(
                    text = "SCAN QR CODE",
                    style = AppTypography.labelLarge,
                    color = AppColors.white,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // QR Scanner - Platform specific implementation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // The actual QR scanner is Android-specific, so we use an expect/actual pattern
                // or check platform and show appropriate UI
                QrScannerContent(
                    onQrCodeDetected = { qrContent ->
                        Napier.d("QR Code detected: $qrContent")
                        
                        // Try to parse as JSON payload first
                        val payload = QrConnectionPayload.fromJson(qrContent)
                            ?: QrConnectionPayload.fromUrl(qrContent)
                        
                        if (payload != null) {
                            val discoveredServer = payload.toDiscoveredServer()
                            Napier.i("Server discovered from QR: ${discoveredServer.baseUrl}")
                            
                            // Return the discovered server to the parent
                            onServerDiscovered(discoveredServer)
                            navigator.pop()
                        } else {
                            Napier.w("Could not parse QR code content: $qrContent")
                            // Show error or allow retry
                        }
                    },
                    onDismiss = {
                        navigator.pop()
                    }
                )
            }

            // Instructions
            Text(
                text = "Point your camera at the QR code displayed on your computer screen",
                style = AppTypography.bodyMedium,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.lg)
            )
        }
    }
}

/**
 * Platform-specific QR scanner content.
 * 
 * On Android: Uses the actual QrCodeScanner composable with CameraX
 * On other platforms: Shows a placeholder or manual entry option
 */
@Composable
expect fun QrScannerContent(
    onQrCodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
)
