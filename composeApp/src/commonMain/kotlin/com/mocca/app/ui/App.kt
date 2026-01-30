package com.mocca.app.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.mocca.app.ui.screens.onboarding.OnboardingScreen
import com.mocca.app.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        Navigator(OnboardingScreen()) { navigator ->
            SlideTransition(navigator)
        }
    }
}
