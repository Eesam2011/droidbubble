package com.droidbubble.app.island.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D1.1: the liquid body's maths. px, with 1 dp = 1 px here; kMax = 10. */
class LiquidSdfTest {

    private val kMax = LiquidSdf.K_MAX.value // 10
    private val h = 18f // a 36 px tall capsule

    private fun pill(cx: Float = 0f, halfW: Float = 90f) = LiquidSdf.Body(cx, 0f, halfW, h, h)

    /** A bubble [gap] px to the right of a pill whose right edge is at x = 90. */
    private fun bubble(gap: Float) = LiquidSdf.Body(90f + gap + h, 0f, h, h, h)

    @Test
    fun capsuleSignAtSamplePoints() {
        assertEquals(-18f, LiquidSdf.sdRoundBox(0f, 0f, 90f, 18f, 18f), 1e-4f)
        assertEquals(0f, LiquidSdf.sdRoundBox(90f, 0f, 90f, 18f, 18f), 1e-4f)
        assertEquals(0f, LiquidSdf.sdRoundBox(0f, 18f, 90f, 18f, 18f), 1e-4f)
        assertEquals(5f, LiquidSdf.sdRoundBox(95f, 0f, 90f, 18f, 18f), 1e-4f)
        // The round end: a point on the end circle's diagonal is on the edge
        val c = 72f + 18f * 0.70710678f
        assertEquals(0f, LiquidSdf.sdRoundBox(c, c - 72f, 90f, 18f, 18f), 1e-3f)
        assertTrue(LiquidSdf.sdRoundBox(89f, 17f, 90f, 18f, 18f) > 0f) // outside the rounded corner
    }

    @Test
    fun bridgeExactlyWhileGapIsUnderHalfK() {
        for (gap in listOf(1f, 3f, 4.9f, 5.1f, 7f, 9f)) {
            val mid = 90f + gap / 2f
            val d = LiquidSdf.d(mid, 0f, pill(), bubble(gap), kMax)
            assertEquals("gap $gap", gap < kMax / 2f, d < 0f)
            assertEquals(gap < kMax / 2f, LiquidSdf.neckPresent(gap, kMax))
        }
    }

    @Test
    fun theNeckSnapsAtFiveDp() {
        assertTrue(LiquidSdf.neckPresent(4.99f, kMax))
        assertFalse(LiquidSdf.neckPresent(5f, kMax))
        assertEquals(5f, kMax / 2f, 0f)
    }

    @Test
    fun splitKOnlyFallsAfterTheSnap() {
        var last = Float.MAX_VALUE
        var gap = 0f
        while (gap <= 2f * kMax) {
            val k = LiquidSdf.splitK(gap, kMax)
            assertTrue("k rose at gap $gap", k <= last + 1e-5f)
            if (gap <= kMax / 2f) assertEquals("full k before the snap", kMax, k, 1e-5f)
            last = k
            gap += 0.25f
        }
        assertEquals(0f, LiquidSdf.splitK(1.5f * kMax, kMax), 1e-5f)
    }

    @Test
    fun noReBridgeAsTheBubbleMovesAway() {
        // After the snap, every frame's k comes from its gap: the bridge never comes back
        var gap = kMax / 2f
        while (gap <= 2f * kMax) {
            val k = LiquidSdf.splitK(gap, kMax)
            assertFalse("bridged again at gap $gap (k $k)", LiquidSdf.neckPresent(gap, k))
            assertTrue(LiquidSdf.d(90f + gap / 2f, 0f, pill(), bubble(gap), k) >= 0f)
            gap += 0.25f
        }
    }

    @Test
    fun mergeKRisesAsTheGapCloses() {
        assertEquals(0f, LiquidSdf.mergeK(1.5f * kMax, kMax), 1e-5f)
        assertEquals(kMax, LiquidSdf.mergeK(0f, kMax), 1e-5f)
        assertTrue(LiquidSdf.mergeK(3f, kMax) > LiquidSdf.mergeK(8f, kMax))
    }

    @Test
    fun bulgeIsSymmetric() {
        val far = LiquidSdf.Body(1000f, 0f, h, h, h)
        for (dx in listOf(10f, 60f, 91f, 93f)) {
            val left = LiquidSdf.d(-dx, 4f, pill(), far, 0f, bulge = 3f)
            val right = LiquidSdf.d(dx, 4f, pill(), far, 0f, bulge = 3f)
            assertEquals("at ±$dx", left, right, 1e-4f)
        }
        // The bulge moves the pill's ends out by exactly its size
        assertEquals(0f, LiquidSdf.d(93f, 0f, pill(), far, 0f, bulge = 3f), 1e-4f)
    }
}
