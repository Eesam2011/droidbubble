package com.droidbubble.app.service.window

import android.view.WindowManager.LayoutParams.MATCH_PARENT
import com.droidbubble.app.island.SettledShape
import com.droidbubble.app.island.scene.IslandMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** F5.3: grow now, shrink on settle (with a timeout). */
@OptIn(ExperimentalCoroutinesApi::class)
class ShrinkOnSettleTest {

    private val strip = WindowState(780, 180, touchable = true)
    private val full = WindowState(MATCH_PARENT, MATCH_PARENT, touchable = true)

    @Test
    fun shrinks_areDetected() {
        assertTrue(ShrinkOnSettle.isShrink(strip, WindowState.Gone))
        assertTrue(ShrinkOnSettle.isShrink(full, strip))
        assertTrue("minimise: narrower", ShrinkOnSettle.isShrink(strip, WindowState(180, 180, true)))
        assertFalse("appear: grows", ShrinkOnSettle.isShrink(WindowState.Gone, strip))
        assertFalse("expand: grows", ShrinkOnSettle.isShrink(strip, full))
        assertFalse("same size, only touchability", ShrinkOnSettle.isShrink(strip, strip.copy(touchable = false)))
        assertFalse("wider but shorter is not a shrink", ShrinkOnSettle.isShrink(strip, WindowState(900, 150, true)))
    }

    @Test
    fun hide_waitsForTheShapeToSettle() = runTest {
        val settled = MutableStateFlow<SettledShape?>(null)
        val wait = async { ShrinkOnSettle.awaitSettled(settled, IslandMode.IDLE) }
        runCurrent()
        assertFalse("still animating", wait.isCompleted)
        settled.value = SettledShape(IslandMode.COMPACT)
        runCurrent()
        assertFalse("settled in another shape (the old one)", wait.isCompleted)
        settled.value = SettledShape(IslandMode.IDLE)
        runCurrent()
        assertTrue(wait.await())
    }

    @Test
    fun timeout_shrinksAnyway() = runTest {
        val settled = MutableStateFlow<SettledShape?>(null)
        val wait = async { ShrinkOnSettle.awaitSettled(settled, IslandMode.IDLE) }
        advanceTimeBy(ShrinkOnSettle.TIMEOUT_MS + 1)
        runCurrent()
        assertTrue(wait.isCompleted)
        assertFalse("reported as a timeout", wait.await())
    }

    @Test
    fun alreadySettled_returnsAtOnce() = runTest {
        val settled = MutableStateFlow<SettledShape?>(SettledShape(IslandMode.MINIMAL))
        assertTrue(ShrinkOnSettle.awaitSettled(settled, IslandMode.MINIMAL))
    }
}
