package com.droidbubble.app.island.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.LiveLayout
import org.junit.Assert.assertEquals
import org.junit.Test

/** V2.1: CardGeometry keeps LiveLayout's numbers and reproduces the concentric radius table. */
class CardGeometryTest {

    private fun eq(expected: Float, actual: Dp, msg: String = "") = assertEquals(msg, expected, actual.value, 0.01f)

    @Test
    fun matchesTheLiveLayoutFormulas() {
        for (s in listOf(0.8f, 1.0f, 1.1f, 1.3f)) for (f in listOf(1.0f, 1.5f)) {
            val g = CardGeometry(s, f)
            val rowGrowth = 1f + (f.coerceIn(1f, 1.5f) - 1f) * 0.6f
            val m = (14f * s).coerceIn(12f, 18f)
            val control = (50f * s).coerceIn(48f, 58f) * rowGrowth
            eq(m, g.m, "m at $s/$f")
            eq((10f * s).coerceIn(10f, 14f), g.gap, "gap at $s/$f")
            eq(control, g.control, "control at $s/$f")
            eq(control / 2 + m, g.R, "R at $s/$f")
            val live = LiveLayout(s, f)
            assertEquals(live.margin, g.m)
            assertEquals(live.control, g.control)
            assertEquals(live.cornerRadius, g.R)
        }
    }

    @Test
    fun radiusTableAtScale1_1() {
        val g = CardGeometry(1.1f)
        eq(15.4f, g.m); eq(55f, g.control); eq(42.9f, g.R)
        eq(21.45f, g.insetRoomy)
        // Media art 59.4 dp at the roomy inset: concentric, under the 0.42 cap (24.9)
        eq(21.45f, g.radiusFor(g.insetRoomy, 59.4.dp, 59.4.dp))
        // A 40 dp header tile: capped at 0.42 × 40
        eq(16.8f, g.radiusFor(g.insetRoomy, 40.dp, 40.dp))
        // A 44 dp tile
        eq(18.48f, g.radiusFor(g.insetRoomy, 44.dp, 44.dp))
        // A bar (long and short): may become a capsule, so the cap is half its height
        eq(21.45f, g.radiusFor(g.insetRoomy, 280.dp, 48.dp))
        eq(12f, g.radiusFor(g.insetRoomy, 280.dp, 24.dp))
        // Compact art 20 dp in a 36 dp pill, inset 8: a circle
        eq(10f, g.radiusInCapsule(36.dp, 8.dp, 20.dp))
        // One-row live card: tight inset, a control there is a circle
        eq(27.5f, g.inner(g.insetTight))
    }

    @Test
    fun innerNeverDropsBelowSix() {
        val g = CardGeometry(1.1f)
        eq(6f, g.inner(40.dp))
        eq(0f, g.radiusInCapsule(36.dp, 20.dp, 20.dp))
    }
}
