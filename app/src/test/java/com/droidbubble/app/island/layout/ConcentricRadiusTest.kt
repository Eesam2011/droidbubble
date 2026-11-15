package com.droidbubble.app.island.layout

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** V2.3: an element at inset i has radius R − i, or its cap; art and tiles stay rounded squares. */
class ConcentricRadiusTest {

    @Test
    fun cornerNestedElementsAreConcentric() {
        for (s in listOf(0.8f, 1.1f, 1.3f)) {
            val g = CardGeometry(s)
            // An element at the roomy inset shares the card's corner centre: radius R − R/2 = R/2
            assertEquals(g.R.value / 2f, g.inner(g.insetRoomy).value, 1e-4f)
            assertEquals((g.R - g.insetTight).value, g.inner(g.insetTight).value, 1e-4f)
        }
    }

    @Test
    fun artAndTilesStayRoundedSquares() {
        for (s in listOf(0.8f, 1.1f, 1.3f)) {
            val g = CardGeometry(s)
            for (side in listOf(32.dp, 40.dp, 48.dp, 54.dp * s, 66.dp)) {
                val r = g.radiusFor(g.insetRoomy, side, side)
                assertTrue("$side at $s is a circle ($r)", r < side / 2)
                assertTrue("$side at $s: over the cap", r.value <= side.value * CardGeometry.SQUARE_CAP + 1e-4f)
                assertEquals(minOf(g.inner(g.insetRoomy), side * CardGeometry.SQUARE_CAP).value, r.value, 1e-4f)
            }
        }
    }

    @Test
    fun buttonsAndBarsMayBecomeCapsules() {
        val g = CardGeometry(1.1f)
        // A wide 44 dp button: concentric radius 21.45 is under half its height (22)
        assertEquals(21.45f, g.radiusFor(g.insetRoomy, 150.dp, 44.dp).value, 0.01f)
        // A thin bar: half its height
        assertEquals(3f, g.radiusFor(g.insetRoomy, 200.dp, 6.dp).value, 1e-4f)
    }
}
