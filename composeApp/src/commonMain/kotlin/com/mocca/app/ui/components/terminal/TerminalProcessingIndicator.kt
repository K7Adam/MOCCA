package com.mocca.app.ui.components.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.coroutines.delay

@Composable
fun TerminalProcessingIndicator() {
    var frameIndex by remember { mutableIntStateOf(0) }
    
    // Ora spinner frames (dots)
    val frames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(80)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = frames[frameIndex],
            color = AppColors.accentGreen,
            style = AppTypography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        
        Text(
            text = "PROCESSING REQUEST...",
            color = AppColors.accentGreen,
            style = AppTypography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
