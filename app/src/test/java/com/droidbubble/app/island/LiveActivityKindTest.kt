package com.droidbubble.app.island

import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which notifications get the native call / recorder / timer designs, and how buttons map. */
class LiveActivityKindTest {

    private fun n(
        pkg: String = "com.example",
        app: String = "App",
        title: String = "Title",
        text: String = "",
        base: Long = 0L,
        countDown: Boolean = false,
        ongoing: Boolean = true,
        call: Boolean = false,
        actions: List<String> = emptyList()
    ) = NotificationInfo(
        key = "k", packageName = pkg, appName = app, title = title, text = text,
        liveTimerBaseMs = base, liveTimerCountDown = countDown, isOngoing = ongoing, isCall = call,
        actions = actions.map { NotificationAction(it, null) }
    )

    @Test
    fun calls() {
        assertEquals(LiveActivityKind.CALL, liveActivityKind(n(call = true)))
    }

    @Test
    fun voiceRecorders() {
        assertEquals(LiveActivityKind.RECORDING, liveActivityKind(n(pkg = "com.google.android.apps.recorder", base = 1L)))
        assertEquals(LiveActivityKind.RECORDING, liveActivityKind(n(pkg = "com.sec.android.app.voicenote", title = "Recording", base = 1L)))
        // Paused recorder: no live timer any more, still the recorder design
        assertEquals(LiveActivityKind.RECORDING, liveActivityKind(n(pkg = "com.miui.soundrecorder", title = "12:04", text = "Paused")))
    }

    @Test
    fun timersAndStopwatches() {
        assertEquals(LiveActivityKind.TIMER, liveActivityKind(n(pkg = "com.google.android.deskclock", title = "Timer", base = 1L, countDown = true)))
        assertEquals(LiveActivityKind.STOPWATCH, liveActivityKind(n(pkg = "com.google.android.deskclock", title = "Stopwatch", base = 1L)))
        assertEquals(LiveActivityKind.TIMER, liveActivityKind(n(pkg = "com.sec.android.app.clockpackage", title = "Time's up", base = 1L)))
    }

    @Test
    fun customViewClockTimers() {
        // Google / AOSP Clock: label + chronometer text read from the custom view
        assertEquals(
            LiveActivityKind.TIMER,
            liveActivityKind(n(pkg = "com.google.android.deskclock", app = "Clock", title = "Tea", text = "4:59", base = 1L, countDown = true))
        )
        // Paused: no live base, label just says "Paused", but the countdown flag keeps the timer design
        assertEquals(
            LiveActivityKind.TIMER,
            liveActivityKind(n(pkg = "com.google.android.deskclock", app = "Clock", title = "Paused", text = "4:59", countDown = true))
        )
        assertTrue(isClockOrRecorderPackage("com.google.android.deskclock"))
        assertTrue(isClockOrRecorderPackage("com.sec.android.app.voicenote"))
        assertFalse(isClockOrRecorderPackage("com.whatsapp"))
    }

    @Test
    fun oemRecorderAndClockPackages() {
        for (pkg in listOf("com.oneplus.soundrecorder", "com.android.soundrecorder", "com.coloros.soundrecorder", "com.android.bbksoundrecorder")) {
            assertTrue(pkg, isRecorderPackage(pkg))
        }
        for (pkg in listOf("com.oneplus.deskclock", "com.android.deskclock", "com.coloros.alarmclock", "com.android.bbkclock")) {
            assertTrue(pkg, isClockPackage(pkg))
        }
        assertEquals(LiveActivityKind.RECORDING, liveActivityKind(n(pkg = "com.oneplus.soundrecorder", title = "Recording", base = 1L)))
    }

    @Test
    fun alertsAndAlarms() {
        val timesUp = n(pkg = "com.google.android.deskclock", title = "Time's up", text = "Tea", actions = listOf("+1:00", "Stop")).copy(isAlert = true)
        assertEquals(LiveActivityKind.ALERT, liveActivityKind(timesUp))
        assertTrue(isTimerAlert(timesUp))
        val controls = alertControls(timesUp)
        assertEquals("+1:00", controls.extra?.title)
        assertEquals("Stop", controls.stop?.title)

        val ringing = n(pkg = "com.android.deskclock", title = "Wake up", text = "7:00 AM", actions = listOf("Snooze", "Dismiss")).copy(isAlert = true)
        assertFalse(isTimerAlert(ringing))
        assertEquals("Snooze", alertControls(ringing).extra?.title)
        assertEquals("Dismiss", alertControls(ringing).stop?.title)

        val snoozed = n(pkg = "com.google.android.deskclock", app = "Clock", title = "Alarm", text = "Snoozing until 7:10 AM")
        assertEquals(LiveActivityKind.ALARM, liveActivityKind(snoozed))
        assertEquals("7:10 AM", alarmTimeText(snoozed))
        // Upcoming alarm: not ongoing, still the alarm design
        assertEquals(LiveActivityKind.ALARM, liveActivityKind(n(pkg = "com.oneplus.deskclock", title = "Upcoming alarm", text = "Wed 7:00 AM", ongoing = false)))
    }

    @Test
    fun ridesLiveUpdatesAndScores() {
        val ride = n(pkg = "com.ubercab", title = "Driver arriving", text = "Arrives in 4 min")
        assertEquals(LiveActivityKind.TRACKING, liveActivityKind(ride))
        assertEquals("4 min", trackingChipText(ride))
        // Android 16 promoted notification from any app
        val live = n(pkg = "com.example.food", title = "Order on the way").copy(isPromoted = true, shortCriticalText = "12 min")
        assertEquals(LiveActivityKind.TRACKING, liveActivityKind(live))
        assertEquals("12 min", trackingChipText(live))
        // Not ongoing + not a tracking app: regular card
        assertEquals(LiveActivityKind.NONE, liveActivityKind(n(pkg = "com.ubercab", title = "Your receipt", ongoing = false)))

        val match = n(pkg = "com.mobilefootball", title = "Arsenal 2 - 1 Chelsea", text = "67'", ongoing = false)
        assertEquals(LiveActivityKind.SPORTS, liveActivityKind(match))
        val score = parseLiveScore(match)!!
        assertEquals("Arsenal", score.home)
        assertEquals("2", score.homeScore)
        assertEquals("1", score.awayScore)
        assertEquals("Chelsea", score.away)
        assertEquals("67'", score.status)
        // A message that just contains numbers isn't a score
        assertEquals(LiveActivityKind.NONE, liveActivityKind(n(pkg = "com.whatsapp", title = "Sam", text = "see you at 2 - 3 pm", ongoing = false)))
    }

    @Test
    fun countdownTotals_growWithExtraTime() {
        assertEquals(60_000L, CountdownTotals.totalFor("t", 60_000L))
        assertEquals(60_000L, CountdownTotals.totalFor("t", 30_000L))
        assertEquals(90_000L, CountdownTotals.totalFor("t", 90_000L))
    }

    @Test
    fun regularNotifications_keepTheNormalCard() {
        assertEquals(LiveActivityKind.NONE, liveActivityKind(null))
        assertEquals(LiveActivityKind.NONE, liveActivityKind(n(pkg = "com.whatsapp", title = "Sam", text = "Hi", ongoing = false)))
        assertEquals(LiveActivityKind.NONE, liveActivityKind(n(pkg = "com.spotify.music", title = "Song", ongoing = true)))
    }

    @Test
    fun controls_mapPauseAndStop() {
        val running = liveActivityControls(n(actions = listOf("Pause", "Stop")))
        assertEquals("Pause", running.pauseResume?.title)
        assertEquals("Stop", running.stop?.title)
        assertFalse(running.isPaused)

        val paused = liveActivityControls(n(actions = listOf("Resume", "Delete")))
        assertTrue(paused.isPaused)
        assertEquals("Delete", paused.stop?.title)

        val timer = liveActivityControls(n(actions = listOf("+1:00", "Pause", "Cancel")))
        assertEquals("Pause", timer.pauseResume?.title)
        assertEquals("Cancel", timer.stop?.title)
    }

    @Test
    fun controls_doNotMatchWordsInsideOtherWords() {
        // "Send" contains "end", "Extend" too: neither is a stop button
        val c = liveActivityControls(n(actions = listOf("Send", "Extend")))
        assertNull(c.stop)
        assertNull(c.pauseResume)
    }

    @Test
    fun staticTimeText_readsTheNotificationTime() {
        assertEquals("12:04", staticLiveTimeText(n(title = "12:04")))
        assertNull(staticLiveTimeText(n(title = "Recording")))
    }
}
