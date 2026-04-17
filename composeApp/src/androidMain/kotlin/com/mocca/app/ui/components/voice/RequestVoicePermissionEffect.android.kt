package com.mocca.app.ui.components.voice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun RequestVoicePermissionEffect(
    requestToken: Int,
    onResult: (Boolean) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult
    )

    LaunchedEffect(requestToken) {
        if (requestToken > 0) {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
