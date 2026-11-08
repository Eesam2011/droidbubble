package com.droidbubble.app.island.harness

import android.graphics.Bitmap
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo

object IslandFixtures {
    private val art: Bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF884422.toInt()) }

    val song = MediaInfo(
        packageName = "com.spotify.music",
        title = "Song",
        artist = "Artist",
        albumArt = art,
        isPlaying = true,
        durationMs = 200_000L,
        positionMs = 10_000L
    )

    fun message(key: String = "msg", text: String = "Hello there") = NotificationInfo(
        key = key,
        packageName = "com.whatsapp",
        appName = "WhatsApp",
        title = "Sam",
        text = text,
        iconBitmap = art,
        actions = listOf(NotificationAction("Reply", null, remoteInputKey = "r"), NotificationAction("Mark as read", null))
    )

    val incomingCall = NotificationInfo(
        key = "call",
        packageName = "com.google.android.dialer",
        appName = "Phone",
        title = "Essam medhat",
        text = "Incoming call",
        isCall = true,
        isIncomingCall = true,
        isOngoing = true
    )

    val recorder get() = NotificationInfo(
        key = "rec",
        packageName = "com.google.android.apps.recorder",
        appName = "Recorder",
        title = "Recording",
        text = "00:12",
        isOngoing = true,
        liveTimerBaseMs = System.currentTimeMillis() - 12_000L,
        actions = listOf(NotificationAction("Pause", null), NotificationAction("Stop", null))
    )

    val timer get() = NotificationInfo(
        key = "timer",
        packageName = "com.google.android.deskclock",
        appName = "Clock",
        title = "Timer",
        text = "Timer",
        isOngoing = true,
        liveTimerBaseMs = System.currentTimeMillis() + 31_000L,
        liveTimerCountDown = true,
        actions = listOf(NotificationAction("Pause", null), NotificationAction("Cancel", null))
    )

    fun progress(pct: Int) = NotificationInfo(
        key = "download", packageName = "com.android.vending", appName = "Google Play",
        title = "Downloading Droidbubble", text = "Download", progress = pct, maxProgress = 100
    )

    val alarmRinging get() = timer.copy(
        key = "alarm", title = "Wake up", text = "7:00 AM", isAlert = true,
        liveTimerBaseMs = 0L, liveTimerCountDown = false,
        actions = listOf(NotificationAction("Snooze", null), NotificationAction("Dismiss", null))
    )

    val timesUp get() = timer.copy(
        key = "times_up", title = "Time's up", text = "Tea", isAlert = true,
        liveTimerBaseMs = System.currentTimeMillis(), liveTimerCountDown = false,
        actions = listOf(NotificationAction("+1:00", null), NotificationAction("Stop", null))
    )

    val ride = NotificationInfo(
        key = "ride", packageName = "com.ubercab", appName = "Uber", title = "Driver arriving",
        text = "Toyota Camry", isOngoing = true, progress = 0, maxProgress = 100, shortCriticalText = "5 min"
    )

    val score = NotificationInfo(
        key = "score", packageName = "com.mobilefootball", appName = "FotMob",
        title = "Arsenal 2 - 1 Chelsea", text = "67'", isOngoing = true
    )
}
