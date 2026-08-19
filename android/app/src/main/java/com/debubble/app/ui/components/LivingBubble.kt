package com.debubble.app.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Pillar
import com.debubble.app.ui.theme.accent
import kotlinx.coroutines.launch

/**
 * Everything the membrane needs to know about the person watching it.
 *
 * The bubble is not decoration with a progress bar bolted on — every field here is bound to
 * something true about the user, so the thing on screen is a readout of their actual state.
 */
data class BubbleState(
    /** Per-pillar radius, 0..1. */
    val access: Float,
    val activity: Float,
    val social: Float,
    /**
     * 0 = moving, 1 = stagnant. Thickens the fluid and damps the breath, so a neglected
     * bubble looks heavy rather than merely smaller.
     */
    val viscosity: Float,
    /** 0 = resting, 1 = just cleared something. Spikes the membrane turbulence. */
    val energy: Float,
    /**
     * 0 = tier 1, 1 = tier 100. Drives the light. At the bottom of the ladder the bubble is
     * dim and tightly bounded; near the top it blooms and bleeds past its own edges.
     */
    val luma: Float
) {
    companion object {
        fun from(tiers: Map<Pillar, Int>, daysSinceActive: Long, energy: Float): BubbleState {
            val mean = Pillar.order.map { tiers[it] ?: 1 }.average().toFloat()
            return BubbleState(
                access = Engine.radius(tiers[Pillar.ACCESS] ?: 1),
                activity = Engine.radius(tiers[Pillar.ACTIVITY] ?: 1),
                social = Engine.radius(tiers[Pillar.SOCIAL] ?: 1),
                // A week away is fully heavy. Same day is fully fluid.
                viscosity = (daysSinceActive.toFloat() / 7f).coerceIn(0f, 1f),
                energy = energy.coerceIn(0f, 1f),
                luma = ((mean - 1f) / 99f).coerceIn(0f, 1f)
            )
        }
    }
}

/**
 * The Bubble, as a living membrane.
 *
 * Rendered with an AGSL runtime shader: three metaball lobes whose boundaries are displaced by
 * fbm noise, breathing at 60 BPM, thickening when the user goes quiet and spiking when they
 * clear something. Overlaps brighten because the metaball field sums — the same additive logic
 * the flat version used, but now the membrane actually deforms.
 *
 * AGSL — not GLSL or Vulkan, neither of which Compose can drive — arrived in API 33, and this
 * app supports 26. Below 33 it falls back to [BubbleMap], which is the same composition and the
 * same colours without the fluid simulation. The fallback is not a stub: it is what most of the
 * install base will actually see for a while, so it has to look deliberate on its own.
 */
@Composable
fun LivingBubble(
    state: BubbleState,
    modifier: Modifier = Modifier,
    pulse: Pillar? = null,
    animate: Boolean = true
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ShaderBubble(state = state, modifier = modifier, animate = animate)
    } else {
        BubbleMap(
            tiers = mapOf(
                Pillar.ACCESS to radiusToTier(state.access),
                Pillar.ACTIVITY to radiusToTier(state.activity),
                Pillar.SOCIAL to radiusToTier(state.social)
            ),
            modifier = modifier,
            pulse = pulse,
            animate = animate
        )
    }
}

/** Engine.radius is the forward map; the fallback needs it inverted to reuse BubbleMap. */
private fun radiusToTier(r: Float): Int {
    val t = ((r - 0.20f) / 0.80f).coerceIn(0f, 1f)
    return (Math.pow(t.toDouble(), 1.0 / 0.62) * Engine.MAX_TIER).toInt().coerceIn(1, Engine.MAX_TIER)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderBubble(
    state: BubbleState,
    modifier: Modifier,
    animate: Boolean
) {
    val shader = remember { RuntimeShader(AGSL) }
    val brush = remember(shader) { ShaderBrush(shader) }
    val scope = rememberCoroutineScope()

    // Frame clock. produceState keeps the loop tied to this composable's lifetime, so the
    // shader stops costing frames the moment the dashboard leaves the screen.
    val time by produceState(0f, animate) {
        if (!animate) {
            value = 0f
            return@produceState
        }
        var origin = 0L
        while (true) {
            withFrameNanos { now ->
                if (origin == 0L) origin = now
                value = (now - origin) / 1_000_000_000f
            }
        }
    }

    // Touch: the membrane is pulled toward the thumb and relaxes when released.
    val touch = remember { Animatable(0f) }
    var touchPoint by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier.pointerInput(animate) {
            detectTapGestures(
                onPress = { offset ->
                    touchPoint = offset
                    scope.launch { touch.animateTo(1f, tween(180, easing = LinearEasing)) }
                    tryAwaitRelease()
                    scope.launch { touch.animateTo(0f, tween(420, easing = LinearEasing)) }
                }
            )
        }
    ) {
        val unit = minOf(size.width, size.height)
        // Touch expressed in the shader's centred, unit-normalised space.
        val tx = if (unit > 0f) (touchPoint.x - size.width / 2f) / unit else 0f
        val ty = if (unit > 0f) (touchPoint.y - size.height / 2f) / unit else 0f

        shader.setFloatUniform("uSize", size.width, size.height)
        shader.setFloatUniform("uTime", time)
        shader.setFloatUniform("uRadii", state.access, state.activity, state.social)
        shader.setFloatUniform("uViscosity", state.viscosity)
        shader.setFloatUniform("uEnergy", state.energy)
        shader.setFloatUniform("uLuma", state.luma)
        shader.setFloatUniform("uTouch", tx, ty)
        shader.setFloatUniform("uTouchAmp", touch.value)
        shader.setColourUniform("cAccess", Pillar.ACCESS.accent)
        shader.setColourUniform("cActivity", Pillar.ACTIVITY.accent)
        shader.setColourUniform("cSocial", Pillar.SOCIAL.accent)

        drawRect(brush = brush)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.setColourUniform(name: String, colour: Color) {
    setFloatUniform(name, colour.red, colour.green, colour.blue)
}

/**
 * AGSL. Kept as one string rather than assembled from fragments — a shader that is stitched
 * together at runtime cannot be read, and this one has to be readable to be tuned.
 */
private val AGSL = """
uniform float2 uSize;
uniform float  uTime;
uniform float3 uRadii;
uniform float  uViscosity;
uniform float  uEnergy;
uniform float  uLuma;
uniform float2 uTouch;
uniform float  uTouchAmp;
uniform float3 cAccess;
uniform float3 cActivity;
uniform float3 cSocial;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        v = v + amp * vnoise(p);
        p = p * 2.03;
        amp = amp * 0.5;
    }
    return v;
}

// One metaball lobe. Returns field strength: it rises toward the centre and falls off with
// distance, so summing three of them makes the overlaps genuinely brighter.
float lobe(float2 uv, float angle, float radius, float seed, float t, float visc, float energy) {
    float orbit = 0.085 + 0.012 * sin(t * 0.9 + seed);
    float sway  = 0.09 * (1.0 - 0.6 * visc) * sin(t * 0.55 + seed * 2.1);
    float2 c = float2(cos(angle + sway), sin(angle + sway)) * orbit;
    float d = length(uv - c);
    // The membrane: noise displaces the boundary. Thick fluid moves slowly; a recent
    // completion makes the surface erratic.
    float n = fbm(uv * 3.2 + float2(t * 0.10 * (1.0 - visc), seed * 7.0));
    float r = radius * (1.0 + (n - 0.5) * (0.10 + 0.30 * energy));
    return r / max(d, 0.0015);
}

half4 main(float2 fragCoord) {
    float unit = min(uSize.x, uSize.y);
    float2 uv = (fragCoord - 0.5 * uSize) / unit;

    // Pull the fluid toward the thumb.
    if (uTouchAmp > 0.001) {
        float2 tv = uv - uTouch;
        float td = length(tv);
        if (td > 0.0004) {
            uv = uv - (tv / td) * (uTouchAmp * exp(-td * 9.0) * 0.055);
        }
    }

    // 60 BPM is one cycle per second. Heavy fluid barely breathes.
    float breathe = 1.0 + (0.030 - 0.014 * uViscosity) * sin(uTime * 6.28318530718);
    float base = 0.30 * breathe;

    float f1 = lobe(uv, -1.5707963, uRadii.x * base, 1.0, uTime, uViscosity, uEnergy);
    float f2 = lobe(uv,  0.5235988, uRadii.y * base, 2.0, uTime, uViscosity, uEnergy);
    float f3 = lobe(uv,  2.6179939, uRadii.z * base, 3.0, uTime, uViscosity, uEnergy);
    float field = f1 + f2 + f3;

    float3 hue = (cAccess * f1 + cActivity * f2 + cSocial * f3) / max(field, 0.0001);

    float inside = smoothstep(0.92, 1.08, field);
    float edge   = smoothstep(0.86, 1.00, field) - smoothstep(1.00, 1.20, field);

    // Light is the progression. Tier 1 is a dim membrane in a void; tier 100 blooms.
    float glow = (0.09 + 0.30 * uLuma) * smoothstep(0.25, 1.00, field);
    float fill = inside * (0.09 + 0.26 * uLuma);
    float rim  = edge * (0.55 + 0.45 * uLuma);

    float intensity = glow * 0.8 + fill + rim * 1.6;
    float alpha = clamp(intensity, 0.0, 1.0);

    // Premultiplied.
    float3 rgb = hue * alpha;
    return half4(rgb.x, rgb.y, rgb.z, alpha);
}
""".trimIndent()
