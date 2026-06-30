package com.mocca.app.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.graphics.Color

class MoccaThemeTest {

    @Test
    fun testMoccaThemeColors() {
        // Test direct access to tokens via MoccaTheme.colors (AppColors)
        val expectedPrimary = Color(0xFFAFC2FF)
        assertEquals(expectedPrimary, MoccaTheme.colors.primary, "Primary color mismatch")
    }

    @Test
    fun testMoccaThemeStatusColors() {
        // Test AppColors directly since extendedColors requires Composable context
        val expectedOnline = Color(0xFF4CAF50)
        assertEquals(expectedOnline, AppColors.statusOnline, "Status Online color mismatch")
    }
    
    @Test
    fun testMoccaThemeTypography() {
        // Verify typography access
        val typography = MoccaTheme.typography
        // Just verify it's not null and we can access tokens
        // (Individual tokens like displayLarge are @Composable, but the object itself is not)
        assertEquals(AppTypography, typography)
    }
}
