package com.mocca.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Shapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * MOCCA Shapes - Modern rounded corners and expressive shapes design system.
 * Features standard rounded aesthetic (16dp-32dp) and M3 Expressive shape library.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object AppShapes {

    // ═══════════════════════════════════════════════════════════════════════════
    // BASE CORNER RADII (Standard M3)
    // ═══════════════════════════════════════════════════════════════════════════

    /** No rounding - for specific elements that need sharp corners */
    val none: Shape = RoundedCornerShape(0.dp)

    /** Extra small - 4dp - for inline code, small badges */
    val extraSmall: Shape = RoundedCornerShape(4.dp)

    /** Small - 8dp - for chips, small cards */
    val small: Shape = RoundedCornerShape(8.dp)

    /** Medium - 12dp - for cards, containers */
    val medium: Shape = RoundedCornerShape(12.dp)

    /** Large - 16dp - for prominent cards, inputs */
    val large: Shape = RoundedCornerShape(16.dp)

    /** Extra large - 24dp - for modal cards, large containers */
    val extraLarge: Shape = RoundedCornerShape(24.dp)

    /** XXL - 28dp - for module cards per design specs */
    val xxl: Shape = RoundedCornerShape(28.dp)

    /** 2XL - 32dp - for floating inputs, pill containers */
    val rounded2xl: Shape = RoundedCornerShape(32.dp)

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPRESSIVE SHAPES (M3 Expressive Library - 35 shapes)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Hero shapes for high-impact UI elements */
    val flower: Shape @Composable get() = MaterialShapes.Flower.toShape()
    val sunny: Shape @Composable get() = MaterialShapes.Sunny.toShape()
    val gem: Shape @Composable get() = MaterialShapes.Gem.toShape()
    val puffy: Shape @Composable get() = MaterialShapes.Puffy.toShape()
    val bun: Shape @Composable get() = MaterialShapes.Bun.toShape()
    val heart: Shape @Composable get() = MaterialShapes.Heart.toShape()
    val boom: Shape @Composable get() = MaterialShapes.Boom.toShape()
    
    /** Geometric expressive shapes */
    val slanted: Shape @Composable get() = MaterialShapes.Slanted.toShape()
    val arch: Shape @Composable get() = MaterialShapes.Arch.toShape()
    val arrow: Shape @Composable get() = MaterialShapes.Arrow.toShape()
    val fan: Shape @Composable get() = MaterialShapes.Fan.toShape()

    // ═══════════════════════════════════════════════════════════════════════════
    // SPECIAL SHAPES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Circle shape - for status dots, avatars, FABs */
    val circle: Shape = RoundedCornerShape(50)

    /** Pill shape - fully rounded for buttons, badges */
    val pill: Shape = RoundedCornerShape(9999.dp)

    /** Status indicator dot (8dp circle) */
    val statusDot: Shape = RoundedCornerShape(4.dp)

    /** Bottom sheet shape - rounded top corners only */
    val bottomSheet: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    /** Top rounded only - for cards at bottom of screen */
    val topRounded: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    /** Bottom rounded only - for cards at top of screen */
    val bottomRounded: Shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPONENT-SPECIFIC SHAPES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Input field shape - rounded rectangle */
    val input: Shape = RoundedCornerShape(32.dp)

    /** Button shape - pill/fully rounded */
    val button: Shape = RoundedCornerShape(9999.dp)

    /** Card shape - medium rounded */
    val card: Shape = RoundedCornerShape(16.dp)

    /** Module card shape - larger rounded per design */
    val moduleCard: Shape = RoundedCornerShape(28.dp)

    /** Alert banner shape - pill */
    val alertBanner: Shape = RoundedCornerShape(9999.dp)

    /** Code block shape */
    val codeBlock: Shape = RoundedCornerShape(12.dp)

    /** Avatar shape - circular */
    val avatar: Shape = RoundedCornerShape(50)

    /** Floating action button */
    val fab: Shape = RoundedCornerShape(50)

    /** Session card shape */
    val sessionCard: Shape = RoundedCornerShape(24.dp)

    /** Tab pill shape */
    val tabPill: Shape = RoundedCornerShape(9999.dp)

    /** Badge shape */
    val badge: Shape = RoundedCornerShape(4.dp)

    /** Tag/chip shape */
    val tag: Shape = RoundedCornerShape(9999.dp)

    /** Dialog shape */
    val dialog: Shape = RoundedCornerShape(24.dp)

    /** File preview card */
    val filePreview: Shape = RoundedCornerShape(16.dp)
}

/**
 * Creates Material3 Shapes with modern rounded corners and expressive extensions.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun appShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)