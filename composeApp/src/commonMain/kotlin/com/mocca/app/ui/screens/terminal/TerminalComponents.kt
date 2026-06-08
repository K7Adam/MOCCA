package com.mocca.app.ui.screens.terminal

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.input.key.*
import com.mocca.app.ui.theme.moccaClickable
import com.mocca.app.ui.theme.*
import com.mocca.app.ui.TestTags
import androidx.compose.ui.platform.testTag
import com.mocca.app.util.AnsiParser

// TAB BAR


@Composable
internal fun TerminalTabBar(
    tabs: List<TerminalTab>,
    activeTabId: String?,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(AppColors.surface)
            .border(
                AppSpacing.borderThin,
                AppColors.outline.copy(alpha = 0.3f)
            )
            .horizontalScroll(scrollState)
            .testTag(TestTags.Terminal.tabBar),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            TerminalTabItem(
                tab = tab,
                isActive = tab.terminal.id == activeTabId,
                onSelected = { onTabSelected(tab.terminal.id) },
                onClosed = { onTabClosed(tab.terminal.id) }
            )
        }
    }
}

@Composable
internal fun TerminalTabItem(
    tab: TerminalTab,
    isActive: Boolean,
    onSelected: () -> Unit,
    onClosed: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) AppColors.surfaceContainerHigh else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) AppColors.onSurface else AppColors.onSurfaceVariant,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "tabText"
    )

    Row(
        modifier = Modifier
            .height(44.dp)
            .widthIn(min = 90.dp, max = 180.dp)
            .background(bgColor)
            .moccaClickable(onClick = onSelected, pressedScale = 0.98f)
            .padding(horizontal = AppSpacing.sm)
            .testTag("terminal_tab_${tab.terminal.id}"),
        verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        // Status indicator dot
        val dotColor = when {
            tab.isConnecting -> AppColors.onSurfaceVariant
            tab.isConnected -> AppColors.statusOnline
            tab.error != null -> AppColors.error
            else -> AppColors.outline
        }
        Box(
            modifier = Modifier
                .size(AppSpacing.statusDotSize)
                .clip(AppShapes.circle)
                .background(dotColor)
        )

        Text(
            text = tab.displayTitle,
            style = AppTypography.labelSmall,
            color = textColor,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false)
        )

        // Close button — only shown when tab is active
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(AppSpacing.iconSizeSmall)
                    .clip(AppShapes.circle)
                    .moccaClickable(onClick = onClosed, pressedScale = 0.9f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close terminal",
                    tint = AppColors.onSurfaceVariant,
                    modifier = Modifier.size(AppSpacing.iconSizeSmall)
                )
            }
        }
    }
}

// TERMINAL ROW - Renders a single terminal row with ANSI colors, cell attributes, and cursor

@Composable
private fun TerminalRow(
    row: com.mocca.app.domain.model.TerminalGridRow,
    cursorX: Int,
    cursorY: Int,
    cursorVisible: Boolean,
    cursorStyle: String,
    currentRowIndex: Int,
    terminalCols: Int,
    monoStyle: TextStyle,
    monoFontFamily: FontFamily,
    lineHeight: TextUnit,
    density: Density,

) {
    // Helper to parse hex color string
    fun parseHexColor(hex: String?): Color? = hex?.let {
        try {
            val cleanHex = if (it.startsWith("#")) it.substring(1) else it
            val colorLong = cleanHex.toLong(16)
            val colorInt = colorLong.toInt()
            // Handle both ARGB (8 chars) and RGB (6 chars)
            if (cleanHex.length == 8) {
                Color(colorInt)
            } else if (cleanHex.length == 6) {
                Color(colorInt or 0xFF000000.toInt())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Build annotated string from cells with ANSI colors and attributes
    // Render cells up to terminalCols, filling missing cells with spaces
    val hasCursorInThisRow = cursorVisible && currentRowIndex == cursorY
    val annotatedString = remember(row, hasCursorInThisRow, if (hasCursorInThisRow) cursorX else -1, if (hasCursorInThisRow) cursorStyle else "", terminalCols, monoStyle.color) {
        buildAnnotatedString {
        for (cellIndex in 0 until terminalCols) {
            val cell = if (cellIndex < row.cells.size) row.cells[cellIndex] else null
            val char = cell?.char ?: " "
            
            // Determine if this cell has the cursor
            val hasCursor = hasCursorInThisRow && cellIndex == cursorX
            
            // Apply cell colors (fg/bg) if present
            val fgColor = cell?.fg?.let { parseHexColor(it) } ?: monoStyle.color
            val bgColor = cell?.bg?.let { parseHexColor(it) } ?: Color.Transparent
            
            // Parse attributes (bold, italic, underline, etc.)
            val attrs = cell?.attrs ?: emptyList()
            val isBold = attrs.contains("bold") || attrs.contains("1")
            val isItalic = attrs.contains("italic") || attrs.contains("3")
            val isUnderline = attrs.contains("underline") || attrs.contains("4")
            val isStrikethrough = attrs.contains("strikethrough") || attrs.contains("9")
            val isDim = attrs.contains("dim") || attrs.contains("2")
            
            // If cursor is on this cell, invert colors for block cursor
            val (finalFg, finalBg) = if (hasCursor && cursorStyle == "block") {
                bgColor to fgColor
            } else {
                fgColor to bgColor
            }
            
            val spanStyle = SpanStyle(
                color = if (isDim) finalFg.copy(alpha = 0.5f) else finalFg,
                background = finalBg,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                textDecoration = when {
                    isUnderline && isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                    isUnderline -> TextDecoration.Underline
                    isStrikethrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                }
            )
            withStyle(spanStyle) {
                append(char)
            }
        }
    }
    
    }

    Text(
        text = annotatedString,
        style = monoStyle,
        softWrap = false,
        modifier = Modifier.fillMaxWidth()
    )
}

// TERMINAL CONTENT AREA


@Composable
internal fun TerminalContent(
    tab: TerminalTab,
    currentCols: Int,
    inputMode: TerminalInputMode,
    onInputModeChange: (TerminalInputMode) -> Unit,
    currentRows: Int,
    fontSizeSp: Float,
    onInput: (String) -> Unit,
    onResize: (cols: Int, rows: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val monoFontFamily = FontFamily.Monospace
    val fontSize = fontSizeSp.sp
    val lineHeight = fontSize
    val monoStyle = TextStyle(
        fontFamily = monoFontFamily,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = AppColors.primary
    )

    // Measure actual character dimensions for accurate resize calculation
    val textMeasurer = rememberTextMeasurer()
    var charWidthPx by remember { mutableStateOf(0f) }
    var charHeightPx by remember { mutableStateOf(0f) }

    LaunchedEffect(monoStyle, density) {
        val sampleText = buildAnnotatedString {
            withStyle(monoStyle.toSpanStyle()) { append("M") }
        }
        val layoutResult = textMeasurer.measure(sampleText)
        charWidthPx = layoutResult.size.width.toFloat()
        charHeightPx = layoutResult.size.height.toFloat()
    }

    Column(
        modifier = modifier
            .background(AppColors.background)
            .testTag(TestTags.Terminal.content)
    ) {

        val outputListState = rememberLazyListState()

        // Track if user has scrolled up (not at bottom)
        var isAtBottom by remember { mutableStateOf(true) }
        
        // Auto-scroll to bottom only when new content arrives and user is at bottom
        LaunchedEffect(tab.grid.scrollbackLength, tab.grid.rowData.size) {
            if (isAtBottom) {
                val last = (tab.grid.rowData.size - 1).coerceAtLeast(0)
                outputListState.animateScrollToItem(last)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                // Measure size to compute cols/rows for resize using actual font metrics
                .onSizeChanged { size ->
                    if (size.width > 0 && size.height > 0) {
                        // Use actual measured font metrics if available, otherwise fall back to approximation
                        val cw = if (charWidthPx > 0) charWidthPx else with(density) { 8.sp.toPx() }
                        val ch = if (charHeightPx > 0) charHeightPx else with(density) { lineHeight.toPx() }
                        val cols = (size.width / cw).toInt().coerceAtLeast(40)
                        val rows = (size.height / ch).toInt().coerceAtLeast(10)
                        if (cols != currentCols || rows != currentRows) {
                            onResize(cols, rows)
                        }
                    }
                }
        ) {
            if (tab.error != null && !tab.isConnected) {
                // Error state overlay
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = AppColors.error,
                        modifier = Modifier.size(AppSpacing.xxl)
                    )
                    Spacer(Modifier.height(AppSpacing.sm))
                    Text(
                        text = tab.error,
                        style = monoStyle.copy(color = AppColors.error),
                        modifier = Modifier.padding(horizontal = AppSpacing.lg)
                    )
                }
            } else {
                val hScrollState = rememberScrollState()
                LazyColumn(
                    state = outputListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(hScrollState)
                        .padding(AppSpacing.xs)
                        .onGloballyPositioned { layoutInfo ->
                            // Detect if user has scrolled away from bottom
                            val atBottom = outputListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index 
                                ?.let { it >= tab.grid.rowData.size - 3 } 
                                ?: true
                            isAtBottom = atBottom
                        }
                        .testTag(TestTags.Terminal.outputArea),
                    userScrollEnabled = true
                ) {
                    if (tab.isConnecting && tab.grid.rowData.all { it.text.isBlank() }) {
                        item(key = "connecting", contentType = "terminal-status") {
                            Text("Connecting...", style = monoStyle)
                        }
                    }
                    items(
                        items = tab.grid.rowData,
                        key = { row -> row.index },
                        contentType = { "terminal-row" }
                    ) { row ->
                        TerminalRow(
                            row = row,
                            cursorX = tab.grid.cursorX,
                            cursorY = tab.grid.cursorY,
                            cursorVisible = tab.grid.cursorVisible,
                            cursorStyle = tab.grid.cursorStyle,
                            currentRowIndex = row.index,
                            terminalCols = tab.grid.cols,
                            monoStyle = monoStyle,
                            monoFontFamily = monoFontFamily,
                            lineHeight = lineHeight,
                            density = density
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = AppColors.outline.copy(alpha = 0.3f))

        TerminalAccessoryToolbar(
            inputMode = inputMode,
            onInputModeChange = onInputModeChange,
            onSpecialKey = onInput,
            isEnabled = tab.isConnected
        )

        HorizontalDivider(color = AppColors.outline.copy(alpha = 0.3f))

        if (inputMode == TerminalInputMode.INTERACTIVE) {
            TerminalInteractiveInputBar(
                isEnabled = tab.isConnected,
                onInput = onInput
            )
        } else {
            TerminalInputBar(isEnabled = tab.isConnected, onInput = onInput) } /*
            isEnabled = tab.isConnected,
            onInput = onInput
        )
    }
}

*/ } } // INPUT BAR


@Composable
internal fun TerminalInputBar(
    isEnabled: Boolean,
    onInput: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    fun submitInput() {
        if (inputText.isNotEmpty() && isEnabled) {
            onInput(inputText + "\r\n")
            inputText = ""
        }
    }

    LaunchedEffect(isEnabled) {
        if (isEnabled) focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
            .testTag(TestTags.Terminal.inputBar),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        // Prompt symbol
        Text(
            text = "❯",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                color = AppColors.primary,
                fontWeight = FontWeight.Bold
            )
        )

        // Input field with terminal-specific key handling
        BasicTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .testTag(TestTags.Terminal.inputField),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                color = AppColors.onSurface
            ),
            cursorBrush = SolidColor(AppColors.primary),
            singleLine = true,
            enabled = isEnabled,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send,
                keyboardType = KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(
                onSend = { submitInput() },
                onDone = { submitInput() }
            )
        )

        // Send button
        Box(
                modifier = Modifier
                    .size(AppSpacing.xxl)
                    .moccaClickable(
                    onClick = {
                        submitInput()
                    },
                    enabled = isEnabled && inputText.isNotEmpty(),
                    pressedScale = 0.92f
                )
                .testTag(TestTags.Terminal.sendButton),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (isEnabled && inputText.isNotEmpty()) AppColors.primary else AppColors.outline,
                modifier = Modifier.size(AppSpacing.iconSizeSmall)
            )
        }
    }
}

// EMPTY STATE


@Composable
internal fun TerminalEmptyState(
    isCreating: Boolean,
    onCreateClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().testTag(TestTags.Terminal.emptyState),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
        ) {
            Icon(
                Icons.Default.Terminal,
                contentDescription = null,
                tint = AppColors.outline,
                modifier = Modifier.size(AppSpacing.xxxl)
            )
            Text(
                "NO TERMINAL SESSIONS",
                style = AppTypography.labelMedium,
                color = AppColors.outline
            )
            Button(
                onClick = onCreateClick,
                enabled = !isCreating,
                shape = AppShapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary,
                    contentColor = AppColors.background
                )
            ) {
                if (isCreating) {
                    LoadingIndicator(
                        modifier = Modifier.size(AppSpacing.iconSizeSmall),
                        color = AppColors.background
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(AppSpacing.iconSizeSmall))
                }
                Spacer(Modifier.width(AppSpacing.inlineGap))
                Text(
                    "NEW TERMINAL",
                    style = AppTypography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// BRIDGE DISCONNECTED STATE

@Composable
internal fun TerminalDisconnectedState(
    onConnectClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().testTag(TestTags.Terminal.disconnectedState),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
        ) {
            Icon(
                Icons.Default.LinkOff,
                contentDescription = null,
                tint = AppColors.outline,
                modifier = Modifier.size(AppSpacing.xxxl)
            )
            Text(
                "CLI BRIDGE DISCONNECTED",
                style = AppTypography.titleMedium,
                color = AppColors.onSurface
            )
            Text(
                "Connect to the MOCCA CLI bridge to use the terminal.",
                style = AppTypography.bodyMedium,
                color = AppColors.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = AppSpacing.xl)
            )
            Button(
                onClick = onConnectClick,
                shape = AppShapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary,
                    contentColor = AppColors.background
                )
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(AppSpacing.iconSizeSmall))
                Spacer(Modifier.width(AppSpacing.inlineGap))
                Text(
                    "CONNECT TO CLI BRIDGE",
                    style = AppTypography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// INTERACTIVE INPUT BAR & ACCESSORY TOOLBAR

@Composable
internal fun TerminalInteractiveInputBar(
    isEnabled: Boolean,
    onInput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(" ", TextRange(1))) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
            .testTag("terminal_interactive_input_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        // Prompt/Indicator
        Text(
            text = "⚡",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                color = AppColors.statusOnline,
                fontWeight = FontWeight.Bold
            )
        )

        // Custom real-time input field
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                if (!isEnabled) return@BasicTextField
                
                val oldText = textFieldValue.text
                val newText = newValue.text

                if (newText.length > oldText.length) {
                    // Character(s) inserted (e.g. typing or autocomplete/paste)
                    val addedText = newText.substring(oldText.length)
                    // If the user pressed enter, send Carriage Return \r instead of \n
                    val sentText = if (addedText == "\n") "\r" else addedText
                    onInput(sentText)
                } else if (newText.length < oldText.length) {
                    // Character deleted (Backspace)
                    onInput("\u007f") // Standard ASCII DEL / backspace
                }

                // Always keep the text field with a single space " " and cursor at index 1
                // to allow continuous backspace and input tracking.
                textFieldValue = TextFieldValue(" ", TextRange(1))
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    if (!isEnabled) return@onPreviewKeyEvent false
                    // Handle physical keys / emulator hardware keys that might not trigger onValueChange
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.Tab -> {
                                onInput("\t")
                                true
                            }
                            Key.Escape -> {
                                onInput("\u001b")
                                true
                            }
                            Key.Enter -> {
                                onInput("\r")
                                true
                            }
                            Key.Backspace -> {
                                onInput("\u007f")
                                true
                            }
                            Key.DirectionUp -> {
                                onInput("\u001b[A")
                                true
                            }
                            Key.DirectionDown -> {
                                onInput("\u001b[B")
                                true
                            }
                            Key.DirectionRight -> {
                                onInput("\u001b[C")
                                true
                            }
                            Key.DirectionLeft -> {
                                onInput("\u001b[D")
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                .testTag("terminal_interactive_input_field"),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                color = AppColors.onSurface
            ),
            cursorBrush = SolidColor(Color.Transparent), // Hide cursor to feel like real terminal typing
            singleLine = true,
            enabled = isEnabled,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.None,
                keyboardType = KeyboardType.Text,
                autoCorrect = false
            )
        )

        Text(
            text = "Interactive Mode",
            style = AppTypography.bodySmall,
            color = AppColors.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
internal fun TerminalAccessoryToolbar(
    inputMode: TerminalInputMode,
    onInputModeChange: (TerminalInputMode) -> Unit,
    onSpecialKey: (String) -> Unit,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(AppColors.surfaceContainerLow)
            .padding(horizontal = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Special Keys
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            TerminalToolbarButton(label = "ESC", onClick = { onSpecialKey("\u001b") }, isEnabled = isEnabled)
            TerminalToolbarButton(label = "TAB", onClick = { onSpecialKey("\t") }, isEnabled = isEnabled)
            TerminalToolbarButton(label = "CTRL+C", onClick = { onSpecialKey("\u0003") }, isEnabled = isEnabled)
            TerminalToolbarButton(label = "CTRL+D", onClick = { onSpecialKey("\u0004") }, isEnabled = isEnabled)
            TerminalToolbarButton(label = "CTRL+Z", onClick = { onSpecialKey("\u001a") }, isEnabled = isEnabled)
            
            Spacer(Modifier.width(AppSpacing.sm))
            
            // Directional keys (Up, Down, Left, Right)
            TerminalToolbarButton(label = "▲", onClick = { onSpecialKey("\u001b[A") }, isEnabled = isEnabled)
            TerminalToolbarButton(label = "▼", onClick = { onSpecialKey("\u001b[B") }, isEnabled = isEnabled)
            TerminalToolbarButton(label = "◀", onClick = { onSpecialKey("\u001b[D") }, isEnabled = isEnabled)
            TerminalToolbarButton(label = "▶", onClick = { onSpecialKey("\u001b[C") }, isEnabled = isEnabled)
        }

        // Right side: Mode Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            val isInteractive = inputMode == TerminalInputMode.INTERACTIVE
            Text(
                text = if (isInteractive) "Interactive" else "Line",
                style = AppTypography.labelSmall,
                color = AppColors.onSurfaceVariant
            )
            Switch(
                checked = isInteractive,
                onCheckedChange = { 
                    onInputModeChange(if (it) TerminalInputMode.INTERACTIVE else TerminalInputMode.LINE)
                },
                thumbContent = {
                    Icon(
                        imageVector = if (isInteractive) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppColors.primary,
                    checkedTrackColor = AppColors.primaryContainer,
                    uncheckedThumbColor = AppColors.outline,
                    uncheckedTrackColor = AppColors.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun TerminalToolbarButton(
    label: String,
    onClick: () -> Unit,
    isEnabled: Boolean
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 36.dp)
            .clip(AppShapes.extraSmall)
            .background(AppColors.surfaceContainerHigh)
            .moccaClickable(
                onClick = onClick,
                enabled = isEnabled,
                pressedScale = 0.92f
            )
            .padding(horizontal = AppSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppTypography.labelSmall.copy(fontSize = 10.sp),
            color = if (isEnabled) AppColors.onSurface else AppColors.outline,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun TerminalCapabilityMissingState() {
    Box(
        modifier = Modifier.fillMaxSize().testTag(TestTags.Terminal.capabilityMissingState),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = AppColors.warning,
                modifier = Modifier.size(AppSpacing.xxxl)
            )
            Text(
                "TERMINAL NOT SUPPORTED",
                style = AppTypography.titleMedium,
                color = AppColors.onSurface
            )
            Text(
                "The connected CLI bridge does not support the terminal capability (ptyGrid).\n" +
                "Please update your MOCCA CLI bridge to a version that supports terminal emulation.",
                style = AppTypography.bodyMedium,
                color = AppColors.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = AppSpacing.xl)
            )
        }
    }
}
