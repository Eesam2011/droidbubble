package com.droidbubble.app.island.shell

import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** M3.1: content progress from geometry, and the content curves over it. Pure JUnit. */
class IslandChoreographyTest {

    private val closed = IslandGeometry(0.dp, 36.dp, 18.dp, 12.dp)
    private val open = IslandGeometry(374.dp, 214.5.dp, 40.dp, 64.dp)

    private fun lerp(t: Float) = IslandGeometry(
        (0f + 374f * t).dp, (36f + 178.5f * t).dp, (18f + 22f * t).dp, (12f + 52f * t).dp,
    )

    @Test
    fun progress_endpoints() {
        assertEquals(0f, contentProgress(closed, closed, open), 0f)
        assertEquals(1f, contentProgress(open, closed, open), 0f)
        assertEquals(0.5f, contentProgress(lerp(0.5f), closed, open), 0.001f)
    }

    @Test
    fun progress_isClamped_whenTheSpringOvershoots() {
        assertEquals(1f, contentProgress(lerp(1.08f), closed, open), 0f)
        assertEquals(0f, contentProgress(lerp(-0.05f), closed, open), 0f)
    }

    @Test
    fun progress_heightLeads() {
        // The drop is done but the width lags: more than half way
        val dropped = IslandGeometry(100.dp, 214.5.dp, 40.dp, 64.dp)
        assertTrue(contentProgress(dropped, closed, open) > 0.8f)
    }

    @Test
    fun progress_skipsAxesThatDontMove() {
        val sameWidth = open.copy(width = 0.dp)
        assertEquals(1f, contentProgress(sameWidth, closed, sameWidth), 0f)
        assertEquals(0f, contentProgress(closed, closed, closed), 0f)
    }

    @Test
    fun curves_haveTheirEndpoints() {
        assertEquals(0f, IslandChoreography.incomingAlpha(0f), 0f)
        assertEquals(0f, IslandChoreography.incomingAlpha(0.45f), 0f)
        assertEquals(1f, IslandChoreography.incomingAlpha(0.90f), 0f)
        assertEquals(1f, IslandChoreography.incomingAlpha(1f), 0f)
        assertEquals(0.92f, IslandChoreography.incomingScale(0f), 0f)
        assertEquals(1f, IslandChoreography.incomingScale(1f), 0f)
        assertEquals(1f, IslandChoreography.compactAlpha(0f), 0f)
        assertEquals(0f, IslandChoreography.compactAlpha(0.35f), 0f)
    }

    @Test
    fun curves_areMonotonic() {
        val ps = (0..200).map { it / 200f }
        ps.zipWithNext().forEach { (a, b) ->
            assertTrue(IslandChoreography.incomingAlpha(b) >= IslandChoreography.incomingAlpha(a))
            assertTrue(IslandChoreography.incomingScale(b) >= IslandChoreography.incomingScale(a))
            assertTrue(IslandChoreography.compactAlpha(b) <= IslandChoreography.compactAlpha(a))
        }
    }

    @Test
    fun curves_areTheSameInBothDirections() {
        // Pure functions of p: reaching p while opening or closing gives the same values
        val opening = (0..100).map { IslandChoreography.incomingAlpha(it / 100f) }
        val closing = (100 downTo 0).map { IslandChoreography.incomingAlpha(it / 100f) }.reversed()
        assertEquals(opening, closing)
    }

    @Test
    fun blur_onlyOnTheWayOut() {
        assertEquals(0f, IslandChoreography.leavingBlurFraction(0.3f, contracting = false), 0f)
        assertEquals(1f, IslandChoreography.leavingBlurFraction(0.3f, contracting = true), 0f)
        assertEquals(0f, IslandChoreography.leavingBlurFraction(1f, contracting = true), 0f)
    }
}
