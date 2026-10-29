package com.debubble.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Domain
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.Stage
import com.debubble.app.engine.Today
import com.debubble.app.ui.components.HouseScene
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
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
 * this", "what will I do every day" and "why does it reward the thing I am afraid of" — and
 * the third one matters most, because friction is the mechanic nobody expects.
 *
 * The second card shows the real room and the real icons, so the first time someone sees
 * the home screen it is already familiar. The last says what setup asks and how long it
 * takes: people stay with a sequence they can see the end of.
 */
@Composable
fun IntroScreen(art: RoomArt, onDone: () -> Unit) {
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

        // Centred when it fits, scrollable when it does not: the second card is the longest
        // and must not be cut off on a small phone.
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // A fresh scroll per card, so the next one does not open halfway down.
                    .verticalScroll(remember(step) { ScrollState(0) })
                    .heightIn(min = maxHeight)
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
                        title = "Four small things a day",
                        body = "Look after your body, your room and your heart, and take one " +
                            "small step outside your bubble. That is the whole day.\n\n" +
                            "You start in an empty room. Everything in it, you earn by doing " +
                            "those things for real.",
                        art = { RoomArtPreview(art) },
                        extra = { FourList(art) }
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
                        art = { FrictionArt(it) },
                        extra = { SetupAhead() }
                    )
                }
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

/** The room you will move into, as it will look: you, your heart, today's letter. */
@Composable
private fun RoomArtPreview(art: RoomArt) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        HouseScene(
            art = art,
            stage = Stage.SEALED,
            home = HomeState(),
            dirt = Domain.order.associateWith { 1 },
            letter = true,
            modifier = Modifier.width(260.dp),
            description = "An empty room, with you in it, your heart beside you, and a letter in your hand."
        )
    }
}

/** The four, each with the icon it has on the home screen and where it lives in the room. */
@Composable
private fun FourList(art: RoomArt) {
    val rows = listOf(
        Triple(Today.icon(Domain.BODY), Domain.BODY.accent, "Body" to "Wash, water, move, eat, sleep. You are your body."),
        Triple(Today.icon(Domain.ROOM), Domain.ROOM.accent, "Room" to "A few minutes of tidying your real room. The mess is your room."),
        Triple(Today.icon(Domain.HEART), Domain.HEART.accent, "Heart" to "Something kind for your mind. The little heart is yours."),
        Triple(Today.ICON_OUT, Ink.Gold, "Outside" to "One small step out of your bubble. The letter in your hand.")
    )
    val scale = pixelScale(2.2f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { (icon, colour, text) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                    SpriteIcon(art, icon, pixelScale = scale)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = text.first, color = colour, style = MaterialTheme.typography.labelLarge)
                    Text(text = text.second, color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** What setup asks, and how long it takes, so the questions ahead have an end in sight. */
@Composable
private fun SetupAhead() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.SurfaceHigh)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Label("Next, about three minutes", color = Ink.Primary, strong = true)
        Text(
            text = "A few questions so the missions fit you, a goal, then you move in and " +
                "pick how you look. After that, a short tour shows you around.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
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
