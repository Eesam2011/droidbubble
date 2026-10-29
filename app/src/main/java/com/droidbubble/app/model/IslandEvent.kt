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
    val isLiked: Boolean = false
) {
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
    val contentIntent: PendingIntent? = null,
    val actions: List<NotificationAction> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val progress: Int = -1,
    val maxProgress: Int = 0,
    val isIndeterminate: Boolean = false,
    val isOngoing: Boolean = false,
    // Wall-clock ms a live timer (voice recorder, call, stopwatch) counts from; 0 = no live timer
    val liveTimerBaseMs: Long = 0L,
    val liveTimerCountDown: Boolean = false
) {
    val hasProgress: Boolean
        get() = maxProgress > 0 || isIndeterminate || (progress >= 0 && maxProgress > 0)

    val progressFraction: Float
        get() = if (maxProgress > 0 && progress >= 0) (progress.toFloat() / maxProgress).coerceIn(0f, 1f) else 0f
}

private val TIMER_TEXT_REGEX = Regex("""^\s*(?:(\d{1,2}):)?(\d{1,2}):(\d{2})\s*$""")

/** Parses timer-looking text such as "00:42" or "1:02:03" into millis, or null if it isn't one. */
fun parseTimerTextMillis(text: String?): Long? {
    val match = TIMER_TEXT_REGEX.matchEntire(text ?: return null) ?: return null
    val (h, m, s) = match.destructured
    val hours = h.toLongOrNull() ?: 0L
    return ((hours * 3600) + (m.toLong() * 60) + s.toLong()) * 1000L
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

