package com.mocca.app.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.graphics.Color

class MoccaThemeTest {

    @Test
    fun testMoccaThemeStatusColors() {
        val expectedOnline = Color(0xFF4CAF50)
        assertEquals(expectedOnline, AppColors.statusOnline, "Status Online color mismatch")
    }

    @Test
    fun testMoccaThemeStaticColors() {
        assertEquals(Color(0xFF4CAF50), AppColors.success, "Success color mismatch")
        assertEquals(Color(0xFFFFB74D), AppColors.warning, "Warning color mismatch")
        assertEquals(Color(0xFFFFFFFF), AppColors.white, "White color mismatch")
    }

    @Test
    fun testMoccaThemeTypography() {
        val typography = MoccaTheme.typography
        assertEquals(AppTypography, typography)
    }
}
