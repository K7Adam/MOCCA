package com.mocca.app.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Shapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object AppShapes {
    val none: CornerBasedShape = RoundedCornerShape(0.dp)
    val extraSmall: CornerBasedShape = RoundedCornerShape(4.dp)
    val small: CornerBasedShape = RoundedCornerShape(8.dp)
    val medium: CornerBasedShape = RoundedCornerShape(12.dp)
    val large: CornerBasedShape = RoundedCornerShape(16.dp)
    val extraLarge: CornerBasedShape = RoundedCornerShape(24.dp)
    val rounded2xl: CornerBasedShape = extraLarge // Alias for compatibility
    val extraExtraLarge: CornerBasedShape = RoundedCornerShape(32.dp)

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

    val circle: Shape = RoundedCornerShape(50)
    val pill: Shape = RoundedCornerShape(9999.dp)
    val statusDot: Shape = RoundedCornerShape(4.dp)
    val bottomSheet: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val bottomSheetExpanded: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val topRounded: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val bottomRounded: Shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    val messageBubbleUser: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 2.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
    val messageBubbleAgent: Shape = RoundedCornerShape(topStart = 2.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
    val input: Shape = RoundedCornerShape(32.dp)
    val card: Shape = RoundedCornerShape(16.dp)
    val moduleCard: Shape = RoundedCornerShape(28.dp)
    val codeBlock: Shape = RoundedCornerShape(12.dp)
    val avatar: Shape = RoundedCornerShape(50)
    val fab: Shape = RoundedCornerShape(50)
    val sessionCard: Shape = RoundedCornerShape(24.dp)
    val tabPill: Shape = RoundedCornerShape(9999.dp)
    val badge: Shape = RoundedCornerShape(4.dp)
    val tag: Shape = RoundedCornerShape(9999.dp)
    val dialog: Shape = RoundedCornerShape(24.dp)
    val filePreview: Shape = RoundedCornerShape(16.dp)
    val squircle: Shape = createSquircleShape()
}

@Composable
fun appShapes(): Shapes = Shapes(
    extraSmall = AppShapes.extraSmall,
    small = AppShapes.small,
    medium = AppShapes.medium,
    large = AppShapes.large,
    extraLarge = AppShapes.extraLarge,
    extraExtraLarge = AppShapes.extraExtraLarge
)
