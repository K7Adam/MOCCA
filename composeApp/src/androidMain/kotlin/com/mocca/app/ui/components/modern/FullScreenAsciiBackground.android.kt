package com.mocca.app.ui.components.modern

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.mocca.app.ui.theme.AppColors
import kotlin.math.sin

@RequiresApi(33)
private const val AGSL_SOURCE = """
    uniform float  uTime;
    uniform float2 uResolution;
    
    // Optimized Plasma: Dropped 1 radial wave and simplified inner parameters for ALU speedup
    float plasmaField(float2 uv, float t) {
        float w1 = sin(uv.x * 7.3  + t * 1.1 + sin(uv.y * 4.1  + t * 0.7) * 1.5);
        float w2 = sin(uv.y * 11.7 + t * 0.8 + sin(uv.x * 8.9  + t * 1.3) * 1.2);
        float w3 = sin((uv.x + uv.y) * 6.5 + t * 1.6);
        return (w1 + w2 + w3) * 0.333;
    }
    
    // O(1) Analytical DNA Helix (Previously O(N) with 80 loops per pixel)
    float dnaHelix(float2 uv, float t) {
        float progress = fract(uv.y + t * 0.06);
        float basePhase = progress * 18.8496 + t * 0.8;
        
        float fade = 1.0 - progress * 0.6;
        
        // Strand 1
        float nx1 = 0.5 + sin(basePhase) * 0.18;
        float dn1 = cos(basePhase) * 3.39; // 0.18 * 18.8496
        float d1 = abs(uv.x - nx1) / sqrt(1.0 + dn1 * dn1);
        
        // Strand 2
        float nx2 = 0.5 + sin(basePhase + 3.1416) * 0.18;
        float dn2 = cos(basePhase + 3.1416) * 3.39;
        float d2 = abs(uv.x - nx2) / sqrt(1.0 + dn2 * dn2);
        
        return (exp(-d1 * 18.0) + exp(-d2 * 18.0)) * fade;
    }
    
    float shockwave(float2 uv, float t) {
        float2 center = float2(
            0.5 + sin(t * 0.37) * 0.28,
            0.5 + cos(t * 0.29) * 0.22
        );
        float pulse = fract(t / 12.0);
        float dist = abs(length(uv - center) - (pulse * 1.8));
        float ring = 1.0 - smoothstep(0.0, 0.028, dist);
        return ring * (1.0 - smoothstep(0.3, 1.0, pulse)) * 1.4;
    }
    
    float asciiGrid(float2 uv) {
        float2 cell = fract(uv * float2(38.0, 76.0));
        return 1.0 - step(0.91, max(cell.x, cell.y)) * 0.45;
    }
    
    half4 main(float2 fragCoord) {
        if (uResolution.x <= 0.0 || uResolution.y <= 0.0) return half4(0.0, 0.0, 0.0, 1.0);

        float2 uv = fragCoord / uResolution;
        
        float plasma = plasmaField(uv, uTime) * 0.5 + 0.5;
        float dna    = dnaHelix(uv, uTime);
        float shock  = shockwave(uv, uTime);
        float grid   = asciiGrid(uv);
        
        float bottomBoost = 1.0 + smoothstep(0.65, 1.0, fragCoord.y / uResolution.y) * 0.5;
        
        float phase = uTime - 60.0 * floor(uTime / 60.0);
        float intro = smoothstep(0.0, 8.0, phase);
        float cresc = smoothstep(44.0, 46.0, phase) * (1.0 - smoothstep(47.0, 49.0, phase));
        float decay = smoothstep(49.0, 60.0, phase) * 0.3;
        float envelope = intro * (1.0 - decay) + cresc * 0.3;
        
        float luma = clamp((plasma * 0.55 + dna * 0.30 + shock * 0.15) * envelope * bottomBoost, 0.0, 1.0);
        luma = floor(luma * 10.0) / 10.0;
        
        half3 darkTeal  = half3(0.0, 0.08, 0.07);
        half3 mintGreen = half3(0.0, 1.0,  0.80);
        
        half3 col = mix(darkTeal, mintGreen, half(luma * grid));
        col += mintGreen * half(0.015);
        
        return half4(col, 1.0);
    }
"""

@RequiresApi(33)
@Composable
private fun AgslAsciiBackground(modifier: Modifier) {
    var time by remember { mutableFloatStateOf(0f) }
    val shader = remember { RuntimeShader(AGSL_SOURCE) }

    val brush = remember(shader) { ShaderBrush(shader) }
    
    LaunchedEffect(Unit) {
        val t0 = System.nanoTime()
        while (true) {
            withFrameNanos { ns -> time = (ns - t0) / 1_000_000_000f }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width.takeIf { it > 0f && it.isFinite() } ?: 1f
        val h = size.height.takeIf { it > 0f && it.isFinite() } ?: 1f
        shader.setFloatUniform("uTime", time)
        shader.setFloatUniform("uResolution", w, h)
        drawRect(brush = brush)
    }
}

private val ASCII_CHARS = charArrayOf(' ', '.', ':', '-', '=', '+', '*', '#', '%', '@')
private const val CPU_COLS = 40
private const val CPU_ROWS = 22
private const val BOTTOM_ROWS = 6

private fun buildAsciiFrame(tSec: Float): String {
    val cx = CPU_COLS / 2f
    val cy = CPU_ROWS / 2f * 2.15f
    return buildString(CPU_COLS * (CPU_ROWS + 1)) {
        for (row in 0 until CPU_ROWS) {
            val isBottom = row >= (CPU_ROWS - BOTTOM_ROWS)
            val boost    = if (isBottom) 1.5f else 1.0f
            for (col in 0 until CPU_COLS) {
                val dx  = (col - cx) / (CPU_COLS * 0.5f)
                val dy  = (row * 2.15f - cy) / (CPU_ROWS * 2.15f * 0.5f)
                val w1  = sin(dx * 7.3f  + tSec * 1.1f + sin(dy * 4.1f + tSec * 0.7f) * 1.5f)
                val w2  = sin(dy * 11.7f + tSec * 0.8f + sin(dx * 8.9f + tSec * 1.3f) * 1.2f)
                val raw = ((w1 * 0.5f + w2 * 0.5f) * 0.5f + 0.5f) * boost
                val idx = (raw.coerceIn(0f, 1f) * (ASCII_CHARS.size - 1)).toInt()
                append(ASCII_CHARS[idx])
            }
            if (row < CPU_ROWS - 1) append('\n')
        }
    }
}

@Composable
private fun CpuAsciiBackground(modifier: Modifier) {
    var frame by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val t0 = System.nanoTime()
        while (true) {
            withFrameNanos { ns ->
                val tSec = (ns - t0) / 1_000_000_000f
                if ((tSec * 30f).toInt() % 2 == 0) frame = buildAsciiFrame(tSec)
            }
        }
    }
    Text(
        text      = frame,
        color     = AppColors.accentGreenBright,
        style     = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
        modifier  = modifier,
        textAlign = TextAlign.Center,
        softWrap  = false
    )
}

@Composable
actual fun FullScreenAsciiBackground(modifier: Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslAsciiBackground(modifier)
    } else {
        CpuAsciiBackground(modifier)
    }
}
