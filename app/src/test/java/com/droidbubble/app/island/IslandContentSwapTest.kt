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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * M3.3: when another activity takes the open card (a call over the media player, a message from
 * another conversation), the two contents overlap (old fading out, new fading in) instead of
 * cutting, and no frame shows neither. An update in the same conversation changes in place.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandContentSwapTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        h.freezeClock()
        h.setContent(mutableStateOf(IslandSettings(showNotifications = true)))
    }

    @After
    fun tearDown() {
        h.unfreezeClock()
        IslandStateManager.resetForTest()
    }

    private val media = IslandTestTags.swapContent("card1", IslandPriority.MEDIA)
    private fun incoming() = listOf(IslandPriority.POP_NOTIFICATION, IslandPriority.CALL).any { h.exists(IslandTestTags.swapContent("card1", it)) }

    /** Steps 30 frames after [change]; every frame shows something, and old and new overlap. */
    private fun assertCrossfade(change: () -> Unit, old: () -> Boolean, new: () -> Boolean) {
        rule.runOnIdle(change)
        var overlapped = 0
        repeat(30) {
            h.advanceFrozen(16)
            val o = old()
            val n = new()
            assertTrue("frame $it: something is showing", o || n)
            if (o && n) overlapped++
        }
        assertTrue("old and new overlap for a few frames ($overlapped)", overlapped >= 3)
        h.advanceFrozen(1000)
        assertTrue("the new content stays", new())
        assertFalse("the old content is gone once settled", old())
    }

    @Test
    fun incomingCallReplacesMedia_crossfades() {
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        h.advanceFrozen(1500)
        rule.runOnIdle { IslandStateManager.expand(null) }
        h.advanceFrozen(1500)
        assertTrue("media is showing", h.exists(media))
        assertCrossfade({ IslandStateManager.updateNotification(IslandFixtures.incomingCall) }, { h.exists(media) }, ::incoming)
    }

    @Test
    fun messageFromAnotherConversation_crossfades() {
        rule.runOnIdle {
            IslandStateManager.updateNotification(IslandFixtures.message("a", "First"))
            IslandStateManager.expand(IslandPriority.POP_NOTIFICATION)
        }
        h.advanceFrozen(1500)
        // Both are POP_NOTIFICATION, so count the keyed children
        val tag = IslandTestTags.swapContent("card1", IslandPriority.POP_NOTIFICATION)
        fun count() = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().size
        assertTrue("the message is showing", count() == 1)
        rule.runOnIdle { IslandStateManager.updateNotification(IslandFixtures.message("b", "Second")) }
        var overlapped = 0
        repeat(30) {
            h.advanceFrozen(16)
            val c = count()
            assertTrue("frame $it: something is showing", c >= 1)
            if (c == 2) overlapped++
        }
        assertTrue("old and new overlap for a few frames ($overlapped)", overlapped >= 3)
        h.advanceFrozen(1000)
        assertTrue("one message once settled", count() == 1)
    }

    @Test
    fun sameConversationUpdate_doesNotTransition() {
        rule.runOnIdle {
            IslandStateManager.updateNotification(IslandFixtures.message("a", "First"))
            IslandStateManager.expand(IslandPriority.POP_NOTIFICATION)
        }
        val tag = IslandTestTags.swapContent("card1", IslandPriority.POP_NOTIFICATION)
        h.advanceFrozen(1500)
        assertTrue("the message is showing", rule.onAllNodesWithTag(tag).fetchSemanticsNodes().size == 1)
        rule.runOnIdle { IslandStateManager.updateNotification(IslandFixtures.message("a", "Second")) }
        repeat(20) {
            h.advanceFrozen(16)
            assertTrue("frame $it: one child only", rule.onAllNodesWithTag(tag).fetchSemanticsNodes().size == 1)
        }
    }
}
