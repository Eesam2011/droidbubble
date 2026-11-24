package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * C3.10: open cards follow the phone's font size up to 2.0 and nothing in them clips; the
 * compact pill stops growing at 1.3.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandFontScaleTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val h = IslandHarness(rule)
    private val settingsState = mutableStateOf(IslandSettings(showNotifications = true))
    private val fontScale = mutableFloatStateOf(1f)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        rule.mainClock.autoAdvance = true
        h.setContent(settingsState, fontScale)
    }

    @After
    fun tearDown() = IslandStateManager.resetForTest()

    // --- The caps ---------------------------------------------------------------------------

    @Test
    fun caps() {
        assertEquals(2.0f, IslandFontScale.card(2.0f))
        assertEquals(2.0f, IslandFontScale.card(3.0f))
        assertEquals(0.85f, IslandFontScale.card(0.5f))
        assertEquals(1.3f, IslandFontScale.compact(2.0f))
        assertEquals(1.1f, IslandFontScale.compact(1.1f))
        val d = Density(3f, 1.2f)
        assertSame("within the cap: the same density", d, IslandFontScale.compactDensity(d))
        assertEquals(1.3f, IslandFontScale.compactDensity(Density(3f, 2f)).fontScale)
    }

    // --- Cards: everything inside the card at 1.0, 1.3 and 2.0 ------------------------------

    /** Text and clickable nodes of the open card 1, unmerged so each text counts. */
    private fun cardContent(card: Rect): List<SemanticsNode> = rule.onAllNodes(
        SemanticsMatcher("text or button") { n ->
            n.config.getOrNull(SemanticsProperties.Text) != null ||
                n.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        },
        useUnmergedTree = true,
    ).fetchSemanticsNodes().filter { n ->
        val b = n.boundsInRoot
        // only nodes of this card (its column), not the backdrop or card 2
        b.width > 0f && b.height > 0f && b.center.y >= card.top && b.center.y <= card.bottom &&
            b.center.x >= card.left && b.center.x <= card.right && b != card
    }

    private fun assertFits(state: String, font: Float) {
        rule.waitForIdle()
        val card = rule.onAllNodesWithTag(IslandTestTags.CARD)[0].fetchSemanticsNode().boundsInRoot
        val nodes = cardContent(card)
        assertTrue("$state at $font: the card has content", nodes.isNotEmpty())
        val slack = 1.5f
        for (n in nodes) {
            val b = n.boundsInRoot
            val name = n.config.getOrNull(SemanticsProperties.Text)?.joinToString() ?: n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
            assertTrue(
                "$state at $font: '$name' $b leaves the card $card",
                b.left >= card.left - slack && b.right <= card.right + slack && b.top >= card.top - slack && b.bottom <= card.bottom + slack,
            )
        }
        println("C3.10 $state font=$font card=${card.height / rule.density.density}dp nodes=${nodes.size}")
    }

    private fun open(state: String, font: Float, show: () -> Unit) {
        rule.runOnIdle {
            IslandStateManager.resetForTest()
            fontScale.floatValue = font
        }
        h.settle(300)
        h.onState { show(); IslandStateManager.expand(null) }
        h.settle(2000)
        assertTrue("$state opened", IslandStateManager.isExpanded.value)
        assertFits(state, font)
    }

    @Test
    fun timerCard_fits() {
        for (font in listOf(1f, 1.3f, 2f)) open("timer", font) { IslandStateManager.updateNotification(IslandFixtures.timer) }
    }

    @Test
    fun messageCard_fits() {
        for (font in listOf(1f, 1.3f, 2f)) open("message", font) { IslandStateManager.updateNotification(IslandFixtures.message()) }
    }

    @Test
    fun mediaCard_fits() {
        for (font in listOf(1f, 1.3f, 2f)) open("media", font) { IslandStateManager.updateMedia(IslandFixtures.song) }
    }

    @Test
    fun cardTextGrowsBeyondTheOldCap() {
        fun titleHeight(font: Float): Float {
            open("message", font) { IslandStateManager.updateNotification(IslandFixtures.message(text = "Hello there")) }
            return rule.onAllNodes(SemanticsMatcher("body") { n ->
                n.config.getOrNull(SemanticsProperties.Text)?.joinToString() == "Hello there"
            }, useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot.height
        }
        val at15 = titleHeight(1.5f)
        val at20 = titleHeight(2f)
        assertTrue("body text at 2.0 ($at20) is taller than at 1.5 ($at15)", at20 > at15)
    }

    // --- Compact: capped at 1.3 -------------------------------------------------------------

    @Test
    fun pill_stopsGrowingAt1_3() {
        fun pillWidth(font: Float): Float {
            rule.runOnIdle {
                IslandStateManager.resetForTest()
                fontScale.floatValue = font
            }
            h.settle(300)
            h.onState { IslandStateManager.updateNotification(IslandFixtures.timer) }
            h.settle(2000)
            return rule.onAllNodesWithTag(IslandTestTags.PILL)[0].fetchSemanticsNode().boundsInRoot.width
        }
        val at13 = pillWidth(1.3f)
        val at20 = pillWidth(2f)
        assertEquals("the pill at 2.0 is the pill at 1.3", at13, at20, 0.5f)
    }
}
