package com.debubble.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Habitat
import com.debubble.app.engine.Stage
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import kotlin.random.Random

/**
 * The room, drawn.
 *
 * Seven stages of one-point perspective: walls and a ceiling that slide outward and fade, a
 * far wall that gets a window, then a door, then disappears, and behind it a horizon, stars
 * and a distant skyline. The floor never goes anywhere, because the ground is the one thing
 * that does not stop being true.
 *
 * All of it is procedural. The alternative was seven exported images, which would have been
 * easier to get right and impossible to keep in step with the palette — and the palette here
 * is not decorative, it is the output of a contrast solver (see engine/Habitat.kt and
 * android/tools/check_habitat.py). Geometry carries the change rather than brightness,
 * because in a dark theme brightness is rationed by WCAG and geometry is not.
 *
 * The bottom third fades back to flat ground on purpose. Perspective lines running underneath
 * a paragraph is how a background stops being a background.
 */
@Composable
fun HabitatBackdrop(stage: Stage, modifier: Modifier = Modifier) {
    val ground = Color(stage.ground)
    val horizon = Color(stage.horizon)
    val structure = Color(stage.structure)
    val open = stage.openness

    // Fixed seeds: a sky that reshuffled on every recomposition would be the one animation
    // in this app nobody asked for.
    val stars = remember(stage) {
        val rng = Random(7)
        List((26 * ((open - 0.45f) / 0.55f).coerceAtLeast(0f)).toInt()) {
            Triple(rng.nextFloat(), rng.nextFloat(), 0.5f + rng.nextFloat() * 0.8f)
        }
    }
    val skyline = remember(stage) {
        val rng = Random(11)
        buildList {
            var x = 0f
            while (x < 1f) {
                val w = 0.03f + rng.nextFloat() * 0.06f
                add(Triple(x, w, 0.012f + rng.nextFloat() * 0.043f))
                x += w + 0.005f + rng.nextFloat() * 0.03f
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // The horizon rises as the room opens: you are seeing further, not standing taller.
        val yH = h * (0.58f - 0.10f * open)
        val vx = w * 0.5f

        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to ground,
                    (yH / h).coerceIn(0.05f, 0.95f) to horizon,
                    1f to ground
                )
            ),
            size = size
        )

        fun line(from: Offset, to: Offset, alpha: Float, width: Float = 1f) {
            if (alpha <= 0.01f) return
            drawLine(structure.copy(alpha = alpha), from, to, strokeWidth = width)
        }

        // Floor: the one part of the room that is there at every stage.
        for (i in -3..3) {
            line(Offset(vx, yH), Offset(vx + i * w * 0.30f, h), 0.30f)
        }
        for (k in 1..4) {
            val y = yH + (h - yH) * pow(k / 5f, 2.1f)
            line(Offset(0f, y), Offset(w, y), 0.16f)
        }

        // Walls and ceiling: slide outward and fade out as the room opens.
        val wall = 1f - open
        if (wall > 0.01f) {
            listOf(-1f, 1f).forEach { side ->
                val xo = vx + side * w * (0.14f + 0.46f * open)
                line(Offset(xo, -h * 0.08f), Offset(xo, h), 0.55f * wall, 2f)
                line(Offset(xo, 0f), Offset(vx, yH), 0.40f * wall)
                line(Offset(xo, h), Offset(vx, yH), 0.26f * wall)
            }
            val yc = h * 0.06f - h * 0.25f * open
            line(Offset(0f, yc), Offset(w, yc), 0.34f * wall)
        }

        // The far wall, and what is cut into it. Gone by the fourth stage.
        val far = (1f - open * 2.2f).coerceAtLeast(0f)
        if (far > 0.01f) {
            val fw = w * 0.30f
            val fh = h * 0.26f
            drawRect(
                color = structure.copy(alpha = 0.45f * far),
                topLeft = Offset(vx - fw / 2, yH - fh),
                size = Size(fw, fh),
                style = Stroke(width = 1f)
            )
            when {
                open > 0.08f && open < 0.26f -> {
                    // A window: small, high, and the first light from outside.
                    val ww = fw * 0.34f
                    val wh = fh * 0.30f
                    drawRect(
                        color = horizon.copy(alpha = 0.85f),
                        topLeft = Offset(vx - ww / 2, yH - fh * 0.70f),
                        size = Size(ww, wh)
                    )
                }
                open >= 0.26f -> {
                    // A door: floor to nearly full height. You can walk through this one.
                    val dw = fw * 0.30f
                    drawRect(
                        color = horizon.copy(alpha = 0.90f),
                        topLeft = Offset(vx - dw / 2, yH - fh * 0.82f),
                        size = Size(dw, fh * 0.82f)
                    )
                }
            }
        }

        // What turns out to have been outside the whole time.
        stars.forEach { (fx, fy, r) ->
            drawCircle(
                color = structure.copy(alpha = 0.35f + 0.45f * ((open - 0.45f) / 0.55f).coerceIn(0f, 1f)),
                radius = r,
                center = Offset(fx * w, fy * yH * 0.82f)
            )
        }
        if (open >= 0.62f) {
            val tall = ((open - 0.62f) / 0.38f).coerceIn(0f, 1f)
            skyline.forEach { (x, bw, bh) ->
                val height = h * bh * tall
                drawRect(
                    color = structure.copy(alpha = 0.30f),
                    topLeft = Offset(x * w, yH - height),
                    size = Size(bw * w, height)
                )
            }
        }
        if (open >= 0.45f) {
            line(Offset(0f, yH), Offset(w, yH), 0.45f)
        }

        // Back to flat ground before any text starts.
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.58f to Color.Transparent,
                    1f to ground
                )
            ),
            size = size
        )
    }
}

private fun pow(x: Float, e: Float): Float = Math.pow(x.toDouble(), e.toDouble()).toFloat()

/**
 * The room's name, and how far off the next one is.
 *
 * Says the number of rungs rather than a percentage, because "fourteen more" is a thing a
 * person can picture doing and "62%" is a thing they can only watch. There is no way to lose
 * a stage, which is why this can be shown without it becoming something to protect.
 */
@Composable
fun HabitatRow(stage: Stage, cleared: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel(fill = Color(stage.ground), border = Color(stage.structure))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stage.label,
                color = Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
            Label("$cleared cleared")
        }
        Text(
            text = stage.note,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        ProgressTrack(Habitat.progress(cleared), Ink.Secondary, height = 6)
        Label(Habitat.caption(cleared))
    }
}

/**
 * Shown once, on the day the room opens.
 *
 * Deliberately quiet — no confetti, no full-screen takeover. The room is already different
 * behind it; the banner only names what changed, so the user does not have to wonder whether
 * they imagined it.
 */
@Composable
fun HabitatAnnouncement(stage: Stage, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel(fill = Color(stage.horizon), border = Ink.Gold)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Glyph(Glyphs.Spark, colour = Ink.Gold, size = 18)
            Label("The room changed", color = Ink.Gold, strong = true)
        }
        Text(
            text = stage.label,
            color = Ink.Primary,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stage.note,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        SecondaryButton("Got it", onClick = onDismiss)
    }
}
