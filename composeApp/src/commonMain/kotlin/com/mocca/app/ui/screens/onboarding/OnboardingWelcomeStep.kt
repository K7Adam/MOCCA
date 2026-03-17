package com.mocca.app.ui.screens.onboarding

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import com.mocca.app.ui.theme.AppTypography

/**
 * Welcome step — app branding, setup checklist, and entry options.
 */
@Composable
internal fun OnboardingWelcomeStep(
    onAutoDiscover: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Subtle breathing animation for the terminal icon
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.screenPaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.15f))

        // Hero branding
        Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = "MOCCA",
            tint = AppColors.accent,
            modifier = Modifier
                .size(72.dp)
                .alpha(breatheAlpha)
        )

        Spacer(modifier = Modifier.height(AppSpacing.xl))

        Text(
            text = "MOCCA",
            style = AppTypography.displayLarge,
            color = AppColors.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        Text(
            text = "Your mobile companion for OpenCode",
            style = AppTypography.bodyLarge,
            color = AppColors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(AppSpacing.xxxl))

        // Setup checklist
        SetupChecklist()

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons (bottom-aligned)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            MoccaButton(
                text = "Auto-Discover Servers",
                onClick = onAutoDiscover,
                modifier = Modifier.fillMaxWidth()
            )

            MoccaOutlinedButton(
                text = "Enter Server Manually",
                onClick = onManualEntry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
