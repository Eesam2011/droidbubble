package com.droidbubble.app.island.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.KeyframesSpec
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

/**
 * A fixed-length animation (fades, one-shot steps). [speedScaled]: the length and delay are
 * divided by the island's animation speed, like the springs.
 */
@Immutable
data class TweenToken(
    val millis: Int,
    val easing: Easing = FastOutSlowInEasing,
    val delayMillis: Int = 0,
    val speedScaled: Boolean = true,
)

/** The island's named tweens (Legacy values, F3.2). */
object MotionTween {
    /** A glance's checkmark or secondary icon fading in. */
    val CHECKMARK_FADE = TweenToken(150)
    /** The charging pill filling to the battery level. */
    val CHARGING_FILL = TweenToken(750, delayMillis = 60)
    /** The charging fill emptying when the glance ends. */
    val CHARGING_DRAIN = TweenToken(150, FastOutLinearInEasing)
    /** The pill's quick squash when it switches activity. */
    val SWAP_SQUASH_IN = TweenToken(110, speedScaled = false)
    /** Tapping the empty pill: it dips, then lifts, then settles on [IslandMotion.idleTapSettle]. */
    val IDLE_TAP_DOWN = TweenToken(70, speedScaled = false)
    val IDLE_TAP_UP = TweenToken(90, speedScaled = false)
    /** New compact content fading in, and the old content leaving. */
    val COMPACT_SWAP_FADE_IN = TweenToken(200, delayMillis = 50, speedScaled = false)
    val COMPACT_SWAP_OUT = TweenToken(120, speedScaled = false)
    /** The media card's play / pause icon swap. */
    val PLAY_PAUSE_IN = TweenToken(140, speedScaled = false)
    val PLAY_PAUSE_OUT = TweenToken(100, speedScaled = false)
    /** One half of the recording dot's pulse. */
    val RECORDING_PULSE = TweenToken(650, speedScaled = false)
    /** One half of a ringing alarm icon's pulse. */
    val ALERT_PULSE = TweenToken(520, speedScaled = false)
    /** The album cover turning away before the new one turns in. */
    val ART_FLIP_OUT = TweenToken(150, FastOutLinearInEasing, speedScaled = false)
    /** The timer clock icon's hand stepping one second. */
    val CLOCK_HAND = TweenToken(280, speedScaled = false)
    /** A countdown ring draining one second. */
    val COUNTDOWN_TICK = TweenToken(1000, LinearEasing, speedScaled = false)
    /** D3.3: a progress ring switching between spinning and showing its value. */
    val PROGRESS_MODE_FADE = TweenToken(150, speedScaled = false)
    /** One digit roll: short enough to always finish before the next second ticks. */
    val ROLL = TweenToken(IslandMotion.ROLL_MS, IslandMotion.ROLL_EASING, speedScaled = false)
}

/** A velocity kick on a spring that rests at its target: a bounce, a wiggle, an alert bump. */
@Immutable
data class Impulse(val spec: SpringSpec<Float>, val initialVelocity: Float)

/** A repeating ambient effect. [frameIntervalMillis] 0 means every display frame. */
@Immutable
data class LoopSpec(val periodMillis: Int, val frameIntervalMillis: Int, val easing: Easing)

enum class LoopToken(val periodMillis: Int) {
    PULSE(IslandMotion.Symbol.PULSE_PERIOD_MS),
    BREATHE(IslandMotion.Symbol.BREATHE_PERIOD_MS),
    ROTATE(IslandMotion.Symbol.ROTATE_PERIOD_MS),
    EQUALIZER(0),
}

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
    /** User curve (D5.1): the card closing. Null: critically damped at 1.4× the expand stiffness. */
    val collapseCurve: MotionToken.Physical? = null,
) {
    /** [Legacy]: the values the island shipped with. [Droid]: "eager in, calm out" (M5.3). */
    enum class Profile { Legacy, Droid }

    private val speedSq = speed * speed
    private val droid get() = profile == Profile.Droid

    /** The card closing: no overshoot, 1.4× the expand stiffness. */
    val collapse: MotionToken.Physical
        get() = if (droid && expand == DEFAULT_EXPAND) DROID_COLLAPSE
            else collapseCurve ?: MotionToken.Physical(1f, expand.stiffness * 1.4f)

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

    /** A glance's checkmark or secondary icon popping in with a small overshoot. */
    val checkmarkPop = MotionToken.Physical(0.52f, 320f)
    /** The empty pill settling after a tap. Not speed-scaled. */
    val idleTapSettle = MotionToken.Physical(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow, speedScaled = false)
    /** The flashlight card's beam following the level. Not speed-scaled. */
    val flashlightBeam = MotionToken.Physical(0.8f, 450f, speedScaled = false)
    /** The media seek thumb growing under a finger. Not speed-scaled. */
    val seekThumb = MotionToken.Physical(0.6f, 600f, speedScaled = false)
    /** The play / pause icon scaling in. Not speed-scaled. */
    val playPauseScale = MotionToken.Physical(0.6f, 500f, speedScaled = false)
    /**
     * D3.3: a progress ring or bar following its value: d 0.5 s, b 0.1 (ζ 0.9, k 158 at speed 1),
     * so 42 % → 57 % glides without a visible overshoot. Replaces the download bar's 0.9 / 140.
     */
    val progress = MotionToken.Perceptual(0.5f, 0.1f)
    /** A ride or delivery bar following its progress. Not speed-scaled. */
    val trackingProgress = MotionToken.Physical(0.9f, 120f, speedScaled = false)
    /** The new album cover turning in. Not speed-scaled. */
    val artFlipIn = MotionToken.Physical(0.7f, 380f, speedScaled = false)

    // ---- Tokens the Droid profile adds. In Legacy each stands in for today's value. ----
    val dismiss: MotionToken.Physical get() = if (droid) DROID_DISMISS else collapse
    val merge: MotionToken.Physical get() = if (droid) DROID_MERGE else bubbleRetract
    val interactive: MotionToken.Physical get() = if (droid) DROID_INTERACTIVE else cardDrag
    val alertBump: MotionToken.Physical get() = if (droid) DROID_ALERT_BUMP else swapSquashOut
    val iconBounce: MotionToken.Physical get() = DROID_ICON_BOUNCE
    val iconWiggle: MotionToken.Physical get() = DROID_ICON_WIGGLE

    /**
     * Symbol effects (phase 5, I0.1): the springs and loop periods of the dynamic icons, at speed 1.
     * Every value is a starting point to tune by eye in the Test panel's icon catalogue (I4.1).
     * The comments give ζ / k as [toPhysical] converts them.
     */
    object Symbol {
        /** "Something happened": a quick elastic dip and overshoot. ζ 0.60, k 322. */
        val bounce = MotionToken.Perceptual(duration = 0.35f, bounce = 0.40f)
        /** "Look here": a loose spring that swings a few times and settles. ζ 0.30, k 158. */
        val wiggle = MotionToken.Perceptual(duration = 0.50f, bounce = 0.70f)
        /** One glyph replacing another. ζ 0.85, k 439. */
        val replace = MotionToken.Perceptual(duration = 0.30f, bounce = 0.15f)
        /** A shape melting into another (play ↔ pause). */
        val morph = replace
        /** A glyph appearing. ζ 0.75, k 322. */
        val appear = MotionToken.Perceptual(duration = 0.35f, bounce = 0.25f)
        /** A stroke drawing itself (checkmark, slash). ζ 1.00, k 322. */
        val draw = MotionToken.Perceptual(duration = 0.35f, bounce = 0f)
        /** A selected glyph growing (catalogue only). */
        val scale = MotionToken.Perceptual(duration = 0.30f, bounce = 0.15f)

        /** Pulse: opacity 1 → 0.4 → 1 over one period (the recording dot's 650 ms each way). */
        const val PULSE_PERIOD_MS = 1_300
        /** Reduced motion keeps the pulse (it's opacity, not motion) but slows it to this. */
        const val PULSE_REDUCED_PERIOD_MS = 2_000
        const val BREATHE_PERIOD_MS = 2_000
        const val ROTATE_PERIOD_MS = 1_200
        /** By-layer effects start each layer this much after the previous one. */
        const val LAYER_STAGGER_MS = 40
        /** One step of a cumulative or iterative variable-color loop. */
        const val VARIABLE_STEP_MS = 150
        /** Reduced motion: a bounce or wiggle becomes a dip in opacity this long. */
        const val REDUCED_FADE_MS = IslandMotion.REDUCED_FADE_MS
    }

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

    /** A named tween. In [MotionTier.REDUCED] it becomes a [REDUCED_FADE_MS] fade with no delay. */
    fun <T> tween(token: TweenToken): TweenSpec<T> {
        if (tier == MotionTier.REDUCED) return TweenSpec(REDUCED_FADE_MS, 0, LinearEasing)
        return if (token.speedScaled) {
            TweenSpec(scaled(token.millis), scaled(token.delayMillis), token.easing)
        } else {
            TweenSpec(token.millis, token.delayMillis, token.easing)
        }
    }

    /**
     * A one-shot keyframe effect (the bell and vibrate shakes). Keyframes can't be interrupted
     * gracefully, so they are only for effects like these. In [MotionTier.REDUCED] it snaps to
     * the end instead.
     */
    fun <T> keyframes(init: KeyframesSpec.KeyframesSpecConfig<T>.() -> Unit): FiniteAnimationSpec<T> =
        if (tier == MotionTier.REDUCED) androidx.compose.animation.core.snap() else androidx.compose.animation.core.keyframes(init)

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

        private val DEFAULT_EXPAND = IslandSettings().let { MotionToken.Perceptual(it.expandDuration, it.expandBounce).toPhysical() }

        // The Droid profile (research 03 §6), at speed 1
        val DROID_COLLAPSE = MotionToken.Perceptual(0.38f, 0f).toPhysical()
        val DROID_DISMISS = MotionToken.Perceptual(0.32f, 0f).toPhysical()
        val DROID_MERGE = MotionToken.Perceptual(0.35f, 0.05f).toPhysical()
        val DROID_ALERT_BUMP = MotionToken.Perceptual(0.30f, 0.45f).toPhysical()
        val DROID_PRESS = MotionToken.Perceptual(0.20f, 0.30f).toPhysical()
        val DROID_INTERACTIVE = MotionToken.Perceptual(0.18f, 0.10f).toPhysical()
        val DROID_EFFECTS = MotionToken.Physical(1f, 1600f)
        // I0.1: the icon springs are the Symbol tokens (they had no users before phase 5)
        val DROID_ICON_BOUNCE = Symbol.bounce.toPhysical()
        val DROID_ICON_WIGGLE = Symbol.wiggle.toPhysical()

        fun from(settings: IslandSettings, tier: MotionTier = MotionTier.FULL, profile: Profile = Profile.Legacy) = IslandMotion(
            speed = settings.animationSpeed.coerceIn(0.5f, 2f),
            tier = tier,
            expand = MotionToken.Perceptual(settings.expandDuration, settings.expandBounce).toPhysical(),
            appear = MotionToken.Perceptual(settings.appearDuration, settings.appearBounce).toPhysical(),
            split = MotionToken.Perceptual(settings.splitDuration, settings.splitBounce).toPhysical(),
            profile = profile,
            collapseCurve = MotionToken.Perceptual(settings.collapseDuration, settings.collapseBounce).toPhysical(),
        )
    }
}

/**
 * The island's motion. `IslandOverlayRoot` provides it from the live settings; the default (the
 * default settings) only serves previews and tests that render one piece of the island alone.
 */
val LocalIslandMotion = staticCompositionLocalOf { IslandMotion.from(IslandSettings()) }
