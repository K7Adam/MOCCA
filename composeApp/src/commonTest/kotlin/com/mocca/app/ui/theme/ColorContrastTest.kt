package com.mocca.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class ColorContrastTest {

    /**
     * Calculates the relative luminance of a color according to WCAG 2.0.
     */
    private fun calculateLuminance(color: Color): Double {
        val r = color.red.toDouble()
        val g = color.green.toDouble()
        val b = color.blue.toDouble()

        fun adjust(c: Double): Double {
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }

        return 0.2126 * adjust(r) + 0.7152 * adjust(g) + 0.0722 * adjust(b)
    }

    /**
     * Calculates the contrast ratio between two colors according to WCAG 2.0.
     * Returns a value between 1.0 and 21.0.
     */
    private fun calculateContrastRatio(color1: Color, color2: Color): Double {
        val l1 = calculateLuminance(color1)
        val l2 = calculateLuminance(color2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testOnBackgroundContrast() {
        val ratio = calculateContrastRatio(AppColors.onBackground, AppColors.background)
        assertTrue(
            ratio >= 4.5,
            "onBackground vs background contrast ratio is $ratio, expected >= 4.5 for normal text"
        )
    }

    @Test
    fun testOnSurfaceContrast() {
        val ratio = calculateContrastRatio(AppColors.onSurface, AppColors.surface)
        assertTrue(
            ratio >= 4.5,
            "onSurface vs surface contrast ratio is $ratio, expected >= 4.5 for normal text"
        )
    }

    @Test
    fun testOnSurfaceVariantContrast() {
        val ratio = calculateContrastRatio(AppColors.onSurfaceVariant, AppColors.surfaceContainer)
        assertTrue(
            ratio >= 4.5,
            "onSurfaceVariant vs surfaceContainer contrast ratio is $ratio, expected >= 4.5 for normal text"
        )
    }

    @Test
    fun testOutlineContrast() {
        val ratio = calculateContrastRatio(AppColors.outline, AppColors.background)
        
        // WCAG requires 3.0 for non-text UI components (like outlines/separators).
        val targetThreshold = 3.0
        
        assertTrue(
            ratio >= targetThreshold,
            "outline vs background contrast ratio is $ratio, expected >= $targetThreshold for non-text UI components"
        )
    }
}
