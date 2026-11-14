package com.droidbubble.app.island.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import com.droidbubble.app.model.IslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** F3.1: the spring formula, the tokens, speed scaling and the motion tiers. Pure JUnit. */
class IslandMotionTest {

    private val defaults = IslandSettings()
    private fun motion(speed: Float = 1f, tier: MotionTier = MotionTier.FULL, profile: IslandMotion.Profile = IslandMotion.Profile.Legacy) =
        IslandMotion.from(defaults.copy(animationSpeed = speed), tier, profile)

    @Test
    fun applesOwnExample() {
        // Spring(duration: 0.5, bounce: 0.3) is stiffness 157.9, damping ratio 0.70
        val p = MotionToken.Perceptual(0.5f, 0.3f).toPhysical()
        assertEquals(0.70f, p.dampingRatio, 0.0001f)
        assertEquals(157.9f, p.stiffness, 0.05f)
    }

    @Test
    fun referenceTable() {
        // research 03 §1 "Converted to Compose": duration, bounce, ζ, k (k as printed, rounded)
        val table = listOf(
            floatArrayOf(0.5f, 0f, 1.00f, 158f),       // .smooth
            floatArrayOf(0.5f, 0.15f, 0.85f, 158f),    // .snappy
            floatArrayOf(0.5f, 0.3f, 0.70f, 158f),     // .bouncy
            floatArrayOf(0.15f, 0.14f, 0.86f, 1755f),  // interactiveSpring
            floatArrayOf(0.42f, 0.2f, 0.80f, 224f),
            floatArrayOf(0.45f, 0f, 1.00f, 195f),
            floatArrayOf(0.32f, 0.2f, 0.80f, 380f),    // M3 Expressive default spatial (printed 380, exact 385.5)
            floatArrayOf(0.22f, 0.4f, 0.60f, 800f),    // fast spatial (exact 815.7)
            floatArrayOf(0.44f, 0.2f, 0.80f, 200f),    // slow spatial (exact 203.9)
        )
        for (row in table) {
            val p = MotionToken.Perceptual(row[0], row[1]).toPhysical()
            assertEquals("ζ of ${row[0]} / ${row[1]}", row[2], p.dampingRatio, 0.0001f)
            assertEquals("k of ${row[0]} / ${row[1]}", row[3], p.stiffness, row[3] * 0.025f)
        }
    }

    @Test
    fun negativeBounce_isOverdamped() {
        assertEquals(2f, MotionToken.Perceptual(0.5f, -0.5f).toPhysical().dampingRatio, 0.0001f)
    }

    @Test
    fun physical_roundTrips() {
        for (p in listOf(MotionToken.Physical(0.8f, 380f), MotionToken.Physical(0.6f, 280f), MotionToken.Physical(0.63f, 320f))) {
            val back = p.toPerceptual().toPhysical()
            assertEquals(p.dampingRatio, back.dampingRatio, 0.0001f)
            assertEquals(p.stiffness, back.stiffness, p.stiffness * 0.0001f)
        }
    }

    @Test
    fun userCurves_comeFromTheSettings_unchanged() {
        val m = IslandMotion.from(defaults.copy(expandDamping = 0.7f, expandStiffness = 300f, appearDamping = 0.5f, appearStiffness = 200f, splitDamping = 0.9f, splitStiffness = 500f))
        assertEquals(MotionToken.Physical(0.7f, 300f), m.expand)
        assertEquals(MotionToken.Physical(0.5f, 200f), m.appear)
        assertEquals(MotionToken.Physical(0.9f, 500f), m.split)
        assertEquals(MotionToken.Physical(1f, 300f * 1.4f), m.collapse)
    }

    @Test
    fun speed_scalesStiffnessBySpeedSquared() {
        for (speed in listOf(0.5f, 0.7f, 2.0f)) {
            val m = motion(speed)
            val spec: SpringSpec<Float> = m.spring(m.expand)
            assertEquals(0.8f, spec.dampingRatio, 0f)
            // The exact float expression the view used: stiffness * (speed * speed)
            assertEquals(380f * (speed * speed), spec.stiffness, 0f)
            assertEquals(380f * 1.4f * (speed * speed), m.spring<Float>(m.collapse).stiffness, 0f)
        }
    }

    @Test
    fun speed_isClamped() {
        assertEquals(0.5f, motion(0.1f).speed, 0f)
        assertEquals(2f, motion(5f).speed, 0f)
    }

    @Test
    fun press_isNotSpeedScaled() {
        val spec: SpringSpec<Float> = motion(2f).spring(motion(2f).press)
        assertEquals(700f, spec.stiffness, 0f)
        assertEquals(0.6f, spec.dampingRatio, 0f)
    }

    @Test
    fun legacyTokens_areTodaysValues() {
        val m = motion()
        assertEquals(MotionToken.Physical(0.82f, 380f), m.pillShrinkOnExpand)
        assertEquals(MotionToken.Physical(0.85f, 380f), m.bubbleRetract)
        assertEquals(MotionToken.Physical(0.76f, 420f), m.cardDrag)
        assertEquals(MotionToken.Physical(0.5f, 380f), m.swapSquashOut)
        assertEquals(MotionToken.Physical(0.65f, 260f), m.parenthesisShoot)
        assertEquals(MotionToken.Physical(0.75f, 400f, speedScaled = false), m.compactSwapScale)
        // Tokens that only exist in Droid stand in for today's values
        assertEquals(m.collapse, m.dismiss)
        assertEquals(m.bubbleRetract, m.merge)
        assertEquals(m.cardDrag, m.interactive)
        assertEquals(m.swapSquashOut, m.alertBump)
        val fade = m.effects<Float>() as TweenSpec<Float>
        assertEquals(130, fade.durationMillis)
    }

    @Test
    fun tween_dividesBySpeed() {
        val t: TweenSpec<Float> = motion(0.7f).tween(750, delayMillis = 60)
        assertEquals(Math.round(750 / 0.7f), t.durationMillis)
        assertEquals(Math.round(60 / 0.7f), t.delay)
        assertEquals(FastOutSlowInEasing, t.easing)
        assertEquals(65, motion(2f).scaled(130))
    }

    @Test
    fun droidProfile_values() {
        val m = motion(profile = IslandMotion.Profile.Droid)
        fun check(token: MotionToken.Physical, zeta: Float, k: Float) {
            assertEquals(zeta, token.dampingRatio, 0.0001f)
            assertEquals(k, token.stiffness, k * 0.005f)
        }
        check(m.collapse, 1.00f, 273.4f)
        check(m.dismiss, 1.00f, 385.5f)
        check(m.merge, 0.95f, 322.3f)
        check(m.alertBump, 0.55f, 438.6f)
        check(m.press, 0.70f, 987.0f)
        check(m.interactive, 0.90f, 1218.5f)
        check(m.iconBounce, 0.55f, 438.6f)
        check(m.iconWiggle, 0.65f, 631.7f)
        val effects = m.effects<Float>() as SpringSpec<Float>
        assertEquals(1600f, effects.stiffness, 0f)
        // The user's curves are never retuned
        assertEquals(motion().expand, m.expand)
        assertEquals(motion().appear, m.appear)
        assertEquals(motion().split, m.split)
    }

    @Test
    fun droidCollapse_onlyWhileExpandIsAtItsDefault() {
        val custom = IslandMotion.from(defaults.copy(expandStiffness = 300f), profile = IslandMotion.Profile.Droid)
        assertEquals(MotionToken.Physical(1f, 300f * 1.4f), custom.collapse)
    }

    @Test
    fun reduced_hasNoOvershootNoImpulsesNoLoops() {
        val m = motion(speed = 2f, tier = MotionTier.REDUCED)
        val geometry: SpringSpec<Float> = m.spring(m.expand)
        val reduced = IslandMotion.REDUCED_GEOMETRY.toPhysical()
        assertEquals(1f, geometry.dampingRatio, 0f)
        assertEquals("not speed-scaled", reduced.stiffness, geometry.stiffness, 0f)
        assertEquals(1f, m.spring<Float>(m.split, geometry = false).dampingRatio, 0f)
        assertNull(m.impulse(m.iconBounce, 4f))
        for (token in LoopToken.entries) assertNull(m.loop(token))
        val fade: TweenSpec<Float> = m.tween(750, delayMillis = 60)
        assertEquals(IslandMotion.REDUCED_FADE_MS, fade.durationMillis)
        assertEquals(0, fade.delay)
        assertFalse(m.allowsStretch || m.allowsBlur || m.allowsStaggerDelays)
    }

    @Test
    fun efficient_capsLoops_andDropsStretchAndBlur() {
        val m = motion(tier = MotionTier.EFFICIENT)
        assertTrue(m.loop(LoopToken.EQUALIZER)!!.frameIntervalMillis >= 33)
        assertTrue(m.loop(LoopToken.PULSE)!!.frameIntervalMillis >= 33)
        assertFalse(m.allowsStretch || m.allowsBlur)
        assertTrue(m.allowsStaggerDelays)
        assertNotNull(m.impulse(m.iconBounce, 4f))
        // Springs are the same as FULL
        assertEquals(motion().spring<Float>(motion().expand).stiffness, m.spring<Float>(m.expand).stiffness, 0f)
    }

    @Test
    fun full_loopsEveryFrame() {
        val loop = motion().loop(LoopToken.PULSE)!!
        assertEquals(0, loop.frameIntervalMillis)
        assertEquals(1_200, loop.periodMillis)
        assertTrue(motion().allowsStretch && motion().allowsBlur)
    }

    @Test
    fun impulse_carriesTheVelocity() {
        val i = motion().impulse(motion().iconBounce, 6f)!!
        assertEquals(6f, i.initialVelocity, 0f)
        assertEquals(0.55f, i.spec.dampingRatio, 0.0001f)
    }
}
