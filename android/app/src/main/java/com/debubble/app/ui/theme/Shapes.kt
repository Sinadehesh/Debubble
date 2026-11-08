package com.debubble.app.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Corners stepped like a sprite's: the outline comes in one pixel, then one more, the way a
 * rounded corner is drawn when there are only whole pixels to draw it with.
 *
 * This is the one shape in the app. Cards, buttons, chips, fields, sheets and dialogs are all
 * cut to it, so everything you touch is drawn in the same hand as the room. The step is
 * rounded to whole device pixels, so the stairs land on the pixel grid and stay crisp.
 */
class PixelCorners(private val step: Dp = 3.dp, private val steps: Int = 2) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val wanted = with(density) { step.toPx() }.roundToInt().toFloat()
        // Never more stair than the thing is tall: a thin bar keeps a one-step corner.
        val s = max(1f, minOf(wanted, floor(minOf(w, h) / (2f * steps + 1))))
        val n = steps.toFloat()
        val path = Path().apply {
            moveTo(n * s, 0f)
            lineTo(w - n * s, 0f)
            for (i in 1..steps) {
                lineTo(w - (n - i + 1) * s, (i - 1) * s + s)
                lineTo(w - (n - i) * s, i * s)
            }
            lineTo(w, h - n * s)
            for (i in 1..steps) {
                lineTo(w - (i - 1) * s - s, h - (n - i + 1) * s)
                lineTo(w - i * s, h - (n - i) * s)
            }
            lineTo(n * s, h)
            for (i in 1..steps) {
                lineTo((n - i + 1) * s, h - i * s)
                lineTo((n - i) * s, h - i * s)
            }
            lineTo(0f, n * s)
            for (i in 1..steps) {
                lineTo((i - 1) * s + s, (n - i + 1) * s)
                lineTo(i * s, (n - i) * s)
            }
            close()
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is PixelCorners && other.step == step && other.steps == steps

    override fun hashCode(): Int = step.hashCode() * 31 + steps
}

/** The cut for small things (chips, ticks, tracks), cards, and big panels. */
object Cut {
    val small: Shape = PixelCorners(2.dp)
    val card: Shape = PixelCorners(3.dp)
    val large: Shape = PixelCorners(4.dp)
}

/** Material's own components (fields, sheets, dialogs, pickers) take the same corners. */
val CozyShapes = Shapes(
    extraSmall = Cut.small,
    small = Cut.small,
    medium = Cut.card,
    large = Cut.large,
    extraLarge = Cut.large
)
