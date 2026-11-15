package com.droidbubble.app.island.layout

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.IslandOverlayRoot
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.IslandTestTags
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** V1.3: the pill's camera gap is the measured camera zone, not a fixed 36 dp. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CameraZoneTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun reset() = IslandStateManager.resetForTest()
    @After fun tearDown() = IslandStateManager.resetForTest()

    private fun gapWith(zone: Dp?): Dp {
        rule.setContent {
            if (zone == null) {
                IslandOverlayRoot(settings = IslandSettings())
            } else {
                CompositionLocalProvider(LocalCameraZone provides zone) { IslandOverlayRoot(settings = IslandSettings()) }
            }
        }
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
        val lead = rule.onAllNodesWithTag(IslandTestTags.COMPACT_LEADING, useUnmergedTree = true)[0].getBoundsInRoot()
        val trail = rule.onAllNodesWithTag(IslandTestTags.COMPACT_TRAILING, useUnmergedTree = true)[0].getBoundsInRoot()
        return trail.left - lead.right
    }

    @Test
    fun theGapIsTheProvidedZone() {
        assertEquals(40f, gapWith(40.dp).value, 0.5f)
    }

    @Test
    fun withoutAMeasuredCamera_theGapIsToday36() {
        assertEquals(36f, gapWith(null).value, 0.5f)
    }
}
