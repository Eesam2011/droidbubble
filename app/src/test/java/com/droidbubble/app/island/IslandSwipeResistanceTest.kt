package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.ScreenRecordingState
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The "Swipe Resistance" accessibility setting: on (the default), the screen-recording card
 * moves a fraction of the finger; off, it follows the finger like every other card.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandSwipeResistanceTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        h.freezeClock()
    }

    @After
    fun tearDown() {
        h.unfreezeClock()
        IslandStateManager.resetForTest()
    }

    /** Opens the recording card, drags it [dxPx] right without letting go, and returns how far it moved. */
    private fun dragRecordingCard(resistance: Boolean, dxPx: Float): Float {
        h.setContent(mutableStateOf(IslandSettings(swipeResistance = resistance)))
        rule.runOnIdle {
            IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = true, durationSeconds = 9))
        }
        h.advanceFrozen(1500)
        rule.runOnIdle { IslandStateManager.expand(null) }
        h.advanceFrozen(1500)
        val tag = IslandTestTags.content("card1", IslandPriority.SCREEN_RECORDING)
        fun left() = rule.onAllNodesWithTag(tag, useUnmergedTree = true)[0].fetchSemanticsNode().boundsInRoot.left
        val before = left()
        rule.onAllNodesWithTag(IslandTestTags.CARD)[0].performTouchInput {
            down(center)
            // Past the touch slop first, then the measured drag
            moveBy(Offset(40f, 0f))
            moveBy(Offset(dxPx, 0f))
        }
        h.advanceFrozen(1000)
        val moved = left() - before
        rule.onAllNodesWithTag(IslandTestTags.CARD)[0].performTouchInput { up() }
        return moved
    }

    @Test
    fun on_theRecordingCardResists() {
        val moved = dragRecordingCard(resistance = true, dxPx = 200f)
        assertTrue("moved $moved px for a 200 px drag", moved in 1f..60f)
    }

    @Test
    fun off_theRecordingCardFollowsTheFinger() {
        val moved = dragRecordingCard(resistance = false, dxPx = 200f)
        assertTrue("moved $moved px for a 200 px drag", moved >= 180f)
    }
}
