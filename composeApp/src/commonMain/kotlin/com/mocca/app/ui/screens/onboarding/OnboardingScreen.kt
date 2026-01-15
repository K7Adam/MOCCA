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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.terminal.BlinkingCursor
import com.mocca.app.ui.components.terminal.StatusMonitorCard
import com.mocca.app.ui.components.terminal.TerminalButton
import com.mocca.app.ui.components.terminal.TerminalHeader
import com.mocca.app.ui.screens.main.MainScreen
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

/**
 * Onboarding screen for initial connection setup.
 * Matches mockup: mockups_screens/onboarding_&_connection/screen.png
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
                .background(TerminalColors.background)
                .padding(TerminalSpacing.lg)
        ) {
            // Top status bar
            TopStatusBar(hasSignal = state.isConnected || state.probeState == ProbeState.SUCCESS)
            
            Spacer(modifier = Modifier.height(TerminalSpacing.xl))
            
            // Boot sequence console
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
                    
                    Spacer(modifier = Modifier.height(TerminalSpacing.xxl))
                    
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
        }
    }
}

/**
 * Top status bar with app name and signal indicator.
 */
@Composable
private fun TopStatusBar(hasSignal: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "OPENCODE_TERM",
            color = TerminalColors.white,
            style = TerminalTypography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (hasSignal) TerminalColors.statusOnline else TerminalColors.statusOffline,
                        RectangleShape
                    )
            )
            Text(
                text = if (hasSignal) "SIGNAL_OK" else "NO_SIGNAL",
                color = if (hasSignal) TerminalColors.statusOnline else TerminalColors.grey,
                style = TerminalTypography.labelSmall
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
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
    ) {
        Text(
            text = "SYS_BOOT_SEQ_${(100..999).random()}",
            color = TerminalColors.grey,
            style = TerminalTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(TerminalSpacing.sm))
        
        logs.forEach { entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)
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
                        BootStatus.DONE -> TerminalColors.statusOnline
                        BootStatus.ERROR -> TerminalColors.error
                        BootStatus.WAIT -> TerminalColors.statusWaiting
                        else -> TerminalColors.grey
                    },
                    style = TerminalTypography.bodySmall
                )
                Text(
                    text = entry.message,
                    color = TerminalColors.white,
                    style = TerminalTypography.bodySmall
                )
                if (entry.status == BootStatus.RUNNING) {
                    BlinkingCursor()
                }
            }
        }
    }
}

/**
 * Probing status card with L-bracket corners.
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
                text = "[[ $probeMessage ]]",
                color = when (probeState) {
                    ProbeState.SUCCESS -> TerminalColors.statusOnline
                    ProbeState.FAILED -> TerminalColors.error
                    ProbeState.PROBING -> TerminalColors.statusWaiting
                    ProbeState.IDLE -> TerminalColors.grey
                },
                style = TerminalTypography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (probeState == ProbeState.PROBING) {
                Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                Row {
                    Text(
                        text = "Attempting connection",
                        color = TerminalColors.grey,
                        style = TerminalTypography.bodySmall
                    )
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
        verticalArrangement = Arrangement.spacedBy(TerminalSpacing.lg)
    ) {
        // Server address field
        TerminalInputField(
            label = "// SERVER_ADDRESS",
            value = serverAddress,
            onValueChange = onServerAddressChange,
            placeholder = "> HTTP://LOCALHOST:8000",
            enabled = !isConnecting
        )
        
        // Auth token field
        TerminalInputField(
            label = "// AUTH_TOKEN",
            value = authToken,
            onValueChange = onAuthTokenChange,
            placeholder = "> ENTER_API_KEY",
            helperText = "* Required for secure handshake",
            isPassword = true,
            enabled = !isConnecting
        )
        
        // Error message
        if (connectionError != null) {
            Text(
                text = "ERROR: $connectionError",
                color = TerminalColors.error,
                style = TerminalTypography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(TerminalSpacing.md))
        
        // Connect button
        TerminalButton(
            text = if (isConnecting) "CONNECTING..." else "CONNECT",
            onClick = onConnectClick,
            enabled = !isConnecting,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Terminal-styled input field.
 */
@Composable
private fun TerminalInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    isPassword: Boolean = false,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = TerminalColors.grey,
            style = TerminalTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(TerminalSpacing.xs))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(TerminalSpacing.borderThin, TerminalColors.borderLight, RectangleShape)
                .background(TerminalColors.surface)
                .padding(TerminalSpacing.md)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TerminalTypography.bodyMedium.copy(
                    color = TerminalColors.white
                ),
                cursorBrush = SolidColor(TerminalColors.white),
                visualTransformation = if (isPassword && value.isNotEmpty()) {
                    PasswordVisualTransformation('*')
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = TerminalColors.grey,
                                style = TerminalTypography.bodyMedium
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        if (helperText != null) {
            Spacer(modifier = Modifier.height(TerminalSpacing.xxs))
            Text(
                text = helperText,
                color = TerminalColors.grey,
                style = TerminalTypography.labelSmall
            )
        }
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
            color = TerminalColors.greyDark,
            style = TerminalTypography.labelSmall
        )
    }
}
