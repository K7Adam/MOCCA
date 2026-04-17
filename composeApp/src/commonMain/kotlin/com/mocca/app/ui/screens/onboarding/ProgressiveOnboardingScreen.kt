package com.mocca.app.ui.screens.onboarding

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.screens.main.MainScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import org.koin.compose.koinInject
import com.mocca.app.data.repository.ConnectionManager
import com.mocca.app.data.repository.ServerConfigRepository
import androidx.compose.animation.core.tween
import com.mocca.app.discovery.ServerDiscovery

/**
 * Progressive Onboarding Screen — 3-step wizard.
 *
 * Flow: WELCOME → CONNECT → CONNECTING (auto-nav to MainScreen on success)
 *
 * @param isSetupMode When true, this screen was pushed from MainScreen for re-setup.
 *                    On completion, pops back instead of replacing.
 * @param connectionError Optional error message to display from a failed connection.
 */
class ProgressiveOnboardingScreen(
    private val isSetupMode: Boolean = false,
    private val connectionError: String? = null
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val serverConfigRepository = koinInject<ServerConfigRepository>()
        val connectionManager = koinInject<ConnectionManager>()
        val serverDiscovery = koinInject<ServerDiscovery>()
        val appStateStore = koinInject<com.mocca.app.data.repository.AppStateStore>()

        val screenModel = rememberScreenModel {
            OnboardingWizardModel(serverConfigRepository, connectionManager, serverDiscovery, appStateStore)
        }

        val state by screenModel.state.collectAsState()

        // Initialize setup mode if needed
        LaunchedEffect(isSetupMode) {
            if (isSetupMode) {
                screenModel.onAction(OnboardingAction.InitializeSetupMode(connectionError))
            }
        }

        // Auto-navigate on success
        LaunchedEffect(state.isSuccess) {
            if (state.isSuccess) {
                if (isSetupMode) {
                    navigator.pop()
                } else {
                    navigator.replace(MainScreen())
                }
            }
        }

        // Credential dialog
        if (state.needsCredentials && state.credentialServer != null) {
            CredentialDialog(
                serverName = state.credentialServer!!.name,
                onConfirm = { username, password ->
                    screenModel.onAction(OnboardingAction.CredentialsProvided(username, password))
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
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Step indicator
            WizardStepIndicator(
                currentStep = state.currentStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppSpacing.screenPaddingHorizontal,
                        vertical = AppSpacing.lg
                    )
            )

            // Step content with animated transitions
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(
                        animationSpec = tween(400),
                        initialOffsetX = { fullWidth -> direction * fullWidth }
                    ) + fadeIn(animationSpec = tween(1000)))
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { fullWidth -> -direction * fullWidth }
                            ) + fadeOut(animationSpec = tween(1000))
                        )
                },
                label = "onboardingStep"
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> OnboardingWelcomeStep(
                        onAutoDiscover = {
                            screenModel.onAction(OnboardingAction.GoToConnect)
                        },
                        onManualEntry = {
                            screenModel.onAction(OnboardingAction.GoToManualEntry)
                        }
                    )
                    OnboardingStep.CONNECT -> OnboardingConnectStep(
                        discoveredServers = state.allServers,
                        isDiscovering = state.isDiscovering,
                        showManualEntry = state.showManualEntry,
                        error = state.error,
                        selectedServer = state.selectedServer,
                        onServerSelected = { server ->
                            screenModel.onAction(OnboardingAction.ServerSelected(server))
                        },
                        onManualConnect = { host, port, username, password, useHttps ->
                            screenModel.onAction(
                                OnboardingAction.ManualConnect(host, port, username, password, useHttps)
                            )
                        },
                        onRefreshDiscovery = {
                            screenModel.onAction(OnboardingAction.StartDiscovery)
                        },
                        onToggleManualEntry = {
                            screenModel.onAction(OnboardingAction.GoToManualEntry)
                        },
                        onBack = {
                            screenModel.onAction(OnboardingAction.Back)
                        }
                    )
                    OnboardingStep.CONNECTING -> OnboardingConnectingStep(
                        connectionStage = state.connectionStage,
                        connectionProgress = state.connectionProgress,
                        error = state.error,
                        isSuccess = state.isSuccess,
                        onRetry = {
                            screenModel.onAction(OnboardingAction.RetryConnection)
                        },
                        onBack = {
                            screenModel.onAction(OnboardingAction.Back)
                        }
                    )
                }
            }
        }
    }
}

// Step Indicator — labeled circles connected by lines


private val stepLabels = listOf("Welcome", "Connect", "Connecting")

@Composable
private fun WizardStepIndicator(
    currentStep: OnboardingStep,
    modifier: Modifier = Modifier
) {
    val currentIndex = currentStep.ordinal

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OnboardingStep.entries.forEachIndexed { index, step ->
            val isActive = index == currentIndex
            val isComplete = index < currentIndex

            // Step circle
            StepCircle(
                label = stepLabels.getOrElse(index) { "" },
                isActive = isActive,
                isComplete = isComplete,
                stepNumber = index + 1
            )

            // Connecting line (except after last step)
            if (index < OnboardingStep.entries.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.width(32.dp),
                    thickness = 2.dp,
                    color = if (index < currentIndex) AppColors.success else AppColors.outline
                )
            }
        }
    }
}

@Composable
private fun StepCircle(
    label: String,
    isActive: Boolean,
    isComplete: Boolean,
    stepNumber: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "stepScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .scale(scale)
                .background(
                    when {
                        isComplete -> AppColors.success
                        isActive -> AppColors.accent
                        else -> AppColors.surfaceVariant
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Complete",
                    tint = AppColors.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "$stepNumber",
                    style = AppTypography.labelSmall,
                    color = if (isActive) AppColors.onSurface else AppColors.outline,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = label,
            style = AppTypography.labelSmall,
            color = when {
                isComplete -> AppColors.success
                isActive -> AppColors.accent
                else -> AppColors.outline
            },
            textAlign = TextAlign.Center,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
        )
    }
}
