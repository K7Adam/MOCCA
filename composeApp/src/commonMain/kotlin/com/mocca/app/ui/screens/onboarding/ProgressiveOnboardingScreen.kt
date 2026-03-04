package com.mocca.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.screens.main.MainScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
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
                .background(Brush.verticalGradient(listOf(AppColors.background, AppColors.surfaceVariant)))
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
