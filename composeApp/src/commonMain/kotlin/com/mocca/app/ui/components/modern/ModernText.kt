package com.mocca.app.ui.components.modern

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.delay

/**
 * Modern Glassmorphic text components.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// BASIC MODERN TEXT
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Basic modern text using system fonts.
 */
@Composable
fun ModernText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppColors.white,
    style: TextStyle = AppTypography.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    uppercase: Boolean = false
) {
    Text(
        text = if (uppercase) text.uppercase() else text,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = maxLines,
        overflow = overflow
    )
}

/**
 * Modern header text - bold, uppercase, with subtle prefix.
 */
@Composable
fun ModernHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppColors.white,
    prefix: String = "//",
    suffix: String = "",
    showBrackets: Boolean = false
) {
    val displayText = if (showBrackets) {
        "[$text]"
    } else {
        "$prefix $text $suffix"
    }
    
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // High-contrast accent indicator
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 18.dp)
                    .background(AppColors.accentGreen)
            )
            
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            
            Text(
                text = displayText.uppercase(),
                color = color,
                style = AppTypography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.xxs))
        
        // Decorative line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            AppColors.accentGreen.copy(alpha = 0.5f),
                            AppColors.accentGreen.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Terminal label text - small, uppercase, with letter spacing.
 * Used for status bar labels like "MODEL: CLAUDE OPUS 4.5"
 */
@Composable
fun TerminalLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppColors.greyLight
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelMedium
    )
}

/**
 * Terminal meta text - very small, dim, uppercase.
 * Used for timestamps, version info, meta data.
 */
@Composable
fun TerminalMeta(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppColors.grey
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelSmall
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// TYPEWRITER TEXT
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Text that types out character by character with optional blinking cursor.
 * 
 * @param text The text to display
 * @param typingDelayMs Delay between each character (ms)
 * @param initialDelayMs Delay before starting to type (ms)
 * @param showCursor Whether to show a blinking cursor
 * @param onComplete Callback when typing is complete
 */
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppColors.white,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    typingDelayMs: Long = 50L,
    initialDelayMs: Long = 0L,
    showCursor: Boolean = true,
    cursorWidth: Dp = 8.dp,
    cursorHeight: Dp = 16.dp,
    onComplete: () -> Unit = {}
) {
    var displayedText by remember { mutableStateOf("") }
    var isTypingComplete by remember { mutableStateOf(false) }
    
    LaunchedEffect(text) {
        displayedText = ""
        isTypingComplete = false
        
        if (initialDelayMs > 0) {
            delay(initialDelayMs)
        }
        
        for (i in text.indices) {
            displayedText = text.substring(0, i + 1)
            delay(typingDelayMs)
        }
        
        isTypingComplete = true
        onComplete()
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = displayedText,
            color = color,
            style = style
        )
        
        if (showCursor) {
            BlinkingCursor(
                width = cursorWidth,
                height = cursorHeight,
                color = color
            )
        }
    }
}

/**
 * Text that cycles through multiple strings with typing and erasing animation.
 * Used for quote rotator effect.
 */
@Composable
fun TypewriterCycleText(
    texts: List<String>,
    modifier: Modifier = Modifier,
    color: Color = AppColors.white,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    typingDelayMs: Long = 80L,
    eraseDelayMs: Long = 40L,
    pauseDelayMs: Long = 2000L,
    showCursor: Boolean = true
) {
    var currentIndex by remember { mutableStateOf(0) }
    var displayedText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(true) }
    
    val currentText = texts.getOrElse(currentIndex) { "" }
    
    LaunchedEffect(currentIndex) {
        displayedText = ""
        isTyping = true
        
        // Type out the text
        for (i in currentText.indices) {
            displayedText = currentText.substring(0, i + 1)
            delay(typingDelayMs)
        }
        
        // Pause at full text
        delay(pauseDelayMs)
        
        // Erase the text
        isTyping = false
        while (displayedText.isNotEmpty()) {
            displayedText = displayedText.dropLast(1)
            delay(eraseDelayMs)
        }
        
        // Move to next text
        currentIndex = (currentIndex + 1) % texts.size
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = displayedText,
            color = color,
            style = style
        )
        
        if (showCursor) {
            BlinkingCursor(
                color = color
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BLINKING CURSOR
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Blinking cursor block for terminal aesthetic.
 */
@Composable
fun BlinkingCursor(
    modifier: Modifier = Modifier,
    width: Dp = 10.dp,
    height: Dp = 18.dp,
    color: Color = AppColors.white,
    blinkDurationMs: Int = 530
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(blinkDurationMs),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    
    Box(
        modifier = modifier
            .size(width, height)
            .alpha(alpha)
            .background(color)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONSOLE LOG LINE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Console log line with status prefix like [DONE], [WAIT], [ERROR].
 */
@Composable
fun ConsoleLogLine(
    message: String,
    status: ConsoleLogStatus = ConsoleLogStatus.INFO,
    modifier: Modifier = Modifier,
    showPrefix: Boolean = true
) {
    val (prefixText, prefixColor) = when (status) {
        ConsoleLogStatus.DONE -> "[DONE]" to AppColors.accentGreen
        ConsoleLogStatus.WAIT -> "[WAIT]" to AppColors.statusWaiting
        ConsoleLogStatus.ERROR -> "[ERROR]" to AppColors.error
        ConsoleLogStatus.INFO -> "[INFO]" to AppColors.greyLight
    }
    
    val textColor = when (status) {
        ConsoleLogStatus.WAIT -> AppColors.white
        else -> AppColors.grey
    }
    
    Row(modifier = modifier) {
        if (showPrefix) {
            Text(
                text = prefixText,
                color = prefixColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " > ",
                color = AppColors.grey,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = message,
            color = textColor,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

enum class ConsoleLogStatus {
    DONE,
    WAIT,
    ERROR,
    INFO
}
