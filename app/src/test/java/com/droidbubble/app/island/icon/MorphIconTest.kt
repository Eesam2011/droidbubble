package com.droidbubble.app.island.icon

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.motion.MotionTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** I2.1: the play / pause parts stay convex all the way through, and their outlines match the glyphs. Pure JUnit. */
class MorphShapesTest {

    private fun isConvex(pts: List<Offset>): Boolean {
        val u = pts.filterIndexed { i, c -> c != pts[(i + pts.size - 1) % pts.size] }
        var sign = 0
        for (i in u.indices) {
            val a = u[i]; val b = u[(i + 1) % u.size]; val c = u[(i + 2) % u.size]
            val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            if (kotlin.math.abs(cross) < 1e-4f) continue
            val s = if (cross > 0) 1 else -1
            if (sign == 0) sign = s else if (s != sign) return false
        }
        return true
    }

    @Test
    fun everyFrame_isConvex_neverFolds() {
        for ((a, b) in MorphShapes.PlayPause.parts) for (k in -2..12) {
            val p = k / 10f   // a little past both ends too: the spring overshoots (clamped)
            val (core, _) = lerpQuad(a, b, p)
            assertTrue("part at p=$p: $core", isConvex(core))
        }
    }

    @Test
    fun ends_areTheShapes() {
        for ((a, b) in MorphShapes.PlayPause.parts) {
            assertEquals(a.core, lerpQuad(a, b, 0f).first)
            assertEquals(b.core, lerpQuad(a, b, 1f).first)
        }
    }

    @Test
    fun inset_thenRoundedStroke_givesTheOutlineBack() {
        // A bar 6.3..10.1 inset by 1.5 is 7.8..8.6; stroked 1.5 out each side it is 6.3..10.1 again
        val bar = MorphShapes.PlayPause.parts[0].second
        assertEquals(7.8f, bar.core.minOf { it.x }, 1e-3f)
        assertEquals(8.6f, bar.core.maxOf { it.x }, 1e-3f)
        assertEquals(6.3f, bar.core.minOf { it.y }, 1e-3f)
    }
}

/** I2.1: a toggle recomposes once; the morph frames don't recompose; it compiles in every tier. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class MorphIconTest {

    @get:Rule
    val rule = createComposeRule()

    private var playing by mutableStateOf(false)
    private val probe = IconRecompositionProbe()

    private fun host(tier: MotionTier) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            WithIslandMotion(tier) {
                SideEffect { probe.hit() }
                MorphIcon(MorphShapes.PlayPause, atEnd = playing, tint = Color.White, contentDescription = "Play/Pause")
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        probe.reset()
    }

    @Test
    fun toggle_recomposesOnce_evenWhenReversedMidway() {
        host(MotionTier.FULL)
        playing = true
        rule.sampleFrames(5) { 0f }
        playing = false
        rule.sampleFrames(40) { 0f }
        assertEquals(2, probe.count)
    }

    @Test
    fun reduced_toggles() {
        host(MotionTier.REDUCED)
        playing = true
        rule.sampleFrames(15) { 0f }
        assertEquals(1, probe.count)
    }
}
