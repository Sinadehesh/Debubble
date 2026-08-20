package com.debubble.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Goals
import com.debubble.app.ui.theme.Ink
import kotlin.math.floor
import kotlin.math.sin

/**
 * The campaign as terrain rather than a checklist.
 *
 * Thirty steps are thirty monuments on a ribbon of ground that ascends away from the viewer.
 * Cleared steps stand lit in the campaign's accent and stay that way permanently; the current
 * one is white and taller than its neighbours; everything past it is a wireframe dissolving
 * into fog. Because the elevation is hashed from the step index, each campaign has its own
 * fixed landscape — leave one at step 15 for a month and it is exactly where you left it.
 *
 * Deliberately dimetric rather than truly isometric. A phone is tall and narrow, and a true
 * isometric ribbon walks off the side of the screen as it recedes; this one climbs straight up.
 */

/** Rows of three. Thirty steps make ten rows of ground. */
private const val COLUMNS = 3
private val ROWS = Goals.CAMPAIGN_LENGTH / COLUMNS

/** Vertical span of one row. The whole terrain is this tall, times the row count. */
val TOPO_ROW_HEIGHT: Dp = 104.dp

@Composable
fun Topography(
    step: Int,
    completed: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val tileW = size.width * 0.30f
        val row = TOPO_ROW_HEIGHT.toPx()
        val skew = row * 0.25f
        val lift = row * 0.52f
        val baseY = size.height - row * 0.9f
        val centreX = size.width / 2f

        fun project(col: Int, r: Int, z: Float) = Offset(
            centreX + col * tileW * 0.5f,
            baseY - r * row + col * skew - z * lift
        )

        // Terrain first, far rows before near ones, so nothing draws over its own foreground.
        for (r in ROWS downTo -1) {
            for (col in -2..2) {
                val z = elevation(col, r)
                val p = project(col, r, z)
                val s = stepAt(col, r)
                val walked = col in -1..1 && s < completed
                val near = s <= step + 3

                val plate = Path().apply {
                    moveTo(p.x, p.y - row * 0.5f)
                    lineTo(p.x + tileW * 0.5f, p.y + skew * 0.5f)
                    lineTo(p.x, p.y + row * 0.5f)
                    lineTo(p.x - tileW * 0.5f, p.y - skew * 0.5f)
                    close()
                }
                drawPath(plate, if (walked) Ink.SurfaceHigh.copy(alpha = 0.92f) else Ink.Void.copy(alpha = 0.85f))
                drawPath(
                    plate,
                    color = when {
                        walked -> accent.copy(alpha = 0.20f)
                        near -> Ink.Border.copy(alpha = 0.55f)
                        else -> Ink.Well.copy(alpha = 0.30f)
                    },
                    style = Stroke(width = 1f)
                )
            }
        }

        // Monuments, likewise back to front.
        for (i in Goals.CAMPAIGN_LENGTH - 1 downTo 0) {
            if (i > step + 2) continue
            val col = columnOf(i)
            val r = i / COLUMNS
            val z = elevation(col, r)
            val cleared = i < completed
            val here = i == step - 1

            val height = (0.55f + hash(i * 3.3f) * 0.85f) * when {
                cleared -> 1f
                here -> 1.15f
                else -> 0.42f
            }
            val base = project(col, r, z)
            val top = project(col, r, z + height)
            val dx = tileW * 0.26f
            val dy = row * 0.13f

            val outline = when {
                cleared -> accent
                here -> Ink.Primary
                else -> Ink.Faint.copy(alpha = 0.75f)
            }
            val stroke = Stroke(width = if (here) 2.4f else 1.3f)

            // Offset is a value class and Kotlin prohibits varargs of those, so the faces
            // are built from lists.
            fun face(pts: List<Offset>) = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (k in 1 until pts.size) lineTo(pts[k].x, pts[k].y)
                close()
            }

            val left = face(
                listOf(
                    Offset(top.x - dx, top.y), Offset(top.x, top.y + dy),
                    Offset(base.x, base.y + dy), Offset(base.x - dx, base.y)
                )
            )
            val right = face(
                listOf(
                    Offset(top.x + dx, top.y), Offset(top.x, top.y + dy),
                    Offset(base.x, base.y + dy), Offset(base.x + dx, base.y)
                )
            )
            val cap = face(
                listOf(
                    Offset(top.x, top.y - dy), Offset(top.x + dx, top.y),
                    Offset(top.x, top.y + dy), Offset(top.x - dx, top.y)
                )
            )

            drawPath(left, if (cleared) accent.copy(alpha = 0.16f) else Ink.Surface.copy(alpha = 0.92f))
            drawPath(left, outline, style = stroke)
            drawPath(right, if (cleared) accent.copy(alpha = 0.09f) else Ink.Void.copy(alpha = 0.92f))
            drawPath(right, outline, style = stroke)
            drawPath(
                cap,
                when {
                    cleared -> accent.copy(alpha = 0.42f)
                    here -> Ink.Primary.copy(alpha = 0.22f)
                    else -> Ink.SurfaceHigh.copy(alpha = 0.95f)
                }
            )
            drawPath(cap, outline, style = stroke)

            if (here) {
                drawCircle(Ink.Primary, radius = 7f, center = Offset(top.x, top.y - dy - 14f))
            }
        }

        // Fog. Only the distance the user has not reached dissolves.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Ink.Void,
                0.6f to Ink.Void.copy(alpha = 0.85f),
                1f to Color.Transparent,
                startY = 0f,
                endY = size.height * 0.42f
            ),
            size = size.copy(height = size.height * 0.42f)
        )
    }
}

/** Deterministic per-step terrain, so a campaign's landscape never shifts under the user. */
private fun hash(n: Float): Float {
    val x = sin(n * 127.1f) * 43758.545f
    return x - floor(x)
}

private fun elevation(col: Int, row: Int): Float =
    (hash(col * 13.7f + row * 7.3f) - 0.5f) * 0.55f

/** Serpentine: each row of three runs the opposite way to the one below it. */
private fun columnOf(step: Int): Int {
    val row = step / COLUMNS
    val k = step % COLUMNS
    return if (row % 2 == 0) k - 1 else 1 - k
}

private fun stepAt(col: Int, row: Int): Int {
    val k = if (row % 2 == 0) col + 1 else 1 - col
    return row * COLUMNS + k
}

/** Total drawing height for the whole campaign, for the scroll container. */
val topographyHeight: Dp get() = TOPO_ROW_HEIGHT * (ROWS + 2)

