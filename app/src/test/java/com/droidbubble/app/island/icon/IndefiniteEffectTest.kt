package com.droidbubble.app.island.icon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.MotionTier
import com.droidbubble.app.model.IslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** I1.2: pulse, breathe and rotate run in the layer only, follow the tier, and stop cleanly. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class IndefiniteEffectTest {

    @get:Rule
    val rule = createComposeRule()

    private val state = IconEffectState()
    private val probe = IconRecompositionProbe()
    private var active by mutableStateOf(true)

    private fun host(effect: IconEffect, tier: MotionTier = MotionTier.FULL) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            WithIslandMotion(tier) {
                SideEffect { probe.hit() }
                Box(Modifier.size(11.dp).iconEffect(effect, active = active, state = state))
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        probe.reset()
    }

    @Test
    fun breathe_recomposesNothingOver120Frames() {
        host(IconEffect.Breathe(minAlpha = 0.35f, scaleFrom = 1.04f, scaleTo = 1.12f, periodMillis = 1300))
        val alphas = rule.sampleFrames(120) { state.alpha }
        assertEquals(0, probe.count)
        assertTrue("fades: ${alphas.min()}..${alphas.max()}", alphas.min() < 0.45f && alphas.max() > 0.95f)
        val scales = rule.sampleFrames(80) { state.totalScale }
        assertTrue(scales.min() >= 1.04f - 1e-4f && scales.max() > 1.1f)
    }

    @Test
    fun rotate_turns() {
        host(IconEffect.Rotate())
        val turns = rule.sampleFrames(36) { state.totalRotation }
        assertTrue("turning: ${turns.last()}°", turns.last() > 150f)
    }

    @Test
    fun inactive_dropsBackToRest() {
        host(IconEffect.Breathe(minAlpha = 0.35f, scaleFrom = 1.04f, scaleTo = 1.12f))
        rule.sampleFrames(20) { state.alpha }
        active = false
        rule.sampleFrames(2) { state.alpha }
        assertEquals(1f, state.alpha, 0f)
        assertEquals(1f, state.totalScale, 0f)
    }

    @Test
    fun reduced_breatheKeepsASlowFadeButNoSizeChange() {
        host(IconEffect.Breathe(minAlpha = 0.35f, scaleFrom = 1.04f, scaleTo = 1.12f, periodMillis = 1300), MotionTier.REDUCED)
        val alphas = rule.sampleFrames(125) { state.alpha }
        val scales = rule.sampleFrames(10) { state.totalScale }
        assertTrue(scales.all { it == 1.04f })
        assertTrue("still fades: ${alphas.min()}", alphas.min() < 0.45f)
        // At 2 s a period, it is still fading in a second in (it would be back down at 1.3 s)
        val firstSecond = alphas.take(60)
        assertTrue(firstSecond.zipWithNext().count { (a, b) -> b < a } < 3)
    }
}

/** I1.2: the tier table for loops. Pure JUnit. */
class ResolveLoopTest {
    private fun motion(tier: MotionTier) = IslandMotion.from(IslandSettings(), tier)

    @Test
    fun efficient_capsTheFrameRate() {
        val l = IconEffectSpecs.resolveLoop(IconEffect.Pulse(), motion(MotionTier.EFFICIENT))
        assertEquals(IslandMotion.EFFICIENT_FRAME_MS, l.frameIntervalMillis)
        assertEquals(0, IconEffectSpecs.resolveLoop(IconEffect.Pulse(), motion(MotionTier.FULL)).frameIntervalMillis)
    }

    @Test
    fun reduced_table() {
        val m = motion(MotionTier.REDUCED)
        val pulse = IconEffectSpecs.resolveLoop(IconEffect.Pulse(), m)
        assertEquals(IslandMotion.Symbol.PULSE_REDUCED_PERIOD_MS, pulse.periodMillis)
        val breathe = IconEffectSpecs.resolveLoop(IconEffect.Breathe(), m)
        assertEquals(breathe.scaleFrom, breathe.scaleTo, 0f)
        assertTrue(IconEffectSpecs.resolveLoop(IconEffect.Rotate(), m).isStatic)
        assertTrue("a size-only breathe holds still", IconEffectSpecs.resolveLoop(IconEffect.Breathe(minAlpha = 1f, scaleFrom = 0.85f, scaleTo = 1f), m).isStatic)
        val removed = IconEffectSpecs.resolveLoop(IconEffect.Pulse(), m, animationsRemoved = true)
        assertTrue(removed.isStatic)
        assertEquals(0.7f, removed.minAlpha, 0f)
    }
}
