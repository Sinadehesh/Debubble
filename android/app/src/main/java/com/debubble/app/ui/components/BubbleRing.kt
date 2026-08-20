package com.debubble.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Pillar
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.accent
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * What the ring is showing.
 *
 * This replaces the AGSL membrane. That version was a genuinely nice piece of shader work and
 * it was also a soft blur that only ran on API 33+, needed a separate fallback path, and told
 * the user almost nothing they could read. This is three crisp closed curves — one per pillar,
 * radius bound to that pillar's level — drawn as vectors at any resolution, on every device,
 * with no shader and no fallback.
 */
data class RingState(
    val access: Float,
    val activity: Float,
    val social: Float,
    /** 0 = active today, 1 = a week or more away. Slows the breath; nothing shrinks. */
    val rest: Float,
    /** 0..1, how far up the ladder overall. Drives brightness only. */
    val level: Float
) {
    companion object {
        fun from(tiers: Map<Pillar, Int>, daysSinceActive: Long): RingState {
            val mean = Pillar.order.map { tiers[it] ?: 1 }.average().toFloat()
            return RingState(
                access = Engine.radius(tiers[Pillar.ACCESS] ?: 1),
                activity = Engine.radius(tiers[Pillar.ACTIVITY] ?: 1),
                social = Engine.radius(tiers[Pillar.SOCIAL] ?: 1),
                rest = (daysSinceActive.toFloat() / 7f).coerceIn(0f, 1f),
                level = ((mean - 1f) / 99f).coerceIn(0f, 1f)
            )
        }
    }
}

/**
 * The bubble: three overlapping perimeters that breathe, and pulse when touched.
 *
 * Each pillar's curve is a circle gently deformed by two low-frequency sine terms, so it reads
 * as something alive rather than as a progress dial, while every line stays a clean stroke.
 * Touching it fires a ring outward from the touch point — the one piece of the old design that
 * people reliably poked at, now with something to poke.
 */
@Composable
fun BubbleRing(
    state: RingState,
    modifier: Modifier = Modifier,
    pulse: Pillar? = null,
    animate: Boolean = true
) {
    val phase by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            // 60 BPM at rest, slowing to about 40 when the user has been away.
            animation = tween(4000 + (state.rest * 2000).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath"
    )

    val touch = remember { Animatable(0f) }
    var touchAt by remember { mutableStateOf(Offset.Unspecified) }
    val scope = rememberCoroutineScope()

    // Fired by the caller after a completion, from the centre.
    val wave = remember { Animatable(0f) }
    LaunchedEffect(pulse) {
        if (pulse != null) {
            wave.snapTo(0f)
            wave.animateTo(1f, tween(900, easing = LinearEasing))
        }
    }

    val breath = if (animate) phase else 0f

    Canvas(
        modifier = modifier
            .semantics {
                contentDescription = "Your bubble. Access level ${(state.access * 100).toInt()}, " +
                    "activity ${(state.activity * 100).toInt()}, social ${(state.social * 100).toInt()} percent of the way out."
            }
            .pointerInput(Unit) {
                detectTapGestures { at ->
                    touchAt = at
                    scope.launch {
                        touch.snapTo(0f)
                        touch.animateTo(1f, tween(620, easing = LinearEasing))
                    }
                }
            }
    ) {
        val unit = minOf(size.width, size.height)
        val centre = Offset(size.width / 2f, size.height / 2f)

        drawGrid(centre, unit, state.level)

        val radii = listOf(state.access, state.activity, state.social)
        Pillar.order.forEachIndexed { i, pillar ->
            val base = -PI.toFloat() / 2f + i * (2f * PI.toFloat() / 3f)
            val offset = unit * 0.075f
            val c = Offset(
                centre.x + cos(base) * offset,
                centre.y + sin(base) * offset
            )
            // Breathing is a radius modulation, staggered per pillar so the three curves
            // never pulse in lockstep and the whole thing looks like one organism.
            val swell = 1f + sin(breath + i * 2.1f) * 0.022f
            val r = radii[i] * unit * 0.40f * swell
            drawBlob(
                centre = c,
                radius = r,
                colour = pillar.accent,
                phase = breath + i * 1.7f,
                glow = 0.20f + state.level * 0.45f
            )
        }

        // Touch pulse, from wherever the finger landed.
        if (touch.value > 0f && touch.value < 1f && touchAt != Offset.Unspecified) {
            val t = touch.value
            drawCircle(
                color = Ink.Primary.copy(alpha = 0.42f * (1f - t)),
                radius = unit * 0.03f + t * unit * 0.42f,
                center = touchAt,
                style = Stroke(width = (2.5.dp.toPx() * (1f - t)).coerceAtLeast(1f))
            )
        }

        // Completion pulse, from the centre, in the pillar's colour.
        if (pulse != null && wave.value > 0f && wave.value < 1f) {
            val t = wave.value
            drawCircle(
                color = pulse.accent.copy(alpha = 0.55f * (1f - t)),
                radius = unit * 0.08f + t * unit * 0.55f,
                center = centre,
                style = Stroke(width = (4.dp.toPx() * (1f - t)).coerceAtLeast(1f))
            )
        }

        // The user, at the centre of their own map.
        drawCircle(color = Ink.Primary, radius = 3.dp.toPx(), center = centre)
    }
}

/**
 * One pillar's perimeter: a closed curve sampled around a circle whose radius wobbles with
 * two low harmonics. 96 samples is smooth to the eye at any phone size and costs nothing.
 */
private fun DrawScope.drawBlob(
    centre: Offset,
    radius: Float,
    colour: Color,
    phase: Float,
    glow: Float
) {
    val path = Path()
    val steps = 96
    for (i in 0..steps) {
        val a = i * 2f * PI.toFloat() / steps
        val wobble = 1f +
            sin(a * 3f + phase) * 0.030f +
            sin(a * 5f - phase * 0.7f) * 0.017f
        val r = radius * wobble
        val x = centre.x + cos(a) * r
        val y = centre.y + sin(a) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    // A soft interior so overlaps read as denser, then a crisp edge on top. The fill is
    // deliberately weak — the line is the information.
    drawPath(path = path, color = colour.copy(alpha = 0.10f + glow * 0.10f))
    drawPath(
        path = path,
        color = colour,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

/** A faint polar grid, so the curves have something to be measured against. */
private fun DrawScope.drawGrid(centre: Offset, unit: Float, level: Float) {
    repeat(4) { i ->
        drawCircle(
            color = Ink.Faint.copy(alpha = 0.30f - i * 0.055f),
            radius = unit * 0.11f * (i + 1),
            center = centre,
            style = Stroke(width = 1f)
        )
    }
    repeat(12) { a ->
        val theta = a * PI.toFloat() / 6f
        val start = Offset(
            centre.x + cos(theta) * unit * 0.08f,
            centre.y + sin(theta) * unit * 0.08f
        )
        val end = Offset(
            centre.x + cos(theta) * unit * 0.46f,
            centre.y + sin(theta) * unit * 0.46f
        )
        drawLine(
            brush = Brush.linearGradient(
                0f to Ink.Faint.copy(alpha = 0.22f + level * 0.10f),
                1f to Color.Transparent,
                start = start,
                end = end
            ),
            start = start,
            end = end,
            strokeWidth = 1f
        )
    }
}
