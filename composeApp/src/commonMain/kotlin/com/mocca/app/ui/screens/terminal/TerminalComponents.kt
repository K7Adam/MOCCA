package com.mocca.app.ui.screens.terminal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
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
import com.mocca.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════════
// TAB BAR
// ═══════════════════════════════════════════════════════════════════════════════

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
                AppColors.border.copy(alpha = 0.3f)
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
        animationSpec = tween(150),
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) AppColors.white else AppColors.textSecondary,
        animationSpec = tween(150),
        label = "tabText"
    )

    Row(
        modifier = Modifier
            .height(44.dp)
            .widthIn(min = 90.dp, max = 180.dp)
            .background(bgColor)
            .clickable(onClick = onSelected)
            .padding(horizontal = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Status indicator dot
        val dotColor = when {
            tab.isConnecting -> AppColors.textSecondary
            tab.isConnected -> AppColors.accent
            tab.error != null -> AppColors.error
            else -> AppColors.textTertiary
        }
        Box(
            modifier = Modifier
                .size(6.dp)
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
                    .size(20.dp)
                    .clip(AppShapes.circle)
                    .clickable(onClick = onClosed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close terminal",
                    tint = AppColors.textSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TERMINAL CONTENT AREA
// ═══════════════════════════════════════════════════════════════════════════════

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
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = AppColors.accent
    )

    Column(modifier = modifier.background(AppColors.background)) {
        // ── OUTPUT AREA ───────────────────────────────────────────────────────
        val outputScrollState = rememberScrollState(Int.MAX_VALUE)

        // Scroll to bottom whenever output changes
        LaunchedEffect(tab.output) {
            outputScrollState.animateScrollTo(Int.MAX_VALUE)
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
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = tab.error,
                        style = monoStyle.copy(color = AppColors.error, fontSize = 12.sp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                // Terminal output text — horizontally scrollable for wide output
                val hScrollState = rememberScrollState()
                Text(
                    text = tab.output.ifEmpty {
                        if (tab.isConnecting) "Connecting..." else ""
                    },
                    style = monoStyle,
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(hScrollState)
                        .verticalScroll(outputScrollState)
                        .padding(4.dp)
                )
            }
        }

        HorizontalDivider(color = AppColors.border.copy(alpha = 0.3f))

        // ── INPUT BAR ─────────────────────────────────────────────────────────
        TerminalInputBar(
            isEnabled = tab.isConnected,
            onInput = onInput
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// INPUT BAR
// ═══════════════════════════════════════════════════════════════════════════════

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
                fontSize = 14.sp,
                color = AppColors.accent,
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
                fontSize = 13.sp,
                color = AppColors.white
            ),
            cursorBrush = SolidColor(AppColors.accent),
            singleLine = true,
            enabled = isEnabled
        )

        // Send button
        IconButton(
            onClick = {
                if (inputText.isNotEmpty() && isEnabled) {
                    onInput(inputText + "\n")
                    inputText = ""
                }
            },
            modifier = Modifier.size(32.dp),
            enabled = isEnabled && inputText.isNotEmpty()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (isEnabled && inputText.isNotEmpty()) AppColors.accent else AppColors.textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════════════════════════════

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Terminal,
                contentDescription = null,
                tint = AppColors.textTertiary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "NO TERMINAL SESSIONS",
                style = AppTypography.labelMedium,
                color = AppColors.textTertiary,
                letterSpacing = 1.sp
            )
            Button(
                onClick = onCreateClick,
                enabled = !isCreating,
                shape = AppShapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.accent,
                    contentColor = AppColors.background
                )
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AppColors.background,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "NEW TERMINAL",
                    style = AppTypography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
