package com.droidbubble.app.model

import kotlin.math.PI
import kotlin.math.sqrt

/** A user motion curve by feel (D5.1): [duration] in seconds at speed 1, [bounce] (0 = no overshoot). */
data class Curve(val duration: Float, val bounce: Float)

/**
 * The curves are stored as duration and bounce since motion schema 2 (D5.1). Before that they
 * were Compose springs (damping ratio ζ, stiffness k) for Expand, Appear and Split, with collapse
 * derived from Expand. [migrate] converts a stored set so every spring the island runs stays
 * the same: Expand, Collapse and Split exactly; the system glances, which used the old Appear
 * curve, now ride the new Appear (taken from the old Split, what people see when the pill
 * appears), a difference under 7% at the defaults.
 *
 * Conversion (the inverse of `MotionToken.Perceptual.toPhysical`): d = 2π/√k; b = 1 − ζ for
 * ζ ≤ 1, b = 1/ζ − 1 above. Animation speed is untouched: it still multiplies k by speed².
 */
object CurveMigration {
    const val SCHEMA = 2
    const val MIN_DURATION = 0.2f
    const val MAX_DURATION = 0.8f
    const val MIN_BOUNCE = -0.2f
    const val MAX_BOUNCE = 0.5f

    /** The springs as stored before schema 2; the defaults are what the app shipped with. */
    data class Legacy(
        val expandDamping: Float = 0.8f,
        val expandStiffness: Float = 380f,
        val appearDamping: Float = 0.6f,
        val appearStiffness: Float = 280f,
        val splitDamping: Float = 0.63f,
        val splitStiffness: Float = 320f,
    )

    data class Curves(val expand: Curve, val collapse: Curve, val appear: Curve, val split: Curve) {
        val all: List<Curve> get() = listOf(expand, collapse, appear, split)
    }

    data class Result(val curves: Curves, val clamped: Boolean)

    /** A spring (ζ, k) as duration and bounce, unclamped. */
    fun fromSpring(dampingRatio: Float, stiffness: Float): Curve = Curve(
        duration = (2.0 * PI / sqrt(stiffness.toDouble())).toFloat(),
        bounce = if (dampingRatio <= 1f) 1f - dampingRatio else 1f / dampingRatio - 1f,
    )

    fun clamp(c: Curve) = Curve(c.duration.coerceIn(MIN_DURATION, MAX_DURATION), c.bounce.coerceIn(MIN_BOUNCE, MAX_BOUNCE))

    fun migrate(old: Legacy): Result {
        val split = fromSpring(old.splitDamping, old.splitStiffness)
        val raw = Curves(
            expand = fromSpring(old.expandDamping, old.expandStiffness),
            // Collapse had no setting: critically damped at 1.4× the expand stiffness
            collapse = fromSpring(1f, old.expandStiffness * 1.4f),
            appear = split,
            split = split,
        )
        val clamped = Curves(clamp(raw.expand), clamp(raw.collapse), clamp(raw.appear), clamp(raw.split))
        return Result(clamped, clamped != raw)
    }

    /** The default curves ("Droid"): the shipped springs, migrated. Fresh installs start here. */
    val DROID: Curves = migrate(Legacy()).curves
}
