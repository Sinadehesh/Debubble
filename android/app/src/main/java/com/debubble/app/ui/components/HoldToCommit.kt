package com.debubble.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.debubble.app.ui.theme.Ink
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.sin

/** How long the user has to hold. */
const val HOLD_MILLIS = 3000

/**
 * The commit gesture.
 *
 * A tap is too cheap for "I am going to go and do this", so committing costs three seconds of
 * sustained pressure against rising haptic tension: the ring fills, the button shakes harder
 * as it closes, and letting go early collapses the whole thing with a hollow thud.
 *
 * Made deliberately large and loud. The previous version was a 112dp circle with a hairline
 * ring and a tick, which read as decoration — people did not know it was the button, and a
 * control you have to hold for three seconds is the last thing that should be subtle. It is
 * now 176dp, filled, labelled in words, and it counts down while you hold it.
 *
 * Two things keep it demanding rather than hostile: the shake is suppressed under
 * reduced-motion, and screen-reader users get a plain click action instead, because a timed
 * press is a bad interaction for anyone driving the phone with TalkBack.
 */
@Composable
fun HoldToCommit(
    accent: Color,
    label: String,
    modifier: Modifier = Modifier,
    holdMillis: Int = HOLD_MILLIS,
    onCommit: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val reduceMotion = rememberReducedMotion()

    var holding by remember { mutableStateOf(false) }
    var phase by remember { mutableFloatStateOf(0f) }

    // Free-running clock while the finger is down, so the shake oscillates independently of
    // the fill. Stops the moment the hold ends — no idle frame cost.
    LaunchedEffect(holding) {
        if (!holding) return@LaunchedEffect
        while (true) {
            withFrameNanos { now -> phase = (now / 1_000_000L % 100_000L) / 1000f }
        }
    }

    // Tension curve rather than the raw fill: the first second is quiet and the last is
    // violent, so the effort reads as accelerating.
    val p = progress.value
    val tension = p * p
    val shake = if (reduceMotion) 0f else tension * 6f
    val secondsLeft = ceil((1f - p) * holdMillis / 1000f).toInt()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(176.dp)
                .graphicsLayer {
                    translationX = sin(phase * 47f) * shake
                    translationY = sin(phase * 61f + 1.7f) * shake
                    val squeeze = 1f - tension * 0.05f
                    scaleX = squeeze
                    scaleY = squeeze
                }
                .clip(CircleShape)
                .semantics {
                    role = Role.Button
                    contentDescription =
                        "$label. Press and hold for three seconds to commit."
                    onClick(label = "commit") { onCommit(); true }
                }
                .pointerInput(holdMillis) {
                    detectTapGestures(
                        onPress = {
                            var closed = false
                            holding = true
                            haptics.tension(holdMillis)
                            val fill = scope.launch {
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(holdMillis, easing = LinearEasing)
                                )
                                closed = true
                                haptics.strike()
                                onCommit()
                            }
                            tryAwaitRelease()
                            holding = false
                            if (closed) {
                                progress.snapTo(0f)
                            } else {
                                fill.cancel()
                                haptics.stop()
                                haptics.thud()
                                // Collapse, do not ease: hesitation costs the whole hold.
                                progress.animateTo(0f, tween(140, easing = LinearEasing))
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(176.dp)) {
                val stroke = 10.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)

                // The body of the button, so it reads as a solid object at rest.
                drawCircle(
                    color = accent.copy(alpha = 0.14f + 0.34f * tension),
                    radius = size.minDimension / 2f - stroke
                )
                // The empty track. Visible from the start, so the shape of the task is clear
                // before the user has touched anything.
                drawArc(
                    color = Ink.Border,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                // The fill.
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * p,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (p > 0.02f) {
                    Text(
                        text = secondsLeft.coerceAtLeast(1).toString(),
                        color = accent,
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "keep holding",
                        color = Ink.Primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Glyph(Glyphs.Check, colour = accent, size = 40, weight = 2.6f)
                    Text(
                        text = "HOLD",
                        color = Ink.Primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Text(
            text = if (p > 0.02f) "Do not let go" else label,
            color = if (p > 0.02f) accent else Ink.Secondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
