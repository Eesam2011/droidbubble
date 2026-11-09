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

/**
 * B6.3: long-pressing a notification pill opens it like a tap in the shade. An auto-cancel
 * notification clears; any other keeps its card (before, long-press always dismissed it).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandLongPressTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        h.setContent(mutableStateOf(IslandSettings(showNotifications = true)))
    }

    @After
    fun tearDown() = IslandStateManager.resetForTest()

    private fun postAndLongPress(autoCancel: Boolean) {
        h.onState { IslandStateManager.updateNotification(IslandFixtures.message().copy(isAutoCancel = autoCancel)) }
        h.settle()
        assertEquals(IslandPriority.POP_NOTIFICATION, h.contentOf("pill"))
        h.longPress(IslandTestTags.PILL)
        h.settle()
    }

    @Test
    fun longPress_keepsANonAutoCancelCard() {
        postAndLongPress(autoCancel = false)
        assertEquals("msg", IslandStateManager.activeNotification.value?.key)
    }

    @Test
    fun longPress_clearsAnAutoCancelCard() {
        postAndLongPress(autoCancel = true)
        assertNull(IslandStateManager.activeNotification.value)
    }
}
