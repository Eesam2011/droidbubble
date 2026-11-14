package com.droidbubble.app.service.window

import android.content.Context
import android.view.KeyEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.shell.IslandBackGesture
import com.droidbubble.app.service.overlayFocusable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * M4.5: Back collapses the expanded island, through the key path (API 29–32, and whenever the
 * platform still sends KEYCODE_BACK), and the window takes focus only while that can happen.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class OverlayBackTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandBackGesture.resetForTest()
    }

    @After
    fun tearDown() {
        IslandBackGesture.resetForTest()
        IslandStateManager.resetForTest()
    }

    @Test
    fun focusable_onlyWhileExpandedWithTheSetting_orTyping() {
        assertFalse("collapsed", overlayFocusable(keyboardFocused = false, expanded = false, backCollapses = true))
        assertTrue("expanded with Back collapses", overlayFocusable(keyboardFocused = false, expanded = true, backCollapses = true))
        assertFalse("setting off: today's window", overlayFocusable(keyboardFocused = false, expanded = true, backCollapses = false))
        assertTrue("typing a reply, as before", overlayFocusable(keyboardFocused = true, expanded = true, backCollapses = false))
    }

    @Test
    fun backKey_table() {
        var commits = 0
        val commit = { commits++; Unit }
        assertTrue(handleBackKey(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_DOWN, canceled = false, active = true, commit = commit))
        assertEquals("the down only claims the key", 0, commits)
        assertTrue(handleBackKey(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_UP, canceled = false, active = true, commit = commit))
        assertEquals(1, commits)
        assertTrue("a cancelled up is still ours, but does nothing", handleBackKey(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_UP, canceled = true, active = true, commit = commit))
        assertEquals(1, commits)
        assertFalse("inactive: the key is not ours", handleBackKey(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_UP, canceled = false, active = false, commit = commit))
        assertFalse("other keys pass", handleBackKey(KeyEvent.KEYCODE_ENTER, KeyEvent.ACTION_UP, canceled = false, active = true, commit = commit))
        assertEquals(1, commits)
    }

    @Test
    fun backKey_collapsesTheExpandedIsland() {
        IslandStateManager.expand(null)
        val handler = OverlayBackHandler(View(context), active = { IslandStateManager.isExpanded.value })
        handler.attach()
        assertTrue(handler.onKey(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)))
        assertTrue("not on the down", IslandStateManager.isExpanded.value)
        assertTrue(handler.onKey(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)))
        assertFalse(IslandStateManager.isExpanded.value)
        // Collapsed: Back belongs to the app again
        assertFalse(handler.onKey(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)))
        handler.detach()
    }

    @Test
    fun commit_handsTheSpeedOverBeforeCollapsing_onlyWhenExpanded() {
        var handedOver = 0
        IslandBackGesture.beforeCollapse = { handedOver++ }
        IslandBackGesture.onInvoked()
        assertEquals("nothing to collapse", 0, handedOver)
        IslandStateManager.expand(null)
        IslandBackGesture.onStarted()
        IslandBackGesture.onProgressed(0.4f)
        IslandBackGesture.onInvoked()
        assertEquals(1, handedOver)
        assertFalse(IslandStateManager.isExpanded.value)
    }
}
