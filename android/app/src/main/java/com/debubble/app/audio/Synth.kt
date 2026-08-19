package com.debubble.app.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Every sound in DeBubble, generated as PCM at runtime.
 *
 * No audio assets ship with the app. That is not a shortcut — a procedural engine is what lets
 * the ambient bed bend continuously with the user's state instead of cross-fading between a
 * handful of recorded loops, and it keeps the APK honest about being a local-only app.
 *
 * Deliberately free of Android imports so the sample maths can be unit-tested: clipping,
 * envelope shape and loop seams are all things that sound catastrophic and are invisible in
 * code review.
 */
object Synth {

    const val SAMPLE_RATE = 44_100

    /** Headroom below full scale. Digital clipping is the one unrecoverable audio mistake. */
    private const val CEILING = 0.86f

    /**
     * The resting atmosphere: a warm, barely-there drone tuned an octave below the pillar
     * accents, breathing at the same 60 BPM as the bubble.
     *
     * [cutoff] runs 1.0 (open) to 0.0 (fully muffled). Dropping it is how the app simulates
     * blood in the ears when a high-exposure challenge is on screen.
     */
    fun drone(seconds: Float = 4f, cutoff: Float = 1f): ShortArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        // Partials chosen as multiples of 0.25 Hz so a 4-second buffer loops without a seam.
        val partials = floatArrayOf(55f, 82.5f, 110f, 137.5f)
        val gains = floatArrayOf(0.5f, 0.26f, 0.16f, 0.08f)

        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            // One breath per second, matching the membrane.
            val breath = 0.82f + 0.18f * sin(2f * PI.toFloat() * 0.25f * t)
            var s = 0f
            for (p in partials.indices) {
                s += gains[p] * sin(2f * PI.toFloat() * partials[p] * t)
            }
            out[i] = s * breath * 0.34f
        }
        lowPass(out, cutoff)
        return finish(out)
    }

    /**
     * The drone with a heartbeat under it, for the moment a frightening challenge is open.
     *
     * The goal is mild, controlled arousal — the user should feel the weight of a tier in their
     * chest before they attempt it. It is emphatically not an alarm: no rising pitch, no
     * dissonance, nothing that reads as danger rather than significance.
     */
    fun tensionBed(seconds: Float = 4f, intensity: Float = 1f): ShortArray {
        val amount = intensity.coerceIn(0f, 1f)
        val n = (seconds * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        val bed = drone(seconds, cutoff = 1f - 0.72f * amount)

        for (i in 0 until n) {
            out[i] = bed[i] / Short.MAX_VALUE.toFloat()
        }
        // One beat per second: a low thud, then a softer second beat 130ms later.
        val period = SAMPLE_RATE
        for (i in 0 until n) {
            val phase = (i % period).toFloat() / SAMPLE_RATE
            out[i] += thud(phase, 0f, 46f, 0.30f * amount)
            out[i] += thud(phase, 0.13f, 40f, 0.19f * amount)
        }
        return finish(out)
    }

    /** A single low percussive hit at [at] seconds into the cycle. */
    private fun thud(phase: Float, at: Float, hz: Float, gain: Float): Float {
        val dt = phase - at
        if (dt < 0f || dt > 0.30f) return 0f
        val env = exp(-dt * 26f)
        return sin(2f * PI.toFloat() * hz * dt) * env * gain
    }

    /**
     * The release, fired when a hold closes.
     *
     * A major triad with a long tail — the acoustic opposite of the muffled tension it
     * replaces, so committing sounds like a room opening rather than a task completing.
     */
    fun chime(root: Float = 261.63f, seconds: Float = 2.2f): ShortArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        // Root, major third, fifth, octave.
        val ratios = floatArrayOf(1f, 1.25f, 1.5f, 2f)
        val gains = floatArrayOf(0.42f, 0.30f, 0.26f, 0.16f)

        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            var s = 0f
            for (k in ratios.indices) {
                // Staggered entry: the chord blooms rather than landing as a block.
                val onset = k * 0.035f
                val dt = t - onset
                if (dt <= 0f) continue
                s += gains[k] * sin(2f * PI.toFloat() * root * ratios[k] * dt) * exp(-dt * 1.9f)
            }
            out[i] = s * 0.5f
        }
        reverb(out, delaySeconds = 0.11f, feedback = 0.42f, mix = 0.38f)
        return finish(out)
    }

    /**
     * Friction. A short, distorted stab that resolves rather than decaying into nothing —
     * the audible half of the fracture, which glorifies the hit instead of mourning it.
     */
    fun stab(seconds: Float = 0.85f): ShortArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        // Integer LCG: the float version of this overflows and degenerates into a tone.
        var seed = 0x9E3779B9.toInt()
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            // Pitch drops fast, then settles onto a held tone: shattering, then reassembling.
            val hz = 190f + 300f * exp(-t * 14f)
            val body = sin(2f * PI.toFloat() * hz * t)
            seed = seed * 1664525 + 1013904223
            val noise = ((seed ushr 8) and 0xFFFF) / 65535f
            val grit = (noise - 0.5f) * 2f * exp(-t * 30f) * 0.35f
            val env = exp(-t * 3.4f) * (1f - exp(-t * 320f))
            // Soft clip: distortion with a defined edge rather than an accidental one.
            out[i] = softClip((body + grit) * env * 1.6f) * 0.5f
        }
        reverb(out, delaySeconds = 0.07f, feedback = 0.3f, mix = 0.25f)
        return finish(out)
    }

    // ---------------------------------------------------------------- dsp

    /** One-pole low-pass. [cutoff] 1.0 passes everything, 0.0 is fully closed. */
    internal fun lowPass(buf: FloatArray, cutoff: Float) {
        val c = cutoff.coerceIn(0.02f, 1f)
        if (c >= 0.999f) return
        val a = c * c
        var y = 0f
        for (i in buf.indices) {
            y += a * (buf[i] - y)
            buf[i] = y
        }
    }

    /** Feedback delay. Cheap, and enough tail to make a chime sound like a space. */
    internal fun reverb(buf: FloatArray, delaySeconds: Float, feedback: Float, mix: Float) {
        val d = (delaySeconds * SAMPLE_RATE).toInt().coerceAtLeast(1)
        if (d >= buf.size) return
        for (i in d until buf.size) {
            buf[i] += buf[i - d] * feedback * mix
        }
    }

    internal fun softClip(x: Float): Float =
        if (x > 1f || x < -1f) (if (x > 0) 1f else -1f) else x - (x * x * x) / 3f

    /**
     * Normalise to the ceiling and convert to 16-bit.
     *
     * Normalising rather than trusting the gains is what stops a change to one partial from
     * silently clipping the whole buffer.
     */
    internal fun finish(buf: FloatArray): ShortArray {
        var peak = 0f
        for (v in buf) peak = max(peak, kotlin.math.abs(v))
        val scale = if (peak > 0.0001f) CEILING / peak else 0f
        val out = ShortArray(buf.size)
        for (i in buf.indices) {
            val v = (buf[i] * scale).coerceIn(-1f, 1f)
            out[i] = (v * Short.MAX_VALUE).toInt().toShort()
        }
        // Fade the seams so a looped buffer never ticks.
        val fade = min(256, out.size / 8)
        for (i in 0 until fade) {
            val g = i.toFloat() / fade
            out[i] = (out[i] * g).toInt().toShort()
            out[out.size - 1 - i] = (out[out.size - 1 - i] * g).toInt().toShort()
        }
        return out
    }
}
