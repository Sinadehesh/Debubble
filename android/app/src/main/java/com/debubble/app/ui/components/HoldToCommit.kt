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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import kotlinx.coroutines.launch

/**
 * Commit gesture: a 900ms long press with a ring that fills clockwise and snaps back if
 * released early.
 *
 * A tap is too cheap to mean anything — marking a challenge done has to cost a deliberate
 * second, or the log becomes noise. The reward fires the moment the ring closes rather than
 * when the finger lifts, and the rising haptic goes first so the phone confirms before the
 * eye does.
 *
 * TalkBack users get a plain click action instead of the hold, since a timed gesture is a
 * poor fit for a screen reader.
 */
@Composable
fun HoldToCommit(
    accent: Color,
    label: String,
    modifier: Modifier = Modifier,
    holdMillis: Int = 900,
    onCommit: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.gap)
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .semantics {
                    contentDescription = "Hold to complete this challenge"
                    onClick(label = "complete") { onCommit(); true }
                }
                .pointerInput(holdMillis) {
                    detectTapGestures(
                        onPress = {
                            var closed = false
                            val fill = scope.launch {
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(holdMillis, easing = LinearEasing)
                                )
                                // Reached full without being cancelled: that is the commit.
                                closed = true
                                haptics.commit()
                                onCommit()
                            }
                            tryAwaitRelease()
                            if (closed) {
                                progress.snapTo(0f)
                            } else {
                                fill.cancel()
                                progress.animateTo(0f, tween(180, easing = LinearEasing))
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(104.dp)) {
                val stroke = 2.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)

                drawCircle(
                    color = accent.copy(alpha = 0.10f),
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
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "✓",
                color = accent,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Text(
            text = label,
            color = Ink.Ash,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
