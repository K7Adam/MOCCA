package com.mocca.app.ui.components.modern

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppTypography
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun TypewriterText(
    text: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AppColors.textPrimary,
    style: TextStyle = AppTypography.bodyLarge
) {
    var displayedText by remember { mutableStateOf("") }
    val textRef by rememberUpdatedState(text)
    val isStreamingRef by rememberUpdatedState(isStreaming)

    LaunchedEffect(Unit) {
        while (true) {
            val currentTarget = textRef
            val streaming = isStreamingRef

            if (!streaming) {
                if (displayedText != currentTarget) {
                    displayedText = currentTarget
                }
                delay(16)
                continue
            }

            if (currentTarget.length < displayedText.length) {
                displayedText = ""
            }

            if (displayedText.length < currentTarget.length) {
                displayedText += currentTarget[displayedText.length]
                delay(Random.nextLong(30, 250))
            } else {
                delay(16)
            }
        }
    }

    Text(
        text = displayedText,
        color = color,
        style = style,
        modifier = modifier
    )
}
