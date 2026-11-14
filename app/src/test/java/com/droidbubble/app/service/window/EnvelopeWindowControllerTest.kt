package com.droidbubble.app.service.window

import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** F5.4: FLAG_WATCH_OUTSIDE_TOUCH rides on the same single window update, and only in the trial. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class EnvelopeWindowControllerTest {

    private val params = WindowManager.LayoutParams(
        1, 1,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        0,
    ).apply { gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL }

    private var sent = 0
    private val controller = OverlayWindowController(params, update = { sent++ })
    private fun watching() = params.flags and WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH != 0

    @Test
    fun matchParentNeverWatchesOutside() {
        controller.apply(WindowState(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, touchable = true))
        assertFalse(watching())
        assertFalse(controller.current.watchOutside)
    }

    @Test
    fun envelopeWatchesOutside_inOneUpdate_andCollapseClearsIt() {
        val envelope = WindowState(1080, 800, touchable = true, watchOutside = true)
        assertTrue(controller.apply(envelope))
        assertEquals(1, sent)
        assertTrue(watching())
        assertEquals(envelope, controller.current)
        assertFalse(controller.apply(envelope))

        controller.apply(WindowState(780, 180, touchable = true))
        assertEquals(2, sent)
        assertFalse(watching())
    }
}
