package com.mocca.app.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
internal fun ConnectingStep(
    progress: String,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = AppColors.accentGreen,
            strokeWidth = 3.dp,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.xxl))
        
        Text(
            text = "Connecting...",
            style = AppTypography.headlineSmall,
            color = AppColors.white
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        Text(
            text = progress,
            style = AppTypography.bodyMedium,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.xxl))
        
        Text(
            text = "Cancel",
            style = AppTypography.bodyMedium,
            color = AppColors.textSecondary,
            modifier = Modifier.clickable(onClick = onCancel)
        )
    }
}
