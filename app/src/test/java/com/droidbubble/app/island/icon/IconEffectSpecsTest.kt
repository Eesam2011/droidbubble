package com.droidbubble.app.island.icon

import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.MotionTier
import com.droidbubble.app.model.IslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** I1.1: tier mapping and speed scaling of the discrete effects. Pure JUnit. */
class IconEffectSpecsTest {

    private fun motion(tier: MotionTier = MotionTier.FULL, speed: Float = 1f) =
        IslandMotion.from(IslandSettings(animationSpeed = speed), tier)

    @Test
    fun reduced_isAnOpacityDipOnly() {
        for (effect in listOf(IconEffect.Bounce(), IconEffect.Wiggle(), IconEffect.Wiggle(IconEffect.WiggleAxis.Horizontal))) {
            val r = IconEffectSpecs.resolve(effect, motion(MotionTier.REDUCED, speed = 2f))
            assertTrue("$effect", r is ResolvedEffect.OpacityDip)
            r as ResolvedEffect.OpacityDip
            assertEquals("not speed-scaled", 150, r.spec.durationMillis)
            assertEquals(0.4f, r.depth, 0f)
        }
    }

    @Test
    fun bounce_kicksScaleAndOffset_upIsNegative() {
        val r = IconEffectSpecs.resolve(IconEffect.Bounce(), motion()) as ResolvedEffect.Kicks
        assertEquals(setOf(IconChannel.Scale, IconChannel.OffsetY), r.kicks.map { it.channel }.toSet())
        assertTrue(r.kicks.first { it.channel == IconChannel.OffsetY }.velocity < 0f)
        val down = IconEffectSpecs.resolve(IconEffect.Bounce(IconEffect.Direction.Down), motion()) as ResolvedEffect.Kicks
        assertTrue(down.kicks.first { it.channel == IconChannel.OffsetY }.velocity > 0f)
        assertEquals(0.60f, r.spec.dampingRatio, 0.001f)
        assertEquals(322.3f, r.spec.stiffness, 3f)
    }

    @Test
    fun wiggle_picksItsAxis() {
        val rot = IconEffectSpecs.resolve(IconEffect.Wiggle(), motion()) as ResolvedEffect.Kicks
        assertEquals(listOf(IconChannel.Rotation), rot.kicks.map { it.channel })
        val side = IconEffectSpecs.resolve(IconEffect.Wiggle(IconEffect.WiggleAxis.Horizontal), motion()) as ResolvedEffect.Kicks
        assertEquals(listOf(IconChannel.OffsetX), side.kicks.map { it.channel })
        assertEquals(0.30f, rot.spec.dampingRatio, 0.001f)
    }

    @Test
    fun speed_scalesStiffnessBySpeedSquared_andKicksBySpeed_soThePeakStays() {
        val one = IconEffectSpecs.resolve(IconEffect.Bounce(), motion(speed = 1f)) as ResolvedEffect.Kicks
        val half = IconEffectSpecs.resolve(IconEffect.Bounce(), motion(speed = 0.5f)) as ResolvedEffect.Kicks
        assertEquals(one.spec.stiffness * 0.25f, half.spec.stiffness, 0.5f)
        assertEquals(one.kicks[0].velocity * 0.5f, half.kicks[0].velocity, 0.001f)
    }

    @Test
    fun efficient_movesLikeFull() {
        assertEquals(
            IconEffectSpecs.resolve(IconEffect.Bounce(), motion()),
            IconEffectSpecs.resolve(IconEffect.Bounce(), motion(MotionTier.EFFICIENT)),
        )
    }
}

/** I1.1: kicks add up, but a burst's energy is capped. */
class StackedVelocityTest {
    private val omega = 18f

    @Test
    fun atRest_aKickIsTheKick() = assertEquals(-4.3f, IconEffectState.stackedVelocity(0f, 0f, -4.3f, omega), 0f)

    @Test
    fun moving_kicksAdd_upToTheCap() {
        val rebounding = IconEffectState.stackedVelocity(0f, 3f, -4.3f, omega)
        assertEquals(-1.3f, rebounding, 0.001f)
        val capped = IconEffectState.stackedVelocity(0.3f, 4f, -4.3f, omega)
        assertTrue("energy capped: $capped", capped * capped + (omega * 0.3f).let { it * it } <= (4.3f * 1.5f).let { it * it } + 0.01f)
    }

    @Test
    fun alreadyMovingThatWay_kickChangesNothing() {
        assertEquals(-4f, IconEffectState.stackedVelocity(-0.1f, -4f, -4.3f, omega), 0f)
    }

    @Test
    fun alreadyFarOut_noMoreEnergy() {
        val v = IconEffectState.stackedVelocity(0.5f, 1f, -4.3f, omega)
        assertEquals(0f, v, 0f)
    }
}
