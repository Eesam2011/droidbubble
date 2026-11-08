package com.droidbubble.app.island

import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Card lifetime, call protection and flashlight flicker rules of [IslandStateManager]. */
@OptIn(ExperimentalCoroutinesApi::class)
class IslandStateBehaviorTest {

    private val scheduler = TestCoroutineScheduler()

    private fun msg(key: String = "m", autoCancel: Boolean = false) = NotificationInfo(
        key = key, packageName = "com.whatsapp", appName = "WhatsApp", title = "Sam", text = "Hi",
        isAutoCancel = autoCancel,
        actions = listOf(NotificationAction("Mark as read", null))
    )

    private val ringing = NotificationInfo(
        key = "call", packageName = "com.google.android.dialer", appName = "Phone", title = "Essam",
        text = "Incoming call", isCall = true, isIncomingCall = true, isOngoing = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        IslandStateManager.resetForTest()
        IslandStateManager.nowMs = { scheduler.currentTime }
    }

    @After
    fun tearDown() {
        IslandStateManager.resetForTest()
        IslandStateManager.nowMs = { System.nanoTime() / 1_000_000L }
        Dispatchers.resetMain()
    }

    private fun advance(ms: Long) {
        scheduler.advanceTimeBy(ms)
        scheduler.runCurrent()
    }

    // --- Cards stay until swiped away or cleared from the shade -------------------------------

    @Test
    fun openingTheApp_keepsTheCard() {
        IslandStateManager.updateNotification(msg())
        IslandStateManager.setForegroundPackage("com.whatsapp")
        IslandStateManager.setForegroundPackage("com.android.launcher")
        assertEquals("m", IslandStateManager.activeNotification.value?.key)
    }

    @Test
    fun actionButton_keepsTheCard_untilTheAppClearsIt() {
        IslandStateManager.updateNotification(msg())
        IslandStateManager.expand(IslandPriority.POP_NOTIFICATION)
        IslandStateManager.onNotificationActionPressed()
        assertNotNull(IslandStateManager.activeNotification.value)
        IslandStateManager.removeNotification("m") // app / shade cleared it
        assertNull(IslandStateManager.activeNotification.value)
    }

    @Test
    fun openingFromIsland_keepsNonAutoCancelCard_butClearsAutoCancel() {
        var cancelled: String? = null
        IslandStateManager.onCancelSystemNotification = { cancelled = it }
        IslandStateManager.updateNotification(msg())
        IslandStateManager.onNotificationOpened()
        assertNotNull(IslandStateManager.activeNotification.value)
        assertNull(cancelled)

        IslandStateManager.updateNotification(msg("auto", autoCancel = true))
        IslandStateManager.onNotificationOpened()
        assertEquals("auto", cancelled)
        IslandStateManager.onCancelSystemNotification = null
    }

    @Test
    fun swipeAway_clearsCardAndShade() {
        var cancelled: String? = null
        IslandStateManager.onCancelSystemNotification = { cancelled = it }
        IslandStateManager.updateNotification(msg())
        assertTrue(IslandStateManager.clearCurrentNotification())
        assertNull(IslandStateManager.activeNotification.value)
        assertEquals("m", cancelled)
        IslandStateManager.onCancelSystemNotification = null
    }

    // --- Swipe gestures on the pill -------------------------------------------------------------

    @Test
    fun repeatedSwipes_areIdempotent() {
        IslandStateManager.updateNotification(msg())
        IslandStateManager.minimizeIsland()
        IslandStateManager.minimizeIsland()
        assertTrue(IslandStateManager.userMinimized.value)
        assertFalse(IslandStateManager.userHidden.value)
        IslandStateManager.hideIsland()
        IslandStateManager.hideIsland()
        assertTrue(IslandStateManager.userHidden.value)
        assertFalse(IslandStateManager.userMinimized.value)
        IslandStateManager.restoreIsland()
        assertFalse(IslandStateManager.userHidden.value || IslandStateManager.userMinimized.value)
    }

    // --- Calls ---------------------------------------------------------------------------------

    @Test
    fun ringingCall_opensCardAndIsNotCoveredByOtherAlerts() {
        IslandStateManager.updateNotification(ringing)
        assertTrue(IslandStateManager.isExpanded.value)
        IslandStateManager.updateNotification(msg())
        IslandStateManager.triggerRingerModeEvent(IslandStateManager.RingerMode.VIBRATE)
        IslandStateManager.triggerUnlockEvent()
        IslandStateManager.triggerChargingEvent(50)
        assertEquals("call", IslandStateManager.activeNotification.value?.key)
        assertTrue(IslandStateManager.isExpanded.value)
        assertNull(IslandStateManager.activeRingerMode.value)
        assertNull(IslandStateManager.activeCharging.value)
    }

    @Test
    fun answeredCall_letsAlertsThroughAgain() {
        IslandStateManager.updateNotification(ringing)
        IslandStateManager.updateNotification(ringing.copy(isIncomingCall = false, liveTimerBaseMs = 1L))
        IslandStateManager.updateNotification(msg())
        assertEquals("m", IslandStateManager.activeNotification.value?.key)
        // The call waits behind the alert and comes back when it is dismissed
        IslandStateManager.dismissCurrentNotification()
        assertEquals("call", IslandStateManager.activeNotification.value?.key)
    }

    // --- Flashlight flicker filter ------------------------------------------------------------

    @Test
    fun flashlight_steadyTorch_showsAfterShortDelay() {
        IslandStateManager.onTorchChanged(true)
        assertFalse(IslandStateManager.isFlashlightOn.value)
        advance(torchShowDelayMs(1) + 10)
        assertTrue(IslandStateManager.isFlashlightOn.value)
        IslandStateManager.onTorchChanged(false)
        assertFalse(IslandStateManager.isFlashlightOn.value)
    }

    @Test
    fun flashlight_callFlashBlinking_neverShowsPill() {
        IslandStateManager.updateNotification(ringing)
        repeat(30) { i ->
            IslandStateManager.onTorchChanged(i % 2 == 0)
            advance(250)
            assertFalse("blink $i showed the flashlight", IslandStateManager.isFlashlightOn.value)
        }
    }

    @Test
    fun flashlight_notificationFlashBlinking_withoutCall_neverShowsPill() {
        repeat(8) { i ->
            IslandStateManager.onTorchChanged(i % 2 == 0)
            advance(300)
            assertFalse(IslandStateManager.isFlashlightOn.value)
        }
    }

    @Test
    fun flashlight_turnedOnDuringCall_showsOnceRingingStops() {
        IslandStateManager.updateNotification(ringing)
        IslandStateManager.onTorchChanged(true)
        advance(5_000)
        assertFalse(IslandStateManager.isFlashlightOn.value)
        IslandStateManager.removeNotification("call")
        advance(torchShowDelayMs(1) + 10)
        assertTrue(IslandStateManager.isFlashlightOn.value)
    }

    @Test
    fun torchDelay_isLongerWhileBlinking() {
        assertTrue(torchShowDelayMs(3) > torchShowDelayMs(1))
    }

    // --- Settings ------------------------------------------------------------------------------

    @Test
    fun islandStyle_defaultsToPill_andNormalizesUnknownValues() {
        assertEquals(IslandSettings.STYLE_PILL, IslandSettings().islandStyle)
        assertFalse(IslandSettings().isNotch)
        assertTrue(IslandSettings(islandStyle = IslandSettings.STYLE_NOTCH).isNotch)
        assertEquals(IslandSettings.STYLE_PILL, IslandSettings.normalizeStyle("garbage"))
        assertEquals(IslandSettings.STYLE_PILL, IslandSettings.normalizeStyle(null))
        assertEquals(IslandSettings.STYLE_NOTCH, IslandSettings.normalizeStyle("NOTCH"))
    }
}
