package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.ScreenRecordingState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * M4.2: an expanded card follows the finger sideways; a quick flick dismisses it, a slow short
 * drag springs back, and a dismissed card leaves in the fling's direction without coming back.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandCardSwipeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)

    private val messageCard = IslandTestTags.content("card1", IslandPriority.POP_NOTIFICATION)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        h.setContent(mutableStateOf(IslandSettings(showNotifications = true)))
    }

    @After
    fun tearDown() = IslandStateManager.resetForTest()

    private fun expandMessage() {
        h.onState {
            IslandStateManager.updateNotification(IslandFixtures.message())
            IslandStateManager.expand(null)
        }
        h.settle()
        assertTrue(IslandStateManager.isExpanded.value)
    }

    /** Left edge of card 1's content, which moves with the card's drag offset. */
    private fun contentLeft(): Float? {
        val nodes = rule.onAllNodesWithTag(messageCard, useUnmergedTree = true)
        return if (nodes.fetchSemanticsNodes().isEmpty()) null else nodes[0].getBoundsInRoot().left.value
    }

    @Test
    fun quickFlick_dismissesTheCard() {
        expandMessage()
        rule.onAllNodesWithTag(IslandTestTags.CARD)[0].performTouchInput { swipeLeft(durationMillis = 80) }
        h.settle()
        assertNull(IslandStateManager.activeNotification.value)
    }

    @Test
    fun slowShortDrag_springsBackToRest() {
        expandMessage()
        val before = contentLeft()!!
        rule.onAllNodesWithTag(IslandTestTags.CARD)[0].performTouchInput {
            val dx = 30.dp.toPx()
            swipe(center, center + Offset(-dx, 0f), 600L)
        }
        h.settle()
        assertEquals("msg", IslandStateManager.activeNotification.value?.key)
        assertEquals("the card is back where it was", before, contentLeft()!!, 0.5f)
    }

    @Test
    fun whileDragging_theCardTracksTheFinger() {
        expandMessage()
        val before = contentLeft()!!
        val card = rule.onAllNodesWithTag(IslandTestTags.CARD)[0]
        card.performTouchInput {
            down(center)
            repeat(6) { moveBy(Offset(-10.dp.toPx(), 0f), delayMillis = 50L) }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val moved = before - contentLeft()!!
        assertEquals("1:1 with a 60 dp drag (was $moved dp)", 60f, moved, 2f)
        card.performTouchInput { up() }
        h.settle()
    }

    @Test
    fun dismissedCard_leavesInTheFlingDirection_neverComingBackFirst() {
        // Dual: the island stays open with the media card after the message goes
        h.onState { IslandStateManager.updateMedia(IslandFixtures.song) }
        expandMessage()
        val start = contentLeft() ?: return
        rule.mainClock.autoAdvance = false
        rule.onAllNodesWithTag(IslandTestTags.CARD)[0].performTouchInput { swipeRight(durationMillis = 80) }
        var last = start
        repeat(40) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val left = contentLeft() ?: return@repeat
            assertTrue("the card moved back ($last → $left)", left >= last - 0.5f)
            last = left
        }
        rule.mainClock.autoAdvance = true
        h.settle()
        assertNull(IslandStateManager.activeNotification.value)
    }

    @Test
    fun screenRecordingCard_resistsAndStays() {
        h.onState {
            IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = true, durationSeconds = 9))
            IslandStateManager.expand(IslandPriority.SCREEN_RECORDING)
        }
        h.settle()
        h.swipeCard(-500f)
        h.settle()
        assertTrue(IslandStateManager.screenRecordingState.value.isRecording)
        assertTrue("an inert card doesn't collapse the island", IslandStateManager.isExpanded.value)
    }
}
