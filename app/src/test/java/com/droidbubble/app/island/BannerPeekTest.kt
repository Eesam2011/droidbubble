package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** D2.2 on the island: a peeking message recedes on its own; a finger on it keeps it open. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BannerPeekTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandStateManager.peekBanners = true
        h.setContent(mutableStateOf(IslandSettings(showNotifications = true)))
    }

    @After
    fun tearDown() = IslandStateManager.resetForTest()

    @Test
    fun aPeek_recedesAfterItsTime() {
        rule.runOnIdle { IslandStateManager.updateNotification(IslandFixtures.message()) }
        h.settle(1_000)
        assertTrue("the card peeks open", IslandStateManager.isExpanded.value)
        h.settle(IslandStateManager.timings.peek)
        assertFalse("then recedes", IslandStateManager.isExpanded.value)
        assertTrue("the message stays in the pill", IslandStateManager.activeNotification.value != null)
    }

    @Test
    fun aFingerOnIt_keepsItOpen() {
        rule.runOnIdle { IslandStateManager.updateNotification(IslandFixtures.message()) }
        h.settle(500)
        rule.onAllNodesWithTag(IslandTestTags.CARD)[0].performTouchInput { down(center) }
        h.settle(IslandStateManager.timings.peek + 1_000)
        assertTrue(IslandStateManager.isExpanded.value)
        assertFalse(IslandStateManager.isPeeking.value)
        rule.onAllNodesWithTag(IslandTestTags.CARD)[0].performTouchInput { cancel() }
    }
}
