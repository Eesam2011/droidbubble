package com.droidbubble.app.island.icon

import androidx.compose.material3.Icon
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.IslandIcons
import com.droidbubble.app.island.motion.MotionTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** I2.3: flashlight, mic and mute slashes: probe 0 during the slash, onSlashed fires once, every tier renders. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SlashGlyphsTest {

    @get:Rule
    val rule = createComposeRule()

    private var slashed by mutableStateOf(false)
    private val probe = IconRecompositionProbe()

    @Test
    fun torchSlash_firesOnSlashedOnce_partWay() {
        var fired = 0
        var firedAtFrame = -1
        var frame = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            WithIslandMotion {
                SideEffect { probe.hit() }
                SlashOverlay(slashed = slashed, slashTint = Color.White, onSlashed = { fired++; firedAtFrame = frame }) {
                    Icon(IslandIcons.Flashlight, null, tint = Color.White)
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        probe.reset()
        slashed = true
        repeat(40) {
            frame++
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        assertEquals(1, fired)
        assertTrue("before the slash settles: frame $firedAtFrame", firedAtFrame in 2..20)
        assertEquals(1, probe.count)
    }

    @Test
    fun everyGlyph_everyTier_renders() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            androidx.compose.foundation.layout.Column {
                for (tier in MotionTier.entries) WithIslandMotion(tier) {
                    SlashOverlay(slashed = slashed, slashTint = Color.White) { Icon(IslandIcons.Compact.Flashlight, null) }
                    SlashOverlay(slashed = slashed, slashTint = Color.White) { Icon(IslandIcons.Compact.Mic, null) }
                    MuteGlyph(muted = slashed, tint = Color.White)
                }
            }
        }
        rule.sampleFrames(3) { 0f }
        slashed = true
        rule.sampleFrames(30) { 0f }
    }
}
