package com.droidbubble.app.model

import android.app.PendingIntent
import android.graphics.Bitmap

data class MediaInfo(
    val packageName: String,
    val title: String,
    val artist: String,
    val albumArt: Bitmap? = null,
    val appIcon: Bitmap? = null,
    val sessionActivity: PendingIntent? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val lastPositionUpdateTimeMs: Long = 0L,
    val canLike: Boolean = false,
    val isLiked: Boolean = false,
    val isShuffleOn: Boolean = false,
    val repeatMode: Int = REPEAT_OFF
) {
    companion object {
        const val REPEAT_OFF = 0
        const val REPEAT_ALL = 1
        const val REPEAT_ONE = 2
    }

    fun computeCurrentPosition(nowMs: Long = runCatching { android.os.SystemClock.elapsedRealtime() }.getOrDefault(0L)): Long {
        var pos = positionMs
        if (isPlaying && lastPositionUpdateTimeMs > 0L && nowMs > 0L) {
            val delta = (nowMs - lastPositionUpdateTimeMs).coerceAtLeast(0L)
            pos += (delta * playbackSpeed).toLong()
        }
        return if (durationMs > 0L) pos.coerceIn(0L, durationMs) else pos.coerceAtLeast(0L)
    }
}

data class NotificationAction(
    val title: String,
    val actionIntent: PendingIntent?,
    val remoteInputKey: String? = null,
    val remoteInputLabel: String? = null,
    val remoteInputChoices: List<String> = emptyList()
) {
    val isReply: Boolean get() = remoteInputKey != null
}

data class NotificationInfo(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val iconBitmap: Bitmap? = null,
    // Sender / conversation picture (WhatsApp contact, SMS sender...); null for plain app alerts
    val avatarBitmap: Bitmap? = null,
    val contentIntent: PendingIntent? = null,
    val actions: List<NotificationAction> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val progress: Int = -1,
    val maxProgress: Int = 0,
    val isIndeterminate: Boolean = false,
    val isOngoing: Boolean = false,
    // Wall-clock ms a live timer (voice recorder, call, stopwatch) counts from; 0 = no live timer
    val liveTimerBaseMs: Long = 0L,
    val liveTimerCountDown: Boolean = false,
    // The app asked for the notification to disappear once opened (FLAG_AUTO_CANCEL)
    val isAutoCancel: Boolean = false,
    // Phone / VoIP calls (CATEGORY_CALL or CallStyle)
    val isCall: Boolean = false,
    val isIncomingCall: Boolean = false,
    val callerNumber: String? = null,
    val answerIntent: PendingIntent? = null,
    val declineIntent: PendingIntent? = null,
    // Notification.category (alarm, transport, navigation...)
    val category: String? = null,
    // A ringing alarm or an expired timer ("Time's up"): opens the full island right away
    val isAlert: Boolean = false,
    // Android 16 promoted ongoing notification ("Live Update")
    val isPromoted: Boolean = false,
    // Android 16 short status chip text ("5 min", "Arriving") for live updates
    val shortCriticalText: String? = null
) {
    val hasProgress: Boolean
        get() = maxProgress > 0 || isIndeterminate || (progress >= 0 && maxProgress > 0)

    val progressFraction: Float
        get() = if (maxProgress > 0 && progress >= 0) (progress.toFloat() / maxProgress).coerceIn(0f, 1f) else 0f

    /** Calls, running timers/recorders and other ongoing activities (vs one-off alerts). */
    val isLiveActivity: Boolean
        get() = isCall || liveTimerBaseMs > 0L || isOngoing || isAlert || isPromoted
}

private val TIMER_TEXT_REGEX = Regex("""^\s*(?:(\d{1,2}):)?(\d{1,2}):(\d{2})\s*$""")

/** Parses timer-looking text such as "00:42" or "1:02:03" into millis, or null if it isn't one. */
fun parseTimerTextMillis(text: String?): Long? {
    val match = TIMER_TEXT_REGEX.matchEntire(text ?: return null) ?: return null
    val (h, m, s) = match.destructured
    val hours = h.toLongOrNull() ?: 0L
    return ((hours * 3600) + (m.toLong() * 60) + s.toLong()) * 1000L
}

/**
 * Keeps one start time per running timer across notification updates.
 *
 * Recorders that post a plain "00:05" title get a start time worked out as `postTime - 5s`. The
 * title only has whole seconds and each post lands a little late, so that guess wanders by up to
 * a second from update to update, and the island clock jumped back a second (rolling the same
 * digit twice) or stood still for two. Every guess is at or after the real start, so the earliest
 * one is the best: keep it while new guesses agree with it. Chronometer bases (`when`) are exact,
 * so for them only a few ms of clock-read noise is absorbed. A real change (resume, +1:00, reset)
 * moves the base by more than the tolerance and is taken as is.
 */
class LiveTimerBaseStabilizer(private val maxTimers: Int = 32) {
    private val bases = object : LinkedHashMap<String, Long>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?) = size > maxTimers
    }

    @Synchronized
    fun stabilize(key: String, rawBaseMs: Long, fromText: Boolean): Long {
        if (rawBaseMs <= 0L) {
            bases.remove(key)
            return rawBaseMs
        }
        val previous = bases[key]
        val tolerance = if (fromText) TEXT_TIMER_TOLERANCE_MS else CHRONOMETER_TOLERANCE_MS
        val stable = when {
            previous == null || kotlin.math.abs(rawBaseMs - previous) > tolerance -> rawBaseMs
            fromText -> minOf(previous, rawBaseMs)
            else -> previous
        }
        bases[key] = stable
        return stable
    }

    private companion object {
        const val TEXT_TIMER_TOLERANCE_MS = 1_600L
        const val CHRONOMETER_TOLERANCE_MS = 40L
    }
}

data class ScreenRecordingState(
    val isRecording: Boolean = false,
    val durationSeconds: Long = 0L,
    val isPaused: Boolean = false
)

enum class IslandPriority {
    SCREEN_RECORDING,
    PROGRESS,
    POP_NOTIFICATION,
    MEDIA,
    FLASHLIGHT,
    FPS,
    WEATHER,
    NETWORK_SPEED,
    IDLE
}

