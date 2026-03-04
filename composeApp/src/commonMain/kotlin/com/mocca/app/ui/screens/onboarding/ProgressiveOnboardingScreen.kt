package com.mocca.app.ui.screens.onboarding

import com.mocca.app.api.NetworkConfig

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.domain.model.DiscoverySource
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaInput
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.screens.main.MainScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.coroutines.delay

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
class ProgressiveOnboardingScreen(
    private val isSetupMode: Boolean = false,
    private val initialError: String? = null
) : Screen {
    
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
                
                // If we were in setup mode on top of MainScreen, just pop. 
                // If it's the first screen on app launch, replace with MainScreen.
                if (isSetupMode) {
                    navigator.pop()
                } else {
                    navigator.replace(MainScreen())
                }
            }
        }
        
        // Handle Setup Mode initialization
        LaunchedEffect(isSetupMode) {
            if (isSetupMode) {
                screenModel.onAction(OnboardingAction.InitializeSetupMode(initialError))
            }
        }
        
        // Credential dialog for mDNS-discovered servers
        if (state.needsCredentials && state.credentialServer != null) {
            CredentialDialog(
                serverName = state.credentialServer!!.name,
                onConfirm = { username, password ->
                    screenModel.onAction(
                        OnboardingAction.CredentialsProvided(username, password)
                    )
                },
                onDismiss = {
                    screenModel.onAction(OnboardingAction.Back)
                }
            )
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
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> -width } + fadeOut()
                },
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        onScanQr = { 
                            navigator.push(QrScannerScreen { discoveredServer ->
                                screenModel.onAction(OnboardingAction.ServerSelected(discoveredServer))
                            })
                        },
                        onStartDiscovery = { screenModel.onAction(OnboardingAction.StartDiscovery) },
                        onManualEntry = { 
                            screenModel.onAction(OnboardingAction.GoToManualEntry)
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
                        onScanQr = { 
                            navigator.push(QrScannerScreen { discoveredServer ->
                                screenModel.onAction(OnboardingAction.ServerSelected(discoveredServer))
                            })
                        },
                        onManualConnect = { host, port, username, password, useHttps ->
                            screenModel.onAction(
                                OnboardingAction.ManualConnect(host, port, username, password, useHttps)
                            )
                        },
                        onRetry = { screenModel.onAction(OnboardingAction.StartDiscovery) }
                    )
                    
                    OnboardingStep.CONNECTING -> ConnectingStep(
                        progress = state.connectionProgress,
                        onCancel = { screenModel.onAction(OnboardingAction.Back) }
                    )
                    
                    OnboardingStep.READY -> ReadyStep(
                        onContinue = { 
                            if (isSetupMode) {
                                navigator.pop()
                            } else {
                                screenModel.onAction(OnboardingAction.Complete)
                            }
                        }
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
                isCurrent -> AppColors.accent
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
                tint = AppColors.accent,
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

@Composable
private fun SetupChecklist() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surfaceVariant, AppShapes.card)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.card)
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Text(
            text = "SETUP CHECKLIST",
            style = AppTypography.labelSmall,
            color = AppColors.textSecondary
        )
        
        ChecklistItem(
            number = "1",
            text = "Install OpenCode on your computer",
            subtext = "Download from github.com/opencode-ai/opencode"
        )
        
        ChecklistItem(
            number = "2",
            text = "Start the OpenCode server",
            subtext = "opencode serve --port 4242"
        )
        
        ChecklistItem(
            number = "3",
            text = "Scan the QR code with this app",
            subtext = "Point camera at the code on your screen"
        )
    }
}

@Composable
private fun ChecklistItem(
    number: String,
    text: String,
    subtext: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        // Number circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(AppColors.accent.copy(alpha = 0.2f), CircleShape)
                .border(AppSpacing.borderThin, AppColors.accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = AppTypography.labelMedium,
                color = AppColors.accent,
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = text,
                style = AppTypography.bodyMedium,
                color = AppColors.white,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = subtext,
                style = AppTypography.bodySmall,
                color = AppColors.textTertiary
            )
        }
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

@Composable
private fun SelectServerStep(
    servers: List<DiscoveredServer>,
    selectedServer: DiscoveredServer?,
    error: String?,
    onServerSelected: (DiscoveredServer) -> Unit,
    onScanQr: () -> Unit,
    onManualConnect: (host: String, port: Int, username: String, password: String, useHttps: Boolean) -> Unit,
    onRetry: () -> Unit
) {
    var showManualEntry by remember { mutableStateOf(true) } // Expanded by default for dev
    // TEMPORARY PREFILLS
    var manualHost by remember { mutableStateOf(NetworkConfig.DEFAULT_HOST_IP) }
    var manualPort by remember { mutableStateOf(NetworkConfig.OPENCODE_SERVER_PORT.toString()) }
    var manualUsername by remember { mutableStateOf(NetworkConfig.DEFAULT_USERNAME) }
    var manualPassword by remember { mutableStateOf(NetworkConfig.DEFAULT_PASSWORD) }
    var useHttps by remember { mutableStateOf(false) }
    
    // Removed: Auto-detect Tailscale (.ts.net) and set HTTPS + port 443
    // Tailscale encrypts its overlay network, so HTTP on 4242 works safely without TLS.
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
                        color = AppColors.accent,
                        modifier = Modifier.clickable(onClick = onRetry)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        // QR scan button
        MoccaButton(
            text = "Scan QR Code",
            onClick = onScanQr,
            icon = Icons.Default.QrCodeScanner,
            modifier = Modifier.fillMaxWidth()
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
            // Manual entry form — Host / Port / Username / Password
            MoccaInput(
                value = manualHost,
                onValueChange = { manualHost = it },
                label = "Host",
                placeholder = NetworkConfig.DEFAULT_HOST_IP
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            MoccaInput(
                value = manualPort,
                onValueChange = { manualPort = it },
                label = "Port",
                placeholder = "4242"
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            MoccaInput(
                value = manualUsername,
                onValueChange = { manualUsername = it },
                label = "Username",
                placeholder = NetworkConfig.DEFAULT_USERNAME
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            MoccaInput(
                value = manualPassword,
                onValueChange = { manualPassword = it },
                label = "Password",
                placeholder = "Leave empty if none",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            // HTTPS toggle (auto-detected for Tailscale)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Use HTTPS",
                    style = AppTypography.bodyMedium,
                    color = AppColors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = useHttps,
                    onCheckedChange = { 
                        useHttps = it
                        if (it && manualPort == "4242") {
                            manualPort = "443"
                        } else if (!it && manualPort == "443") {
                            manualPort = "4242"
                        }
                    }
                )
            }
            
            // Show effective URL preview
            val effectiveProtocol = if (useHttps) "https" else "http"
            val effectivePort = manualPort.toIntOrNull() ?: 4242
            Text(
                text = "$effectiveProtocol://${manualHost.trim()}:$effectivePort",
                style = AppTypography.bodySmall,
                color = AppColors.accent,
                modifier = Modifier.padding(vertical = AppSpacing.sm)
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            MoccaButton(
                text = "Connect",
                onClick = {
                    onManualConnect(
                        manualHost.trim(),
                        manualPort.trim().toIntOrNull() ?: 4242,
                        manualUsername.trim().ifBlank { NetworkConfig.DEFAULT_USERNAME },
                        manualPassword,
                        useHttps
                    )
                },
                enabled = manualHost.isNotBlank(),
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
    
    val borderColor = if (isSelected) AppColors.accent else AppColors.border
    val backgroundColor = if (isSelected) AppColors.accent.copy(alpha = 0.1f) else AppColors.surfaceVariant
    
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
            tint = if (isSelected) AppColors.accent else AppColors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.md))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                style = AppTypography.bodyMedium,
                color = if (isSelected) AppColors.accent else AppColors.white,
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
                tint = AppColors.accent,
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
            color = AppColors.accent,
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
        
        MoccaButton(
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
            color = AppColors.accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onRetry)
        )
    }
}

@Composable
private fun CredentialDialog(
    serverName: String,
    onConfirm: (username: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(NetworkConfig.DEFAULT_USERNAME) }
    var password by remember { mutableStateOf(NetworkConfig.DEFAULT_PASSWORD) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceElevated,
        shape = AppShapes.dialog,
        title = {
            Text(
                text = "SERVER CREDENTIALS",
                color = AppColors.white,
                style = AppTypography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter credentials for $serverName",
                    style = AppTypography.bodyMedium,
                    color = AppColors.textSecondary
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                
                MoccaInput(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    placeholder = NetworkConfig.DEFAULT_USERNAME
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.md))
                
                MoccaInput(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "Enter server password"
                )
            }
        },
        confirmButton = {
            MoccaButton(
                text = "Connect",
                onClick = {
                    onConfirm(
                        username.trim().ifBlank { NetworkConfig.DEFAULT_USERNAME },
                        password
                    )
                },
                height = AppSpacing.buttonHeightCompact
            )
        },
        dismissButton = {
            Text(
                text = "Cancel",
                style = AppTypography.bodyMedium,
                color = AppColors.textSecondary,
                modifier = Modifier.clickable(onClick = onDismiss)
            )
        }
    )
}
