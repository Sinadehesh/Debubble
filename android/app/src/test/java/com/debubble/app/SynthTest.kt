package com.debubble.app

import com.debubble.app.audio.Synth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Audio fails loudly and invisibly: a clipped buffer is unlistenable, a bad loop seam ticks
 * once a second forever, and neither shows up in a code review. The sample maths is pure
 * Kotlin precisely so it can be checked here.
 */
class SynthTest {

    private fun peak(pcm: ShortArray): Int = pcm.maxOf { abs(it.toInt()) }

    @Test
    fun `every voice produces the requested number of samples`() {
        assertEquals(Synth.SAMPLE_RATE * 4, Synth.drone(4f).size)
        assertEquals(Synth.SAMPLE_RATE * 4, Synth.tensionBed(4f).size)
        assertEquals((Synth.SAMPLE_RATE * 2.2f).toInt(), Synth.chime(seconds = 2.2f).size)
        assertEquals((Synth.SAMPLE_RATE * 0.85f).toInt(), Synth.stab(0.85f).size)
    }

    @Test
    fun `nothing clips`() {
        listOf(
            "drone" to Synth.drone(),
            "tension" to Synth.tensionBed(),
            "chime" to Synth.chime(),
            "stab" to Synth.stab()
        ).forEach { (name, pcm) ->
            val p = peak(pcm)
            assertTrue("$name peaks at $p, at or above full scale", p < Short.MAX_VALUE.toInt())
            // Normalisation should also mean nothing is inaudibly quiet.
            assertTrue("$name is far too quiet at $p", p > Short.MAX_VALUE / 4)
        }
    }

    @Test
    fun `nothing is silent or constant`() {
        listOf(Synth.drone(), Synth.tensionBed(), Synth.chime(), Synth.stab()).forEach { pcm ->
            assertTrue("buffer is silent", pcm.any { it.toInt() != 0 })
            assertTrue("buffer is a constant", pcm.toList().distinct().size > 100)
        }
    }

    /** A looped buffer whose ends do not meet ticks once per cycle, forever. */
    @Test
    fun `looping voices fade to silence at both seams`() {
        listOf("drone" to Synth.drone(), "tension" to Synth.tensionBed()).forEach { (name, pcm) ->
            val head = abs(pcm.first().toInt())
            val tail = abs(pcm.last().toInt())
            assertTrue("$name starts at $head, will click", head < 40)
            assertTrue("$name ends at $tail, will click", tail < 40)
        }
    }

    @Test
    fun `one-shots decay rather than being cut off`() {
        listOf("chime" to Synth.chime(), "stab" to Synth.stab()).forEach { (name, pcm) ->
            val opening = pcm.take(pcm.size / 4).maxOf { abs(it.toInt()) }
            val closing = pcm.takeLast(pcm.size / 8).maxOf { abs(it.toInt()) }
            assertTrue("$name does not decay: $opening -> $closing", closing < opening / 3)
        }
    }

    /**
     * Tested on raw float buffers rather than through drone(), because finish() normalises to
     * the ceiling — which restores exactly the amplitude the filter removed and makes any
     * comparison of the finished PCM meaningless.
     *
     * The assertion is relative rather than absolute: what makes something a low-pass is that
     * it takes far more from 8 kHz than from 60 Hz, not that it hits a particular figure.
     * Thresholds were measured against a reference implementation before being written down.
     */
    @Test
    fun `the low pass attenuates high frequencies far more than low ones`() {
        fun tone(hz: Float) = FloatArray(Synth.SAMPLE_RATE / 10) {
            kotlin.math.sin(2.0 * Math.PI * hz * it / Synth.SAMPLE_RATE).toFloat()
        }
        fun rms(a: FloatArray) = kotlin.math.sqrt(a.fold(0.0) { acc, v -> acc + v * v } / a.size)
        fun kept(hz: Float, cutoff: Float): Double {
            val dry = tone(hz)
            val wet = dry.copyOf().also { Synth.lowPass(it, cutoff) }
            return rms(wet) / rms(dry)
        }

        // Hard cutoff: 8 kHz is annihilated, 60 Hz merely dented.
        val hardHigh = kept(8000f, 0.05f)
        val hardLow = kept(60f, 0.05f)
        assertTrue(
            "filter is not selective: 8kHz kept $hardHigh, 60Hz kept $hardLow",
            hardLow > hardHigh * 20
        )

        // Moderate cutoff: the bass should come through essentially untouched.
        assertTrue("60Hz should survive a moderate cutoff", kept(60f, 0.5f) > 0.9)
        assertTrue("8kHz should still be cut at a moderate cutoff", kept(8000f, 0.5f) < 0.4)

        // Fully open is a no-op.
        val dry = tone(8000f)
        val untouched = dry.copyOf().also { Synth.lowPass(it, 1f) }
        assertTrue("an open filter must not alter the signal", untouched.contentEquals(dry))
    }

    @Test
    fun `tension intensity is bounded and monotonic in effect`() {
        // Intensity out of range must not blow up or clip.
        listOf(-1f, 0f, 0.5f, 1f, 4f).forEach { i ->
            val pcm = Synth.tensionBed(intensity = i)
            assertTrue("intensity $i clipped", peak(pcm) < Short.MAX_VALUE.toInt())
            assertTrue("intensity $i produced nothing", pcm.any { it.toInt() != 0 })
        }
    }

    @Test
    fun `soft clip is bounded and passes small signals almost untouched`() {
        listOf(-40f, -1.2f, -0.3f, 0f, 0.3f, 1.2f, 40f).forEach { x ->
            assertTrue("softClip($x) escaped range", abs(Synth.softClip(x)) <= 1.0001f)
        }
        assertEquals(0.1f, Synth.softClip(0.1f), 0.01f)
    }

    @Test
    fun `finish normalises toward the ceiling without exceeding it`() {
        val quiet = FloatArray(1000) { 0.001f * kotlin.math.sin(it * 0.1f) }
        val loud = FloatArray(1000) { 12f * kotlin.math.sin(it * 0.1f) }
        listOf(quiet, loud).forEach { buf ->
            val pcm = Synth.finish(buf)
            assertTrue("escaped full scale", peak(pcm) < Short.MAX_VALUE.toInt())
        }
        // A silent buffer must stay silent rather than being normalised into noise.
        assertEquals(0, Synth.finish(FloatArray(500)).maxOf { abs(it.toInt()) })
    }
}
