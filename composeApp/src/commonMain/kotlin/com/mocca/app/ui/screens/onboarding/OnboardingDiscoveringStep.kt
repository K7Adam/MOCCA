package com.mocca.app.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
internal fun DiscoveringStep(
    isLoading: Boolean,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated scanning indicator
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = AppColors.accent,
                strokeWidth = 3.dp
            )
            
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = AppColors.accent,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.xxl))
        
        Text(
            text = "Discovering servers...",
            style = AppTypography.headlineSmall,
            color = AppColors.white
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        Text(
            text = "Looking for OpenCode on your network",
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
