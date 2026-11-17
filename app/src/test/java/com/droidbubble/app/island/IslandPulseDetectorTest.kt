package com.droidbubble.app.island

import com.droidbubble.app.model.NotificationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** D2.3: "same app, new event" beats: what pulses, what doesn't, and the 600 ms throttle. */
class IslandPulseDetectorTest {

    private fun msg(key: String, text: String, pkg: String = "com.whatsapp") = NotificationInfo(
        key = key, packageName = pkg, appName = "WhatsApp", title = "Sam", text = text,
    )

    private fun timer(text: String) = NotificationInfo(
        key = "timer", packageName = "com.google.android.deskclock", appName = "Clock", title = "Timer",
        text = text, isOngoing = true, liveTimerBaseMs = 1_000L,
    )

    /** Feeds [updates] one second apart and counts the pulses. */
    private fun pulses(updates: List<NotificationInfo>, stepMs: Long = 1_000): Int {
        val d = PulseDetector()
        var previous: NotificationInfo? = null
        var count = 0
        updates.forEachIndexed { i, n ->
            if (d.onUpdate(previous, n, i * stepMs) != null) count++
            previous = n
        }
        return count
    }

    @Test
    fun threeMessagesInOneChat_afterTheFirst_pulseEachTime() {
        val chat = listOf(msg("c", "Hi"), msg("c", "Are you there?"), msg("c", "Hello?"), msg("c", "Call me"))
        assertEquals(3, pulses(chat))
    }

    @Test
    fun theFirstAppearance_isNotAPulse() {
        assertNull(PulseDetector().onUpdate(null, msg("c", "Hi"), 0))
    }

    @Test
    fun timerTicks_neverPulse() {
        assertEquals(0, pulses((59 downTo 50).map { timer("00:$it") }))
    }

    @Test
    fun anotherApp_isASwap_notAPulse() {
        assertEquals(0, pulses(listOf(msg("a", "Hi"), msg("b", "Yo", pkg = "org.telegram.messenger"))))
    }

    @Test
    fun aBurst_withinSixHundredMs_isOneBeat() {
        val burst = listOf(msg("c", "1"), msg("c", "2"), msg("c", "3"), msg("c", "4"), msg("c", "5"))
        assertEquals("200 ms apart: the 2nd and the 5th (600 ms later) pulse", 2, pulses(burst, stepMs = 200))
    }
}
