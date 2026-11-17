package com.droidbubble.app.island

import com.droidbubble.app.island.layout.ProgressAnimationPolicy
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.toPhysical
import com.droidbubble.app.model.IslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt

/** D3.3: progress rings and bars spring forward and snap back on a new download. */
class ProgressAnimationPolicyTest {

    @Test
    fun forward_springs() {
        assertFalse(ProgressAnimationPolicy.snaps(0.42f, 0.57f))
        assertFalse(ProgressAnimationPolicy.snaps(0f, 1f))
    }

    @Test
    fun smallStepBack_springs() {
        assertFalse(ProgressAnimationPolicy.snaps(0.57f, 0.42f))
        assertFalse(ProgressAnimationPolicy.snaps(0.59f, 0.40f))
    }

    @Test
    fun newDownload_snaps() {
        assertTrue(ProgressAnimationPolicy.snaps(0.95f, 0f))
        assertTrue(ProgressAnimationPolicy.snaps(0.5f, 0.29f))
    }

    @Test
    fun token_isZeta09_k158_atSpeed1() {
        val p = IslandMotion.from(IslandSettings()).progress.toPhysical()
        assertEquals(0.9f, p.dampingRatio, 1e-4f)
        assertEquals(157.9f, p.stiffness, 0.1f)
    }

    @Test
    fun overshoot_staysUnderOnePercentOfTheStep() {
        // Underdamped step response overshoot: exp(−ζπ / √(1 − ζ²)); 42 % → 57 % is a 15 % step
        val zeta = 0.9
        val overshoot = exp(-zeta * PI / sqrt(1 - zeta * zeta))
        assertTrue("overshoot ${overshoot * 15} % of the ring", overshoot * 15 < 1.0)
    }
}
