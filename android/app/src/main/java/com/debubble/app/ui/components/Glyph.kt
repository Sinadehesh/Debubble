package com.debubble.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * The icon set, drawn rather than imported.
 *
 * The project does not depend on material-icons-extended and adding it for six shapes would
 * pull in a thousand vectors to ship five. These are stroked paths on a 24-unit grid: they
 * scale cleanly, take the colour they are given, and cannot drift out of sync with a library
 * version.
 *
 * Every glyph is decorative — the control around it carries the label — so they clear their
 * own semantics rather than being announced twice.
 */
enum class Glyphs {
    Check,
    Plus,
    Minus,
    ArrowLeft,
    ArrowRight,
    Undo,
    Lock,
    Spark
}

@Composable
fun Glyph(
    glyph: Glyphs,
    modifier: Modifier = Modifier,
    colour: Color = Color.White,
    size: Int = 20,
    weight: Float = 2.2f
) {
    Canvas(
        modifier = modifier
            .size(size.dp)
            .clearAndSetSemantics { }
    ) {
        val u = this.size.minDimension / 24f
        val stroke = Stroke(
            width = weight * u,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        when (glyph) {
            Glyphs.Check -> drawPathOf(u, stroke, colour) {
                moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 6.5f)
            }

            Glyphs.Plus -> {
                line(u, stroke, colour, 12f, 5f, 12f, 19f)
                line(u, stroke, colour, 5f, 12f, 19f, 12f)
            }

            Glyphs.Minus -> line(u, stroke, colour, 5f, 12f, 19f, 12f)

            Glyphs.ArrowLeft -> {
                line(u, stroke, colour, 19f, 12f, 5f, 12f)
                drawPathOf(u, stroke, colour) {
                    moveTo(11f, 6f); lineTo(5f, 12f); lineTo(11f, 18f)
                }
            }

            Glyphs.ArrowRight -> {
                line(u, stroke, colour, 5f, 12f, 19f, 12f)
                drawPathOf(u, stroke, colour) {
                    moveTo(13f, 6f); lineTo(19f, 12f); lineTo(13f, 18f)
                }
            }

            // A counter-clockwise arc with a head at its start: "put that one back".
            Glyphs.Undo -> {
                drawPathOf(u, stroke, colour) {
                    moveTo(4f, 9f); lineTo(9f, 9f)
                }
                drawPathOf(u, stroke, colour) {
                    moveTo(4f, 9f); lineTo(4f, 4f)
                }
                drawPathOf(u, stroke, colour) {
                    moveTo(4.5f, 9.5f)
                    cubicTo(8f, 4f, 17f, 4.5f, 19f, 11f)
                    cubicTo(20.4f, 15.6f, 17.4f, 20f, 12.5f, 20f)
                }
            }

            Glyphs.Lock -> {
                drawPathOf(u, stroke, colour) {
                    moveTo(8f, 10.5f); lineTo(8f, 7.5f)
                    cubicTo(8f, 4.9f, 9.8f, 3.5f, 12f, 3.5f)
                    cubicTo(14.2f, 3.5f, 16f, 4.9f, 16f, 7.5f)
                    lineTo(16f, 10.5f)
                }
                drawRect(
                    color = colour,
                    topLeft = Offset(5.5f * u, 10.5f * u),
                    size = androidx.compose.ui.geometry.Size(13f * u, 10f * u),
                    style = stroke
                )
            }

            // Four-pointed star. Unlocks and XP.
            Glyphs.Spark -> drawPathOf(u, stroke, colour) {
                moveTo(12f, 3f)
                cubicTo(13f, 9f, 15f, 11f, 21f, 12f)
                cubicTo(15f, 13f, 13f, 15f, 12f, 21f)
                cubicTo(11f, 15f, 9f, 13f, 3f, 12f)
                cubicTo(9f, 11f, 11f, 9f, 12f, 3f)
                close()
            }
        }
    }
}

private fun DrawScope.line(
    u: Float,
    stroke: Stroke,
    colour: Color,
    x1: Float, y1: Float, x2: Float, y2: Float
) = drawLine(
    color = colour,
    start = Offset(x1 * u, y1 * u),
    end = Offset(x2 * u, y2 * u),
    strokeWidth = stroke.width,
    cap = StrokeCap.Round
)

/** Path building in glyph units, so the shapes above read as coordinates on a 24-grid. */
private fun DrawScope.drawPathOf(
    u: Float,
    stroke: Stroke,
    colour: Color,
    build: PathBuilder.() -> Unit
) {
    val path = Path()
    PathBuilder(path, u).build()
    drawPath(path = path, color = colour, style = stroke)
}

private class PathBuilder(private val path: Path, private val u: Float) {
    fun moveTo(x: Float, y: Float) = path.moveTo(x * u, y * u)
    fun lineTo(x: Float, y: Float) = path.lineTo(x * u, y * u)
    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        path.cubicTo(x1 * u, y1 * u, x2 * u, y2 * u, x3 * u, y3 * u)
    fun close() = path.close()
}
