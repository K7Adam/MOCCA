package com.mocca.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Terminal shapes with 0dp corners (blocky/rectangular aesthetic).
 * NO rounded corners allowed per mockup specifications.
 */
object TerminalShapes {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BASE SHAPES (ALL RECTANGULAR)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** No rounding - default for all elements */
    val none: Shape = RectangleShape
    
    /** Extra small - same as none (0dp) */
    val extraSmall: Shape = RectangleShape
    
    /** Small - same as none (0dp) */
    val small: Shape = RectangleShape
    
    /** Medium - same as none (0dp) */
    val medium: Shape = RectangleShape
    
    /** Large - same as none (0dp) */
    val large: Shape = RectangleShape
    
    /** Extra large - same as none (0dp) */
    val extraLarge: Shape = RectangleShape
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SPECIAL SHAPES (only if mockup explicitly shows them)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Circle shape - ONLY for explicit circular elements like status dots */
    val circle: Shape = RoundedCornerShape(50)
    
    /** Status indicator dot (8dp circle) */
    val statusDot: Shape = RoundedCornerShape(4.dp)
}

/**
 * Creates Material3 Shapes with all rectangular (0dp) corners.
 */
fun terminalShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)
