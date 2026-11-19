package com.droidbubble.app.service

import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** C3.8: the frame turns TalkBack focus events into "focus inside the island". */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class IslandA11yFrameTest {

    private val seen = mutableListOf<Boolean>()
    private val frame = IslandA11yFrame(ApplicationProvider.getApplicationContext(), { seen += it }, Handler(Looper.getMainLooper()))
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    fun focusedThenCleared_isInsideThenOutsideAfterTheDelay() {
        frame.onAccessibilityEventType(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
        assertEquals(listOf(true), seen)
        frame.onAccessibilityEventType(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED)
        idle(200)
        assertEquals("not yet", listOf(true), seen)
        idle(200)
        assertEquals(listOf(true, false), seen)
    }

    @Test
    fun movingBetweenNodes_staysInside() {
        frame.onAccessibilityEventType(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
        // TalkBack moves to the next node: cleared, then focused within 300 ms
        frame.onAccessibilityEventType(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED)
        idle(50)
        frame.onAccessibilityEventType(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
        idle(1_000)
        assertEquals("never reported outside", listOf(true, true), seen)
    }

    @Test
    fun otherEvents_areIgnored() {
        frame.onAccessibilityEventType(AccessibilityEvent.TYPE_VIEW_CLICKED)
        idle(1_000)
        assertEquals(emptyList<Boolean>(), seen)
    }
}
