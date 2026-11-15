package com.droidbubble.app.ui.main

import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.layout.CompactSlots
import com.droidbubble.app.island.motion.MotionToken
import com.droidbubble.app.island.motion.toPhysical
import com.droidbubble.app.island.scene.IslandEnv
import com.droidbubble.app.island.scene.expandedCardWidth
import com.droidbubble.app.model.Curve
import com.droidbubble.app.model.CurveMigration
import com.droidbubble.app.model.IslandSettings
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * D5.2: what a user curve does once the island runs it. The spring is the one the island builds
 * (`MotionToken.Perceptual.toPhysical`, stiffness × speed², as `IslandMotion.spring` does), so
 * the editor's preview and numbers are the motion people see.
 *
 * - [settleSeconds]: the last time the step response leaves the 1% band around its target. For
 *   ζ < 1 it is found on the exact response, under the envelope bound
 *   ln(100/√(1−ζ²))/(ζω): the envelope alone overstates it by up to about 25% at the default
 *   bounces, because the response settles between its peaks. ζ = 1 gives 6.64/ω; ζ > 1 is
 *   solved numerically too.
 * - [overshoot]: exp(−πζ/√(1−ζ²)) of the travel (0 for ζ ≥ 1); [overshootDp] is that over
 *   the transition's travel in dp.
 */
data class SpringReadout(
    val dampingRatio: Double,
    /** The stiffness the island runs: k × speed². */
    val stiffness: Double,
    /** Seconds the curve's duration lasts at this speed: d ÷ speed. */
    val effectiveDuration: Double,
    val settleSeconds: Double,
    /** The peak past the target, as a fraction of the travel. */
    val overshoot: Double,
    val overshootDp: Double,
) {
    /** The preview's time axis: t ∈ [0, max(1.2 · settle, 0.3 s)]. */
    val plotSeconds: Double get() = max(1.2 * settleSeconds, 0.3)

    /** Progress (0 → 1) at [t] seconds. */
    fun step(t: Double): Double = springStep(dampingRatio, stiffness, t)

    companion object {
        /** The settle band: within 1% of the travel. */
        const val BAND = 0.01

        fun of(duration: Float, bounce: Float, speed: Float, travelDp: Float): SpringReadout {
            val physical = MotionToken.Perceptual(duration, bounce).toPhysical()
            val zeta = physical.dampingRatio.toDouble()
            val k = physical.stiffness.toDouble() * speed * speed
            val os = overshootOf(zeta)
            return SpringReadout(
                dampingRatio = zeta,
                stiffness = k,
                effectiveDuration = duration.toDouble() / speed,
                settleSeconds = settleTime(zeta, k),
                overshoot = os,
                overshootDp = os * travelDp,
            )
        }

        fun overshootOf(zeta: Double): Double =
            if (zeta < 1.0) exp(-PI * zeta / sqrt(1.0 - zeta * zeta)) else 0.0

        /** The last time the step response is more than [BAND] from its target, in seconds. */
        fun settleTime(zeta: Double, stiffness: Double): Double {
            val w = sqrt(stiffness)
            fun err(t: Double) = abs(1.0 - springStep(zeta, stiffness, t))
            // An upper bound: for ζ < 1 the error never exceeds its envelope; for ζ ≥ 1 it only
            // falls, so doubling finds a time inside the band
            var hi = if (zeta < 1.0) ln(1.0 / (BAND * sqrt(1.0 - zeta * zeta))) / (zeta * w) else 1.0 / w
            if (zeta >= 1.0) while (err(hi) > BAND) hi *= 2.0
            val n = 4000
            var lastOut = -1
            for (i in 0..n) if (err(hi * i / n) > BAND) lastOut = i
            if (lastOut < 0) return 0.0
            if (lastOut == n) return hi
            var a = hi * lastOut / n
            var b = hi * (lastOut + 1) / n
            repeat(50) {
                val m = (a + b) / 2.0
                if (err(m) > BAND) a = m else b = m
            }
            return b
        }
    }
}

/** The unit step response of a spring with damping ratio [zeta] and [stiffness] (unit mass), at [t] s. */
fun springStep(zeta: Double, stiffness: Double, t: Double): Double {
    val w = sqrt(stiffness)
    return when {
        abs(zeta - 1.0) < 1e-6 -> 1.0 - exp(-w * t) * (1.0 + w * t)
        zeta < 1.0 -> {
            val wd = w * sqrt(1.0 - zeta * zeta)
            1.0 - exp(-zeta * w * t) * (cos(wd * t) + (zeta * w / wd) * sin(wd * t))
        }
        else -> {
            val s = sqrt(zeta * zeta - 1.0)
            val slow = -w * (zeta - s)
            val fast = -w * (zeta + s)
            1.0 - (fast * exp(slow * t) - slow * exp(fast * t)) / (fast - slow)
        }
    }
}

/** The four user curves the editor edits (D5.1's fields). */
enum class CurveTarget(val label: String) {
    EXPAND("Expand"),
    COLLAPSE("Collapse"),
    APPEAR("Appear"),
    SPLIT("Split");

    fun curveOf(s: IslandSettings): Curve = when (this) {
        EXPAND -> Curve(s.expandDuration, s.expandBounce)
        COLLAPSE -> Curve(s.collapseDuration, s.collapseBounce)
        APPEAR -> Curve(s.appearDuration, s.appearBounce)
        SPLIT -> Curve(s.splitDuration, s.splitBounce)
    }

    /** [s] with this curve set to [c], clamped to the editor's range. */
    fun withCurve(s: IslandSettings, c: Curve): IslandSettings {
        val v = CurveMigration.clamp(c)
        return when (this) {
            EXPAND -> s.copy(expandDuration = v.duration, expandBounce = v.bounce)
            COLLAPSE -> s.copy(collapseDuration = v.duration, collapseBounce = v.bounce)
            APPEAR -> s.copy(appearDuration = v.duration, appearBounce = v.bounce)
            SPLIT -> s.copy(splitDuration = v.duration, splitBounce = v.bounce)
        }
    }

    /** The Droid preset for this curve: what Reset goes back to. */
    val droid: Curve get() = when (this) {
        EXPAND -> CurveMigration.DROID.expand
        COLLAPSE -> CurveMigration.DROID.collapse
        APPEAR -> CurveMigration.DROID.appear
        SPLIT -> CurveMigration.DROID.split
    }

    /**
     * How far the transition moves, in dp, for the overshoot readout: Expand widens the pill to
     * the card, Appear widens it out of the camera circle, Split moves the bubble its width plus
     * the gap. Collapse ends on the pill and counts no overshoot.
     */
    fun travelDp(s: IslandSettings, screenWidthDp: Int): Float = when (this) {
        EXPAND -> (expandedCardWidth(s, IslandEnv(screenWidthDp = screenWidthDp)) - s.widthDp.dp).value
        COLLAPSE -> 0f
        APPEAR -> (s.widthDp - s.heightDp).toFloat()
        SPLIT -> (CompactSlots.bubbleWidth(0.dp, s.heightDp.dp) + IslandTokens.Shell.BubbleGap).value
    }.coerceAtLeast(0f)
}

/** The animation speed the island runs at (`IslandMotion.from` clamps it the same way). */
fun islandSpeed(s: IslandSettings): Float = s.animationSpeed.coerceIn(0.5f, 2f)

/** The Bounce chip's word: None at 0 and below (no overshoot), then Gentle, Bouncy, Very bouncy. */
fun bounceLabel(bounce: Float): String = when {
    bounce <= 0f -> "None"
    bounce <= 0.15f -> "Gentle"
    bounce <= 0.3f -> "Bouncy"
    else -> "Very bouncy"
}
