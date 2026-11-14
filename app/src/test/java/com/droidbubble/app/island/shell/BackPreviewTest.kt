package com.droidbubble.app.island.shell

import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** M4.5: the predictive-back preview: at most a 10 % shrink toward the pill, the corner growing. */
class BackPreviewTest {

    private val closed = IslandGeometry(0.dp, 36.dp, 18.dp, 12.dp)
    private val open = IslandGeometry(374.dp, 214.dp, 40.dp, 64.dp)

    @Test
    fun noProgress_isTheOpenCard() {
        assertEquals(open, backPreviewGeometry(0f, closed, open))
    }

    @Test
    fun fullProgress_isATenthOfTheWay_withALargerCorner() {
        val g = backPreviewGeometry(1f, closed, open)
        assertEquals(374f * 0.9f, g.width.value, 0.01f)
        assertEquals(214f - (214f - 36f) * 0.1f, g.height.value, 0.01f)
        assertEquals(64f - (64f - 12f) * 0.1f, g.top.value, 0.01f)
        assertEquals(40f + BackPreviewCornerGrowth.value, g.corner.value, 0.01f)
        assertEquals("clamped", g, backPreviewGeometry(3f, closed, open))
    }

    @Test
    fun velocity_pointsAlongThePreviewTowardThePill() {
        val v = backPreviewVelocity(2f, closed, open)
        assertEquals(-374f * 0.1f * 2f, v.width.value, 0.01f)
        assertTrue(v.height.value < 0f && v.top.value < 0f)
        assertEquals(0f, backPreviewVelocity(-1f, closed, open).width.value, 0.001f)
    }

    @Test
    fun speed_fromTwoSamples() {
        val s = BackSpeed()
        s.add(0.1f, 1_000_000_000L)
        assertEquals(0f, s.progressPerSec, 0.001f)
        s.add(0.3f, 1_100_000_000L)
        assertEquals(2f, s.progressPerSec, 0.01f)
        s.reset()
        assertEquals(0f, s.progressPerSec, 0.001f)
    }
}
