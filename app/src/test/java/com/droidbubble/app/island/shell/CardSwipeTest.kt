package com.droidbubble.app.island.shell

import androidx.compose.ui.unit.Velocity
import com.droidbubble.app.island.gesture.IslandGestureResult
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.NotificationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** M4.2: what each kind of card does with a sideways swipe, and how far it follows the finger. */
class CardSwipeTest {

    private fun notif(call: Boolean = false, alarm: Boolean = false) =
        NotificationInfo(key = "k", packageName = "p", appName = "A", title = "T", text = "x", isIncomingCall = call, isAlert = alarm)

    @Test
    fun kinds() {
        assertEquals(CardSwipeKind.DISMISS, cardSwipeKind(IslandPriority.MEDIA, null))
        assertEquals(CardSwipeKind.DISMISS, cardSwipeKind(IslandPriority.PROGRESS, null))
        assertEquals(CardSwipeKind.DISMISS, cardSwipeKind(IslandPriority.POP_NOTIFICATION, notif()))
        assertEquals(CardSwipeKind.TUCK, cardSwipeKind(IslandPriority.POP_NOTIFICATION, notif(call = true)))
        assertEquals(CardSwipeKind.TUCK, cardSwipeKind(IslandPriority.POP_NOTIFICATION, notif(alarm = true)))
        assertEquals(CardSwipeKind.INERT, cardSwipeKind(IslandPriority.SCREEN_RECORDING, null))
        assertEquals(CardSwipeKind.INERT, cardSwipeKind(IslandPriority.FLASHLIGHT, null))
        assertEquals(CardSwipeKind.INERT, cardSwipeKind(IslandPriority.CALL, null))
        assertEquals(CardSwipeKind.INERT, cardSwipeKind(null, null))
    }

    @Test
    fun dragX_oneToOne_banded_orResisted() {
        assertEquals(200f, cardDragX(200f, CardSwipeKind.DISMISS, IslandPriority.MEDIA, 120f), 0.001f)
        assertEquals(200f * RECORDING_DRAG_RESISTANCE, cardDragX(200f, CardSwipeKind.INERT, IslandPriority.SCREEN_RECORDING, 120f), 0.001f)
        val banded = cardDragX(200f, CardSwipeKind.TUCK, IslandPriority.POP_NOTIFICATION, 120f)
        assertTrue("banded ($banded) follows less than the finger", banded in 1f..199f)
        assertTrue("and never past the band length", cardDragX(10_000f, CardSwipeKind.INERT, IslandPriority.FLASHLIGHT, 120f) < 120f)
        assertEquals(-banded, cardDragX(-200f, CardSwipeKind.TUCK, IslandPriority.POP_NOTIFICATION, 120f), 0.001f)
    }

    @Test
    fun dragVelocity_matchesTheBandsSlope() {
        assertEquals(900f, cardDragVelocity(50f, 900f, CardSwipeKind.DISMISS, IslandPriority.MEDIA, 120f), 0.001f)
        // At 120 px into a 120 px band the card moves at a quarter of the finger's speed
        assertEquals(225f, cardDragVelocity(120f, 900f, CardSwipeKind.INERT, IslandPriority.FLASHLIGHT, 120f), 0.001f)
    }

    @Test
    fun releases() {
        val fling = IslandGestureResult.Dismiss(direction = 1, velocityX = 2000f)
        assertEquals(CardRelease.FlingOut(1, 2000f), cardReleaseFor(fling, CardSwipeKind.DISMISS))
        assertEquals(CardRelease.TuckBack(2000f), cardReleaseFor(fling, CardSwipeKind.TUCK))
        assertEquals(CardRelease.SpringBack(2000f), cardReleaseFor(fling, CardSwipeKind.INERT))
        val tuck = IslandGestureResult.TuckInstead(-1500f)
        assertEquals(CardRelease.TuckBack(-1500f), cardReleaseFor(tuck, CardSwipeKind.TUCK))
        assertEquals(CardRelease.SpringBack(-1500f), cardReleaseFor(tuck, CardSwipeKind.INERT))
        val cancel = IslandGestureResult.Cancel(Velocity(300f, 0f))
        assertEquals(CardRelease.SpringBack(300f), cardReleaseFor(cancel, CardSwipeKind.DISMISS))
    }
}
