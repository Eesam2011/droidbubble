package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandPriority
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

/**
 * M2.6: the pill's content is laid out once, at the width the pill is heading to, and the
 * growing pill reveals it with its clip. Mid-appear the content keeps its final width while the
 * pill is still narrower; settled, the two are the same. The content doesn't recompose while
 * the pill's width springs.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandPillFinalWidthTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)
    private var compactCompositions = 0

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        h.freezeClock()
        RecompositionProbe.onCompactContent = { compactCompositions++ }
        h.setContent(mutableStateOf(IslandSettings(showNotifications = true)))
        h.advanceFrozen(100)
    }

    @After
    fun tearDown() {
        RecompositionProbe.onCompactContent = null
        h.unfreezeClock()
        IslandStateManager.resetForTest()
    }

    private val mediaContent = IslandTestTags.content("pill", IslandPriority.MEDIA)

    private fun pillWidthPx(): Int? =
        rule.onAllNodesWithTag(IslandTestTags.PILL).fetchSemanticsNodes().firstOrNull()?.size?.width

    /** The content's own laid-out width (not clipped by the pill), in px. */
    private fun contentWidthPx(): Int? =
        rule.onAllNodesWithTag(mediaContent, useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.size?.width

    @Test
    fun contentKeepsItsFinalWidth_whileThePillGrows() {
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        val seen = mutableListOf<Pair<Int, Int>>()
        repeat(40) {
            h.advanceFrozen(16)
            val pill = pillWidthPx()
            val content = contentWidthPx()
            if (pill != null && content != null) seen += pill to content
        }
        h.settle()
        val settledPill = pillWidthPx()!!
        assertEquals("settled, the content is exactly as wide as the pill", settledPill, contentWidthPx())
        assertTrue("the content was visible during the appear", seen.isNotEmpty())
        for ((pill, content) in seen) {
            assertEquals("mid-appear the content is laid out at the final width (pill $pill px)", settledPill, content)
        }
        assertTrue("some frame showed the pill narrower than its content", seen.any { (pill, content) -> pill < content })
    }

    @Test
    fun contentDoesntRecompose_duringTheWidthMorph() {
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        h.advanceFrozen(16)
        compactCompositions = 0
        repeat(30) { h.advanceFrozen(16) }
        // The first composition when the content becomes visible, at most once more for the
        // swap into place; never once per frame
        assertTrue("CompactContent composed $compactCompositions times during a 480 ms appear", compactCompositions <= 2)
    }
}
