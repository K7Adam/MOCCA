package com.mocca.app.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing

/**
 * Terminal-styled card containers with blocky appearance.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// BASIC TERMINAL CARD
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Basic terminal card with white border and black background.
 * Sharp corners (0dp), no elevation.
 */
@Composable
fun TerminalCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.surface,
    borderColor: Color = TerminalColors.border,
    borderWidth: Dp = TerminalSpacing.borderThin,
    contentPadding: Dp = TerminalSpacing.cardPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(backgroundColor, RectangleShape)
            .border(borderWidth, borderColor, RectangleShape)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Terminal card with prominent white border (2dp).
 * Used for input fields, status monitors, important containers.
 */
@Composable
fun TerminalCardElevated(
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.background,
    borderColor: Color = TerminalColors.borderLight,
    borderWidth: Dp = TerminalSpacing.borderStandard,
    contentPadding: Dp = TerminalSpacing.cardPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(backgroundColor, RectangleShape)
            .border(borderWidth, borderColor, RectangleShape)
            .padding(contentPadding),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// STATUS MONITOR CARD (with decorative L-brackets)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Status monitor container with decorative L-shaped corner brackets.
 * Used on onboarding screen for the hero status section.
 */
@Composable
fun StatusMonitorCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.background,
    borderColor: Color = TerminalColors.borderLight,
    borderWidth: Dp = TerminalSpacing.borderStandard,
    bracketLength: Dp = 16.dp,
    bracketThickness: Dp = 4.dp,
    contentPadding: Dp = TerminalSpacing.xl,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RectangleShape)
            .border(borderWidth, borderColor, RectangleShape)
            .drawBehind {
                val bracketLengthPx = bracketLength.toPx()
                val bracketThicknessPx = bracketThickness.toPx()
                
                // Top-left L-bracket
                // Horizontal part
                drawRect(
                    color = borderColor,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(bracketLengthPx, bracketThicknessPx)
                )
                // Vertical part
                drawRect(
                    color = borderColor,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(bracketThicknessPx, bracketLengthPx)
                )
                
                // Top-right L-bracket
                drawRect(
                    color = borderColor,
                    topLeft = Offset(size.width - bracketLengthPx, 0f),
                    size = androidx.compose.ui.geometry.Size(bracketLengthPx, bracketThicknessPx)
                )
                drawRect(
                    color = borderColor,
                    topLeft = Offset(size.width - bracketThicknessPx, 0f),
                    size = androidx.compose.ui.geometry.Size(bracketThicknessPx, bracketLengthPx)
                )
                
                // Bottom-left L-bracket
                drawRect(
                    color = borderColor,
                    topLeft = Offset(0f, size.height - bracketThicknessPx),
                    size = androidx.compose.ui.geometry.Size(bracketLengthPx, bracketThicknessPx)
                )
                drawRect(
                    color = borderColor,
                    topLeft = Offset(0f, size.height - bracketLengthPx),
                    size = androidx.compose.ui.geometry.Size(bracketThicknessPx, bracketLengthPx)
                )
                
                // Bottom-right L-bracket
                drawRect(
                    color = borderColor,
                    topLeft = Offset(size.width - bracketLengthPx, size.height - bracketThicknessPx),
                    size = androidx.compose.ui.geometry.Size(bracketLengthPx, bracketThicknessPx)
                )
                drawRect(
                    color = borderColor,
                    topLeft = Offset(size.width - bracketThicknessPx, size.height - bracketLengthPx),
                    size = androidx.compose.ui.geometry.Size(bracketThicknessPx, bracketLengthPx)
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION CARD (with left border indicator)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Card with left border accent, used for console log sections.
 */
@Composable
fun TerminalSectionCard(
    modifier: Modifier = Modifier,
    leftBorderColor: Color = TerminalColors.grey,
    leftBorderWidth: Dp = TerminalSpacing.borderThin,
    backgroundColor: Color = Color.Transparent,
    contentPadding: Dp = TerminalSpacing.lg,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(backgroundColor)
            .drawBehind {
                drawRect(
                    color = leftBorderColor,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(leftBorderWidth.toPx(), size.height)
                )
            }
            .padding(start = contentPadding),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// SESSION CARD (with active indicator)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Session/conversation list item card with active indicator.
 */
@Composable
fun TerminalSessionCard(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    backgroundColor: Color = TerminalColors.surface,
    borderColor: Color = TerminalColors.border,
    activeIndicatorColor: Color = TerminalColors.activeIndicator,
    activeIndicatorWidth: Dp = TerminalSpacing.activeIndicatorWidth,
    contentPadding: Dp = TerminalSpacing.cardPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RectangleShape)
            .then(
                if (isActive) {
                    Modifier.drawBehind {
                        drawRect(
                            color = activeIndicatorColor,
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(
                                activeIndicatorWidth.toPx(),
                                size.height
                            )
                        )
                    }
                } else {
                    Modifier
                }
            )
            .border(TerminalSpacing.borderThin, borderColor, RectangleShape)
            .padding(contentPadding),
        content = content
    )
}
