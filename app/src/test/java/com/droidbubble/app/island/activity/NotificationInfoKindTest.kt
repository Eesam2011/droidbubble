package com.droidbubble.app.island.activity

import com.droidbubble.app.island.LiveActivityKind
import com.droidbubble.app.island.LiveActivityKindStats
import com.droidbubble.app.model.NotificationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** A1.1: the kind is classified once per instance, re-classified on copy(), and outside equals(). */
class NotificationInfoKindTest {

    private fun clock(title: String) = NotificationInfo(
        key = "k", packageName = "com.google.android.deskclock", appName = "Clock",
        title = title, text = "", isOngoing = true, liveTimerBaseMs = 1L,
    )

    @Test
    fun copyReclassifies() {
        val stopwatch = clock("Stopwatch")
        assertEquals(LiveActivityKind.STOPWATCH, stopwatch.kind)
        val timesUp = stopwatch.copy(title = "Time's up")
        assertEquals(LiveActivityKind.TIMER, timesUp.kind)
    }

    @Test
    fun equalsIgnoresKind() {
        val a = clock("Stopwatch")
        val b = clock("Stopwatch")
        a.kind // a classified, b not yet
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(title = "Timer"))
    }

    @Test
    fun classifiesOncePerInstance() {
        val n = clock("Stopwatch")
        val before = LiveActivityKindStats.calls.get()
        repeat(60) { n.kind } // a minute of timer ticks reading the kind
        assertEquals(1, LiveActivityKindStats.calls.get() - before)
    }
}
