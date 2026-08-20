package com.debubble.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Pillar
import com.debubble.app.ui.components.Dot
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.rememberReducedMotion
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The three-card explainer, shown once before anything else.
 *
 * The app previously dropped a first-time user straight into a calibration questionnaire with
 * no statement of what it was for. Three cards is the smallest thing that answers "what is
 * this", "what does it measure" and "why does it reward the thing I am afraid of" — and the
 * third one matters most, because friction is the mechanic nobody expects.
 */
@Composable
fun IntroScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val steps = 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
            .padding(horizontal = Space.gutter)
            .padding(top = 24.dp, bottom = Space.gutter)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(steps) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i <= step) Ink.Primary else Ink.Faint)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            when (step) {
                0 -> IntroCard(
                    kicker = "Step 1 of 3",
                    title = "What is your bubble?",
                    body = "Your bubble is everywhere you already go and everything you " +
                        "already do. It is comfortable, and it gets smaller every year " +
                        "without you noticing.\n\n" +
                        "Same streets. Same three people. Same evening. Nothing is wrong " +
                        "with any of it — it just keeps shrinking, quietly, on its own.\n\n" +
                        "This app makes it bigger on purpose. A little at a time.",
                    art = { BubbleArt(it) }
                )

                1 -> IntroCard(
                    kicker = "Step 2 of 3",
                    title = "Three directions out",
                    body = "Your bubble has three edges, and you push on all of them " +
                        "separately. You get one small challenge in each, every day.",
                    art = { VectorArt() },
                    extra = { VectorList() }
                )

                else -> IntroCard(
                    kicker = "Step 3 of 3",
                    title = "Getting turned down is worth the most",
                    body = "Most apps only count wins. This one counts the awkward stuff " +
                        "highest.\n\n" +
                        "Ask and get a no. Start a conversation that dies. Turn back " +
                        "halfway. Every one of those earns Friction — the score you " +
                        "actually want.\n\n" +
                        "Friction is proof you reached the edge of what is comfortable. " +
                        "Nobody who stayed inside their bubble ever earns any.",
                    art = { FrictionArt(it) }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PrimaryButton(
                text = if (step == steps - 1) "Set me up" else "Next",
                accent = when (step) {
                    0 -> Ink.Access
                    1 -> Ink.Activity
                    else -> Ink.Ember
                }
            ) {
                if (step == steps - 1) onDone() else step++
            }
            if (step > 0) {
                TextAction("Back") { step-- }
            } else {
                TextAction("Skip the intro") { onDone() }
            }
        }
    }
}

@Composable
private fun IntroCard(
    kicker: String,
    title: String,
    body: String,
    art: @Composable (Float) -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    val reduced = rememberReducedMotion()
    val phase by rememberInfiniteTransition(label = "intro").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "art"
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            contentAlignment = Alignment.Center
        ) {
            art(if (reduced) 0f else phase)
        }
        VSpace(24)
        Label(kicker)
        VSpace(8)
        Text(
            text = title,
            color = Ink.Primary,
            style = MaterialTheme.typography.displayMedium
        )
        VSpace(14)
        Text(
            text = body,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyLarge
        )
        if (extra != null) {
            VSpace(18)
            extra()
        }
    }
}

/** A small life inside a large boundary that keeps closing in on it. */
@Composable
private fun BubbleArt(phase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val unit = minOf(size.width, size.height)
        val c = Offset(size.width / 2f, size.height / 2f)

        // The world, most of which is outside.
        repeat(3) { i ->
            drawCircle(
                color = Ink.Faint.copy(alpha = 0.55f - i * 0.14f),
                radius = unit * (0.30f + i * 0.11f),
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        // The bubble, breathing in a little and out a little but never getting anywhere.
        val r = unit * (0.17f + sin(phase) * 0.012f)
        drawCircle(color = Ink.Access.copy(alpha = 0.20f), radius = r, center = c)
        drawCircle(
            color = Ink.Access,
            radius = r,
            center = c,
            style = Stroke(width = 2.5.dp.toPx())
        )
        drawCircle(color = Ink.Primary, radius = 3.dp.toPx(), center = c)
    }
}

/** Three arrows out of one centre, one per pillar. */
@Composable
private fun VectorArt() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val unit = minOf(size.width, size.height)
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = Ink.Faint.copy(alpha = 0.5f),
            radius = unit * 0.40f,
            center = c,
            style = Stroke(width = 1.dp.toPx())
        )
        Pillar.order.forEachIndexed { i, pillar ->
            val a = -PI.toFloat() / 2f + i * (2f * PI.toFloat() / 3f)
            val from = Offset(c.x + cos(a) * unit * 0.09f, c.y + sin(a) * unit * 0.09f)
            val to = Offset(c.x + cos(a) * unit * 0.44f, c.y + sin(a) * unit * 0.44f)
            drawLine(
                color = pillar.accent,
                start = from,
                end = to,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            arrowHead(to, a, unit * 0.045f, pillar.accent)
        }
        drawCircle(color = Ink.Primary, radius = 4.dp.toPx(), center = c)
    }
}

private fun DrawScope.arrowHead(tip: Offset, angle: Float, size: Float, colour: Color) {
    val spread = 0.45f
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(
            tip.x - cos(angle - spread) * size * 1.8f,
            tip.y - sin(angle - spread) * size * 1.8f
        )
        lineTo(
            tip.x - cos(angle + spread) * size * 1.8f,
            tip.y - sin(angle + spread) * size * 1.8f
        )
        close()
    }
    drawPath(path = path, color = colour)
}

@Composable
private fun VectorList() {
    val rows = listOf(
        Triple(Pillar.ACCESS, "Access", "Where you go. New streets, new places."),
        Triple(Pillar.ACTIVITY, "Activity", "What you do. Things you have never tried."),
        Triple(Pillar.SOCIAL, "Social", "Who you talk to. Strangers included.")
    )
    Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
        rows.forEach { (pillar, name, what) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Dot(pillar.accent, size = 10)
                Column {
                    Text(
                        text = name,
                        color = pillar.accent,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = what,
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/** A boundary being crossed, and the spark it throws off. */
@Composable
private fun FrictionArt(phase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val unit = minOf(size.width, size.height)
        val c = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = Ink.Faint,
            radius = unit * 0.32f,
            center = c,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Something leaving, past the line.
        val t = (sin(phase) + 1f) / 2f
        val d = unit * (0.18f + t * 0.30f)
        val p = Offset(c.x + d, c.y - d * 0.25f)

        drawLine(
            color = Ink.Ember.copy(alpha = 0.45f),
            start = c,
            end = p,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = Ink.Ember, radius = unit * 0.045f, center = p)

        // Sparks where it crossed.
        repeat(7) { i ->
            val a = i * (2f * PI.toFloat() / 7f) + phase * 0.4f
            val burst = unit * (0.06f + t * 0.05f)
            drawLine(
                color = Ink.Ember.copy(alpha = 0.30f + t * 0.45f),
                start = Offset(p.x + cos(a) * burst * 0.6f, p.y + sin(a) * burst * 0.6f),
                end = Offset(p.x + cos(a) * burst * 1.5f, p.y + sin(a) * burst * 1.5f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
