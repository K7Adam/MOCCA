package com.mocca.app.ui.components.modern

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.MoccaTheme

/**
 * Modern Material 3 Expressive Button with advanced motion and theme integration.
 */
@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = MoccaTheme.colors.primary,
    contentColor: Color = MoccaTheme.colors.onPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "modernButtonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(MoccaButtonDefaults.Height)
            .scale(scale),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = MoccaTheme.shapes.pill,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(MoccaButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(MoccaButtonDefaults.SpacingIcon))
            }
            Text(
                text = text.uppercase(),
                style = MoccaTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Modern Material 3 Expressive FAB with enhanced motion.
 */
@Composable
fun ModernFAB(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    containerColor: Color = MoccaTheme.colors.primaryContainer,
    contentColor: Color = MoccaTheme.colors.onPrimaryContainer
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "modernFABScale"
    )

    LargeFloatingActionButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        containerColor = containerColor,
        contentColor = contentColor,
        shape = AppShapes.extraLarge,
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(32.dp)
        )
    }
}
