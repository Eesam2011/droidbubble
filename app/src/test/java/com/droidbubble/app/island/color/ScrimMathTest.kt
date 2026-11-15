package com.droidbubble.app.island.color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** V5.3: the scrim keeps white text at 4.5:1 or better, and leaves dark covers alone. */
class ScrimMathTest {
    private fun whiteContrast(y: Float) = 1.05f / (y + 0.05f)

    @Test
    fun darkCoversKeepTheOldScrim() {
        assertEquals(0.38f, Scrim.alphaFor(0.1f, 0.75f), 1e-6f)
        assertEquals(0.38f, Scrim.alphaFor(0.24f, 0.75f), 1e-6f)
    }

    @Test
    fun brightCoversGetEnough() {
        for (y in listOf(0.3f, 0.5f, 0.7f, 0.9f, 1f)) {
            val a = Scrim.alphaFor(y, 0.75f)
            val composite = (1f - a) * 0.75f * y
            assertTrue("Y $y: contrast ${whiteContrast(composite)}", whiteContrast(composite) >= 4.5f)
        }
    }

    @Test
    fun capsAtTheMaximum() {
        assertTrue(Scrim.alphaFor(1f, 1f) <= Scrim.MAX)
    }

    @Test
    fun glassCanStayClearOnDarkArt() {
        assertEquals(0f, Scrim.alphaFor(0.2f, 0.42f, min = 0f), 1e-6f)
    }

    @Test
    fun bandsTakeTheBrightestCell() {
        val px = IntArray(64) { 0xFF000000.toInt() }
        px[3] = 0xFFFFFFFF.toInt()  // top row
        val b = Scrim.bandsOf(px)
        assertEquals(1f, b.top, 1e-3f)
        assertEquals(0f, b.bottom, 1e-3f)
    }
}
