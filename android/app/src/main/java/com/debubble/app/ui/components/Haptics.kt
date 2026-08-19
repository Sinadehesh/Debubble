package com.debubble.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * The app's tactile vocabulary. Four signatures, and only four.
 *
 * [tension] escalates — the phone should feel like a spring being drawn back.
 * [strike] is the release: one sharp, final hit.
 * [thud] is a hollow drop, for letting go early. Not a buzzer; a disappointment you can feel.
 * [friction] is one blunt pulse. Friction is credit in this app, so it must never feel
 * like an error tone.
 */
class Haptics(private val vibrator: Vibrator?) {

    private fun available(): Vibrator? = vibrator?.takeIf { it.hasVibrator() }

    /**
     * A rising rumble for the whole hold, composed up front as a single waveform.
     *
     * Built in one call rather than ticked from a timer: a coroutine firing a pulse every
     * frame drifts against the visual ring and ends up feeling loose. One waveform stays
     * locked to the animation because both are started from the same instant.
     */
    fun tension(durationMs: Int) {
        val v = available() ?: return
        val steps = 24
        val slot = (durationMs / steps).coerceAtLeast(8)
        val timings = LongArray(steps * 2)
        val amplitudes = IntArray(steps * 2)
        for (i in 0 until steps) {
            val k = if (steps > 1) i / (steps - 1f) else 1f
            val pulse = (6 + 12 * k).toLong()
            val gap = (slot - pulse).coerceAtLeast(3L)
            timings[i * 2] = pulse
            amplitudes[i * 2] = (38 + 200 * k * k).toInt().coerceIn(1, 255)
            timings[i * 2 + 1] = gap
            amplitudes[i * 2 + 1] = 0
        }
        if (v.hasAmplitudeControl()) {
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            v.vibrate(VibrationEffect.createWaveform(timings, -1))
        }
    }

    /** Kill the rumble the instant a hold is abandoned. */
    fun stop() {
        available()?.cancel()
    }

    /** The commit. One sharp, unambiguous hit. */
    fun strike() {
        val v = available() ?: return
        v.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            v.vibrate(VibrationEffect.createOneShot(24, 255))
        }
    }

    /** Let go too early. Hollow, low, over immediately. */
    fun thud() {
        val v = available() ?: return
        v.cancel()
        v.vibrate(
            if (v.hasAmplitudeControl()) VibrationEffect.createOneShot(52, 70)
            else VibrationEffect.createOneShot(52, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    /** Logging friction. An acknowledgement, never a reprimand. */
    fun friction() {
        val v = available() ?: return
        v.vibrate(
            if (v.hasAmplitudeControl()) VibrationEffect.createOneShot(30, 140)
            else VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context) { Haptics(context.vibrator()) }
}

private fun Context.vibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
