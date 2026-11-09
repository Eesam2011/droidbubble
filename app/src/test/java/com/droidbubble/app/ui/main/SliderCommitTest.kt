package com.droidbubble.app.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** B8.7: a slider drag writes the setting once, on release, while its label follows the finger. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SliderCommitTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun longDrag_commitsOnce_onRelease() {
        val commits = mutableListOf<Float>()
        rule.setContent {
            SliderSettingItem(label = "Width", value = 180f, unit = "dp", valueRange = 100f..300f, onCommit = { commits += it })
        }
        val slider = rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.performTouchInput {
            down(center)
            // A slow 2 s drag in many small steps
            repeat(100) { moveBy(Offset(width / 400f, 0f), delayMillis = 20) }
        }
        assertTrue("nothing written while the finger is down", commits.isEmpty())
        slider.performTouchInput { up() }
        rule.waitForIdle()
        assertEquals("one write when the drag ends", 1, commits.size)
        assertTrue("the value moved right", commits.single() > 180f)
        // The label shows the dragged value
        val label = String.format(java.util.Locale.US, "%.1f", commits.single()).replace(".0", "") + " dp"
        rule.onNodeWithText(label, useUnmergedTree = true).assertExists()
    }
}
