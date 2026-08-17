package com.debubble.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Pillar
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.accent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Bubble Map. Three additively-blended spheres on a faint polar grid, one per pillar,
 * with radius bound to that pillar's tier. Overlaps light up where pillars reinforce each
 * other, which is the whole point of drawing them on one field instead of three gauges.
 *
 * It reports and does not respond: there is no gesture handling here on purpose. The moment
 * it becomes a toy it stops reading as an instrument.
 *
 * @param pulse set to a pillar to fire a one-shot shockwave when that pillar grows.
 */
@Composable
fun BubbleMap(
    tiers: Map<Pillar, Int>,
    modifier: Modifier = Modifier,
    pulse: Pillar? = null,
    animate: Boolean = true
) {
    // Radii spring to their target so a cleared tier is visibly *felt* as growth.
    val radii = Pillar.order.map { pillar ->
        val target = Engine.radius(tiers[pillar] ?: 1)
        pillar to animateFloatAsState(
            targetValue = target,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 90f),
            label = "radius_${pillar.name}"
        )
    }

    // Always composed, so the composition shape does not change with `animate`.
    val driftRaw by rememberInfiniteTransition(label = "drift").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(38_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "driftAngle"
    )
    val drift = if (animate) driftRaw else 0f

    val wave by animateFloatAsState(
        targetValue = if (pulse != null) 1f else 0f,
        animationSpec = tween(if (pulse != null) 1100 else 0, easing = LinearEasing),
        label = "shockwave"
    )

    Canvas(modifier = modifier) {
        val unit = minOf(size.width, size.height)
        val centre = Offset(size.width / 2f, size.height / 2f)

        drawPolarGrid(centre, unit)

        // Additive blending is what makes the overlaps glow without hand-mixing colours.
        radii.forEachIndexed { index, (pillar, radiusState) ->
            val r = radiusState.value * unit * 0.42f
            val base = -PI.toFloat() / 2f + index * (2f * PI.toFloat() / 3f)
            val wobble = if (animate) sin(drift + index * 2.1f) * 0.09f else 0f
            val offset = unit * (0.085f + if (animate) sin(drift * 0.8f + index) * 0.012f else 0f)
            val c = Offset(
                centre.x + cos(base + wobble) * offset,
                centre.y + sin(base + wobble) * offset
            )
            val colour = pillar.accent

            drawCircle(
                brush = Brush.radialGradient(
                    0f to colour.copy(alpha = 0.38f),
                    0.55f to colour.copy(alpha = 0.17f),
                    1f to Color.Transparent,
                    center = c,
                    radius = r
                ),
                radius = r,
                center = c,
                blendMode = BlendMode.Plus
            )
            drawCircle(
                color = colour.copy(alpha = 0.72f),
                radius = r,
                center = c,
                style = Stroke(width = (unit * 0.0035f).coerceAtLeast(1f)),
                blendMode = BlendMode.Plus
            )
            drawCircle(
                color = colour.copy(alpha = 0.9f),
                radius = unit * 0.008f,
                center = c,
                blendMode = BlendMode.Plus
            )
        }

        if (pulse != null && wave > 0f && wave < 1f) {
            drawCircle(
                color = pulse.accent.copy(alpha = 0.55f * (1f - wave)),
                radius = unit * 0.1f + wave * unit * 0.55f,
                center = centre,
                style = Stroke(width = (unit * 0.006f * (1f - wave)).coerceAtLeast(0.5f)),
                blendMode = BlendMode.Plus
            )
        }

        // The user, at the centre of their own map.
        drawCircle(color = Ink.Primary, radius = unit * 0.011f, center = centre)
    }
}

private fun DrawScope.drawPolarGrid(centre: Offset, unit: Float) {
    repeat(4) { i ->
        drawCircle(
            color = Color(0xFF1A2029),
            radius = unit * 0.11f * (i + 1),
            center = centre,
            style = Stroke(width = 1f)
        )
    }
    repeat(12) { a ->
        val theta = a * PI.toFloat() / 6f
        drawLine(
            color = Color(0xFF141A22),
            start = Offset(
                centre.x + cos(theta) * unit * 0.08f,
                centre.y + sin(theta) * unit * 0.08f
            ),
            end = Offset(
                centre.x + cos(theta) * unit * 0.48f,
                centre.y + sin(theta) * unit * 0.48f
            ),
            strokeWidth = 1f
        )
    }
}
