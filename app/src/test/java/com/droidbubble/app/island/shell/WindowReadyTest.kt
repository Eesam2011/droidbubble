package com.droidbubble.app.island.shell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** F5.2: a bigger shape waits for the window, but never longer than the timeout. */
class WindowReadyTest {

    @Test
    fun nothingWanted_isReady() {
        assertTrue(isWindowReady(want = false, fits = false, waitedMs = 0))
    }

    @Test
    fun windowAlreadyFits_isReadyAtOnce() {
        assertTrue(isWindowReady(want = true, fits = true, waitedMs = 0))
    }

    @Test
    fun windowTooSmall_waits_untilTheTimeout() {
        assertFalse(isWindowReady(want = true, fits = false, waitedMs = 0))
        assertFalse(isWindowReady(want = true, fits = false, waitedMs = WINDOW_READY_TIMEOUT_MS - 1))
        assertTrue("a device that never resizes must not block the expand", isWindowReady(want = true, fits = false, waitedMs = WINDOW_READY_TIMEOUT_MS))
    }

    @Test
    fun timeout_isAtMostAFewFrames() {
        assertTrue(WINDOW_READY_TIMEOUT_MS <= 100L)
    }
}
