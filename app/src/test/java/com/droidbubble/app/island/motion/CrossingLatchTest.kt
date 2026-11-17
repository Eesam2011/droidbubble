package com.droidbubble.app.island.motion

import org.junit.Assert.assertEquals
import org.junit.Test

/** D2.4: one haptic per neck snap and per merge, whatever the spring does around the lines. */
class CrossingLatchTest {

    private val snapGap = 5f

    /** Feeds [gaps] [stepMs] apart and returns the moments reported. */
    private fun moments(gaps: List<Float>, stepMs: Long = 16): List<NeckMoment> {
        val latch = CrossingLatch(snapGap)
        return gaps.mapIndexedNotNull { i, g -> latch.update(g, i * stepMs) }
    }

    @Test
    fun aSplit_snapsOnce_evenWhenTheSpringWobbles() {
        // Joined, pulling apart, overshooting to 7.5 and wobbling around 6 (the rest gap)
        val gaps = listOf(-20f, -10f, -2f, 1f, 2.4f, 2.6f, 4f, 7.5f, 5.2f, 6.4f, 5.8f, 6.1f, 6f)
        assertEquals(listOf(NeckMoment.SNAP), moments(gaps))
    }

    @Test
    fun wobblingAroundHalfTheSnapGap_isStillOneSnap() {
        assertEquals(listOf(NeckMoment.SNAP), moments(listOf(-3f, 2.6f, 2.4f, 2.6f, 2.3f, 2.7f, 6f)))
    }

    @Test
    fun aMerge_ticksOnce_whenTheyTouch() {
        val gaps = listOf(6f, 4f, 2f, 0.5f, -0.2f, 0.3f, -1f, 0.1f, -8f, -20f)
        assertEquals(listOf(NeckMoment.MERGE), moments(gaps))
    }

    @Test
    fun splitThenMerge_givesOneOfEach() {
        val gaps = listOf(-10f, 0f, 3f, 6f, 6f, 6f, 3f, 0f, -10f)
        assertEquals(listOf(NeckMoment.SNAP, NeckMoment.MERGE), moments(gaps, stepMs = 100))
    }

    @Test
    fun anIslandAlreadyAtRest_isSilent() {
        assertEquals(emptyList<NeckMoment>(), moments(listOf(6f, 6f, 6f)))
        assertEquals(emptyList<NeckMoment>(), moments(listOf(-20f, -20f)))
    }

    @Test
    fun twoMomentsWithin150ms_giveOne() {
        // Snap then merge 32 ms apart: the merge is swallowed by the throttle
        assertEquals(listOf(NeckMoment.SNAP), moments(listOf(-1f, 3f, -1f)))
    }

    @Test
    fun reset_forgetsTheState() {
        val latch = CrossingLatch(snapGap)
        latch.update(-5f, 0)
        latch.reset()
        assertEquals(null, latch.update(6f, 1_000))
        assertEquals(NeckMoment.MERGE, latch.update(-1f, 2_000))
    }
}
