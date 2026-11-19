package com.droidbubble.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.droidbubble.app.island.IslandA11y
import com.droidbubble.app.island.LiveActivityKind
import com.droidbubble.app.island.isTimerAlert
import com.droidbubble.app.model.NotificationInfo

/**
 * Accessibility: reads the island's urgent moments aloud ("Time's up", a ringing alarm, who's
 * calling). Each alert is spoken once, and nothing is said in silent or vibrate mode. The speech
 * engine is only created while the setting is on.
 *
 * C3.9: never while a screen reader is on; TalkBack announces these itself (C3.7), and two voices
 * would talk over each other. [screenReaderOn] and [speakOverride] are seams for tests.
 */
class SpokenAlerts(
    private val context: Context,
    private val screenReaderOn: () -> Boolean = { IslandA11y.screenReaderOn.value },
    @get:VisibleForTesting internal val speakOverride: ((String) -> Unit)? = null,
) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    private var lastSpokenKey: String? = null

    /** Turns speech on or off to match the setting; off releases the engine. */
    fun setEnabled(enabled: Boolean) {
        if (enabled && tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    tts?.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    pending?.let { say(it) }
                }
                pending = null
            }
        } else if (!enabled) {
            shutdown()
        }
    }

    /** Called with the island's current notification; speaks when a new alert or call appears. */
    fun onNotification(n: NotificationInfo?) {
        if (tts == null && speakOverride == null) return
        if (n == null) {
            lastSpokenKey = null
            return
        }
        val phrase = phraseFor(n) ?: return
        val key = "${n.key}|${n.kind}"
        if (key == lastSpokenKey) return
        // Marked spoken first, so an alert isn't spoken late if TalkBack turns off while it's up
        lastSpokenKey = key
        if (screenReaderOn()) return
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio != null && audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        speakOverride?.let { it(phrase); return }
        if (ready) say(phrase) else pending = phrase
    }

    private fun say(text: String) {
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "droidbubble-alert") }
            .onFailure { Log.w("SpokenAlerts", "Couldn't speak alert", it) }
    }

    fun shutdown() {
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null
        ready = false
        pending = null
        lastSpokenKey = null
    }

    companion object {
        /** What to say for [n], or null when it isn't something worth speaking. */
        fun phraseFor(n: NotificationInfo): String? = when (n.kind) {
            LiveActivityKind.CALL -> if (n.isIncomingCall) {
                n.title.takeIf { it.isNotBlank() }?.let { "Call from $it" } ?: "Incoming call"
            } else null
            LiveActivityKind.ALERT -> if (isTimerAlert(n)) {
                val label = n.title.takeIf {
                    it.isNotBlank() && !it.equals("Timer", true) && !it.contains("time's up", true) && !it.contains("times up", true)
                }
                if (label != null) "Time's up. $label" else "Time's up"
            } else {
                val name = n.title.takeIf { it.isNotBlank() && !it.equals("Alarm", true) }
                if (name != null) "Alarm. $name" else "Alarm"
            }
            else -> null
        }
    }
}
