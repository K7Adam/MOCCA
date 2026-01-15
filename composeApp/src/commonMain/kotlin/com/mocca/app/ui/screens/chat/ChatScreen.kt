package com.mocca.app.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf

data class ChatScreen(val sessionId: String) : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ChatScreenModel> { parametersOf(sessionId) }
        
        // Handle navigation events
        LaunchedEffect(Unit) {
            screenModel.navigationEvent.collect { newSessionId ->
                navigator.push(ChatScreen(newSessionId))
            }
        }
        
        ChatContent(screenModel)
    }
}
