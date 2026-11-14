package com.droidbubble.app.island.shell

import androidx.compose.animation.core.spring
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandGeometry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** M1.1: retargeting the body mid-flight keeps its velocity, so the shape never jumps. */
class IslandShellStateTest {

    /** A 60 Hz frame clock that lets other coroutines run between frames. */
    private class StepClock : MonotonicFrameClock {
        var frame = 0L
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            yield()
            frame++
            return onFrame(frame * 16_666_667L)
        }
    }

    private val pill = IslandGeometry(0.dp, 36.dp, 22.dp, 12.dp)
    private val card = IslandGeometry(374.dp, 214.5.dp, 40.dp, 64.dp)
    private val spec = spring(dampingRatio = 0.8f, stiffness = 380f * 0.49f, visibilityThreshold = IslandGeometry.VisibilityThreshold)
    private val collapse = spring(dampingRatio = 1f, stiffness = 380f * 1.4f * 0.49f, visibilityThreshold = IslandGeometry.VisibilityThreshold)

    private fun widthSteps(samples: List<IslandGeometry>) = samples.zipWithNext { a, b -> abs(b.width.value - a.width.value) }

    @Test
    fun uninterrupted_settlesOnTarget() = runBlocking(StepClock()) {
        val shell = IslandShellState(pill, 0.dp)
        shell.animateBodyTo(card, spec)
        assertEquals(card, shell.geometry)
        assertTrue(shell.isSettled)
    }

    @Test
    fun retargetMidFlight_keepsVelocity_andNeverJumps() {
        val clock = StepClock()
        val samples = ArrayList<IslandGeometry>()
        var velocityBefore = 0f
        var velocityAfterFirstFrame = 0f
        runBlocking(clock) {
            val shell = IslandShellState(pill, 0.dp)
            launch { shell.animateBodyTo(card, spec) }
            while (clock.frame < 12) { samples += shell.geometry; yield() }
            velocityBefore = shell.velocity.width.value
            assertTrue("moving fast toward the card", velocityBefore > 100f)
            val back = launch { shell.animateBodyTo(pill, collapse) }
            val frameAtRetarget = clock.frame
            while (clock.frame == frameAtRetarget) yield()
            velocityAfterFirstFrame = shell.velocity.width.value
            while (back.isActive) { samples += shell.geometry; yield() }
            samples += shell.geometry
            assertEquals(pill, shell.geometry)
        }
        // The new spring starts with the old velocity: still moving outward on its first frame
        assertTrue("velocity carried over ($velocityBefore → $velocityAfterFirstFrame)", velocityAfterFirstFrame > 0f)
        // No jump at the retarget: the first step after it is no bigger than the step before it
        // (the shape decelerates from where it was, instead of snapping toward the new target)
        val steps = widthSteps(samples)
        val before = steps[10]
        val after = steps[11]
        assertTrue("step before $before dp, after $after dp", after <= before * 1.05f)
    }

    @Test
    fun animateTo_movesShapeAndPositionTogether() = runBlocking(StepClock()) {
        val shell = IslandShellState(pill, 14.dp)
        shell.animateTo(card, 0.dp, spec, spring(dampingRatio = 0.8f, stiffness = 380f * 0.49f))
        assertEquals(card, shell.geometry)
        assertEquals(0f, shell.x.value, 0.01f)
        assertTrue(shell.isSettled)
    }

    @Test
    fun explicitVelocity_replacesTheCurrentOne() = runBlocking(StepClock()) {
        val shell = IslandShellState(card, 0.dp)
        // A fling upward: the body leaves with the finger's velocity, overshooting the pill's height first
        var minHeight = Float.MAX_VALUE
        shell.body.animateTo(pill, collapse, IslandGeometry(0.dp, (-8000).dp, 0.dp, (-8000).dp)) { minHeight = minOf(minHeight, value.height.value) }
        assertTrue("the fling carried the body past the pill ($minHeight)", minHeight < pill.height.value)
    }

    // ---- M4.3: drag and velocity hand-off ----

    @Test
    fun dragUp_shortensFromTheBottom_rubberBanded_neverBelowTheFloor() {
        val up = dragGeometry(card, (-40).dp, minHeight = 36.dp, band = 120.dp)
        assertEquals(card.top, up.top)
        assertEquals(card.width, up.width)
        val shortened = card.height.value - up.height.value
        assertTrue("follows less than the finger ($shortened dp of 40)", shortened in 1f..39.9f)
        val far = dragGeometry(card, (-10_000).dp, minHeight = 200.dp, band = 120.dp)
        assertEquals(200f, far.height.value, 0.01f)
    }

    @Test
    fun dragDown_lowersTheTop_withAShortBand() {
        val down = dragGeometry(card, 400.dp, minHeight = 36.dp, band = 120.dp)
        assertEquals(card.height, down.height)
        val dropped = down.top.value - card.top.value
        assertTrue("a small give ($dropped dp)", dropped > 0f && dropped < 120f * DOWN_DRAG_BAND_FRACTION)
    }

    @Test
    fun gestureVelocity_movesEveryValueAlongTheMorph_atTheFingersHeightSpeed() {
        val closed = IslandGeometry(0.dp, 36.dp, 18.dp, 12.dp)
        // Swipe up at 1000 dp/s from the card toward the pill
        val v = gestureVelocity(-1000f, card, closed)
        assertEquals(-1000f, v.height.value, 0.5f)
        val f = 1000f / (card.height.value - closed.height.value)
        assertEquals((closed.width.value - card.width.value) * f, v.width.value, 0.5f)
        assertEquals((closed.top.value - card.top.value) * f, v.top.value, 0.5f)
        // Against the morph (moving down while tucking): nothing carries over
        assertEquals(0f, gestureVelocity(800f, card, closed).height.value, 0.001f)
        // Absurd flings are capped
        assertEquals(-MAX_HAND_OFF_DP_PER_SEC, gestureVelocity(-50_000f, card, closed).height.value, 0.5f)
    }

    @Test
    fun handOff_isTakenOnce_andExpires() {
        val shell = IslandShellState(card, 0.dp)
        val v = IslandGeometry(0.dp, (-900).dp, 0.dp, 0.dp)
        shell.handOff(v, nowNanos = 0L)
        assertEquals(v, shell.takeHandOff(nowNanos = 100_000_000L))
        assertEquals(null, shell.takeHandOff(nowNanos = 100_000_000L))
        shell.handOff(v, nowNanos = 0L)
        assertEquals("stale after the TTL", null, shell.takeHandOff(nowNanos = HAND_OFF_TTL_NANOS + 1))
    }

    @Test
    fun release_springsBackToTheTarget() = runBlocking(StepClock()) {
        val shell = IslandShellState(card, 0.dp)
        shell.dragBy(card, (-30).dp, minHeight = 36.dp, band = 120.dp)
        assertTrue(shell.geometry.height < card.height)
        shell.release(card, spec, IslandGeometry(0.dp, (-400).dp, 0.dp, 0.dp))
        assertEquals(card, shell.geometry)
    }
}
