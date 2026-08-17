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
 * Two haptic signatures, and only two.
 *
 * [commit] rises — the phone should feel like something opened up.
 * [friction] is one blunt pulse — an acknowledgement, never a buzzer. Friction is credit
 * in this app, so it must not feel like an error.
 */
class Haptics(private val vibrator: Vibrator?) {

    fun commit() = play(longArrayOf(0, 6, 26, 10, 40, 18), intArrayOf(0, 90, 0, 150, 0, 210))

    fun friction() = play(longArrayOf(0, 30), intArrayOf(0, 140))

    private fun play(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && v.hasAmplitudeControl()) {
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(timings, -1)
        }
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
