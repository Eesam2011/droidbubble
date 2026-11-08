package com.droidbubble.app.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The morph speed that blur and stretch follow: high mid-morph, zero once settled. */
class MorphSpeedMeterTest {

    private val frame = 16_666_667L

    @Test
    fun fastGrowthReadsAsFastAndGrowing() {
        val m = MorphSpeedMeter()
        m.update(size = 200f, target = 560f, nowNanos = 0L, fullSpeed = 1800f)
        var s = m.update(size = 230f, target = 560f, nowNanos = frame, fullSpeed = 1800f)
        s = m.update(size = 260f, target = 560f, nowNanos = frame * 2, fullSpeed = 1800f)
        // 30dp a frame = 1800dp/s = full speed, smoothed up over two frames
        assertTrue(s.speed > 0.7f)
        assertTrue(s.growing)
    }

    @Test
    fun settledShapeHasNoSpeedLeft() {
        val m = MorphSpeedMeter()
        m.update(size = 500f, target = 560f, nowNanos = 0L, fullSpeed = 1800f)
        m.update(size = 540f, target = 560f, nowNanos = frame, fullSpeed = 1800f)
        val s = m.update(size = 559.8f, target = 560f, nowNanos = frame * 2, fullSpeed = 1800f)
        assertEquals(0f, s.speed, 0f)
    }

    @Test
    fun shrinkingIsNotGrowing() {
        val m = MorphSpeedMeter()
        m.update(size = 560f, target = 200f, nowNanos = 0L, fullSpeed = 1800f)
        val s = m.update(size = 520f, target = 200f, nowNanos = frame, fullSpeed = 1800f)
        assertFalse(s.growing)
        assertTrue(s.speed > 0f)
    }

    @Test
    fun aLongPauseIsNotReadAsSpeed() {
        val m = MorphSpeedMeter()
        m.update(size = 200f, target = 560f, nowNanos = 0L, fullSpeed = 1800f)
        // First frame of a new morph, seconds after the last one
        val s = m.update(size = 210f, target = 560f, nowNanos = 3_000_000_000L, fullSpeed = 1800f)
        assertEquals(0f, s.speed, 0f)
    }
}
