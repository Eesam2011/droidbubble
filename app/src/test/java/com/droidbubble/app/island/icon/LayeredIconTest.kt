package com.droidbubble.app.island.icon

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.IslandIcons
import com.droidbubble.app.island.motion.MotionTier
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** I1.4: the level-to-layers rule. Pure JUnit. */
class LitLayersTest {
    @Test
    fun edges() {
        assertEquals(0, litLayers(0f, 3))
        assertEquals(1, litLayers(0.01f, 3))
        assertEquals(1, litLayers(1f / 3f, 3))
        assertEquals(2, litLayers(0.34f, 3))
        assertEquals(3, litLayers(1f, 3))
        assertEquals(3, litLayers(1.4f, 3))
        assertEquals(0, litLayers(-1f, 3))
    }
}

/** I1.4: a level that ticks fast recomposes nothing; every mode and tier composes. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LayeredIconTest {

    @get:Rule
    val rule = createComposeRule()

    private var level by mutableFloatStateOf(0f)
    private val probe = IconRecompositionProbe()

    @Test
    fun levelTicking10TimesASecond_recomposesNothing() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            WithIslandMotion {
                SideEffect { probe.hit() }
                LayeredIcon(IslandIcons.Compact.Download, Color.White, level = { level })
            }
        }
        rule.mainClock.advanceTimeByFrame()
        probe.reset()
        repeat(30) {
            level = (it + 1) / 30f
            rule.sampleFrames(6) { 0f }
        }
        assertEquals(0, probe.count)
    }

    @Test
    fun everyModeAndTier_composes() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            androidx.compose.foundation.layout.Column {
                for (tier in MotionTier.entries) for (mode in VariableMode.entries) {
                    WithIslandMotion(tier) {
                        LayeredIcon(IslandIcons.Compact.Wifi, Color.White, level = { 0.5f }, mode = mode, reversing = mode == VariableMode.Iterative)
                    }
                }
            }
        }
        rule.sampleFrames(30) { 0f }
    }
}
