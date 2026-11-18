package com.droidbubble.app.island.activity

import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.NotificationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** A1.4: the old single slots as projections of the store; no live activity is lost any more. */
@OptIn(ExperimentalCoroutinesApi::class)
class LegacyProjectionTest {

    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        IslandStateManager.resetForTest()
    }

    @After
    fun tearDown() {
        IslandStateManager.resetForTest()
        Dispatchers.resetMain()
    }

    private val ongoingCall = NotificationInfo(
        key = "call", packageName = "com.whatsapp", appName = "WhatsApp", title = "Sam", text = "Ongoing call",
        isCall = true, isOngoing = true, liveTimerBaseMs = 1L,
    )
    private val ringing = ongoingCall.copy(key = "ring", isIncomingCall = true, liveTimerBaseMs = 0L, text = "Incoming call")
    private fun timer(key: String = "timer") = NotificationInfo(
        key = key, packageName = "com.google.android.deskclock", appName = "Clock", title = "Timer", text = "",
        isOngoing = true, liveTimerBaseMs = 10_000L, liveTimerCountDown = true,
    )
    private fun message(key: String = "msg", text: String = "Hi") = NotificationInfo(
        key = key, packageName = "org.telegram.messenger", appName = "Telegram", title = "Ana", text = text,
    )
    private fun download(key: String) = NotificationInfo(
        key = key, packageName = "com.android.vending", appName = "Play", title = "Downloading $key", text = "",
        progress = 10, maxProgress = 100,
    )

    private val shown get() = IslandStateManager.activeNotification.value?.key
    private val progress get() = IslandStateManager.activeProgress.value?.key

    // ---- the four rules, through IslandStateManager ----

    @Test
    fun rule1_ringingCallIsNotCovered() {
        IslandStateManager.updateNotification(ringing)
        IslandStateManager.updateNotification(message())
        assertEquals("ring", shown)
        // The message is in the store all the same
        assertEquals(setOf("n:ring", "n:msg"), IslandStateManager.store.activities.value.keys.map { it.value }.toSet())
    }

    @Test
    fun rule2_oneOffAlertCoversLive_liveRepostWaits() {
        IslandStateManager.updateNotification(timer())
        IslandStateManager.updateNotification(message())
        assertEquals("msg", shown)
        IslandStateManager.updateNotification(timer()) // a tick behind the message
        assertEquals("msg", shown)
    }

    @Test
    fun rule3_thirdActivityIsNotLost() {
        // Ongoing call → timer → message; dismiss the message: the timer, then the call
        IslandStateManager.updateNotification(ongoingCall)
        IslandStateManager.updateNotification(timer())
        IslandStateManager.updateNotification(message())
        assertEquals("msg", shown)
        IslandStateManager.dismissCurrentNotification()
        assertEquals("timer", shown)
        IslandStateManager.removeNotification("timer")
        assertEquals("call", shown)
    }

    @Test
    fun rule3_secondTimerAfterAMessage_bothReachable() {
        IslandStateManager.updateNotification(timer("t1"))
        IslandStateManager.updateNotification(message())
        IslandStateManager.updateNotification(timer("t2"))
        assertEquals("msg", shown)
        IslandStateManager.dismissCurrentNotification()
        assertEquals("t2", shown)
        IslandStateManager.removeNotification("t2")
        assertEquals("t1", shown)
    }

    @Test
    fun rule3_aMessageDoesNotComeBack() {
        IslandStateManager.updateNotification(message("m1"))
        IslandStateManager.updateNotification(message("m2", "Yo"))
        IslandStateManager.dismissCurrentNotification()
        assertNull("one-off cards don't return, as before", shown)
    }

    @Test
    fun rule4_twoDownloads_removingTheNewerShowsTheOlder() {
        IslandStateManager.updateNotification(download("d1"))
        IslandStateManager.updateNotification(download("d2"))
        assertEquals("d2", progress)
        IslandStateManager.removeNotification("d2")
        assertEquals("d1", progress)
    }

    @Test
    fun dismissedDownload_staysGoneWhileItTicks() {
        IslandStateManager.updateNotification(download("d1"))
        IslandStateManager.dismissProgress()
        IslandStateManager.updateNotification(download("d1").copy(progress = 50))
        assertNull(progress)
        IslandStateManager.removeNotification("d1")
        IslandStateManager.updateNotification(download("d1"))
        assertEquals("d1", progress)
    }

    @Test
    fun ringingScansTheStore() {
        IslandStateManager.updateNotification(ringing)
        assertEquals(true, IslandStateManager.isIncomingCallRinging)
        IslandStateManager.updateNotification(ringing.copy(isIncomingCall = false, liveTimerBaseMs = 1L))
        assertEquals(false, IslandStateManager.isIncomingCallRinging)
    }

    // ---- the pure function ----

    @Test
    fun pure_keepsTheShownEntryWhenSomethingElseChanges() {
        val a = LegacySpecMapper.fromNotification(message("a"))
        val b = LegacySpecMapper.fromNotification(timer("b"))
        val slots = projectLegacySlots(listOf(a, b), LegacySlots(a, null), changed = null)
        assertEquals(a.id, slots.notification?.id)
        assertEquals(b.id, projectLegacySlots(listOf(b), LegacySlots(a, null)).notification?.id)
        assertNull(projectLegacySlots(emptyList(), LegacySlots(a, null)).notification)
    }
}
