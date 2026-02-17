package com.mocca.app.ui.screens.console

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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.ui.components.modern.ModernHeader
import com.mocca.app.ui.components.modern.MoccaIconButton
import com.mocca.app.ui.components.modern.CommandLineInput
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.util.parseAnsi

class ConsoleScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ConsoleScreenModel>()
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
                .background(AppColors.background)
                .padding(AppSpacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    MoccaIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { navigator.pop() },
                        iconColor = AppColors.white
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    ModernHeader(text = "REMOTE CONSOLE")
                }
                
                // Show current terminal size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    Text(
                        text = "${state.currentCols}x${state.currentRows}",
                        style = AppTypography.code.copy(fontSize = 10.sp),
                        color = AppColors.grey
                    )
                    
                    MoccaIconButton(
                        icon = Icons.Default.Delete,
                        onClick = { screenModel.clearTerminal() },
                        contentDescription = "CLEAR"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            // Terminal Output with resize tracking
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(AppSpacing.md)
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
                    state.output.parseAnsi(AppColors.statusOnline)
                }
                
                SelectionContainer {
                    Text(
                        text = parsedOutput,
                        style = AppTypography.code.copy(fontSize = 12.sp, lineHeight = 14.sp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
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
