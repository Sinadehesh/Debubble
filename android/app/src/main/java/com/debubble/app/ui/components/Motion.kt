package com.debubble.app.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user has switched animations off system-wide — Settings > Accessibility >
 * Remove animations, which zeroes the animator duration scale.
 *
 * The web prototype honours prefers-reduced-motion; this is the Android equivalent. It matters
 * more here than on most screens because the Bubble Map drifts continuously rather than
 * animating once, and continuous background motion is exactly what triggers vestibular
 * symptoms.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
