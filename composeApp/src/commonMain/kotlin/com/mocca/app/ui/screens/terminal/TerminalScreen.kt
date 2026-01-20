package com.mocca.app.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
import com.mocca.app.util.parseAnsi

class TerminalScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<TerminalScreenModel>()
        val state by screenModel.state.collectAsState()
        val scrollState = rememberScrollState()
        val density = LocalDensity.current

        LaunchedEffect(Unit) {
            screenModel.connect()
        }

        LaunchedEffect(state.output) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        
        // ═══════════════════════════════════════════════════════════════════════════════
        // TERMINAL RESIZE UI (Priority 5.1)
        // ═══════════════════════════════════════════════════════════════════════════════
        
        // Approximate character dimensions for monospace font
        val charWidthDp = 8.dp
        val charHeightDp = 16.dp
        
        // Track container size for resize calculations
        var containerSize by remember { mutableStateOf(IntSize.Zero) }
        
        // Calculate and notify resize when container size changes
        LaunchedEffect(containerSize) {
            if (containerSize.width > 0 && containerSize.height > 0) {
                with(density) {
                    val cols = (containerSize.width / charWidthDp.toPx()).toInt().coerceAtLeast(20)
                    val rows = (containerSize.height / charHeightDp.toPx()).toInt().coerceAtLeast(5)
                    screenModel.resizeTerminal(cols, rows)
                }
            }
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
                
                // Show current terminal size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TerminalSpacing.md)
                ) {
                    Text(
                        text = "${state.currentCols}x${state.currentRows}",
                        style = TerminalTypography.code.copy(fontSize = 10.sp),
                        color = TerminalColors.grey
                    )
                    
                    TerminalIconButton(
                        icon = Icons.Default.Delete,
                        onClick = { screenModel.clearTerminal() },
                        contentDescription = "CLEAR"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            // Terminal Output with resize tracking
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(TerminalColors.surface)
                    .padding(TerminalSpacing.md)
                    .onSizeChanged { size ->
                        containerSize = size
                    }
                    .verticalScroll(scrollState)
            ) {
                // ═══════════════════════════════════════════════════════════════════════════════
                // ANSI COLOR PARSING (Priority 5.2)
                // ═══════════════════════════════════════════════════════════════════════════════
                
                // Parse ANSI escape sequences for colored output
                val parsedOutput = remember(state.output) {
                    state.output.parseAnsi(TerminalColors.statusOnline)
                }
                
                SelectionContainer {
                    Text(
                        text = parsedOutput,
                        style = TerminalTypography.code.copy(fontSize = 12.sp, lineHeight = 14.sp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            // ═══════════════════════════════════════════════════════════════════════════════
            // COMMAND HISTORY (Priority 5.3) - Input with history navigation
            // ═══════════════════════════════════════════════════════════════════════════════
            
            var inputText by remember { mutableStateOf("") }
            
            CommandLineInput(
                value = inputText,
                onValueChange = { 
                    inputText = it
                    screenModel.resetHistoryNavigation()
                },
                onSubmit = {
                    if (inputText.isNotBlank()) {
                        screenModel.sendInputWithHistory(inputText + "\n")
                        inputText = ""
                    }
                },
                onHistoryUp = {
                    screenModel.navigateHistoryUp(inputText)?.let { 
                        inputText = it 
                    }
                },
                onHistoryDown = {
                    screenModel.navigateHistoryDown()?.let { 
                        inputText = it 
                    }
                },
                placeholder = "ENTER_COMMAND..."
            )
        }
    }
}
