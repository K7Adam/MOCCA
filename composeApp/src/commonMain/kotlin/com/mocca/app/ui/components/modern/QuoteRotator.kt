package com.mocca.app.ui.components.modern

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.coroutines.delay

/**
 * Quote rotator component for the main chat initial state.
 * Shows rotating quotes with fade animation.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// DEFAULT QUOTES
// ═══════════════════════════════════════════════════════════════════════════════

val defaultQuotes = listOf(
    "Built for what people want",
    "Code at the speed of thought",
    "Your AI pair programmer",
    "Making the impossible possible",
    "Agents that understand context",
    "From idea to implementation",
    "The future of development"
)

// ═══════════════════════════════════════════════════════════════════════════════
// QUOTE ROTATOR
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Rotating quote display with fade animation.
 * Centers content and rotates through quotes.
 */
@Composable
fun QuoteRotator(
    quotes: List<String> = defaultQuotes,
    modifier: Modifier = Modifier,
    intervalMs: Long = 4000L,
    fadeInDurationMs: Int = 500,
    fadeOutDurationMs: Int = 500,
    textColor: Color = AppColors.white,
    showAsciiArt: Boolean = true,
    versionText: String? = null,
    serverText: String? = null,
    isLoading: Boolean = false,       // Show loading indicator under ASCII art
    loadingText: String = "LOADING..." // Text to show when loading
) {
    var currentIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(intervalMs)
            currentIndex = (currentIndex + 1) % quotes.size
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {

        
        // Loading indicator (shown under ASCII art when loading)
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.statusWaiting
                )
                Text(
                    text = loadingText,
                    color = AppColors.statusWaiting,
                    style = AppTypography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Rotating quote with fade animation (only show when NOT loading)
        AnimatedVisibility(
            visible = !isLoading,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(fadeInDurationMs)) togetherWith
                        fadeOut(animationSpec = tween(fadeOutDurationMs)))
                        .using(SizeTransform(clip = true))
                },
                label = "quoteAnimation"
            ) { index ->
                Text(
                    text = quotes.getOrElse(index) { "" },
                    color = textColor,
                    style = AppTypography.headlineLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Version and server info
        if (versionText != null || serverText != null) {
            val metaText = listOfNotNull(versionText, serverText).joinToString(" • ")
            Text(
                text = metaText.uppercase(),
                color = AppColors.greyDark,
                style = AppTypography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ASCII ART
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * ASCII art globe/logo for the quote rotator section.
 */
@Composable
fun AsciiGlobe(
    modifier: Modifier = Modifier,
    color: Color = AppColors.grey
) {
    val asciiArt = """
           .---.
          /     \
         | () () |
          \  ^  /
           '---'
        /=========\
       /===========\
      [=============]
       \===========/
        `=========`
    """.trimIndent()
    
    Text(
        text = asciiArt,
        color = color,
        style = AppTypography.bodySmall,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

/**
 * Alternative ASCII art - terminal/command prompt style.
 */
@Composable
fun AsciiTerminal(
    modifier: Modifier = Modifier,
    color: Color = AppColors.grey
) {
    val asciiArt = """
        ┌─────────────────────┐
        │  >_                 │
        │                     │
        │  OPENCODE TERMINAL  │
        │                     │
        └─────────────────────┘
    """.trimIndent()
    
    Text(
        text = asciiArt,
        color = color,
        style = AppTypography.bodySmall,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

/**
 * Decorative brackets symbol [[ ]].
 */
@Composable
fun DecorativeBrackets(
    modifier: Modifier = Modifier,
    color: Color = AppColors.white
) {
    Text(
        text = "[[ ]]",
        color = color,
        style = AppTypography.displayLarge,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}
