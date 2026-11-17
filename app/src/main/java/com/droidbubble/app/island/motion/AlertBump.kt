package com.droidbubble.app.island.motion

import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * D2.1: the alert bump is a velocity kick on the pill's width spring, not a scale. A spring at
 * rest that gets a kick of v0 swings out to `(v0 / ω) · g(ζ)` and comes back, with
 * `g(ζ) = exp(−ζ / √(1−ζ²) · atan(√(1−ζ²) / ζ))` (0.522 at ζ 0.55). So for a peak of [fraction]
 * of the width, `v0 = fraction · W · ω / g(ζ)`.
 */
object AlertBump {
    /** A change of activity widens the pill by 6 % at the peak. */
    const val FRACTION = 0.06f
    /** D2.3: a new event from the activity already shown, half as much. */
    const val PULSE_FRACTION = 0.03f

    /** The peak of a kicked spring per unit of v0 / ω. Critically damped and stiffer: 1/e. */
    fun peakGain(dampingRatio: Float): Float {
        val z = dampingRatio.coerceIn(0.01f, 0.999f)
        val s = sqrt(1f - z * z)
        return exp(-z / s * atan(s / z))
    }

    /** The kick, in width units per second, that peaks at [fraction] of [width]. */
    fun velocity(width: Float, dampingRatio: Float, stiffness: Float, fraction: Float = FRACTION): Float =
        fraction * width * sqrt(stiffness) / peakGain(dampingRatio)
}
