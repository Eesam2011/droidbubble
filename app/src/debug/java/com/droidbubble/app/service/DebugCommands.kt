package com.droidbubble.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.IslandStateManager.RingerMode
import com.droidbubble.app.model.ActiveCallState
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState

/**
 * Debug-build test commands for the island (B1.1). Release and benchmark builds have a no-op
 * DebugCommands instead, so no APK that ships contains these actions.
 *
 * Registered RECEIVER_NOT_EXPORTED on every API level: only this app's own UID can send them, so
 * another installed app can't post fake cards or loop ACTION_EXPAND. That also means
 * `adb shell am broadcast` no longer reaches them (the shell is another UID). For manual testing
 * use the in-app Test Live Activities panel; adb control was explicitly declined for now.
 */
object DebugCommands {

    const val PREFIX = "com.droidbubble.app."
    const val ACTION_SIMULATE_MEDIA = PREFIX + "ACTION_SIMULATE_MEDIA"
    const val ACTION_SIMULATE_NOTIFICATION = PREFIX + "ACTION_SIMULATE_NOTIFICATION"

    val ACTIONS = listOf(
        "ACTION_SIMULATE_UNLOCK", "ACTION_SIMULATE_BLUETOOTH", "ACTION_SIMULATE_FLASHLIGHT",
        "ACTION_SIMULATE_CHARGING", "ACTION_SIMULATE_MEDIA", "ACTION_SIMULATE_NOTIFICATION",
        "ACTION_SIMULATE_PROGRESS", "ACTION_SIMULATE_RECORDING", "ACTION_SIMULATE_CALL",
        "ACTION_SIMULATE_RINGER", "ACTION_EXPAND", "ACTION_COLLAPSE", "ACTION_RESET",
        "ACTION_CRASH_TEST",
    ).map { PREFIX + it }

    fun register(context: Context): BroadcastReceiver {
        val receiver = Receiver()
        val filter = IntentFilter().apply { ACTIONS.forEach(::addAction) }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        return receiver
    }

    /**
     * B8.4: debug builds log main-thread disk reads and writes, so a blocking read in a hot path
     * (such as the service's onCreate) shows up in logcat as a StrictMode violation.
     */
    fun enableStrictMode() {
        android.os.StrictMode.setThreadPolicy(
            android.os.StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyLog().build()
        )
    }

    fun unregister(context: Context, receiver: BroadcastReceiver?) {
        if (receiver != null) runCatching { context.unregisterReceiver(receiver) }
    }

    private class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                // B11.3: crash with a package as the island's primary, to try the crash-loop guard
                PREFIX + "ACTION_CRASH_TEST" -> {
                    val pkg = intent.getStringExtra("pkg") ?: "com.example.crashy"
                    com.droidbubble.app.util.CrashLog.breadcrumb("state", "primary kind=NONE pkg=$pkg keyHash=-")
                    throw IllegalStateException("ACTION_CRASH_TEST (debug)")
                }
                "com.droidbubble.app.ACTION_SIMULATE_MEDIA" -> {
                    val title = intent.getStringExtra("title") ?: "Starboy"
                    val artist = intent.getStringExtra("artist") ?: "The Weeknd"
                    IslandStateManager.updateMedia(
                        MediaInfo(
                            packageName = "com.spotify.music",
                            title = title,
                            artist = artist,
                            isPlaying = true,
                            durationMs = 230000L,
                            positionMs = 65000L
                        )
                    )
                }
                "com.droidbubble.app.ACTION_SIMULATE_NOTIFICATION" -> {
                    val title = intent.getStringExtra("title") ?: "Sarah Jenkins"
                    val appName = intent.getStringExtra("appName") ?: "Messages"
                    val text = intent.getStringExtra("text") ?: "Dinner is ready! Can you pick up some dessert on your way home?"
                    val action1 = intent.getStringExtra("action1") ?: "Reply"
                    val action2 = intent.getStringExtra("action2")
                    val actionsList = mutableListOf<NotificationAction>()
                    if (action1.isNotBlank() && action1 != "none") {
                        actionsList.add(NotificationAction(action1, null))
                    }
                    if (!action2.isNullOrBlank()) {
                        actionsList.add(NotificationAction(action2, null))
                    }
                    IslandStateManager.updateNotification(
                        NotificationInfo(
                            key = "debug_notif_1",
                            packageName = "com.google.android.apps.messaging",
                            appName = appName,
                            title = title,
                            text = text,
                            actions = actionsList
                        )
                    )
                }
                "com.droidbubble.app.ACTION_SIMULATE_PROGRESS" -> {
                    val progress = intent.getIntExtra("progress", 68)
                    val maxProgress = intent.getIntExtra("max", 100)
                    val appName = intent.getStringExtra("appName") ?: "Google Play Store"
                    val title = intent.getStringExtra("title") ?: "Updating Spotify"
                    val text = intent.getStringExtra("text") ?: "45 MB / 66 MB • 3.2 MB/s"
                    IslandStateManager.updateNotification(
                        NotificationInfo(
                            key = "debug_progress_1",
                            packageName = "com.android.vending",
                            appName = appName,
                            title = title,
                            text = text,
                            progress = progress,
                            maxProgress = maxProgress,
                            isOngoing = true
                        )
                    )
                }
                "com.droidbubble.app.ACTION_SIMULATE_RECORDING" -> {
                    val isRec = intent.getBooleanExtra("isRecording", true)
                    IslandStateManager.updateRecordingState(
                        ScreenRecordingState(isRecording = isRec, durationSeconds = 42)
                    )
                }
                "com.droidbubble.app.ACTION_SIMULATE_CALL" -> {
                    val isAct = intent.getBooleanExtra("isActive", true)
                    val caller = intent.getStringExtra("callerName") ?: "Alex Rivera"
                    IslandStateManager.updateCallState(
                        ActiveCallState(isActive = isAct, callerName = caller, durationSeconds = 84)
                    )
                }
                "com.droidbubble.app.ACTION_EXPAND" -> {
                    IslandStateManager.expand()
                }
                "com.droidbubble.app.ACTION_COLLAPSE" -> {
                    IslandStateManager.collapse()
                }
                "com.droidbubble.app.ACTION_RESET" -> {
                    IslandStateManager.updateMedia(null)
                    IslandStateManager.updateNotification(null)
                    IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = false))
                    IslandStateManager.updateCallState(ActiveCallState(isActive = false))
                    IslandStateManager.updateFlashlightState(false)
                    IslandStateManager.collapse()
                }
                "com.droidbubble.app.ACTION_SIMULATE_UNLOCK" -> {
                    IslandStateManager.triggerUnlockEvent()
                }
                "com.droidbubble.app.ACTION_SIMULATE_BLUETOOTH" -> {
                    IslandStateManager.triggerBluetoothEvent()
                }
                "com.droidbubble.app.ACTION_SIMULATE_FLASHLIGHT" -> {
                    val enabled = intent.getBooleanExtra("enabled", true)
                    val level = intent.getIntExtra("level", 3)
                    IslandStateManager.updateFlashlightState(enabled, level.coerceIn(1, 4), 4)
                }
                "com.droidbubble.app.ACTION_SIMULATE_CHARGING" -> {
                    val pct = intent.getIntExtra("percentage", 85)
                    val wattage = if (intent.hasExtra("wattage")) {
                        intent.getDoubleExtra("wattage", 0.0).takeIf { it > 0 }
                    } else {
                        BatteryUtils.getChargingWattage(context)
                    }
                    IslandStateManager.triggerChargingEvent(pct, wattage)
                }
                "com.droidbubble.app.ACTION_SIMULATE_RINGER" -> {
                    val modeStr = intent.getStringExtra("mode")?.lowercase()
                    val mode = when (modeStr) {
                        "silent" -> RingerMode.SILENT
                        "vibrate" -> RingerMode.VIBRATE
                        else -> RingerMode.RING
                    }
                    IslandStateManager.triggerRingerModeEvent(mode)
                }
            }
        }
    }
}
