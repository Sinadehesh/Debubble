package com.debubble.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.fill

/**
 * The release, fired the instant a hold closes: one ring of pixel squares crossing the
 * screen in the task's colour. The celebration belongs to the postcard a beat later; firing
 * both at once turns a decisive moment into noise.
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
    val fillColour = accent.fill
    Canvas(modifier = modifier.fillMaxSize()) {
        val v = p.value
        if (v <= 0f || v >= 1f) return@Canvas
        val centre = Offset(size.width / 2f, size.height * origin)
        val reach = kotlin.math.sqrt(size.width * size.width + size.height * size.height)
        val radius = 40f + v * reach
        val block = (6.dp.toPx() * (1f - v)).coerceAtLeast(2.dp.toPx())
        val count = 28
        for (i in 0 until count) {
            val a = (i / count.toFloat()) * 2f * Math.PI.toFloat()
            val x = centre.x + kotlin.math.cos(a) * radius
            val y = centre.y + kotlin.math.sin(a) * radius
            drawRect(Ink.Border.copy(alpha = 1f - v), topLeft = Offset(x - block / 2f - 2f, y - block / 2f - 2f), size = Size(block + 4f, block + 4f))
            drawRect(fillColour.copy(alpha = 1f - v), topLeft = Offset(x - block / 2f, y - block / 2f), size = Size(block, block))
        }
    }
}

/** A plain rule, in the wall's shade: two pixels, like a seam between boards. */
@Composable
fun Divider(modifier: Modifier = Modifier, colour: Color = Ink.Faint) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(colour)
    )
}
