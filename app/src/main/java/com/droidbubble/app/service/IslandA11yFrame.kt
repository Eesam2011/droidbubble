package com.droidbubble.app.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import com.droidbubble.app.island.IslandStateManager

/**
 * C3.8: the island window's root. It watches TalkBack's focus inside the island, so the open card
 * doesn't collapse while it's being read: TalkBack swipe navigation sends the overlay no pointer
 * events, so the touch-based hold never sees it.
 *
 * Moving between nodes sends "focus cleared" then "focused"; a clear only counts once nothing
 * inside is focused again within [CLEAR_DELAY_MS].
 */
class IslandA11yFrame @JvmOverloads constructor(
    context: Context,
    private val onFocusInside: (Boolean) -> Unit = IslandStateManager::setA11yFocusInside,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) : FrameLayout(context) {

    private val clear = Runnable { onFocusInside(false) }

    override fun onRequestSendAccessibilityEvent(child: View, event: AccessibilityEvent): Boolean {
        onAccessibilityEventType(event.eventType)
        return super.onRequestSendAccessibilityEvent(child, event)
    }

    @VisibleForTesting
    internal fun onAccessibilityEventType(type: Int) {
        when (type) {
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
                handler.removeCallbacks(clear)
                onFocusInside(true)
            }
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED -> {
                handler.removeCallbacks(clear)
                handler.postDelayed(clear, CLEAR_DELAY_MS)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(clear)
        onFocusInside(false)
    }

    companion object {
        const val CLEAR_DELAY_MS = 300L
    }
}
