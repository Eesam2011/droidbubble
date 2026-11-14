package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.MotionTier
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * M5.2: with Reduce motion on, no layer of the island ever gets a blur (every motion blur goes
 * through `motionBlur`, which the probe counts), through an expand, a collapse and an activity
 * swap; and the island still ends in the same settled places. Full motion does blur, which
 * proves the probe sees the blurs at all.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandReducedMotionTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)
    private val settings = mutableStateOf(IslandSettings(showNotifications = true))
    private var blurs = 0

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        h.freezeClock()
        RecompositionProbe.onMotionBlur = { blurs++ }
    }

    @After
    fun tearDown() {
        RecompositionProbe.onMotionBlur = null
        h.unfreezeClock()
        IslandStateManager.resetForTest()
    }

    /** Steps [ms] in 16 ms frames, drawing each one so every graphicsLayer block runs. */
    private fun frames(ms: Long) {
        repeat((ms / 16).toInt()) {
            h.advanceFrozen(16)
            h.draw()
        }
    }

    /** Media pill → expand → collapse → a message takes the pill. Returns the blurs handed out. */
    private fun journey(reduce: Boolean): Int {
        settings.value = IslandSettings(showNotifications = true, reduceMotion = reduce)
        h.setContent(settings)
        h.advanceFrozen(100)
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        frames(1500)
        blurs = 0
        rule.runOnIdle { IslandStateManager.expand(null) }
        frames(800)
        rule.runOnIdle { IslandStateManager.collapse() }
        frames(800)
        rule.runOnIdle { IslandStateManager.updateNotification(IslandFixtures.incomingCall) }
        frames(800)
        return blurs
    }

    @Test
    fun reduced_neverBlurs() {
        assertEquals("motion blurs handed out with Reduce motion on", 0, journey(reduce = true))
    }

    @Test
    fun full_blurs_soTheProbeWorks() {
        assertTrue("no motion blur seen in Full motion: the probe isn't wired", journey(reduce = false) > 0)
    }

    @Test
    fun reduced_settlesWhereFullDoes() {
        settings.value = IslandSettings(showNotifications = true, reduceMotion = true)
        h.setContent(settings)
        h.advanceFrozen(100)
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        h.settle()
        assertNotNull("the pill is drawn", h.bounds(IslandTestTags.PILL))
        assertEquals(IslandPriority.MEDIA, h.contentOf("pill"))
        rule.runOnIdle { IslandStateManager.expand(null) }
        h.settle()
        assertNotNull("the card opened", h.bounds(IslandTestTags.CARD))
        assertEquals(IslandPriority.MEDIA, h.contentOf("card1"))
        rule.runOnIdle { IslandStateManager.collapse() }
        h.settle()
        assertFalse("the card closed", h.exists(IslandTestTags.CARD))
        assertEquals(IslandPriority.MEDIA, h.contentOf("pill"))
    }

    @Test
    fun rollingNumbers_crossfadeInReducedMotion() {
        // RollingNumberText slides digits only when rollsDigits(); otherwise they crossfade in place
        for (tier in MotionTier.values()) {
            val m = IslandMotion.from(IslandSettings(), tier)
            assertEquals("$tier", tier != MotionTier.REDUCED, rollsDigits(m))
        }
    }

    @Test
    fun reduced_runningTimerStillRenders() {
        // The timer's digits change every second through the crossfade path without crashing
        settings.value = IslandSettings(showNotifications = true, reduceMotion = true)
        h.setContent(settings)
        h.advanceFrozen(100)
        rule.runOnIdle { IslandStateManager.updateNotification(IslandFixtures.timer) }
        h.settle()
        IslandClock.wallMs = { IslandHarness.FROZEN_WALL_MS + 1_000L }
        frames(400)
        assertEquals(IslandPriority.POP_NOTIFICATION, h.contentOf("pill"))
    }
}
