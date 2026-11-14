package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * M4.3: a swipe up from card 1's pill zone tucks the island back, however slow; a short pull
 * springs back; a swipe up that starts on the card's content (the media slider) does nothing.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandCardTuckTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        h.setContent(mutableStateOf(IslandSettings(showNotifications = true)))
        h.onState {
            IslandStateManager.updateMedia(IslandFixtures.song)
            IslandStateManager.expand(null)
        }
        h.settle()
        assertTrue(IslandStateManager.isExpanded.value)
    }

    @After
    fun tearDown() = IslandStateManager.resetForTest()

    private val card get() = rule.onAllNodesWithTag(IslandTestTags.CARD)[0]

    private fun cardHeight(): Float = card.getBoundsInRoot().let { (it.bottom - it.top).value }

    /** A swipe up by [dyDp] from the card's pill zone (8 dp below its top). */
    private fun swipeUpFromPillZone(dyDp: Float, durationMs: Long) {
        card.performTouchInput {
            val start = Offset(width / 2f, 8.dp.toPx())
            swipe(start, start - Offset(0f, dyDp.dp.toPx()), durationMs)
        }
    }

    @Test
    fun swipeUp_fromThePillZone_collapses() {
        h.swipeUpOnCard()
        h.settle()
        assertFalse(IslandStateManager.isExpanded.value)
    }

    @Test
    fun slowSwipeUp_past48dp_collapses() {
        // Before M4.3 a slow swipe up never collapsed (it needed 15 px in one event)
        swipeUpFromPillZone(dyDp = 60f, durationMs = 1200L)
        h.settle()
        assertFalse(IslandStateManager.isExpanded.value)
    }

    @Test
    fun shortPull_springsBack() {
        val before = cardHeight()
        swipeUpFromPillZone(dyDp = 20f, durationMs = 600L)
        h.settle()
        assertTrue(IslandStateManager.isExpanded.value)
        assertEquals("the card is back to its full height", before, cardHeight(), 0.5f)
    }

    @Test
    fun whileDraggingUp_theCardShortens_thenSpringsBack() {
        val before = cardHeight()
        card.performTouchInput {
            down(Offset(width / 2f, 8.dp.toPx()))
            repeat(4) { moveBy(Offset(0f, -6.dp.toPx()), delayMillis = 60L) }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val during = cardHeight()
        assertTrue("shorter while pulled up ($before → $during)", during < before - 5f)
        assertTrue("rubber-banded: less than the 24 dp pulled", during > before - 24f)
        card.performTouchInput { up() }
        h.settle()
        assertTrue(IslandStateManager.isExpanded.value)
        assertEquals(before, cardHeight(), 0.5f)
    }

    @Test
    fun pullDown_givesAndSpringsBack_withoutAction() {
        val before = card.getBoundsInRoot().top.value
        swipeUpFromPillZone(dyDp = -80f, durationMs = 200L)
        h.settle()
        assertTrue(IslandStateManager.isExpanded.value)
        assertEquals(before, card.getBoundsInRoot().top.value, 0.5f)
    }

    @Test
    fun swipeUp_startingOnTheCardContent_doesNotCollapse() {
        // The media slider and buttons sit below the pill zone: a swipe from there is theirs
        card.performTouchInput {
            val start = Offset(width / 2f, height * 0.75f)
            swipe(start, start - Offset(0f, 200f), 150L)
        }
        h.settle()
        assertTrue(IslandStateManager.isExpanded.value)
    }
}
