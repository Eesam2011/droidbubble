package com.droidbubble.app.island.icon

import androidx.compose.animation.core.SpringSpec
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LoopToken
import com.droidbubble.app.island.motion.MotionTier
import com.droidbubble.app.island.motion.MotionToken
import com.droidbubble.app.island.motion.toPhysical
import com.droidbubble.app.model.IslandSettings
import org.junit.Assert.assertEquals
import org.junit.Test

/** I0.1: the symbol-effect tokens convert to the ζ / k their comments give, and speed scales k by speed². */
class IconTokensTest {

    private fun check(name: String, token: MotionToken.Perceptual, zeta: Float, k: Float) {
        val p = token.toPhysical()
        assertEquals("$name ζ", zeta, p.dampingRatio, zeta * 0.01f)
        assertEquals("$name k", k, p.stiffness, k * 0.01f)
    }

    @Test
    fun tokens_matchTheTable() {
        val s = IslandMotion.Symbol
        check("bounce", s.bounce, 0.60f, 322f)
        check("wiggle", s.wiggle, 0.30f, 158f)
        check("replace", s.replace, 0.85f, 439f)
        check("morph", s.morph, 0.85f, 439f)
        check("appear", s.appear, 0.75f, 322f)
        check("draw", s.draw, 1.00f, 322f)
        check("scale", s.scale, 0.85f, 439f)
    }

    @Test
    fun speedScalesStiffnessBySpeedSquared() {
        for (speed in listOf(0.5f, 0.7f, 1f, 2f)) {
            val m = IslandMotion.from(IslandSettings(animationSpeed = speed), MotionTier.FULL)
            val spec: SpringSpec<Float> = m.spring(IslandMotion.Symbol.bounce)
            assertEquals(322.3f * speed * speed, spec.stiffness, 322.3f * speed * speed * 0.01f)
            assertEquals(0.60f, spec.dampingRatio, 0.001f)
        }
    }

    @Test
    fun loopPeriods_comeFromTheSymbolTokens() {
        assertEquals(IslandMotion.Symbol.PULSE_PERIOD_MS, LoopToken.PULSE.periodMillis)
        assertEquals(IslandMotion.Symbol.BREATHE_PERIOD_MS, LoopToken.BREATHE.periodMillis)
        assertEquals(IslandMotion.Symbol.ROTATE_PERIOD_MS, LoopToken.ROTATE.periodMillis)
    }
}
