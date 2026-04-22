package com.mocca.app.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun BridgeQrScanButton(
    enabled: Boolean,
    onPayloadScanned: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
)
