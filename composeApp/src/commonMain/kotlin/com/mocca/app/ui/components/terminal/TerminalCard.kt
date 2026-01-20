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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalShapes
import com.mocca.app.ui.theme.TerminalSpacing

/**
 * Modern MOCCA card containers with rounded corners.
 * Based on UI overhaul designs - soft, rounded aesthetic with subtle borders.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// BASIC TERMINAL CARD (16dp rounded corners)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Basic card with rounded corners and subtle border.
 * Default: 16dp rounded corners, dark surface background.
 */
@Composable
fun TerminalCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.surfaceContainer,
    borderColor: Color = TerminalColors.border,
    borderWidth: Dp = TerminalSpacing.borderThin,
    contentPadding: Dp = TerminalSpacing.cardPadding,
    shape: Shape = TerminalShapes.card,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Elevated card with subtle highlight and slightly thicker border.
 * Used for input fields, status monitors, important containers.
 */
@Composable
fun TerminalCardElevated(
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.cardBackground,
    borderColor: Color = TerminalColors.borderLight,
    borderWidth: Dp = TerminalSpacing.borderThin,
    contentPadding: Dp = TerminalSpacing.cardPadding,
    shape: Shape = TerminalShapes.card,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// STATUS MONITOR CARD (Glass effect with rounded corners)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Status monitor card with glassmorphic effect.
 * Used on onboarding screen for the hero status section.
 * 
 * Modern design: 24dp rounded corners, subtle glass background, soft border.
 */
@Composable
fun StatusMonitorCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.surfaceContainer,
    borderColor: Color = TerminalColors.border,
    borderWidth: Dp = TerminalSpacing.borderThin,
    contentPadding: Dp = TerminalSpacing.cardPaddingLarge,
    shape: Shape = TerminalShapes.extraLarge,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION CARD (with left accent border)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Card with left accent border, used for console log sections.
 * Retains left-border style but with rounded corners on right side.
 */
@Composable
fun TerminalSectionCard(
    modifier: Modifier = Modifier,
    leftBorderColor: Color = TerminalColors.accentGreen,
    leftBorderWidth: Dp = TerminalSpacing.borderStandard,
    backgroundColor: Color = TerminalColors.surfaceVariant,
    contentPadding: Dp = TerminalSpacing.lg,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(TerminalShapes.medium)
            .background(backgroundColor, TerminalShapes.medium)
            .drawBehind {
                drawRect(
                    color = leftBorderColor,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(leftBorderWidth.toPx(), size.height)
                )
            }
            .padding(start = contentPadding + leftBorderWidth, end = contentPadding, 
                     top = contentPadding, bottom = contentPadding),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// SESSION CARD (with active indicator)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Session/conversation list item card with active indicator.
 * Modern design: 24dp rounded corners, subtle border, left accent when active.
 */
@Composable
fun TerminalSessionCard(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    backgroundColor: Color = TerminalColors.surfaceContainer,
    borderColor: Color = TerminalColors.border,
    activeIndicatorColor: Color = TerminalColors.accentGreen,
    activeIndicatorWidth: Dp = TerminalSpacing.activeIndicatorWidth,
    contentPadding: Dp = TerminalSpacing.cardPadding,
    shape: Shape = TerminalShapes.sessionCard,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor, shape)
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
            .border(TerminalSpacing.borderThin, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODULE CARD (for dashboard modules/tools)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Module card for dashboard tools.
 * 28dp rounded corners per design specs.
 */
@Composable
fun ModuleToolCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.moduleBackground,
    borderColor: Color = TerminalColors.border,
    contentPadding: Dp = TerminalSpacing.modulePadding,
    shape: Shape = TerminalShapes.moduleCard,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(TerminalSpacing.borderThin, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// GLASS CARD (Glassmorphic effect)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Glass card with semi-transparent background for overlay effects.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = TerminalColors.glassBackground,
    borderColor: Color = TerminalColors.glassBorder,
    contentPadding: Dp = TerminalSpacing.cardPadding,
    shape: Shape = TerminalShapes.card,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(TerminalSpacing.borderThin, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}
