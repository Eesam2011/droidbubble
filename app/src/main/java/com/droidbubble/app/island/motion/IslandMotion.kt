package com.droidbubble.app.island.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.droidbubble.app.model.IslandSettings
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A spring described two ways. Perceptual is the design language; Physical is the stored form. */
sealed interface MotionToken {
    /** A spring by feel: [duration] in seconds at speed 1, [bounce] from −1 to 1 (0 = no overshoot). */
    data class Perceptual(val duration: Float, val bounce: Float) : MotionToken

    /**
     * A raw Compose spring. Used for the user's curves and for values that must stay bit-exact.
     * [speedScaled]: the stiffness is multiplied by the island's animation speed squared.
     */
    data class Physical(val dampingRatio: Float, val stiffness: Float, val speedScaled: Boolean = true) : MotionToken
}

/** duration and bounce → Compose: ζ = 1 − b (b ≥ 0) or 1 / (1 + b) (b < 0); k = (2π / d)². */
fun MotionToken.Perceptual.toPhysical(): MotionToken.Physical {
    val zeta = if (bounce >= 0f) 1f - bounce else 1f / (1f + bounce)
    val k = (2f * PI.toFloat() / duration).pow(2)
    return MotionToken.Physical(zeta.coerceIn(0.05f, 4f), k)
}

/** The inverse, for springs with ζ ≤ 1: d = 2π / √k, b = 1 − ζ. */
fun MotionToken.Physical.toPerceptual() =
    MotionToken.Perceptual(2f * PI.toFloat() / sqrt(stiffness), 1f - dampingRatio)

/** A velocity kick on a spring that rests at its target: a bounce, a wiggle, an alert bump. */
@Immutable
data class Impulse(val spec: SpringSpec<Float>, val initialVelocity: Float)

/** A repeating ambient effect. [frameIntervalMillis] 0 means every display frame. */
@Immutable
data class LoopSpec(val periodMillis: Int, val frameIntervalMillis: Int, val easing: Easing)

enum class LoopToken(val periodMillis: Int) { PULSE(1_200), BREATHE(2_400), ROTATE(1_000), EQUALIZER(0) }

/**
 * Every animation spec the island uses, in one place (F3.1). It applies the user's animation
 * speed and the motion tier, so call sites name a token instead of writing a spring.
 *
 * The three user curves ([expand], [appear], [split]) are the ones from the settings and are
 * never retuned here.
 */
@Immutable
data class IslandMotion(
    /** `settings.animationSpeed`, clamped to 0.5–2. */
    val speed: Float,
    val tier: MotionTier,
    /** User curve: the card opening. */
    val expand: MotionToken.Physical,
    /** User curve: system glances (charging, unlock, connection, ringer) changing the shape. */
    val appear: MotionToken.Physical,
    /** User curve: the pill appearing, hiding and minimizing, and the bubble splitting off. */
    val split: MotionToken.Physical,
    val profile: Profile = Profile.Legacy,
) {
    /** [Legacy]: the values the island shipped with. [Droid]: "eager in, calm out" (M5.3). */
    enum class Profile { Legacy, Droid }

    private val speedSq = speed * speed
    private val droid get() = profile == Profile.Droid

    /** The card closing: no overshoot, 1.4× the expand stiffness. */
    val collapse: MotionToken.Physical
        get() = if (droid && expand == DEFAULT_EXPAND) DROID_COLLAPSE else MotionToken.Physical(1f, expand.stiffness * 1.4f)

    // ---- Fixed tokens (today's values) ----
    /** The pill shrinking away while the card opens. */
    val pillShrinkOnExpand = MotionToken.Physical(0.82f, 380f)
    /** The bubble going back into the pill. */
    val bubbleRetract = MotionToken.Physical(0.85f, 380f)
    /** A dragged card following and returning. */
    val cardDrag = MotionToken.Physical(0.76f, 420f)
    /** The pill and bubble sinking under a finger. Not speed-scaled. */
    val press: MotionToken.Physical get() = if (droid) DROID_PRESS else MotionToken.Physical(0.6f, 700f, speedScaled = false)
    /** The pill springing back after the swap squash. */
    val swapSquashOut = MotionToken.Physical(0.5f, 380f)
    val parenthesisShoot = MotionToken.Physical(0.65f, 260f)
    /** New compact content scaling in. Not speed-scaled. */
    val compactSwapScale = MotionToken.Physical(0.75f, 400f, speedScaled = false)

    // ---- Tokens the Droid profile adds. In Legacy each stands in for today's value. ----
    val dismiss: MotionToken.Physical get() = if (droid) DROID_DISMISS else collapse
    val merge: MotionToken.Physical get() = if (droid) DROID_MERGE else bubbleRetract
    val interactive: MotionToken.Physical get() = if (droid) DROID_INTERACTIVE else cardDrag
    val alertBump: MotionToken.Physical get() = if (droid) DROID_ALERT_BUMP else swapSquashOut
    val iconBounce: MotionToken.Physical get() = DROID_ICON_BOUNCE
    val iconWiggle: MotionToken.Physical get() = DROID_ICON_WIGGLE

    /**
     * A spring for [token]. Speed-scaled tokens get stiffness × speed².
     * In [MotionTier.REDUCED] a shape ([geometry]) moves on [REDUCED_GEOMETRY] and anything else
     * on a plain critically damped spring.
     */
    fun <T> spring(token: MotionToken, visibilityThreshold: T? = null, geometry: Boolean = true): SpringSpec<T> {
        if (tier == MotionTier.REDUCED) {
            return if (geometry) {
                val reduced = REDUCED_GEOMETRY.toPhysical()
                SpringSpec(reduced.dampingRatio, reduced.stiffness, visibilityThreshold)
            } else {
                SpringSpec(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium, visibilityThreshold)
            }
        }
        val physical = when (token) {
            is MotionToken.Physical -> token
            is MotionToken.Perceptual -> token.toPhysical()
        }
        val stiffness = if (physical.speedScaled) physical.stiffness * speedSq else physical.stiffness
        return SpringSpec(physical.dampingRatio, stiffness, visibilityThreshold)
    }

    /** A tween of [millis] ÷ speed. In [MotionTier.REDUCED] it becomes a [REDUCED_FADE_MS] fade with no delay. */
    fun <T> tween(millis: Int, easing: Easing = FastOutSlowInEasing, delayMillis: Int = 0): TweenSpec<T> {
        if (tier == MotionTier.REDUCED) return TweenSpec(REDUCED_FADE_MS, 0, LinearEasing)
        return TweenSpec(scaled(millis), scaled(delayMillis), easing)
    }

    /** Alpha, blur and colour changes: a 130 ms fade today, a quick critically damped spring in Droid. */
    fun <T> effects(): FiniteAnimationSpec<T> =
        if (droid && tier != MotionTier.REDUCED) spring(DROID_EFFECTS, geometry = false) else tween(CONTENT_FADE_MS)

    /** [millis] ÷ speed, rounded: how the island scales every duration. */
    fun scaled(millis: Int): Int = (millis / speed).roundToInt()

    /** A velocity kick for an icon bounce or an alert bump. Null in [MotionTier.REDUCED]: nothing overshoots. */
    fun impulse(token: MotionToken, velocity: Float): Impulse? =
        if (tier == MotionTier.REDUCED) null else Impulse(spring(token), velocity)

    /** An ambient loop. Null in [MotionTier.REDUCED] (draw the still frame); at most 30 fps in [MotionTier.EFFICIENT]. */
    fun loop(token: LoopToken): LoopSpec? = when (tier) {
        MotionTier.REDUCED -> null
        MotionTier.EFFICIENT -> LoopSpec(token.periodMillis, EFFICIENT_FRAME_MS, loopEasing(token))
        MotionTier.FULL -> LoopSpec(token.periodMillis, 0, loopEasing(token))
    }

    private fun loopEasing(token: LoopToken): Easing =
        if (token == LoopToken.ROTATE || token == LoopToken.EQUALIZER) LinearEasing else FastOutSlowInEasing

    val allowsStretch: Boolean get() = tier == MotionTier.FULL
    val allowsBlur: Boolean get() = tier == MotionTier.FULL
    val allowsStaggerDelays: Boolean get() = tier != MotionTier.REDUCED

    companion object {
        const val REDUCED_FADE_MS = 150
        const val CONTENT_FADE_MS = 130
        const val EFFICIENT_FRAME_MS = 33
        const val ROLL_MS = 380

        /** Reduced motion: no overshoot, settled in about 0.3 s. */
        val REDUCED_GEOMETRY = MotionToken.Perceptual(0.30f, 0f)

        /** The rolling digits' curve. */
        val ROLL_EASING: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

        private val DEFAULT_EXPAND = MotionToken.Physical(IslandSettings().expandDamping, IslandSettings().expandStiffness)

        // The Droid profile (research 03 §6), at speed 1
        val DROID_COLLAPSE = MotionToken.Perceptual(0.38f, 0f).toPhysical()
        val DROID_DISMISS = MotionToken.Perceptual(0.32f, 0f).toPhysical()
        val DROID_MERGE = MotionToken.Perceptual(0.35f, 0.05f).toPhysical()
        val DROID_ALERT_BUMP = MotionToken.Perceptual(0.30f, 0.45f).toPhysical()
        val DROID_PRESS = MotionToken.Perceptual(0.20f, 0.30f).toPhysical()
        val DROID_INTERACTIVE = MotionToken.Perceptual(0.18f, 0.10f).toPhysical()
        val DROID_EFFECTS = MotionToken.Physical(1f, 1600f)
        val DROID_ICON_BOUNCE = MotionToken.Perceptual(0.30f, 0.45f).toPhysical()
        val DROID_ICON_WIGGLE = MotionToken.Perceptual(0.25f, 0.35f).toPhysical()

        fun from(settings: IslandSettings, tier: MotionTier = MotionTier.FULL, profile: Profile = Profile.Legacy) = IslandMotion(
            speed = settings.animationSpeed.coerceIn(0.5f, 2f),
            tier = tier,
            expand = MotionToken.Physical(settings.expandDamping, settings.expandStiffness),
            appear = MotionToken.Physical(settings.appearDamping, settings.appearStiffness),
            split = MotionToken.Physical(settings.splitDamping, settings.splitStiffness),
            profile = profile,
        )
    }
}

/**
 * The island's motion. `IslandOverlayRoot` provides it from the live settings; the default (the
 * default settings) only serves previews and tests that render one piece of the island alone.
 */
val LocalIslandMotion = staticCompositionLocalOf { IslandMotion.from(IslandSettings()) }
