package com.droidbubble.app.service.window

import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** F5.1: one window update per change, none when nothing changed. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class OverlayWindowControllerTest {

    private fun params() = WindowManager.LayoutParams(
        1, 1,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        0,
    ).apply { gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL } // as the service sets it

    private val sent = ArrayList<WindowState>()
    private fun controller(p: WindowManager.LayoutParams = params()): OverlayWindowController {
        lateinit var c: OverlayWindowController
        c = OverlayWindowController(p, update = { sent += c.current })
        return c
    }

    private val compact = WindowState(width = 780, height = 180, touchable = true)

    @Test
    fun unchangedState_sendsNothing() {
        val c = controller()
        assertFalse(c.apply(WindowState.Gone))
        assertTrue(sent.isEmpty())
        assertTrue(c.apply(compact))
        assertFalse(c.apply(compact))
        assertEquals(1, sent.size)
    }

    @Test
    fun sizeAndRefreshRate_goInOneUpdate() {
        val c = controller()
        c.apply(compact.copy(preferredRefreshRate = 120f))
        assertEquals(1, c.updates)
        assertEquals(compact.copy(preferredRefreshRate = 120f), sent.single())
    }

    @Test
    fun expandStart_isOneUpdate() {
        val c = controller()
        c.apply(compact)
        val full = WindowState(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, touchable = true, preferredRefreshRate = 120f)
        c.apply(full)
        assertEquals("one for compact, one for the expand (boost and resize together)", 2, c.updates)
        assertEquals(full, c.current)
    }

    @Test
    fun focusAlone_isOneUpdate_andFlagsFollow() {
        val c = controller()
        c.apply(compact)
        c.apply(compact.copy(focusable = true))
        assertEquals(2, c.updates)
        assertTrue(c.current.focusable)
        c.apply(compact)
        assertFalse(c.current.focusable)
        assertTrue(c.current.touchable)
    }

    @Test
    fun touchableFlag_isTheOnlyFlagChanged() {
        val p = params().apply { flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN }
        val c = controller(p)
        c.apply(compact)
        assertTrue(p.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN != 0)
        assertEquals(0, p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        c.apply(WindowState.Gone)
        assertTrue(p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertTrue(p.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN != 0)
    }

    @Test
    fun aFailingUpdate_doesNotThrow() {
        val c = OverlayWindowController(params(), update = { error("window gone") })
        assertTrue(c.apply(compact))
        assertEquals(1, c.updates)
    }
}
