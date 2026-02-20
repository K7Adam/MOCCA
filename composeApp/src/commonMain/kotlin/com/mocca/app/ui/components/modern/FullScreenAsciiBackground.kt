package com.mocca.app.ui.components.modern

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Full-screen ASCII shader background.
 * API 33+ → AGSL RuntimeShader (GPU, 60 fps).
 * API <33  → CPU wave-field ASCII grid (30 fps cap).
 *
 * Designed to be placed BELOW all content layers in MainScreen so the
 * Kyant0/backdrop liquidBackdropSource captures it for the glass bottom bar.
 */
expect @Composable fun FullScreenAsciiBackground(modifier: Modifier = Modifier)
