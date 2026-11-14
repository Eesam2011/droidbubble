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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** I2.2: the trimmed length. Pure JUnit. */
class DrawnLengthTest {
    @Test
    fun segments() {
        assertEquals(0f, drawnLength(10f, 0f), 0f)
        assertEquals(5f, drawnLength(10f, 0.5f), 1e-4f)
        assertEquals(10f, drawnLength(10f, 1f), 0f)
        assertEquals("nothing before the delay", 0f, drawnLength(10f, 0.3f, delayFraction = 0.3f), 0f)
        assertEquals(5f, drawnLength(10f, 0.65f, delayFraction = 0.3f), 1e-3f)
    }
}

/** I2.2: drawing the check and the slash recomposes nothing per frame, in every tier. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DrawOnIconTest {

    @get:Rule
    val rule = createComposeRule()

    private var on by mutableStateOf(false)
    private val probe = IconRecompositionProbe()

    private fun hostCheck(tier: MotionTier) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            WithIslandMotion(tier) {
                SideEffect { probe.hit() }
                DrawOnIcon(IconStrokes.Check, drawn = on, tint = Color.White, delayFraction = 0.3f)
            }
        }
        rule.mainClock.advanceTimeByFrame()
        probe.reset()
    }

    @Test
    fun check_drawsWithOneRecomposition() {
        for (tier in listOf(MotionTier.FULL)) {
            hostCheck(tier)
            on = true
            rule.sampleFrames(40) { 0f }
            assertEquals(1, probe.count)
        }
    }

    @Test
    fun slash_drawsWithOneRecompositionPlusTheSettle() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            WithIslandMotion {
                SideEffect { probe.hit() }
                SlashOverlay(slashed = on, slashTint = Color.White, slashedGlyph = {
                    Icon(IslandIcons.Compact.RingerSilent, null, tint = Color.White)
                }) { Icon(IslandIcons.Compact.RingerOn, null, tint = Color.White) }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        probe.reset()
        on = true
        rule.sampleFrames(40) { 0f }
        on = false
        rule.sampleFrames(40) { 0f }
        // The host recomposes for each toggle only; the slash and its settle stay inside SlashOverlay
        assertEquals(2, probe.count)
    }

    @Test
    fun reducedComposes() {
        hostCheck(MotionTier.REDUCED)
        on = true
        rule.sampleFrames(15) { 0f }
        assertEquals(1, probe.count)
    }
}
