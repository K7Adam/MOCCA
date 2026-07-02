package com.mocca.app.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Shapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * MOCCA Shape Tokens — Full Material 3 Expressive shape scale.
 *
 * Radius scale (10 levels):
 *   none=0, extraSmall=4, small=8, medium=12, large=16, largeIncreased=20,
 *   extraLarge=28, extraLargeIncreased=32, extraExtraLarge=48, full=50%.
 *
 * Also exposes all 35 [MaterialShapes] polygon shapes and semantic aliases
 * consumed by UI components.
 */
object AppShapes {

    // =========================================================================
    // M3 EXPRESSIVE SHAPE SCALE — 10 corner-radius levels
    // =========================================================================

    val none: CornerBasedShape = RoundedCornerShape(0.dp)
    val extraSmall: CornerBasedShape = RoundedCornerShape(4.dp)
    val small: CornerBasedShape = RoundedCornerShape(8.dp)
    val medium: CornerBasedShape = RoundedCornerShape(12.dp)
    val large: CornerBasedShape = RoundedCornerShape(16.dp)
    val largeIncreased: CornerBasedShape = RoundedCornerShape(20.dp)
    val extraLarge: CornerBasedShape = RoundedCornerShape(28.dp)
    val extraLargeIncreased: CornerBasedShape = RoundedCornerShape(32.dp)
    val extraExtraLarge: CornerBasedShape = RoundedCornerShape(48.dp)
    val full: CornerBasedShape = RoundedCornerShape(50)

    // =========================================================================
    // MATERIAL SHAPES — 35 polygon shapes via M3 Expressive
    // =========================================================================

    val flower: Shape @Composable get() = MaterialShapes.Flower.toShape()
    val sunny: Shape @Composable get() = MaterialShapes.Sunny.toShape()
    val gem: Shape @Composable get() = MaterialShapes.Gem.toShape()
    val puffy: Shape @Composable get() = MaterialShapes.Puffy.toShape()
    val bun: Shape @Composable get() = MaterialShapes.Bun.toShape()
    val heart: Shape @Composable get() = MaterialShapes.Heart.toShape()
    val boom: Shape @Composable get() = MaterialShapes.Boom.toShape()
    val slanted: Shape @Composable get() = MaterialShapes.Slanted.toShape()
    val arch: Shape @Composable get() = MaterialShapes.Arch.toShape()
    val arrow: Shape @Composable get() = MaterialShapes.Arrow.toShape()
    val fan: Shape @Composable get() = MaterialShapes.Fan.toShape()
    val cookie4Sided: Shape @Composable get() = MaterialShapes.Cookie4Sided.toShape()
    val cookie6Sided: Shape @Composable get() = MaterialShapes.Cookie6Sided.toShape()
    val cookie7Sided: Shape @Composable get() = MaterialShapes.Cookie7Sided.toShape()
    val cookie9Sided: Shape @Composable get() = MaterialShapes.Cookie9Sided.toShape()
    val cookie12Sided: Shape @Composable get() = MaterialShapes.Cookie12Sided.toShape()
    val ghostish: Shape @Composable get() = MaterialShapes.Ghostish.toShape()
    val clover4Leaf: Shape @Composable get() = MaterialShapes.Clover4Leaf.toShape()
    val clover8Leaf: Shape @Composable get() = MaterialShapes.Clover8Leaf.toShape()
    val burst: Shape @Composable get() = MaterialShapes.Burst.toShape()
    val softBurst: Shape @Composable get() = MaterialShapes.SoftBurst.toShape()
    val oval: Shape @Composable get() = MaterialShapes.Oval.toShape()
    val materialPill: Shape @Composable get() = MaterialShapes.Pill.toShape()
    val pentagon: Shape @Composable get() = MaterialShapes.Pentagon.toShape()
    val diamond: Shape @Composable get() = MaterialShapes.Diamond.toShape()
    val veryRoundedSquare: Shape = RoundedCornerShape(32.dp)

    // =========================================================================
    // SEMANTIC SHAPE TOKENS — component-specific aliases
    // =========================================================================

    val circle: Shape = RoundedCornerShape(50)
    val pill: Shape = RoundedCornerShape(9999.dp)
    val statusDot: Shape = RoundedCornerShape(4.dp)

    // -- Message group shapes ------------------------------------------------
    val groupOuterRadius = 24.dp
    val groupInnerRadius = 4.dp
    val groupSingle: Shape = RoundedCornerShape(groupOuterRadius)
    val groupTop: Shape = RoundedCornerShape(
        topStart = groupOuterRadius,
        topEnd = groupOuterRadius,
        bottomEnd = groupInnerRadius,
        bottomStart = groupInnerRadius,
    )
    val groupMiddle: Shape = RoundedCornerShape(groupInnerRadius)
    val groupBottom: Shape = RoundedCornerShape(
        topStart = groupInnerRadius,
        topEnd = groupInnerRadius,
        bottomEnd = groupOuterRadius,
        bottomStart = groupOuterRadius,
    )

    // -- Sheet / toolbar shapes ----------------------------------------------
    val bottomSheet: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val bottomSheetExpanded: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val topRounded: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val bottomRounded: Shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)

    // -- Chat bubble shapes --------------------------------------------------
    val messageBubbleUser: Shape = RoundedCornerShape(
        topStart = 24.dp, topEnd = 2.dp, bottomEnd = 24.dp, bottomStart = 24.dp,
    )
    val messageBubbleAgent: Shape = RoundedCornerShape(
        topStart = 2.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp,
    )

    // -- Component shapes ----------------------------------------------------
    val input: Shape = RoundedCornerShape(32.dp)
    val card: Shape = groupSingle
    val moduleCard: Shape = RoundedCornerShape(28.dp)
    val codeBlock: Shape = RoundedCornerShape(12.dp)
    val avatar: Shape = RoundedCornerShape(50)
    val fab: Shape = RoundedCornerShape(50)
    val sessionCard: Shape = RoundedCornerShape(24.dp)
    val floatingToolbar: Shape = RoundedCornerShape(24.dp)
    val navItem: Shape = RoundedCornerShape(20.dp)
    val tabPill: Shape = RoundedCornerShape(9999.dp)
    val badge: Shape = RoundedCornerShape(4.dp)
    val tag: Shape = RoundedCornerShape(9999.dp)
    val dialog: Shape = RoundedCornerShape(24.dp)
    val filePreview: Shape = RoundedCornerShape(16.dp)
}

@Composable
fun appShapes(): Shapes = Shapes(
    extraSmall = AppShapes.extraSmall,
    small = AppShapes.small,
    medium = AppShapes.medium,
    large = AppShapes.large,
    extraLarge = AppShapes.extraLarge,
    extraExtraLarge = AppShapes.extraExtraLarge,
)
