package com.droidbubble.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** B10.4: a running clock app's custom view is read once per state change, not on every repost. */
class CustomTimerCacheTest {

    private val running = listOf("Pause", "Reset")
    private val paused = listOf("Resume", "Reset")

    /** Simulates the listener: read (inflate) only on a cache miss. */
    private class Reader(val cache: CustomTimerCache<String>) {
        var inflations = 0
        fun read(key: String, actions: List<String>, now: Long, isRunning: Boolean): String {
            cache.lookup(key, actions, now)?.let { return it }
            inflations++
            val value = "reading#$inflations"
            cache.store(key, actions, value, isRunning, now)
            return value
        }
    }

    @Test
    fun runningTimer_repostedTenTimes_inflatesOnce() {
        val reader = Reader(CustomTimerCache())
        repeat(10) { second -> reader.read("t", running, now = second * 1000L, isRunning = true) }
        assertEquals(1, reader.inflations)
    }

    @Test
    fun pausing_changesTheButtons_andIsReadAgain() {
        val reader = Reader(CustomTimerCache())
        reader.read("t", running, 0L, isRunning = true)
        reader.read("t", paused, 1000L, isRunning = false)
        assertEquals(2, reader.inflations)
        // A paused timer isn't cached as running: each repost reads it again
        reader.read("t", paused, 2000L, isRunning = false)
        assertEquals(3, reader.inflations)
    }

    @Test
    fun safetyNet_rereadsAfter30s_andRemovalClears() {
        val cache = CustomTimerCache<String>(maxAgeMs = 30_000L)
        cache.store("t", running, "a", running = true, nowMs = 0L)
        assertEquals("a", cache.lookup("t", running, 29_999L))
        assertNull(cache.lookup("t", running, 30_000L))
        cache.remove("t")
        assertNull(cache.lookup("t", running, 1L))
    }
}
