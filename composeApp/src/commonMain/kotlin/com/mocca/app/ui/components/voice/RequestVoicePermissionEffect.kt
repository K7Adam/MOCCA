package com.mocca.app.ui.components.voice

import androidx.compose.runtime.Composable

@Composable
expect fun RequestVoicePermissionEffect(
    requestToken: Int,
    onResult: (Boolean) -> Unit
)
