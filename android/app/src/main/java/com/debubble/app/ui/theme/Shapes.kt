package com.debubble.app.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
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
 * cut to it, so everything you touch is drawn in the same hand as the room. Each step is
 * rounded to whole device pixels, so the stairs land on the pixel grid and stay crisp.
 *
 * It is a [CornerBasedShape], like Material's own, so Material can take it for its fields,
 * sheets and dialogs, and square off the corners it needs to (a sheet's bottom edge).
 */
class PixelCorners(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    private val steps: Int = 2
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    /** Every corner the same: [steps] stairs of [step] each. */
    constructor(step: Dp = 3.dp, steps: Int = 2) : this(
        CornerSize(step * steps), CornerSize(step * steps),
        CornerSize(step * steps), CornerSize(step * steps), steps
    )

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        if (topStart + topEnd + bottomEnd + bottomStart == 0f) return Outline.Rectangle(size.toRect())
        val ltr = layoutDirection == LayoutDirection.Ltr
        val w = size.width
        val h = size.height
        // Never more stair than the thing is tall: a thin bar keeps a one-step corner.
        val cap = max(1f, floor(minOf(w, h) / (2f * steps + 1)))
        fun stair(corner: Float): Float =
            if (corner <= 0f) 0f else max(1f, minOf((corner / steps).roundToInt().toFloat(), cap))
        val a = stair(if (ltr) topStart else topEnd)        // top left
        val b = stair(if (ltr) topEnd else topStart)        // top right
        val c = stair(if (ltr) bottomEnd else bottomStart)  // bottom right
        val d = stair(if (ltr) bottomStart else bottomEnd)  // bottom left
        val n = steps.toFloat()
        val path = Path().apply {
            moveTo(n * a, 0f)
            lineTo(w - n * b, 0f)
            for (i in 1..steps) {
                lineTo(w - (n - i + 1) * b, i * b)
                lineTo(w - (n - i) * b, i * b)
            }
            lineTo(w, h - n * c)
            for (i in 1..steps) {
                lineTo(w - i * c, h - (n - i + 1) * c)
                lineTo(w - i * c, h - (n - i) * c)
            }
            lineTo(n * d, h)
            for (i in 1..steps) {
                lineTo((n - i + 1) * d, h - i * d)
                lineTo((n - i) * d, h - i * d)
            }
            lineTo(0f, n * a)
            for (i in 1..steps) {
                lineTo(i * a, (n - i + 1) * a)
                lineTo(i * a, (n - i) * a)
            }
            close()
        }
        return Outline.Generic(path)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
    ): CornerBasedShape = PixelCorners(topStart, topEnd, bottomEnd, bottomStart, steps)

    override fun equals(other: Any?): Boolean =
        other is PixelCorners && other.steps == steps &&
            other.topStart == topStart && other.topEnd == topEnd &&
            other.bottomEnd == bottomEnd && other.bottomStart == bottomStart

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        return 31 * result + steps
    }

    override fun toString(): String = "PixelCorners($topStart, $topEnd, $bottomEnd, $bottomStart, steps=$steps)"
}

/** The cut for small things (chips, ticks, tracks), cards, big panels, and a sheet's top. */
object Cut {
    val small: CornerBasedShape = PixelCorners(2.dp)
    val card: CornerBasedShape = PixelCorners(3.dp)
    val large: CornerBasedShape = PixelCorners(4.dp)
    /** A sheet sits on the bottom of the screen, so only its top corners are cut. */
    val sheet: CornerBasedShape = large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
}

/** Material's own components (fields, sheets, dialogs, pickers) take the same corners. */
val CozyShapes = Shapes(
    extraSmall = Cut.small,
    small = Cut.small,
    medium = Cut.card,
    large = Cut.large,
    extraLarge = Cut.large
)
