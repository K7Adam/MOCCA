package com.mocca.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WCAG contrast-ratio tests against the Mocha dark color scheme values.
 *
 * These use the hardcoded palette values from [AppTheme] rather than
 * [AppColors] composable getters, so they run without a Compose runtime.
 */
class ColorContrastTest {

    // Mocha dark scheme constants (seed #6F4E37, TonalSpot)
    private val background = Color(0xFF19120D)
    private val onBackground = Color(0xFFFBEBE1)
    private val surface = Color(0xFF19120D)
    private val onSurface = Color(0xFFFBEBE1)
    private val surfaceContainer = Color(0xFF2B221C)
    private val onSurfaceVariant = Color(0xFFD7C3B7)
    private val outline = Color(0xFFA9978D)

    private fun calculateLuminance(color: Color): Double {
        val r = color.red.toDouble()
        val g = color.green.toDouble()
        val b = color.blue.toDouble()

        fun adjust(c: Double): Double {
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }

        return 0.2126 * adjust(r) + 0.7152 * adjust(g) + 0.0722 * adjust(b)
    }

    private fun calculateContrastRatio(color1: Color, color2: Color): Double {
        val l1 = calculateLuminance(color1)
        val l2 = calculateLuminance(color2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testOnBackgroundContrast() {
        val ratio = calculateContrastRatio(onBackground, background)
        assertTrue(
            ratio >= 4.5,
            "onBackground vs background contrast ratio is $ratio, expected >= 4.5 for normal text",
        )
    }

    @Test
    fun testOnSurfaceContrast() {
        val ratio = calculateContrastRatio(onSurface, surface)
        assertTrue(
            ratio >= 4.5,
            "onSurface vs surface contrast ratio is $ratio, expected >= 4.5 for normal text",
        )
    }

    @Test
    fun testOnSurfaceVariantContrast() {
        val ratio = calculateContrastRatio(onSurfaceVariant, surfaceContainer)
        assertTrue(
            ratio >= 4.5,
            "onSurfaceVariant vs surfaceContainer contrast ratio is $ratio, expected >= 4.5 for normal text",
        )
    }

    @Test
    fun testOutlineContrast() {
        val ratio = calculateContrastRatio(outline, background)
        val targetThreshold = 3.0
        assertTrue(
            ratio >= targetThreshold,
            "outline vs background contrast ratio is $ratio, expected >= $targetThreshold for non-text UI components",
        )
    }
}
