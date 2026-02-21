package com.mocca.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.ui.screens.main.MainScreen
import com.mocca.app.ui.screens.onboarding.ProgressiveOnboardingScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    AppTheme {
        val serverConfigRepository = koinInject<ServerConfigRepository>()
        val activeConfig = serverConfigRepository.activeServer.value

        val startScreen = if (activeConfig != null && activeConfig.host.isNotBlank()) {
            MainScreen()
        } else {
            ProgressiveOnboardingScreen()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
        ) {
            Navigator(startScreen) { navigator ->
                SlideTransition(navigator)
            }
        }
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
