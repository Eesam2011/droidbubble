package com.droidbubble.app.island

import android.content.res.Resources
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.icu.util.ULocale
import com.droidbubble.app.R
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.parseTimerTextMillis
import java.text.NumberFormat
import java.util.Locale

/**
 * C3.3: what the island says to TalkBack, in one tested place (phase 8, "Semantics spec for each
 * island element"). The wiring tasks (C3.4–C3.7) only read from here.
 *
 * A [SpeechSubject] is what a node shows. The headline (`contentDescription`) changes only on real
 * events; the value (`stateDescription`) may tick. Strings live in resources so they can be
 * translated; durations go through ICU [MeasureFormat] and percentages through [NumberFormat].
 *
 * When A5 lands, subjects are built from the redacted model, never the raw notification text.
 */
class IslandSpeech(private val res: Resources, private val locale: Locale = Locale.getDefault()) {

    // --- Headline and value -----------------------------------------------------------------

    fun headline(s: SpeechSubject): String = when (s) {
        is SpeechSubject.Media -> if (s.artist.isNotBlank()) res.getString(R.string.speech_media_headline, s.title, s.artist)
            else res.getString(R.string.speech_media_headline_no_artist, s.title)
        is SpeechSubject.Message -> res.getString(R.string.speech_message_headline, s.appName, s.title)
        is SpeechSubject.IncomingCall -> res.getString(R.string.speech_incoming_call_headline, s.name)
        is SpeechSubject.OngoingCall -> res.getString(R.string.speech_call_headline, s.name)
        is SpeechSubject.TimesUp -> s.label?.let { res.getString(R.string.speech_times_up_headline, it) }
            ?: res.getString(R.string.speech_times_up)
        is SpeechSubject.Alarm -> s.label?.let { res.getString(R.string.speech_alarm_headline, it) }
            ?: res.getString(R.string.speech_alarm)
        is SpeechSubject.Timer -> s.label?.let { res.getString(R.string.speech_timer_headline, it) }
            ?: res.getString(R.string.speech_timer)
        is SpeechSubject.Stopwatch -> res.getString(R.string.speech_stopwatch)
        is SpeechSubject.Recording -> res.getString(R.string.speech_recording)
        is SpeechSubject.ScreenRecording -> res.getString(R.string.speech_screen_recording)
        is SpeechSubject.Tracking -> s.title
        is SpeechSubject.Score -> s.title
        is SpeechSubject.Download -> s.title
        is SpeechSubject.Flashlight -> res.getString(R.string.speech_flashlight_on)
        is SpeechSubject.NetworkSpeed -> res.getString(R.string.speech_network_speed)
        is SpeechSubject.Charging -> res.getString(R.string.speech_charging)
        SpeechSubject.Unlocked -> res.getString(R.string.speech_unlocked)
        is SpeechSubject.Connected -> res.getString(R.string.speech_connected, s.device)
        is SpeechSubject.Ringer -> ringerLabel(s.mode)
        is SpeechSubject.Minimised -> res.getString(R.string.speech_minimised)
    }

    /** The ticking part, or null when there is none. */
    fun value(s: SpeechSubject): String? = when (s) {
        is SpeechSubject.Media -> res.getString(if (s.isPlaying) R.string.speech_playing else R.string.speech_paused)
        is SpeechSubject.Message -> s.text.take(MESSAGE_VALUE_CHARS).takeIf { it.isNotBlank() }
        is SpeechSubject.IncomingCall -> null
        is SpeechSubject.OngoingCall -> s.elapsedMs?.let { spokenDuration(it) }
        is SpeechSubject.TimesUp -> null
        is SpeechSubject.Alarm -> s.timeText
        is SpeechSubject.Timer -> if (s.paused) res.getString(R.string.speech_timer_paused_left, spokenDuration(s.remainingMs))
            else res.getString(R.string.speech_timer_left, spokenDuration(s.remainingMs))
        is SpeechSubject.Stopwatch -> elapsedOrPaused(s.elapsedMs, s.paused)
        is SpeechSubject.Recording -> elapsedOrPaused(s.elapsedMs, s.paused)
        is SpeechSubject.ScreenRecording -> elapsedOrPaused(s.elapsedMs, s.paused)
        is SpeechSubject.Tracking -> listOfNotNull(s.shortText?.takeIf { it.isNotBlank() }, s.percent?.let { spokenPercent(it) })
            .joinToString(", ").takeIf { it.isNotEmpty() }
        is SpeechSubject.Score -> s.text.takeIf { it.isNotBlank() }
        is SpeechSubject.Download -> s.percent?.let { spokenPercent(it) }
        is SpeechSubject.Flashlight -> res.getString(R.string.speech_brightness, spokenPercent(s.brightnessPercent))
        is SpeechSubject.NetworkSpeed -> res.getString(R.string.speech_network_value, s.down, s.up)
        is SpeechSubject.Charging -> s.percent?.let { spokenPercent(it) }
        SpeechSubject.Unlocked -> null
        is SpeechSubject.Connected -> null
        is SpeechSubject.Ringer -> null
        is SpeechSubject.Minimised -> headline(s.primary)
    }

    private fun elapsedOrPaused(elapsedMs: Long?, paused: Boolean): String? =
        if (paused) res.getString(R.string.speech_paused) else elapsedMs?.let { spokenDuration(it) }

    // --- Actions ----------------------------------------------------------------------------

    /** "Double-tap to <label>". Null for glances, which can't be tapped. */
    fun clickLabel(s: SpeechSubject): String? = when {
        s is SpeechSubject.Minimised -> res.getString(R.string.speech_action_restore)
        s.isGlance -> null
        else -> res.getString(R.string.speech_action_expand)
    }

    /** "Double-tap and hold to <label>": open the app, or (B6.3) the conversation itself. */
    fun longClickLabel(appName: String, target: OpenTarget): String = when (target) {
        OpenTarget.CONVERSATION -> res.getString(R.string.speech_open_conversation)
        OpenTarget.APP -> res.getString(R.string.speech_open_in, appName)
    }

    /** The actions TalkBack's Actions menu offers on [node] (C3.5), in menu order. */
    fun availableActions(node: SpeechNode, s: SpeechSubject, pauseAvailable: Boolean = false): List<IslandA11yAction> {
        if (s.isGlance) return emptyList()
        val primary = (s as? SpeechSubject.Minimised)?.primary ?: s
        val dismiss = primary.canDismiss
        return when (node) {
            SpeechNode.PILL -> if (s is SpeechSubject.Minimised) listOf(IslandA11yAction.HIDE) else buildList {
                add(IslandA11yAction.MINIMISE)
                add(IslandA11yAction.HIDE)
                if (dismiss) add(IslandA11yAction.DISMISS)
                if (primary is SpeechSubject.Flashlight) add(IslandA11yAction.TURN_OFF_FLASHLIGHT)
                if (pauseAvailable) add(IslandA11yAction.PAUSE_ISLAND)
            }
            SpeechNode.BUBBLE -> if (dismiss) listOf(IslandA11yAction.DISMISS) else emptyList()
            SpeechNode.CARD -> buildList {
                add(IslandA11yAction.COLLAPSE)
                if (dismiss) add(IslandA11yAction.DISMISS)
            }
        }
    }

    fun actionLabel(action: IslandA11yAction): String = res.getString(
        when (action) {
            IslandA11yAction.MINIMISE -> R.string.speech_action_minimise
            IslandA11yAction.HIDE -> R.string.speech_action_hide
            IslandA11yAction.DISMISS -> R.string.speech_action_dismiss
            IslandA11yAction.TURN_OFF_FLASHLIGHT -> R.string.speech_action_flashlight_off
            IslandA11yAction.PAUSE_ISLAND -> R.string.speech_action_pause_island
            IslandA11yAction.COLLAPSE -> R.string.speech_action_collapse
        }
    )

    // --- Card, backdrop, announcements ------------------------------------------------------

    /** The expanded card's pane title, announced when it appears: "Island: Timer". */
    fun paneTitle(s: SpeechSubject): String = res.getString(R.string.speech_pane_title, kindName(s))

    fun closeIsland(): String = res.getString(R.string.speech_close_island)
    fun closeLabel(): String = res.getString(R.string.speech_action_close)

    /**
     * How an event is announced (C3.7). Incoming calls, alarms and Time's up are assertive and
     * carried by the card's pane title (they auto-expand); ticking values, media, unlock and the
     * flashlight are never announced.
     */
    fun announcement(s: SpeechSubject): Announcement? {
        val politeness = when (s) {
            is SpeechSubject.IncomingCall, is SpeechSubject.Alarm, is SpeechSubject.TimesUp -> Politeness.ASSERTIVE
            is SpeechSubject.Message, is SpeechSubject.Timer, is SpeechSubject.Stopwatch, is SpeechSubject.Recording,
            is SpeechSubject.ScreenRecording, is SpeechSubject.Tracking, is SpeechSubject.Download,
            is SpeechSubject.OngoingCall, is SpeechSubject.Score,
            is SpeechSubject.Connected, is SpeechSubject.Ringer, is SpeechSubject.Charging -> Politeness.POLITE
            is SpeechSubject.Media, SpeechSubject.Unlocked, is SpeechSubject.Flashlight,
            is SpeechSubject.NetworkSpeed, is SpeechSubject.Minimised -> return null
        }
        return Announcement(politeness, headline(s))
    }

    // --- Formatting -------------------------------------------------------------------------

    /** "4 minutes, 59 seconds", localised (ICU, wide). Whole seconds; zero parts are left out. */
    fun spokenDuration(ms: Long): String {
        val total = (ms.coerceAtLeast(0L) + 500L) / 1000L
        val h = total / 3600
        val m = (total % 3600) / 60
        val sec = total % 60
        val parts = buildList {
            if (h > 0) add(Measure(h, MeasureUnit.HOUR))
            if (m > 0) add(Measure(m, MeasureUnit.MINUTE))
            if (sec > 0 || isEmpty()) add(Measure(sec, MeasureUnit.SECOND))
        }
        return MeasureFormat.getInstance(ULocale.forLocale(locale), MeasureFormat.FormatWidth.WIDE)
            .formatMeasures(*parts.toTypedArray())
    }

    /** "54%" in the locale's own form; TalkBack reads it as "54 percent". */
    fun spokenPercent(n: Int): String = NumberFormat.getPercentInstance(locale).format(n / 100.0)

    private fun ringerLabel(mode: IslandStateManager.RingerMode): String = res.getString(
        when (mode) {
            IslandStateManager.RingerMode.SILENT -> R.string.speech_ringer_silent
            IslandStateManager.RingerMode.VIBRATE -> R.string.speech_ringer_vibrate
            IslandStateManager.RingerMode.RING -> R.string.speech_ringer_ring
        }
    )

    private fun kindName(s: SpeechSubject): String = when (s) {
        is SpeechSubject.Media -> res.getString(R.string.speech_kind_media)
        is SpeechSubject.Message -> s.appName
        is SpeechSubject.IncomingCall -> res.getString(R.string.speech_kind_incoming_call)
        is SpeechSubject.OngoingCall -> res.getString(R.string.speech_kind_call)
        is SpeechSubject.TimesUp -> res.getString(R.string.speech_times_up)
        is SpeechSubject.Alarm -> res.getString(R.string.speech_alarm)
        is SpeechSubject.Timer -> res.getString(R.string.speech_timer)
        is SpeechSubject.Minimised -> kindName(s.primary)
        else -> headline(s)
    }

    companion object {
        const val MESSAGE_VALUE_CHARS = 80

        /**
         * What a notification shows, as a [SpeechSubject]. [nowMs] reads the live timers.
         * [paused] is the notification's own pause state (`liveActivityControls(n).isPaused`).
         */
        fun subjectFor(n: NotificationInfo, nowMs: Long, paused: Boolean = false): SpeechSubject {
            val label = n.title.takeIf { it.isNotBlank() }
            val elapsed = n.liveTimerBaseMs.takeIf { it > 0L && !n.liveTimerCountDown }?.let { (nowMs - it).coerceAtLeast(0L) }
            return when (n.kind) {
                LiveActivityKind.CALL -> if (n.isIncomingCall) SpeechSubject.IncomingCall(n.title.ifBlank { n.callerNumber.orEmpty() })
                    else SpeechSubject.OngoingCall(n.title.ifBlank { n.callerNumber.orEmpty() }, elapsed)
                LiveActivityKind.ALERT -> if (isTimerAlert(n)) {
                    // The timer's own label ("Tea") may be the title or the text
                    SpeechSubject.TimesUp(listOf(n.title, n.text).firstOrNull { it.isNotBlank() && !isGenericTimerText(it) })
                } else SpeechSubject.Alarm(label?.takeUnless { it.equals("Alarm", true) }, alarmTimeText(n))
                LiveActivityKind.ALARM -> SpeechSubject.Alarm(label?.takeUnless { it.equals("Alarm", true) }, alarmTimeText(n))
                LiveActivityKind.TIMER -> SpeechSubject.Timer(
                    label?.takeUnless { it.equals("Timer", true) || parseTimerTextMillis(it) != null },
                    remainingMs = if (n.liveTimerBaseMs > 0L && n.liveTimerCountDown) (n.liveTimerBaseMs - nowMs).coerceAtLeast(0L) else 0L,
                    paused = paused,
                )
                LiveActivityKind.STOPWATCH -> SpeechSubject.Stopwatch(elapsed, paused)
                LiveActivityKind.RECORDING -> SpeechSubject.Recording(elapsed, paused)
                LiveActivityKind.TRACKING -> SpeechSubject.Tracking(
                    n.title, trackingChipText(n)?.takeUnless { it.endsWith("%") }, n.percentOrNull()
                )
                LiveActivityKind.SPORTS -> SpeechSubject.Score(n.title, n.text)
                LiveActivityKind.NONE -> if (n.hasProgress && n.isOngoing) SpeechSubject.Download(n.title, n.percentOrNull())
                    else SpeechSubject.Message(n.appName, n.title, n.text)
            }
        }

        private fun isGenericTimerText(t: String): Boolean {
            val w = t.trim().lowercase()
            return w == "timer" || "time's up" in w || "time’s up" in w || "time is up" in w || parseTimerTextMillis(t) != null
        }

        private fun NotificationInfo.percentOrNull(): Int? =
            if (maxProgress > 0 && progress >= 0 && !isIndeterminate) (progressFraction * 100).toInt() else null
    }
}

/** What an island node shows, for [IslandSpeech]. */
sealed interface SpeechSubject {
    data class Media(val title: String, val artist: String, val isPlaying: Boolean) : SpeechSubject
    data class Message(val appName: String, val title: String, val text: String) : SpeechSubject
    data class IncomingCall(val name: String) : SpeechSubject
    data class OngoingCall(val name: String, val elapsedMs: Long?) : SpeechSubject
    data class TimesUp(val label: String?) : SpeechSubject
    data class Alarm(val label: String?, val timeText: String?) : SpeechSubject
    data class Timer(val label: String?, val remainingMs: Long, val paused: Boolean) : SpeechSubject
    data class Stopwatch(val elapsedMs: Long?, val paused: Boolean) : SpeechSubject
    data class Recording(val elapsedMs: Long?, val paused: Boolean) : SpeechSubject
    data class ScreenRecording(val elapsedMs: Long?, val paused: Boolean) : SpeechSubject
    data class Tracking(val title: String, val shortText: String?, val percent: Int?) : SpeechSubject
    data class Score(val title: String, val text: String) : SpeechSubject
    data class Download(val title: String, val percent: Int?) : SpeechSubject
    data class Flashlight(val brightnessPercent: Int) : SpeechSubject
    data class NetworkSpeed(val down: String, val up: String) : SpeechSubject

    // System glances: spoken, never tappable
    data class Charging(val percent: Int?) : SpeechSubject
    data object Unlocked : SpeechSubject
    data class Connected(val device: String) : SpeechSubject
    data class Ringer(val mode: IslandStateManager.RingerMode) : SpeechSubject

    /** The minimised ring; [primary] is what it stands for. */
    data class Minimised(val primary: SpeechSubject) : SpeechSubject

    val isGlance: Boolean get() = this is Charging || this is Unlocked || this is Connected || this is Ringer

    /**
     * Whether "Dismiss" is offered. Not for a ringing call, alarm or Time's up (they tuck, B6.2),
     * screen recording (it has its own Stop), or the flashlight ("Turn off flashlight" instead).
     */
    val canDismiss: Boolean get() = when (this) {
        is IncomingCall, is Alarm, is TimesUp, is ScreenRecording, is Flashlight -> false
        is Minimised -> primary.canDismiss
        else -> !isGlance
    }
}

enum class SpeechNode { PILL, BUBBLE, CARD }

enum class OpenTarget { APP, CONVERSATION }

enum class IslandA11yAction { MINIMISE, HIDE, DISMISS, TURN_OFF_FLASHLIGHT, PAUSE_ISLAND, COLLAPSE }

enum class Politeness { POLITE, ASSERTIVE }

data class Announcement(val politeness: Politeness, val text: String)
