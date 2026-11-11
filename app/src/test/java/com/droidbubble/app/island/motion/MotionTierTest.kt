package com.droidbubble.app.island.motion

import org.junit.Assert.assertEquals
import org.junit.Test

/** M5.1: which tier the island runs at. */
class MotionTierTest {

    @Test
    fun table() {
        data class Row(val setting: Boolean, val scale: Float, val saver: Boolean, val tier: MotionTier)
        val rows = listOf(
            Row(false, 1f, false, MotionTier.FULL),
            Row(false, 1.5f, false, MotionTier.FULL),
            Row(false, 1f, true, MotionTier.EFFICIENT),
            Row(true, 1f, false, MotionTier.REDUCED),
            Row(true, 1f, true, MotionTier.REDUCED),
            Row(false, 0f, false, MotionTier.REDUCED),     // "Remove animations"
            Row(false, 0.5f, false, MotionTier.REDUCED),   // turned down
            Row(false, 0f, true, MotionTier.REDUCED),      // reduced wins over Battery Saver
        )
        for (r in rows) {
            assertEquals("$r", r.tier, resolveMotionTier(r.setting, r.scale, r.saver))
        }
    }
}
