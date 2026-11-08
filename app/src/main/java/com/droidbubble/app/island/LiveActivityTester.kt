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

    fun show(demo: Demo) {
        clear()
        val now = System.currentTimeMillis()
        when (demo) {
            Demo.CALL -> post(
                base(demo, "com.google.android.dialer", "Phone", "Sam Carter", "Incoming call").copy(
                    isCall = true, isIncomingCall = true, isOngoing = false
                )
            )
            Demo.TIMER -> postTimer(remainingMs = 5 * 60_000L, running = true)
            Demo.TIMES_UP -> post(
                base(demo, "com.google.android.deskclock", "Clock", "Time's up", "Tea").copy(
                    isAlert = true, liveTimerBaseMs = now, actions = actions("+1:00", "Stop")
                )
            )
            Demo.ALARM -> post(
                base(demo, "com.google.android.deskclock", "Clock", "Wake up", "7:00 AM").copy(
                    isAlert = true, actions = actions("Snooze", "Dismiss")
                )
            )
            Demo.SNOOZED -> post(
                base(demo, "com.google.android.deskclock", "Clock", "Alarm", "Snoozing until 7:10 AM").copy(
                    actions = actions("Dismiss")
                )
            )
            Demo.STOPWATCH -> post(
                base(demo, "com.google.android.deskclock", "Clock", "Stopwatch", "").copy(
                    liveTimerBaseMs = now - 83_000L, actions = actions("Pause", "Reset")
                )
            )
            Demo.RECORDER -> post(
                base(demo, "com.google.android.apps.recorder", "Recorder", "Recording", "").copy(
                    liveTimerBaseMs = now, actions = actions("Pause", "Stop")
                )
            )
            Demo.DOWNLOAD -> {
                val start = base(demo, "com.android.vending", "Google Play", "Downloading Droidbubble", "12 MB of 40 MB")
                    .copy(isOngoing = false, progress = 0, maxProgress = 100)
                tick(start, steps = 50, everyMs = 200L) { n, i -> n.copy(progress = i * 2) }
            }
            Demo.RIDE -> {
                val ride = base(demo, "com.ubercab", "Uber", "Driver arriving", "Toyota Camry · ABC 123")
                    .copy(progress = 0, maxProgress = 100, shortCriticalText = "5 min")
                tick(ride, steps = 50, everyMs = 1_000L) { n, i ->
                    val mins = (5 - i / 10).coerceAtLeast(1)
                    n.copy(progress = i * 2, shortCriticalText = if (i >= 48) "Here" else "$mins min")
                }
            }
            Demo.DELIVERY -> {
                val food = base(demo, "com.example.food", "Food", "Order on the way", "Rider is 1.2 km away")
                    .copy(isPromoted = true, progress = 40, maxProgress = 100, shortCriticalText = "12 min")
                tick(food, steps = 30, everyMs = 1_000L) { n, i -> n.copy(progress = 40 + i * 2) }
            }
            Demo.FLIGHT -> post(
                base(demo, "com.flightradar24free", "Flightradar24", "EK 202 to New York", "Departs 10:40 · Gate B12")
                    .copy(progress = 0, maxProgress = 100, shortCriticalText = "Boarding")
            )
            Demo.SCORE -> {
                val match = base(demo, "com.mobilefootball", "FotMob", "Arsenal 2 - 1 Chelsea", "67'")
                tick(match, steps = 20, everyMs = 1_500L) { n, i ->
                    val minute = 67 + i
                    n.copy(
                        title = if (i >= 12) "Arsenal 3 - 1 Chelsea" else n.title,
                        text = if (i >= 19) "FT" else "$minute'"
                    )
                }
            }
        }
    }

    private fun postTimer(remainingMs: Long, running: Boolean) {
        val n = base(Demo.TIMER, "com.google.android.deskclock", "Clock", "Timer", if (running) "" else formatTimerText(remainingMs))
            .copy(
                liveTimerBaseMs = if (running) System.currentTimeMillis() + remainingMs else 0L,
                liveTimerCountDown = true,
                actions = if (running) actions("Pause", "+1:00", "Cancel") else actions("Resume", "Cancel")
            )
        post(n)
    }

    private fun post(n: NotificationInfo) {
        if (n.hasProgress && liveActivityKind(n) == LiveActivityKind.NONE) {
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
