package com.debubble.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Served
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * One of the day's offers.
 *
 * Built to be obviously a button: a filled panel with a visible border, the pillar's colour
 * on a chunky left edge, and an explicit "Start" affordance in the corner. The previous
 * version relied on the whole card being tappable with nothing indicating that it was.
 */
@Composable
fun ChallengeCard(
    served: Served,
    done: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillar = served.pillar
    val shape = RoundedCornerShape(Space.radius)

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Intrinsic min height so the accent edge below can fillMaxHeight — inside a
            // scrolling column the incoming height constraint is infinite otherwise.
            .height(IntrinsicSize.Min)
            .panel(
                shape = shape,
                fill = if (done) Ink.Void else Ink.Surface,
                border = if (done) Ink.Faint else Ink.Border
            )
            .semantics { stateDescription = if (done) "Done today" else "Not started" }
            .then(
                if (done) Modifier
                else Modifier.clickable(role = Role.Button, onClick = onClick)
            )
    ) {
        // The pillar's colour as a solid edge. Reads instantly, and does not depend on the
        // user having learned which accent means what.
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(if (done) Ink.Faint else pillar.accent)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Pill(
                    text = pillar.display,
                    colour = if (done) Ink.Muted else pillar.accent
                )
                Label(
                    if (served.kind == "MISSION") "Step ${served.tier} of 30"
                    else "Level ${served.tier}"
                )
            }

            Text(
                text = served.directive,
                color = if (done) Ink.Muted else Ink.Primary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Pill(Engine.formatMinutes(served.minutes), Ink.Muted)
                Pill(if (served.cost == 0) "Free" else "Costs ${served.cost}", Ink.Muted)
                Pill("Nerve ${served.exposure}/10", if (done) Ink.Muted else Ink.Ember)
            }

            if (served.substituted && served.substitutionReason != null && !done) {
                Text(
                    text = served.substitutionReason,
                    color = pillar.accent,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // The call to action, or the receipt.
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (done) {
                    CheckBox(selected = true, accent = Ink.Activity, size = 20)
                    Label("Done today", color = Ink.Activity, strong = true)
                } else {
                    Label("Start this", color = pillar.accent, strong = true)
                    Glyph(Glyphs.ArrowRight, colour = pillar.accent, size = 17)
                }
            }
        }
    }
}

/** Progress up one pillar's hundred levels. */
@Composable
fun PillarBar(pillar: Pillar, tier: Int, modifier: Modifier = Modifier) {
    LabelledProgress(
        label = pillar.display,
        value = "Level $tier of 100",
        fraction = tier / 100f,
        accent = pillar.accent,
        modifier = modifier
    )
}

/**
 * The release, fired the instant a hold closes: one ring crossing the whole surface.
 * The particle burst belongs to the completion screen a beat later — firing both at once
 * turns a decisive moment into noise.
 */
@Composable
fun Shockwave(
    accent: Color,
    origin: Float = 0.72f,
    modifier: Modifier = Modifier
) {
    val p = remember { Animatable(0f) }
    val reduced = rememberReducedMotion()
    LaunchedEffect(Unit) {
        if (reduced) p.snapTo(1f)
        else p.animateTo(1f, tween(durationMillis = 460, easing = LinearOutSlowInEasing))
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val v = p.value
        if (v <= 0f || v >= 1f) return@Canvas
        val centre = Offset(size.width / 2f, size.height * origin)
        val reach = kotlin.math.sqrt(size.width * size.width + size.height * size.height)
        drawCircle(
            color = accent.copy(alpha = 0.55f * (1f - v)),
            radius = 40f + v * reach,
            center = centre,
            style = Stroke(width = (7f * (1f - v)).coerceAtLeast(1f)),
            blendMode = BlendMode.Plus
        )
        drawCircle(
            color = accent.copy(alpha = 0.10f * (1f - v)),
            radius = v * reach * 0.72f,
            center = centre,
            blendMode = BlendMode.Plus
        )
    }
}

/** A plain horizontal rule. */
@Composable
fun Divider(modifier: Modifier = Modifier, colour: Color = Ink.Border) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colour.copy(alpha = 0.6f))
    )
}
