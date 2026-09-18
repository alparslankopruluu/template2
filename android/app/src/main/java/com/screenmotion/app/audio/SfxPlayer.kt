package com.screenmotion.app.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import com.screenmotion.app.data.ConfigRepository
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight procedural SFX via ToneGenerator.
 * Fails silently if audio is unavailable. Respects mute preference.
 */
object SfxPlayer {

    private val ready = AtomicBoolean(false)
    private var tone: ToneGenerator? = null
    private var appRef: WeakReference<Context>? = null
    private val main = Handler(Looper.getMainLooper())
    private var lastPlayMs = 0L
    private var lastKind: SfxKind? = null

    fun init(context: Context) {
        appRef = WeakReference(context.applicationContext)
        if (ready.get()) return
        try {
            tone = ToneGenerator(AudioManager.STREAM_MUSIC, 55)
            ready.set(true)
        } catch (_: Exception) {
            tone = null
            ready.set(false)
        }
    }

    fun release() {
        try {
            tone?.release()
        } catch (_: Exception) {
        }
        tone = null
        ready.set(false)
    }

    fun play(kind: SfxKind) {
        val ctx = appRef?.get() ?: return
        play(ctx, kind)
    }

    fun play(context: Context, kind: SfxKind) {
        try {
            if (appRef?.get() == null) {
                appRef = WeakReference(context.applicationContext)
            }
            val repo = ConfigRepository.get(context)
            if (repo.soundMuted) return
            if (!ready.get()) init(context)
            val tg = tone ?: return

            val now = System.currentTimeMillis()
            val minGap = when (kind) {
                SfxKind.BUBBLE_POP, SfxKind.WIND_WHOOSH, SfxKind.TOUCH_SPLASH -> 180L
                SfxKind.BIRD_CHIRP -> 320L
                SfxKind.METEOR -> 220L
                else -> 80L
            }
            if (kind == lastKind && now - lastPlayMs < minGap) return
            lastPlayMs = now
            lastKind = kind

            val (toneType, durationMs) = when (kind) {
                SfxKind.THEME_SELECT -> ToneGenerator.TONE_PROP_BEEP to 90
                SfxKind.TOUCH_SPLASH -> ToneGenerator.TONE_PROP_PROMPT to 50
                SfxKind.BOOST -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 160
                SfxKind.METEOR -> ToneGenerator.TONE_CDMA_PIP to 120
                SfxKind.BUBBLE_POP -> ToneGenerator.TONE_PROP_ACK to 40
                SfxKind.BIRD_CHIRP -> ToneGenerator.TONE_DTMF_1 to 70
                SfxKind.WIND_WHOOSH -> ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE to 140
                SfxKind.APPLY_SUCCESS -> ToneGenerator.TONE_PROP_BEEP to 180
            }

            main.post {
                try {
                    tg.startTone(toneType, durationMs)
                } catch (_: Exception) {
                }
            }

            if (kind == SfxKind.BIRD_CHIRP) {
                main.postDelayed({
                    try {
                        tg.startTone(ToneGenerator.TONE_DTMF_3, 55)
                    } catch (_: Exception) {
                    }
                }, 85)
            }
        } catch (_: Exception) {
            // Fail silently
        }
    }
}
