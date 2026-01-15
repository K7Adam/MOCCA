package com.mocca.app.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.terminal.CommandLineInput
import com.mocca.app.ui.components.terminal.TerminalHeader
import com.mocca.app.ui.components.terminal.TerminalIconButton
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

class TerminalScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<TerminalScreenModel>()
        val state by screenModel.state.collectAsState()
        val scrollState = rememberScrollState()

        LaunchedEffect(Unit) {
            screenModel.connect()
        }

        LaunchedEffect(state.output) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalColors.background)
                .padding(TerminalSpacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TerminalIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { navigator.pop() },
                        iconColor = TerminalColors.white
                    )
                    Spacer(modifier = Modifier.width(TerminalSpacing.md))
                    TerminalHeader(text = "REMOTE_TERMINAL", showBrackets = true)
                }
                
                TerminalIconButton(
                    icon = Icons.Default.Delete,
                    onClick = { screenModel.clearTerminal() },
                    contentDescription = "CLEAR"
                )
            }
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            // Terminal Output
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(TerminalColors.surface)
                    .padding(TerminalSpacing.md)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = state.output,
                    color = TerminalColors.statusOnline, // Green text
                    style = TerminalTypography.code.copy(fontSize = 12.sp, lineHeight = 14.sp),
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            // Input
            var inputText by remember { mutableStateOf("") }
            
            CommandLineInput(
                value = inputText,
                onValueChange = { inputText = it },
                onSubmit = {
                    if (inputText.isNotBlank()) {
                        screenModel.sendInput(inputText + "\n")
                        inputText = ""
                    }
                },
                placeholder = "ENTER_COMMAND..."
            )
        }
    }
}
