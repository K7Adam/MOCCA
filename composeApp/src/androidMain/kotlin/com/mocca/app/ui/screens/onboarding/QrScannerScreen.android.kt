package com.mocca.app.ui.screens.onboarding

import androidx.compose.runtime.Composable
import com.mocca.app.ui.components.QrCodeScanner

/**
 * Android implementation of QR scanner content.
 * Uses the QrCodeScanner composable with CameraX integration.
 */
@Composable
actual fun QrScannerContent(
    onQrCodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    QrCodeScanner(
        onQrCodeDetected = onQrCodeDetected,
        onDismiss = onDismiss
    )
}
