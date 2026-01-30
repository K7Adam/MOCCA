package com.mocca.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.domain.model.DiscoverySource
import com.mocca.app.ui.components.terminal.TerminalButton
import com.mocca.app.ui.components.terminal.TerminalInput
import com.mocca.app.ui.screens.main.MainScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import io.github.aakira.napier.Napier

/**
 * Progressive onboarding screen with wizard-style flow.
 * 
 * Flow:
 * 1. WELCOME - Intro and quick start options
 * 2. DISCOVERING - Auto-scanning for OpenCode servers
 * 3. SELECT_SERVER - Choose from discovered/saved servers or manual entry
 * 4. CONNECTING - Attempting connection with progress
 * 5. READY - Success and transition to main app
 */
class ProgressiveOnboardingScreen : Screen {
    
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<OnboardingWizardModel>()
        val state by screenModel.state.collectAsState()
        
        // Navigate to main screen when connected
        LaunchedEffect(state.isConnected, state.currentStep) {
            if (state.isConnected && state.currentStep == OnboardingStep.READY) {
                delay(500) // Brief delay for visual feedback
                navigator.replace(MainScreen())
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .padding(horizontal = AppSpacing.screenPaddingHorizontal)
        ) {
            // Progress indicator
            WizardProgressIndicator(
                currentStep = state.currentStep,
                modifier = Modifier.padding(top = AppSpacing.screenPaddingTop)
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            
            // Main content with animations
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() with
                    slideOutHorizontally { width -> -width } + fadeOut()
                },
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        onStartDiscovery = { screenModel.onAction(OnboardingAction.StartDiscovery) },
                        onManualEntry = { 
                            screenModel.onAction(OnboardingAction.ManualEntryUpdated("", ""))
                        }
                    )
                    
                    OnboardingStep.DISCOVERING -> DiscoveringStep(
                        isLoading = state.isLoading,
                        onCancel = { screenModel.onAction(OnboardingAction.Back) }
                    )
                    
                    OnboardingStep.SELECT_SERVER -> SelectServerStep(
                        servers = state.allServers,
                        selectedServer = state.selectedServer,
                        error = state.error,
                        onServerSelected = { server ->
                            screenModel.onAction(OnboardingAction.ServerSelected(server))
                        },
                        onScanQr = { /* QR scanning handled by parent */ },
                        onManualEntry = { url, token ->
                            screenModel.onAction(OnboardingAction.ManualEntryUpdated(url, token))
                        },
                        onRetry = { screenModel.onAction(OnboardingAction.StartDiscovery) }
                    )
                    
                    OnboardingStep.CONNECTING -> ConnectingStep(
                        progress = state.connectionProgress,
                        onCancel = { screenModel.onAction(OnboardingAction.Back) }
                    )
                    
                    OnboardingStep.READY -> ReadyStep(
                        onContinue = { screenModel.onAction(OnboardingAction.Complete) }
                    )
                }
            }
            
            // Bottom error message
            if (state.error != null) {
                ErrorMessage(
                    message = state.error!!,
                    onRetry = { screenModel.onAction(OnboardingAction.RetryConnection) },
                    modifier = Modifier.padding(bottom = AppSpacing.lg)
                )
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.screenPaddingBottom))
        }
    }
}

@Composable
private fun WizardProgressIndicator(
    currentStep: OnboardingStep,
    modifier: Modifier = Modifier
) {
    val steps = OnboardingStep.values()
    val currentIndex = steps.indexOf(currentStep)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        steps.forEachIndexed { index, step ->
            val isCompleted = index < currentIndex
            val isCurrent = index == currentIndex
            
            val color = when {
                isCompleted -> AppColors.statusOnline
                isCurrent -> AppColors.accentGreen
                else -> AppColors.border
            }
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            
            if (index < steps.size - 1) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun WelcomeStep(
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
        
        Spacer(modifier = Modifier.height(AppSpacing.xxl))
        
        // Auto-discover button
        TerminalButton(
            text = "Find My Server",
            onClick = onStartDiscovery,
            showArrow = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        // Manual entry option
        Text(
            text = "Enter server address manually",
            style = AppTypography.bodyMedium,
            color = AppColors.textSecondary,
            modifier = Modifier.clickable(onClick = onManualEntry)
        )
    }
}

@Composable
private fun DiscoveringStep(
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
                color = AppColors.accentGreen,
                strokeWidth = 3.dp
            )
            
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = AppColors.accentGreen,
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

@Composable
private fun SelectServerStep(
    servers: List<DiscoveredServer>,
    selectedServer: DiscoveredServer?,
    error: String?,
    onServerSelected: (DiscoveredServer) -> Unit,
    onScanQr: () -> Unit,
    onManualEntry: (String, String) -> Unit,
    onRetry: () -> Unit
) {
    var showManualEntry by remember { mutableStateOf(false) }
    var manualUrl by remember { mutableStateOf("") }
    var manualToken by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Select Server",
            style = AppTypography.headlineSmall,
            color = AppColors.white
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        Text(
            text = "Choose an OpenCode server to connect",
            style = AppTypography.bodyMedium,
            color = AppColors.textSecondary
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        if (servers.isNotEmpty()) {
            // Server list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                modifier = Modifier.weight(1f)
            ) {
                items(servers, key = { it.baseUrl }) { server ->
                    ServerListItem(
                        server = server,
                        isSelected = server.baseUrl == selectedServer?.baseUrl,
                        onClick = { onServerSelected(server) }
                    )
                }
            }
        } else {
            // No servers found
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    
                    Text(
                        text = "No servers found",
                        style = AppTypography.bodyMedium,
                        color = AppColors.textSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    
                    Text(
                        text = "Scan again",
                        style = AppTypography.bodyMedium,
                        color = AppColors.accentGreen,
                        modifier = Modifier.clickable(onClick = onRetry)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        // QR scan button
        TerminalButton(
            text = "Scan QR Code",
            onClick = onScanQr,
            icon = Icons.Default.QrCodeScanner,
            modifier = Modifier.fillMaxWidth(),
            variant = TerminalButtonVariant.OUTLINED
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        // Manual entry toggle
        if (!showManualEntry) {
            Text(
                text = "Enter server manually",
                style = AppTypography.bodyMedium,
                color = AppColors.textSecondary,
                modifier = Modifier
                    .clickable { showManualEntry = true }
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            // Manual entry form
            TerminalInput(
                value = manualUrl,
                onValueChange = { manualUrl = it },
                label = "Server URL",
                placeholder = "http://192.168.1.42:4096"
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            TerminalInput(
                value = manualToken,
                onValueChange = { manualToken = it },
                label = "Auth Token (optional)",
                placeholder = "Enter API key"
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            TerminalButton(
                text = "Connect",
                onClick = { onManualEntry(manualUrl, manualToken) },
                enabled = manualUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ServerListItem(
    server: DiscoveredServer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val sourceIcon = when (server.source) {
        DiscoverySource.MDNS -> Icons.Default.Wifi
        DiscoverySource.QR_CODE -> Icons.Default.QrCodeScanner
        DiscoverySource.SAVED -> Icons.Default.Check
        DiscoverySource.MANUAL -> Icons.Default.SettingsEthernet
        DiscoverySource.EMULATOR_AUTO -> Icons.Default.Refresh
    }
    
    val borderColor = if (isSelected) AppColors.accentGreen else AppColors.border
    val backgroundColor = if (isSelected) AppColors.accentGreen.copy(alpha = 0.1f) else AppColors.surfaceVariant
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(backgroundColor, AppShapes.card)
            .border(AppSpacing.borderThin, borderColor, AppShapes.card)
            .clickable(onClick = onClick)
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = sourceIcon,
            contentDescription = null,
            tint = if (isSelected) AppColors.accentGreen else AppColors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.md))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                style = AppTypography.bodyMedium,
                color = if (isSelected) AppColors.accentGreen else AppColors.white,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            Text(
                text = server.baseUrl,
                style = AppTypography.bodySmall,
                color = AppColors.textSecondary
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = AppColors.accentGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ConnectingStep(
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

@Composable
private fun ReadyStep(
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(AppColors.statusOnline),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AppColors.background,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.xxl))
        
        Text(
            text = "You're all set!",
            style = AppTypography.headlineMedium,
            color = AppColors.white
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        Text(
            text = "Connected to your OpenCode server",
            style = AppTypography.bodyMedium,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        Text(
            text = "Swipe left for session history\nSwipe right for tools",
            style = AppTypography.bodySmall,
            color = AppColors.textTertiary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.xxl))
        
        TerminalButton(
            text = "Get Started",
            onClick = onContinue,
            showArrow = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.alertRed.copy(alpha = 0.1f), AppShapes.card)
            .border(AppSpacing.borderThin, AppColors.alertRed, AppShapes.card)
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Error",
            tint = AppColors.alertRed,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        
        Text(
            text = message,
            style = AppTypography.bodySmall,
            color = AppColors.alertRed,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        
        Text(
            text = "Retry",
            style = AppTypography.labelSmall,
            color = AppColors.accentGreen,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onRetry)
        )
    }
}

// Import missing
import androidx.compose.foundation.clickable

// Button variant enum for TerminalButton (placeholder - should be in TerminalButton.kt)
enum class TerminalButtonVariant {
    DEFAULT,
    OUTLINED
}
