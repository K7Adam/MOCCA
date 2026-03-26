package com.mocca.app.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppTypography

@Composable
internal fun EmptySessionState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            ModernBootSequence()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Mocca AI v2",
                style = AppTypography.headlineMedium,
                color = AppColors.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "System ready // Select model",
                style = AppTypography.labelExtraSmall,
                color = AppColors.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
internal fun ModernBootSequence() {
    val lines = listOf(
        "Mocca OS boot",
        "Network uplink secured",
        "Resources maximized"
    )
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        lines.forEachIndexed { index, line ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index * 120L)
                visible = true
            }
            if (visible) {
                Text(
                    text = line,
                    style = AppTypography.labelExtraSmall,
                    color = if (index == lines.size - 1) AppColors.primary else AppColors.outline,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
