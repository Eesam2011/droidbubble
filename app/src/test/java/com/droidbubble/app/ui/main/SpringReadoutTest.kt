package com.droidbubble.app.ui.main

import com.droidbubble.app.model.CurveMigration
import com.droidbubble.app.model.IslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** D5.2: the editor's settle time and overshoot match a numeric simulation of the same spring. */
class SpringReadoutTest {

    /** RK4 on x'' = k(1 − x) − 2ζ√k·x' from rest at 0; the peak and the last time outside the 1% band. */
    private fun simulate(zeta: Double, k: Double, tEnd: Double, dt: Double = 1e-5): Pair<Double, Double> {
        val c = 2.0 * zeta * sqrt(k)
        fun acc(x: Double, v: Double) = k * (1.0 - x) - c * v
        var x = 0.0
        var v = 0.0
        var t = 0.0
        var peak = 0.0
        var lastOut = 0.0
        while (t < tEnd) {
            val k1x = v; val k1v = acc(x, v)
            val k2x = v + k1v * dt / 2; val k2v = acc(x + k1x * dt / 2, v + k1v * dt / 2)
            val k3x = v + k2v * dt / 2; val k3v = acc(x + k2x * dt / 2, v + k2v * dt / 2)
            val k4x = v + k3v * dt; val k4v = acc(x + k3x * dt, v + k3v * dt)
            x += dt / 6 * (k1x + 2 * k2x + 2 * k3x + k4x)
            v += dt / 6 * (k1v + 2 * k2v + 2 * k3v + k4v)
            t += dt
            peak = max(peak, x)
            if (abs(1.0 - x) > SpringReadout.BAND) lastOut = t
        }
        return (peak - 1.0).coerceAtLeast(0.0) to lastOut
    }

    private fun assertMatchesSimulation(duration: Float, bounce: Float, speed: Float) {
        val r = SpringReadout.of(duration, bounce, speed, travelDp = 100f)
        val (simOvershoot, simSettle) = simulate(r.dampingRatio, r.stiffness, tEnd = 3.0 * r.settleSeconds + 0.2)
        assertEquals("settle d=$duration b=$bounce s=$speed", simSettle, r.settleSeconds, 0.02 * simSettle)
        if (simOvershoot > 1e-4) {
            assertEquals("overshoot d=$duration b=$bounce s=$speed", simOvershoot, r.overshoot, 0.02 * simOvershoot)
        } else {
            assertTrue(r.overshoot < 1e-3)
        }
    }

    @Test
    fun droidCurves_atDefaultSpeed_matchSimulation() {
        val speed = islandSpeed(IslandSettings())
        CurveMigration.DROID.all.forEach { assertMatchesSimulation(it.duration, it.bounce, speed) }
    }

    @Test
    fun rangeEdges_matchSimulation() {
        for (d in listOf(0.2f, 0.5f, 0.8f)) for (b in listOf(-0.2f, -0.05f, 0f, 0.1f, 0.3f, 0.5f)) for (s in listOf(0.5f, 1f, 2f)) {
            assertMatchesSimulation(d, b, s)
        }
    }

    @Test
    fun speedSquared_isIncluded() {
        // At 0.7× the island runs k·0.49, so the curve takes 1/0.7 as long as at 1×
        val atOne = SpringReadout.of(0.322f, 0.2f, 1f, 0f)
        val atDefault = SpringReadout.of(0.322f, 0.2f, 0.7f, 0f)
        assertEquals(atOne.stiffness * 0.49, atDefault.stiffness, 1e-6)
        assertEquals(atOne.settleSeconds / 0.7, atDefault.settleSeconds, 1e-3)
        assertEquals(0.46, atDefault.effectiveDuration, 0.005)
    }

    @Test
    fun criticallyDamped_settlesAt6_64OverOmega() {
        val r = SpringReadout.of(0.3f, 0f, 1f, 0f)
        assertEquals(6.64 / sqrt(r.stiffness), r.settleSeconds, 0.01 * r.settleSeconds)
        assertEquals(0.0, r.overshoot, 0.0)
    }

    @Test
    fun expandOvershoot_atDefaults_isAbout2_9dp() {
        val s = IslandSettings()
        val travel = CurveTarget.EXPAND.travelDp(s, screenWidthDp = 411)
        assertEquals(194f, travel, 0.5f)
        val r = SpringReadout.of(s.expandDuration, s.expandBounce, islandSpeed(s), travel)
        assertEquals(2.9, r.overshootDp, 0.1)
    }

    @Test
    fun travel_perTransition() {
        val s = IslandSettings()
        assertEquals(0f, CurveTarget.COLLAPSE.travelDp(s, 411), 0f)
        assertEquals((s.widthDp - s.heightDp).toFloat(), CurveTarget.APPEAR.travelDp(s, 411), 0f)
        assertEquals(s.heightDp + 6f, CurveTarget.SPLIT.travelDp(s, 411), 0.01f)
    }

    @Test
    fun plotAxis_coversTheSettleOrAtLeast0_3s() {
        val slow = SpringReadout.of(0.8f, 0.3f, 0.5f, 0f)
        assertEquals(1.2 * slow.settleSeconds, slow.plotSeconds, 1e-9)
        val fast = SpringReadout.of(0.2f, 0f, 2f, 0f)
        assertEquals(0.3, fast.plotSeconds, 1e-9)
    }

    @Test
    fun bounceLabels() {
        assertEquals("None", bounceLabel(-0.2f))
        assertEquals("None", bounceLabel(0f))
        assertEquals("Gentle", bounceLabel(0.15f))
        assertEquals("Bouncy", bounceLabel(0.2f))
        assertEquals("Bouncy", bounceLabel(0.3f))
        assertEquals("Very bouncy", bounceLabel(0.37f))
    }
}
