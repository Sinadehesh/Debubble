package com.debubble.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.AvatarState
import com.debubble.app.engine.Progress
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The avatar, drawn on a 100-unit square grid.
 *
 * Everything is a primitive — circles, rounded rectangles, a hexagon, two arcs. Nothing here
 * is an asset, so it recolours instantly, scales to any size without a second file, and adds
 * nothing to the APK.
 *
 * The geometry is fixed: the figure occupies y 16..80 with its centre of mass at (50, 48),
 * and the armour rings start at radius 35 — outside the figure's half-extent of 32 — so a
 * fully armoured avatar never has a plate cutting through its own head.
 */

/** The body colours, indexed by [AvatarState.body]. */
private val BODY_COLOURS = listOf(
    Color(0xFF606E84), // Slate
    Color(0xFF4DA3FF), // Ocean
    Color(0xFF3DDC97), // Moss
    Color(0xFFD67C4C), // Clay
    Color(0xFFA871D6), // Plum
    Color(0xFFFFB020)  // Ember
)

private val STEEL = Color(0xFF96A5BA)
private val DARK = Color(0xFF0B0E13)
private val LIGHT = Color(0xFFF5F7FA)

fun avatarColour(index: Int): Color = BODY_COLOURS[index.coerceIn(BODY_COLOURS.indices)]

private const val HX = 50f
private const val HY = 30f
private const val HR = 14f
private const val BODY_TOP = 46f
private const val BODY_BOTTOM = 80f
private const val BODY_LEFT = 30f
private const val BODY_RIGHT = 70f
private const val CX = 50f
private const val CY = 48f

/**
 * @param friction total friction events, which is what armour is made of
 * @param breathe gentle idle motion; off for list thumbnails and when the system asks for
 *   reduced animation
 */
@Composable
fun Avatar(
    avatar: AvatarState,
    friction: Int,
    modifier: Modifier = Modifier,
    size: Int = 120,
    breathe: Boolean = true
) {
    val reduced = rememberReducedMotion()
    val animate = breathe && !reduced

    val phase by rememberInfiniteTransition(label = "avatar").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath"
    )

    val plates = Progress.plates(friction)
    val aura = Progress.auraStrength(friction)

    Box(
        modifier = modifier
            .size(size.dp)
            .semantics {
                contentDescription = describe(avatar, plates)
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val u = this.size.minDimension / 100f
            val lift = if (animate) sin(phase) * 0.9f else 0f

            drawBackdrop(avatar.backdrop, u)
            if (aura > 0f) drawAura(aura, u)
            drawPlates(plates, u, if (animate) phase else 0f)

            // The figure breathes as one piece rather than stretching.
            translate(top = lift * u) {
                drawFigure(avatar, u)
            }
        }
    }
}

private fun DrawScope.drawFigure(a: AvatarState, u: Float) {
    val colour = avatarColour(a.body)

    // The hood sits behind the head, so it is drawn before anything else on the figure.
    if (a.shirt == 3) {
        drawCircle(
            color = colour.darken(0.70f),
            radius = (HR + 6f) * u,
            center = Offset(HX * u, (HY + 5f) * u)
        )
    }

    drawBody(a.shape, colour, u)
    drawShirt(a.shirt, colour, u)

    drawCircle(color = colour, radius = HR * u, center = Offset(HX * u, HY * u))
    drawCircle(color = DARK, radius = 2.4f * u, center = Offset((HX - 5f) * u, (HY + 1f) * u))
    drawCircle(color = DARK, radius = 2.4f * u, center = Offset((HX + 5f) * u, (HY + 1f) * u))

    drawHat(a.hat, u)
}

private fun DrawScope.drawBody(shape: Int, colour: Color, u: Float) {
    when (shape) {
        0 -> drawRoundRect(
            color = colour,
            topLeft = Offset(BODY_LEFT * u, BODY_TOP * u),
            size = Size((BODY_RIGHT - BODY_LEFT) * u, (BODY_BOTTOM - BODY_TOP) * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(17f * u)
        )

        1 -> drawRoundRect(
            color = colour,
            topLeft = Offset(BODY_LEFT * u, BODY_TOP * u),
            size = Size((BODY_RIGHT - BODY_LEFT) * u, (BODY_BOTTOM - BODY_TOP) * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f * u)
        )

        else -> drawPath(
            path = pathOf(u, listOf(50f to 42f, 73f to 63f, 50f to 82f, 27f to 63f)),
            color = colour
        )
    }
}

private fun DrawScope.drawShirt(shirt: Int, colour: Color, u: Float) {
    when (shirt) {
        1 -> listOf(56f, 63f, 70f).forEach { y ->
            drawLine(
                color = LIGHT.copy(alpha = 0.85f),
                start = Offset(33f * u, y * u),
                end = Offset(67f * u, y * u),
                strokeWidth = 2.8f * u,
                cap = StrokeCap.Round
            )
        }

        2 -> {
            drawLine(LIGHT, Offset(43f * u, 47f * u), Offset(50f * u, 57f * u), 2.4f * u, StrokeCap.Round)
            drawLine(LIGHT, Offset(57f * u, 47f * u), Offset(50f * u, 57f * u), 2.4f * u, StrokeCap.Round)
        }

        3 -> drawRoundRect(
            color = colour.darken(0.70f),
            topLeft = Offset(43f * u, 62f * u),
            size = Size(14f * u, 11f * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * u)
        )

        4 -> {
            drawPath(pathOf(u, listOf(39f to 46f, 50f to 57f, 50f to 46f)), colour.darken(0.63f))
            drawPath(pathOf(u, listOf(61f to 46f, 50f to 57f, 50f to 46f)), colour.darken(0.63f))
            drawLine(
                color = DARK.copy(alpha = 0.8f),
                start = Offset(50f * u, 46f * u),
                end = Offset(50f * u, 80f * u),
                strokeWidth = 1.8f * u
            )
        }
    }
}

/**
 * Hats. Every brim stops above the eye line at y = HY + 1, which is the whole reason these
 * are authored as coordinates rather than eyeballed.
 */
private fun DrawScope.drawHat(hat: Int, u: Float) {
    when (hat) {
        1 -> {
            crown(HR, u, Color(0xFF5A6880), 180f, 180f)
            drawRoundRect(
                color = Color(0xFF7080A0),
                topLeft = Offset((HX - HR - 1f) * u, (HY - 5f) * u),
                size = Size((HR * 2f + 2f) * u, 4f * u),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * u)
            )
        }

        2 -> {
            crown(HR, u, Color(0xFF466EB4), 184f, 172f)
            drawRoundRect(
                color = Color(0xFF385C9B),
                topLeft = Offset(HX * u, (HY - 6f) * u),
                size = Size((HR + 13f) * u, 4f * u),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * u)
            )
        }

        3 -> {
            crown(HR - 1f, u, Color(0xFF788460), 184f, 172f)
            drawRoundRect(
                color = Color(0xFF8A966E),
                topLeft = Offset((HX - HR - 7f) * u, (HY - 6f) * u),
                size = Size((HR * 2f + 14f) * u, 5f * u),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * u)
            )
        }

        4 -> {
            val band = Color(0xFF3C4452)
            drawArc(
                color = band,
                startAngle = 196f,
                sweepAngle = 148f,
                useCenter = false,
                topLeft = Offset((HX - HR - 4f) * u, (HY - HR - 4f) * u),
                size = Size((HR + 4f) * 2f * u, (HR + 4f) * 2f * u),
                style = Stroke(width = 3.2f * u, cap = StrokeCap.Round)
            )
            listOf(HX - HR - 6f, HX + HR - 2f).forEach { x ->
                drawRoundRect(
                    color = band,
                    topLeft = Offset(x * u, (HY - 4f) * u),
                    size = Size(8f * u, 12f * u),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * u)
                )
            }
        }
    }
}

/** The filled dome of a hat, sitting on the crown of the head. */
private fun DrawScope.crown(r: Float, u: Float, colour: Color, start: Float, sweep: Float) {
    drawArc(
        color = colour,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = true,
        topLeft = Offset((HX - r) * u, (HY - r) * u),
        size = Size(r * 2f * u, r * 2f * u)
    )
}

private fun DrawScope.drawBackdrop(backdrop: Int, u: Float) {
    when (backdrop) {
        1 -> drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFFFB020).copy(alpha = 0.55f),
                0.55f to Color(0xFFFF7A45).copy(alpha = 0.22f),
                1f to Color.Transparent,
                center = Offset(50f * u, 54f * u),
                radius = 46f * u
            ),
            radius = 46f * u,
            center = Offset(50f * u, 54f * u)
        )

        2 -> {
            drawCircle(
                color = Color(0xFF141A2C),
                radius = 46f * u,
                center = Offset(CX * u, CY * u)
            )
            listOf(
                22f to 24f, 74f to 20f, 82f to 50f, 18f to 58f, 66f to 72f, 32f to 76f
            ).forEach { (x, y) ->
                drawCircle(
                    color = Color(0xFFDCE6FF).copy(alpha = 0.85f),
                    radius = 1.3f * u,
                    center = Offset(x * u, y * u)
                )
            }
        }

        3 -> listOf(47f, 39f, 31f).forEach { r ->
            drawCircle(
                color = Color(0xFF5A697D).copy(alpha = 0.75f),
                radius = r * u,
                center = Offset(CX * u, CY * u),
                style = Stroke(width = 1.1f * u)
            )
        }

        4 -> {
            var i = 6f
            while (i < 100f) {
                drawLine(
                    color = Color(0xFF465264).copy(alpha = 0.6f),
                    start = Offset(i * u, 4f * u), end = Offset(i * u, 96f * u),
                    strokeWidth = 1f * u
                )
                drawLine(
                    color = Color(0xFF465264).copy(alpha = 0.6f),
                    start = Offset(4f * u, i * u), end = Offset(96f * u, i * u),
                    strokeWidth = 1f * u
                )
                i += 14f
            }
        }
    }
}

/**
 * Resilience armour: concentric hexagonal plates, one per four friction events.
 *
 * Hexagons rather than rings because a ring reads as a halo and this is not a reward for
 * being good — it is plate, and it is supposed to look like something that took hits.
 */
private fun DrawScope.drawPlates(plates: Int, u: Float, phase: Float) {
    repeat(plates) { i ->
        val r = (35f + i * 2.6f) * u
        // Each plate turns very slightly out of true with the one inside it, so the stack
        // reads as forged rather than printed.
        val rot = PI.toFloat() / 6f + sin(phase * 0.5f + i) * 0.02f
        val path = Path()
        repeat(6) { k ->
            val a = rot + k * PI.toFloat() / 3f
            val x = CX * u + cos(a) * r
            val y = CY * u + sin(a) * r
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path = path,
            color = STEEL.copy(alpha = 0.88f - i * 0.09f),
            style = Stroke(width = 1.7f * u)
        )
    }
}

/** The glow that keeps growing once every plate has been earned. */
private fun DrawScope.drawAura(strength: Float, u: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.Transparent,
            0.62f to Color(0xFFFFB020).copy(alpha = 0.05f + 0.16f * strength),
            1f to Color.Transparent,
            center = Offset(CX * u, CY * u),
            radius = 50f * u
        ),
        radius = 50f * u,
        center = Offset(CX * u, CY * u)
    )
}

private fun DrawScope.pathOf(u: Float, points: List<Pair<Float, Float>>): Path {
    val p = Path()
    points.forEachIndexed { i, (x, y) ->
        if (i == 0) p.moveTo(x * u, y * u) else p.lineTo(x * u, y * u)
    }
    p.close()
    return p
}

private fun Color.darken(factor: Float): Color =
    Color(red * factor, green * factor, blue * factor, alpha)

private fun describe(a: AvatarState, plates: Int): String {
    val armour = when {
        plates == 0 -> "no armour yet"
        plates == 1 -> "1 plate of resilience armour"
        else -> "$plates plates of resilience armour"
    }
    return "Your avatar, wearing ${Progress.hats[a.hat.coerceIn(Progress.hats.indices)].name.lowercase()} " +
        "and ${Progress.shirts[a.shirt.coerceIn(Progress.shirts.indices)].name.lowercase()}, with $armour"
}
