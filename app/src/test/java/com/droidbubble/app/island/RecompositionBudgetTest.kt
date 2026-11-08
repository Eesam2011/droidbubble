package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.NetworkSpeedInfo
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * How many times IslandOverlayRoot composes in each scenario (S4.1). The counts are recorded in
 * [PerfBaseline]; a change that adds 10 % or more fails. A task that lowers a count updates the
 * baseline down. The clock is driven by hand in 16 ms frames.
 *
 * Record mode prints the new numbers: `-Pisland.record=true`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecompositionBudgetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)
    private val settings = mutableStateOf(IslandSettings(showNotifications = true))
    private var count = 0

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        h.freezeClock()
        // The wall clock moves with the test clock, so timers tick exactly as time is advanced
        IslandClock.wallMs = { IslandHarness.FROZEN_WALL_MS + rule.mainClock.currentTime }
        RecompositionProbe.onRoot = { count++ }
        h.setContent(settings)
        frames(500)
    }

    @After
    fun tearDown() {
        RecompositionProbe.onRoot = null
        h.unfreezeClock()
        IslandStateManager.resetForTest()
    }

    private fun frames(ms: Long) {
        repeat((ms / 16).toInt()) { rule.mainClock.advanceTimeBy(16) }
        rule.waitForIdle()
    }

    /** Counts root compositions while [block] runs. */
    private fun measure(block: () -> Unit): Int {
        rule.waitForIdle()
        count = 0
        block()
        rule.waitForIdle()
        return count
    }

    private fun check(name: String, actual: Int, baseline: Int) {
        if (System.getProperty("island.record") == "true") {
            println("PerfBaseline $name = $actual")
            return
        }
        assertTrue("$name: $actual root compositions, baseline $baseline (+10 % allowed)", actual <= baseline * 1.1)
    }

    @Test
    fun mediaExpand_500ms() {
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        frames(1500)
        val n = measure {
            rule.runOnIdle { IslandStateManager.expand(null) }
            frames(500)
        }
        check("MEDIA_EXPAND_500MS", n, PerfBaseline.MEDIA_EXPAND_500MS)
    }

    @Test
    fun mediaCollapse_600ms() {
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        frames(1500)
        rule.runOnIdle { IslandStateManager.expand(null) }
        frames(1500)
        val n = measure {
            rule.runOnIdle { IslandStateManager.collapse() }
            frames(600)
        }
        check("MEDIA_COLLAPSE_600MS", n, PerfBaseline.MEDIA_COLLAPSE_600MS)
    }

    @Test
    fun playingMediaPill_atRest_5s() {
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        frames(2000)
        val n = measure { frames(5000) }
        check("MEDIA_PILL_REST_5S", n, PerfBaseline.MEDIA_PILL_REST_5S)
    }

    @Test
    fun runningTimerPill_5s() {
        rule.runOnIdle { IslandStateManager.updateNotification(IslandFixtures.timer) }
        frames(2000)
        val n = measure { frames(5000) }
        check("TIMER_PILL_5S", n, PerfBaseline.TIMER_PILL_5S)
    }

    @Test
    fun networkSpeed_1Hz_3s() {
        rule.runOnIdle { settings.value = settings.value.copy(showNetworkSpeed = true) }
        frames(2000)
        val n = measure {
            for (i in 1..3) {
                rule.runOnIdle {
                    IslandStateManager.updateNetworkSpeed(
                        NetworkSpeedInfo(downloadSpeedFormatted = "${i * 100} KB/s", downloadBytesPerSec = i * 100_000L)
                    )
                }
                frames(1000)
            }
        }
        check("NETWORK_SPEED_1HZ_3S", n, PerfBaseline.NETWORK_SPEED_1HZ_3S)
    }
}
