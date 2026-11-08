package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The content probes (S1.2): tests can tell which activity each slot draws, without pixels. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandContentProbeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)
    private val settings = mutableStateOf(IslandSettings(showNotifications = true))

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        rule.mainClock.autoAdvance = true
        h.setContent(settings)
    }

    @After
    fun tearDown() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
    }

    @Test
    fun idle_showsNothing() {
        h.settle()
        for (slot in IslandTestTags.CONTENT_SLOTS) assertNull(slot, h.contentOf(slot))
    }

    @Test
    fun media_isInThePill() {
        h.onState { IslandStateManager.updateMedia(IslandFixtures.song) }
        h.settle()
        assertEquals(IslandPriority.MEDIA, h.contentOf("pill"))
        assertNull(h.contentOf("bubble"))
    }

    @Test
    fun timer_isANotificationInThePill() {
        h.onState { IslandStateManager.updateNotification(IslandFixtures.timer) }
        h.settle()
        assertEquals(IslandPriority.POP_NOTIFICATION, h.contentOf("pill"))
    }

    @Test
    fun mediaAndMessage_mediaInPill_messageInBubble() {
        h.onState {
            IslandStateManager.updateMedia(IslandFixtures.song)
            IslandStateManager.updateNotification(IslandFixtures.message())
        }
        h.settle(5000) // past the message's pop-in
        assertEquals(IslandPriority.MEDIA, h.contentOf("pill"))
        assertEquals(IslandPriority.POP_NOTIFICATION, h.contentOf("bubble"))
    }

    @Test
    fun expandedMedia_isInCard1() {
        h.onState { IslandStateManager.updateMedia(IslandFixtures.song) }
        h.settle()
        h.onState { IslandStateManager.expand(null) }
        h.settle()
        assertEquals(IslandPriority.MEDIA, h.contentOf("card1"))
        assertNull(h.contentOf("card2"))
    }
}
