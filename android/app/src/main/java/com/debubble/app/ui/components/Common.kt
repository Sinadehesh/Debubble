package com.debubble.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Served
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.InstrumentFamily
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * How far open the interface is, 0..1, supplied at the root from the mean tier.
 *
 * Page 10 of the design doctrine: the app should visually stop being able to contain the life
 * it is measuring. Rather than a switch at some threshold, every surface that reads it opens
 * continuously — nobody should be able to point at the day it changed.
 */
val LocalAscension = androidx.compose.runtime.compositionLocalOf { 0f }

/** Widely tracked capitals in the serif: labels read as engraving, not as UI chrome. */
@Composable
fun Instrument(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ink.Dim,
    small: Boolean = false
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        style = if (small) MaterialTheme.typography.labelSmall
        else MaterialTheme.typography.labelMedium
    )
}

fun tierCode(tier: Int): String = tier.toString().padStart(3, '0')

/**
 * One of the day's three offers. The card *is* the button — there is no separate CTA,
 * because a second tap target on a card this size is just a smaller card.
 */
@Composable
fun ChallengeCard(
    served: Served,
    done: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillar = served.pillar
    // Low on the ladder a card is a tight box in a void. High up the edges give way and the
    // accent starts to leak past them.
    val open = LocalAscension.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            // Sfumato: no outline anywhere. A card is a surface catching light from the upper
            // left and falling off into the dark, which is what stops the screen reading as a
            // dashboard of boxes.
            .background(
                Brush.linearGradient(
                    0f to Ink.Primary.copy(alpha = (if (done) 0.018f else 0.055f) + open * 0.030f),
                    0.6f to Ink.Primary.copy(alpha = (if (done) 0.004f else 0.012f)),
                    1f to Color.Transparent
                )
            )
            .drawLeftRail(pillar.accent)
            // Done is signalled visually by strikethrough and dimming alone, so the state has
            // to be spoken as well.
            .semantics { stateDescription = if (done) "Completed today" else "Not started" }
            .then(
                if (done) Modifier.alpha(0.42f)
                else Modifier.clickable(role = Role.Button, onClick = onClick)
            )
            .padding(start = 15.dp, top = 14.dp, end = 15.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(Space.gap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Instrument(pillar.display, color = pillar.accent, small = true)
            Instrument("Tier ${tierCode(served.tier)} · ${pillar.code}", small = true)
        }
        Text(
            text = served.directive,
            color = Ink.Primary,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Instrument(Engine.formatMinutes(served.minutes), small = true)
            Instrument(if (served.cost == 0) "Free" else "${served.cost} spend", small = true)
            Instrument("Exposure ${served.exposure}/10", small = true)
        }
        if (served.substituted && served.substitutionReason != null && !done) {
            Instrument(served.substitutionReason, color = pillar.accent.copy(alpha = 0.75f), small = true)
        }
    }
}

/**
 * The pillar rail: the only chrome identifying a card, and it fades out at both ends rather
 * than stopping. A hard-terminated bar is a UI element; a rail that dissolves is lit edge.
 */
private fun Modifier.drawLeftRail(colour: Color): Modifier =
    drawWithContent {
        drawContent()
        val inset = size.height * 0.10f
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to colour,
                1f to Color.Transparent,
                startY = inset,
                endY = size.height - inset
            ),
            topLeft = Offset(0f, inset),
            size = Size(2.dp.toPx(), size.height - inset * 2f)
        )
    }

/**
 * The reward. Four concentric geometric frames expanding from the press point, rotating 45°,
 * fading at the edge.
 *
 * Not confetti: confetti is a slot machine telling you that you won something. This is a
 * perimeter visibly getting bigger, which is the only thing that actually happened.
 */
@Composable
fun ExpansionBurst(
    accent: Color,
    modifier: Modifier = Modifier
) {
    // animateFloatAsState would start *at* its target on first composition and never move,
    // so the burst is driven explicitly from 0 on first appearance.
    val progress = remember { Animatable(0f) }
    val reducedMotion = rememberReducedMotion()
    LaunchedEffect(reducedMotion) {
        // At 1f every ring has already passed its own cutoff below, so the burst simply does
        // not draw. The haptic and the state change still land; only the motion is dropped.
        if (reducedMotion) progress.snapTo(1f)
        else progress.animateTo(1f, tween(durationMillis = 2000, easing = LinearOutSlowInEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val unit = minOf(size.width, size.height)
        val centre = Offset(size.width / 2f, size.height * 0.42f)
        // Staggered so it reads as one expansion rather than four rings.
        val stagger = listOf(0f, 0.08f, 0.16f, 0.24f)
        stagger.forEachIndexed { i, delay ->
            val p = ((progress.value - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (p <= 0f || p >= 1f) return@forEachIndexed
            val r = unit * (0.12f + p * (0.30f + i * 0.12f))
            val alpha = (1f - p) * 0.7f
            rotate(degrees = 45f, pivot = centre) {
                if (i < 2) {
                    drawRect(
                        color = accent.copy(alpha = alpha),
                        topLeft = Offset(centre.x - r, centre.y - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = 1.5.dp.toPx()),
                        blendMode = BlendMode.Plus
                    )
                } else {
                    drawCircle(
                        color = accent.copy(alpha = alpha),
                        radius = r,
                        center = centre,
                        style = Stroke(width = 1.5.dp.toPx()),
                        blendMode = BlendMode.Plus
                    )
                }
            }
        }
    }
}

/**
 * The release. A single ring crossing the entire surface, fired the instant a hold closes.
 *
 * Deliberately one ring rather than a particle burst: the burst belongs to the completion
 * screen a beat later, and firing both at once turns a decisive moment into noise.
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

/** Thin progress bar to 100, in the pillar's accent. */
@Composable
fun PillarBar(pillar: Pillar, tier: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Instrument(pillar.display, color = pillar.accent, small = true)
            Instrument("${tierCode(tier)} / 100", small = true)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Ink.EdgeSoft)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (tier / 100f).coerceIn(0.01f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(pillar.accent)
            )
        }
    }
}

/** A labelled statistic tile. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Color = Ink.Primary) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Space.radius))
            .background(Ink.Ridge)
            .border(1.dp, Ink.EdgeSoft, RoundedCornerShape(Space.radius))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Instrument(label, small = true)
        Text(
            text = value,
            color = accent,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = InstrumentFamily)
        )
    }
}

@Composable
fun VSpace(height: Int) = Spacer(modifier = Modifier.height(height.dp))

@Composable
fun HSpace(width: Int) = Spacer(modifier = Modifier.width(width.dp))

@Composable
fun Dot(colour: Color, size: Int = 7) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(50))
            .background(colour)
    )
}
