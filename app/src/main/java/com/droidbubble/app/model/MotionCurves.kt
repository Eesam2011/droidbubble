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

/**
 * D5.3: three one-tap sets for all four curves (duration in s at speed 1, bounce). "Eager in, calm
 * out" holds in each: Collapse bounces at most 0.05 and is quicker than Expand. [DROID] is the
 * default, today's feel (the migrated shipped springs, see [CurveMigration.DROID]).
 */
enum class MotionPreset(val label: String, val curves: CurveMigration.Curves) {
    CALM(
        "Calm",
        CurveMigration.Curves(
            expand = Curve(0.37f, 0.05f),
            collapse = Curve(0.31f, 0f),
            appear = Curve(0.40f, 0.12f),
            split = Curve(0.40f, 0.15f),
        ),
    ),
    DROID("Droid", CurveMigration.DROID),
    LIVELY(
        "Lively",
        CurveMigration.Curves(
            expand = Curve(0.29f, 0.28f),
            collapse = Curve(0.25f, 0.05f),
            appear = Curve(0.32f, 0.38f),
            split = Curve(0.32f, 0.40f),
        ),
    );

    /** [s] with all eight curve fields set to this preset: one copy, so one write. */
    fun applyTo(s: IslandSettings): IslandSettings = s.copy(
        expandDuration = curves.expand.duration, expandBounce = curves.expand.bounce,
        collapseDuration = curves.collapse.duration, collapseBounce = curves.collapse.bounce,
        appearDuration = curves.appear.duration, appearBounce = curves.appear.bounce,
        splitDuration = curves.split.duration, splitBounce = curves.split.bounce,
    )

    /** True when every one of [s]'s eight curve fields is exactly this preset's. */
    fun matches(s: IslandSettings): Boolean = curvesOf(s) == curves

    companion object {
        /** The four curves stored in [s]. */
        fun curvesOf(s: IslandSettings) = CurveMigration.Curves(
            expand = Curve(s.expandDuration, s.expandBounce),
            collapse = Curve(s.collapseDuration, s.collapseBounce),
            appear = Curve(s.appearDuration, s.appearBounce),
            split = Curve(s.splitDuration, s.splitBounce),
        )

        /** The preset [s] is on, or null ("Custom") after any manual edit. */
        fun detect(s: IslandSettings): MotionPreset? = entries.firstOrNull { it.matches(s) }
    }
}
