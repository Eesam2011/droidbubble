package com.droidbubble.app.service

import com.droidbubble.app.model.LiveTimerBaseStabilizer
import com.droidbubble.app.island.formatTimerText
import org.junit.Assert.assertEquals
import org.junit.Test

/** The island clock keeps one start time per timer, however the app's updates wander. */
class LiveTimerBaseStabilizerTest {

    private val start = 1_000_000L

    /** Start time guessed from a recorder posting a whole-second title at [postAt]. */
    private fun guess(postAt: Long): Long = postAt - (postAt - start) / 1000 * 1000

    @Test
    fun textTimersNeverStepBack() {
        val s = LiveTimerBaseStabilizer()
        // Posts land at uneven points inside each second, as they do on a real phone
        val posts = listOf(5_900L, 6_050L, 7_700L, 8_020L, 9_480L, 10_010L).map { start + it }
        var lastShown = -1L
        var base = 0L
        for (post in posts) {
            base = s.stabilize("rec", guess(post), fromText = true)
            // What the island shows 300ms after each update, then just before the next second
            for (t in listOf(post + 300, post + 990)) {
                val shown = (t - base) / 1000
                assert(shown >= lastShown) { "clock went back from $lastShown to $shown" }
                lastShown = shown
            }
        }
        // The kept guess is the earliest one, within one post's delay of the true start
        assertEquals(start + 10, base)
    }

    @Test
    fun realChangesAreTakenAsIs() {
        val s = LiveTimerBaseStabilizer()
        assertEquals(start, s.stabilize("t", start, fromText = false))
        // +1:00 on a countdown
        assertEquals(start + 60_000, s.stabilize("t", start + 60_000, fromText = false))
        // A few ms of clock-read noise on a chronometer base is absorbed
        assertEquals(start + 60_000, s.stabilize("t", start + 60_012, fromText = false))
        // Pause forgets the timer; resume starts over from the new base
        assertEquals(0L, s.stabilize("t", 0L, fromText = false))
        assertEquals(start + 5, s.stabilize("t", start + 5, fromText = false))
    }

    @Test
    fun formatIsUnchanged() {
        assertEquals("00:05", formatTimerText(5_999))
    }
}
