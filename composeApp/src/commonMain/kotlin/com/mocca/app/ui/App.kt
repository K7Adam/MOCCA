package com.mocca.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.mocca.app.bridge.connection.BridgePairingIntentStore
import com.mocca.app.bridge.connection.BridgeTargetRepository
import com.mocca.app.bridge.opencode.BridgeRuntimeBootstrapper
import com.mocca.app.data.repository.PreferencesManager
import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import com.mocca.app.ui.navigation.ModernTransitions
import com.mocca.app.ui.screens.main.MainScreen
import com.mocca.app.ui.screens.onboarding.ProgressiveOnboardingScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppPerformance
import com.mocca.app.ui.theme.AppTheme
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.detectPerformanceTier
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@Composable
fun App() {
    val performance = remember { AppPerformance(tier = detectPerformanceTier()) }
    val preferencesManager = koinInject<PreferencesManager>()
    val preferences by preferencesManager.preferences.collectAsState()
    
    AppTheme(
        performance = performance,
        codeFontFamilyKey = { preferences.codeFontFamily }
    ) {
        val serverConfigRepository = koinInject<ServerConfigRepository>()
        val bridgePairingIntentStore = koinInject<BridgePairingIntentStore>()
        val bridgeTargetRepository = koinInject<BridgeTargetRepository>()
        val bridgeRuntimeBootstrapper = koinInject<BridgeRuntimeBootstrapper>()
        val isLoaded by serverConfigRepository.isLoaded.collectAsState()
        val activeConfig by serverConfigRepository.activeServer.collectAsState()
        val pendingBridgePairingPayload by bridgePairingIntentStore.pendingPayload.collectAsState()
        val bridgeTargetLoaded by bridgeTargetRepository.isLoaded.collectAsState()
        val activeBridgeTarget by bridgeTargetRepository.activeTarget.collectAsState()
        var attemptedInitialBridgeBootstrap by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            bridgeTargetRepository.load()
        }

        LaunchedEffect(bridgeTargetLoaded, pendingBridgePairingPayload) {
            if (!bridgeTargetLoaded || pendingBridgePairingPayload != null || attemptedInitialBridgeBootstrap > 0) {
                return@LaunchedEffect
            }
            attemptedInitialBridgeBootstrap = 1
            val target = activeBridgeTarget ?: return@LaunchedEffect
            try {
                bridgeRuntimeBootstrapper.ensureRuntimeServer(target)
            } catch (error: Exception) {
                if (error.isExpectedBridgeOffline()) {
                    Napier.i("MOCCA CLI bridge is offline; startup continues in disconnected mode")
                } else {
                    Napier.w("MOCCA CLI runtime bootstrap failed", error)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            if (!isLoaded || !bridgeTargetLoaded) {
                SplashScreen()
            } else {
                val config = activeConfig
                val startScreen = when {
                    pendingBridgePairingPayload != null -> ProgressiveOnboardingScreen()
                    (config != null && config.host.isNotBlank()) || activeBridgeTarget != null -> MainScreen()
                    else -> ProgressiveOnboardingScreen()
                }

                if (performance.useHeavyNavigationMotion) {
                    SharedTransitionLayout {
                        CompositionLocalProvider(
                            LocalSharedTransitionScope provides this
                        ) {
                            MoccaNavigator(
                                startScreen = startScreen,
                                pendingBridgePairingPayload = pendingBridgePairingPayload,
                                animatedRootTransitions = true
                            )
                        }
                    }
                } else {
                    CompositionLocalProvider(
                        LocalSharedTransitionScope provides null,
                        LocalNavAnimatedVisibilityScope provides null
                    ) {
                        MoccaNavigator(
                            startScreen = startScreen,
                            pendingBridgePairingPayload = pendingBridgePairingPayload,
                            animatedRootTransitions = false
                        )
                    }
                }
            }
        }
    }
}

private fun Throwable.isExpectedBridgeOffline(): Boolean {
    val message = message.orEmpty()
    return "Failed to connect" in message ||
        "bridge did not connect" in message ||
        "health check failed" in message
}

@Composable
private fun MoccaNavigator(
    startScreen: cafe.adriel.voyager.core.screen.Screen,
    pendingBridgePairingPayload: String?,
    animatedRootTransitions: Boolean
) {
    Navigator(startScreen) { navigator ->
        LaunchedEffect(pendingBridgePairingPayload) {
            if (
                pendingBridgePairingPayload != null &&
                navigator.lastItem !is ProgressiveOnboardingScreen
            ) {
                navigator.push(ProgressiveOnboardingScreen(isSetupMode = true))
            }
        }

        if (animatedRootTransitions) {
            val transitionSpec = ModernTransitions.expressiveFadeScale()

            androidx.compose.animation.AnimatedContent(
                targetState = navigator.lastItem,
                transitionSpec = { transitionSpec },
                label = "expressive_screen_transition"
            ) { screen ->
                navigator.saveableState("transition", screen) {
                    screen.Content()
                }
            }
        } else {
            val screen = navigator.lastItem
            navigator.saveableState("screen", screen) {
                screen.Content()
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    var entrancePhase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(100)
        entrancePhase = 1
        delay(500)
        entrancePhase = 2
        delay(350)
        entrancePhase = 3
    }

    val iconScale by animateFloatAsState(
        targetValue = if (entrancePhase >= 1) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconScale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (entrancePhase >= 1) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "iconAlpha"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (entrancePhase >= 2) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "textAlpha"
    )
    val textOffsetY by animateFloatAsState(
        targetValue = if (entrancePhase >= 2) 0f else 12f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "textOffsetY"
    )
    val loadingAlpha by animateFloatAsState(
        targetValue = if (entrancePhase >= 3) 1f else 0f,
        animationSpec = tween(250),
        label = "loadingAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheAlpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = "MOCCA",
            tint = AppColors.accent,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha = iconAlpha * if (entrancePhase >= 3) breatheAlpha else 1f
                }
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        Text(
            text = "MOCCA",
            style = AppTypography.headlineMedium,
            color = AppColors.white,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                translationY = textOffsetY * density
                alpha = textAlpha
            }
        )

        Spacer(modifier = Modifier.height(AppSpacing.xxl))

        LoadingIndicator(
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer {
                    alpha = loadingAlpha
                },
            color = AppColors.accent
        )
    }
}

@Composable
fun Modifier.edgeToEdgePadding(): Modifier = this.windowInsetsPadding(
    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
)

@Composable
fun Modifier.navigationBarPadding(): Modifier = this.windowInsetsPadding(
    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
)
