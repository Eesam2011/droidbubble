package com.droidbubble.app.island.shell

import androidx.compose.ui.unit.Velocity
import com.droidbubble.app.island.gesture.Axis
import com.droidbubble.app.island.gesture.IslandGestureResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** M4.4: what the pill does with the resolver's verdict, given what it can do right now. */
class PillReleaseTest {

    @Test
    fun expand_onlyWhenThePillCanOpen() {
        assertEquals(PillRelease.Expand(900f), pillReleaseFor(IslandGestureResult.Expand(900f), canExpand = true, canMinimise = true, canRestore = false))
        assertEquals(PillRelease.SpringBack, pillReleaseFor(IslandGestureResult.Expand(900f), canExpand = false, canMinimise = true, canRestore = false))
    }

    @Test
    fun shade_evenOnAnIdlePill_andItsFallback() {
        assertEquals(PillRelease.OpenShade, pillReleaseFor(IslandGestureResult.OpenShade, canExpand = false, canMinimise = true, canRestore = false))
        assertEquals(PillRelease.Expand(0f), shadeFallback(canExpand = true))
        assertEquals(PillRelease.SpringBack, shadeFallback(canExpand = false))
    }

    @Test
    fun minimiseAndRestore() {
        assertEquals(PillRelease.Minimise, pillReleaseFor(IslandGestureResult.Minimise, canExpand = true, canMinimise = true, canRestore = false))
        assertEquals(PillRelease.SpringBack, pillReleaseFor(IslandGestureResult.Minimise, canExpand = true, canMinimise = false, canRestore = false))
        assertEquals(PillRelease.Restore, pillReleaseFor(IslandGestureResult.Restore, canExpand = false, canMinimise = false, canRestore = true))
        assertEquals(PillRelease.SpringBack, pillReleaseFor(IslandGestureResult.Restore, canExpand = false, canMinimise = false, canRestore = false))
    }

    @Test
    fun cancelAndCardResults_springBack() {
        assertEquals(PillRelease.SpringBack, pillReleaseFor(IslandGestureResult.Cancel(Velocity(10f, 0f)), true, true, false))
        assertEquals(PillRelease.SpringBack, pillReleaseFor(IslandGestureResult.Tuck(-500f), true, true, false))
    }

    @Test
    fun dragOffset_isBanded_andGivesLessUpward() {
        val down = pillDragOffset(60f, Axis.VERTICAL, 120f)
        val up = pillDragOffset(-60f, Axis.VERTICAL, 120f)
        assertTrue(down in 1f..59f)
        assertTrue("up ($up) gives less than down ($down)", -up < down)
        assertEquals(-pillDragOffset(60f, Axis.HORIZONTAL, 120f), pillDragOffset(-60f, Axis.HORIZONTAL, 120f), 0.001f)
    }
}
