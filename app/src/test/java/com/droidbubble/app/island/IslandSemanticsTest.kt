package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.A11yMatchers
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
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
 * The island's TalkBack contract (phase 8, C3). C3.1 pinned the unlabelled pill; C3.4 gives the
 * pill and the bubble their spoken summary and labelled tap and long-press.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandSemanticsTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val h = IslandHarness(rule)
    private val settingsState = mutableStateOf(IslandSettings(showNotifications = true))

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        rule.mainClock.autoAdvance = true
        h.setContent(settingsState)
    }

    @After
    fun tearDown() {
        IslandStateManager.resetForTest()
    }

    private fun node(tag: String) = rule.onAllNodesWithTag(tag)[0]
    private fun config(tag: String) = node(tag).fetchSemanticsNode().config

    /** The node's own headline: the first description (children's merged ones follow it). */
    private fun headline(tag: String) = config(tag).getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

    @Test
    fun timerPill_readsTimer_expandAndOpenInClock() {
        h.onState { IslandStateManager.updateNotification(IslandFixtures.timer) }
        h.settle()
        assertTrue(h.exists(IslandTestTags.PILL))
        assertEquals("Timer", headline(IslandTestTags.PILL))
        val value = config(IslandTestTags.PILL).getOrNull(SemanticsProperties.StateDescription)
        assertTrue("value: $value", value != null && value.endsWith("seconds left"))
        node(IslandTestTags.PILL).assert(A11yMatchers.hasClickLabel("Expand"))
        node(IslandTestTags.PILL).assert(A11yMatchers.hasLongClickLabel("Open in Clock"))
    }

    @Test
    fun messagePill_opensTheConversationOnlyWithItsOwnTarget() {
        h.onState { IslandStateManager.updateNotification(IslandFixtures.message()) }
        h.settle()
        assertEquals("WhatsApp: Sam", headline(IslandTestTags.PILL))
        // The fixture has no contentIntent: long-press opens the app
        node(IslandTestTags.PILL).assert(A11yMatchers.hasLongClickLabel("Open in WhatsApp"))
    }

    @Test
    fun idleAlwaysShownPill_isSkipped() {
        settingsState.value = IslandSettings(showNotifications = true, alwaysShowPill = true)
        h.settle()
        assertTrue(h.exists(IslandTestTags.PILL))
        assertNull(headline(IslandTestTags.PILL))
        assertNull(config(IslandTestTags.PILL).getOrNull(SemanticsActions.OnClick))
    }

    @Test
    fun chargingGlance_readsChargingAndPercent_withNoTapAction() {
        settingsState.value = IslandSettings(showNotifications = true, alwaysShowPill = true)
        h.onState { IslandStateManager.triggerChargingEvent(54) }
        h.settle(300)
        assertEquals("Charging", headline(IslandTestTags.PILL))
        assertEquals("54%", config(IslandTestTags.PILL).getOrNull(SemanticsProperties.StateDescription))
        assertNull(config(IslandTestTags.PILL).getOrNull(SemanticsActions.OnClick))
    }

    @Test
    fun minimisedRing_readsMinimised_restore() {
        h.onState { IslandStateManager.updateNotification(IslandFixtures.timer) }
        h.settle()
        h.onState { IslandStateManager.minimizeIsland() }
        h.settle()
        assertEquals("Island minimised", headline(IslandTestTags.PILL))
        assertEquals("Timer", config(IslandTestTags.PILL).getOrNull(SemanticsProperties.StateDescription))
        node(IslandTestTags.PILL).assert(A11yMatchers.hasClickLabel("Restore"))
    }

    @Test
    fun dualMode_bubbleHasItsOwnLabel() {
        h.onState {
            IslandStateManager.updateMedia(IslandFixtures.song)
            IslandStateManager.updateNotification(IslandFixtures.timer)
        }
        h.settle()
        assertTrue("dual mode shows the bubble", h.exists(IslandTestTags.BUBBLE))
        val pill = headline(IslandTestTags.PILL)
        val bubble = headline(IslandTestTags.BUBBLE)
        assertTrue("pill $pill, bubble $bubble", setOf(pill, bubble) == setOf("Timer", "Now playing: Song, Artist"))
        node(IslandTestTags.BUBBLE).assert(A11yMatchers.hasClickLabel("Expand"))
    }

    // --- C3.5: custom actions -------------------------------------------------------------

    private fun runAction(tag: String, label: String) {
        val actions = config(tag).getOrNull(SemanticsActions.CustomActions).orEmpty()
        val action = actions.firstOrNull { it.label == label } ?: error("No '$label' on $tag: ${actions.map { it.label }}")
        rule.runOnIdle { action.action() }
        h.settle()
    }

    private fun actionLabels(tag: String) = config(tag).getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label }

    @Test
    fun timerPill_actionsMenu() {
        h.onState { IslandStateManager.updateNotification(IslandFixtures.timer) }
        h.settle()
        assertEquals(listOf("Minimise", "Hide until next update"), actionLabels(IslandTestTags.PILL).take(2))
        runAction(IslandTestTags.PILL, "Minimise")
        assertTrue(IslandStateManager.userMinimized.value)
        // The ring offers only "Hide until next update"
        assertEquals(listOf("Hide until next update"), actionLabels(IslandTestTags.PILL))
    }

    @Test
    fun messagePill_dismissClearsIt_hideHidesTheIsland() {
        h.onState { IslandStateManager.updateNotification(IslandFixtures.message()) }
        h.settle()
        assertTrue("Dismiss" in actionLabels(IslandTestTags.PILL))
        runAction(IslandTestTags.PILL, "Dismiss")
        assertNull(IslandStateManager.activeNotification.value)

        h.onState { IslandStateManager.updateNotification(IslandFixtures.message("m2")) }
        h.settle()
        runAction(IslandTestTags.PILL, "Hide until next update")
        assertTrue(IslandStateManager.userHidden.value)
    }

    @Test
    fun ringingCall_hasNoDismiss_anywhere() {
        // A ringing call opens its card by itself
        h.onState {
            IslandStateManager.updateNotification(IslandFixtures.incomingCall)
            IslandStateManager.expand(null)
        }
        h.settle()
        assertTrue(IslandStateManager.isExpanded.value)
        assertEquals(listOf("Collapse"), actionLabels(IslandTestTags.CARD))
        // Collapsed (tucked), the pill offers no Dismiss either
        runAction(IslandTestTags.CARD, "Collapse")
        assertTrue(h.exists(IslandTestTags.PILL))
        assertTrue("Dismiss" !in actionLabels(IslandTestTags.PILL))
    }

    @Test
    fun mediaCard_collapseAndDismiss() {
        h.onState {
            IslandStateManager.updateMedia(IslandFixtures.song)
            IslandStateManager.expand(null)
        }
        h.settle()
        assertEquals(listOf("Collapse", "Dismiss"), actionLabels(IslandTestTags.CARD))
        runAction(IslandTestTags.CARD, "Collapse")
        assertTrue(!IslandStateManager.isExpanded.value)
    }
}
