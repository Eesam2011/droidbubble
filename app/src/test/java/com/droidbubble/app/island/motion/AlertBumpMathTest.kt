package com.droidbubble.app.island.motion

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

/** D2.1: the kick that makes the pill's width spring peak at 6 %, checked against a simulated spring. */
class AlertBumpMathTest {

    /** Peak of a spring at rest at 0 that is kicked with [v0], integrated in small steps. */
    private fun simulatedPeak(v0: Double, zeta: Double, k: Double): Double {
        var x = 0.0
        var v = v0
        var peak = 0.0
        val dt = 1e-5
        repeat(400_000) {
            val a = -k * x - 2 * zeta * sqrt(k) * v
            v += a * dt
            x += v * dt
            if (x > peak) peak = x
        }
        return peak
    }

    @Test
    fun specGain_is0522_atZeta055() {
        assertEquals(0.522f, AlertBump.peakGain(0.55f), 0.001f)
    }

    @Test
    fun specKick_isAbout240WidthsPerSecond() {
        // d 0.30 s, b 0.45: zeta 0.55, omega 2 pi / 0.30
        val omega = 2 * Math.PI / 0.30
        val v0 = AlertBump.velocity(1f, 0.55f, (omega * omega).toFloat())
        assertEquals(2.40f, v0, 0.01f)
        assertEquals(432f, AlertBump.velocity(180f, 0.55f, (omega * omega).toFloat()), 2f)
    }

    @Test
    fun peak_isSixPercent_forEverySpringTheIslandUses() {
        // The Droid token, the Legacy swap spring at speed 1 and at the default speed 0.7, and a stiff one
        val springs = listOf(0.55f to 438.6f, 0.5f to 380f, 0.5f to 380f * 0.49f, 0.8f to 1500f)
        for ((zeta, k) in springs) {
            for (width in listOf(90f, 180f, 260f)) {
                val v0 = AlertBump.velocity(width, zeta, k)
                val peak = simulatedPeak(v0.toDouble(), zeta.toDouble(), k.toDouble())
                assertEquals("zeta $zeta, k $k, W $width", 0.06, peak / width, 0.0015)
            }
        }
    }

    @Test
    fun pulse_isHalfTheBump() {
        val full = AlertBump.velocity(180f, 0.5f, 380f)
        val pulse = AlertBump.velocity(180f, 0.5f, 380f, AlertBump.PULSE_FRACTION)
        assertEquals(full / 2f, pulse, 0.01f)
    }
}
