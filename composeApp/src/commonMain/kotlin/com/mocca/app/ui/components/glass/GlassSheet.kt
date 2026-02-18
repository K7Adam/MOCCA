package com.mocca.app.ui.components.glass

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing

/**
 * Glass-styled bottom sheet container.
 * 
 * A glass-material bottom sheet optimized for modal content.
 * Features rounded top corners and glass effect with proper drag handle.
 * 
 * Usage:
 * ```kotlin
 * var showSheet by remember { mutableStateOf(false) }
 * 
 * if (showSheet) {
 *     GlassSheet(
 *         onDismissRequest = { showSheet = false }
 *     ) {
 *         Text("Sheet content")
 *     }
 * }
 * ```
 * 
 * @param onDismissRequest Callback when the sheet should be dismissed
 * @param content Sheet content
 * @param modifier Modifier for the sheet
 * @param tokens Theme-aware glass tokens
 * @param shape Shape of the sheet (default: rounded top corners)
 * @param reducedTransparency Accessibility mode
 * @param dragEnabled Whether the sheet can be dragged to dismiss
 * @param peekHeight Initial peek height of the sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    shape: Shape = GlassDefaults.shapeSheet(),
    reducedTransparency: Boolean = false,
    dragEnabled: Boolean = true,
    peekHeight: Dp = 400.dp
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = shape,
        containerColor = if (reducedTransparency) {
            tokens.fallbackBackground
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        dragHandle = {
            GlassSheetDragHandle(tokens = tokens)
        },
        modifier = modifier
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            tokens = tokens,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shaderParams = GlassShaderParams.Sheet,
            reducedTransparency = reducedTransparency
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppSpacing.screenPaddingBottom),
                content = content
            )
        }
    }
}

/**
 * Simple glass sheet without Material3 ModalBottomSheet.
 * Use this when you need more control over the sheet behavior.
 * 
 * @param visible Whether the sheet is visible
 * @param onDismissRequest Callback when the sheet should be dismissed
 * @param content Sheet content
 * @param modifier Modifier for the sheet
 * @param tokens Theme-aware glass tokens
 * @param sheetHeight Height of the sheet (can be fractional for percentage)
 * @param reducedTransparency Accessibility mode
 */
@Composable
fun GlassSheetSimple(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    sheetHeight: Dp = 400.dp,
    reducedTransparency: Boolean = false
) {
    if (visible) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .navigationBarsPadding()
        ) {
            GlassSheetContent(
                tokens = tokens,
                reducedTransparency = reducedTransparency,
                onDrag = { onDismissRequest() },
                content = content
            )
        }
    }
}

/**
 * Internal content for the glass sheet.
 */
@Composable
private fun GlassSheetContent(
    tokens: GlassThemeTokens,
    reducedTransparency: Boolean,
    onDrag: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var dragProgress by remember { mutableFloatStateOf(0f) }
    
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragProgress > 0.3f) {
                            onDrag()
                        }
                        dragProgress = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragProgress += -dragAmount / size.height
                    }
                )
            },
        tokens = tokens,
        shape = AppShapes.bottomSheet,
        shaderParams = GlassShaderParams.Sheet,
        reducedTransparency = reducedTransparency
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Drag handle
            GlassSheetDragHandle(tokens = tokens)
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenPaddingHorizontal)
                    .padding(bottom = AppSpacing.screenPaddingBottom),
                content = content
            )
        }
    }
}

/**
 * Drag handle for the glass sheet.
 */
@Composable
private fun GlassSheetDragHandle(
    tokens: GlassThemeTokens,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.15f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .glassButton(tokens = tokens)
        )
    }
}

/**
 * Glass sheet with header.
 * 
 * @param title Header title content
 * @param onDismissRequest Callback when the sheet should be dismissed
 * @param content Sheet content
 * @param modifier Modifier for the sheet
 * @param tokens Theme-aware glass tokens
 * @param reducedTransparency Accessibility mode
 */
@Composable
fun GlassSheetWithHeader(
    title: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    reducedTransparency: Boolean = false
) {
    GlassSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        tokens = tokens,
        reducedTransparency = reducedTransparency,
        content = {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenPaddingHorizontal)
                    .padding(bottom = AppSpacing.md)
            ) {
                title()
            }
            
            // Content
            content()
        }
    )
}

/**
 * Full-screen glass sheet that covers the entire screen.
 * 
 * @param visible Whether the sheet is visible
 * @param onDismissRequest Callback when the sheet should be dismissed
 * @param content Sheet content
 * @param modifier Modifier for the sheet
 * @param tokens Theme-aware glass tokens
 * @param reducedTransparency Accessibility mode
 */
@Composable
fun GlassSheetFullscreen(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    reducedTransparency: Boolean = false
) {
    if (visible) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .navigationBarsPadding()
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxSize(),
                tokens = tokens,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shaderParams = GlassShaderParams.Sheet.copy(
                    refractionStrength = 0.3f,
                    chromaticAberration = 0.006f
                ),
                reducedTransparency = reducedTransparency,
                content = content
            )
        }
    }
}

/**
 * Glass sheet variant with integrated close button.
 * 
 * @param title Header title content
 * @param onDismissRequest Callback when the sheet should be dismissed
 * @param content Sheet content
 * @param onClose Click handler for close button (defaults to onDismissRequest)
 * @param modifier Modifier for the sheet
 * @param tokens Theme-aware glass tokens
 * @param reducedTransparency Accessibility mode
 */
@Composable
fun GlassSheetDismissible(
    title: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    onClose: () -> Unit = onDismissRequest,
    modifier: Modifier = Modifier,
    tokens: GlassThemeTokens = GlassDefaults.tokens(),
    reducedTransparency: Boolean = false
) {
    GlassSheetWithHeader(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    title()
                }
                
                // Close button
                IconButton(
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = tokens.strokeColor
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        tokens = tokens,
        reducedTransparency = reducedTransparency,
        content = content
    )
}
