package com.mocca.app.ui.components.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb

/**
 * AGSL (Android Graphics Shading Language) Shader for Liquid Glass Effect.
 * 
 * This is a first-principles implementation that creates authentic liquid glass
 * by simulating optical properties of real glass:
 * 
 * 1. REFRACTION - Lens distortion based on distance from glass center
 * 2. CHROMATIC ABERRATION - RGB channel separation at edges (prism effect)
 * 3. BLUR - 9x9 Gaussian blur kernel for frosted appearance
 * 4. SPECULAR HIGHLIGHT - Bright rim at top edge
 * 5. INNER SHADOW - Soft shadow at bottom edge
 * 6. EDGE STROKE - Translucent border
 * 
 * Requires API 33+ (Android 13). Falls back to RenderEffect blur on API 31-32.
 * 
 * SHADER EXPLANATION:
 * The shader works by sampling the background content and applying transformations
 * to simulate light passing through glass. The key equations are:
 * 
 * - Refraction: UV offset = (centerDistance * refractionStrength) * normalizedDirection
 * - Chromatic Aberration: RGB channels sampled at slightly different UV offsets
 * - Blur: Weighted average of samples in a grid pattern
 * - Specular: Brightness increases at top edge based on gradient
 * - Shadow: Darkening at bottom edge based on gradient
 */
object LiquidGlassShader {
    
    /**
     * The complete AGSL shader string for liquid glass effect.
     * 
     * Uniforms:
     * - resolution: Size of the glass element in pixels
     * - glassCenter: Center point for refraction calculation
     * - refractionStrength: Intensity of lens distortion
     * - blurRadius: Radius of the blur kernel
     * - chromaticAberration: RGB split intensity
     * - saturation: Color saturation multiplier
     * - contrast: Contrast adjustment
     * - tintColor: RGBA color for surface tint
     * - darkOverlayColor: RGBA color for depth overlay
     * - highlightTopColor: RGBA color for top edge highlight
     * - specularInnerColor: RGBA color for inner glow
     * - strokeColor: RGBA color for edge border
     * - shadowColor: RGBA color for inner shadow
     * - cornerRadius: Radius of rounded corners
     * - enableRefraction: Whether to apply refraction (0.0 or 1.0)
     * - enableChromaticAberration: Whether to apply chromatic aberration (0.0 or 1.0)
     * - backgroundContent: Shader containing the background to sample
     */
    val ShaderSource = """
        // ═══════════════════════════════════════════════════════════════════════════
        // UNIFORMS
        // ═══════════════════════════════════════════════════════════════════════════
        
        uniform float2 resolution;
        uniform float2 glassCenter;
        uniform float refractionStrength;
        uniform float blurRadius;
        uniform float chromaticAberration;
        uniform float saturation;
        uniform float contrast;
        uniform float4 tintColor;
        uniform float4 darkOverlayColor;
        uniform float4 highlightTopColor;
        uniform float4 specularInnerColor;
        uniform float4 strokeColor;
        uniform float4 shadowColor;
        uniform float cornerRadius;
        uniform float enableRefraction;
        uniform float enableChromaticAberration;
        uniform shader backgroundContent;
        
        // ═══════════════════════════════════════════════════════════════════════════
        // CONSTANTS
        // ═══════════════════════════════════════════════════════════════════════════
        
        const float PI = 3.14159265359;
        const float BLUR_SAMPLES = 4.0; // 9x9 = 81 samples
        
        // ═══════════════════════════════════════════════════════════════════════════
        // HELPER FUNCTIONS
        // ═══════════════════════════════════════════════════════════════════════════
        
        // SDF for rounded rectangle
        float roundedRectSDF(float2 uv, float2 size, float radius) {
            float2 d = abs(uv - size * 0.5) - (size * 0.5 - radius);
            return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - radius;
        }
        
        // Check if point is inside rounded rectangle
        bool insideRoundedRect(float2 uv, float2 size, float radius) {
            return roundedRectSDF(uv, size, radius) < 0.0;
        }
        
        // Distance from edge of rounded rectangle
        float edgeDistance(float2 uv, float2 size, float radius) {
            return -roundedRectSDF(uv, size, radius);
        }
        
        // Smooth step with custom edges
        float smoothEdge(float value, float edge0, float edge1) {
            float t = clamp((value - edge0) / (edge1 - edge0), 0.0, 1.0);
            return t * t * (3.0 - 2.0 * t);
        }
        
        // Apply saturation adjustment
        float3 adjustSaturation(float3 color, float sat) {
            float luminance = dot(color, float3(0.299, 0.587, 0.114));
            return mix(float3(luminance), color, sat);
        }
        
        // Apply contrast adjustment
        float3 adjustContrast(float3 color, float cont) {
            return (color - 0.5) * cont + 0.5;
        }
        
        // Gaussian weight for blur
        float gaussianWeight(float x, float y, float sigma) {
            float2 offset = float2(x, y);
            float dist = length(offset);
            return exp(-(dist * dist) / (2.0 * sigma * sigma));
        }
        
        // ═══════════════════════════════════════════════════════════════════════════
        // BLUR FUNCTION
        // ═══════════════════════════════════════════════════════════════════════════
        
        // High-quality blur using Gaussian kernel
        float4 blur(float2 uv, float radius, float2 size) {
            if (radius <= 0.0) {
                return backgroundContent.eval(uv);
            }
            
            float4 color = float4(0.0);
            float totalWeight = 0.0;
            float sigma = radius / 3.0;
            
            // Sample in a grid pattern
            for (float i = -BLUR_SAMPLES; i <= BLUR_SAMPLES; i += 1.0) {
                for (float j = -BLUR_SAMPLES; j <= BLUR_SAMPLES; j += 1.0) {
                    float weight = gaussianWeight(i, j, sigma);
                    float2 offset = float2(i, j) * (radius / BLUR_SAMPLES) / size;
                    float2 sampleUv = uv + offset;
                    
                    // Clamp to valid UV range
                    sampleUv = clamp(sampleUv, float2(0.0), float2(1.0));
                    
                    color += backgroundContent.eval(sampleUv * size) * weight;
                    totalWeight += weight;
                }
            }
            
            return color / totalWeight;
        }
        
        // ═══════════════════════════════════════════════════════════════════════════
        // REFRACTION FUNCTION
        // ═══════════════════════════════════════════════════════════════════════════
        
        // Lens distortion based on distance from center
        float2 applyRefraction(float2 uv, float2 center, float strength, float2 size) {
            if (strength <= 0.0 || enableRefraction < 0.5) {
                return uv;
            }
            
            // Calculate distance and direction from center
            float2 centeredUv = uv - center / size;
            float dist = length(centeredUv);
            
            // Apply barrel distortion (like a magnifying glass)
            // The further from center, the more distortion
            float distortion = 1.0 + strength * dist * dist;
            
            // Scale UV outward from center
            float2 refractedUv = center / size + centeredUv * distortion;
            
            return refractedUv;
        }
        
        // ═══════════════════════════════════════════════════════════════════════════
        // CHROMATIC ABERRATION FUNCTION
        // ═══════════════════════════════════════════════════════════════════════════
        
        // RGB channel separation for prism effect
        float3 applyChromaticAberration(float2 uv, float strength, float2 size) {
            if (strength <= 0.0 || enableChromaticAberration < 0.5) {
                return backgroundContent.eval(uv * size).rgb;
            }
            
            // Direction from center for aberration
            float2 center = float2(0.5);
            float2 dir = normalize(uv - center);
            
            // Sample each color channel with slight offset
            float offset = strength * length(uv - center);
            
            float r = backgroundContent.eval((uv + dir * offset) * size).r;
            float g = backgroundContent.eval(uv * size).g;
            float b = backgroundContent.eval((uv - dir * offset) * size).b;
            
            return float3(r, g, b);
        }
        
        // ═══════════════════════════════════════════════════════════════════════════
        // MAIN SHADER
        // ═══════════════════════════════════════════════════════════════════════════
        
        half4 main(float2 fragCoord) {
            // Convert to UV coordinates (0-1)
            float2 uv = fragCoord / resolution;
            
            // Check if we're inside the rounded rectangle
            float edgeDist = edgeDistance(fragCoord, resolution, cornerRadius);
            if (edgeDist < 0.0) {
                // Outside the glass - return transparent
                return half4(0.0, 0.0, 0.0, 0.0);
            }
            
            // ═══════════════════════════════════════════════════════════════════════
            // 1. REFRACTION (Lens Distortion)
            // ═══════════════════════════════════════════════════════════════════════
            float2 refractedUv = applyRefraction(uv, glassCenter, refractionStrength, resolution);
            
            // ═══════════════════════════════════════════════════════════════════════
            // 2. CHROMATIC ABERRATION (Prism Effect)
            // ═══════════════════════════════════════════════════════════════════════
            float3 chromaColor = applyChromaticAberration(refractedUv, chromaticAberration, resolution);
            
            // ═══════════════════════════════════════════════════════════════════════
            // 3. BLUR (Frosted Glass)
            // ═══════════════════════════════════════════════════════════════════════
            float4 blurredColor = blur(refractedUv, blurRadius, resolution);
            
            // Combine chromatic aberration with blur
            float3 glassColor = blurredColor.rgb;
            
            // Blend chromatic aberration at edges
            float edgeFactor = 1.0 - smoothEdge(edgeDist, 0.0, cornerRadius);
            glassColor = mix(glassColor, chromaColor, edgeFactor * 0.5);
            
            // ═══════════════════════════════════════════════════════════════════════
            // 4. COLOR ADJUSTMENTS (Saturation & Contrast)
            // ═══════════════════════════════════════════════════════════════════════
            glassColor = adjustSaturation(glassColor, saturation);
            glassColor = adjustContrast(glassColor, contrast);
            glassColor = clamp(glassColor, 0.0, 1.0);
            
            // ═══════════════════════════════════════════════════════════════════════
            // 5. TINT & OVERLAY (Surface Color)
            // ═══════════════════════════════════════════════════════════════════════
            glassColor = glassColor * (1.0 - tintColor.a) + tintColor.rgb * tintColor.a;
            glassColor = glassColor * (1.0 - darkOverlayColor.a) + darkOverlayColor.rgb * darkOverlayColor.a;
            
            // ═══════════════════════════════════════════════════════════════════════
            // 6. SPECULAR HIGHLIGHT (Top Edge Rim Light)
            // ═══════════════════════════════════════════════════════════════════════
            float topEdgeFactor = 1.0 - smoothEdge(fragCoord.y, 0.0, 8.0);
            float specularGradient = smoothEdge(fragCoord.y, 0.0, resolution.y * 0.15);
            float specular = topEdgeFactor * specularGradient * highlightTopColor.a;
            glassColor = glassColor + highlightTopColor.rgb * specular;
            
            // Inner specular glow
            float innerSpecular = (1.0 - specularGradient) * specularInnerColor.a * 0.3;
            glassColor = glassColor + specularInnerColor.rgb * innerSpecular;
            
            // ═══════════════════════════════════════════════════════════════════════
            // 7. INNER SHADOW (Bottom Edge Depth)
            // ═══════════════════════════════════════════════════════════════════════
            float bottomEdgeFactor = smoothEdge(fragCoord.y, resolution.y - 16.0, resolution.y);
            float shadowGradient = 1.0 - smoothEdge(fragCoord.y, resolution.y * 0.85, resolution.y);
            float innerShadow = bottomEdgeFactor * shadowGradient * shadowColor.a;
            glassColor = glassColor * (1.0 - innerShadow) + shadowColor.rgb * innerShadow;
            
            // ═══════════════════════════════════════════════════════════════════════
            // 8. EDGE STROKE (Border)
            // ═══════════════════════════════════════════════════════════════════════
            float strokeFactor = 1.0 - smoothEdge(edgeDist, 0.0, 2.0);
            glassColor = glassColor * (1.0 - strokeFactor * strokeColor.a) + strokeColor.rgb * strokeFactor * strokeColor.a;
            
            // ═══════════════════════════════════════════════════════════════════════
            // OUTPUT
            // ═══════════════════════════════════════════════════════════════════════
            return half4(glassColor, 0.95); // Slightly transparent for glass effect
        }
    """.trimIndent()
    
    /**
     * Creates a RuntimeShader with the liquid glass effect.
     * Requires API 33+.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createShader(): RuntimeShader = RuntimeShader(ShaderSource)
}

/**
 * Brush that applies the liquid glass shader effect.
 * Requires API 33+ for AGSL RuntimeShader support.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class LiquidGlassBrush(
    private val params: GlassShaderParams
) : ShaderBrush() {
    
    private val shader: RuntimeShader = LiquidGlassShader.createShader()
    
    override fun createShader(size: Size): android.graphics.Shader {
        // Set all uniform values
        shader.setFloatUniform(
            "resolution",
            size.width,
            size.height
        )
        shader.setFloatUniform(
            "glassCenter",
            size.width / 2f,
            size.height / 2f
        )
        shader.setFloatUniform(
            "refractionStrength",
            if (params.enableRefraction) params.refractionStrength else 0f
        )
        shader.setFloatUniform("blurRadius", params.blurRadius)
        shader.setFloatUniform(
            "chromaticAberration",
            if (params.enableChromaticAberration) params.chromaticAberration else 0f
        )
        shader.setFloatUniform("saturation", params.saturation)
        shader.setFloatUniform("contrast", params.contrast)
        shader.setFloatUniform("cornerRadius", params.cornerRadius)
        shader.setFloatUniform("enableRefraction", if (params.enableRefraction) 1f else 0f)
        shader.setFloatUniform(
            "enableChromaticAberration",
            if (params.enableChromaticAberration) 1f else 0f
        )
        
        // Set color uniforms (convert Compose Color to float array)
        shader.setColorUniform("tintColor", params.tintColor.toArgb())
        shader.setColorUniform("darkOverlayColor", params.darkOverlayColor.toArgb())
        shader.setColorUniform("highlightTopColor", params.highlightTopColor.toArgb())
        shader.setColorUniform("specularInnerColor", params.specularInnerColor.toArgb())
        shader.setColorUniform("strokeColor", params.strokeColor.toArgb())
        shader.setColorUniform("shadowColor", params.shadowColor.toArgb())
        
        return shader
    }
}

/**
 * Remembers a LiquidGlassBrush with the given parameters.
 * Only creates the shader on API 33+.
 */
@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun rememberLiquidGlassBrush(params: GlassShaderParams): LiquidGlassBrush {
    return remember(params) { LiquidGlassBrush(params) }
}

/**
 * Extension to set color uniform from ARGB int.
 */
private fun RuntimeShader.setColorUniform(name: String, color: Int) {
    val a = android.graphics.Color.alpha(color) / 255f
    val r = android.graphics.Color.red(color) / 255f
    val g = android.graphics.Color.green(color) / 255f
    val b = android.graphics.Color.blue(color) / 255f
    setFloatUniform(name, r, g, b, a)
}
