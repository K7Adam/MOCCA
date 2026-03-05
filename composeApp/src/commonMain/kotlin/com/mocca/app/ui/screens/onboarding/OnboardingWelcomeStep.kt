package com.mocca.app.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
internal fun WelcomeStep(
    onScanQr: () -> Unit,
    onStartDiscovery: () -> Unit,
    onManualEntry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon/branding
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(AppColors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SettingsEthernet,
                contentDescription = null,
                tint = AppColors.accentGreen,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.xxl))
        
        Text(
            text = "Welcome to MOCCA",
            style = AppTypography.headlineMedium,
            color = AppColors.white,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        Text(
            text = "Control your OpenCode AI agent from anywhere",
            style = AppTypography.bodyMedium,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        // Setup checklist
        SetupChecklist()
        
        Spacer(modifier = Modifier.height(AppSpacing.xxl))
        
        // Primary action: QR Scan
        MoccaButton(
            text = "Scan QR Code",
            onClick = onScanQr,
            icon = Icons.Default.QrCodeScanner,
            showArrow = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        // Secondary: Auto-discover
        MoccaOutlinedButton(
            text = "Find Server Automatically",
            onClick = onStartDiscovery,
            modifier = Modifier.fillMaxWidth(),
            height = AppSpacing.buttonHeightCompact
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        // Tertiary: Manual entry
        Text(
            text = "Enter server address manually",
            style = AppTypography.bodyMedium,
            color = AppColors.textSecondary,
            modifier = Modifier.clickable(onClick = onManualEntry)
        )
    }
}
