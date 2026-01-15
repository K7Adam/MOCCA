package com.mocca.app.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.mocca.app.ui.screens.onboarding.OnboardingScreen
import com.mocca.app.ui.theme.TerminalTheme

@Composable
fun App() {
    TerminalTheme {
        Navigator(OnboardingScreen()) { navigator ->
            SlideTransition(navigator)
        }
    }
}
