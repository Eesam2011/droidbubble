package com.droidbubble.app.island.icon

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.IslandClock
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.RecompositionProbe
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.island.IslandStateManager.RingerMode
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * I3.2: the attention effects are wired into the real island and stay in the layer: a ringing
 * call wiggling for 10 s recomposes the root a couple of times a second (its timer), not per frame, and
 * every wired event plays without crashing.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class IconWiringTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)
    private val settings = mutableStateOf(IslandSettings(showNotifications = true))
    private var rootCount = 0

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        h.freezeClock()
        IslandClock.wallMs = { IslandHarness.FROZEN_WALL_MS + rule.mainClock.currentTime }
        RecompositionProbe.onRoot = { rootCount++ }
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

    @Test
    fun ringingCall_tenSeconds_rootRecomposesAboutOnceASecond() {
        rule.runOnIdle { IslandStateManager.updateNotification(IslandFixtures.incomingCall) }
        frames(1500)
        IslandStateManager.collapse()
        frames(1000)
        rootCount = 0
        frames(10_000)
        // ~600 frames: per-frame recomposition would be hundreds. Measured 20 (the call timer
        // ticking, about twice a second); the wiggle itself adds none
        assertTrue("root recomposed $rootCount times in 10 s", rootCount <= 25)
    }

    @Test
    fun everyWiredEvent_plays() {
        rule.runOnIdle {
            IslandStateManager.updateNotification(IslandFixtures.message(text = "one"))
        }
        frames(300)
        repeat(5) {
            rule.runOnIdle { IslandStateManager.updateNotification(IslandFixtures.message(text = "burst $it")) }
            frames(400)
        }
        rule.runOnIdle { IslandStateManager.triggerBluetoothEvent() }
        frames(800)
        for (mode in listOf(RingerMode.RING, RingerMode.SILENT, RingerMode.RING, RingerMode.VIBRATE, RingerMode.SILENT)) {
            rule.runOnIdle { IslandStateManager.triggerRingerModeEvent(mode) }
            frames(300)
        }
        frames(2000)
    }
}
