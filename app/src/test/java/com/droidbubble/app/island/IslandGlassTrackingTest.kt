package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * M2.5: with liquid glass on, the blur pane follows card 1 every frame of the expand and the
 * collapse (published from layout and from the strength changes, not from recomposition), ends
 * at full strength on the settled card, and is gone once the card has closed.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandGlassTrackingTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        h.freezeClock()
        h.setContent(mutableStateOf(IslandSettings(showNotifications = true, liquidGlassCards = true)))
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        h.advanceFrozen(1500)
    }

    @After
    fun tearDown() {
        h.unfreezeClock()
        IslandStateManager.resetForTest()
    }

    private fun cardRect() = rule.onAllNodesWithTag(IslandTestTags.CARD).fetchSemanticsNodes().firstOrNull()?.boundsInRoot

    private fun panes() = rule.runOnIdle { IslandStateManager.glassPanes.value }

    @Test
    fun pane_tracksTheCard_throughExpandAndCollapse() {
        assertTrue("no pane while compact", panes().isEmpty())
        rule.runOnIdle { IslandStateManager.expand(null) }
        var tracked = 0
        repeat(40) {
            h.advanceFrozen(16)
            val pane = panes().firstOrNull() ?: return@repeat
            val card = cardRect() ?: return@repeat
            tracked++
            // Inside the card by at most the motion inset (3 dp = 9 px) plus rounding, every frame
            assertTrue("frame $it: pane $pane vs card $card", abs(pane.left - card.left) <= 10f && abs(pane.top - card.top) <= 10f)
            assertTrue("frame $it: pane $pane vs card $card", abs(pane.left + pane.width - card.right) <= 10f && abs(pane.top + pane.height - card.bottom) <= 10f)
        }
        assertTrue("the pane appeared during the expand ($tracked frames)", tracked > 10)
        h.advanceFrozen(1000)
        val settled = panes().single()
        assertEquals(1f, settled.strength, 0f)
        val card = cardRect()!!
        assertEquals(card.left, settled.left.toFloat(), 1f)
        assertEquals(card.width, settled.width.toFloat(), 1f)

        rule.runOnIdle { IslandStateManager.collapse() }
        h.advanceFrozen(1500)
        assertTrue("no pane once closed", panes().isEmpty())
    }
}
