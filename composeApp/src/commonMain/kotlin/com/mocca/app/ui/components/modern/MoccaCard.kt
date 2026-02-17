package com.mocca.app.ui.components.modern

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
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing

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
fun MoccaCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.surfaceContainer,
    borderColor: Color = AppColors.border,
    borderWidth: Dp = AppSpacing.borderThin,
    contentPadding: Dp = AppSpacing.cardPadding,
    shape: Shape = AppShapes.card,
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
fun MoccaCardElevated(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.cardBackground,
    borderColor: Color = AppColors.borderLight,
    borderWidth: Dp = AppSpacing.borderThin,
    contentPadding: Dp = AppSpacing.cardPadding,
    shape: Shape = AppShapes.card,
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
    backgroundColor: Color = AppColors.surfaceContainer,
    borderColor: Color = AppColors.border,
    borderWidth: Dp = AppSpacing.borderThin,
    contentPadding: Dp = AppSpacing.cardPaddingLarge,
    shape: Shape = AppShapes.extraLarge,
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
fun MoccaSectionCard(
    modifier: Modifier = Modifier,
    leftBorderColor: Color = AppColors.accentGreen,
    leftBorderWidth: Dp = AppSpacing.borderStandard,
    backgroundColor: Color = AppColors.surfaceVariant,
    contentPadding: Dp = AppSpacing.lg,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(AppShapes.medium)
            .background(backgroundColor, AppShapes.medium)
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
fun MoccaSessionCard(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    backgroundColor: Color = AppColors.surfaceContainer,
    borderColor: Color = AppColors.border,
    activeIndicatorColor: Color = AppColors.accentGreen,
    activeIndicatorWidth: Dp = AppSpacing.activeIndicatorWidth,
    contentPadding: Dp = AppSpacing.cardPadding,
    shape: Shape = AppShapes.sessionCard,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isActive) backgroundColor.copy(alpha = 0.5f) else backgroundColor, 
                shape
            )
            .border(AppSpacing.borderThin, if (isActive) activeIndicatorColor else borderColor, shape)
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
    backgroundColor: Color = AppColors.moduleBackground,
    borderColor: Color = AppColors.border,
    contentPadding: Dp = AppSpacing.modulePadding,
    shape: Shape = AppShapes.moduleCard,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(AppSpacing.borderThin, borderColor, shape)
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
    backgroundColor: Color = AppColors.glassBackground,
    borderColor: Color = AppColors.glassBorder,
    contentPadding: Dp = AppSpacing.cardPadding,
    shape: Shape = AppShapes.card,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(AppSpacing.borderThin, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}
