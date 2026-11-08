package com.debubble.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.debubble.app.ui.theme.Cut
import com.debubble.app.ui.theme.fill
import com.debubble.app.ui.theme.wash
import com.debubble.app.ui.theme.ButtonText
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.sin

/** How long the user has to hold. */
const val HOLD_MILLIS = 3000

/**
 * "I did it", as a bar that fills left to right in pixel steps, for the bottom of a task
 * where it shares a row with "Remind me". The same block as every button, on a plum lip.
 *
 * Still a hold rather than a tap: three seconds is long enough to be a decision and short
 * enough not to be a chore, and a tap would make "I did it" as cheap to say as to mis-press.
 * TalkBack gets an ordinary click.
 */
@Composable
fun HoldBar(
    accent: Color,
    label: String,
    modifier: Modifier = Modifier,
    holdMillis: Int = HOLD_MILLIS,
    onCommit: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val p = progress.value
    val secondsLeft = ceil((1f - p) * holdMillis / 1000f).toInt()

    val fillColour = accent.fill
    Box(
        modifier = modifier
            .heightIn(min = 64.dp)
            .castShadow(Cut.card, Space.lift, Ink.Border)
            .clip(Cut.card)
            .background(accent.wash)
            .drawBehind {
                // Filled in whole steps, like a bar in a game, rather than a smooth wipe.
                val step = 6.dp.toPx()
                val w = (size.width * p / step).toInt() * step
                if (w > 0f) drawRect(fillColour, size = size.copy(width = w.coerceAtMost(size.width)))
            }
            .border(BorderStroke(3.dp, Ink.Border), Cut.card)
            .semantics {
                role = Role.Button
                contentDescription = "$label. Press and hold for three seconds."
                onClick(label = label) { onCommit(); true }
            }
            .pointerInput(holdMillis) {
                detectTapGestures(
                    onPress = {
                        var closed = false
                        haptics.tension(holdMillis)
                        val fill = scope.launch {
                            progress.animateTo(1f, tween(holdMillis, easing = LinearEasing))
                            closed = true
                            haptics.strike()
                            onCommit()
                        }
                        tryAwaitRelease()
                        if (closed) {
                            progress.snapTo(0f)
                        } else {
                            fill.cancel()
                            haptics.stop()
                            progress.animateTo(0f, tween(160, easing = LinearEasing))
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(Cut.small)
                    .background(if (p > 0.15f) Ink.Surface else accent.fill)
                    .border(BorderStroke(2.dp, Ink.Border), Cut.small),
                contentAlignment = Alignment.Center
            ) {
                Glyph(Glyphs.Check, colour = Ink.Primary, size = 20)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (p > 0.02f) "Keep holding… ${secondsLeft.coerceAtLeast(1)}" else label,
                    color = Ink.Primary,
                    style = ButtonText
                )
                if (p <= 0.02f) {
                    Text(
                        text = "Press and hold",
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
