package com.debubble.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Playback for the procedural voices.
 *
 * Three rules keep this from being the thing that makes people uninstall the app:
 *
 *  1. The phone's ringer mode is absolute. Silent means silent, including for one-shots.
 *  2. The ambient bed never plays over someone's music. If audio is already active it stays
 *     out of the way entirely rather than ducking and fighting.
 *  3. The bed is opt-in. One-shot feedback on a deliberate action is expected; a continuous
 *     drone starting on its own is an intrusion, so it defaults off.
 */
class SoundEngine(private val context: Context) {

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var bed: AudioTrack? = null
    private val cache = mutableMapOf<String, ShortArray>()

    private fun audible(): Boolean =
        audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL

    private fun attributes() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun format() = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(Synth.SAMPLE_RATE)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    /** One-shots are generated once and reused; the maths is cheap but not free. */
    private fun sample(key: String, make: () -> ShortArray): ShortArray =
        cache.getOrPut(key, make)

    private fun oneShot(key: String, make: () -> ShortArray) {
        if (!audible()) return
        val pcm = sample(key, make)
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes())
            .setAudioFormat(format())
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.setNotificationMarkerPosition(pcm.size)
        track.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) { t?.release() }
                override fun onPeriodicNotification(t: AudioTrack?) = Unit
            }
        )
        track.play()
    }

    /** The commit. Fired when a hold closes. */
    fun chime() = oneShot("chime") { Synth.chime() }

    /** Friction. The audible half of the fracture. */
    fun stab() = oneShot("stab") { Synth.stab() }

    /**
     * Start or update the ambient bed. [intensity] 0 is the resting drone; 1 is the muffled,
     * heartbeat-under-it state for a challenge that frightens the user.
     */
    fun startBed(intensity: Float) {
        if (!audible()) return
        if (audioManager?.isMusicActive == true) return
        stopBed()
        val pcm = sample("bed_${(intensity * 4).toInt()}") {
            if (intensity <= 0.01f) Synth.drone() else Synth.tensionBed(intensity = intensity)
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes())
            .setAudioFormat(format())
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.setLoopPoints(0, pcm.size, -1)
        track.setVolume(0.5f)
        track.play()
        bed = track
    }

    fun stopBed() {
        bed?.runCatching {
            stop()
            release()
        }
        bed = null
    }

    fun release() {
        stopBed()
        cache.clear()
    }
}

@Composable
fun rememberSound(): SoundEngine {
    val context = LocalContext.current
    val engine = remember(context) { SoundEngine(context) }
    DisposableEffect(engine) {
        onDispose { engine.release() }
    }
    return engine
}
