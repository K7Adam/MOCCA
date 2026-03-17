package com.mocca.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppTypography

/**
 * MOCCA GodBlocks - High-fidelity UI primitives for the "God Mode" aesthetic.
 * Translates HTML/Tailwind mocks from ui_overhaul_refactoring into Compose.
 */

/**
 * A sticky, blurred header with a pitch-black background and subtle bottom border.
 * Matches the header in git_version_control_center/code.html.
 */
@Composable
fun GodHeader(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    subtitle: String? = null,
    subtitleIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.surfaceContainer, AppShapes.none)
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(AppColors.primary, AppShapes.circle)
                ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                        tint = AppColors.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (subtitle != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        subtitleIcon?.invoke()
                        Text(
                            text = subtitle,
                            style = AppTypography.labelSmall,
                            color = AppColors.white.copy(alpha = 0.4f)
                        )
                    }
                }
                Text(
                    text = title,
                    style = AppTypography.titleMedium,
                    color = AppColors.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions()
            }
        }
    }
}

/**
 * A rounded, elevated card for content sections.
 * Matches the card style in the UI overhaul.
 */
@Composable
fun GodCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.surface,
    shape: Shape = AppShapes.large,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = shape,
        border = BorderStroke(1.dp, AppColors.primary)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

/**
 * A pill-shaped button with high contrast or accent colors.
 */
@Composable
fun GodButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AppColors.accentGreen,
    contentColor: Color = AppColors.background,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = AppShapes.pill,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            icon?.invoke()
        }
    }
}

/**
 * A standard list item with a leading icon/avatar and trailing metadata.
 */
@Composable
fun GodListItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = Color.Transparent,
        shape = AppShapes.large
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppColors.surfaceVariant, AppShapes.large),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTypography.titleSmall,
                    color = AppColors.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = AppTypography.bodySmall,
                    color = AppColors.white.copy(alpha = 0.4f)
                )
            }

            trailing?.invoke()
        }
    }
}

/**
 * A status badge with a colored background.
 */
@Composable
fun GodBadge(
    text: String,
    containerColor: Color = AppColors.primary.copy(alpha = 0.2f),
    contentColor: Color = AppColors.primary
) {
    Surface(
        color = containerColor,
        shape = AppShapes.small
) {
        Text(
            text = text.uppercase(),
            style = AppTypography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
