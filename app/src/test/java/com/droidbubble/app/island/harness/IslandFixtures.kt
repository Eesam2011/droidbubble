package com.droidbubble.app.island.harness

import android.graphics.Bitmap
import com.droidbubble.app.island.IslandClock
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo

/**
 * Island test data, shared by every island test. Names and texts are neutral (no real people).
 * Values that depend on the wall clock are getters, relative to [IslandClock] (frozen in screenshot tests).
 */
object IslandFixtures {

    val art: Bitmap by lazy {
        Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF884422.toInt()) }
    }

    val song: MediaInfo by lazy {
        MediaInfo(
            packageName = "com.spotify.music",
            title = "Song",
            artist = "Artist",
            albumArt = art,
            isPlaying = true,
            durationMs = 200_000L,
            positionMs = 10_000L
        )
    }

    fun message(key: String = "msg", text: String = "Hello there") = NotificationInfo(
        key = key,
        packageName = "com.whatsapp",
        appName = "WhatsApp",
        title = "Sam",
        text = text,
        iconBitmap = art,
        actions = listOf(NotificationAction("Reply", null, remoteInputKey = "r"), NotificationAction("Mark as read", null))
    )

    val incomingCall: NotificationInfo
        get() = NotificationInfo(
            key = "call",
            packageName = "com.google.android.dialer",
            appName = "Phone",
            title = "Sam Carter",
            text = "Incoming call",
            isCall = true,
            isIncomingCall = true,
            isOngoing = true
        )

    val recorder: NotificationInfo
        get() = NotificationInfo(
            key = "rec",
            packageName = "com.google.android.apps.recorder",
            appName = "Recorder",
            title = "Recording",
            text = "00:12",
            isOngoing = true,
            liveTimerBaseMs = IslandClock.wallMs() - 12_000L,
            actions = listOf(NotificationAction("Pause", null), NotificationAction("Stop", null))
        )

    val timer: NotificationInfo
        get() = NotificationInfo(
            key = "timer",
            packageName = "com.google.android.deskclock",
            appName = "Clock",
            title = "Timer",
            text = "Timer",
            isOngoing = true,
            liveTimerBaseMs = IslandClock.wallMs() + 31_000L,
            liveTimerCountDown = true,
            actions = listOf(NotificationAction("Pause", null), NotificationAction("Cancel", null))
        )

    // ---- Built the same way as LiveActivityTester's demos ----

    /** A plain download (no live kind), routed through IslandStateManager.updateProgress. */
    fun progress(pct: Int) = NotificationInfo(
        key = "dl",
        packageName = "com.android.vending",
        appName = "Google Play",
        title = "Downloading Droidbubble",
        text = "12 MB of 40 MB",
        progress = pct,
        maxProgress = 100
    )

    val alarmRinging: NotificationInfo
        get() = NotificationInfo(
            key = "alarm",
            packageName = "com.google.android.deskclock",
            appName = "Clock",
            title = "Wake up",
            text = "7:00 AM",
            isOngoing = true,
            isAlert = true,
            actions = listOf(NotificationAction("Snooze", null), NotificationAction("Dismiss", null))
        )

    val timesUp: NotificationInfo
        get() = NotificationInfo(
            key = "timesup",
            packageName = "com.google.android.deskclock",
            appName = "Clock",
            title = "Time's up",
            text = "Tea",
            isOngoing = true,
            isAlert = true,
            liveTimerBaseMs = IslandClock.wallMs(),
            actions = listOf(NotificationAction("+1:00", null), NotificationAction("Stop", null))
        )

    val ride: NotificationInfo
        get() = NotificationInfo(
            key = "ride",
            packageName = "com.ubercab",
            appName = "Uber",
            title = "Driver arriving",
            text = "Toyota Camry · ABC 123",
            isOngoing = true,
            progress = 20,
            maxProgress = 100,
            shortCriticalText = "5 min"
        )

    val score: NotificationInfo
        get() = NotificationInfo(
            key = "score",
            packageName = "com.mobilefootball",
            appName = "FotMob",
            title = "Arsenal 2 - 1 Chelsea",
            text = "67'",
            isOngoing = true
        )
}
