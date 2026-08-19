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
import androidx.compose.ui.unit.dp
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import kotlinx.coroutines.launch
import kotlin.math.sin

/** How long the user has to hold their nerve. */
const val HOLD_MILLIS = 3000

/**
 * The commit gesture, built to physically resist the user.
 *
 * A tap is too cheap to mean anything, and so is a short press. Accepting a challenge costs a
 * full three seconds of sustained pressure against escalating haptic tension — the phone winds
 * up like a spring being drawn back, the button shakes harder as the ring closes, and letting
 * go early collapses all of it with a hollow thud. Holding past the threshold releases
 * everything at once: one sharp strike and a shockwave.
 *
 * Two things keep that from being hostile rather than demanding. The shake is suppressed when
 * the system asks for reduced animation, and screen-reader users get a plain click action
 * instead — a timed gesture is a bad interaction for someone driving the phone with TalkBack.
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

    // Tension curve, not the raw fill: shake stays imperceptible for the first second and
    // becomes violent only near the threshold, so the effort reads as accelerating.
    val tension = progress.value * progress.value
    val shake = if (reduceMotion) 0f else tension * 5.5f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.gap)
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer {
                    translationX = sin(phase * 47f) * shake
                    translationY = sin(phase * 61f + 1.7f) * shake
                    val squeeze = 1f - tension * 0.06f
                    scaleX = squeeze
                    scaleY = squeeze
                }
                .clip(CircleShape)
                .semantics {
                    role = Role.Button
                    contentDescription = "Hold for three seconds to commit to this challenge"
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
                                // Collapse, do not ease: hesitation should cost the whole hold.
                                progress.animateTo(0f, tween(140, easing = LinearEasing))
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(112.dp)) {
                val stroke = 2.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)

                drawCircle(
                    color = accent.copy(alpha = 0.08f + 0.16f * tension),
                    radius = size.minDimension / 2f - stroke
                )
                drawArc(
                    color = Ink.Edge,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke + tension * 2f, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "✓",
                color = accent,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Text(
            text = if (progress.value > 0.02f) "Keep holding" else label,
            color = if (progress.value > 0.02f) accent else Ink.Ash,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
