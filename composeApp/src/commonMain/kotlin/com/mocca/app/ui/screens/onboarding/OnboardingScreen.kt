package com.mocca.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.terminal.BlinkingCursor
import com.mocca.app.ui.components.terminal.StatusDot
import com.mocca.app.ui.components.terminal.StatusMonitorCard
import com.mocca.app.ui.components.terminal.TerminalButton
import com.mocca.app.ui.components.terminal.TerminalInput
import com.mocca.app.ui.screens.main.MainScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Onboarding screen for initial connection setup.
 * Matches mockup: mockups_screens/onboarding_&_connection/screen.png
 * Refactored to modern UI/UX standards.
 */
class OnboardingScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<OnboardingScreenModel>()
        val state by screenModel.state.collectAsState()
        
        // Navigate to main when connected
        LaunchedEffect(state.isConnected) {
            if (state.isConnected) {
                navigator.replace(MainScreen())
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .padding(AppSpacing.screenPaddingHorizontal)
        ) {
            // Top status bar
            TopStatusBar(hasSignal = state.isConnected || state.probeState == ProbeState.SUCCESS)
            
            Spacer(modifier = Modifier.height(AppSpacing.sectionGap))
            
            // Boot sequence console (kept for flavor, but modernized)
            AnimatedVisibility(
                visible = !state.bootComplete,
                exit = fadeOut(animationSpec = tween(300))
            ) {
                BootSequenceConsole(
                    logs = state.bootLogs,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Main content (after boot)
            AnimatedVisibility(
                visible = state.bootComplete,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(500)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Probing status card
                    ProbingStatusCard(
                        probeState = state.probeState,
                        probeMessage = state.probeMessage
                    )
                    
                    Spacer(modifier = Modifier.height(AppSpacing.xxl))
                    
                    // Connection form
                    ConnectionForm(
                        serverAddress = state.serverAddress,
                        authToken = state.authToken,
                        isConnecting = state.isConnecting,
                        connectionError = state.connectionError,
                        onServerAddressChange = screenModel::updateServerAddress,
                        onAuthTokenChange = screenModel::updateAuthToken,
                        onConnectClick = screenModel::connect
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            FooterInfo()
            Spacer(modifier = Modifier.height(AppSpacing.screenPaddingBottom))
        }
    }
}

/**
 * Top status bar with app name and signal indicator.
 */
@Composable
private fun TopStatusBar(hasSignal: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.screenPaddingTop),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "OPENCODE_TERM",
            color = AppColors.white,
            style = AppTypography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            StatusDot(
                color = if (hasSignal) AppColors.statusOnline else AppColors.statusOffline,
                size = AppSpacing.statusDotSize
            )
            Text(
                text = if (hasSignal) "SIGNAL_OK" else "NO_SIGNAL",
                color = if (hasSignal) AppColors.statusOnline else AppColors.textSecondary,
                style = AppTypography.labelSmall
            )
        }
    }
}

/**
 * Boot sequence console log.
 */
@Composable
private fun BootSequenceConsole(
    logs: List<BootLogEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(AppShapes.medium)
            .background(AppColors.surfaceVariant, AppShapes.medium)
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Text(
            text = "SYS_BOOT_SEQ_${(100..999).random()}",
            color = AppColors.textTertiary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        logs.forEach { entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Text(
                    text = when (entry.status) {
                        BootStatus.DONE -> "[DONE]"
                        BootStatus.ERROR -> "[FAIL]"
                        BootStatus.WAIT -> "[WAIT]"
                        BootStatus.RUNNING -> "[....]"
                        BootStatus.PENDING -> "[----]"
                    },
                    color = when (entry.status) {
                        BootStatus.DONE -> AppColors.statusOnline
                        BootStatus.ERROR -> AppColors.error
                        BootStatus.WAIT -> AppColors.statusWaiting
                        else -> AppColors.textTertiary
                    },
                    style = AppTypography.bodySmall
                )
                Text(
                    text = entry.message,
                    color = AppColors.white,
                    style = AppTypography.bodySmall
                )
                if (entry.status == BootStatus.RUNNING) {
                    BlinkingCursor()
                }
            }
        }
    }
}

/**
 * Probing status card with glassmorphic effect.
 */
@Composable
private fun ProbingStatusCard(
    probeState: ProbeState,
    probeMessage: String
) {
    StatusMonitorCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = probeMessage.uppercase(),
                color = when (probeState) {
                    ProbeState.SUCCESS -> AppColors.statusOnline
                    ProbeState.FAILED -> AppColors.error
                    ProbeState.PROBING -> AppColors.statusWaiting
                    ProbeState.IDLE -> AppColors.textSecondary
                },
                style = AppTypography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (probeState == ProbeState.PROBING) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Attempting connection",
                        color = AppColors.textSecondary,
                        style = AppTypography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BlinkingCursor()
                }
            }
        }
    }
}

/**
 * Connection input form.
 */
@Composable
private fun ConnectionForm(
    serverAddress: String,
    authToken: String,
    isConnecting: Boolean,
    connectionError: String?,
    onServerAddressChange: (String) -> Unit,
    onAuthTokenChange: (String) -> Unit,
    onConnectClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        // Server address field
        TerminalInput(
            label = "SERVER ADDRESS",
            value = serverAddress,
            onValueChange = onServerAddressChange,
            placeholder = "http://localhost:8000",
            enabled = !isConnecting,
            hint = "Enter the full URL of your OpenCode server"
        )
        
        // Auth token field
        TerminalInput(
            label = "AUTH TOKEN",
            value = authToken,
            onValueChange = onAuthTokenChange,
            placeholder = "Enter API Key",
            hint = "Required for secure handshake",
            enabled = !isConnecting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )
        
        // Error message
        if (connectionError != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.medium)
                    .background(AppColors.alertRedDim, AppShapes.medium)
                    .padding(AppSpacing.md)
            ) {
                Text(
                    text = "!",
                    color = AppColors.alertRed,
                    style = AppTypography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = connectionError,
                    color = AppColors.alertRed,
                    style = AppTypography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        // Connect button
        TerminalButton(
            text = if (isConnecting) "CONNECTING..." else "CONNECT",
            onClick = onConnectClick,
            enabled = !isConnecting,
            showArrow = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Footer with version info.
 */
@Composable
private fun FooterInfo() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OPENCODE_AGENT_V1.0 // UNENCRYPTED_LOCAL_LINK",
            color = AppColors.textTertiary,
            style = AppTypography.labelSmall
        )
    }
}
