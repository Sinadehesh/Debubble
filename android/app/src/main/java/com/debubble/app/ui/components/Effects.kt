package com.debubble.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.debubble.app.ui.theme.Ink
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The celebration, on completing anything.
 *
 * Particles thrown outward on a fixed random seed, with gravity and drag, plus three
 * expanding rings underneath. The seed is fixed per burst rather than per frame so the
 * particles follow a stable arc instead of jittering.
 *
 * Fully skipped under reduced-motion — the state change and the haptic still land.
 */
@Composable
fun ParticleBurst(
    accent: Color,
    modifier: Modifier = Modifier,
    origin: Float = 0.42f,
    count: Int = 34
) {
    val progress = remember { Animatable(0f) }
    val reduced = rememberReducedMotion()

    // One seed for the life of this burst.
    val seed = remember { Random.nextInt() }
    val particles = remember(seed, count) {
        val rng = Random(seed)
        List(count) {
            Particle(
                angle = rng.nextFloat() * 2f * PI.toFloat(),
                speed = 0.35f + rng.nextFloat() * 0.75f,
                size = 2f + rng.nextFloat() * 4.5f,
                spin = (rng.nextFloat() - 0.5f) * 8f,
                square = rng.nextFloat() < 0.45f,
                tint = rng.nextFloat()
            )
        }
    }

    LaunchedEffect(reduced) {
        if (reduced) progress.snapTo(1f)
        else progress.animateTo(1f, tween(1500, easing = LinearEasing))
    }

    Canvas(modifier = modifier.fillMaxSize().clearAndSetSemantics { }) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas

        val unit = minOf(size.width, size.height)
        val centre = Offset(size.width / 2f, size.height * origin)

        // Rings first, so particles read as being in front of them.
        listOf(0f, 0.13f, 0.26f).forEachIndexed { i, delay ->
            val p = ((t - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (p <= 0f || p >= 1f) return@forEachIndexed
            drawCircle(
                color = accent.copy(alpha = 0.45f * (1f - p)),
                radius = unit * (0.10f + p * (0.34f + i * 0.10f)),
                center = centre,
                style = Stroke(width = ((3f - i) * (1f - p) * 1.6f).coerceAtLeast(0.6f) * density),
                blendMode = BlendMode.Plus
            )
        }

        particles.forEach { p ->
            // Ballistic: constant outward velocity with drag, plus gravity pulling down.
            val travel = p.speed * unit * 0.46f * (1f - (1f - t) * (1f - t))
            val x = centre.x + cos(p.angle) * travel
            val y = centre.y + sin(p.angle) * travel + t * t * unit * 0.34f
            val fade = (1f - t) * (1f - t)
            val colour = when {
                p.tint < 0.55f -> accent
                p.tint < 0.85f -> Ink.Gold
                else -> Ink.Primary
            }.copy(alpha = fade)
            val r = p.size * density * (0.5f + fade * 0.5f)

            if (p.square) {
                rotate(degrees = p.spin * t * 90f, pivot = Offset(x, y)) {
                    drawRect(
                        color = colour,
                        topLeft = Offset(x - r, y - r),
                        size = Size(r * 2f, r * 2f)
                    )
                }
            } else {
                drawCircle(color = colour, radius = r, center = Offset(x, y))
            }
        }
    }
}

private data class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val spin: Float,
    val square: Boolean,
    val tint: Float
)

/**
 * The friction effect: a short burst of horizontal channel slip and scan tears.
 *
 * Deliberately amber rather than red, and deliberately resolving rather than degrading. The
 * app's position is that logging friction is a deposit, not a fault, so this is allowed to
 * look violent for half a second and is not allowed to look like an error state.
 */
@Composable
fun GlitchBurst(
    modifier: Modifier = Modifier,
    durationMillis: Int = 620
) {
    val progress = remember { Animatable(0f) }
    val reduced = rememberReducedMotion()
    val seed = remember { Random.nextInt() }

    val bands = remember(seed) {
        val rng = Random(seed)
        List(9) {
            GlitchBand(
                y = rng.nextFloat(),
                height = 0.012f + rng.nextFloat() * 0.055f,
                shift = (rng.nextFloat() - 0.5f) * 0.22f,
                at = rng.nextFloat() * 0.55f
            )
        }
    }

    LaunchedEffect(reduced) {
        if (reduced) progress.snapTo(1f)
        else progress.animateTo(1f, tween(durationMillis, easing = LinearOutSlowInEasing))
    }

    Canvas(modifier = modifier.fillMaxSize().clearAndSetSemantics { }) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        val envelope = 1f - t

        bands.forEach { b ->
            // Each band tears at its own moment and snaps back, so the whole thing reads as
            // interference rather than as one wipe.
            val local = ((t - b.at) / 0.30f).coerceIn(0f, 1f)
            if (local <= 0f || local >= 1f) return@forEach
            val punch = sin(local * PI.toFloat())
            val dx = b.shift * size.width * punch

            drawRect(
                color = Ink.Ember.copy(alpha = 0.30f * punch * envelope),
                topLeft = Offset(dx, b.y * size.height),
                size = Size(size.width, b.height * size.height),
                blendMode = BlendMode.Plus
            )
            // A hard leading edge on the tear.
            drawLine(
                color = Ink.Ember.copy(alpha = 0.85f * punch * envelope),
                start = Offset(dx, b.y * size.height),
                end = Offset(dx + size.width, b.y * size.height),
                strokeWidth = 1.6f * density,
                cap = StrokeCap.Butt
            )
        }

        // A single sweep down the screen closes it out.
        val sweepY = t * size.height
        drawLine(
            color = Ink.Ember.copy(alpha = 0.35f * envelope),
            start = Offset(0f, sweepY),
            end = Offset(size.width, sweepY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private data class GlitchBand(
    val y: Float,
    val height: Float,
    val shift: Float,
    val at: Float
)
