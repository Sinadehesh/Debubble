package com.debubble.app.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.debubble.app.ui.theme.Ink
import kotlin.math.absoluteValue
import kotlin.math.sin

/**
 * The fracture: what the interface does when the user logs friction.
 *
 * The brief called for corrupted-data glitch art, and that is very nearly right — but
 * "corruption" is the one reading this app cannot afford. Ember never marks fault anywhere in
 * DeBubble; friction is credit, and a visual language of damage would quietly turn the
 * anti-score back into an error state.
 *
 * So the channels split and tear exactly as violently, and then snap back into a *larger*
 * number. It is a discharge, not a break — the surface cannot hold what just happened, and
 * what is left standing afterwards is bigger than what was there before.
 */

/**
 * RGB channel separation and horizontal tearing, applied to real content via a RenderEffect.
 * Only available from API 33, which is where RuntimeShader landed.
 */
@Composable
fun Modifier.fractureEffect(amount: Float): Modifier {
    if (amount <= 0.001f || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    return this.applyFracture(amount)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.applyFracture(amount: Float): Modifier {
    val shader = remember { RuntimeShader(FRACTURE_AGSL) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    return this
        .onSizeChanged { size = it }
        .graphicsLayer {
            if (size.width == 0 || size.height == 0) return@graphicsLayer
            shader.setFloatUniform("uSize", size.width.toFloat(), size.height.toFloat())
            shader.setFloatUniform("uAmount", amount)
            // Seeded from the amount rather than a clock: the tear pattern should evolve
            // through the animation without needing its own frame loop.
            shader.setFloatUniform("uSeed", amount * 37f)
            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "uContent")
                .asComposeRenderEffect()
            clip = true
        }
}

/**
 * Ember tear bands drawn over the surface.
 *
 * On API 33+ this rides on top of the channel split. Below it, this is the whole effect —
 * and it has to stand on its own, because it is what a large share of devices will see. Bands
 * of light thrown off the interface read as energy leaving, which is the point either way.
 */
@Composable
fun FractureOverlay(
    amount: Float,
    modifier: Modifier = Modifier,
    accent: Color = Ink.Ember
) {
    if (amount <= 0.001f) return
    Canvas(modifier = modifier) {
        val bands = 9
        val fade = 1f - amount
        for (i in 0 until bands) {
            val seed = sin(i * 91.7f + amount * 11f)
            if (seed.absoluteValue < 0.35f) continue
            val y = size.height * ((i + 0.5f) / bands + seed * 0.03f)
            val h = (2f + seed.absoluteValue * 9f) * (0.4f + fade)
            val shift = seed * size.width * 0.16f * amount
            drawRect(
                color = accent.copy(alpha = (0.30f * fade).coerceIn(0f, 1f)),
                topLeft = Offset(shift, y),
                size = Size(size.width, h),
                blendMode = BlendMode.Plus
            )
        }
        // A single bright seam where the discharge is widest.
        drawRect(
            color = accent.copy(alpha = (0.16f * fade).coerceIn(0f, 1f)),
            topLeft = Offset(0f, size.height * 0.5f - 1f),
            size = Size(size.width, 2f + 6f * amount),
            blendMode = BlendMode.Plus
        )
    }
}

private val FRACTURE_AGSL = """
uniform shader uContent;
uniform float2 uSize;
uniform float  uAmount;
uniform float  uSeed;

float hash11(float n) {
    return fract(sin(n * 91.7 + uSeed) * 43758.5453);
}

half4 main(float2 coord) {
    float a = uAmount;

    // Horizontal tearing: whole bands of the surface slip sideways.
    float bandH = max(6.0, uSize.y * 0.022);
    float band = floor(coord.y / bandH);
    float n = hash11(band);
    float tear = (n - 0.5) * 34.0 * a * step(0.58, n);

    float2 p = float2(coord.x + tear, coord.y);

    // Channel separation, widening with the discharge.
    float split = 11.0 * a;
    half4 r = uContent.eval(float2(p.x + split, p.y));
    half4 g = uContent.eval(p);
    half4 b = uContent.eval(float2(p.x - split, p.y));

    return half4(r.r, g.g, b.b, g.a);
}
""".trimIndent()
