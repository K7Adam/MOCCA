package com.mocca.app.ui.components.glass

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.math.sign
import kotlin.time.Duration

/**
 * Android implementation of luminance detection using pixel sampling.
 * 
 * Samples a 5x5 pixel grid and calculates average luminance using
 * ITU-R BT.709 coefficients for perceptual accuracy.
 */

/**
 * Android implementation of luminance animation.
 */
@Composable
actual fun rememberLuminanceAnimation(
    layer: GraphicsLayer,
    samplingInterval: Duration,
    animationSpec: AnimationSpec<Float>
): Animatable<Float, *> {
    val luminanceAnimation = remember { Animatable(0f) }
    
    LaunchedEffect(layer) {
        // Buffer for 5x5 pixel grid = 25 pixels
        val buffer = IntBuffer.allocate(25)
        
        while (isActive) {
            try {
                withContext(Dispatchers.IO) {
                    val imageBitmap: ImageBitmap = layer.toImageBitmap()
                    val sourceBitmap = imageBitmap.asAndroidBitmap()
                    val thumbnail = Bitmap.createScaledBitmap(sourceBitmap, 5, 5, false)
                        .copy(Bitmap.Config.ARGB_8888, false)
                    buffer.rewind()
                    thumbnail.copyPixelsToBuffer(buffer)
                }
            } catch (e: Exception) {
                // Layer not ready yet, skip this frame
                delay(samplingInterval)
                continue
            }
            
            // Calculate average luminance using ITU-R BT.709 coefficients
            val averageLuminance = (0 until 25).sumOf { index ->
                val color = buffer.get(index)
                val r = (color shr 16 and 0xFF) / 255f
                val g = (color shr 8 and 0xFF) / 255f
                val b = (color and 0xFF) / 255f
                // ITU-R BT.709 luminance formula
                0.2126 * r + 0.7152 * g + 0.0722 * b
            } / 25
            
            luminanceAnimation.animateTo(
                // Cap at 0.8 to avoid extreme brightness
                averageLuminance.coerceAtMost(0.8).toFloat(),
                animationSpec
            )
            
            delay(samplingInterval)
        }
    }
    
    return luminanceAnimation
}

/**
 * Android implementation of luminance-based text color.
 */
@Composable
actual fun rememberLuminanceTextColor(
    luminance: Float,
    threshold: Float,
    animationSpec: AnimationSpec<Color>
): Color {
    val animatedColor by animateColorAsState(
        targetValue = if (luminance > threshold) Color.Black else Color.White,
        animationSpec = animationSpec,
        label = "LuminanceTextColor"
    )
    return animatedColor
}

/**
 * Calculates the signed luminance factor for effect adaptation.
 */
fun calculateLuminanceFactor(luminance: Float): Float {
    val normalized = luminance * 2f - 1f
    return sign(normalized) * normalized * normalized
}

/**
 * Interpolates between values based on luminance factor.
 */
fun lerpByLuminance(darkValue: Float, brightValue: Float, l: Float): Float {
    return if (l > 0f) {
        androidx.compose.ui.util.lerp(darkValue, brightValue, l)
    } else {
        androidx.compose.ui.util.lerp(darkValue, brightValue * 0.5f, -l)
    }
}
