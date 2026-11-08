package com.debubble.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Stage
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.RoomShell
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.castShadow
import com.debubble.app.ui.components.rememberReducedMotion
import com.debubble.app.ui.theme.Cut
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import com.debubble.app.ui.theme.fill
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Step 100. The end of a road, and the end of the app's usefulness on that road.
 *
 * Everything else in DeBubble is built to keep someone coming back. This screen is built to
 * stop. The room, which has been opening a window, a door, a street at a time, is shown with
 * its last wall gone, and the bubble around it comes apart into pixels that fly off and do
 * not come back. What is left is one sentence and a way out.
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
    val bits = pillar.accent.fill

    Box(modifier = Modifier.fillMaxSize()) {
        // The bubble, coming apart: a ring of pixel squares in the road's colour, each with
        // the plum edge everything in the room has, flying outwards and fading. Nothing glows.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, size.height * 0.27f)
            val reach = size.minDimension
            val px = floor(2.dp.toPx()).coerceAtLeast(1f)
            val fade = (1f - p).coerceIn(0f, 1f)
            if (fade > 0f) {
                val count = 72
                for (i in 0 until count) {
                    val a = i * (PI.toFloat() * 2f / count) + (i % 5) * 0.05f
                    val drift = 0.30f + (i % 9) * 0.07f
                    val dist = reach * (0.34f + p * drift)
                    val side = px * (2 + i % 3)
                    val x = floor(centre.x + cos(a) * dist - side / 2f)
                    val y = floor(centre.y + sin(a) * dist * 0.9f - side / 2f)
                    drawRect(Ink.Border.copy(alpha = fade), topLeft = Offset(x - px, y - px), size = Size(side + px * 2, side + px * 2))
                    drawRect(bits.copy(alpha = fade), topLeft = Offset(x, y), size = Size(side, side))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(top = 56.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Your room, with nothing left around it.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .castShadow(Cut.large, 4.dp, Ink.Border)
                        .clip(Cut.large)
                        .border(BorderStroke(3.dp, Ink.Border), Cut.large)
                ) {
                    RoomShell(Stage.all.last(), width = 232.dp)
                }
            }
            VSpace(36)
            Column(
                modifier = Modifier.alpha(((p - 0.35f) / 0.4f).coerceIn(0f, 1f)),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Pill("${pillar.display} · step 100", pillar.accent, filled = true)
                VSpace(16)
                Text(
                    text = closingLine(pillar),
                    modifier = Modifier.semantics { heading() },
                    color = Ink.Primary,
                    style = MaterialTheme.typography.displayMedium
                )
                VSpace(16)
                Text(
                    text = "There is no step 101. Whatever you do out here now, you do without " +
                        "an app telling you to.",
                    color = Ink.Secondary,
                    style = MaterialTheme.typography.bodyLarge
                )
                VSpace(28)
                PrimaryButton("Close it") { onClose() }
            }
        }
    }
}

/**
 * One line each, and each one is the promise the road was making from step 1.
 * Deliberately not congratulation: nobody needs a trophy at this point.
 */
private fun closingLine(pillar: Pillar): String = when (pillar) {
    Pillar.ACCESS -> "You have no\nradius left\nto measure."
    Pillar.ACTIVITY -> "You are not\nyour routine.\nYou proved it."
    Pillar.SOCIAL -> "You said it.\nOut loud.\nTo their face."
}
