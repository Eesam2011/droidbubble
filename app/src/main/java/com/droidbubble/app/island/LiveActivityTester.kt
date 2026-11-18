package com.droidbubble.app.island

import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fake live activities for the settings test page, so every design can be checked without the
 * real apps. They go through the same IslandStateManager calls as real notifications, use real
 * package names so detection picks the right design, and their buttons work (handled here, since
 * fake actions have no PendingIntent).
 */
object LiveActivityTester {
    private const val PREFIX = "droidbubble.test."
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null

    enum class Demo(val label: String) {
        CALL("Incoming call"),
        TIMER("Timer"),
        TIMES_UP("Time's up"),
        ALARM("Alarm ringing"),
        SNOOZED("Snoozed alarm"),
        STOPWATCH("Stopwatch"),
        RECORDER("Voice recorder"),
        DOWNLOAD("Download"),
        RIDE("Ride"),
        DELIVERY("Live update"),
        FLIGHT("Flight"),
        SCORE("Live score")
    }

    fun isTest(key: String) = key.startsWith(PREFIX)

    private fun key(demo: Demo) = PREFIX + demo.name.lowercase()

    private fun actions(vararg titles: String) = titles.map { NotificationAction(it, null) }

    private fun base(demo: Demo, pkg: String, app: String, title: String, text: String) = NotificationInfo(
        key = key(demo),
        packageName = pkg,
        appName = app,
        title = title,
        text = text,
        isOngoing = true
    )

    /**
     * The first notification [demo] posts. Pure, so tests (and A1.2's mapper test) can use the
     * same demos the Test lab shows.
     */
    fun demoNotification(demo: Demo, now: Long): NotificationInfo = when (demo) {
        Demo.CALL -> base(demo, "com.google.android.dialer", "Phone", "Sam Carter", "Incoming call").copy(
            isCall = true, isIncomingCall = true, isOngoing = false
        )
        Demo.TIMER -> timer(remainingMs = 5 * 60_000L, running = true, now = now)
        Demo.TIMES_UP -> base(demo, "com.google.android.deskclock", "Clock", "Time's up", "Tea").copy(
            isAlert = true, liveTimerBaseMs = now, actions = actions("+1:00", "Stop")
        )
        Demo.ALARM -> base(demo, "com.google.android.deskclock", "Clock", "Wake up", "7:00 AM").copy(
            isAlert = true, actions = actions("Snooze", "Dismiss")
        )
        Demo.SNOOZED -> base(demo, "com.google.android.deskclock", "Clock", "Alarm", "Snoozing until 7:10 AM").copy(
            actions = actions("Dismiss")
        )
        Demo.STOPWATCH -> base(demo, "com.google.android.deskclock", "Clock", "Stopwatch", "").copy(
            liveTimerBaseMs = now - 83_000L, actions = actions("Pause", "Reset")
        )
        Demo.RECORDER -> base(demo, "com.google.android.apps.recorder", "Recorder", "Recording", "").copy(
            liveTimerBaseMs = now, actions = actions("Pause", "Stop")
        )
        Demo.DOWNLOAD -> base(demo, "com.android.vending", "Google Play", "Downloading Droidbubble", "12 MB of 40 MB")
            .copy(isOngoing = false, progress = 0, maxProgress = 100)
        Demo.RIDE -> base(demo, "com.ubercab", "Uber", "Driver arriving", "Toyota Camry · ABC 123")
            .copy(progress = 0, maxProgress = 100, shortCriticalText = "5 min")
        Demo.DELIVERY -> base(demo, "com.example.food", "Food", "Order on the way", "Rider is 1.2 km away")
            .copy(isPromoted = true, progress = 40, maxProgress = 100, shortCriticalText = "12 min")
        Demo.FLIGHT -> base(demo, "com.flightradar24free", "Flightradar24", "EK 202 to New York", "Departs 10:40 · Gate B12")
            .copy(progress = 0, maxProgress = 100, shortCriticalText = "Boarding")
        Demo.SCORE -> base(demo, "com.mobilefootball", "FotMob", "Arsenal 2 - 1 Chelsea", "67'")
    }

    fun show(demo: Demo) {
        clear()
        val first = demoNotification(demo, System.currentTimeMillis())
        when (demo) {
            Demo.DOWNLOAD -> tick(first, steps = 50, everyMs = 200L) { n, i -> n.copy(progress = i * 2) }
            Demo.RIDE -> tick(first, steps = 50, everyMs = 1_000L) { n, i ->
                val mins = (5 - i / 10).coerceAtLeast(1)
                n.copy(progress = i * 2, shortCriticalText = if (i >= 48) "Here" else "$mins min")
            }
            Demo.DELIVERY -> tick(first, steps = 30, everyMs = 1_000L) { n, i -> n.copy(progress = 40 + i * 2) }
            Demo.SCORE -> tick(first, steps = 20, everyMs = 1_500L) { n, i ->
                val minute = 67 + i
                n.copy(
                    title = if (i >= 12) "Arsenal 3 - 1 Chelsea" else n.title,
                    text = if (i >= 19) "FT" else "$minute'"
                )
            }
            else -> post(first)
        }
    }

    private fun timer(remainingMs: Long, running: Boolean, now: Long): NotificationInfo =
        base(Demo.TIMER, "com.google.android.deskclock", "Clock", "Timer", if (running) "" else formatTimerText(remainingMs))
            .copy(
                liveTimerBaseMs = if (running) now + remainingMs else 0L,
                liveTimerCountDown = true,
                actions = if (running) actions("Pause", "+1:00", "Cancel") else actions("Resume", "Cancel")
            )

    private fun postTimer(remainingMs: Long, running: Boolean) =
        post(timer(remainingMs, running, System.currentTimeMillis()))

    private fun post(n: NotificationInfo) {
        if (n.hasProgress && n.kind == LiveActivityKind.NONE) {
            IslandStateManager.updateProgress(n)
        } else {
            IslandStateManager.updateNotification(n)
        }
    }

    private fun tick(start: NotificationInfo, steps: Int, everyMs: Long, step: (NotificationInfo, Int) -> NotificationInfo) {
        post(start)
        tickJob = scope.launch {
            for (i in 1..steps) {
                delay(everyMs)
                post(step(start, i))
            }
            delay(1_500L)
            remove(start.key)
        }
    }

    private fun remove(key: String) = IslandStateManager.removeNotification(key)

    /** Removes every test activity from the island. */
    fun clear() {
        tickJob?.cancel()
        tickJob = null
        Demo.entries.forEach { remove(key(it)) }
        remove(IconDemo.KEY_MESSAGE)
        remove(IconDemo.KEY_DOWNLOAD)
        if (iconDemoTorch) {
            IslandStateManager.updateFlashlightState(false)
            iconDemoTorch = false
        }
    }

    /**
     * I4.2: events that make the island's icons react, fired through the same public
     * IslandStateManager calls as the real sources (never the debug broadcast receiver), so each
     * row of the phase-5 wiring table can be checked on a phone without the hardware. The icon
     * signal counters are left alone by [clear]: resetting one would itself look like an event.
     */
    enum class IconDemo(val label: String) {
        SAME_APP("Same app ×3"),
        BLUETOOTH("Connect Bluetooth"),
        RINGER("Ringer cycle"),
        CHARGING("Charging 40 %"),
        DOWNLOAD("Download 0→100"),
        TORCH("Torch levels");

        companion object {
            const val KEY_MESSAGE = PREFIX + "icon.message"
            const val KEY_DOWNLOAD = PREFIX + "icon.download"
            /** Gap between the steps of a demo, long enough to watch each effect play out. */
            const val STEP_MS = 1_200L
        }
    }

    private var iconDemoTorch = false

    fun show(demo: IconDemo) {
        clear()
        tickJob = scope.launch { runIconDemo(demo) }
    }

    /** The steps of [demo]. Suspends between steps; public for tests. */
    suspend fun runIconDemo(demo: IconDemo) {
        val step = IconDemo.STEP_MS
        when (demo) {
            IconDemo.SAME_APP -> listOf("Are you coming?", "I'm outside", "Hello??").forEach { text ->
                post(
                    NotificationInfo(
                        key = IconDemo.KEY_MESSAGE, packageName = "com.whatsapp", appName = "WhatsApp",
                        title = "Sam Carter", text = text, actions = actions("Reply", "Mark as read"),
                    )
                )
                delay(step)
            }
            IconDemo.BLUETOOTH -> IslandStateManager.triggerBluetoothEvent()
            IconDemo.RINGER -> listOf(
                IslandStateManager.RingerMode.RING, IslandStateManager.RingerMode.SILENT,
                IslandStateManager.RingerMode.RING, IslandStateManager.RingerMode.VIBRATE,
                IslandStateManager.RingerMode.RING,
            ).forEach { mode ->
                IslandStateManager.triggerRingerModeEvent(mode)
                delay(step)
            }
            IconDemo.CHARGING -> IslandStateManager.triggerChargingEvent(40, 18.0)
            IconDemo.DOWNLOAD -> {
                val start = NotificationInfo(
                    key = IconDemo.KEY_DOWNLOAD, packageName = "com.android.vending", appName = "Google Play",
                    title = "Downloading Maps", text = "", progress = 0, maxProgress = 100, isOngoing = true,
                )
                for (pct in 0..100 step 10) {
                    IslandStateManager.updateProgress(start.copy(progress = pct))
                    delay(step / 3)
                }
                delay(step)
                remove(IconDemo.KEY_DOWNLOAD)
            }
            IconDemo.TORCH -> {
                iconDemoTorch = true
                for (level in listOf(1, 2, 3, 4, 2)) {
                    IslandStateManager.updateFlashlightState(true, level = level, maxLevel = 4)
                    delay(step)
                }
                IslandStateManager.updateFlashlightState(false)
                iconDemoTorch = false
            }
        }
    }

    /** Buttons of a fake activity (they have no PendingIntent to fire). */
    fun onAction(n: NotificationInfo, action: NotificationAction) {
        if (!isTest(n.key)) return
        val title = action.title.lowercase()
        when {
            n.key == key(Demo.TIMER) -> when {
                title == "pause" -> postTimer((n.liveTimerBaseMs - System.currentTimeMillis()).coerceAtLeast(0L), running = false)
                title == "resume" -> postTimer(parseTimerTextMillisSafe(n.text), running = true)
                title.startsWith("+") -> post(n.copy(liveTimerBaseMs = n.liveTimerBaseMs + 60_000L))
                else -> remove(n.key)
            }
            n.key == key(Demo.TIMES_UP) && title.startsWith("+") -> {
                remove(n.key)
                postTimer(60_000L, running = true)
            }
            n.key == key(Demo.ALARM) && title == "snooze" -> {
                remove(n.key)
                show(Demo.SNOOZED)
            }
            n.key == key(Demo.RECORDER) && title == "pause" ->
                post(n.copy(liveTimerBaseMs = 0L, title = formatTimerText(System.currentTimeMillis() - n.liveTimerBaseMs), text = "Paused", actions = actions("Resume", "Stop")))
            n.key == key(Demo.RECORDER) && title == "resume" ->
                post(n.copy(liveTimerBaseMs = System.currentTimeMillis() - parseTimerTextMillisSafe(n.title), title = "Recording", text = "", actions = actions("Pause", "Stop")))
            n.key == key(Demo.STOPWATCH) && title == "pause" ->
                post(n.copy(liveTimerBaseMs = 0L, text = formatTimerText(System.currentTimeMillis() - n.liveTimerBaseMs), actions = actions("Start", "Reset")))
            n.key == key(Demo.STOPWATCH) && title == "start" ->
                post(n.copy(liveTimerBaseMs = System.currentTimeMillis() - parseTimerTextMillisSafe(n.text), text = "", actions = actions("Pause", "Reset")))
            else -> remove(n.key)
        }
    }

    private fun parseTimerTextMillisSafe(text: String): Long =
        com.droidbubble.app.model.parseTimerTextMillis(text) ?: 0L
}
