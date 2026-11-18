package com.droidbubble.app.island.activity

import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.LiveActivityTester
import com.droidbubble.app.island.LiveActivityTester.Demo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A2.6: the Test lab can stack several demos at once. */
@OptIn(ExperimentalCoroutinesApi::class)
class TesterStackTest {

    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        IslandStateManager.resetForTest()
    }

    @After
    fun tearDown() {
        LiveActivityTester.stack = false
        LiveActivityTester.clear()
        IslandStateManager.resetForTest()
        Dispatchers.resetMain()
    }

    private val ids get() = IslandStateManager.store.activities.value.keys.map { it.value }

    @Test
    fun stacked_threeDemosGiveThreeEntries() {
        LiveActivityTester.stack = true
        LiveActivityTester.show(Demo.TIMER)
        LiveActivityTester.show(Demo.RECORDER)
        LiveActivityTester.show(Demo.DOWNLOAD)
        assertEquals(3, ids.size)
        assertTrue("test keys keep their prefix", ids.all { it.startsWith("n:droidbubble.test.") })
    }

    @Test
    fun notStacked_eachDemoReplacesTheLast() {
        LiveActivityTester.show(Demo.TIMER)
        LiveActivityTester.show(Demo.RECORDER)
        assertEquals(listOf("n:droidbubble.test.recorder"), ids)
    }

    @Test
    fun clearAll_removesStackedDemosAndTheirTickers() {
        LiveActivityTester.stack = true
        LiveActivityTester.show(Demo.DOWNLOAD)
        LiveActivityTester.show(Demo.SCORE)
        LiveActivityTester.clear()
        scheduler.advanceTimeBy(10_000L)
        scheduler.runCurrent()
        assertEquals(emptyList<String>(), ids)
    }
}
