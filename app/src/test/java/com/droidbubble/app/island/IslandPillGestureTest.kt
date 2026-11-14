package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.island.shell.ShadeOpener
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * M4.4: the compact pill. Down 24–64 dp expands, further opens the shade (or expands without the
 * accessibility service), inward minimises to the ring, the ring outward restores, tap still expands.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandPillGestureTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)
    private var shadeOpened = 0

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        shadeOpened = 0
        // A fake accessibility service: the shade "opens"
        ShadeOpener.open = { shadeOpened++; true }
        h.setContent(mutableStateOf(IslandSettings(showNotifications = true)))
        h.onState { IslandStateManager.updateMedia(IslandFixtures.song) }
        h.settle()
    }

    @After
    fun tearDown() {
        ShadeOpener.reset()
        IslandStateManager.resetForTest()
    }

    private fun pullDown(dyDp: Float, durationMs: Long = 400L) {
        rule.onAllNodesWithTag(IslandTestTags.PILL)[0].performTouchInput {
            swipe(center, center + Offset(0f, dyDp.dp.toPx()), durationMs)
        }
        h.settle()
    }

    @Test
    fun pullDown40dp_expands() {
        pullDown(40f)
        assertTrue(IslandStateManager.isExpanded.value)
        assertTrue("no shade for a short pull", shadeOpened == 0)
    }

    @Test
    fun slowShortPull_springsBack() {
        pullDown(12f, durationMs = 600L)
        assertFalse(IslandStateManager.isExpanded.value)
    }

    @Test
    fun pullDownPast64dp_opensTheShade() {
        pullDown(100f)
        assertTrue(shadeOpened == 1)
        assertFalse(IslandStateManager.isExpanded.value)
    }

    @Test
    fun pullDownPast64dp_withoutTheService_expands() {
        ShadeOpener.open = { false }
        pullDown(100f)
        assertTrue(IslandStateManager.isExpanded.value)
    }

    @Test
    fun inwardFromTheRightHalf_minimises() {
        h.swipePill(-150f, startFraction = 0.85f)
        h.settle()
        assertTrue(IslandStateManager.userMinimized.value)
    }

    @Test
    fun inwardFromTheLeftHalf_minimises() {
        h.swipePill(150f, startFraction = 0.15f)
        h.settle()
        assertTrue(IslandStateManager.userMinimized.value)
    }

    @Test
    fun outwardOnThePill_doesNothing() {
        h.swipePill(-150f, startFraction = 0.15f)
        h.settle()
        h.swipePill(150f, startFraction = 0.85f)
        h.settle()
        assertFalse(IslandStateManager.userMinimized.value)
        assertFalse(IslandStateManager.userHidden.value)
    }

    @Test
    fun outwardOnTheRing_restores() {
        h.swipePill(-150f, startFraction = 0.5f) // from the centre: LTR inward is left
        h.settle()
        assertTrue(IslandStateManager.userMinimized.value)
        h.swipePill(150f, startFraction = 0.5f)
        h.settle()
        assertFalse(IslandStateManager.userMinimized.value)
    }

    @Test
    fun tap_stillExpands() {
        h.tap(IslandTestTags.PILL)
        h.settle()
        assertTrue(IslandStateManager.isExpanded.value)
    }
}
