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
import com.mocca.app.ui.theme.moccaClickable
import com.mocca.app.ui.theme.*
import com.mocca.app.ui.TestTags
import androidx.compose.ui.platform.testTag
import com.mocca.app.util.AnsiParser

// Extension to convert TextStyle to SpanStyle
fun TextStyle.toSpanStyle(): SpanStyle = SpanStyle(
    fontSize = this.fontSize,
    fontWeight = this.fontWeight,
    fontStyle = this.fontStyle,
    fontFamily = this.fontFamily,
    letterSpacing = this.letterSpacing,
    textDecoration = this.textDecoration
)

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
    monoStyle: TextStyle,
    monoFontFamily: FontFamily,
    lineHeight: TextUnit,
    density: Density,
    onCharMetricsMeasured: (Float, Float) -> Unit
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
    val annotatedString = buildAnnotatedString {
        row.cells.forEachIndexed { cellIndex, cell ->
            val char = cell.char
            if (char.isNotBlank()) {
                // Determine if this cell has the cursor
                val hasCursor = cursorVisible && currentRowIndex == cursorY && cellIndex == cursorX
                
                // Apply cell colors (fg/bg) if present
                val fgColor = cell.fg?.let { parseHexColor(it) } ?: monoStyle.color
                val bgColor = cell.bg?.let { parseHexColor(it) } ?: Color.Transparent
                
                // Parse attributes (bold, italic, underline, etc.)
                val isBold = cell.attrs.contains("bold") || cell.attrs.contains("1")
                val isItalic = cell.attrs.contains("italic") || cell.attrs.contains("3")
                val isUnderline = cell.attrs.contains("underline") || cell.attrs.contains("4")
                val isStrikethrough = cell.attrs.contains("strikethrough") || cell.attrs.contains("9")
                val isDim = cell.attrs.contains("dim") || cell.attrs.contains("2")
                
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
            } else {
                // Empty cell - append space
                append(" ")
            }
        }
    }
    
    // Measure character dimensions for resize calculation (only once per session)
    val textMeasurer = rememberTextMeasurer()
    val measured = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!measured.value && row.cells.isNotEmpty()) {
            measured.value = true
            val sampleText = buildAnnotatedString {
                val sampleSpanStyle = monoStyle.toSpanStyle()
                withStyle(sampleSpanStyle) { append("M") }
            }
            val layoutResult = textMeasurer.measure(sampleText)
            val charWidth = layoutResult.size.width
            val charHeight = layoutResult.size.height
            if (charWidth > 0 && charHeight > 0) {
                onCharMetricsMeasured(charWidth.toFloat(), charHeight.toFloat())
            }
        }
    }

    Text(
        text = annotatedString,
        style = monoStyle,
        softWrap = false,
        modifier = Modifier
            .height(with(density) { lineHeight.roundToPx().dp })
            .fillMaxWidth()
    )
}

// TERMINAL CONTENT AREA


@Composable
internal fun TerminalContent(
    tab: TerminalTab,
    currentCols: Int,
    currentRows: Int,
    onInput: (String) -> Unit,
    onResize: (cols: Int, rows: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val monoFontFamily = FontFamily.Monospace
    val lineHeight = 18.sp
    val monoStyle = TextStyle(
        fontFamily = monoFontFamily,
        lineHeight = lineHeight,
        color = AppColors.primary
    )

    // Measure actual character dimensions for accurate resize calculation
    var charWidthPx by remember { mutableStateOf(0f) }
    var charHeightPx by remember { mutableStateOf(0f) }

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
                            monoStyle = monoStyle,
                            monoFontFamily = monoFontFamily,
                            lineHeight = lineHeight,
                            density = density,
                            onCharMetricsMeasured = { width, height ->
                                charWidthPx = width
                                charHeightPx = height
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = AppColors.outline.copy(alpha = 0.3f))

        TerminalInputBar(
            isEnabled = tab.isConnected,
            onInput = onInput
        )
    }
}

// INPUT BAR


@Composable
internal fun TerminalInputBar(
    isEnabled: Boolean,
    onInput: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

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
                onDone = { 
                    if (inputText.isNotEmpty() && isEnabled) {
                        onInput(inputText + "\n")
                        inputText = ""
                    }
                }
            )
        )

        // Send button
        Box(
                modifier = Modifier
                    .size(AppSpacing.xxl)
                    .moccaClickable(
                    onClick = {
                        if (inputText.isNotEmpty() && isEnabled) {
                            onInput(inputText + "\n")
                            inputText = ""
                        }
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

// TERMINAL CAPABILITY MISSING STATE

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
