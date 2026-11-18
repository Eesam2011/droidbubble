package com.droidbubble.app.island.activity

import com.droidbubble.app.island.activity.adapters.ChronometerReader
import com.droidbubble.app.island.activity.adapters.ChronometerReader.Inputs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A3.7: the timer-base rules NRS used, now pure. The fixture parity (identical bases for every
 * clock/ and recorder/ fixture through the real NRS) is FixtureCharacterizationTest's golden.
 */
class ChronometerReaderTest {

    private fun inputs(
        chrono: Boolean = false, countDown: Boolean = false, whenMs: Long = 0L, post: Long = 100_000L,
        title: String? = null, text: String? = null, ongoing: Boolean = true,
        custom: ChronometerReader.CustomViewTimer? = null,
    ) = Inputs(chrono, countDown, whenMs, post, title, text, ongoing, custom)

    @Test
    fun chronometerUsesWhen() {
        val r = ChronometerReader.read(inputs(chrono = true, countDown = true, whenMs = 50_000L))
        assertEquals(50_000L, r.rawBaseMs)
        assertTrue(r.countDown)
        assertFalse(r.baseFromText)
    }

    @Test
    fun countDownFlagNeedsTheChronometer() {
        // EXTRA_CHRONOMETER_COUNT_DOWN without EXTRA_SHOW_CHRONOMETER is ignored, as before
        assertFalse(ChronometerReader.read(inputs(countDown = true, title = "00:42")).countDown)
    }

    @Test
    fun staticTitleCountsFromPostTime() {
        val r = ChronometerReader.read(inputs(title = "00:42"))
        assertEquals(100_000L - 42_000L, r.rawBaseMs)
        assertTrue(r.baseFromText)
    }

    @Test
    fun pausedOrNotOngoingTitleHasNoBase() {
        assertEquals(0L, ChronometerReader.read(inputs(title = "00:42", text = "Paused")).rawBaseMs)
        assertTrue(ChronometerReader.read(inputs(title = "00:42", text = "Paused")).paused)
        assertEquals(0L, ChronometerReader.read(inputs(title = "00:42", ongoing = false)).rawBaseMs)
    }

    @Test
    fun customViewRunningAndPaused() {
        val running = ChronometerReader.CustomViewTimer(70_000L, countDown = true, running = true, timeText = "4:59", label = "Tea")
        val r = ChronometerReader.read(inputs(custom = running))
        assertEquals(70_000L, r.rawBaseMs)
        assertTrue(r.countDown)
        assertFalse(r.baseFromText)
        assertEquals(0L, ChronometerReader.read(inputs(custom = running.copy(running = false))).rawBaseMs)
    }

    @Test
    fun customViewRunningDetection() {
        fun cv(actions: List<String>, labels: List<String> = listOf("Tea", "4:59")) =
            ChronometerReader.fromCustomView(
                chronoBaseElapsed = 10_000L, elapsedNow = 4_000L, wallNow = 1_000_000L,
                isCountDown = true, chronoText = "−4:59", labels = labels, actionTitles = actions,
            )
        val t = cv(listOf("Pause", "Reset"))
        assertTrue(t.running)
        assertEquals(1_006_000L, t.baseWallMs)
        assertEquals("4:59", t.timeText)
        assertEquals("Tea", t.label)
        assertFalse(cv(listOf("Resume", "Reset")).running)
        assertFalse(cv(listOf("Pause"), labels = listOf("Paused")).running)
        // "Stopwatch" in a button isn't "stop"
        assertFalse(cv(listOf("Start stopwatch")).running)
        // No buttons at all: assume running
        assertTrue(cv(emptyList()).running)
        assertEquals("Timer", cv(emptyList(), labels = listOf("4:59")).label)
    }

    @Test
    fun customViewGate() {
        fun gate(pkg: Boolean = false, category: String? = null, chrono: Boolean = false, call: Boolean = false) =
            ChronometerReader.wantsCustomViewTimer(true, false, call, chrono, pkg, category)
        assertTrue(gate(pkg = true))
        assertTrue(gate(category = "alarm"))
        assertTrue(gate(category = ChronometerReader.CATEGORY_STOPWATCH))
        assertFalse(gate(category = "msg"))
        assertFalse(gate(pkg = true, chrono = true))
        assertFalse(gate(pkg = true, call = true))
        assertFalse(ChronometerReader.wantsCustomViewTimer(false, false, false, false, true, null))
    }

    @Test
    fun overtime() {
        assertTrue(ChronometerReader.isOvertime(baseMs = 1_000L, countDown = true, nowMs = 13_000L))
        assertFalse(ChronometerReader.isOvertime(baseMs = 20_000L, countDown = true, nowMs = 13_000L))
        assertFalse(ChronometerReader.isOvertime(baseMs = 1_000L, countDown = false, nowMs = 13_000L))
        assertFalse(ChronometerReader.isOvertime(baseMs = 0L, countDown = true, nowMs = 13_000L))
    }
}
