package com.mocca.app.ui.screens.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.TestTags
import androidx.compose.ui.platform.testTag

@Composable
internal actual fun BridgeQrScanButton(
    enabled: Boolean,
    onPayloadScanned: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val options = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
    }
    var isScanning by remember { mutableStateOf(false) }

    MoccaOutlinedButton(
        text = if (isScanning) "Scanning..." else "Scan QR",
        icon = Icons.Default.QrCodeScanner,
        enabled = enabled && !isScanning,
        modifier = modifier.testTag(TestTags.Onboarding.qrScanButton),
        onClick = {
            if (isScanning) return@MoccaOutlinedButton
            isScanning = true
            GmsBarcodeScanning.getClient(context, options)
                .startScan()
                .addOnSuccessListener(mainExecutor) { barcode ->
                    isScanning = false
                    val rawValue = barcode.rawValue
                    if (rawValue.isNullOrBlank()) {
                        onError("QR code did not contain a MOCCA pairing link")
                    } else {
                        onPayloadScanned(rawValue)
                    }
                }
                .addOnCanceledListener(mainExecutor) {
                    isScanning = false
                }
                .addOnFailureListener(mainExecutor) { error ->
                    isScanning = false
                    onError(error.message ?: "QR scan failed")
                }
        }
    )
}
