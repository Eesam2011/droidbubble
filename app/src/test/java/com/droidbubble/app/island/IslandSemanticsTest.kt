package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.A11yMatchers
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The island's TalkBack contract (phase 8, C3). C3.1 pins today's state; C3.4–C3.8 flip these
 * assertions as the labels, actions, pane titles and live regions land.
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

    @Test
    fun timerPill_isThere_andTappable() {
        h.onState { IslandStateManager.updateNotification(IslandFixtures.timer) }
        h.settle()
        assertTrue(h.exists(IslandTestTags.PILL))
        // Today: tappable, but with no click label, no description and no custom actions
        rule.onAllNodesWithTag(IslandTestTags.PILL)[0].assert(A11yMatchers.hasClickAction)
    }
}
