package com.debubble.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.debubble.app.ui.theme.Ink

/**
 * The light the whole app is lit by.
 *
 * At tier 1 this is a single narrow source: most of the screen is genuinely empty, the content
 * sits in a small pool of warm light, and the effect should be closer to claustrophobia than to
 * atmosphere. That is the point — the interface is meant to feel as small as the life it is
 * describing.
 *
 * As the ladder is climbed the source diffuses and two ambient bleeds come up at the lower
 * corners, indigo against crimson, until by the top of the ladder the screen is lit corner to
 * corner and nothing is hidden. Nobody should be able to name the day it changed.
 *
 * Drawn behind content, never over it: this is illumination, not a scrim, and putting it on top
 * would wash out the very text it exists to reveal.
 */
@Composable
fun Chiaroscuro(
    modifier: Modifier = Modifier,
    openness: Float = LocalAscension.current
) {
    // Eased so that recalibration or a big jump does not snap the room's lighting.
    val open by animateFloatAsState(
        targetValue = openness.coerceIn(0f, 1f),
        animationSpec = tween(1400),
        label = "chiaroscuro"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // The key light. Narrow and high at the bottom of the ladder, broad and soft at the top.
        val reach = (w * (0.55f + open * 1.25f))
        val key = Offset(w * 0.5f, h * 0.24f)
        drawRect(
            brush = Brush.radialGradient(
                0f to CANDLE.copy(alpha = 0.085f + open * 0.10f),
                0.5f to CANDLE.copy(alpha = 0.030f + open * 0.055f),
                1f to Color.Transparent,
                center = key,
                radius = reach
            )
        )

        // Ambient bleed. Absent early, and by the end the two of them meet in the middle.
        if (open > 0.02f) {
            drawRect(
                brush = Brush.radialGradient(
                    0f to INDIGO.copy(alpha = 0.16f * open),
                    1f to Color.Transparent,
                    center = Offset(w * 0.08f, h * 0.92f),
                    radius = w * (0.55f + open * 0.5f)
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    0f to CRIMSON.copy(alpha = 0.14f * open),
                    1f to Color.Transparent,
                    center = Offset(w * 0.95f, h * 0.74f),
                    radius = w * (0.55f + open * 0.5f)
                )
            )
        }

        // Vignette. Heaviest at the start, and it lifts as the light opens — the walls of the
        // room receding rather than the lamp getting brighter.
        drawRect(
            brush = Brush.radialGradient(
                0f to Color.Transparent,
                0.55f to Color.Transparent,
                1f to Ink.Void.copy(alpha = 0.85f - open * 0.55f),
                center = key,
                radius = maxOf(w, h) * 0.92f
            )
        )
    }
}

private val CANDLE = Color(0xFFFFF1D6)
private val INDIGO = Color(0xFF603C8C)
private val CRIMSON = Color(0xFF962A3A)
