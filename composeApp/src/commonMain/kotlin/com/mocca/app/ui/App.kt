package com.mocca.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.mocca.app.data.repository.PreferencesManager
import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import com.mocca.app.ui.navigation.ModernTransitions
import com.mocca.app.ui.screens.main.MainScreen
import com.mocca.app.ui.screens.onboarding.ProgressiveOnboardingScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppPerformance
import com.mocca.app.ui.theme.AppTheme
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.PerformanceTier
import org.koin.compose.koinInject

@Composable
fun App() {
    // Simple heuristic: could be improved with actual device info via expect/actual
    val performance = remember { AppPerformance(tier = PerformanceTier.HIGH) }
    val preferencesManager = koinInject<PreferencesManager>()
    val preferences by preferencesManager.preferences.collectAsState()
    
    AppTheme(
        performance = performance,
        codeFontFamilyKey = { preferences.codeFontFamily }
    ) {
        val serverConfigRepository = koinInject<ServerConfigRepository>()
        val isLoaded by serverConfigRepository.isLoaded.collectAsState()
        val activeConfig by serverConfigRepository.activeServer.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            if (!isLoaded) {
                SplashScreen()
            } else {
                SharedTransitionLayout {
                    CompositionLocalProvider(
                        LocalSharedTransitionScope provides this
                    ) {
                        val config = activeConfig
                        val startScreen = if (config != null && config.host.isNotBlank()) {
                            MainScreen()
                        } else {
                            ProgressiveOnboardingScreen()
                        }

                        Navigator(startScreen) { navigator ->
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
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
                .alpha(breatheAlpha)
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        Text(
            text = "MOCCA",
            style = AppTypography.headlineMedium,
            color = AppColors.white,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(AppSpacing.xxl))

        LoadingIndicator(
            modifier = Modifier.size(32.dp),
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
