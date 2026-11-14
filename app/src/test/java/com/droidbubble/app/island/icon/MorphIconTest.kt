package com.droidbubble.app.island.icon

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.graphics.shapes.RoundedPolygon
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.motion.MotionTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** I2.1: the play / pause shapes are convex and the morph lands exactly on them. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class MorphShapesTest {

    /** Bounds of the curves themselves, sampled (anchors alone miss the rounded corners' extent). */
    private fun bounds(cubics: List<androidx.graphics.shapes.Cubic>): FloatArray {
        val xs = ArrayList<Float>()
        val ys = ArrayList<Float>()
        for (c in cubics) for (k in 0..20) {
            val t = k / 20f
            val u = 1f - t
            fun bez(a: Float, b: Float, c2: Float, d: Float) = u * u * u * a + 3 * u * u * t * b + 3 * u * t * t * c2 + t * t * t * d
            xs += bez(c.anchor0X, c.control0X, c.control1X, c.anchor1X)
            ys += bez(c.anchor0Y, c.control0Y, c.control1Y, c.anchor1Y)
        }
        return floatArrayOf(xs.min(), ys.min(), xs.max(), ys.max())
    }

    private fun isConvex(p: RoundedPolygon): Boolean {
        val pts = p.cubics.map { it.anchor0X to it.anchor0Y }
        var sign = 0
        for (i in pts.indices) {
            val (ax, ay) = pts[i]
            val (bx, by) = pts[(i + 1) % pts.size]
            val (cx, cy) = pts[(i + 2) % pts.size]
            val cross = (bx - ax) * (cy - by) - (by - ay) * (cx - bx)
            if (kotlin.math.abs(cross) < 1e-4f) continue
            val s = if (cross > 0) 1 else -1
            if (sign == 0) sign = s else if (s != sign) return false
        }
        return true
    }

    @Test
    fun partsAreConvex() {
        for ((a, b) in MorphShapes.PlayPause.parts) {
            assertTrue(isConvex(a))
            assertTrue(isConvex(b))
        }
    }

    @Test
    fun morphEndsMatchTheShapes() {
        MorphShapes.PlayPause.morphs.forEachIndexed { i, m ->
            val (a, b) = MorphShapes.PlayPause.parts[i]
            val at0 = bounds(m.asCubics(0f))
            val at1 = bounds(m.asCubics(1f))
            val wantA = bounds(a.cubics)
            val wantB = bounds(b.cubics)
            for (k in 0..3) {
                assertEquals("part $i start [$k]", wantA[k], at0[k], 24f * 0.005f)
                assertEquals("part $i end [$k]", wantB[k], at1[k], 24f * 0.005f)
            }
        }
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
