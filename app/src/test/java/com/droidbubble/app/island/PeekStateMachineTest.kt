package com.droidbubble.app.island

import com.droidbubble.app.model.IslandPriority
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * D2.2: new message banners peek open into their card, then recede. The policy is pure; the
 * state part runs on [IslandStateManager] (the recede itself is the shell's auto-collapse loop,
 * covered by BannerPeekTest).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PeekStateMachineTest {

    private val scheduler = TestCoroutineScheduler()

    private fun msg(key: String = "m", pkg: String = "com.whatsapp") = NotificationInfo(
        key = key, packageName = pkg, appName = "WhatsApp", title = "Sam", text = "Hi $key",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        IslandStateManager.resetForTest()
        IslandStateManager.nowMs = { scheduler.currentTime }
        IslandStateManager.peekBanners = true
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

    // ---- the pure policy ----

    private fun peeks(
        n: NotificationInfo = msg(), isNew: Boolean = true, now: Long = 10_000, last: Long? = null,
        locked: Boolean = false, talkBack: Boolean = false, expanded: Boolean = false,
    ) = PeekPolicy.shouldPeek(n, isNew, now, last, locked, talkBack, expanded)

    @Test
    fun aNewMessage_peeks() = assertTrue(peeks())

    @Test
    fun updatesAndOtherKinds_dontPeek() {
        assertFalse(peeks(isNew = false))
        assertFalse(peeks(msg().copy(isCall = true)))
        assertFalse(peeks(msg().copy(isIncomingCall = true)))
        assertFalse(peeks(msg().copy(isAlert = true)))
        assertFalse(peeks(msg().copy(isOngoing = true)))
    }

    @Test
    fun lockedTalkBackOrOpenCard_dontPeek() {
        assertFalse(peeks(locked = true))
        assertFalse(peeks(talkBack = true))
        assertFalse(peeks(expanded = true))
    }

    @Test
    fun cooldown_isFiveSecondsPerApp() {
        assertFalse(peeks(now = 14_999, last = 10_000))
        assertTrue(peeks(now = 15_000, last = 10_000))
    }

    // ---- on the island ----

    @Test
    fun newMessage_opensItsCardAsAPeek() {
        IslandStateManager.updateNotification(msg())
        assertTrue(IslandStateManager.isExpanded.value)
        assertEquals(IslandPriority.POP_NOTIFICATION, IslandStateManager.expandedPriority.value)
        assertTrue(IslandStateManager.isPeeking.value)
    }

    @Test
    fun offByDefault_soTheIslandStaysCompact() {
        IslandStateManager.peekBanners = false
        IslandStateManager.updateNotification(msg())
        assertFalse(IslandStateManager.isExpanded.value)
        assertFalse(IslandStateManager.isPeeking.value)
    }

    @Test
    fun aBurstFromOneApp_peeksOnce() {
        IslandStateManager.updateNotification(msg("a"))
        IslandStateManager.collapse()
        advance(1_000)
        IslandStateManager.updateNotification(msg("b"))
        assertFalse("within 5 s of the last peek", IslandStateManager.isExpanded.value)
        advance(4_000)
        IslandStateManager.updateNotification(msg("c"))
        assertTrue("5 s later it peeks again", IslandStateManager.isPeeking.value)
    }

    @Test
    fun anotherApp_peeksDuringTheCooldown() {
        IslandStateManager.updateNotification(msg("a"))
        IslandStateManager.collapse()
        IslandStateManager.updateNotification(msg("b", pkg = "org.telegram.messenger"))
        assertTrue(IslandStateManager.isPeeking.value)
    }

    @Test
    fun locked_orTalkBack_dontPeek() {
        IslandStateManager.setDeviceLocked(true)
        IslandStateManager.updateNotification(msg("a"))
        assertFalse(IslandStateManager.isExpanded.value)
        IslandStateManager.setDeviceLocked(false)
        IslandStateManager.removeNotification("a")
        IslandStateManager.touchExplorationOn = true
        IslandStateManager.updateNotification(msg("b"))
        assertFalse(IslandStateManager.isExpanded.value)
    }

    @Test
    fun aTouch_keepsItOpenAsANormalCard() {
        IslandStateManager.updateNotification(msg())
        IslandStateManager.cancelPeek()
        assertFalse(IslandStateManager.isPeeking.value)
        assertTrue(IslandStateManager.isExpanded.value)
    }

    @Test
    fun theUserOpeningACard_isNotAPeek() {
        IslandStateManager.updateNotification(msg())
        IslandStateManager.expand(IslandPriority.POP_NOTIFICATION)
        assertFalse(IslandStateManager.isPeeking.value)
    }

    @Test
    fun afterReceding_theMessageLeavesOnTheAutoDismissTimer() {
        IslandStateManager.autoDismissNotifications = true
        IslandStateManager.updateNotification(msg())
        advance(IslandStateManager.timings.notificationAuto + 500)
        assertEquals("open card: not dismissed", "m", IslandStateManager.activeNotification.value?.key)
        IslandStateManager.collapse() // the peek recedes
        advance(IslandStateManager.timings.notificationAuto + 100)
        assertNull(IslandStateManager.activeNotification.value)
    }
}
