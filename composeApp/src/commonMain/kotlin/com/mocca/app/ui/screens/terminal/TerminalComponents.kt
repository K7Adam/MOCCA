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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocca.app.ui.theme.moccaClickable
import com.mocca.app.ui.theme.*

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
            .horizontalScroll(scrollState),
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
            .padding(horizontal = AppSpacing.sm),
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
    val monoStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        lineHeight = 18.sp,
        color = AppColors.primary
    )

    Column(modifier = modifier.background(AppColors.background)) {

        val outputListState = rememberLazyListState()

        LaunchedEffect(tab.grid.scrollbackLength, tab.grid.rowData.size) {
            val last = (tab.grid.rowData.size - 1).coerceAtLeast(0)
            outputListState.scrollToItem(last)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                // Measure size to compute cols/rows for resize
                .onSizeChanged { size ->
                    if (size.width > 0 && size.height > 0) {
                        // Approximate char size in monospace 13sp
                        val charWidthPx = with(density) { 8.sp.toPx() }
                        val charHeightPx = with(density) { 18.sp.toPx() }
                        val cols = (size.width / charWidthPx).toInt().coerceAtLeast(40)
                        val rows = (size.height / charHeightPx).toInt().coerceAtLeast(10)
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
                        .padding(AppSpacing.xs),
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
                        Text(
                            text = row.text,
                            style = monoStyle,
                            softWrap = false,
                            modifier = Modifier.height(18.dp)
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
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
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

        // Input field
        BasicTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                color = AppColors.onSurface
            ),
            cursorBrush = SolidColor(AppColors.primary),
            singleLine = true,
            enabled = isEnabled
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
                ),
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
        modifier = Modifier.fillMaxSize(),
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
