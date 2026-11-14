package com.droidbubble.app.island.icon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.motion.MotionTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.abs

/** I1.3: a replace never cuts, retargets from the current frame and recomposes once per change. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DynamicIconSwitchTest {

    @get:Rule
    val rule = createComposeRule()

    private var target by mutableStateOf("A")
    private val probe = IconRecompositionProbe()
    private val slots = mutableStateListOf(SwitchSlot("A", 1f))

    private fun host(tier: MotionTier = MotionTier.FULL) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            WithIslandMotion(tier) {
                SideEffect { probe.hit() }
                DynamicIconSwitch(target, slots) { Box(Modifier.size(18.dp)) }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        probe.reset()
    }

    private fun progressOf(value: String) = slots.firstOrNull { it.value == value }?.progress?.value ?: 0f
    private fun visibleSum() = slots.sumOf { it.progress.value.coerceIn(0f, 1f).toDouble() }.toFloat()

    @Test
    fun replace_neverBlank_andSettlesOnTheNewGlyph() {
        host()
        target = "B"
        val sums = rule.sampleFrames(40) { visibleSum() }
        assertTrue("never blank: ${sums.min()}", sums.min() > 0.5f)
        assertEquals(listOf("B"), slots.map { it.value })
        assertEquals(1f, progressOf("B"), 0.01f)
    }

    @Test
    fun aToBToA_within100ms_endsOnA_withoutAJump() {
        host()
        target = "B"
        val leaving = rule.sampleFrames(3) { progressOf("A") }
        target = "A"
        val back = rule.sampleFrames(40) { progressOf("A") }
        val steps = (listOf(1f) + leaving + back).zipWithNext { a, b -> abs(b - a) }
        assertTrue("A's progress never jumps: ${steps.max()}", steps.max() < 0.35f)
        assertEquals(listOf("A"), slots.map { it.value })
        assertEquals(1f, progressOf("A"), 0.01f)
    }

    @Test
    fun recomposesOncePerTargetChange() {
        host()
        target = "B"
        rule.sampleFrames(30) { 0f }
        target = "C"
        rule.sampleFrames(30) { 0f }
        assertEquals(2, probe.count)
    }

    @Test
    fun reduced_isALinearCrossfade() {
        host(MotionTier.REDUCED)
        target = "B"
        val sums = rule.sampleFrames(14) { visibleSum() }
        assertTrue("crossfade never dips: ${sums.min()}", sums.min() > 0.9f)
        assertEquals(listOf("B"), slots.map { it.value })
    }
}

/** I1.3: appear and disappear. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DynamicIconVisibilityTest {

    @get:Rule
    val rule = createComposeRule()

    private var visible by mutableStateOf(false)
    private var composed = false

    @Test
    fun appearsThenLeaves_andIsRemovedWhenGone() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            WithIslandMotion {
                composed = false
                DynamicIconVisibility(visible) { SideEffect { composed = true }; Box(Modifier.size(18.dp)) }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        assertTrue(!composed)
        visible = true
        rule.sampleFrames(3) { 0f }
        assertTrue(composed)
        visible = false
        rule.sampleFrames(30) { 0f }
        composed = false
        visible = false
        rule.sampleFrames(2) { 0f }
        assertTrue("content leaves composition once faded", !composed)
    }
}
