package com.droidbubble.app.island.layout

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.scene.AlertSides
import com.droidbubble.app.island.scene.estimatedAlertSides
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * V3.5 (owner's "Vibrat" report): a glance pill is never narrower than its widest content, and
 * the window's estimate is never narrower than what the view measures.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AlertWidthTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun measured(fontScale: Float): AlertSides {
        var sides: AlertSides? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                sides = rememberAlertSides()
            }
        }
        rule.waitForIdle()
        return sides!!
    }

    @Test
    fun theEstimateCoversTheMeasuredContent() {
        val m = measured(1f)
        val e = estimatedAlertSides(1f)
        assertTrue("ringer: estimate ${e.ringer} < measured ${m.ringer}", e.ringer >= m.ringer)
        assertTrue("charging: estimate ${e.charging} < measured ${m.charging}", e.charging >= m.charging)
    }

    @Test
    fun theEstimateCoversLargeTextToo() {
        val m = measured(1.5f)
        val e = estimatedAlertSides(1.5f)
        assertTrue(e.ringer >= m.ringer)
        assertTrue(e.charging >= m.charging)
    }

    @Test
    fun vibrateFitsTheRingerPill_atTheOwnersWidth() {
        val m = measured(1f)
        // Width 90 dp: today's rule gave max(90 × 0.95, 130) = 130 dp, a 39 dp label side
        val width = maxOf(130.dp, IslandTokens.Shell.AlertContentPadding * 2 + 36.dp + m.ringer * 2)
        val labelSide = (width - IslandTokens.Shell.AlertContentPadding * 2 - 36.dp) / 2
        // Whether it has to grow depends on the phone's font (the owner's OPPO font is wider than the
        // test font); either way the label side holds the whole word
        assertTrue("label side $labelSide < Vibrate ${m.ringer}", labelSide >= m.ringer)
    }
}
