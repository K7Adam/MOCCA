package com.mocca.app.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.ui.screens.main.MainScreen
import com.mocca.app.ui.screens.onboarding.ProgressiveOnboardingScreen
import com.mocca.app.ui.theme.AppTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    AppTheme {
        val serverConfigRepository = koinInject<ServerConfigRepository>()
        val activeConfig = serverConfigRepository.activeServer.value

        // Skip onboarding if we already have a valid server configured
        val startScreen = if (activeConfig != null && activeConfig.host.isNotBlank()) {
            MainScreen()
        } else {
            ProgressiveOnboardingScreen()
        }

        Navigator(startScreen) { navigator ->
            SlideTransition(navigator)
        }
    }
}
