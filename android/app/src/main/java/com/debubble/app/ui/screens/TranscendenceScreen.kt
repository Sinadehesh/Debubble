package com.debubble.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Pillar
import com.debubble.app.ui.components.Instrument
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.rememberReducedMotion
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tier 100. The end of a ladder, and the end of the app's usefulness on that axis.
 *
 * Everything else in DeBubble is built to keep someone coming back. This screen is built to
 * stop. The interface it has spent a hundred tiers constructing — the map, the counters, the
 * cards — dissolves into particles and light, and what is left is one sentence and a way out.
 *
 * It is the only place the product argues its own thesis out loud: the measure of this app
 * working is that it becomes unnecessary. Which is also the reason there is no variable-ratio
 * reward engine anywhere in it. A slot machine and an exit cannot both be the design.
 */
@Composable
fun TranscendenceScreen(
    pillar: Pillar,
    onClose: () -> Unit
) {
    val reduced = rememberReducedMotion()
    val phase = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduced) phase.snapTo(1f)
        else phase.animateTo(1f, tween(durationMillis = 4200, easing = LinearOutSlowInEasing))
    }
    val p = phase.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, size.height * 0.42f)
            val reach = size.minDimension

            // The bubble's last act: it expands past every boundary it ever had.
            val bloom = 0.18f + p * 1.6f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to pillar.accent.copy(alpha = 0.55f * (1f - p * 0.35f)),
                    0.45f to pillar.accent.copy(alpha = 0.18f * (1f - p * 0.5f)),
                    1f to Color.Transparent,
                    center = centre,
                    radius = reach * bloom
                ),
                radius = reach * bloom,
                center = centre
            )

            // The interface itself, coming apart. Each particle is a piece of the perimeter
            // that no longer has anything to enclose.
            val count = 108
            for (i in 0 until count) {
                val a = i * (PI.toFloat() * 2f / count) + i * 0.013f
                val drift = 0.10f + (i % 7) * 0.035f
                val dist = reach * (0.16f + p * (0.55f + drift))
                val fade = (1f - p).coerceIn(0f, 1f)
                drawCircle(
                    color = pillar.accent.copy(alpha = 0.55f * fade * fade),
                    radius = (1f - p) * 3.2f + 0.7f,
                    center = Offset(
                        centre.x + cos(a) * dist,
                        centre.y + sin(a) * dist * 0.92f
                    )
                )
            }

            // A final wash that takes the whole screen.
            if (p > 0.45f) {
                val w = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
                drawRect(
                    brush = Brush.radialGradient(
                        0f to Ink.Primary.copy(alpha = 0.30f * w * (1f - w * 0.6f)),
                        1f to Color.Transparent,
                        center = centre,
                        radius = reach * (0.4f + w)
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Space.gutter)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            Column(modifier = Modifier.alpha(((p - 0.35f) / 0.4f).coerceIn(0f, 1f))) {
                Instrument("${pillar.display} · tier 100", color = pillar.accent)
                VSpace(16)
                Text(
                    text = closingLine(pillar),
                    color = Ink.Primary,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Start
                )
                VSpace(18)
                Text(
                    text = "There is no tier 101. Whatever you do on this axis now, you will be " +
                        "doing it without an app telling you to.",
                    color = Ink.Ash,
                    style = MaterialTheme.typography.bodyLarge
                )
                VSpace(28)
                PrimaryButton("Close it") { onClose() }
            }
        }
    }
}

/**
 * One line each, and each one is the promise the pillar was making from tier 1.
 * Deliberately not congratulation — nobody needs a trophy at this point.
 */
private fun closingLine(pillar: Pillar): String = when (pillar) {
    Pillar.ACCESS -> "You have no\nradius left\nto measure."
    Pillar.ACTIVITY -> "You are not\nyour routine.\nYou proved it."
    Pillar.SOCIAL -> "You said it.\nOut loud.\nTo their face."
}
