package com.droidbubble.app.island.icon

import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.LiveActivityTester
import com.droidbubble.app.island.IslandStateManager.RingerMode
import com.droidbubble.app.island.LiveActivityTester.IconDemo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** I4.2: each Test-panel icon demo produces the event it names. */
@OptIn(ExperimentalCoroutinesApi::class)
class IconDemoTest {

    private val scheduler = TestCoroutineScheduler()
    private val signals get() = IslandStateManager.iconSignals.value

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        IslandStateManager.resetForTest()
        // D2.3 throttles nudges in time: the demo's steps must pass on the test clock
        IslandStateManager.nowMs = { scheduler.currentTime }
    }

    @After
    fun tearDown() {
        IslandStateManager.resetForTest()
        IslandStateManager.nowMs = { System.nanoTime() / 1_000_000L }
        Dispatchers.resetMain()
    }

    private fun run(demo: IconDemo) = runTest(scheduler) { LiveActivityTester.runIconDemo(demo) }

    @Test
    fun sameApp_nudgesTwice() {
        run(IconDemo.SAME_APP)
        assertEquals("three messages from one chat: the 2nd and 3rd are nudges", 2, signals.sameAppNudge)
    }

    @Test
    fun bluetooth_appears() {
        run(IconDemo.BLUETOOTH)
        assertEquals(1, signals.connectionAppear)
    }

    @Test
    fun charging_starts() {
        run(IconDemo.CHARGING)
        assertEquals(1, signals.chargeStart)
    }

    @Test
    fun ringer_cyclesRingSilentRingVibrateRing() = runTest(scheduler) {
        val job = launch { LiveActivityTester.runIconDemo(IconDemo.RINGER) }
        val seen = ArrayList<IslandStateManager.RingerMode?>()
        repeat(5) {
            advanceTimeBy(IconDemo.STEP_MS / 2)
            seen += IslandStateManager.activeRingerMode.value
            advanceTimeBy(IconDemo.STEP_MS / 2)
        }
        job.join()
        assertEquals(listOf(RingerMode.RING, RingerMode.SILENT, RingerMode.RING, RingerMode.VIBRATE, RingerMode.RING), seen)
    }

    @Test
    fun download_rampsThenGoes() {
        run(IconDemo.DOWNLOAD)
        assertNull(IslandStateManager.activeProgress.value)
    }

    @Test
    fun torch_stepsThenTurnsOff() {
        run(IconDemo.TORCH)
        assertFalse(IslandStateManager.isFlashlightOn.value)
    }
}
