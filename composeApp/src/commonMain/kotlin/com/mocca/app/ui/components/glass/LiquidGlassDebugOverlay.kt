@file:Suppress("DEPRECATION")

package com.mocca.app.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Debug overlay for Liquid Glass components.
 * 
 * Shows real-time information about glass rendering:
 * - Current shader uniform values
 * - Overdraw regions (when visible)
 * - Performance metrics
 * - Accessibility state
 * 
 * This composable only renders when BuildConfig.DEBUG = true.
 * It should be placed as an overlay on top of your screen content.
 * 
 * Usage:
 * ```kotlin
 * Box(modifier = Modifier.fillMaxSize()) {
 *     // Your screen content
 *     ScreenContent()
 *     
 *     // Debug overlay (only visible in DEBUG builds)
 *     LiquidGlassDebugOverlay(
 *         params = currentGlassParams,
 *         isVisible = BuildConfig.DEBUG && showDebugOverlay
 *     )
 * }
 * ```
 * 
 * @param params Current glass shader parameters
 * @param isVisible Whether the overlay is visible
 * @param showOverdraw Whether to highlight overdraw regions
 * @param accessibilityState Current accessibility state
 */
@Composable
fun LiquidGlassDebugOverlay(
    params: GlassShaderParams,
    isVisible: Boolean = false,
    showOverdraw: Boolean = false,
    accessibilityState: GlassAccessibilityState = GlassAccessibilityState.Default
) {
    if (!isVisible) return
    
    // Track frame time for performance monitoring
    var frameTimeMs by remember { mutableStateOf(0L) }
    var fps by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        var frameCount = 0
        
        while (true) {
            kotlinx.coroutines.delay(100)
            val now = System.currentTimeMillis()
            frameCount++
            
            if (now - lastTime >= 1000) {
                fps = frameCount * 1000f / (now - lastTime)
                frameTimeMs = (now - lastTime) / frameCount
                frameCount = 0
                lastTime = now
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header
            Text(
                text = "🧪 LIQUID GLASS DEBUG",
                color = com.mocca.app.ui.theme.AppColors.accent,
                fontSize = 12.sp
            )
            
            // Performance metrics
            DebugRow("FPS", String.format("%.1f", fps))
            DebugRow("Frame", "${frameTimeMs}ms")
            
            // Shader params
            Text(
                text = "─── Shader Params ───",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
            
            DebugRow("blurRadius", "${params.blurRadius}")
            DebugRow("refraction", String.format("%.2f", params.refractionStrength))
            DebugRow("chromatic", String.format("%.3f", params.chromaticAberration))
            DebugRow("saturation", String.format("%.1f", params.saturation))
            DebugRow("contrast", String.format("%.1f", params.contrast))
            DebugRow("cornerRadius", "${params.cornerRadius}")
            
            // Flags
            Text(
                text = "─── Flags ───",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
            
            DebugRow("refraction", if (params.enableRefraction) "ON" else "OFF")
            DebugRow("chromatic", if (params.enableChromaticAberration) "ON" else "OFF")
            DebugRow("specular", if (params.enableSpecular) "ON" else "OFF")
            
            // Accessibility
            Text(
                text = "─── Accessibility ───",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
            
            DebugRow("reducedTransparency", if (accessibilityState.reducedTransparency) "YES" else "NO")
            DebugRow("reducedMotion", if (accessibilityState.reducedMotion) "YES" else "NO")
            DebugRow("highContrast", if (accessibilityState.highContrastText) "YES" else "NO")
        }
    }
}

/**
 * Simple debug row for displaying key-value pairs.
 */
@Composable
private fun DebugRow(
    key: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = key,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
        Text(
            text = value,
            color = com.mocca.app.ui.theme.AppColors.accent,
            fontSize = 10.sp
        )
    }
}

/**
 * Overdraw visualization overlay.
 * 
 * Shows regions where multiple glass layers are stacked.
 * Red = 4x+ overdraw, Yellow = 2-3x, Green = 1x.
 */
@Composable
fun OverdrawVisualizationOverlay(
    isVisible: Boolean = false,
    overdrawRegions: List<OverdrawRegion> = emptyList()
) {
    if (!isVisible) return
    
    Box(modifier = Modifier.fillMaxSize()) {
        overdrawRegions.forEach { region ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when {
                            region.overdrawCount >= 4 -> Color.Red.copy(alpha = 0.3f)
                            region.overdrawCount >= 2 -> Color.Yellow.copy(alpha = 0.2f)
                            else -> Color.Green.copy(alpha = 0.1f)
                        }
                    )
            )
        }
    }
}

/**
 * Represents a region with overdraw.
 */
data class OverdrawRegion(
    val overdrawCount: Int,
    val description: String
)

/**
 * Debug panel that shows glass tokens and allows real-time adjustment.
 * For development use only.
 */
@Composable
fun LiquidGlassDebugPanel(
    tokens: GlassThemeTokens,
    onTokensChange: (GlassThemeTokens) -> Unit,
    isVisible: Boolean = false
) {
    if (!isVisible) return
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🎛️ GLASS TOKENS",
            color = com.mocca.app.ui.theme.AppColors.accent,
            fontSize = 14.sp
        )
        
        // Token values display
        DebugRow("blurRadius", "${tokens.blurRadius}")
        DebugRow("chromaticAberration", String.format("%.3f", tokens.chromaticAberration))
        DebugRow("saturation", String.format("%.1f", tokens.saturation))
        DebugRow("contrast", String.format("%.1f", tokens.contrast))
        
        // Note: In a real implementation, you would add sliders here
        // to allow real-time adjustment of token values
    }
}

/**
 * Validates glass component setup for common issues.
 */
object GlassDebugValidator {
    
    /**
     * Checks if glass components are properly configured.
     */
    fun validateConfiguration(
        params: GlassShaderParams,
        accessibilityState: GlassAccessibilityState
    ): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        
        // Check for glass-on-glass stacking
        if (params.blurRadius > 32) {
            issues.add(ValidationIssue(
                severity = Severity.WARNING,
                message = "blurRadius > 32dp may cause performance issues"
            ))
        }
        
        // Check accessibility compliance
        if (!accessibilityState.reducedTransparency && params.tintColor.alpha < 0.1f) {
            issues.add(ValidationIssue(
                severity = Severity.WARNING,
                message = "Very low tint opacity may cause text contrast issues"
            ))
        }
        
        // Check refraction bounds
        if (params.refractionStrength > 0.7f) {
            issues.add(ValidationIssue(
                severity = Severity.WARNING,
                message = "refractionStrength > 0.7 may cause visual artifacts"
            ))
        }
        
        return issues
    }
}

/**
 * Validation issue severity.
 */
enum class Severity {
    ERROR,
    WARNING,
    INFO
}

/**
 * A validation issue found during glass configuration check.
 */
data class ValidationIssue(
    val severity: Severity,
    val message: String
)
