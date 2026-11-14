package com.droidbubble.app.island.icon

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
import org.junit.Before
import org.junit.Test

/** I3.1: each counter goes up exactly once per real event. */
@OptIn(ExperimentalCoroutinesApi::class)
class IconSignalsTest {

    private val scheduler = TestCoroutineScheduler()
    private val signals get() = IslandStateManager.iconSignals.value

    private fun msg(key: String = "chat", text: String = "Hi", pkg: String = "com.whatsapp") = NotificationInfo(
        key = key, packageName = pkg, appName = "WhatsApp", title = "Sam", text = text,
    )

    private fun timer(text: String) = NotificationInfo(
        key = "timer", packageName = "com.google.android.deskclock", appName = "Clock", title = "Timer",
        text = text, isOngoing = true, liveTimerBaseMs = 1_000L, liveTimerCountDown = true,
    )

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

    @Test
    fun timerReposts_neverNudge() {
        for (s in 10 downTo 1) IslandStateManager.updateNotification(timer("00:0$s".takeLast(5)))
        assertEquals(0, signals.sameAppNudge)
    }

    @Test
    fun sameChatNewText_nudgesOnce() {
        IslandStateManager.updateNotification(msg(text = "Hi"))
        assertEquals("the first message is a new card, not a nudge", 0, signals.sameAppNudge)
        IslandStateManager.updateNotification(msg(text = "Are you there?"))
        assertEquals(1, signals.sameAppNudge)
        IslandStateManager.updateNotification(msg(text = "Are you there?"))
        assertEquals("an identical repost isn't a new event", 1, signals.sameAppNudge)
    }

    @Test
    fun sameAppNewKey_nudges_otherApp_doesNot() {
        IslandStateManager.updateNotification(msg(key = "a"))
        IslandStateManager.updateNotification(msg(key = "b"))
        assertEquals(1, signals.sameAppNudge)
        IslandStateManager.updateNotification(msg(key = "c", pkg = "org.telegram.messenger"))
        assertEquals("a different app is a swap, not a nudge", 1, signals.sameAppNudge)
    }

    @Test
    fun ringingCall_startsOnce() {
        val call = NotificationInfo(
            key = "call", packageName = "com.google.android.dialer", appName = "Phone", title = "Sam",
            text = "Incoming call", isCall = true, isIncomingCall = true, isOngoing = true,
        )
        IslandStateManager.updateNotification(call)
        IslandStateManager.updateNotification(call.copy(text = "Incoming call · 0:05"))
        assertEquals(1, signals.ringStart)
        assertEquals(0, signals.sameAppNudge)
    }

    @Test
    fun alert_startsOnce() {
        val alarm = NotificationInfo(
            key = "alarm", packageName = "com.google.android.deskclock", appName = "Clock", title = "Alarm",
            text = "7:00", isAlert = true,
        )
        IslandStateManager.updateNotification(alarm)
        IslandStateManager.updateNotification(alarm.copy(text = "7:00 · snoozable"))
        assertEquals(1, signals.alertStart)
    }

    @Test
    fun glances_countAndResetClears() {
        IslandStateManager.triggerBluetoothEvent()
        IslandStateManager.triggerWifiEvent()
        IslandStateManager.triggerChargingEvent(40)
        assertEquals(2, signals.connectionAppear)
        assertEquals(1, signals.chargeStart)
        IslandStateManager.resetForTest()
        assertEquals(IconSignals(), signals)
    }

    @Test
    fun pureRule() {
        assertEquals(false, isSameAppNudge(null, msg()))
        assertEquals(false, isSameAppNudge(msg(), null))
        assertEquals(true, isSameAppNudge(msg(text = "a"), msg(text = "b")))
        assertEquals(false, isSameAppNudge(timer("00:05"), timer("00:04")))
    }
}
